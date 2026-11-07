package com.example.survivor.game;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;


class ItemInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private int rarity;
    private String description;
    private int price;
    private int durability = 100;
    private boolean conditionInitialized = true;
    private void readObject(java.io.ObjectInputStream input) throws java.io.IOException, ClassNotFoundException {
        input.defaultReadObject();
        if (!conditionInitialized) { durability = 100; conditionInitialized = true; }
    }
    public boolean isDurable() { return this instanceof Weapons || getArmorProtection() > 0; }
    public int getDurability() { return durability; }
    public void wear(int amount) { if (isDurable()) durability = Math.max(0, durability - Math.max(0, amount)); }
    public void repair(int amount) { if (isDurable()) durability = Math.min(100, durability + Math.max(0, amount)); }
    public int getConditionPercent() { return durability == 0 ? 0 : 50 + durability / 2; }
    public int getArmorProtection() { return 0; }
    public boolean isRepairTool() { return false; }
    public boolean isRepairMaterial() { return false; }
    public int getWeightGrams() { return 500; }
    public int getSalePrice() {return Economy.sale(this);}
    public String physicalStats() {
        return String.format(java.util.Locale.ROOT, "Вес: %.2f кг", getWeightGrams() / 1000.0)
                + (isDurable() ? "; прочность: " + durability + "/100" : "");
    }



    ItemInfo(String name, String description, int rarity, int price) {
        this.name = name;
        this.description = description;
        this.rarity = rarity;
        this.price = price;


    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getRarity() {
        return rarity;
    }

    int rawPrice() {return price;}
    public int getPrice() {return Economy.base(this);}
    public void multiplyPrice(int multiplier) { price *= multiplier; }

    public void showResult() {
        GameIO.out().println("Предмет: " + name);
        GameIO.out().println("Описание: " + description);
        GameIO.out().println("Редкость: " + rarity);
        GameIO.out().println("Оценочная стоимость: " + getPrice()+" $; продажа: "+getSalePrice()+" $");
        GameIO.out().println(physicalStats());
    }

}
class Food extends ItemInfo {
    private static final long serialVersionUID = 1L;
    private final int nutrition;
    public Food(String name, String description, int rarity, int price) {
        this(name, description, rarity, price, Math.min(70, 10 + rarity * 10));
    }
    public Food(String name, String description, int rarity, int price, int nutrition) {
        super(name, description, rarity, price);
        this.nutrition = Math.max(1, nutrition);
    }   
    public int getNutrition() { return nutrition; }
    @Override
        public void showResult() {
            GameIO.out().println("Еда: " + getName());
            GameIO.out().println("Описание: " + getDescription());
            GameIO.out().println("Редкость: " + getRarity());
            GameIO.out().println("Утоляет голод: " + nutrition);
        GameIO.out().println("Цена: " + getPrice());
        GameIO.out().println(physicalStats());
        }
}


class Tools extends ItemInfo{
    private static final long serialVersionUID = 1L;
    public Tools(String name, String description, int rarity, int price)
    {
        super(name, description, rarity, price);
    }
    @Override public int getArmorProtection() {
        String name = getName().toLowerCase(java.util.Locale.ROOT);
        if (name.contains("бронежилет") || name.contains("защитный жилет")) return 40;
        if (name.contains("шлем")) return 20;
        if (name.contains("бронепластина")) return 25;
        return 0;
    }
    @Override public boolean isRepairTool() {
        return getName().matches(".*(Набор инструментов|Набор отверток|Набор гаечных ключей|Плоскогубцы|Набор швейных принадлежностей|Набор шестигранников|Набор надфилей).*" );
    }
    @Override public boolean isRepairMaterial() {
        return getName().matches(".*(Запчасти|Металлические пластины|Медная проволока|Изолента).*" );
    }
    @Override public int getWeightGrams() {
        if(Crafting.isComponent(getName()))return Crafting.componentWeight(getName());
        if (getName().equals(PoliceArchive.KEY)) return 10;
        if (getName().equals("Электрочип")) return 50;
        if (getArmorProtection() >= 25) return 6000;
        if (getArmorProtection() > 0) return 1500;
        return isRepairMaterial() ? 500 : 1000;
    }
    @Override
        public void showResult() {
            GameIO.out().println("Инструмент: " + getName());
            GameIO.out().println("Описание: " + getDescription());
            GameIO.out().println("Редкость: " + getRarity());
            GameIO.out().println("Цена: " + getPrice());
            GameIO.out().println(physicalStats());
            if (getArmorProtection() > 0) GameIO.out().println("Защита при полной прочности: " + getArmorProtection() + "%");
            if (isRepairTool()) GameIO.out().println("Подходит для ремонта; инструмент не расходуется.");
            if (isRepairMaterial()) GameIO.out().println("Материал для ремонта: +30 прочности, расходуется целиком.");
        }
}
enum WeaponStyle {
    FISTS("Без оружия", 5, 9, 6, 1, 80, 0, 0),
    PIPE("20% оглушения после попадания", 14, 22, 15, 1, 85, 20, 0),
    KNIFE("Два быстрых удара", 6, 10, 8, 2, 90, 0, 0),
    AXE("25% кровотечения / повреждения подвижности", 24, 36, 24, 1, 80, 0, 0),
    LONG("35% удержания; в соседней клетке урон −25%", 12, 19, 13, 1, 85, 0, 35),
    BLADE("Рубящий удар", 15, 23, 14, 1, 90, 0, 0),
    CLUB("20% оглушения после попадания", 10, 16, 10, 1, 90, 20, 0),
    HAMMER("30% оглушения после попадания", 12, 19, 14, 1, 85, 30, 0),
    SCREWDRIVER("Два быстрых укола", 5, 8, 9, 2, 85, 0, 0),
    WRENCH("25% оглушения после попадания", 13, 20, 15, 1, 85, 25, 0),
    SHOVEL("30% удержания после попадания", 13, 21, 17, 1, 80, 0, 30),
    CROWBAR("30% оглушения после попадания", 19, 28, 21, 1, 80, 30, 0),
    HATCHET("15% кровотечения / повреждения подвижности", 17, 26, 17, 1, 85, 0, 0),
    SAW("Тяжёлый режущий удар", 15, 25, 18, 1, 70, 0, 0),
    SHIELD("50% оглушения после попадания", 6, 11, 12, 1, 90, 50, 0),
    SLEDGEHAMMER("45% оглушения; разрушает лёгкие и часть тяжёлых преград", 35, 49, 32, 1, 75, 45, 0),
    FIRE_AXE("25% кровотечения / повреждения подвижности; рубит лёгкие преграды", 31, 44, 28, 1, 80, 0, 0),
    HUNTING_KNIFE("Два удара; 20% кровотечения / повреждения подвижности; хуже против брони", 8, 12, 11, 2, 90, 0, 0),
    MACHETE("30% кровотечения / повреждения подвижности", 23, 34, 19, 1, 90, 0, 0),
    SHOTGUN("Урон падает с расстоянием", 55, 80, 0, 1, 80, 0, 0),
    REVOLVER("Выстрел без расхода сил; можно прицелиться", 38, 52, 0, 1, 85, 0, 0),
    CROSSBOW("Один болт; ручное взведение", 35, 50, 10, 1, 85, 0, 0),
    SLINGSHOT("Один снаряд", 8, 15, 7, 1, 80, 0, 0),
    PISTOL("Выстрел без расхода сил; можно прицелиться", 32, 45, 0, 1, 85, 0, 0),
    CARBINE("Выстрел без расхода сил; можно прицелиться", 48, 68, 0, 1, 85, 0, 0),
    KATANA("30% кровотечения / повреждения подвижности",25,38,22,1,90,0,0),
    UNUSABLE("Пока нельзя использовать в бою", 0, 0, 0, 0, 0, 0, 0);

