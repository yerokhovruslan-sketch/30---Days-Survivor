from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'WorldStories.java';s=p.read_text(encoding='utf-8').replace('    boolean archiveComplete;', '    transient PoliceArchive.Attempt archiveAttempt;\n    void leavePoliceLocation(){archiveAttempt=null;}\n    boolean archiveComplete;');p.write_text(s,encoding='utf-8')
p=b/'GameSession.java';s=p.read_text(encoding='utf-8').replace('        player.settleAtHome();','        if(world!=null)world.getStories().leavePoliceLocation();\n        player.settleAtHome();');p.write_text(s,encoding='utf-8')
p=b/'Locations.java';s=p.read_text(encoding='utf-8').replace('        getStories().beginVisit(index + 1,', '        if(index+1!=15 && index+1!=16)getStories().leavePoliceLocation();\n        getStories().beginVisit(index + 1,');p.write_text(s,encoding='utf-8')
p=b/'TacticalBattle.java';s=p.read_text(encoding='utf-8').replace('int cell, skip, jump=-1', 'boolean alerted;int lastKnown=-1;\n        int cell, skip, jump=-1').replace('    final Map<Integer,String> markers=', '    int lastActionNoise;\n    final Map<Integer,String> markers=')
s=s.replace('    void act(int action,Scanner scanner) {','    void act(int action,Scanner scanner) {\n            lastActionNoise=0;')
s=s.replace('player.addNoise(style.noise());', 'lastActionNoise=Math.max(lastActionNoise,style.noise());player.addNoise(style.noise());')
# Only fixed archive enemies use sight/hearing; ordinary combat retains its rules.
s=s.replace('            boolean slowed=unit.slow>0;', '''            if(fixedEncounter) {
                if(clearShot(unit.cell,playerCell)){unit.alerted=true;unit.lastKnown=playerCell;}
                else if(lastActionNoise>0){unit.alerted=true;unit.lastKnown=playerCell;}
                if(!unit.alerted)continue;
                if(!clearShot(unit.cell,playerCell)) {
                    if(unit.skip>0){unit.skip--;continue;}
                    if(unit.lastKnown>=0 && unit.cell!=unit.lastKnown)toward(unit,unit.lastKnown,unit.slow>0?0:1);
                    if(unit.slow>0)unit.slow--;
                    if(unit.cell==unit.lastKnown)unit.alerted=false;
                    continue;
                }
            }
            boolean slowed=unit.slow>0;''')
p.write_text(s,encoding='utf-8')
p=b/'PoliceArchive.java';s=p.read_text(encoding='utf-8').replace('final boolean[] visited={true,false};','final boolean[] visited={true,false};int searchProgress;boolean papersCollected;')
s=s.replace('            from.endTurn();if(!from.player.isAlive())return false;', '''            for(TacticalBattle.Unit u:from.units)if(u.active() && from.clearShot(u.cell,from.playerCell)){u.alerted=true;u.lastKnown=exit;}
            from.lastActionNoise=0;from.endTurn();if(!from.player.isAlive())return false;''')
s=s.replace('            if(!visited[1-room])return;', '            TacticalBattle listening=rooms[room];\n            if(!visited[1-room] && listening.lastActionNoise==0)return;')
s=s.replace('            for(TacticalBattle.Unit unit:new ArrayList<>(other.units))if(unit.active()) {', '''            for(TacticalBattle.Unit unit:new ArrayList<>(other.units))if(unit.active()) {
                if(active.lastActionNoise>0){unit.alerted=true;unit.lastKnown=exit;}
                if(!unit.alerted)continue;
                if(unit.skip>0){unit.skip--;continue;}
                if(unit.slow>0){unit.slow--;continue;}''')
