from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/game/Item.java');s=p.read_text(encoding='utf-8').replace('@Override public int getSalePrice() {return getPrice()/2;}', '@Override public int getSalePrice() {return getPrice()*40/100;}');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/game/Economy.java');s=p.read_text(encoding='utf-8').replace('WEAPONS.put(WeaponStyle.KATANA,160)', 'WEAPONS.put(WeaponStyle.KATANA,150)');p.write_text(s,encoding='utf-8')
p=Path('app/src/test/java/com/example/survivor/game/EconomyTest.java');s=p.read_text(encoding='utf-8').replace('assertEquals(Shop.createItem(30),null);','assertNull(Shop.createItem(30));');s=s.replace('            assertTrue(r.name,resale<r.cost(1)*80/100);','''            int discountedCost=0;
            for(Map.Entry<String,Integer> part:r.materials.entrySet()) {
                ItemInfo material=null;
                for(int n=1;n<=61;n++){ItemInfo candidate=Shop.createItem(n);if(candidate!=null && candidate.getName().equals(part.getKey())){material=candidate;break;}}
                assertNotNull(part.getKey(),material);
                discountedCost+=Math.max(1,material.getPrice()*80/100)*part.getValue();
            }
            assertTrue(r.name+" resale="+resale+" costs="+discountedCost,resale<discountedCost);''');p.write_text(s,encoding='utf-8')
