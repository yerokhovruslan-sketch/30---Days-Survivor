package com.example.survivor.game;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class GameSessionTest {
    @Test public void familyChoiceConsumesSuppliesAndCheckpointKeepsResolution() throws Exception {
        Fighter john=new Fighter("John","",100);john.addItem(ResidentialStories.waterBottle());john.addItem(new MedicalItem("Бинт",15,20));
        Locations world=new Locations();world.getStories().advanceMinutes(2880);
        ResidentialStories story=world.getStories().residential();story.family=1;story.familyDue=6;story.familyCleared=true;
        try(GameSession session=new GameSession(directory.getRoot(),frames::offer,()->{
            GameIO.mainMenu(john,25,world);story.run(1,john,1,world.getStories(),GameIO.scanner(),new java.util.Random(2));
        })) {
            session.start();GameSession.Frame frame=next(GameSession.Mode.CHOICE);
            assertEquals("Окно на третьем этаже",frame.menu.title);
            frame=choose(session,frame,3,GameSession.Mode.CHOICE);assertEquals("Находки",frame.menu.title);
            choose(session,frame,0,GameSession.Mode.ENDED);
            assertEquals(2,story.family);assertEquals(1,john.getInventory().size());
            GameSave loaded=GameSave.load(new java.io.File(directory.getRoot(),"survivor-save.dat"));
            assertEquals(2,loaded.locations.getStories().residential().family);assertEquals(25,loaded.balance);
        }
    }
    @Test public void returningKirillBackpackOffersSingleRewardThenPersistsFriendship() throws Exception {
        Fighter john=new Fighter("John","",100);Locations world=new Locations();
        ResidentialStories story=world.getStories().residential();story.kirill=1;
        try(GameSession session=new GameSession(directory.getRoot(),frames::offer,()->{
            GameIO.mainMenu(john,0,world);story.run(2,john,1,world.getStories(),GameIO.scanner(),new java.util.Random(2));
        })) {
            session.start();GameSession.Frame frame=next(GameSession.Mode.CHOICE);
            assertEquals("Чужая добыча",frame.menu.title);
            frame=choose(session,frame,1,GameSession.Mode.CHOICE);assertEquals("Предложение Кирилла",frame.menu.title);
            choose(session,frame,1,GameSession.Mode.ENDED);
            assertEquals(2,story.kirill);assertTrue(story.toolInfo);assertFalse(story.toolCache);
        }
    }
    @Test public void actualEquippedWeaponIsSelectedAndNoiseResetsOnlyWhenHome() throws Exception {
        Fighter john=new Fighter("John","",100);Weapons revolver=(Weapons)Shop.createItem(9);
        john.addItem(revolver);john.equipWeapon(revolver);john.addNoise(60);
        try(GameSession session=new GameSession(directory.getRoot(),frames::offer,()->{
            GameIO.player(john);Survivor.chooseEquipment(john,GameIO.scanner());
            GameIO.mainMenu(john,0,new Locations());GameIO.scanner().nextInt();
        })) {
            session.start();GameSession.Frame frame=next(GameSession.Mode.CHOICE);
            assertEquals(60,frame.noise);
            assertFalse(frame.choices.get(0).selected);assertTrue(frame.choices.get(1).selected);
            assertFalse(frame.choices.get(1).enabled);assertTrue(frame.choices.get(1).label.contains("Уже экипировано"));
            assertFalse(session.submit(frame.revision,2));
            frame=choose(session,frame,0,GameSession.Mode.MAIN);assertEquals(0,frame.noise);
        }
    }
    @Test public void radioMessageAndThoughtUseSeparateContinueGatesAndSaveObjective() throws Exception {
        Fighter john=new Fighter("John","",100);Locations world=new Locations();
        john.getUpgrades().radio=1;john.getUpgrades().signalAttempts=2;
        john.getUpgrades().signalReceived=true;john.getUpgrades().signalStage=1;
        try(GameSession session=new GameSession(directory.getRoot(),frames::offer,()->{
            GameIO.mainMenu(john,33,world);
            john.getUpgrades().listen(john,GameIO.scanner(),world.getStories());
        })) {
            session.start();GameSession.Frame frame=next(GameSession.Mode.CHOICE);
            assertEquals("radio_audio",frame.menu.scene);assertEquals(1,john.getUpgrades().signalStage);
            assertFalse(session.submit(frame.revision,0));
            frame=choose(session,frame,1,GameSession.Mode.CHOICE);
            assertEquals("radio_thought",frame.menu.scene);
            assertTrue(frame.menu.hint.contains("проверить дату"));assertEquals(2,john.getUpgrades().signalStage);
            choose(session,frame,1,GameSession.Mode.ENDED);
            assertEquals(0,john.getUpgrades().signalStage);assertEquals(1,world.getStories().journal().size());
            GameSave loaded=GameSave.load(new java.io.File(directory.getRoot(),"survivor-save.dat"));
            assertTrue(((Fighter)loaded.player).getUpgrades().signalReceived);
            assertEquals(0,((Fighter)loaded.player).getUpgrades().signalStage);assertEquals(33,loaded.balance);
        }
    }
    @Rule public TemporaryFolder directory = new TemporaryFolder();
    private final LinkedBlockingQueue<GameSession.Frame> frames = new LinkedBlockingQueue<>();

    private GameSession.Frame next(GameSession.Mode mode) throws Exception {
        GameSession.Frame frame;
        do {
            frame = frames.poll(5, TimeUnit.SECONDS);
            assertNotNull("Game worker did not reach a prompt", frame);
        } while (frame.mode == GameSession.Mode.BUSY);
        assertEquals(frame.text, mode, frame.mode);
        if (mode == GameSession.Mode.CHOICE) {
            assertFalse("Every input must have a named screen", frame.menu.title.isEmpty());
            assertTrue("Every input must have controls", frame.quantity || !frame.choices.isEmpty());
            assertFalse("Menu options leaked into events", frame.text.matches("(?s).*(?:^|\n)\\d+ - .*"));
        }
        return frame;
    }

    private GameSession.Frame choose(GameSession session, GameSession.Frame frame, int action,
                                     GameSession.Mode nextMode) throws Exception {
        assertTrue(session.submit(frame.revision, action));
        assertFalse("Duplicate taps must not queue a second turn", session.submit(frame.revision, action));
        return next(nextMode);
    }

    @Test public void restSleepSaveLoadAndContinue() throws Exception {
        Fighter tired = new Fighter("John", "", 100); tired.increaseSleepiness(60);
        GameSave.save(new File(directory.getRoot(), "survivor-save.dat"), tired, 0, new Locations());
        try (GameSession session = new GameSession(directory.getRoot(), frames::offer)) {
            session.start();
            GameSession.Frame frame = next(GameSession.Mode.CHOICE);
            frame = choose(session, frame, 1, GameSession.Mode.MAIN);
            assertEquals(0, frame.hunger);
            frame = choose(session, frame, 7, GameSession.Mode.MAIN);
            assertEquals(1, frame.hunger);
            frame = choose(session, frame, 10, GameSession.Mode.MAIN);
            assertEquals(1, frame.day);
            assertEquals(11, frame.hunger);
            frame = choose(session, frame, 11, GameSession.Mode.MAIN);
            assertTrue(frame.text, frame.text.contains("Игра сохранена:"));
            frame = choose(session, frame, 10, GameSession.Mode.MAIN);
            assertEquals(1, frame.day);
            assertTrue(frame.text.contains("выше 40"));
            frame = choose(session, frame, 12, GameSession.Mode.MAIN);
            assertEquals(1, frame.day);
            assertEquals(11, frame.hunger);
            frame = choose(session, frame, 0, GameSession.Mode.ENDED);
            assertTrue(frame.text.contains("Игра сохранена:"));
        }
        try (GameSession restarted = new GameSession(directory.getRoot(), frames::offer)) {
            restarted.start();
            GameSession.Frame frame = next(GameSession.Mode.CHOICE);
            assertTrue(frame.choices.stream().anyMatch(choice -> choice.label.contains("Продолжить")));
            frame = choose(restarted, frame, 1, GameSession.Mode.MAIN);
            assertEquals(1, frame.day);
            assertEquals(11, frame.hunger);
        }
    }

    @Test public void shopDoesNotRequestInvisibleInputAndUpdatesMoneyAndFood() throws Exception {
        try (GameSession session = new GameSession(directory.getRoot(), frames::offer)) {
            session.start();
            GameSession.Frame frame = next(GameSession.Mode.MAIN);
            frame = choose(session, frame, 4, GameSession.Mode.CHOICE);
            assertEquals("Магазин", frame.menu.title);
            frame = choose(session, frame, 1, GameSession.Mode.CHOICE);
            assertEquals("Продажа", frame.menu.title);
            frame = choose(session, frame, 1, GameSession.Mode.CHOICE);
            assertEquals("Магазин", frame.menu.title); // Last item sold: back to store.
            frame = choose(session, frame, 2, GameSession.Mode.CHOICE);
            assertEquals("Покупка", frame.menu.title);
            assertTrue(frame.choices.stream().filter(c->c.number>0).allMatch(c->!c.priceTag.isEmpty() && c.enabled));
            assertTrue(frame.choices.stream().noneMatch(c->c.detail.contains("Вес:") || c.detail.contains("прочность: 100")));
            frame = choose(session, frame, 16, GameSession.Mode.CHOICE);
            assertTrue(frame.quantity);
            frame = choose(session, frame, 2, GameSession.Mode.CHOICE);
            assertTrue(frame.text, frame.text.contains("Куплено: Галеты × 2"));
            frame = choose(session, frame, 0, GameSession.Mode.CHOICE);
            frame = choose(session, frame, 0, GameSession.Mode.MAIN);
            assertEquals(1, frame.balance);
            frame = choose(session, frame, 9, GameSession.Mode.CHOICE);
            frame = choose(session, frame, 1, GameSession.Mode.MAIN);
            assertEquals(0, frame.hunger);
            frame = choose(session, frame, 3, GameSession.Mode.MAIN);
            assertTrue(frame.menu.cards.stream().anyMatch(card -> card.title.equals("Галеты")));
            assertFalse(frame.menu.cards.stream().anyMatch(card -> card.title.equals("Металлическая труба")));
        }
    }

    @Test public void regionsCancelWithoutSpendingResourcesAndEquipmentWorks() throws Exception {
        try (GameSession session = new GameSession(directory.getRoot(), frames::offer)) {
            session.start();
            GameSession.Frame frame = next(GameSession.Mode.MAIN);
            frame = choose(session, frame, 2, GameSession.Mode.CHOICE);
            assertEquals("Районы", frame.menu.title);
            frame = choose(session, frame, 1, GameSession.Mode.CHOICE);
            assertTrue(frame.choices.stream().anyMatch(choice -> choice.label.equals("Тихий двор")));
            frame = choose(session, frame, 0, GameSession.Mode.CHOICE);
            frame = choose(session, frame, 0, GameSession.Mode.MAIN);
            assertEquals(0, frame.hunger);
            assertEquals(100, frame.stamina);
            frame = choose(session, frame, 6, GameSession.Mode.CHOICE);
            assertTrue(frame.choices.stream().anyMatch(choice -> choice.number == -1));
            frame = choose(session, frame, -1, GameSession.Mode.MAIN);
            assertTrue(frame.text.contains("Кулаки"));
            frame = choose(session, frame, 14, GameSession.Mode.MAIN);
            assertTrue(frame.text.contains("Ремонт не нужен"));
            frame = choose(session, frame, 16, GameSession.Mode.MAIN);
            assertTrue(frame.text.contains("В журнале пока"));
        }
    }

    @Test public void longSessionHandlesScannerBufferBoundaries() throws Exception {
        try (GameSession session = new GameSession(directory.getRoot(), frames::offer)) {
            session.start();
            GameSession.Frame frame = next(GameSession.Mode.MAIN);
            for (int i = 0; i < 700; i++) frame = choose(session, frame, 7, GameSession.Mode.MAIN);
            assertEquals(100, frame.hunger);
            assertEquals(100, frame.health);
        }
    }

    @Test public void lockedLocationsAndQuantityLimitsCannotBeSelected() throws Exception {
        try (GameSession session = new GameSession(directory.getRoot(), frames::offer)) {
            session.start();
            GameSession.Frame frame = next(GameSession.Mode.MAIN);
            frame = choose(session, frame, 2, GameSession.Mode.CHOICE);
            frame = choose(session, frame, 1, GameSession.Mode.CHOICE);
            GameSession.Choice locked = frame.choices.stream().filter(choice -> choice.detail.contains("Сначала посетите")).findFirst().get();
            assertTrue(locked.detail.contains("Сначала посетите"));
            frame = choose(session, frame, locked.number, GameSession.Mode.CHOICE);
            assertFalse(frame.choices.stream().filter(choice -> choice.number == 1).findFirst().get().enabled);
            frame = choose(session, frame, 0, GameSession.Mode.CHOICE);
            frame = choose(session, frame, 0, GameSession.Mode.CHOICE);
            frame = choose(session, frame, 0, GameSession.Mode.MAIN);
            frame = choose(session, frame, 4, GameSession.Mode.CHOICE);
            frame = choose(session, frame, 2, GameSession.Mode.CHOICE);
            frame = choose(session, frame, 16, GameSession.Mode.CHOICE);
            assertEquals(0, frame.menu.maxQuantity);
            assertFalse(session.submit(frame.revision, 1));
            assertFalse(session.submit(frame.revision, -1));
            frame = choose(session, frame, 0, GameSession.Mode.CHOICE);
            assertEquals("Покупка", frame.menu.title);
            assertTrue(frame.choices.stream().filter(c->c.number>0).allMatch(c->!c.priceTag.isEmpty() && c.enabled));
            assertTrue(frame.choices.stream().noneMatch(c->c.detail.contains("Вес:") || c.detail.contains("прочность: 100")));
        }
    }

    @Test public void combatHasSharedStatusAndTargetCardsWithoutTextMenus() throws Exception {
        Fighter john = new Fighter("John", "Боец", 100);
        List<CombatEnemy> opponents = new java.util.ArrayList<>();
        opponents.add(new Zombie(1, ZombieType.NORMAL));
        opponents.add(new Zombie(2, ZombieType.BRUTE));
        java.util.Random random = new java.util.Random(1) {
            @Override public int nextInt(int bound) { return 0; }
        };
        try (GameSession session = new GameSession(directory.getRoot(), frames::offer, () -> {
            GameIO.player(john);
            Survivor.fightEnemies(john, GameIO.scanner(), 1, random, opponents);
        })) {
            session.start();
            GameSession.Frame frame = next(GameSession.Mode.CHOICE);
            assertTrue(frame.menu.title.startsWith("Тактический бой"));
            assertNotNull(frame.menu.board);
            assertEquals(64, frame.menu.board.cells.size());
            assertEquals(2, frame.status.enemies.size());
            assertTrue(frame.status.enemies.get(0).selected);
            assertFalse(frame.text.contains("ПРОТИВНИКИ"));
            assertFalse(frame.text.contains("Выносливость:"));
            int bruteCell = frame.menu.board.cells.stream().filter(cell -> cell.unit == 7).findFirst().get().index;
            frame = choose(session, frame, 100 + bruteCell, GameSession.Mode.CHOICE);
            assertTrue(frame.status.enemies.get(1).selected);
            assertEquals(100, frame.health); // Target selection costs no turn.
            assertEquals(1, frame.menu.board.turn);
            frame = choose(session, frame, 4, GameSession.Mode.CHOICE);
            assertEquals("Экипировка", frame.menu.title);
            assertEquals(2, frame.status.enemies.size());
            frame = choose(session, frame, 0, GameSession.Mode.CHOICE);
            assertEquals(1, frame.menu.board.turn);
            int moveCell = frame.menu.board.cells.stream().filter(cell -> cell.move).findFirst().get().index;
            frame = choose(session, frame, 100 + moveCell, GameSession.Mode.CHOICE);
            assertEquals(moveCell, frame.menu.board.playerCell);
            assertEquals(1, frame.menu.board.turn); // Movement leaves the action available.
            frame = choose(session, frame, 2, GameSession.Mode.CHOICE);
            assertEquals(2, frame.menu.board.turn);
        }
    }

    @Test public void explorationMedicineArmorRepairAndJournalUseExplicitChoices() throws Exception {
        Fighter john = new Fighter("John", "Боец", 100);
        john.takeDamage(30);
        john.addItem(new MedicalItem("Бинт", 15, 20));
        john.addItem(new Tools("Защитный жилет", "Защита", 5, 300));
        john.getEquippedWeapon().wear(20);
        try (GameSession session = new GameSession(directory.getRoot(), frames::offer, () -> {
            GameIO.player(john);
            Survivor.continueExploring(john, GameIO.scanner(), new WorldStories());
        })) {
            session.start();
            GameSession.Frame frame = next(GameSession.Mode.CHOICE);
            assertEquals("На вылазке", frame.menu.title);
            assertEquals(70, frame.health);
            assertEquals(10, frame.choices.size());
            frame = choose(session, frame, 2, GameSession.Mode.CHOICE);
            assertEquals("Лечение", frame.menu.title);
            frame = choose(session, frame, 1, GameSession.Mode.CHOICE);
            assertEquals(90, frame.health);
            assertTrue(frame.text.contains("Восстановлено: 20"));
            assertFalse(frame.text.contains("Здоровье:"));
            frame = choose(session, frame, 8, GameSession.Mode.CHOICE);
            assertEquals("Броня", frame.menu.title);
            frame = choose(session, frame, 1, GameSession.Mode.CHOICE);
            assertTrue(frame.status.protection > 0);
            frame = choose(session, frame, 7, GameSession.Mode.CHOICE);
            assertEquals("Ремонт", frame.menu.title);
            frame = choose(session, frame, 0, GameSession.Mode.CHOICE);
            frame = choose(session, frame, 6, GameSession.Mode.CHOICE);
            assertEquals("Разгрузить рюкзак", frame.menu.title);
            frame = choose(session, frame, 0, GameSession.Mode.CHOICE);
            choose(session, frame, 0, GameSession.Mode.ENDED);
        }
    }

    @Test public void interruptedWriteRecoversPreviousSaveAndInvalidSaveDoesNotReplaceIt() throws Exception {
        File file = new File(directory.getRoot(), "survivor-save.dat");
        Fighter john = new Fighter("John", "Боец", 100);
        GameSave.save(file, john, 20, new Locations());
        try { GameSave.save(file, john, -1, new Locations()); fail("Negative balance accepted"); }
        catch (java.io.IOException expected) { assertEquals(20, GameSave.load(file).balance); }
        assertTrue(file.renameTo(new File(file + ".bak")));
        assertEquals(20, GameSave.load(file).balance);
        Files.write(file.toPath(), new byte[] {1, 2, 3});
        try { GameSave.load(file); fail("Corrupt save accepted"); }
        catch (java.io.IOException expected) { /* Reported to the UI, never starts broken state. */ }
    }
}
