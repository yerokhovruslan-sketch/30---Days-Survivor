package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ShelterUpgradesTest {
    private Fighter player() { return new Fighter("John", "Боец", 100); }
    private void stock(Fighter player, int product, int quantity) {
        for (int i = 0; i < quantity; i++) player.getStorage().add(Shop.createItem(product));
    }
    @Test public void radioRequiresTwoChipsAndConsumesThemAcrossBagAndStorage() {
        Fighter player = player(); WorldStories clock = new WorldStories();
        stock(player, 43, 2); stock(player, 19, 2); stock(player, 42, 1); stock(player, 20, 1);
        stock(player, 49, 1);
        int stored = player.getStorage().size();
        assertFalse(player.getUpgrades().build(player, 3, clock));
        assertEquals(stored, player.getStorage().size());
        assertEquals("08:00", clock.time());
        player.addItem(Shop.createItem(49));
        assertTrue(player.getUpgrades().build(player, 3, clock));
        assertEquals(1, player.getUpgrades().radio);
        assertEquals("10:00", clock.time());
        assertFalse(player.getInventory().stream().anyMatch(item -> item.getName().equals("Электрочип")));
        assertFalse(player.getStorage().stream().anyMatch(item -> item.getName().equals("Электрочип")));
        assertEquals(1, player.getStorage().size()); // Reusable tool retained.
        Locations world = new Locations();
        assertEquals("Военный склад", world.getLocation(Locations.EVACUATION_LOCATION).getName());
        assertTrue(player.getUpgrades().evacuationPlan(player, world).contains("Военный склад"));
    }
    @Test public void twoChipsCost1200AndCannotBePartiallyPurchased() {
        Fighter player = player(); int size = player.getInventory().size();
        assertEquals(1199, Shop.buyQuantity(player, 1199, 49, 2));
        assertEquals(size, player.getInventory().size());
        assertEquals(0, Shop.buyQuantity(player, 1200, 49, 2));
        assertEquals(size + 2, player.getInventory().size());
        assertEquals(50, Shop.createItem(49).getWeightGrams());
        assertFalse(Shop.createItem(49).isRepairMaterial());
    }
    @Test public void sleepRequiresStrictlyMoreThanFortyAndCannotBeRepeated() {
        Fighter player = player(); WorldStories clock = new WorldStories();
        player.takeDamage(50); player.increaseSleepiness(40);
        assertEquals(-1, ShelterUpgrades.sleep(player, clock));
        assertEquals("08:00", clock.time()); assertEquals(50, player.getHealth());
        player.increaseSleepiness(1);
        assertEquals(3, ShelterUpgrades.sleep(player, clock));
        assertEquals(53, player.getHealth()); assertEquals("16:00", clock.time());
        assertEquals(0, player.getSleepiness()); assertEquals(10, player.getHunger());
        assertEquals(-1, ShelterUpgrades.sleep(player, clock));
        assertEquals("16:00", clock.time());
    }
    @Test public void bedQualityHungerAndHealthCapAffectHealing() {
        for (int level = 0; level < 3; level++) {
            Fighter player = player(); player.takeDamage(70); player.increaseSleepiness(80);
            player.getUpgrades().bed = level;
            assertEquals(new int[] {3, 8, 15}[level], ShelterUpgrades.sleep(player, new WorldStories()));
        }
        Fighter hungry = player(); hungry.takeDamage(50); hungry.increaseSleepiness(60); hungry.increaseHunger(75);
        hungry.getUpgrades().bed = 2;
        assertEquals(7, ShelterUpgrades.sleep(hungry, new WorldStories()));
        Fighter nearFull = player(); nearFull.takeDamage(1); nearFull.increaseSleepiness(60);
        nearFull.getUpgrades().bed = 2;
        assertEquals(1, ShelterUpgrades.sleep(nearFull, new WorldStories()));
        nearFull.takeDamage(100); nearFull.increaseSleepiness(60);
        assertEquals(-1, ShelterUpgrades.sleep(nearFull, new WorldStories()));
        assertEquals(0, nearFull.getHealth());
    }
    @Test public void buildingConsumesFullRecipeFromStorageOnlyWhenAvailable() {
        Fighter player = player(); WorldStories clock = new WorldStories();
        stock(player, 48, 1); stock(player, 42, 1);
        assertFalse(player.getUpgrades().build(player, 1, clock));
        assertEquals(2, player.getStorage().size()); assertEquals("08:00", clock.time());
        stock(player, 48, 1);
        assertTrue(player.getUpgrades().build(player, 1, clock));
        assertTrue(player.getStorage().isEmpty()); assertEquals("10:00", clock.time());
        stock(player, 48, 3); stock(player, 19, 2); stock(player, 42, 1);
        assertTrue(player.getUpgrades().build(player, 1, clock));
        assertEquals(2, player.getUpgrades().bed);
        assertFalse(player.getUpgrades().build(player, 1, clock));
    }
    @Test public void workbenchBonusAndStorageMaterialsApplyOnlyAtHome() {
        Fighter player = player(); WorldStories clock = new WorldStories();
        stock(player, 19, 2); stock(player, 44, 1); stock(player, 20, 1);
        assertTrue(player.getUpgrades().build(player, 2, clock));
        assertEquals(1, player.getStorage().size()); // Tool is retained.
        stock(player, 42, 1); player.getEquippedWeapon().wear(80);
        assertFalse(SurvivalSystems.repair(player, player.getEquippedWeapon(), false));
        assertTrue(SurvivalSystems.repair(player, player.getEquippedWeapon(), true));
        assertEquals(65, player.getEquippedWeapon().getDurability());
        assertEquals(1, player.getStorage().size());
    }
    @Test public void radioQuestsRequireSuppliesRewardOnlyOnceAndPersist() throws Exception {
        Fighter player = player(); WorldStories clock = new WorldStories();
        stock(player, 43, 2); stock(player, 19, 2); stock(player, 42, 1); stock(player, 20, 1);
        stock(player, 49, 2);
        assertTrue(player.getUpgrades().build(player, 3, clock));
        assertTrue(player.getUpgrades().accept());
        assertFalse(player.getUpgrades().accept());
        assertFalse(player.getUpgrades().deliver(player));
        stock(player, 1, 2);
        assertTrue(player.getUpgrades().deliver(player));
        int count = player.getStorage().size();
        assertFalse(player.getUpgrades().deliver(player));
        assertEquals(count, player.getStorage().size());
        assertTrue(player.getStorage().stream().anyMatch(item -> item.getName().equals("Тушёнка")));
        assertTrue(player.getUpgrades().accept());
        File folder = Files.createTempDirectory("survivor-upgrades").toFile(); File file = new File(folder, "save.dat");
        try {
            GameSave.save(file, player, 0, new Locations());
            Fighter loaded = (Fighter) GameSave.load(file).player;
            assertEquals(1, loaded.getUpgrades().radio); assertEquals(1, loaded.getUpgrades().completed);
            assertTrue(loaded.getUpgrades().accepted);
            stock(loaded, 16, 3);
            assertTrue(loaded.getUpgrades().deliver(loaded));
            assertTrue(loaded.getUpgrades().accept()); stock(loaded, 19, 3); stock(loaded, 43, 2);
            assertTrue(loaded.getUpgrades().deliver(loaded)); assertEquals(3, loaded.getUpgrades().completed);
            assertFalse(loaded.getUpgrades().accept()); assertTrue(loaded.getUpgrades().valid());
        } finally { file.delete(); folder.delete(); }
    }
}