    final String effect;
    final int minDamage, maxDamage, staminaCost, strikes, accuracy, stunChance, distanceChance;

    WeaponStyle(String effect, int minDamage, int maxDamage, int staminaCost,
                int strikes, int accuracy, int stunChance, int distanceChance) {
        this.effect = effect;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.staminaCost = staminaCost;
        this.strikes = strikes;
        this.accuracy = accuracy;
        this.stunChance = stunChance;
        this.distanceChance = distanceChance;
    }
    int bleedChance() {return switch(this){case HUNTING_KNIFE->20;case HATCHET->15;case AXE,FIRE_AXE->25;case MACHETE,KATANA->30;default->0;};}
    boolean firearm() {return this==REVOLVER || this==PISTOL || this==SHOTGUN || this==CARBINE;}
    int noise() {return switch(this){case KNIFE,HUNTING_KNIFE,SCREWDRIVER->1;case CLUB->3;case PIPE->5;case SLEDGEHAMMER->8;case CROSSBOW->4;case PISTOL->20;case REVOLVER->25;case CARBINE->35;case SHOTGUN->45;case FISTS,UNUSABLE->0;default->4;};}
    int capacity() {return switch(this){case REVOLVER->6;case PISTOL->12;case SHOTGUN->2;case CARBINE->5;default->1;};}
    int range() {return switch(this){case LONG,SHOVEL->2;case SHOTGUN->4;case CARBINE->8;case REVOLVER,PISTOL,CROSSBOW,SLINGSHOT->6;default->1;};}
}

class Weapons extends ItemInfo{
    private static final long serialVersionUID = 1L;
    private final WeaponStyle style;
    private boolean crafted;
    void markCrafted() {crafted=true;}
    @Override public int getSalePrice() {return Economy.sale(this);}
    public Weapons(String name, String description, int rarity, int price){
        this(name, description, rarity, price, WeaponStyle.UNUSABLE);
    }
    public Weapons(String name, String description, int rarity, int price, WeaponStyle style){
        super(name, description, rarity, price);
        this.style = style;
    }
    public WeaponStyle getStyle() { return style; }
    public boolean canEquip() { return style != WeaponStyle.UNUSABLE; }
    @Override public boolean isRepairTool() {
        return style == WeaponStyle.HAMMER || style == WeaponStyle.SCREWDRIVER || style == WeaponStyle.WRENCH;
    }
    @Override public int getWeightGrams() {
        return switch (style) {
            case KNIFE, SCREWDRIVER, HUNTING_KNIFE, SLINGSHOT -> 400;
            case REVOLVER, PISTOL, HAMMER, WRENCH -> 1200;
            case SLEDGEHAMMER -> 7000;
            case SHIELD -> 6000;
            case SHOTGUN, CARBINE, CROSSBOW, FIRE_AXE -> 3500;
            default -> 2000;
        };
    }
    public boolean isLoud() { return style.firearm(); }
    public AmmoType getAmmoType() {
        switch (style) {
            case SHOTGUN: return AmmoType.SHELL;
            case REVOLVER: return AmmoType.REVOLVER;
            case PISTOL: return AmmoType.PISTOL;
            case CARBINE: return AmmoType.RIFLE;
            case CROSSBOW: return AmmoType.BOLT;
            case SLINGSHOT: return AmmoType.PELLET;
            default: return null;
        }
    }
    public String combatStats() {
        if (!canEquip()) return style.effect;
        return "Урон: " + style.minDamage + "–" + style.maxDamage + " × " + style.strikes
                + "; выносливость: " + style.staminaCost + " за ход; попадание: " + style.accuracy
                + "%. " + style.effect
                + (getAmmoType() == null ? "" : "; боеприпасы: " + getAmmoType().label + ", 1 за выстрел")
                + "; дальность: " + style.range() + "; шум: +" + style.noise()
                + (getAmmoType()==null?"":"; заряжено до "+style.capacity());
    }
    @Override
        public void showResult() {
            GameIO.out().println("Оружие: " + getName());
            GameIO.out().println("Описание: " + getDescription());
            GameIO.out().println("Редкость: " + getRarity());
            GameIO.out().println("Цена: " + getPrice());
            GameIO.out().println(combatStats());
            GameIO.out().println(physicalStats());
            GameIO.out().println("Урон с учётом состояния: " + getConditionPercent() + "% от базового");
        }
    }




