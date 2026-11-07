package com.example.survivor.game;

import java.util.*;

/** Abstract game recipes: inventory transactions are atomic and output sizes are exact. */
final class Crafting {
    static final String POWDER="Порох",KIT="Комплект деталей для патронов",METAL="Металл",WOOD="Древесина";
    static String casing(AmmoType type) {return switch(type){case REVOLVER->"Пустая револьверная гильза";case PISTOL->"Пустая пистолетная гильза";case SHELL->"Пустая дробовая гильза";case RIFLE->"Пустая карабинная гильза";default->"";};}
    static int componentPrice(String name) {
        if(name.equals(POWDER))return 8;if(name.equals(KIT))return 12;if(name.equals(METAL)||name.equals(WOOD))return 5;
        for(AmmoType type:AmmoType.values())if(!casing(type).isEmpty() && casing(type).equals(name))return switch(type){case REVOLVER,SHELL->3;case PISTOL->2;case RIFLE->4;default->0;};
        return 0;
    }
    static boolean isComponent(String name) {return componentPrice(name)>0;}
    static int componentWeight(String name) {return name.contains("гильза")?10:name.equals(POWDER)?50:name.equals(KIT)?100:500;}
    static Tools component(String name) {int price=componentPrice(name);if(price==0)throw new IllegalArgumentException(name);return new Tools(name,"Условный игровой компонент для крафтинга",name.equals(POWDER)||name.equals(KIT)?3:1,price);}
    static final class Recipe {
        final String name;final int bench,anvil,amount,minutes;final AmmoType ammo;final WeaponStyle weapon;
        final LinkedHashMap<String,Integer> materials=new LinkedHashMap<>();
        Recipe(String name,int bench,int anvil,int amount,int minutes,AmmoType ammo,WeaponStyle weapon,Object... parts) {
            this.name=name;this.bench=bench;this.anvil=anvil;this.amount=amount;this.minutes=minutes;this.ammo=ammo;this.weapon=weapon;
            for(int i=0;i<parts.length;i+=2)materials.put((String)parts[i],(Integer)parts[i+1]);
        }
        int cost(int batches) {int total=0;for(Map.Entry<String,Integer> part:materials.entrySet()) {
            int price=componentPrice(part.getKey());if(price==0)price=switch(part.getKey()){case "Запчасти"->12;case "Металлические пластины"->14;case "Изолента"->9;case "Плотная ткань"->8;default->0;};
            total+=price*part.getValue()*batches;
        }return total;}
    }
    static final List<Recipe> RECIPES=Arrays.asList(
        new Recipe("Револьверные патроны",2,0,4,40,AmmoType.REVOLVER,null,POWDER,2,casing(AmmoType.REVOLVER),4,KIT,1),
        new Recipe("Пистолетные патроны",2,0,6,45,AmmoType.PISTOL,null,POWDER,2,casing(AmmoType.PISTOL),6,KIT,1),
        new Recipe("Дробовые патроны",2,0,4,60,AmmoType.SHELL,null,POWDER,3,casing(AmmoType.SHELL),4,KIT,2),
        new Recipe("Карабинные патроны",2,0,4,70,AmmoType.RIFLE,null,POWDER,3,casing(AmmoType.RIFLE),4,KIT,2),
        new Recipe("Арбалетные болты",1,0,4,30,AmmoType.BOLT,null,METAL,2,KIT,1),
        new Recipe("Топор",1,1,1,90,null,WeaponStyle.AXE,METAL,5,"Запчасти",2,WOOD,2),
        new Recipe("Мачете",1,1,1,120,null,WeaponStyle.MACHETE,METAL,4,"Металлические пластины",1,"Запчасти",2,"Изолента",1),
        new Recipe("Катана",2,1,1,240,null,WeaponStyle.KATANA,METAL,6,"Металлические пластины",2,"Запчасти",3,"Плотная ткань",1),
        new Recipe("Пожарный топор",2,1,1,180,null,WeaponStyle.FIRE_AXE,METAL,7,"Металлические пластины",2,"Запчасти",3,"Изолента",2)
    );
    static Weapons weapon(Recipe r) {
        Weapons w=new Weapons(r.name,"Изготовлено на наковальне",4,r.weapon==WeaponStyle.KATANA?220:r.weapon==WeaponStyle.AXE?40:145,r.weapon);
        w.markCrafted();return w;
    }
    static List<ItemInfo> possessions(Fighter p) {List<ItemInfo> all=new ArrayList<>(p.getStorage());all.addAll(p.getInventory());return all;}
    static List<ItemInfo> select(Fighter p,Map<String,Integer> materials,int batches) {
        if(batches<1 || batches>100)return null;
        List<ItemInfo> available=possessions(p),selected=new ArrayList<>();
        for(Map.Entry<String,Integer> part:materials.entrySet()) {
            int need=part.getValue()*batches;
            for(ItemInfo item:available)if(need>0 && !selected.contains(item) && item.getName().equals(part.getKey())){selected.add(item);need--;}
            if(need>0)return null;
        }
        return selected;
    }
    static boolean unlocked(Fighter p,Recipe r) {return p.getUpgrades().bench>=r.bench && p.getUpgrades().anvil>=r.anvil;}
    static List<ItemInfo> output(Recipe r,int batches) {
        List<ItemInfo> result=new ArrayList<>();int count=r.amount*batches;
        if(r.ammo!=null)while(count>0){int n=Math.min(r.ammo.packSize,count);result.add(new Ammunition(r.ammo,n));count-=n;}
        else for(int i=0;i<count;i++)result.add(weapon(r));return result;
    }
    static boolean craft(Fighter p,WorldStories clock,Recipe r,int batches,boolean storage) {
        if(!unlocked(p,r)){GameIO.sound(GameSound.ERROR);return false;}List<ItemInfo> ingredients=select(p,r.materials,batches);if(ingredients==null){GameIO.sound(GameSound.ERROR);return false;}
        List<ItemInfo> result=output(r,batches);
        int removed=ingredients.stream().filter(p.getInventory()::contains).mapToInt(ItemInfo::getWeightGrams).sum();
        int weight=result.stream().mapToInt(ItemInfo::getWeightGrams).sum();
        if(!storage && p.getCarryWeight()-removed+weight>Fighter.MAX_WEIGHT){GameIO.sound(GameSound.ERROR);return false;}
        for(ItemInfo item:ingredients){p.getInventory().remove(item);p.getStorage().remove(item);}
        (storage?p.getStorage():p.getInventory()).addAll(result);clock.advanceMinutes(r.minutes*batches);
        GameIO.player(p);GameIO.checkpoint();return true;
    }
    static String requirements(Fighter p,Recipe r,int batches) {
        StringBuilder text=new StringBuilder();List<ItemInfo> all=possessions(p);
        for(Map.Entry<String,Integer> part:r.materials.entrySet())text.append(part.getKey()).append(": ").append(all.stream().filter(i->i.getName().equals(part.getKey())).count()).append('/').append(part.getValue()*batches).append('\n');
        return text+"Результат: "+r.amount*batches+" шт. · время: "+r.minutes*batches+" мин\nСтоимость компонентов в магазине: "+r.cost(batches)+" $";
    }
    static void recipeMenu(Fighter p,WorldStories clock,Recipe r,Scanner scanner) {
        while(true) {
            GameIO.menu(r.name,"Выберите количество целых партий. Ресурсы берутся из рюкзака и со склада.");
            GameIO.card("Одна партия",requirements(p,r,1));
            int[] counts=r.ammo==null?new int[]{1,2,3}:r.ammo==AmmoType.PISTOL?new int[]{1,2,4,10}:new int[]{1,3,6,15};
            for(int i=0;i<counts.length;i++)GameIO.action(i+1,"Рассчитать "+r.amount*counts[i]+" шт.",counts[i]+" партий · "+r.minutes*counts[i]+" мин · компоненты "+r.cost(counts[i])+" $",unlocked(p,r));
            GameIO.action(0,"Назад к рецептам");int choice=SurvivalSystems.choice(scanner);if(choice==0)return;
            if(choice<1 || choice>counts.length)continue;int batches=counts[choice-1];
            GameIO.menu("Подтвердить изготовление","Ничего не расходуется до подтверждения.");GameIO.card(r.name,requirements(p,r,batches));
            boolean ready=unlocked(p,r) && select(p,r.materials,batches)!=null;
            GameIO.action(1,"Изготовить в рюкзак","Нужны все указанные компоненты",ready);GameIO.action(2,"Изготовить на склад","Нужны все указанные компоненты",ready);GameIO.action(0,"Отмена");choice=SurvivalSystems.choice(scanner);
            if(choice==1 || choice==2) {
                if(craft(p,clock,r,batches,choice==2))GameIO.out().println("Изготовлено: "+r.name+" × "+r.amount*batches+(choice==2?". Отправлено на склад.":". Добавлено в рюкзак."));
                else GameIO.out().println("Не хватает ресурсов, уровня станции или места для всей партии. Ничего не потрачено. Можно выбрать склад.");
            }
        }
    }
    static final LinkedHashMap<String,Integer> ANVIL=new LinkedHashMap<>();
    static {ANVIL.put(METAL,6);ANVIL.put("Запчасти",4);ANVIL.put("Металлические пластины",2);}
    static boolean buildAnvil(Fighter p,WorldStories clock) {
        if(p.getUpgrades().anvil>0 || p.getUpgrades().bench<1 || possessions(p).stream().noneMatch(i->i.isRepairTool() && i.getDurability()>0))return false;
        List<ItemInfo> parts=select(p,ANVIL,1);if(parts==null)return false;
        for(ItemInfo i:parts){p.getInventory().remove(i);p.getStorage().remove(i);}p.getUpgrades().anvil=1;clock.advanceMinutes(240);GameIO.checkpoint();return true;
    }
    static List<ItemInfo> salvage(Fighter p,ItemInfo item) {
        List<ItemInfo> result=new ArrayList<>();
        if(item.getName().equals(PoliceArchive.KEY) || item.getName().equals(GroceryStore.KEY) || item.getName().equals(ApartmentBuilding.ATTIC_KEY) || item.getName().equals(School.HOUSE_KEY) || item.getName().equals(School.MED_KEY))return result;
        if(item.getName().equals("Электрочип") && p.getUpgrades().radio==0)return result;
        if(p.getUpgrades().bench<1 || !(item instanceof Weapons || item instanceof Tools) || isComponent(item.getName())
                || item.getName().matches("Запчасти|Металлические пластины|Медная проволока|Изолента|Плотная ткань"))return result;
        if(item.getName().equals("Блок управления радиостанцией")) {
            if(possessions(p).stream().anyMatch(i->i.isRepairTool() && i.getDurability()>0))result.add(Shop.createItem(49));
            return result;
        }
        int budget=item.getSalePrice();
        if(budget>=12) {
            boolean rare=p.getUpgrades().bench>=2 && item.getRarity()>=3 && item.getName().toLowerCase(Locale.ROOT).matches(".*(плата|радио|электро|мультиметр).*" );
            result.add(rare?component(KIT):Shop.createItem(19));budget-=12;
        }
        for(int i=0;i<Math.min(3,budget/5);i++)result.add(component(METAL));return result;
    }
    static boolean dismantle(Fighter p,ItemInfo item,WorldStories clock) {
        if(!p.getInventory().contains(item) && !p.getStorage().contains(item))return false;
        List<ItemInfo> result=salvage(p,item);if(result.isEmpty())return false;
        p.getInventory().remove(item);p.getStorage().remove(item);p.getEquippedWeapon();p.getEquippedArmor();
        p.getStorage().addAll(result);clock.advanceMinutes(15);GameIO.player(p);GameIO.checkpoint();return true;
    }
    static void dismantleMenu(Fighter p,WorldStories clock,Scanner scanner) {
        while(true) {
            List<ItemInfo> available=new ArrayList<>();for(ItemInfo item:possessions(p))if(!salvage(p,item).isEmpty())available.add(item);
            GameIO.menu("Разборка предметов","Предмет уничтожается. Компоненты попадут на склад. Один предмет — 15 минут.");
            for(int i=0;i<available.size();i++) {
                ItemInfo item=available.get(i);StringBuilder parts=new StringBuilder();for(ItemInfo part:salvage(p,item))parts.append(part.getName()).append("; ");
                GameIO.action(i+1,item.getName(),parts.toString());
            }
            GameIO.action(0,"Назад");int choice=SurvivalSystems.choice(scanner);if(choice==0)return;
            if(choice<1 || choice>available.size())continue;ItemInfo item=available.get(choice-1);
            GameIO.menu("Уничтожить предмет?",item.getName()+" будет разобран без возможности восстановления. Надетая вещь снимется.");
            GameIO.action(1,"Разобрать");GameIO.action(0,"Отмена");if(SurvivalSystems.choice(scanner)==1)dismantle(p,item,clock);
        }
    }
    static void menu(Fighter p,WorldStories clock,Scanner scanner) {
        while(true) {
            GameIO.player(p);GameIO.menu("Крафтинг","Верстак "+p.getUpgrades().bench+"/2 · наковальня "+p.getUpgrades().anvil+"/1");
            for(int i=0;i<RECIPES.size();i++){Recipe r=RECIPES.get(i);GameIO.action(i+1,r.name,"Верстак "+r.bench+(r.anvil>0?" + наковальня":"")+" · партия "+r.amount+" шт.",unlocked(p,r));}
            GameIO.action(20,"Разобрать предмет","Запчасти и металл; редкая электроника даёт комплект деталей при верстаке II",p.getUpgrades().bench>=1);
            GameIO.action(21,"Обслуживание и ремонт","Расширенный ремонт огнестрела требует верстака II",p.getUpgrades().bench>=1);
            GameIO.action(22,"Построить наковальню","6 металла, 4 запчасти, 2 пластины · рабочий инструмент · 4 часа",p.getUpgrades().bench>=1 && p.getUpgrades().anvil==0);
            GameIO.action(0,"Назад в убежище");int choice=SurvivalSystems.choice(scanner);if(choice==0)return;
            if(choice>=1 && choice<=RECIPES.size() && unlocked(p,RECIPES.get(choice-1)))recipeMenu(p,clock,RECIPES.get(choice-1),scanner);
            if(choice==20 && p.getUpgrades().bench>=1)dismantleMenu(p,clock,scanner);
            if(choice==21 && p.getUpgrades().bench>=1)SurvivalSystems.repairMenu(p,scanner);
            if(choice==22 && !buildAnvil(p,clock))GameIO.out().println("Не хватает ресурсов, верстака I или рабочего инструмента. Ничего не потрачено.");
        }
    }
    static boolean searchCases(Fighter p,WorldStories world,Scanner scanner,Random random) {
        int place=world.currentPlace();if(world.caseCount(place)==0)return true;
        GameIO.menu("Осмотреть место стрельбы","Доступно гильз: "+world.caseCount(place)+". Каждая проверяется один раз; не найденные теряются.");
        GameIO.action(1,"Быстрый сбор","5 минут · 65% на каждую гильзу");
        GameIO.action(2,"Тщательный сбор","15 минут · 90% на каждую гильзу");GameIO.action(0,"Отмена");
        int choice=SurvivalSystems.choice(scanner);if(choice!=1 && choice!=2)return true;
        int[] cases=world.takeCases(place);world.advanceMinutes(choice==1?5:15);List<ItemInfo> found=new ArrayList<>();
        for(int i=0;i<cases.length;i++)for(int n=0;n<cases[i];n++)if(random.nextInt(100)<(choice==1?65:90)) {
            String name=casing(AmmoType.values()[i]);if(!name.isEmpty())found.add(component(name));
        }
        if(random.nextInt(100)<15 && !Survivor.encounterRandom(p,scanner,2,random,world))return false;
        if(found.isEmpty())GameIO.out().println("Пригодных гильз не найдено.");else SurvivalSystems.collect(p,found,scanner);
        GameIO.checkpoint();return true;
    }
}
