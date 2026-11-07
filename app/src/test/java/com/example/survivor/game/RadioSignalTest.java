package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import static org.junit.Assert.*;

public class RadioSignalTest {
    private Random roll(int value) {return new Random(1) {@Override public int nextInt(int bound){return value;}};}
    @Test public void firstAttemptAlwaysFailsAndWaitIsExactlyTwentyFourGameHours() {
        ShelterUpgrades radio=new ShelterUpgrades();WorldStories clock=new WorldStories();
        assertEquals(ShelterUpgrades.SignalResult.LOCKED,radio.attemptSignal(clock,roll(0)));
        assertEquals(0,radio.signalAttempts);radio.radio=1;
        assertEquals(ShelterUpgrades.SignalResult.NO_SIGNAL_DAY,radio.attemptSignal(clock,roll(0)));
        assertEquals(1440,radio.signalWait(clock));clock.advanceMinutes(1439);
        assertFalse(radio.canListen(clock));assertEquals(ShelterUpgrades.SignalResult.LOCKED,radio.attemptSignal(clock,roll(0)));
        assertEquals(1,radio.signalAttempts);clock.advanceMinutes(1);assertTrue(radio.canListen(clock));
        assertEquals(ShelterUpgrades.SignalResult.RECEIVED,radio.attemptSignal(clock,roll(49)));
        assertEquals(1,radio.signalStage);assertTrue(radio.signalReceived);assertTrue(radio.valid());
    }
    @Test public void secondFailureWaitsTwelveHoursThenThirdAttemptCannotFail() {
        ShelterUpgrades radio=new ShelterUpgrades();radio.radio=1;WorldStories clock=new WorldStories();
        radio.attemptSignal(clock,roll(0));clock.advanceMinutes(1440);
        assertEquals(ShelterUpgrades.SignalResult.NO_SIGNAL_HALF_DAY,radio.attemptSignal(clock,roll(50)));
        assertEquals(720,radio.signalWait(clock));clock.advanceMinutes(719);assertFalse(radio.canListen(clock));
        clock.advanceMinutes(1);assertEquals(ShelterUpgrades.SignalResult.RECEIVED,radio.attemptSignal(clock,roll(99)));
        assertEquals(3,radio.signalAttempts);assertTrue(radio.valid());
        radio.signalStage=0;assertFalse(radio.canListen(clock));
        assertEquals(ShelterUpgrades.SignalResult.ALREADY_RECEIVED,radio.attemptSignal(clock,roll(99)));
        assertEquals(3,radio.signalAttempts);
    }
    @Test public void cooldownPendingSceneAndObjectivePersistWithoutDuplicateJournalEntries() throws Exception {
        Fighter player=new Fighter("John","",100);player.getUpgrades().radio=1;Locations world=new Locations();
        player.getUpgrades().attemptSignal(world.getStories(),roll(0));world.getStories().advanceMinutes(1350);
        File dir=Files.createTempDirectory("radio-save").toFile();File file=new File(dir,"save.dat");
        try {
            GameSave.save(file,player,17,world);GameSave loaded=GameSave.load(file);
            Fighter restored=(Fighter)loaded.player;
            assertEquals(90,restored.getUpgrades().signalWait(loaded.locations.getStories()));
            loaded.locations.getStories().advanceMinutes(90);
            assertEquals(ShelterUpgrades.SignalResult.RECEIVED,restored.getUpgrades().attemptSignal(loaded.locations.getStories(),roll(0)));
            restored.getUpgrades().signalStage=2;
            loaded.locations.getStories().recordRadioSignal();loaded.locations.getStories().recordRadioSignal();
            assertEquals(1,loaded.locations.getStories().journal().size());
            GameSave.save(file,restored,17,loaded.locations);
            Fighter finalPlayer=(Fighter)GameSave.load(file).player;
            assertEquals(2,finalPlayer.getUpgrades().signalStage);assertTrue(finalPlayer.getUpgrades().signalReceived);
        } finally {file.delete();new File(dir,"save.dat.bak").delete();dir.delete();}
    }
}