enum AmmoType {
    SHELL("Дробовой патрон", 3, 144, 6),
    REVOLVER("Револьверный патрон", 3, 96, 6),
    BOLT("Арбалетный болт", 2, 45, 5),
    PELLET("Шарик для рогатки", 1, 6, 12),
    PISTOL("Пистолетный патрон",3,132,12),
    RIFLE("Карабинный патрон",4,130,5);

    final String label;
    final int rarity, price, packSize;
    AmmoType(String label, int rarity, int price, int packSize) {
        this.label = label;
        this.rarity = rarity;
        this.price = price;
        this.packSize = packSize;
    }
}

class Ammunition extends ItemInfo {
    private static final long serialVersionUID = 1L;
    private final AmmoType type;
    private int quantity;
    public Ammunition(AmmoType type) {
        this(type,type.packSize);
    }
    Ammunition(AmmoType type,int count) {
        super(type.label, "Боеприпасы: " + type.packSize + " шт.; расход — 1 за выстрел", type.rarity, type.price);
        this.type = type;
        if(count<1 || count>type.packSize)throw new IllegalArgumentException("Invalid ammunition batch");
        quantity = count;
    }
    public AmmoType getType() { return type; }
    public int getQuantity() { return quantity; }
    @Override public int getWeightGrams() { return quantity * switch(type) {case SHELL->45;case BOLT->30;case REVOLVER,PISTOL->15;case RIFLE->25;case PELLET->5;}; }
    public boolean consume() {
        if (quantity == 0) return false;
        quantity--;
        return true;
    }
    @Override public int getPrice() { return type.price * quantity / type.packSize; }
    @Override public int getSalePrice() {return getPrice()*40/100;}
    @Override public String getDescription() { return type.label + ": осталось " + quantity + " шт.; расход — 1 за выстрел"; }
}

class Item{
     public static ItemInfo createItems() {
         return createItems(1);
     }

     public static ItemInfo createItems(int difficulty) {
         return createItems(difficulty, LootProfile.GENERAL, ThreadLocalRandom.current());
     }

     public static ItemInfo createItems(Location location) {
         return createItems(location, ThreadLocalRandom.current());
     }

     public static ItemInfo createItems(Location location, java.util.Random random) {
         return createItems(location, random, 0);
     }
     public static ItemInfo createItems(Location location, java.util.Random random, int bonus) {
         if (!location.searchSupplies(random, bonus+Difficulty.loot(GameIO.difficulty()))) {
             GameIO.out().println("Пустые полки и вскрытые ящики. Ничего полезного не найдено.");
             GameIO.out().println("Запасы: " + location.getSuppliesStatus());
             return null;
         }
         ItemInfo item = createLootForLocation(location, random);
         GameIO.out().println("Запасы: " + location.getSuppliesStatus());
         return item;
     }

     static boolean matchesProfile(ItemInfo item, LootProfile profile) {
         return switch (profile) {
             case FOOD -> item instanceof Food;
             case MEDICAL -> item instanceof MedicalItem;
             case ARMS -> item instanceof Ammunition || item.getArmorProtection() > 0 || item instanceof Weapons && ((Weapons) item).getAmmoType() != null
                     || Crafting.isComponent(item.getName()) && !item.getName().equals(Crafting.METAL) && !item.getName().equals(Crafting.WOOD);
             case WORKSHOP -> item instanceof Tools || item instanceof Weapons && ((Weapons) item).getAmmoType() == null;
             default -> true;
         };
     }

