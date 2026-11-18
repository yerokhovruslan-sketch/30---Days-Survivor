package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import static org.junit.Assert.*;

public class EarlyLootBalanceTest {
    @Test public void difficultyModifiersMatchTheNewBalance() {
        assertEquals(65, Difficulty.needsPercent(1));
        assertEquals(110, Difficulty.needsPercent(2));
        assertEquals(125, Difficulty.needsPercent(3));
        assertEquals(11, Difficulty.damage(10, 2));
        assertEquals(12, Difficulty.damage(10, 3));
        assertEquals(42, Difficulty.encounterChance(40, 2));
        assertEquals(-5, Difficulty.loot(2));
    }

    @Test public void apartmentCrowbarIsAvailableWithoutAnotherCrowbarAndPersists() throws Exception {
        Locations locations = new Locations();
        Fighter player = new Fighter("John", "", 100);
        WorldStories world = locations.getStories();
        world.apartment().visit(player, locations.getLocation(5), world,
                new Scanner("2 2 0 3 4 0 0"), new Random(1));
        assertEquals(1, player.getInventory().stream().filter(i -> i.getName().equals("Лом")).count());
        File save = Files.createTempFile("apartment-crowbar", ".dat").toFile();
        try {
            GameSave.save(save, player, 0, locations);
            GameSave restored = GameSave.load(save);
            Fighter restoredPlayer = (Fighter) restored.player;
            restored.locations.getStories().apartment().visit(restoredPlayer, restored.locations.getLocation(5),
                    restored.locations.getStories(), new Scanner("3 4 0 0"), new Random(2));
            assertEquals(1, restoredPlayer.getInventory().stream().filter(i -> i.getName().equals("Лом")).count());
        } finally { save.delete(); }
    }

    @Test public void aFindLeftBehindDoesNotReroll() {
        Fighter player = new Fighter("John", "", 100);
        while (player.addItem(Shop.createItem(19))) { }
        HashMap<String, ItemInfo> finds = new HashMap<>();
        assertFalse(EarlyLoot.search(player, finds, "bandage", new Random(0), 100, 1, -1));
        ItemInfo original = finds.get("bandage");
        assertNotNull(original);
        assertFalse(EarlyLoot.search(player, finds, "bandage", new Random(2), 0, 1, -1));
        assertSame(original, finds.get("bandage"));
        player.getInventory().remove(player.getInventory().size() - 1);
        assertTrue(EarlyLoot.search(player, finds, "bandage", new Random(2), 0, 1, -1));
        assertTrue(player.getInventory().contains(original));
    }

    @Test public void leftBehindFindSurvivesSaveAndReload() throws Exception {
        Locations locations = new Locations();
        Fighter player = new Fighter("John", "", 100);
        while (player.addItem(Shop.createItem(19))) { }
        locations.getStories().courtyard().visit(player, locations.getLocation(1), locations.getStories(),
                new Scanner("1 3 0 0"), new Random() { @Override public int nextInt(int bound) { return 0; } });
        File save = Files.createTempFile("left-behind-loot", ".dat").toFile();
        try {
            GameSave.save(save, player, 0, locations);
            GameSave restored = GameSave.load(save);
            Fighter restoredPlayer = (Fighter) restored.player;
            restoredPlayer.getInventory().remove(restoredPlayer.getInventory().size() - 1);
            WorldStories world = restored.locations.getStories();
            world.courtyard().visit(restoredPlayer, restored.locations.getLocation(1), world,
                    new Scanner("1 3 0 0"), new Random() { @Override public int nextInt(int bound) { return bound - 1; } });
            assertEquals(1, restoredPlayer.getInventory().stream().filter(i -> i.getName().equals("Бинт")).count());
        } finally { save.delete(); }
    }
}
