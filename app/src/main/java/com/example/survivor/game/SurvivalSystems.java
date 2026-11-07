package com.example.survivor.game;

import java.util.*;

final class SurvivalSystems {
    static int choice(Scanner scanner) {
        if (!scanner.hasNext()) return 0;
        if (scanner.hasNextInt()) return scanner.nextInt();
        scanner.next();
        return Integer.MIN_VALUE;
    }
    static String load(Fighter fighter) {
        return String.format(Locale.ROOT, "Рюкзак: %.2f/25 кг; комфорт 15 кг; расход сил +%d%%",
                fighter.getCarryWeight() / 1000.0, fighter.getOverloadPercent());
    }
    static boolean protectedRadioPart(Fighter p,ItemInfo item){return item.getName().equals(PoliceArchive.KEY) || item.getName().equals(GroceryStore.KEY) || item.getName().equals(DistrictClinic.CABINET_KEY) || item.getName().equals(PharmacyWarehouse.KEY) || item.getName().equals(CityHospital.KEY) || item.getName().equals(ForestStories.BOX) && GameIO.clock()!=null && GameIO.clock().forest().boxRequired() || item.getName().equals(ApartmentBuilding.ATTIC_KEY) || item.getName().equals(School.HOUSE_KEY) || item.getName().equals(School.MED_KEY) || p.getUpgrades().radio==0 && (item.getName().equals("Электрочип") || item.getName().equals("Блок управления радиостанцией"));}
    static void backpack(Fighter fighter, Scanner scanner) {
        GameIO.sound(GameSound.BAG);
        while (true) {
            GameIO.player(fighter);
            GameIO.menu("Разгрузить рюкзак", "Выберите вещь, которую хотите выбросить. Удалённую вещь вернуть нельзя.");
            List<ItemInfo> items = fighter.getInventory();
            for (int i = 0; i < items.size(); i++)
                GameIO.item(i + 1, items.get(i), "Выбросить: ", !protectedRadioPart(fighter,items.get(i)));
            GameIO.action(0, "Назад");
            int n = choice(scanner);
            if (n == 0) {GameIO.sound(GameSound.BAG);return;}
            if (n < 1 || n > items.size()) { GameIO.out().println("Неверный номер."); continue; }
            if(protectedRadioPart(fighter,items.get(n-1))){GameIO.out().println("Джону кажется, этот предмет важен. Его можно оставить на складе убежища.");continue;}
            GameIO.out().println("Оставлено: " + items.remove(n - 1).getName());
            fighter.getEquippedWeapon();
            fighter.getEquippedArmor();
        }
    }
    static void collect(Fighter fighter, List<ItemInfo> found, Scanner scanner) {
        List<ItemInfo> remaining = new ArrayList<>(found);
        while (!remaining.isEmpty()) {
            GameIO.player(fighter);
            GameIO.menu("Находки", "Заберите нужное. Вес рюкзака не должен превышать 25 кг.");
            for (int i = 0; i < remaining.size(); i++) {
                ItemInfo item = remaining.get(i);
                GameIO.item(i + 1, item, "Забрать: ", fighter.canCarry(item));
            }
            GameIO.action(-1, "Разгрузить рюкзак");
            GameIO.action(0, "Оставить остальные находки");
            int n = choice(scanner);
            if (n == 0) { GameIO.out().println("Остальные находки оставлены."); return; }
            if (n == -1) { backpack(fighter, scanner); continue; }
            if (n < 1 || n > remaining.size()) { GameIO.out().println("Неверный номер."); continue; }
            ItemInfo item = remaining.get(n - 1);
            if (!fighter.addItem(item)) { GameIO.sound(GameSound.ERROR);GameIO.out().println("Больше 25 кг не унести. Освободите место или выберите другое."); continue; }
            GameIO.sound(GameSound.BAG);remaining.remove(n - 1);
            GameIO.out().println("Забрано: " + item.getName());
        }
    }
    static boolean repair(Fighter fighter, ItemInfo target) {
        return repair(fighter, target, GameIO.atHome());
    }
    static boolean repair(Fighter fighter, ItemInfo target, boolean home) {
        if(home && target instanceof Weapons && ((Weapons)target).getAmmoType()!=null && fighter.getUpgrades().bench<2)return false;
        List<ItemInfo> inventory = fighter.getInventory();
        if ((!inventory.contains(target) && !(home && fighter.getUpgrades().bench>0 && fighter.getStorage().contains(target))) || !target.isDurable() || target.getDurability() >= 100) return false;
        boolean tool = inventory.stream().anyMatch(item -> item != target && item.isRepairTool()
                && (!item.isDurable() || item.getDurability() > 0));
        ItemInfo material = inventory.stream().filter(ItemInfo::isRepairMaterial).findFirst().orElse(null);
        if (home && fighter.getUpgrades().bench > 0) {
            tool = true;
            if (material == null) material = fighter.getStorage().stream().filter(ItemInfo::isRepairMaterial).findFirst().orElse(null);
        }
        if (!tool || material == null) return false;
        inventory.remove(material);
        fighter.getStorage().remove(material);
        target.repair(home ? fighter.getUpgrades().repairAmount() : 30);
        return true;
    }
    static void repairMenu(Fighter fighter, Scanner scanner) {
        while (true) {
            List<ItemInfo> targets = (GameIO.atHome()?Crafting.possessions(fighter):fighter.getInventory()).stream()
                    .filter(item -> item.isDurable() && item.getDurability() < 100).collect(java.util.stream.Collectors.toList());
            if (targets.isEmpty()) { GameIO.out().println("Ремонт не нужен."); return; }
            GameIO.player(fighter);
            boolean bench = GameIO.atHome() && fighter.getUpgrades().bench > 0;
            GameIO.menu("Ремонт", "Восстановление +" + (bench ? fighter.getUpgrades().repairAmount() : 30)
                    + " прочности. " + (bench ? "Верстак заменяет инструмент; материал можно взять со склада."
                    : "Нужны отдельный исправный инструмент и материал."));
            for (int i = 0; i < targets.size(); i++) {
                ItemInfo item=targets.get(i);boolean locked=GameIO.atHome() && item instanceof Weapons && ((Weapons)item).getAmmoType()!=null && fighter.getUpgrades().bench<2;
                GameIO.item(i+1,item,locked?"Нужен верстак II: ":"Починить: ",!locked);
            }
            GameIO.action(0, "Назад");
            int n = choice(scanner);
            if (n == 0) return;
            if (n < 1 || n > targets.size()) { GameIO.out().println("Неверный номер."); continue; }
            ItemInfo target = targets.get(n - 1);
            if (!repair(fighter, target)) {
                GameIO.out().println("Нужен отдельный инструмент (набор, молоток, ключ, отвёртка) и запчасти, пластины, проволока или изолента.");
                return;
            }
            GameIO.out().println("Починено: " + target.getName() + ", прочность " + target.getDurability() + "/100.");
        }
    }
    static void armorMenu(Fighter fighter, Scanner scanner) {
        List<ItemInfo> armor = fighter.getInventory().stream().filter(item -> item.getArmorProtection() > 0).collect(java.util.stream.Collectors.toList());
        GameIO.player(fighter);
        GameIO.menu("Броня", "Два слота: шлем и защита корпуса. Можно носить одновременно.");
        for (int i = 0; i < armor.size(); i++) {
            boolean equipped=fighter.isArmorEquipped(armor.get(i));
            GameIO.item(i+1,armor.get(i),equipped?"Уже экипирован: ":"Надеть: ",!equipped,equipped);
        }
        GameIO.action(-1, "Снять броню корпуса","Шлем останется надетым",fighter.getEquippedArmor()!=null);
        GameIO.action(-2, "Снять шлем","Броня корпуса останется надетой",fighter.getEquippedHelmet()!=null);
        GameIO.action(0, "Отмена");
        int n = choice(scanner);
        if (n == -1) fighter.removeBodyArmor();
        else if(n == -2)fighter.removeHelmet();
        else if (n >= 1 && n <= armor.size()) fighter.equipArmor(armor.get(n - 1));
    }
    static int detectionChance(SurvivorInfo player) {
        return detectionChance(player, GameIO.clock());
    }
    static int detectionChance(SurvivorInfo player, WorldStories clock) {
        Fighter fighter = player instanceof Fighter ? (Fighter) player : null;
        int night = clock != null && clock.isNight() ? (fighter != null && fighter.isFlashlightOn() ? 8 : 25) : 0;
        return Math.min(95, Math.max(10, 75 - player.getSleepiness() / 2 - player.getNoise() / 2 - night
                + (fighter == null ? 0 : fighter.searchStealthModifier)));
    }
    static int bypassChance(SurvivorInfo player) {
        int overload = player instanceof Fighter ? ((Fighter) player).getOverloadPercent() / 5 : 0;
        return bypassChance(player, GameIO.clock(), overload);
    }
    static int bypassChance(SurvivorInfo player, WorldStories clock, int overload) {
        Fighter fighter = player instanceof Fighter ? (Fighter) player : null;
        int light = clock != null && clock.isNight() && fighter != null && fighter.isFlashlightOn() ? 20 : 0;
        return Math.min(50, Math.max(10, 50 - player.getSleepiness() / 3 - player.getNoise() / 2 - overload - light
                + (fighter == null ? 0 : fighter.searchStealthModifier)));
    }
}

