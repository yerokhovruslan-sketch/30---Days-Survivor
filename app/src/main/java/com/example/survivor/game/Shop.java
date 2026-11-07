package com.example.survivor.game;

import java.util.*;

final class Shop {
    static final int LAST_PRODUCT = 61;
    static ItemInfo createItem(int number) {
        ItemInfo boughtItem;
        switch (number) {
            case 1: boughtItem = new MedicalItem("Бинт", 15, 20); break;
            case 2: boughtItem = new MedicalItem("Аптечка", 40, 50); break;
            case 3: boughtItem = new Weapons("Металлическая труба", "Тяжёлые удары", 2, 15, WeaponStyle.PIPE); break;
            case 4: boughtItem = new Weapons("Нож", "Два быстрых удара за ход", 2, 25, WeaponStyle.KNIFE); break;
            case 8: boughtItem = new Weapons("Охотничье ружье", "Огнестрельное оружие", 5, 400, WeaponStyle.SHOTGUN); break;
            case 9: boughtItem = new Weapons("Револьвер", "Огнестрельное оружие", 5, 350, WeaponStyle.REVOLVER); break;
            case 10: boughtItem = new Weapons("Арбалет", "Тихая стрельба; взведение после каждого болта", 4, 180, WeaponStyle.CROSSBOW); break;
            case 11: boughtItem = new Weapons("Метательная рогатка", "Стреляет шариками", 2, 20, WeaponStyle.SLINGSHOT); break;
            case 16: boughtItem = new Food("Галеты", "Сухой перекус", 1, 3, 20); break;
            case 17: boughtItem = new Food("Тушёнка", "Сытные консервы", 2, 8, 40); break;
            case 18: boughtItem = new Food("Сухпаёк", "Запас на тяжёлый день", 3, 45, 70); break;
            case 19: boughtItem = new Tools("Запчасти", "Материал для ремонта", 1, 12); break;
            case 20: boughtItem = new Tools("Набор инструментов", "Для ремонта оружия и брони", 2, 35); break;
            case 21: boughtItem = new Tools("Защитный жилет", "Снижает урон; можно надеть в разделе «Броня»", 5, 300); break;
            case 22: boughtItem = new Tools("Боевой шлем", "Снижает урон; можно надеть в разделе «Броня»", 4, 100); break;
            case 12: case 13: case 14: case 15:
                AmmoType type=AmmoType.values()[number-12];boughtItem=new Ammunition(type,type==AmmoType.PELLET?12:4);break;
            case 6: boughtItem = new Weapons("Копьё", "Удерживает противника на расстоянии", 2, 30, WeaponStyle.LONG); break;
            case 23: boughtItem = new Weapons("Молоток", "Оглушение в ближнем бою", 2, 32, WeaponStyle.HAMMER); break;
            case 24: boughtItem = new Weapons("Разводной ключ", "Тяжёлый удар и шанс оглушения", 2, 30, WeaponStyle.WRENCH); break;
            case 25: boughtItem = new Weapons("Отвёртка", "Два быстрых укола", 1, 12, WeaponStyle.SCREWDRIVER); break;
            case 26: boughtItem = new Weapons("Лопата", "Помогает удержать дистанцию", 2, 38, WeaponStyle.SHOVEL); break;
            case 27: boughtItem = new Weapons("Лом", "Сильный удар с оглушением", 3, 55, WeaponStyle.CROWBAR); break;
            case 28: boughtItem = new Weapons("Туристический топорик", "Легче большого топора", 3, 60, WeaponStyle.HATCHET); break;
            case 29: boughtItem = new Weapons("Охотничий нож", "Два точных выпада", 4, 110, WeaponStyle.HUNTING_KNIFE); break;
            case 31: boughtItem = new Weapons("Кувалда", "Большой урон ценой выносливости", 4, 120, WeaponStyle.SLEDGEHAMMER); break;
            case 33: boughtItem = new Weapons("Самодельный щит", "Толчок с высоким шансом оглушения", 3, 85, WeaponStyle.SHIELD); break;
            case 34: boughtItem = new Weapons("Дубинка", "Простое оружие для короткой вылазки", 1, 18, WeaponStyle.CLUB); break;
            case 35: boughtItem = new Food("Сухари", "Запечатанный дорожный запас", 1, 4, 25); break;
            case 36: boughtItem = new Food("Шоколад", "Плитка в целой упаковке", 2, 6, 30); break;
            case 37: boughtItem = new Food("Мясные консервы", "Сытная герметичная банка", 2, 10, 50); break;
            case 38: boughtItem = new Food("Армейский рацион", "Плотный запас еды", 3, 22, 65); break;
            case 39: boughtItem = new Food("Аварийный паёк", "Почти полностью утоляет голод", 4, 55, 90); break;
            case 40: boughtItem = new MedicalItem("Перевязочный пакет", 24, 35); break;
            case 41: boughtItem = new MedicalItem("Полевая аптечка", 65, 80); break;
            case 42: boughtItem = new Tools("Изолента", "Материал: ремонт +30 прочности", 1, 9); break;
            case 43: boughtItem = new Tools("Медная проволока", "Материал: ремонт +30 прочности", 1, 10); break;
            case 44: boughtItem = new Tools("Металлические пластины", "Материал: ремонт +30 прочности", 2, 14); break;
            case 45: boughtItem = new Tools("Набор отверток", "Многоразовый инструмент для ремонта", 2, 28); break;
            case 46: boughtItem = new Tools("Набор гаечных ключей", "Многоразовый инструмент для ремонта", 2, 32); break;
            case 47: boughtItem = new Tools("Фонарик", "Помогает замечать врагов ночью, но выдаёт ваше положение. Включается при выборе поиска.", 2, 45); break;
            case 48: boughtItem = new Tools("Плотная ткань", "Материал для улучшения кровати в убежище", 1, 8); break;
            case 49: boughtItem = new Tools("Электрочип", "Компонент радиостанции. Для восстановления связи нужны 2 штуки. Цена за один чип.", 5, 600); break;
            case 50: boughtItem=new Weapons("Пистолет","Магазин на 12 патронов",4,300,WeaponStyle.PISTOL);break;
            case 51: boughtItem=new Weapons("Охотничий карабин","Магазин на 5 патронов",5,600,WeaponStyle.CARBINE);break;
            case 52: boughtItem=new Ammunition(AmmoType.PISTOL,6);break;
            case 53: boughtItem=new Ammunition(AmmoType.RIFLE,4);break;
            case 54: boughtItem=Crafting.component(Crafting.POWDER);break;
            case 55: boughtItem=Crafting.component(Crafting.casing(AmmoType.REVOLVER));break;
            case 56: boughtItem=Crafting.component(Crafting.casing(AmmoType.PISTOL));break;
            case 57: boughtItem=Crafting.component(Crafting.casing(AmmoType.SHELL));break;
            case 58: boughtItem=Crafting.component(Crafting.casing(AmmoType.RIFLE));break;
            case 59: boughtItem=Crafting.component(Crafting.KIT);break;
            case 60: boughtItem=Crafting.component(Crafting.METAL);break;
            case 61: boughtItem=Crafting.component(Crafting.WOOD);break;
            default: return null;
        }
        return boughtItem;
    }
    static WorldStories clock(){return GameIO.world()==null?null:GameIO.world().getStories();}
    static int price(int number){return Economy.buyPrice(clock(),number);}
    static int category(ItemInfo item) {
        if(item instanceof Food || item instanceof MedicalItem)return 0;
        if(item instanceof Weapons)return 1;
        if(item instanceof Ammunition)return 2;
        return 3;
    }
    static String statDetails(ItemInfo item, int stock, int minutes) {
        List<String> details = new ArrayList<>();
        if(item instanceof Weapons) {
            WeaponStyle style=((Weapons)item).getStyle();
            details.add("Урон: "+style.minDamage+"–"+style.maxDamage+(style.strikes>1?" ×"+style.strikes:""));
            details.add("Энергия: "+style.staminaCost);
            details.add("Попадание: "+style.accuracy+"%");
            details.add("Шум: +"+style.noise());
            if(style.bleedChance()>0)details.add("Кровотечение: "+style.bleedChance()+"%");
        } else if(item instanceof Food) details.add("Еда: +"+((Food)item).getNutrition());
        else if(item instanceof MedicalItem) details.add(item.getName().toLowerCase(Locale.ROOT).contains("бинт")?"Лечение: кровотечение −3":"Лечение: кровотечение −5");
        else if(item.getArmorProtection()>0)details.add("Защита: "+item.getArmorProtection()+"%");
        else if(item.getName().equals("Электрочип"))details.add("Компонент радиостанции");
        else if(item.getName().equals("Фонарик"))details.add("Помогает замечать врагов ночью");
        details.add("В наличии: "+(stock==Integer.MAX_VALUE?"—":stock));
        if(stock==0 && minutes>=0)details.add("Поставка через: "+minutes/1440+" д. "+minutes%1440/60+" ч. "+minutes%60+" мин. (товар не гарантирован)");
        return String.join("\n",details);
    }
    static void showCatalogue() { showCatalogue(0); }
    static void showCatalogue(int selected) {
        String[] labels={"Припасы","Оружие","Патроны","Особое"};
        for(int i=0;i<labels.length;i++)GameIO.shopTab(1001+i,labels[i],i==selected);
        if(clock()!=null)GameIO.card("Поставки",clock().marketStatus());
        for (int i=1;i<=LAST_PRODUCT;i++) {
            ItemInfo item=createItem(i);if(item==null || category(item)!=selected)continue;
            int stock=clock()==null?Integer.MAX_VALUE:clock().craftStock(i);
            int minutes=clock()==null?-1:clock().marketMinutesUntil(i);
            int count=item instanceof Ammunition?((Ammunition)item).getQuantity():1;
            String tag=count==1?price(i)+" $\nза 1 шт.":String.format(Locale.ROOT,"%.2f $\nза 1 шт.\n%d шт. — %d $",price(i)/(double)count,count,price(i));
            GameIO.shopAction(i,item.getName()+(count>1?" ×"+count:""),statDetails(item,stock,minutes),tag,stock>0);
        }
        GameIO.action(0,"Назад в магазин");
    }
    static int buy(Fighter fighter, Scanner scanner, int balance) {
        int selectedCategory=0;
        while (GameIO.hasInput(scanner)) {
            GameIO.balance(balance);
            GameIO.player(fighter);
            GameIO.menu("Покупка", "Выберите товар. Баланс: " + balance + " $.");

            showCatalogue(selectedCategory);GameIO.checkpoint();
            int number = SurvivalSystems.choice(scanner);
            if (number == 0) return balance;
            if(number>=1001 && number<=1004){selectedCategory=number-1001;continue;}
            ItemInfo sample = createItem(number);
            if (sample == null || category(sample)!=selectedCategory) { GameIO.out().println("Неверный номер товара."); continue; }
            if(clock()!=null && clock().craftStock(number)<=0){GameIO.out().println("Товара пока нет в наличии.");continue;}
            int maxQuantity = Math.min(balance / Math.max(1, price(number)),
                    (Fighter.MAX_WEIGHT - fighter.getCarryWeight()) / Math.max(1, sample.getWeightGrams()));
            if(GameIO.world()!=null)maxQuantity=Math.min(maxQuantity,GameIO.world().getStories().craftStock(number));
            GameIO.quantity(sample.getName(), price(number) + " $ за "
                    + (sample instanceof Ammunition ? "упаковку (" + ((Ammunition) sample).getQuantity() + " шт.)" : "штуку")
                    + ". Доступно к покупке: " + maxQuantity + ".", maxQuantity, price(number));
            GameIO.card(sample.getName(), sample.getDescription() + "\n" + sample.physicalStats());
            int quotedPrice=price(number);
            int quantity = SurvivalSystems.choice(scanner);
            if(price(number)!=quotedPrice){GameIO.out().println("Цена изменилась с новой поставкой. Проверьте предложение ещё раз; деньги не списаны.");continue;}
            if (quantity == 0) continue;
            balance = buyQuantity(fighter, balance, number, quantity);
        }
        return balance;
    }
    static int buyQuantity(Fighter fighter, int balance, int number, int quantity) {
        ItemInfo sample = createItem(number);
        if (sample == null || quantity <= 0) {
            GameIO.out().println("Укажите положительное целое количество.");
            return balance;
        }
        long cost = (long) price(number) * quantity;
        long weight = (long) sample.getWeightGrams() * quantity;
        if (cost > balance) {
            GameIO.sound(GameSound.ERROR);GameIO.out().println("Недостаточно денег: нужно " + cost + " $, есть " + balance + " $. Покупка отменена.");
            return balance;
        }
        if (weight + fighter.getCarryWeight() > Fighter.MAX_WEIGHT) {
            GameIO.sound(GameSound.ERROR);GameIO.out().println("Вся покупка не поместится в 25 кг. Покупка отменена — выберите меньше или разгрузите рюкзак.");
            return balance;
        }
        List<ItemInfo> batch = new ArrayList<>();
        if(GameIO.world()!=null && quantity>GameIO.world().getStories().craftStock(number)){GameIO.sound(GameSound.ERROR);GameIO.out().println("На сегодня недостаточно товара.");return balance;}
        for (int i = 0; i < quantity; i++) batch.add(createItem(number));
        fighter.getInventory().addAll(batch);
        balance -= (int) cost;
        if(GameIO.world()!=null)GameIO.world().getStories().buyCraftStock(number,quantity);
        GameIO.balance(balance);GameIO.checkpoint();
        GameIO.sound(GameSound.TRADE);
        GameIO.out().println("Куплено: " + sample.getName() + " × " + quantity + ". Списано " + cost + " $. Осталось " + balance + " $.");
        if (sample instanceof Weapons) GameIO.out().println("Оружие можно выбрать в разделе «Экипировка».");
        return balance;
    }
    static int sell(Fighter fighter, Scanner scanner, int balance) {
        while (GameIO.hasInput(scanner)) {
            GameIO.balance(balance);
            GameIO.player(fighter);
            List<ItemInfo> items = fighter.getInventory();
            if (items.isEmpty()) { GameIO.out().println("Предметов для продажи больше нет."); return balance; }
            GameIO.menu("Продажа", "Выберите вещь для продажи. Баланс: " + balance + " $.");
            for (int i = 0; i < items.size(); i++)
                GameIO.action(i + 1, "Продать: " + items.get(i).getName(), Economy.salePrice(clock(),items.get(i)) + " $ · " + items.get(i).physicalStats()+(SurvivalSystems.protectedRadioPart(fighter,items.get(i))?" · Джону кажется, этот предмет важен":""),!SurvivalSystems.protectedRadioPart(fighter,items.get(i)));
            GameIO.action(0, "Назад в магазин");
            int number = SurvivalSystems.choice(scanner);
            if (number == 0) return balance;
            if (number < 1 || number > items.size()) { GameIO.out().println("Неверный номер."); continue; }
            ItemInfo item = items.get(number - 1);
            if(SurvivalSystems.protectedRadioPart(fighter,item)){GameIO.out().println("Джону кажется, этот предмет ещё пригодится для основной цели. Он не станет продавать его сейчас.");continue;}
            long updated = (long) balance + Economy.salePrice(clock(),item);
            if (updated > Integer.MAX_VALUE) { GameIO.out().println("Достигнут предел баланса. Предмет не продан."); continue; }
            items.remove(number - 1);
            fighter.getEquippedWeapon(); fighter.getEquippedArmor();
            balance = (int) updated;
            GameIO.balance(balance);GameIO.checkpoint();
        GameIO.sound(GameSound.TRADE);
            GameIO.out().println("Продан предмет: " + item.getName() + ". Баланс: " + balance + " $.");
        }
        return balance;
    }
}

