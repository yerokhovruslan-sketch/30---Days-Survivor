from pathlib import Path
b=Path('app/src/main/java/com/example/survivor/game')
p=b/'WorldStories.java';s=p.read_text(encoding='utf-8').replace('    int absoluteMinute()', '    WorldStories(){this(new Random().nextLong());}\n    WorldStories(long seed){marketSeed=seed;}\n    int absoluteMinute()').replace('marketSeed=new Random().nextLong();eventDue=', 'if(marketSeed==0)marketSeed=new Random().nextLong();eventDue=')
s=s.replace('Основные товары ожидаются примерно через 2 игровых дня.', 'Ориентировочное ожидание: ещё "+Math.max(0,marketEventUntil-absoluteMinute())/60+" игровых ч.').replace('дополнительная партия патронов на 2 дня.', 'в текущей поставке больше патронов.')
p.write_text(s,encoding='utf-8')
p=b/'Item.java';s=p.read_text(encoding='utf-8').replace('GameIO.out().println("Цена: " + price);','GameIO.out().println("Оценочная стоимость: " + getPrice()+" $; продажа: "+getSalePrice()+" $");');p.write_text(s,encoding='utf-8')
p=b/'GameSession.java';s=p.read_text(encoding='utf-8').replace('if(minutes>0){clock().advanceMinutes(minutes);clock().refreshMarket();','''if(minutes>0){int before=clock().absoluteMinute();clock().advanceMinutes(minutes);
            if(player!=null){player.increaseHunger(clock().absoluteMinute()/60-before/60);player.increaseSleepiness(clock().absoluteMinute()/45-before/45);}
            clock().refreshMarket();''');p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/EconomyTest.java');s=p.read_text(encoding='utf-8').replace('WorldStories w=new WorldStories();\n        for(int i=0;i<300;i++)', 'WorldStories w=new WorldStories(73);w.refreshMarket();\n        for(int i=0;i<300;i++)').replace('contains("через 2 игровых дня")','contains("Ориентировочное ожидание")');p.write_text(s,encoding='utf-8')
