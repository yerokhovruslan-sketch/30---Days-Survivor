package com.example.survivor.game;

import java.util.*;

/** One valuation policy for found, purchased and crafted goods. */
final class Economy {
    private static String key(String name){return name.toLowerCase(Locale.ROOT).replace('ё','е');}
    private static final Map<String,Integer> PRICES=new HashMap<>();
    private static final Map<WeaponStyle,Integer> WEAPONS=new EnumMap<>(WeaponStyle.class);
    static {
        for(ItemInfo item:Item.lootCatalogue())PRICES.put(key(item.getName()),item.rawPrice());
        for(int n=1;n<=Shop.LAST_PRODUCT;n++) {
            ItemInfo item=Shop.createItem(n);if(item==null || item instanceof Ammunition)continue;
            PRICES.put(key(item.getName()),item.rawPrice());
            if(item instanceof Weapons)WEAPONS.put(((Weapons)item).getStyle(),item.rawPrice());
        }
        WEAPONS.put(WeaponStyle.AXE,40);WEAPONS.put(WeaponStyle.MACHETE,100);
        WEAPONS.put(WeaponStyle.FIRE_AXE,145);WEAPONS.put(WeaponStyle.KATANA,150);
    }
    static int base(ItemInfo item) {
        if(item instanceof Weapons && WEAPONS.containsKey(((Weapons)item).getStyle()))return WEAPONS.get(((Weapons)item).getStyle());
        return PRICES.getOrDefault(key(item.getName()),item.rawPrice());
    }
    static int sale(ItemInfo item) {
        int value=item.getPrice();
        return value*(item.isDurable()?20+item.getDurability()*30/100:50)/100;
    }
    static int category(int product) {
        ItemInfo i=Shop.createItem(product);if(i==null)return -1;
        if(product==49)return 6;
        if(i instanceof Ammunition)return 3;
        if(i instanceof Food)return 0;
        if(i instanceof MedicalItem)return 1;
        if(i instanceof Weapons)return ((Weapons)i).getStyle().firearm()?5:4;
        if(i.getArmorProtection()>0)return 5;
        if(i.isRepairTool() || product==47)return 4;
        return 2;
    }
    static int period(int product){return switch(category(product)){case 4->5760;case 5,6->8640;default->2880;};}
    static int target(int product,int day,Random random) {
        int category=category(product);
        int deficit=day>=22?15:day>=15?8:0;
        int chance=switch(category){case 0->90;case 1->85;case 2->80;case 3->55;case 4->60;case 5->25;case 6->20;default->0;};
        if(random.nextInt(100)>=Math.max(10,chance-deficit))return 0;
        return switch(category){case 0->1+random.nextInt(2);case 1->1+random.nextInt(2);case 2->product==54?2+random.nextInt(4):product==59?1+random.nextInt(3):product>=55&&product<=58?3+random.nextInt(10):2+random.nextInt(5);case 3->1;default->1;};
    }
    static int buyPrice(WorldStories world,int number) {
        ItemInfo item=Shop.createItem(number);int percent=100;
        if(world!=null) {
            world.refreshMarket();int event=world.marketEvent();int category=category(number);
            if(event==2 && category==1)percent=130;
            if(event==4 && (number==49 || number==43))percent=125;
            if(event==5 && category==world.saleCategory())percent=80;
            if(event==6 && category==0)percent=115;
        }
        return Math.max(1,item.getPrice()*percent/100);
    }
    static int salePrice(WorldStories world,ItemInfo item) {
        int value=item.getSalePrice();
        // A discount never creates a buy/sell loop; demand remains below 75% of base.
        if(world!=null && world.marketEvent()==6 && (item.isRepairTool() || item.getName().contains("инструмент")))
            value=Math.min(item.getPrice()*3/4,value*6/5);
        return value;
    }
}
