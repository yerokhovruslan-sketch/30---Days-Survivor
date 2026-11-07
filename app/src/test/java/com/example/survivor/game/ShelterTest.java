package com.example.survivor.game;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class ShelterTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void equippedItemsAreRemovedAndRetainTheirState() {
        Fighter player = new Fighter("John", "Боец", 100);
        Weapons weapon = player.getEquippedWeapon();
        weapon.wear(45);
        Tools armor = new Tools("Защитный жилет", "Броня", 5, 300);
        armor.wear(12);
        player.addItem(armor);
        player.equipArmor(armor);
        assertTrue(Shelter.deposit(player, 0));
        assertNull(player.getEquippedWeapon());
        assertTrue(Shelter.deposit(player, 0));
        assertNull(player.getEquippedArmor());
        assertEquals(0, player.getCarryWeight());
        assertTrue(Shelter.withdraw(player, 0));
        assertSame(weapon, player.getInventory().get(0));
        assertEquals(55, weapon.getDurability());
        assertNull(player.getEquippedWeapon()); // Taking from storage does not silently equip it.
        assertTrue(Shelter.withdraw(player, 0));
        assertSame(armor, player.getInventory().get(1));
        assertEquals(88, armor.getDurability());
    }

    @Test public void heavyWithdrawalsAreAtomicAndDoNotDuplicateItems() {
        Fighter player = new Fighter("John", "", 100);
        for (int i = 0; i < 23; i++) player.addItem(new Tools("Груз", "", 1, 1));
        ItemInfo stored = new Tools("Запас", "", 1, 1);
        player.getStorage().add(stored);
        assertFalse(Shelter.withdraw(player, 0));
        assertEquals(1, player.getStorage().size());
        assertSame(stored, player.getStorage().get(0));
        assertFalse(Shelter.withdrawAll(player));
        int count = player.getInventory().size();
        Shelter.depositAll(player);
        assertEquals(count + 1, player.getStorage().size());
        assertTrue(player.getInventory().isEmpty());
        assertFalse(Shelter.withdrawAll(player));
        assertTrue(player.getInventory().isEmpty());
        assertTrue(Shelter.withdraw(player, 0));
        assertEquals(1, player.getInventory().size());
        assertEquals(count, player.getStorage().size());
        assertFalse(Shelter.withdraw(player, -1));
        assertFalse(Shelter.deposit(player, 999));
    }

    @Test public void storedAmmoAndDamagePersistAcrossSaveAndReload() throws Exception {
        Fighter player = new Fighter("John", "", 100);
        Ammunition ammo = new Ammunition(AmmoType.SHELL);
        ammo.consume();
        int remaining = ammo.getQuantity();
        player.addItem(ammo);
        player.getEquippedWeapon().wear(37);
        Shelter.depositAll(player);
        File file = new File(folder.getRoot(), "save.dat");
        GameSave.save(file, player, 24, new Locations());
        Fighter loaded = (Fighter) GameSave.load(file).player;
        assertTrue(loaded.getInventory().isEmpty());
        assertEquals(2, loaded.getStorage().size());
        assertEquals(63, loaded.getStorage().get(0).getDurability());
        assertEquals(remaining, ((Ammunition) loaded.getStorage().get(1)).getQuantity());
        assertTrue(Shelter.withdrawAll(loaded));
        assertTrue(loaded.getStorage().isEmpty());
        assertEquals(remaining, loaded.getAmmoCount(AmmoType.SHELL));
    }

    @Test public void actualOldSaveLoadsWithEmptyStorageAndKeepsProgress() throws Exception {
        // Fixture was written by the compiled pre-storage version, with no storage field.
        File fixture = new File("app/src/test/resources/survivor-before-storage.dat");
        if (!fixture.isFile()) fixture = new File("src/test/resources/survivor-before-storage.dat");
        GameSave old = GameSave.load(fixture);
        Fighter player = (Fighter) old.player;
        assertEquals(53, old.balance);
        assertEquals(4, old.locations.getStories().day());
        assertEquals(77, player.getEquippedWeapon().getDurability());
        assertEquals(4, player.getAmmoCount(AmmoType.SHELL));
        assertTrue(player.getStorage().isEmpty());
        Shelter.depositAll(player);
        File updated = new File(folder.getRoot(), "updated.dat");
        GameSave.save(updated, player, old.balance, old.locations);
        assertEquals(2, ((Fighter) GameSave.load(updated).player).getStorage().size());
    }

    @Test public void menuTransfersWithoutTimeCostAndSurvivesContinueAndLoad() throws Exception {
        LinkedBlockingQueue<GameSession.Frame> frames = new LinkedBlockingQueue<>();
        try (GameSession session = new GameSession(folder.getRoot(), frames::offer)) {
            session.start();
            GameSession.Frame frame = next(frames, GameSession.Mode.MAIN);
            frame = send(session, frames, frame, 17, GameSession.Mode.CHOICE);
            assertEquals("Убежище · склад", frame.menu.title);
            frame = send(session, frames, frame, 1, GameSession.Mode.CHOICE);
            frame = send(session, frames, frame, 1, GameSession.Mode.CHOICE);
            assertEquals("Кулаки", frame.status.weapon);
            frame = send(session, frames, frame, 0, GameSession.Mode.CHOICE);
            frame = send(session, frames, frame, 0, GameSession.Mode.MAIN);
            assertEquals(0, frame.hunger);
            assertEquals(0, frame.sleepiness);
            assertEquals(100, frame.stamina);
            frame = send(session, frames, frame, 11, GameSession.Mode.MAIN);
            frame = send(session, frames, frame, 12, GameSession.Mode.MAIN);
            frame = send(session, frames, frame, 17, GameSession.Mode.CHOICE);
            frame = send(session, frames, frame, 2, GameSession.Mode.CHOICE);
            assertTrue(frame.choices.stream().anyMatch(choice -> choice.label.contains("Металлическая труба")));
            frame = send(session, frames, frame, 1, GameSession.Mode.CHOICE);
            frame = send(session, frames, frame, 0, GameSession.Mode.CHOICE);
            frame = send(session, frames, frame, 0, GameSession.Mode.MAIN);
            frame = send(session, frames, frame, 6, GameSession.Mode.CHOICE);
            assertTrue(frame.choices.stream().anyMatch(choice -> choice.label.equals("Металлическая труба")));
        }
    }

    private GameSession.Frame send(GameSession session, LinkedBlockingQueue<GameSession.Frame> frames,
                                   GameSession.Frame frame, int number, GameSession.Mode mode) throws Exception {
        assertTrue(session.submit(frame.revision, number));
        return next(frames, mode);
    }
    private GameSession.Frame next(LinkedBlockingQueue<GameSession.Frame> frames, GameSession.Mode mode) throws Exception {
        GameSession.Frame frame;
        do { frame = frames.poll(5, TimeUnit.SECONDS); assertNotNull(frame); }
        while (frame.mode == GameSession.Mode.BUSY);
        assertEquals(frame.text, mode, frame.mode);
        return frame;
    }
}
