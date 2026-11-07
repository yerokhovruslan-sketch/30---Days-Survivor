from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'WorldStories.java';s=p.read_text(encoding='utf-8').replace('eventDue=absoluteMinute()+2880;', 'eventDue=(absoluteMinute()/2880+1)*2880;').replace('            marketEventId=r.nextInt(100)<23?1+r.nextInt(6):0;', '            int previous=marketEventId;marketEventId=r.nextInt(100)<23?1+r.nextInt(6):0;\n            if(previous==1 && marketEventId==1)marketEventId=0;')
s=s.replace('            if(!delayed)marketStock.put(n,Math.max(marketStock.getOrDefault(n,0),target));','''            if(n==16 && now<2880)target=2; // Modest guaranteed starter food.
            if(Economy.category(n)<=1) {
                int used=0;for(int other:marketStock.keySet())if(other!=n && Economy.category(other)==Economy.category(n))used+=marketStock.get(other);
                target=Math.min(target,Math.max(0,(Economy.category(n)==0?12:5)-used));
            }
            if(!delayed)marketStock.put(n,Math.max(marketStock.getOrDefault(n,0),target));''')
p.write_text(s,encoding='utf-8')
p=b/'Crafting.java';s=p.read_text(encoding='utf-8').replace('int budget=item.getSalePrice()/2;', 'int budget=item.getSalePrice();');p.write_text(s,encoding='utf-8')
p=b/'GameSession.java';s=p.read_text(encoding='utf-8').replace('foreground,manualPause,reading;', 'foreground,manualPause,reading,pendingNarrative;').replace('    void pauseReading(){reading=true;}', '    void pauseReading(){reading=true;}\n    void narrative(){pendingNarrative=true;}')
s=s.replace('                waiting = true;','                if(pendingNarrative){reading=true;pendingNarrative=false;}\n                waiting = true;')
s=s.replace('if(minutes>0){clock().advanceMinutes(minutes);publish(atMain?Mode.MAIN:Mode.CHOICE);if(clock().minuteOfDay()%5==0)checkpoint();}', '''if(minutes>0){clock().advanceMinutes(minutes);clock().refreshMarket();
            if(menu.title.equals("Покупка") && !menu.quantity){menu=new GameMenu.Builder("Покупка","Остатки и цены обновляются по игровым часам.");Shop.showCatalogue();}
            publish(atMain?Mode.MAIN:Mode.CHOICE);if(clock().minuteOfDay()%5==0)checkpoint();}''')
p.write_text(s,encoding='utf-8')
p=b/'GameIO.java';s=p.read_text(encoding='utf-8').replace('    static void storyMenu', '    static void narrative(){if(CURRENT.get()!=null)CURRENT.get().narrative();}\n    static void storyMenu');p.write_text(s,encoding='utf-8')
p=b/'WorldStories.java';s=p.read_text(encoding='utf-8');s=s.replace('static void print(Discovery discovery) {','static void print(Discovery discovery) {\n        GameIO.narrative();');p.write_text(s,encoding='utf-8')
p=b/'Survivor.java';s=p.read_text(encoding='utf-8').replace('                        if (destination.isDepleted()) {','''                        if (destination.isDepleted()) {
                            List<ItemInfo> recovered=locations.getStories().guaranteedChips(locations.getNumber(destination));
                            if(!recovered.isEmpty()){((Fighter)selectedSurvivor).getStorage().addAll(recovered);GameIO.out().println("Чип служебного оборудования отправлен на склад убежища.");GameIO.checkpoint();}
''',1);p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/ShopTest.java');s=p.read_text(encoding='utf-8').replace('balance == 100 && buyer.getInventory().size() == 2','balance == 90 && buyer.getInventory().size() == 2');p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/GameSessionTest.java');s=p.read_text(encoding='utf-8').replace('assertEquals(9, frame.balance);','assertEquals(1, frame.balance);');p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/CraftingTest.java');s=p.read_text(encoding='utf-8');a=s.index('        WorldStories w=new WorldStories();assertEquals(8,w.craftStock(54));');z=s.index('\n    }',a);s=s[:a]+'''        WorldStories w=new WorldStories();int powder=w.craftStock(54),kits=w.craftStock(59);
        assertTrue(powder>=0 && powder<=5);assertTrue(kits>=0 && kits<=3);
        w.buyCraftStock(54,powder);w.buyCraftStock(59,kits);assertEquals(0,w.craftStock(54));assertEquals(0,w.craftStock(59));
        w.advanceMinutes(60);assertEquals(0,w.craftStock(54));assertEquals(0,w.craftStock(59));
        w.advanceMinutes(2880);assertTrue(w.craftStock(54)<=5);
''' +s[z:]
s=s.replace('Locations world=new Locations();world.getStories().buyCraftStock(54,2);','Locations world=new Locations();int remaining=Math.max(0,world.getStories().craftStock(54)-2);world.getStories().buyCraftStock(54,2);').replace('assertEquals(6,save.locations.getStories().craftStock(54))','assertEquals(remaining,save.locations.getStories().craftStock(54))');p.write_text(s,encoding='utf-8')
