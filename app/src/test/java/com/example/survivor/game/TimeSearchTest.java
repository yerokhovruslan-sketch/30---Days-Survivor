package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import static org.junit.Assert.*;

public class TimeSearchTest {
    private static final Random ZERO = new Random() { @Override public int nextInt(int bound) { return 0; } };
    @Test public void midnightAndNightBoundariesComeFromActions() {
        WorldStories clock = new WorldStories();
        assertEquals("08:00", clock.time());
        clock.advanceMinutes(710);
        assertFalse(clock.isNight());
        clock.advanceMinutes(10);
        assertEquals("20:00", clock.time());
        assertTrue(clock.isNight());
        clock.advanceMinutes(240);
        assertEquals(2, clock.day());
        assertEquals("00:00", clock.time());
        clock.advanceMinutes(360);
        assertFalse(clock.isNight());
        for (int i = 0; i < 16; i++) clock.discover(1, "Двор", ZERO, true);
        assertEquals("06:00", clock.time());
        assertEquals(2, clock.day()); // Searches no longer advance an artificial day every eight rolls.
    }
    @Test public void searchModesSpendTheirAdvertisedResourcesOnlyWhenPossible() {
        for (SearchMode mode : SearchMode.values()) {
            Fighter player = new Fighter("John", "", 100);
            WorldStories clock = new WorldStories();
            assertTrue(mode.begin(player, clock));
            assertEquals(480 + mode.minutes, clock.minuteOfDay());
            assertEquals(100 - mode.stamina, player.getStamina());
            assertEquals(mode.needs, player.getHunger());
            player.spendStamina(player.getStamina());
            int before = clock.minuteOfDay();
            assertFalse(mode.begin(player, clock));
            assertEquals(before, clock.minuteOfDay());
        }
    }
    @Test public void thoroughSearchImprovesLootWithoutCreatingExtraSearchSites() {
        Location careful = new Location("Двор", "", 0);
        Location thorough = new Location("Двор", "", 0);
        for (int i = 0; i < 10; i++) { careful.searchSupplies(ZERO); thorough.searchSupplies(ZERO); }
        Random roll45 = new Random() { @Override public int nextInt(int bound) { return 45; } };
        assertFalse(careful.searchSupplies(roll45, 0));
        assertTrue(thorough.searchSupplies(roll45, SearchMode.THOROUGH.lootBonus));
        assertEquals(careful.getRemainingSearches(), thorough.getRemainingSearches());
    }
    @Test public void flashlightOffsetsNightPenaltyButMakesBypassHarder() {
        Fighter player = new Fighter("John", "", 100);
        WorldStories clock = new WorldStories();
        int daytime = SurvivalSystems.detectionChance(player, clock);
        clock.advanceMinutes(720);
        int dark = SurvivalSystems.detectionChance(player, clock);
        assertTrue(dark < daytime);
        player.addItem(Shop.createItem(47));
        player.toggleFlashlight();
        assertTrue(SurvivalSystems.detectionChance(player, clock) > dark);
        int litBypass = SurvivalSystems.bypassChance(player, clock, 0);
        player.toggleFlashlight();
        assertTrue(SurvivalSystems.bypassChance(player, clock, 0) > litBypass);
        player.searchStealthModifier = 10;
        assertTrue(SurvivalSystems.detectionChance(player, clock) > dark);
    }
    @Test public void clockAndFlashlightSurviveSaveAndLegacySavesStartAtEight() throws Exception {
        File folder = Files.createTempDirectory("survivor-clock").toFile();
        File file = new File(folder, "save.dat");
        try {
            Locations world = new Locations();
            world.getStories().advanceMinutes(1000);
            Fighter player = new Fighter("John", "", 100);
            player.addItem(Shop.createItem(47)); player.toggleFlashlight();
            GameSave.save(file, player, 10, world);
            GameSave saved = GameSave.load(file);
            assertEquals(world.getStories().time(), saved.locations.getStories().time());
            assertEquals(world.getStories().day(), saved.locations.getStories().day());
            assertTrue(((Fighter) saved.player).isFlashlightOn());
            File legacy = new File("app/src/test/resources/survivor-before-storage.dat");
            if (!legacy.isFile()) legacy = new File("src/test/resources/survivor-before-storage.dat");
            assertEquals("08:00", GameSave.load(legacy).locations.getStories().time());
        } finally { file.delete(); folder.delete(); }
    }
}