s=s.replace('unit.cell=arrival;active.units.add(unit);', 'unit.cell=arrival;unit.lastKnown=arrival;active.units.add(unit);')
s=s.replace('        if(world.archiveComplete){documents(world,scanner);return;}', '        if(world.archiveComplete){documents(world,scanner);return;}')
s=s.replace('Два связанных зала: в первом два обычных заражённых, во втором два обычных и громила. Новых подкреплений от выстрелов нет. Д — вход и полный выход, В — переход, П — документы. Полный выход сбросит врагов и расстановку, но не вернёт здоровье, патроны и потраченные предметы.', 'Ключ подходит, но дверь поддаётся с трудом: её недавно запирали изнутри. Кто это сделал — неизвестно. На раме глубокие вмятины, из второго зала слышны тяжёлые шаги: там громила. С одной трубой опасно, лучше подготовить исправное оружие и лечение. Можно отказаться от входа.\\nШкаф обыскивается за три действия, зачистка необязательна. Выстрелы привлекут только уже находящихся внутри врагов. Их состояние сохранится до ухода из участка; здоровье и патроны не восстанавливаются.')
s=s.replace('p.reduceNoise(60);Attempt attempt=new Attempt(p,random);', 'p.reduceNoise(60);Attempt attempt=world.archiveAttempt;\n        if(attempt==null || attempt.rooms[0].player!=p){attempt=new Attempt(p,random);world.archiveAttempt=attempt;}\n        attempt.room=0;attempt.rooms[0].playerCell=ENTRY;')
s=s.replace('Уничтожьте всех пятерых, затем осмотрите документы. Соседние враги могут преследовать через переход.', 'Шкаф обыскивается за три действия прямо на его клетке. Врагов можно отвлечь и обойти. Соседние враги идут на выстрелы или преследуют замеченного Джона. После копирования папки вернитесь ко входу.')
s=s.replace('GameIO.action(21,"Осмотреть документы","В нижнем зале у клетки П, когда оба зала очищены",attempt.room==1 && attempt.cleared() && TacticalBattle.distance(b.playerCell,DOCUMENTS)<=1);', 'GameIO.action(21,attempt.papersCollected?"Документы скопированы":"Обыскать шкаф · "+attempt.searchProgress+"/3","На клетке шкафа · одно действие, затем ход врагов",attempt.room==1 && !attempt.papersCollected && b.playerCell==DOCUMENTS);')
s=s.replace('Только с клетки Д. Весь бой придётся начать заново', 'Только с клетки выхода. Состояние залов сохранится до ухода из участка')
a=s.index('                if(action==0 && attempt.room==0');z=s.index('                if(action==20)',a)
s=s[:a]+'''                if(action==0 && attempt.room==0 && b.playerCell==ENTRY){
                    GameIO.storyMenu("Выйти в участок?","Убитые и повреждения сохранятся до ухода из участка. Потраченные ресурсы не вернутся.");GameIO.action(1,"Выйти в участок");GameIO.action(0,"Остаться");
                    if(SurvivalSystems.choice(scanner)==1){if(attempt.papersCollected){world.archiveComplete=true;world.note("Противоречие в приказах",EVIDENCE);GameIO.checkpoint();documents(world,scanner);}return;}continue;
                }
''' +s[z:]
a=s.index('                if(action==21 &&');z=s.index('                if(action==7',a)
s=s[:a]+'''                if(action==21 && attempt.room==1 && !attempt.papersCollected && b.playerCell==DOCUMENTS){
                    b.lastActionNoise=0;b.endTurn();attempt.pursue();
                    if(p.isAlive()) {attempt.searchProgress++;if(attempt.searchProgress==3){attempt.papersCollected=true;b.log.add("Папка скопирована. Вернитесь к выходу из архива.");}else b.log.add("Поиск в шкафу: "+attempt.searchProgress+"/3.");}
                    GameIO.checkpoint();continue;
                }
''' +s[z:]
s=s.replace('        }finally{for(TacticalBattle b:attempt.rooms)b.depositCases();p.reduceNoise(60);', '        }finally{if(!p.isAlive())world.archiveAttempt=null;p.reduceNoise(60);')
s=s.replace('    static void documents(WorldStories world,Scanner scanner) {', '''    static final String EVIDENCE="Приказ о сборе гражданских на военном складе датирован за два дня до первой передачи. Отмена выделения колонны подписана на следующий день, тоже до первой передачи. На отдельном листе без даты и подписи: «Сбор сохранить».\\nСхема: северный блокпост закрыт; подход к складу — через служебную полосу промышленной зоны, затем обход ограждения к восточным воротам. Медицинский экипаж: машина № 17, станция скорой помощи. В журнале выезда указано: «Вернулся с пункта сбора». Кто продолжил сбор и что произошло с экипажем — неизвестно.";
    static void documents(WorldStories world,Scanner scanner) {''')
a=s.index('        GameIO.storyMenu("Архив · противоречие",');z=s.index('\n        GameIO.action(1,"Отметить',a)
s=s[:a]+'        GameIO.storyMenu("Архив · противоречие","Ты кладёшь два приказа рядом.\\n"+EVIDENCE);'+s[z:]
p.write_text(s,encoding='utf-8')
