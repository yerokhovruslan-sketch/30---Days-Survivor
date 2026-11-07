package com.example.survivor.game;

import java.util.*;

/** Fixed two-room encounter. Attempt state is deliberately never serialized. */
final class PoliceArchive {
    static final String KEY="Ключ от архива участка";
    static final int ENTRY=24,UPPER_DOOR=62,LOWER_DOOR=1,DOCUMENTS=14;
    static boolean unlocked(Fighter p){return p.getUpgrades().radio>0 && p.getUpgrades().signalReceived && p.getUpgrades().signalStage==0;}
    static boolean hasKey(Fighter p){return p.getInventory().stream().anyMatch(i->i.getName().equals(KEY));}
    static final class Attempt implements java.io.Serializable {
        private static final long serialVersionUID=1L;
        final TacticalBattle[] rooms=new TacticalBattle[2];int room;final boolean[] visited={true,false};int searchProgress;boolean papersCollected;
        Attempt(Fighter p,Random random) {
            for(int r=0;r<2;r++) {
                TacticalBattle b=new TacticalBattle(p,1,random,Collections.emptyList());rooms[r]=b;
                Arrays.fill(b.obstacles,null);b.units.clear();b.log.clear();b.fixedEncounter=true;
                b.terrain="Архив · "+(r==0?"верхний зал":"нижний зал");
                // Each character is one exact square; shelves and walls block both walking and fire.
                String[] plan=r==0?new String[]{"########","#..#...#","#......#","...###.#","#.#....#","#.#.##.#","#......#","######.#"}
                    :new String[]{"#.######","#.#....#","#.#.#..#","#...#..#","#.###..#","#.....##","#..##..#","########"};
                for(int y=0;y<8;y++)for(int x=0;x<8;x++)if(plan[y].charAt(x)=='#')b.obstacles[y*8+x]=TacticalBattle.Obstacle.WALL;
                b.playerCell=r==0?ENTRY:LOWER_DOOR;
                int[] spawns=r==0?new int[]{13,38}:new int[]{9,54,22};
                for(int n=0;n<spawns.length;n++)b.units.add(new TacticalBattle.Unit(new Zombie(n+1,r==1 && n==2?ZombieType.BRUTE:ZombieType.NORMAL),spawns[n]));
            }
            rooms[0].markers.put(ENTRY,"ВХОД · покинуть архив");rooms[0].markers.put(UPPER_DOOR,"ПЕРЕХОД · нижний зал");
            rooms[1].markers.put(LOWER_DOOR,"ПЕРЕХОД · верхний зал");rooms[1].markers.put(DOCUMENTS,"ДОКУМЕНТЫ");
        }
        boolean cleared(){return rooms[0].won() && rooms[1].won();}
        boolean canChangeRoom(){return rooms[room].playerCell==(room==0?UPPER_DOOR:LOWER_DOOR) && rooms[1-room].unitAt(room==0?LOWER_DOOR:UPPER_DOOR)==null;}
        boolean searchCabinet(){
            TacticalBattle b=rooms[room];if(room!=1 || papersCollected || b.playerCell!=DOCUMENTS)return false;
            GameIO.sound(searchProgress==0?GameSound.METAL_CONTAINER:GameSound.PAPER);
            b.lastActionNoise=0;b.endTurn();pursue();
            if(b.player.isAlive()){searchProgress++;if(searchProgress==3){papersCollected=true;b.log.add("Папка скопирована. Вернитесь к выходу из архива.");}else b.log.add("Поиск в шкафу: "+searchProgress+"/3.");}
            return true;
        }
        boolean changeRoom() {
            TacticalBattle from=rooms[room],to=rooms[1-room];int exit=room==0?UPPER_DOOR:LOWER_DOOR,arrival=room==0?LOWER_DOOR:UPPER_DOOR;
            if(!canChangeRoom())return false;
            for(TacticalBattle.Unit u:from.units)if(u.active() && from.clearShot(u.cell,from.playerCell)){u.alerted=true;u.lastKnown=exit;}
            from.lastActionNoise=0;from.endTurn();if(!from.player.isAlive())return false;
            to.magazines.putAll(from.magazines);to.playerCell=arrival;to.moved=false;to.aimed=false;to.guarded=false;to.lastActionNoise=0;GameIO.sound(GameSound.METAL_DOOR);room=1-room;visited[room]=true;return true;
        }
        void pursue() {
            TacticalBattle listening=rooms[room];
            if(!visited[1-room] && listening.lastActionNoise==0)return;
            TacticalBattle active=rooms[room],other=rooms[1-room];int exit=room==0?LOWER_DOOR:UPPER_DOOR,arrival=room==0?UPPER_DOOR:LOWER_DOOR;
            for(TacticalBattle.Unit unit:new ArrayList<>(other.units))if(unit.active()) {
                if(active.lastActionNoise>0){unit.alerted=true;unit.lastKnown=exit;}
                if(!unit.alerted)continue;
                if(unit.skip>0){unit.skip--;continue;}
                if(unit.slow>0){unit.slow--;continue;}
                if(unit.cell!=exit)unit.cell=other.nextStep(unit.cell,exit,unit.type()==ZombieType.BRUTE);
                if(unit.cell==exit && active.playerCell!=arrival && active.unitAt(arrival)==null) {
                    other.units.remove(unit);unit.cell=arrival;unit.lastKnown=arrival;active.units.add(unit);active.log.add(unit.enemy.getName()+" перешёл из соседнего зала.");
                }
            }
        }
    }
    static void enter(Fighter p,WorldStories world,Scanner scanner,Random random) {
        if(!unlocked(p)){GameIO.out().println("Пока неизвестно, какие документы искать в архиве.");return;}
        if(world.archiveComplete){documents(world,scanner);return;}
        if(!hasKey(p)){ResidentialStories.pause("Дверь архива","Нужен ключ. Поищите его в полицейском участке; если он на складе убежища — возьмите в рюкзак.",scanner);return;}
        GameIO.storyMenu("Попасть в архив","Ключ подходит, но дверь поддаётся с трудом: её недавно запирали изнутри. Кто это сделал — неизвестно. На раме глубокие вмятины, из второго зала слышны тяжёлые шаги: там громила. С одной трубой опасно, лучше подготовить исправное оружие и лечение. Можно отказаться от входа.\nШкаф обыскивается за три действия, зачистка необязательна. Выстрелы привлекут только уже находящихся внутри врагов. Их состояние сохранится после ухода; здоровье и патроны не восстанавливаются.");
        GameIO.action(1,"Открыть дверь ключом");GameIO.action(0,"Подготовиться и вернуться позже");if(SurvivalSystems.choice(scanner)!=1)return;
        GameIO.sound(GameSound.METAL_DOOR);
        p.reduceNoise(60);Attempt attempt=world.archiveAttempt;
        if(attempt==null || attempt.rooms[0].player!=p){attempt=new Attempt(p,random);world.archiveAttempt=attempt;}
        attempt.room=0;attempt.rooms[0].playerCell=ENTRY;
        try {
            while(p.isAlive()) {
                TacticalBattle b=attempt.rooms[attempt.room];b.dropDead();b.render();GameIO.removeActions(0,7);
                GameIO.card("Архив · два зала","Д: "+TacticalBattle.coordinate(ENTRY)+" верхнего зала. В: "+TacticalBattle.coordinate(attempt.room==0?UPPER_DOOR:LOWER_DOOR)+". П: "+TacticalBattle.coordinate(DOCUMENTS)+" нижнего зала. Шкаф обыскивается за три действия прямо на его клетке. Врагов можно отвлечь и обойти. Соседние враги идут на выстрелы или преследуют замеченного Джона. После копирования папки вернитесь ко входу.");
                GameIO.action(20,attempt.room==0?"В нижний зал":"В верхний зал","Только с клетки В · занимает ход",attempt.canChangeRoom());
                GameIO.action(21,attempt.papersCollected?"Документы скопированы":"Обыскать шкаф · "+attempt.searchProgress+"/3","На клетке шкафа · одно действие, затем ход врагов",attempt.room==1 && !attempt.papersCollected && b.playerCell==DOCUMENTS);
                GameIO.action(0,"Покинуть архив","Только с клетки выхода. Состояние залов сохранится после ухода",attempt.room==0 && b.playerCell==ENTRY);
                if(!scanner.hasNextInt())return;int action=scanner.nextInt();
                if(action==0 && attempt.room==0 && b.playerCell==ENTRY){
                    GameIO.storyMenu("Выйти в участок?","Убитые и повреждения сохранятся после ухода. Потраченные ресурсы не вернутся.");GameIO.action(1,"Выйти в участок");GameIO.action(0,"Остаться");
                    if(SurvivalSystems.choice(scanner)==1){GameIO.sound(GameSound.METAL_DOOR);if(attempt.papersCollected){world.archiveComplete=true;GameIO.sound(GameSound.QUEST);world.note("Противоречие в приказах",EVIDENCE);GameIO.checkpoint();documents(world,scanner);}return;}continue;
                }
                if(action==20){if(attempt.changeRoom())attempt.pursue();continue;}
                if(action==21 && attempt.room==1 && !attempt.papersCollected && b.playerCell==DOCUMENTS){
                    attempt.searchCabinet();
                    GameIO.checkpoint();continue;
                }
                if(action==7 || action==0 || action==20 || action==21)continue;
                int turn=b.turn;b.act(action,scanner);if(turn!=b.turn){attempt.pursue();GameIO.checkpoint();}
            }
        }finally{if(!p.isAlive())world.archiveAttempt=null;p.reduceNoise(60);GameIO.clearBattle();GameIO.checkpoint();}
    }
    static final String EVIDENCE="Приказ о сборе гражданских на военном складе датирован за два дня до первой передачи. Отмена выделения колонны подписана на следующий день, тоже до первой передачи. На отдельном листе без даты и подписи: «Сбор сохранить».\nСхема: северный блокпост закрыт; подход к складу — через служебную полосу промышленной зоны, затем обход ограждения к восточным воротам. Медицинский экипаж: машина № 17, станция скорой помощи. В журнале выезда указано: «Вернулся с пункта сбора». Кто продолжил сбор и что произошло с экипажем — неизвестно.";
    static void documents(WorldStories world,Scanner scanner) {
        GameIO.musicCue("silence");
        GameIO.storyMenu("Архив · противоречие","Ты кладёшь два приказа рядом.\n"+EVIDENCE+(world.schoolyard().busListsRead()?"\nДжон вспоминает школьные списки: группа А завершила посадку, группа Б ждала дополнительный транспорт. Что случилось с автобусами, по-прежнему неизвестно.":""));
        GameIO.action(1,"Отметить путь к военному складу","Проверить пункт сбора; сведения об экипаже останутся в журнале");
        GameIO.action(2,"Сначала проверить медицинский экипаж","Отметить станцию скорой помощи; схема склада сохранится");GameIO.action(0,"Решить позже");int c=SurvivalSystems.choice(scanner);
        if(c==1 || c==2)world.archiveGoal=c;
        world.note("Разведать резервный маршрут","Схема ведёт через промышленную территорию к восточной стороне военного склада.");
        world.note("Медицинский экипаж №17","Проверить станцию скорой помощи. Обе зацепки доступны независимо от выбора.");
        GameIO.checkpoint();
    }
    static void followup(Fighter p,WorldStories w,int place,Scanner scanner) {
        w.routeAfterArchive().open(p,w,place,scanner);
    }
}
