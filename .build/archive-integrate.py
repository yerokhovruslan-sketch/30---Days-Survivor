from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'WorldStories.java';s=p.read_text(encoding='utf-8');at=s.index('    private boolean radioOpeningSeen');s=s[:at]+'''    boolean archiveComplete;
    int archiveGoal;
    private boolean archiveKeyFound;
    boolean searchedPlace(int place){return searched.contains(place);}
    boolean archiveAvailable(Fighter p){return PoliceArchive.unlocked(p);}
    boolean findArchiveKey(Fighter p,int place){
        if(place!=15 || archiveKeyFound)return false;
        Tools key=new Tools(PoliceArchive.KEY,"Джону кажется, этот ключ важен. Он открывает архив участка; продать или выбросить его нельзя.",1,0);
        if(!p.addItem(key)){GameIO.out().println("В дежурной части найден ключ от архива, но рюкзак полон. Освободите место и вернитесь.");return false;}
        archiveKeyFound=true;note("Ключ от архива","В дежурной части найден ключ от архива участка. Ключ в рюкзаке. Какие документы нужны, пока неизвестно.");
        GameIO.out().println("Найден ключ от архива. Джон убирает его в рюкзак: кажется, он ещё пригодится.");GameIO.checkpoint();return true;
    }
''' +s[at:]
s=s.replace('        if (completedSearch) searched.add(place);\n        return result;', '        if (completedSearch) searched.add(place);\n        return result;')
# No random archival stories can prematurely reveal documents or bypass the encounter.
s=s.replace('        Discovery result = null;','        if(place==15 || place==16){if(completedSearch)searched.add(place);return null;}\n        Discovery result = null;',1)
p.write_text(s,encoding='utf-8')
p=b/'Survivor.java';s=p.read_text(encoding='utf-8').replace('        if (location.isDepleted()) return;\n        List<ItemInfo> chips=', '''        if(world.getNumber(location)==16){PoliceArchive.enter(player,world.getStories(),scanner,random);return;}
        if (location.isDepleted()) return;
        world.getStories().findArchiveKey(player,world.getNumber(location));
        List<ItemInfo> chips=''')
s=s.replace('                        if (destination.isDepleted()) {','''                        if(locations.getNumber(destination)==16){PoliceArchive.enter((Fighter)selectedSurvivor,locations.getStories(),scanner,ThreadLocalRandom.current());travel(selectedSurvivor,"Возвращение из участка",5);break;}
                        if (destination.isDepleted()) {
                            locations.getStories().findArchiveKey((Fighter)selectedSurvivor,locations.getNumber(destination));
                            if(locations.getNumber(destination)==15 && PoliceArchive.unlocked((Fighter)selectedSurvivor)) {
                                GameIO.storyMenu("Полицейский участок","Обычные запасы исчерпаны. Архив доступен отдельно.");GameIO.action(1,"Попасть в архив");GameIO.action(0,"Вернуться домой");
                                if(SurvivalSystems.choice(scanner)==1)PoliceArchive.enter((Fighter)selectedSurvivor,locations.getStories(),scanner,ThreadLocalRandom.current());
                            }
''',1)
p.write_text(s,encoding='utf-8')
p=b/'SearchMode.java';s=p.read_text(encoding='utf-8').replace('            GameIO.action(0, "Вернуться домой");','''            if(clock.currentPlace()==15 && PoliceArchive.unlocked(player))GameIO.action(8,"Попасть в архив","Ключ в рюкзаке · два связанных зала");
            if(clock.archiveComplete && clock.searchedPlace(clock.currentPlace()) && (clock.currentPlace()==13 || clock.currentPlace()==47))GameIO.action(9,clock.currentPlace()==13?"Проверить медицинский экипаж":"Проверить следы выезда и подвалы склада","Зацепка для следующей сюжетной части");
            GameIO.action(0, "Вернуться домой");''')
s=s.replace('            if (choice == 4) player.toggleFlashlight();','''            if(choice==8 && clock.currentPlace()==15 && PoliceArchive.unlocked(player)){PoliceArchive.enter(player,clock,scanner,java.util.concurrent.ThreadLocalRandom.current());if(!player.isAlive())return null;}
            if(choice==9 && clock.archiveComplete && clock.searchedPlace(clock.currentPlace()) && (clock.currentPlace()==13 || clock.currentPlace()==47))PoliceArchive.followup(clock,clock.currentPlace(),scanner);
            if (choice == 4) player.toggleFlashlight();''');p.write_text(s,encoding='utf-8')
p=b/'SurvivalSystems.java';s=p.read_text(encoding='utf-8').replace('return p.getUpgrades().radio==0 && (item.getName()', 'return item.getName().equals(PoliceArchive.KEY) || p.getUpgrades().radio==0 && (item.getName()');p.write_text(s,encoding='utf-8')
p=b/'Shop.java';s=p.read_text(encoding='utf-8').replace('Этот компонент нужен для восстановления связи. Продажа доступна после ремонта радиостанции.', 'Джону кажется, этот предмет ещё пригодится для основной цели. Он не станет продавать его сейчас.');p.write_text(s,encoding='utf-8')
p=b/'Item.java';s=p.read_text(encoding='utf-8').replace('        if (getName().equals("Электрочип"))', '        if (getName().equals(PoliceArchive.KEY)) return 10;\n        if (getName().equals("Электрочип"))');p.write_text(s,encoding='utf-8')
p=b/'Crafting.java';s=p.read_text(encoding='utf-8').replace('        if(item.getName().equals("Электрочип")', '        if(item.getName().equals(PoliceArchive.KEY))return result;\n        if(item.getName().equals("Электрочип")');p.write_text(s,encoding='utf-8')
p=b/'PoliceArchive.java';s=p.read_text(encoding='utf-8').replace('rooms=new TacticalBattle[2];int room;', 'rooms=new TacticalBattle[2];int room;final boolean[] visited={true,false};').replace('room=1-room;return true;', 'room=1-room;visited[room]=true;return true;').replace('            TacticalBattle active=rooms[room],other=rooms[1-room];', '            if(!visited[1-room])return;\n            TacticalBattle active=rooms[room],other=rooms[1-room];');p.write_text(s,encoding='utf-8')
p=b/'ShelterUpgrades.java';s=p.read_text(encoding='utf-8').replace('        return goal + "Сигнал «Маяк» записан.', '''        if(world!=null && world.getStories().archiveComplete)return goal+"Схема резервного прохода скопирована. Транспорт отменён, но сбор приказано сохранить.\\nБлижайшая цель: "+(world.getStories().archiveGoal==2?"станция скорой помощи":"военный склад")+". Обе зацепки остаются доступны.";
        return goal + "Сигнал «Маяк» записан.''');p.write_text(s,encoding='utf-8')
