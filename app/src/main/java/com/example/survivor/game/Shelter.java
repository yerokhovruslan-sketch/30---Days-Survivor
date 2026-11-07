package com.example.survivor.game;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/** Storage exists only at home and retains the actual items, including ammo and wear. */
final class Shelter {
    static long weight(List<ItemInfo> items) {
        long total = 0;
        for (ItemInfo item : items) total += item.getWeightGrams();
        return total;
    }

    static boolean deposit(Fighter player, int index) {
        if (index < 0 || index >= player.getInventory().size()) return false;
        ItemInfo item = player.getInventory().remove(index);
        player.getStorage().add(item);
        player.getEquippedWeapon();
        player.getEquippedArmor();
        return true;
    }

    static boolean withdraw(Fighter player, int index) {
        if (index < 0 || index >= player.getStorage().size()) return false;
        ItemInfo item = player.getStorage().get(index);
        if (!player.canCarry(item)) return false;
        player.getStorage().remove(index);
        player.addItem(item);
        return true;
    }

    static void depositAll(Fighter player) {
        player.getStorage().addAll(player.getInventory());
        player.getInventory().clear();
        player.getEquippedWeapon();
        player.getEquippedArmor();
    }

    static boolean withdrawAll(Fighter player) {
        if (weight(player.getStorage()) + player.getCarryWeight() > Fighter.MAX_WEIGHT) return false;
        player.getInventory().addAll(player.getStorage());
        player.getStorage().clear();
        return true;
    }

    static void menu(Fighter player, Scanner scanner) {
        while (true) {
            GameIO.player(player);
            GameIO.menu("Убежище · склад", "Вещи на складе не занимают место в рюкзаке. Склад доступен только дома.");
            GameIO.card("Запасы убежища", String.format(Locale.ROOT, "%d предметов · %.2f кг\nЛимита склада нет.",
                    player.getStorage().size(), weight(player.getStorage()) / 1000.0));
            GameIO.card("Рюкзак", SurvivalSystems.load(player));
            GameIO.card("План эвакуации", player.getUpgrades().evacuationPlan(player, GameIO.world()));
            GameIO.action(1, "Положить вещи на склад", "Освободить место в рюкзаке");
            GameIO.action(2, "Забрать вещи со склада", "Подготовить снаряжение к вылазке");
            GameIO.action(3, "Улучшения убежища", "Кровать, верстак и радиостанция");
            GameIO.action(6,"Осмотреть приёмник","Неисправность и способы добычи компонентов");
            GameIO.action(5,"Крафтинг","Боеприпасы, наковальня, разборка и обслуживание");
            GameIO.action(4,"Поймать сигнал",player.getUpgrades().signalHint(GameIO.clock()),player.getUpgrades().canListen(GameIO.clock()));
            if(GameIO.clock()!=null && GameIO.clock().semen().heartStage>=5 && GameIO.clock().isNight())
                GameIO.action(7,"Посмотреть на ночное небо","Вспомнить слова Семёна");
            GameIO.action(0, "В главное меню");
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) return;
            if(choice==7 && GameIO.clock()!=null && GameIO.clock().semen().heartStage>=5 && GameIO.clock().isNight())
                ResidentialStories.pause("Ночное небо","Звёзды выглядят так же, как вчера.\n\nВпрочем, Джон никогда особенно не разбирался в созвездиях.\n\nИнтересно, зачем он вообще начал обращать на это внимание?",scanner);
            if (choice == 1 || choice == 2) transferMenu(player, scanner, choice == 1);
            if (choice == 3) ShelterUpgrades.menu(player, scanner, GameIO.clock());
            if(choice==6 && GameIO.clock()!=null)GameIO.clock().inspectRadio(player,scanner);
            if(choice==5)Crafting.menu(player,GameIO.clock(),scanner);
            if (choice == 4) player.getUpgrades().listen(player,scanner,GameIO.clock());
        }
    }

    private static void transferMenu(Fighter player, Scanner scanner, boolean depositing) {
        while (true) {
            List<ItemInfo> items = depositing ? player.getInventory() : player.getStorage();
            GameIO.player(player);
            GameIO.menu(depositing ? "Положить на склад" : "Забрать со склада", depositing
                    ? "Выберите вещь. Если она надета, она будет снята. Характеристики и патроны сохраняются."
                    : "Вес рюкзака — не больше 25 кг. Оружие и броню после переноса можно надеть через меню.");
            if (items.isEmpty()) GameIO.card(depositing ? "Рюкзак пуст" : "Склад пуст", depositing
                    ? "Все вещи уже оставлены в убежище." : "Принесите находки и оставьте их здесь до следующей вылазки.");
            for (int i = 0; i < items.size(); i++) {
                ItemInfo item = items.get(i);
                String verb = depositing ? "Положить: " : "Забрать: ";
                if (!depositing && !player.canCarry(item)) verb = "Не помещается: ";
                GameIO.item(i + 1, item, verb, depositing || player.canCarry(item));
            }
            if (!items.isEmpty()) GameIO.action(-1, depositing ? "Положить всё" : "Забрать всё",
                    depositing ? "Включая надетое оружие и броню" : "Вся партия должна поместиться в 25 кг",
                    depositing || weight(items) + player.getCarryWeight() <= Fighter.MAX_WEIGHT);
            GameIO.action(0, "Назад на склад");
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) return;
            if (choice == -1) {
                int count = items.size();
                if (depositing) depositAll(player);
                else if (!withdrawAll(player)) { GameIO.out().println("Всё не поместится. Заберите вещи по отдельности."); continue; }
                GameIO.out().println((depositing ? "Убрано на склад: " : "Забрано со склада: ") + count + " предметов.");
            } else if (choice >= 1 && choice <= items.size()) {
                String name = items.get(choice - 1).getName();
                boolean moved = depositing ? deposit(player, choice - 1) : withdraw(player, choice - 1);
                GameIO.out().println(moved ? (depositing ? "На складе: " : "В рюкзаке: ") + name
                        : "Не хватает места в рюкзаке. Освободите место и попробуйте снова.");
            }
        }
    }
}