     static List<ItemInfo> lootCatalogue() {
        List<ItemInfo> items = new ArrayList<>();
        for (AmmoType type : AmmoType.values()) items.add(new Ammunition(type));
        items.add(Crafting.component(Crafting.POWDER));items.add(Crafting.component(Crafting.KIT));
        items.add(Crafting.component(Crafting.METAL));items.add(Crafting.component(Crafting.WOOD));
        for(AmmoType type:AmmoType.values())if(!Crafting.casing(type).isEmpty())items.add(Crafting.component(Crafting.casing(type)));
        items.add(new Tools("Запчасти", "Детали для ремонта оружия и брони", 1, 12));
        items.add(new Tools("Плотная ткань", "Материал для улучшения кровати в убежище", 1, 8));
        items.add(new Tools("Набор инструментов", "Для ремонта нужен этот набор или подходящий ручной инструмент", 2, 35));


         // ===== ЕДА И НАПИТКИ =====
         items.add(new Food("Фасоль в банке", "Консервированная фасоль с долгим сроком хранения", 1, 2));
         items.add(new Food("Тушенка", "Мясные консервы для восстановления сил", 2, 8));
         items.add(new Food("Пачка галет", "Сухое печенье для длительных походов", 1, 3));
         items.add(new Food("Овсяные хлопья", "Питательный сухой завтрак", 1, 4));
         items.add(new Food("Рис", "Пакет риса для приготовления еды", 1, 4));
         items.add(new Food("Гречка", "Пакет гречневой крупы", 1, 5));
         items.add(new Food("Макароны", "Упаковка сухих макарон", 1, 3));
         items.add(new Food("Мука", "Пакет муки для приготовления пищи", 1, 4));
         items.add(new Food("Соль", "Небольшой пакет поваренной соли", 1, 2));
         items.add(new Food("Сахар", "Пакет сахара для приготовления пищи", 1, 4));
         items.add(new Food("Мед", "Банка натурального меда", 2, 12));
         items.add(new Food("Арахисовая паста", "Калорийная паста для быстрого восполнения энергии", 2, 10));
         items.add(new Food("Шоколад", "Плитка шоколада для быстрого перекуса", 1, 5));
         items.add(new Food("Энергетический батончик", "Компактный высококалорийный перекус", 1, 6));
         items.add(new Food("Пакет орехов", "Смесь орехов для восполнения энергии", 2, 8));
         items.add(new Food("Пакет чечевицы", "Сухая чечевица для приготовления пищи", 1, 4));
         items.add(new Food("Сгущенное молоко", "Банка сладкого концентрированного молока", 2, 9));
         items.add(new Food("Сухое молоко", "Порошковое молоко для приготовления напитков", 2, 10));
         items.add(new Food("Пакет сухарей", "Сухари для длительного хранения", 1, 3));
         items.add(new Food("Детское питание", "Баночка питательного детского пюре", 2, 12));
         items.add(new Food("Растворимый кофе", "Пакет кофе для приготовления горячего напитка", 1, 5));
         items.add(new Food("Чай в пакетиках", "Упаковка чая для горячих напитков", 1, 3));
         items.add(new Food("Фляга с водой", "Запас питьевой воды в металлической фляге", 2, 10));
         items.add(new Food("Фруктовый компот", "Банка сладкого фруктового компота", 1, 4));

         items.add(new Food("Консервированная кукуруза", "Жестяная банка сладкой кукурузы", 1, 4));
         items.add(new Food("Консервированный горошек", "Банка зелёного горошка длительного хранения", 1, 4));
         items.add(new Food("Консервированные персики", "Фрукты в сладком сиропе", 2, 9));
         items.add(new Food("Консервированные ананасы", "Кольца ананаса в сиропе", 2, 10));
         items.add(new Food("Рыбные консервы", "Банка рыбы в собственном соку", 1, 7));
         items.add(new Food("Сардины в масле", "Небольшая банка сардин", 1, 6));
         items.add(new Food("Консервированный паштет", "Мясной паштет длительного хранения", 1, 5));
         items.add(new Food("Суп быстрого приготовления", "Пакет сухой суповой смеси", 1, 3));
         items.add(new Food("Лапша быстрого приготовления", "Сухая лапша со специями", 1, 3));
         items.add(new Food("Картофельное пюре быстрого приготовления", "Сухие картофельные хлопья", 1, 4));
         items.add(new Food("Кукурузные хлопья", "Коробка сухих хлопьев для завтрака", 1, 5));
         items.add(new Food("Крекеры", "Упаковка солёных крекеров", 1, 3));
         items.add(new Food("Сушёные яблоки", "Пакет сушёных фруктов", 1, 5));
         items.add(new Food("Изюм", "Небольшая упаковка сушёного винограда", 1, 5));
         items.add(new Food("Курага", "Пакет сушёных абрикосов", 2, 8));
         items.add(new Food("Финики", "Питательные сушёные плоды", 2, 9));
         items.add(new Food("Протеиновый батончик", "Высокобелковый перекус в герметичной упаковке", 2, 10));
         items.add(new Food("Сублимированное мясо", "Обезвоженное мясо для длительных походов", 3, 30));
         items.add(new Food("Сублимированные овощи", "Овощная смесь с длительным сроком хранения", 3, 25));
         items.add(new Food("Армейский сухпаёк", "Комплект продуктов для одного человека", 3, 45));
         items.add(new Food("Аварийный пищевой рацион", "Высококалорийный рацион в герметичной упаковке", 4, 80));
         items.add(new Food("Бутылка минеральной воды", "Запечатанная бутылка питьевой воды", 1, 4));
         items.add(new Food("Пакет фруктового сока", "Сок в герметичной картонной упаковке", 1, 5));
         items.add(new Food("Порошковый изотоник", "Сухая смесь для приготовления напитка", 2, 12));
         items.add(new Food("Растворимый какао", "Сладкий порошок для горячего напитка", 1, 6));


         // = МЕДИЦИНА И ПЕРВАЯ ПОМОЩЬ =
         items.add(new MedicalItem("Бинты", 15, 20));
         items.add(new Tools("Марлевые салфетки", "Стерильные салфетки для обработки ран", 1, 4));
         items.add(new Tools("Антисептик", "Средство для обработки поврежденной кожи", 2, 12));
         items.add(new Tools("Медицинские перчатки", "Пара одноразовых перчаток для оказания помощи", 1, 5));
         items.add(new Tools("Медицинские ножницы", "Ножницы для разрезания бинтов и одежды", 2, 10));
         items.add(new Tools("Эластичный бинт", "Эластичный бинт для фиксации поврежденных конечностей", 2, 9));
         items.add(new Tools("Холодный компресс", "Пакет для временного охлаждения ушибов", 2, 8));
         items.add(new Tools("Медицинский жгут", "Средство для экстренной остановки сильного кровотечения", 3, 25));
         items.add(new Tools("Термометр", "Прибор для измерения температуры тела", 1, 10));
         items.add(new Tools("Пинцет", "Инструмент для извлечения мелких инородных предметов", 1, 6));
         items.add(new Tools("Медицинская маска", "Маска для защиты дыхательных путей от пыли", 1, 3));
         items.add(new Tools("Респиратор", "Средство защиты от пыли и некоторых аэрозолей", 3, 30));
         items.add(new Tools("Шина для конечности", "Складная шина для фиксации травмированной руки или ноги", 3, 25));
         items.add(new Tools("Медицинский набор", "Комплект базовых принадлежностей для оказания первой помощи", 3, 50));
         items.add(new Tools("Складные носилки", "Компактные носилки для транспортировки пострадавших", 4, 100));

         items.add(new Tools("Кровоостанавливающий бинт", "Специальная повязка для тяжёлых кровотечений", 3, 35));
         items.add(new Tools("Стерильная повязка", "Упакованная повязка для закрытия ран", 1, 7));
         items.add(new Tools("Набор пластырей", "Коробка медицинских пластырей", 1, 6));
         items.add(new Tools("Медицинский скотч", "Лента для фиксации повязок", 1, 5));
         items.add(new Tools("Физиологический раствор", "Стерильный раствор для промывания", 2, 12));
         items.add(new Tools("Промывочная бутылка", "Ёмкость для очистки загрязнённых ран", 2, 10));
         items.add(new Tools("Шприц одноразовый", "Стерильный медицинский шприц", 1, 4));
         items.add(new Tools("Пульсоксиметр", "Прибор для измерения насыщения крови кислородом", 3, 45));
         items.add(new Tools("Тонометр", "Прибор для измерения артериального давления", 2, 30));
         items.add(new Tools("Стетоскоп", "Прибор для прослушивания сердца и дыхания", 2, 25));
         items.add(new Tools("Медицинский фонарик", "Компактный фонарь для осмотра пациентов", 2, 15));
         items.add(new Tools("Термопокрывало", "Компактное покрывало для сохранения тепла", 2, 12));
         items.add(new Tools("Медицинская сумка", "Сумка с отделениями для лекарств и инструментов", 3, 55));
         items.add(new Tools("Перевязочный комплект", "Комплект стерильных материалов для обработки ран", 3, 40));
         items.add(new Tools("Хирургический набор", "Набор инструментов для оснащения медицинского пункта", 4, 130));
         items.add(new Tools("Портативный дефибриллятор", "Медицинский прибор экстренной помощи", 5, 450));
         items.add(new Tools("Складная медицинская кушетка", "Переносная кушетка для медицинского помещения", 4, 110));
         items.add(new Tools("Контейнер для медикаментов", "Жёсткий ящик для безопасной транспортировки лекарств", 2, 20));
         items.add(new Tools("Аптечный холодильник", "Холодильное оборудование для хранения медикаментов", 5, 320));
         items.add(new Tools("Ультрафиолетовый стерилизатор", "Устройство для обработки совместимых инструментов", 4, 160));



         // == ИНСТРУМЕНТЫ И СНАРЯЖЕНИЕ =
         items.add(new Weapons("Молоток", "Инструмент и короткое ударное оружие", 1, 10, WeaponStyle.HAMMER));
         items.add(new Weapons("Отвертка", "Инструмент, которым можно наносить быстрые уколы", 1, 6, WeaponStyle.SCREWDRIVER));
         items.add(new Tools("Набор отверток", "Набор отверток разных размеров", 2, 18));
         items.add(new Tools("Плоскогубцы", "Инструмент для захвата и сгибания деталей", 1, 10));
         items.add(new Weapons("Разводной ключ", "Тяжёлый инструмент пригоден для ударов", 2, 15, WeaponStyle.WRENCH));
         items.add(new Tools("Набор гаечных ключей", "Комплект ключей для ремонта оборудования", 2, 25));
         items.add(new Weapons("Ножовка", "Пила: неудобна в бою, но наносит режущие удары", 2, 18, WeaponStyle.SAW));
         items.add(new Tools("Ручной бур", "Инструмент для создания отверстий в грунте и древесине", 3, 35));
         items.add(new Tools("Кусачки", "Инструмент для перекусывания проволоки", 2, 15));
         items.add(new Tools("Изолента", "Рулон изоляционной ленты для временного ремонта", 1, 4));
         items.add(new Tools("Клейкая лента", "Прочная лента для ремонта снаряжения", 1, 5));
         items.add(new Tools("Набор швейных принадлежностей", "Иглы, нитки и пуговицы для ремонта одежды", 1, 8));
         items.add(new Tools("Швейный набор для ремонта обуви", "Инструменты и материалы для починки обуви", 2, 18));
         items.add(new Weapons("Складная лопата", "Лопата помогает удерживать противника на расстоянии", 2, 25, WeaponStyle.SHOVEL));
         items.add(new Weapons("Металлический лом", "Прочный лом для демонтажа и тяжёлых ударов", 2, 20, WeaponStyle.CROWBAR));
         items.add(new Weapons("Топорик для рубки", "Небольшой топор для заготовки древесины", 2, 25, WeaponStyle.HATCHET));
         items.add(new Weapons("Пила по металлу", "Пила: режущие удары требуют много сил", 3, 30, WeaponStyle.SAW));
         items.add(new Tools("Рулетка", "Измерительная лента для строительных работ", 1, 8));
         items.add(new Tools("Строительный уровень", "Инструмент для проверки горизонтальности поверхностей", 2, 20));
         items.add(new Tools("Набор сверл", "Сверла разных диаметров для дрели", 2, 20));
         items.add(new Tools("Ручная дрель", "Механическая дрель для сверления отверстий", 3, 40));
         items.add(new Tools("Карабин", "Металлический карабин для крепления снаряжения", 1, 8));
         items.add(new Tools("Веревочная лестница", "Компактная лестница для подъема и спуска", 3, 45));
         items.add(new Tools("Тактический рюкзак", "Прочный рюкзак с отделениями для снаряжения", 3, 60));
         items.add(new Tools("Спальный мешок", "Утепленный мешок для ночлега в холодных условиях", 2, 35));
         items.add(new Tools("Палатка", "Легкая палатка для временного укрытия", 3, 70));

         items.add(new Tools("Болторез", "Мощный инструмент для перекусывания металлических прутьев", 3, 65));
         items.add(new Tools("Трубный ключ", "Крупный ключ для работы с трубными соединениями", 2, 25));
         items.add(new Tools("Набор шестигранников", "Комплект ключей разных размеров", 1, 12));
         items.add(new Tools("Торцевой набор", "Головки и трещотка для ремонта механизмов", 3, 45));
         items.add(new Tools("Динамометрический ключ", "Ключ для контролируемой затяжки крепежа", 3, 60));
         items.add(new Tools("Струбцина", "Металлическое приспособление для фиксации деталей", 2, 18));
         items.add(new Tools("Слесарные тиски", "Настольное устройство для надёжного зажима деталей", 3, 55));
         items.add(new Tools("Напильник", "Инструмент для обработки металлических поверхностей", 1, 8));
         items.add(new Tools("Набор надфилей", "Маленькие напильники для точной обработки", 2, 16));
         items.add(new Tools("Наждачная бумага", "Листы абразивного материала", 1, 3));
         items.add(new Tools("Точильный камень", "Камень для обслуживания режущих инструментов", 2, 15));
         items.add(new Tools("Маслёнка", "Небольшая ёмкость для смазки механизмов", 1, 8));
         items.add(new Tools("Смазка для подшипников", "Густой состав для обслуживания механизмов", 2, 12));
         items.add(new Tools("Домкрат", "Механическое устройство для подъёма тяжёлых объектов", 3, 60));
         items.add(new Tools("Буксировочный трос", "Прочный трос для перемещения техники", 2, 30));
         items.add(new Tools("Складной верстак", "Переносной стол для ремонтных работ", 4, 110));
         items.add(new Tools("Электрическая дрель", "Электроинструмент для сверления материалов", 3, 70));
         items.add(new Tools("Аккумуляторный шуруповёрт", "Переносной инструмент для сборки конструкций", 3, 65));
         items.add(new Tools("Угловая шлифмашина", "Электроинструмент для резки и шлифования", 4, 95));
         items.add(new Tools("Электрический лобзик", "Инструмент для фигурной резки древесины", 3, 75));
         items.add(new Tools("Паяльник", "Инструмент для соединения электронных компонентов", 2, 25));
         items.add(new Tools("Катушка припоя", "Расходный материал для пайки", 1, 10));
         items.add(new Tools("Мультиметр", "Прибор для проверки электрических параметров", 3, 55));
         items.add(new Tools("Тестер кабелей", "Прибор для диагностики кабельных соединений", 3, 45));
         items.add(new Tools("Сварочный аппарат", "Оборудование для соединения металлических деталей", 5, 300));



         // = ЭЛЕКТРОНИКА И ЭНЕРГИЯ =
         items.add(new Tools("Батарейки AA", "Комплект пальчиковых батареек", 1, 6));
         items.add(new Tools("Батарейки AAA", "Комплект мизинчиковых батареек", 1, 6));
         items.add(new Tools("Аккумулятор 18650", "Перезаряжаемый литий-ионный элемент питания", 3, 25));
         items.add(new Tools("Пауэрбанк", "Переносной аккумулятор для зарядки устройств", 3, 60));
         items.add(new Tools("Солнечная панель", "Компактная панель для выработки электричества от солнца", 5, 250));
         items.add(new Tools("Ручной генератор", "Компактный генератор с ручным приводом", 4, 120));
         items.add(new Tools("Зарядное устройство", "Устройство для зарядки совместимых аккумуляторов", 2, 20));
         items.add(new Tools("Кабель USB", "Кабель для зарядки и передачи данных", 1, 5));
         // = МАТЕРИАЛЫ И РЕСУРСЫ =
         items.add(new Tools("Деревянные доски", "Доски для укрепления укрытия и ремонта мебели", 1, 5));
         items.add(new Tools("Металлические пластины", "Пластины для изготовления и ремонта конструкций", 3, 20));
         items.add(new Tools("Гвозди", "Коробка строительных гвоздей", 1, 4));
         items.add(new Tools("Шурупы", "Упаковка шурупов для сборки конструкций", 1, 5));
         items.add(new Tools("Болты и гайки", "Комплект крепежных элементов", 1, 6));
         items.add(new Tools("Медная проволока", "Проволока для ремонта и самодельных устройств", 2, 12));
         items.add(new Weapons("Стальная труба", "Кусок трубы для ремонта или тяжёлых ударов", 2, 15, WeaponStyle.PIPE));
         items.add(new Tools("Кусок брезента", "Прочная ткань для укрытия и защиты от дождя", 2, 15));
         items.add(new Tools("Пластиковая пленка", "Пленка для изоляции и временного укрытия", 1, 5));
         items.add(new Tools("Рулон сетки", "Металлическая сетка для ограждений и ремонта", 3, 25));


         // =ОРУЖИЕ И ЗАЩИТА =


         items.add(new Weapons("Гаечный ключ с длинной рукоятью", "Увесистый ключ для мощных ударов", 2, 22, WeaponStyle.WRENCH));
         items.add(new Weapons("Столярный молоток", "Прочный молоток с деревянной рукоятью", 1, 12, WeaponStyle.HAMMER));
         items.add(new Weapons("Кувалда", "Очень тяжёлое оружие с огромным расходом выносливости", 3, 55, WeaponStyle.SLEDGEHAMMER));
         items.add(new Weapons("Монтажная монтировка", "Компактный лом для вскрытия и защиты", 2, 23, WeaponStyle.CROWBAR));
         items.add(new Weapons("Садовый топор", "Топор с удлинённой рукоятью", 3, 45, WeaponStyle.AXE));
         items.add(new Weapons("Пожарный топор", "Тяжёлый топор из пожарного инвентаря", 4, 90, WeaponStyle.FIRE_AXE));
         items.add(new Weapons("Кухонный нож", "Обычный кухонный нож для ближнего боя", 1, 8, WeaponStyle.KNIFE));
         items.add(new Weapons("Складной нож", "Компактный нож с фиксатором лезвия", 2, 28, WeaponStyle.KNIFE));
         items.add(new Weapons("Тесак мясника", "Тяжёлое лезвие для рубящих ударов", 3, 45, WeaponStyle.BLADE));
         items.add(new Weapons("Садовая лопата", "Длинная лопата для защиты и отталкивания", 2, 20, WeaponStyle.SHOVEL));
         items.add(new Weapons("Деревянная бита", "Простое ударное оружие из дерева", 1, 12, WeaponStyle.CLUB));
         items.add(new Weapons("Алюминиевая бита", "Лёгкая спортивная бита", 2, 28, WeaponStyle.CLUB));
         items.add(new Weapons("Металлический прут", "Прочный стальной прут для ударов", 2, 14, WeaponStyle.PIPE));
         items.add(new Weapons("Длинный шест", "Древко для удержания противника на расстоянии", 1, 10, WeaponStyle.LONG));
         items.add(new Weapons("Усиленный защитный щит", "Тяжёлый щит для блокирования и отталкивания", 4, 120, WeaponStyle.SHIELD));

         items.add(new Weapons("Охотничье ружье", "Старое охотничье оружие", 5, 400, WeaponStyle.SHOTGUN));
         items.add(new Weapons("Револьвер", "Компактное огнестрельное оружие", 5, 350, WeaponStyle.REVOLVER));
         items.add(new Weapons("Пистолет", "Магазин на 12 патронов", 4, 300, WeaponStyle.PISTOL));
         items.add(new Weapons("Охотничий карабин", "Магазин на 5 патронов", 5, 600, WeaponStyle.CARBINE));
         items.add(new Weapons("Арбалет", "Метательное оружие для бесшумной охоты", 4, 180, WeaponStyle.CROSSBOW));
         items.add(new Weapons("Охотничий нож", "Прочный нож для полевых задач", 3, 50, WeaponStyle.HUNTING_KNIFE));
         items.add(new Weapons("Мачете", "Длинный тяжелый нож для расчистки зарослей", 4, 100, WeaponStyle.MACHETE));
         items.add(new Weapons("Метательная рогатка", "Простое оружие для стрельбы небольшими снарядами", 2, 20, WeaponStyle.SLINGSHOT));
         items.add(new Weapons("Деревянная дубинка", "Простое оружие ближнего боя", 1, 8, WeaponStyle.CLUB));
         items.add(new Weapons("Металлическая труба", "Тяжелый предмет для защиты в ближнем бою", 2, 15, WeaponStyle.PIPE));
         items.add(new Weapons("Топор", "Мощные удары требуют много сил", 2, 40, WeaponStyle.AXE));
         items.add(new Weapons("Копьё", "Длинное древко помогает держать дистанцию", 2, 30, WeaponStyle.LONG));
         items.add(new Tools("Боевой шлем", "Защитный шлем для головы; защищает после экипировки в пункте 15", 4, 100));
         items.add(new Tools("Защитный жилет", "Защитный жилет; защищает после экипировки в пункте 15", 5, 300));
         items.add(new Tools("Кожаные перчатки", "Прочные перчатки для защиты рук", 1, 10));
         items.add(new Tools("Защитные очки", "Очки для защиты глаз от пыли и осколков", 1, 8));
         items.add(new Weapons("Щит из металлолома", "Самодельный щит: толчки могут оглушить зомби", 3, 60, WeaponStyle.SHIELD));
         items.add(new Tools("Бронепластина", "Защитная пластина; защищает после экипировки в пункте 15", 5, 150));
         // ===== БЫТОВЫЕ И ПОХОДНЫЕ ПРЕДМЕТЫ =====
         items.add(new Tools("Зажигалка", "Простая зажигалка для бытовых нужд", 1, 5));
         items.add(new Tools("Коробок спичек", "Спички для разведения огня в сухих условиях", 1, 2));
         items.add(new Tools("Огниво", "Приспособление для получения искр", 2, 15));
         items.add(new Tools("Металлическая кружка", "Прочная кружка для напитков", 1, 8));
         items.add(new Tools("Походный котелок", "Посуда для приготовления пищи на костре", 2, 20));
         items.add(new Tools("Столовый набор", "Ложка, вилка и нож для еды", 1, 5));
         items.add(new Tools("Плащ-дождевик", "Легкая защита от дождя и ветра", 1, 10));
         items.add(new Tools("Теплые перчатки", "Утепленные перчатки для холодной погоды", 1, 12));
         items.add(new Tools("Зимняя куртка", "Теплая куртка для выживания в холоде", 3, 70));
         items.add(new Tools("Запасные носки", "Сухая пара носков для длительного похода", 1, 5));
         items.add(new Tools("Прочная обувь", "Износостойкие ботинки для пересеченной местности", 3, 80));
         items.add(new Tools("Мыло", "Кусок мыла для поддержания гигиены", 1, 3));
         items.add(new Tools("Зубная щетка", "Щетка для ежедневной гигиены", 1, 3));
         items.add(new Tools("Зубная паста", "Тюбик зубной пасты", 1, 4));




        return items;
     }

