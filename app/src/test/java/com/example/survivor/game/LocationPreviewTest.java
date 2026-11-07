package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class LocationPreviewTest {
    @Test public void nightlyRoutesMigrateAndRequireBothNightAndPreviousVisit() throws Exception {
        Locations world = new Locations();
        assertEquals(7, world.getGroupCount());
        assertTrue(world.getLocation(51).isNightOnly());
        assertFalse(world.isAvailable(51));
        world.getStories().advanceMinutes(720);
        assertFalse(world.isAvailable(51));
        world.visit(1);
        assertTrue(world.isAvailable(51)); assertTrue(LocationPreview.canDepart(world, 51));
        world.enter(world.getLocation(51));
        assertTrue(world.isVisited(51));
        world.getStories().advanceMinutes(600);
        assertFalse(world.isAvailable(51));
        File legacy = new File("app/src/test/resources/survivor-before-storage.dat");
        if (!legacy.isFile()) legacy = new File("src/test/resources/survivor-before-storage.dat");
        Locations loaded = GameSave.load(legacy).locations;
        assertTrue(loaded.getLocation(53).isNightOnly());
        assertEquals(53, loaded.getNumber(loaded.getLocation(53)));
        assertEquals(53, loaded.getNumber(loaded.getLocation(53))); // No repeated migration/duplicate locations.
    }
    @Test public void departureNearDawnCannotStrandPlayer() {
        Locations world = new Locations(); world.visit(1);
        world.getStories().advanceMinutes(1285); // Day 2, 05:25: 35 minutes left.
        assertEquals("05:25", world.getStories().time());
        assertTrue(world.isAvailable(51)); assertFalse(LocationPreview.canDepart(world, 51));
        world.getStories().advanceMinutes(35);
        assertFalse(world.isAvailable(51));
    }
    @Test public void jumpersAppearOnlyAtNightAndIgnoreDistance() {
        Random zero = new Random() { @Override public int nextInt(int bound) { return 0; } };
        assertEquals(ZombieType.NORMAL, Survivor.createZombie(1, zero, false).getType());
        Zombie jumper = Survivor.createZombie(1, zero, true);
        assertEquals(ZombieType.JUMPER, jumper.getType());
        assertEquals(165, jumper.getHealth());
        assertTrue(jumper.getMinDamage() > new Zombie(1, ZombieType.BRUTE).getMinDamage());
        Fighter player = new Fighter("John", "", 100);
        Weapons spear = (Weapons) Shop.createItem(6); player.addItem(spear); player.equipWeapon(spear);
        assertFalse(Survivor.attack(player, jumper, zero).keptDistance);
    }
    @Test public void previewIsReadOnlyAndListsActualGoals() throws Exception {
        Locations world = new Locations(); Fighter player = new Fighter("John", "", 100);
        LinkedBlockingQueue<GameSession.Frame> frames = new LinkedBlockingQueue<>();
        File folder = Files.createTempDirectory("survivor-preview").toFile();
        try (GameSession session = new GameSession(folder, frames::offer, () -> {
            GameIO.mainMenu(player, 0, world);
            LocationPreview.show(world, 1, player, GameIO.scanner());
        })) {
            session.start();
            GameSession.Frame frame = frames.poll(5, TimeUnit.SECONDS);
            assertNotNull(frame); assertEquals(GameSession.Mode.CHOICE, frame.mode);
            assertEquals("Тихий двор", frame.menu.title);
            assertTrue(frame.menu.cards.stream().anyMatch(card -> card.title.equals("Дорога и время") && card.detail.contains("25 минут")));
            assertTrue(frame.menu.cards.stream().anyMatch(card -> card.title.equals("Зачем идти") && card.detail.contains("Плотная ткань")));
            assertTrue(session.submit(frame.revision, 0));
            do { frame = frames.poll(5, TimeUnit.SECONDS); assertNotNull(frame); } while (frame.mode == GameSession.Mode.BUSY);
            assertEquals(GameSession.Mode.ENDED, frame.mode);
            assertEquals("08:00", world.getStories().time()); assertFalse(world.isVisited(1));
            assertEquals(100, player.getStamina());
        } finally { folder.delete(); }
    }
}
