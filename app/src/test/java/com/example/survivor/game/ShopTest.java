package com.example.survivor.game;
import java.io.*;
import java.util.*;

public class ShopTest {
    @org.junit.Test public void everyExpandedProductCanBeBoughtWithoutSharingInstances() {
        int count = 0;
        for (int number = 1; number <= Shop.LAST_PRODUCT; number++) {
            ItemInfo sample = Shop.createItem(number);
            if (sample == null) continue;
            count++;
            Fighter buyer = new Fighter("John", "", 100);
            int remaining = Shop.buyQuantity(buyer, 5000, number, 2);
            check(remaining == 5000 - sample.getPrice() * 2, "Correct expanded product price " + number);
            check(buyer.getInventory().size() == 3, "Full batch received " + number);
            check(buyer.getInventory().get(1) != buyer.getInventory().get(2), "Items are independent " + number);
            check(buyer.getCarryWeight() <= Fighter.MAX_WEIGHT, "Weight limit respected " + number);
        }
        check(count == 57, "57 catalogue products, excluding healing and craft-only weapons");
    }

    @org.junit.Test public void newSuppliesWorkWithFoodAndRepairSystems() {
        Fighter buyer = new Fighter("John", "", 100);
        buyer.getEquippedWeapon().wear(70);
        int money = Shop.buyQuantity(buyer, 100, 45, 1);
        money = Shop.buyQuantity(buyer, money, 42, 1);
        check(money == 63, "New repair supplies charged");
        check(SurvivalSystems.repair(buyer, buyer.getEquippedWeapon()), "New tool and tape repair weapon");
        check(buyer.getEquippedWeapon().getDurability() == 60, "Repair restores 30 condition");
        check(buyer.getInventory().stream().noneMatch(ItemInfo::isRepairMaterial), "Repair material consumed");
        check(buyer.getInventory().stream().anyMatch(ItemInfo::isRepairTool), "Tool retained");
        buyer.increaseHunger(80);
        Shop.buyQuantity(buyer, money, 35, 1);
        Survivor.useFood(buyer, new java.util.Scanner("1"));
        check(buyer.getHunger() == 55, "New food restores advertised hunger");
        check(buyer.getInventory().stream().noneMatch(item -> item instanceof Food), "Meal consumed");
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    @org.junit.Test public void originalRegression() throws Exception { java.nio.file.Files.createDirectories(java.nio.file.Path.of(".build")); main(new String[0]); }
    public static void main(String[] args) {
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            Fighter buyer = new Fighter("John", "", 100);
            int balance = Shop.buy(buyer, new Scanner("16 2 19 1 0"), 100);
            check(balance == 82 && buyer.getInventory().size() == 4, "Repeated purchases stay in section");
            balance = Shop.sell(buyer, new Scanner("1 1 0"), balance);
            check(balance == 90 && buyer.getInventory().size() == 2, "Repeated sales stay in section");
            check(buyer.getEquippedWeapon() == null, "Selling clears equipped weapon");
            int size = buyer.getInventory().size();
            check(Shop.buyQuantity(buyer, balance, 8, 2) == balance && buyer.getInventory().size() == size,
                    "Insufficient funds do not partially buy");
            check(Shop.buyQuantity(buyer, balance, 16, Integer.MAX_VALUE) == balance, "No overflow for large quantity");
            check(Shop.buyQuantity(buyer, balance, 16, -1) == balance, "Negative quantity rejected");
            check(Shop.buy(buyer, new Scanner("16 0 16 nope 16 -3 0"), balance) == balance, "Cancel and invalid quantity preserve balance");
            Fighter full = new Fighter("John", "", 100);
            for (int i = 0; i < 22; i++) full.addItem(new Tools("Груз", "", 1, 1));
            check(Shop.buyQuantity(full, 100, 16, 3) == 100 && full.getCarryWeight() == 24000,
                    "Whole batch must fit before money is charged");
            check(Shop.buyQuantity(full, 100, 16, 2) == 94 && full.getCarryWeight() == 25000, "Exact capacity purchase works");
            Fighter ammo = new Fighter("John", "", 100);
            check(Shop.buyQuantity(ammo, 192, 12, 2) == 0 && ammo.getAmmoCount(AmmoType.SHELL) == 8,
                    "Ammo quantity counts full packs; exact balance allowed");
            ammo.consumeAmmo(AmmoType.SHELL);
            check(((Ammunition) ammo.getInventory().get(1)).getQuantity() == 3
                    && ((Ammunition) ammo.getInventory().get(2)).getQuantity() == 4, "Purchased objects are independent");
            Shop.sell(ammo, new Scanner("1 1 1"), 0);
            check(ammo.getInventory().isEmpty(), "Selling final item returns from section");
        } finally { System.setOut(original); }
        System.out.println("Shop tests passed");
    }
}
