from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'SurvivalSystems.java';s=p.read_text(encoding='utf-8').replace('    static void backpack(', '''    static boolean protectedRadioPart(Fighter p,ItemInfo item){return p.getUpgrades().radio==0 && (item.getName().equals("Электрочип") || item.getName().equals("Блок управления радиостанцией"));}
    static void backpack(''').replace('GameIO.item(i + 1, items.get(i), "Выбросить: ", true);', 'GameIO.item(i + 1, items.get(i), "Выбросить: ", !protectedRadioPart(fighter,items.get(i)));').replace('            GameIO.out().println("Оставлено: " + items.remove(n - 1).getName());', '            if(protectedRadioPart(fighter,items.get(n-1))){GameIO.out().println("Компонент нужен для связи. Его можно оставить на складе убежища.");continue;}\n            GameIO.out().println("Оставлено: " + items.remove(n - 1).getName());');p.write_text(s,encoding='utf-8')
p=b/'Shop.java';s=p.read_text(encoding='utf-8').replace('Economy.salePrice(clock(),items.get(i)) + " $ · " + items.get(i).physicalStats());', 'Economy.salePrice(clock(),items.get(i)) + " $ · " + items.get(i).physicalStats()+(SurvivalSystems.protectedRadioPart(fighter,items.get(i))?" · Нужен для радиостанции":""),!SurvivalSystems.protectedRadioPart(fighter,items.get(i)));').replace('            ItemInfo item = items.get(number - 1);\n            long updated', '            ItemInfo item = items.get(number - 1);\n            if(SurvivalSystems.protectedRadioPart(fighter,item)){GameIO.out().println("Этот компонент нужен для восстановления связи. Продажа доступна после ремонта радиостанции.");continue;}\n            long updated');p.write_text(s,encoding='utf-8')
p=b/'ShelterUpgrades.java';s=p.read_text(encoding='utf-8').replace('GameIO.menu("Нет сигнала",result==SignalResult.NO_SIGNAL_DAY?"Попробуй завтра":"Попробуйте через 12 часов");', 'GameIO.menu("Нет сигнала","Приёмник исправен. На частоте только помехи: передача может быть непостоянной. Чтобы беречь питание, Джон откладывает следующую длительную проверку.\\n"+(result==SignalResult.NO_SIGNAL_DAY?"Попробуй завтра — через 24 игровых часа":"Попробуйте через 12 игровых часов"));');p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/OpeningStoryTest.java');s=p.read_text(encoding='utf-8').replace('assertEquals(201,w.discover(3,"Остановка",r,true).id());', 'assertNull(w.discover(3,"Остановка",r,true));assertTrue(w.hasSeen(201));').replace('assertEquals(202,w.discover(5,"Подъезд",r,true).id());', 'assertNull(w.discover(5,"Подъезд",r,true));assertTrue(w.hasSeen(202));')
pos=s.rfind('}');s=s[:pos]+''' @Test public void uncollectedEquipmentStaysAtSourceAndNeverTeleportsHome() {
  WorldStories w=new WorldStories();Fighter p=new Fighter("John","",100);
  List<ItemInfo> first=w.guaranteedChips(9);assertEquals(1,first.size());
  w.collectEquipment(9,p,first,new Scanner("0"));assertTrue(p.getStorage().isEmpty());
  List<ItemInfo> again=w.guaranteedChips(9);assertEquals(1,again.size());
  w.collectEquipment(9,p,again,new Scanner("1"));assertTrue(p.getInventory().contains(again.get(0)));assertTrue(p.getStorage().isEmpty());
  assertTrue(w.guaranteedChips(9).isEmpty());assertTrue(w.guaranteedChips(35).isEmpty());
  assertTrue(SurvivalSystems.protectedRadioPart(p,again.get(0)));p.getUpgrades().radio=1;assertFalse(SurvivalSystems.protectedRadioPart(p,again.get(0)));
 }
 @Test public void earlyCluesStopWhenFullSignalIsRecorded() {
  WorldStories w=new WorldStories();w.recordRadioSignal();assertTrue(w.signalKnown());assertNull(w.signalClue(3,"Остановка"));
 }
''' +s[pos:];p.write_text(s,encoding='utf-8')
