from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'TacticalBattle.java';s=p.read_text(encoding='utf-8').replace('this.selectedObstacle=cell;continue;', 'this.selectedObstacle=cell;return;').replace('if(moved && !won() || cell<0', 'if(moved && (!won() || fixedEncounter) || cell<0');p.write_text(s,encoding='utf-8')
p=b/'PoliceArchive.java';s=p.read_text(encoding='utf-8').replace('if(action==0 && attempt.room==0 && b.playerCell==ENTRY)return;', '''if(action==0 && attempt.room==0 && b.playerCell==ENTRY){GameIO.storyMenu("Покинуть архив?","Все враги и расстановка сбросятся. Потраченные патроны, здоровье и предметы не восстановятся.");GameIO.action(1,"Покинуть архив");GameIO.action(0,"Остаться");if(SurvivalSystems.choice(scanner)==1)return;continue;}''').replace('if(turn!=b.turn)attempt.pursue();', 'if(turn!=b.turn){attempt.pursue();GameIO.checkpoint();}');p.write_text(s,encoding='utf-8')
# Follow-ups remain usable even if the location was exhausted before the archive.
p=b/'Survivor.java';s=p.read_text(encoding='utf-8').replace('                            List<ItemInfo> recovered=', '''                            if(locations.getStories().archiveComplete && (locations.getNumber(destination)==13 || locations.getNumber(destination)==47)){
                                GameIO.storyMenu(destination.getName(),"Обычные запасы исчерпаны. Сюжетная зацепка остаётся доступной.");GameIO.action(1,locations.getNumber(destination)==13?"Проверить медицинский экипаж":"Проверить следы выезда и подвалы склада");GameIO.action(0,"Назад");
                                if(SurvivalSystems.choice(scanner)==1)PoliceArchive.followup(locations.getStories(),locations.getNumber(destination),scanner);
                            }
                            List<ItemInfo> recovered=''',1);p.write_text(s,encoding='utf-8')