     static boolean valuableEarlyItem(ItemInfo item) { return item.getRarity() >= 4 || item.rawPrice() >= 100; }
     static List<ItemInfo> eligibleLoot(List<ItemInfo> items, LootProfile profile, boolean themed, boolean early, java.util.Random random) {
        boolean allowValuables = !early || random.nextInt(100) == 0;
        List<ItemInfo> pool = new ArrayList<>();
        for (ItemInfo item : items) if ((!themed || matchesProfile(item, profile)) && (allowValuables || !valuableEarlyItem(item))) pool.add(item);
        return pool;
     }
     static int denAdjustedRarity(int rarity,boolean denBonus,java.util.Random random){
        return denBonus && random.nextInt(100)<40?Math.min(5,rarity+1):rarity;
     }
     static ItemInfo createLootForLocation(Location location, java.util.Random random) {
        Locations world=GameIO.world();
        boolean denBonus=world!=null && world.getStories().monsterDens().active(world.getNumber(location));
        return createItems(location.getDifficulty(), location.getLootProfile(), random, location.getDifficulty() <= 2 && !denBonus,denBonus);
     }
     public static ItemInfo createItems(int difficulty, LootProfile profile, java.util.Random random) {
        return createItems(difficulty, profile, random, false,false);
     }
     private static ItemInfo createItems(int difficulty, LootProfile profile, java.util.Random random, boolean early,boolean denBonus) {
        difficulty = Math.max(1, Math.min(3, difficulty));
        boolean themed = profile != LootProfile.GENERAL && random.nextInt(100) < 65;
        if (themed && profile == LootProfile.MEDICAL || !themed && random.nextInt(100) < 15) {
            MedicalItem medicine = random.nextInt(100) < (denBonus?28:20)
                    ? new MedicalItem("Аптечка", 40, 50)
                    : new MedicalItem("Бинт", 15, 20);
            medicine.showResult();
            return medicine;
        }

        List<ItemInfo> items = lootCatalogue();
        int roll = random.nextInt(100) + 1;
        roll = Math.min(100, roll + (difficulty - 1) * 15);
        int rarity=0;
        if (roll <= 70){
           rarity = 1;
        } else if (roll >= 71 && roll <= 80) {
            rarity = 2;
        } else if (roll >=81 && roll <=90) {
            rarity = 3;
        } else if (roll >= 91 &&roll<= 96) {
            rarity = 4;
        }else{
            rarity = 5;
        }

        rarity=denAdjustedRarity(rarity,denBonus,random);
        List<ItemInfo> matchingItems =new ArrayList<>();

        List<ItemInfo> pool = eligibleLoot(items, profile, themed, early, random);
        for (ItemInfo item : pool){
            if (item.getRarity() == rarity){
                matchingItems.add(item);
            }

        }
        if (matchingItems.isEmpty()) matchingItems.addAll(pool);
         ItemInfo foundItem = matchingItems.get(
                 random.nextInt(matchingItems.size())
         );


         if (!(foundItem instanceof MedicalItem)) {
             // Difficulty changes rarity, never the value of an identical object.
         }
         if (foundItem.isDurable()) foundItem.wear(random.nextInt(61));
         foundItem.showResult();

         return foundItem;

     }
}
