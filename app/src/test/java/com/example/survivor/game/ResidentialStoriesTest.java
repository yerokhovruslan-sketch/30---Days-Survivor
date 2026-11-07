package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

public class ResidentialStoriesTest {
    private Random roll(int value) {return new Random(1) {@Override public int nextInt(int bound){return Math.min(value,bound-1);}};}
    private Fighter player() {return new Fighter("John","",100);}
    @Test public void onlyFirstThreeEventsAreRegisteredAndWindowRequiresDayThree() {
        WorldStories world=new WorldStories();ResidentialStories stories=world.residential();
        assertEquals(4,ResidentialStories.TITLES.length);
        stories.kirill=2;
        assertEquals(0,stories.discover(1,SearchMode.THOROUGH,world,roll(0)));
        world.advanceMinutes(2880);
        assertEquals(1,stories.discover(1,SearchMode.FAST,world,roll(14)));
        assertEquals(1,stories.family);assertTrue(stories.points(1,world).contains(1));
        for(int place=5;place<=53;place++)assertEquals(0,stories.discover(place,SearchMode.THOROUGH,world,roll(0)));
    }
    @Test public void discoveryChancesDependOnSearchAndKnownWindowDoesNotRepeat() {
        WorldStories world=new WorldStories();world.advanceMinutes(2880);ResidentialStories s=world.residential();s.kirill=2;
        assertEquals(0,s.discover(1,SearchMode.FAST,world,roll(15)));
        assertEquals(0,s.discover(1,SearchMode.CAREFUL,world,roll(35)));
        assertEquals(1,s.discover(1,SearchMode.THOROUGH,world,roll(54)));
        int entries=world.journal().size();assertEquals(0,s.discover(1,SearchMode.THOROUGH,world,roll(0)));
        assertEquals(entries,world.journal().size());
    }
    @Test public void familyHelpAtomicallyConsumesCarriedWaterAndMedicineOnlyOnce() {
        WorldStories w=new WorldStories();ResidentialStories s=w.residential();s.family=1;
        Fighter p=player();p.addItem(ResidentialStories.waterBottle());int before=p.getInventory().size();
        assertFalse(s.helpFamily(p,w,roll(0)));assertEquals(before,p.getInventory().size());
        p.getStorage().add(new MedicalItem("Бинт",15,20));assertFalse(s.helpFamily(p,w,roll(0)));
        p.addItem(new MedicalItem("Бинт",15,20));assertTrue(s.helpFamily(p,w,roll(0)));
        assertEquals(2,s.family);assertEquals(3,s.humanity);assertEquals(w.day()+3,s.familyDue);
        assertFalse(s.helpFamily(p,w,roll(0)));assertEquals(1,p.getStorage().size());
    }
    @Test public void delayedFamilyDepartureAndRobberyReputationRequireActualWitnessMeeting() {
        WorldStories w=new WorldStories();ResidentialStories s=w.residential();s.family=4;s.familyDue=4;
        w.advanceMinutes(4320);s.update(w);assertTrue(s.familyDeparted);assertEquals(0,s.reputation);
        s.run(1,player(),3,w,new Scanner("0"),roll(0));assertEquals(-2,s.reputation);assertTrue(s.familyFollowup);
        assertFalse(s.points(3,w).contains(1));
    }
    @Test public void starvingViktorCanBeRescuedBeforeDeadlineAndBasementNeedsToolOrKey() {
        WorldStories w=new WorldStories();ResidentialStories s=w.residential();s.viktor=4;s.viktorDue=4;
        Fighter p=player();p.addItem(Shop.createItem(17));int before=p.getInventory().size();
        assertFalse(s.helpViktor(p,w,false));assertEquals(before,p.getInventory().size());
        p.addItem(ResidentialStories.waterBottle());assertTrue(s.helpViktor(p,w,false));
        w.advanceMinutes(5760);s.update(w);assertEquals(2,s.viktor);assertTrue(s.basementKnown);
        assertFalse(ResidentialStories.lever(p));p.addItem(Shop.createItem(27));assertTrue(ResidentialStories.lever(p));
    }
    @Test public void robberyHasDelayedDeathAndBasementCannotRegenerateLoot() {
        WorldStories w=new WorldStories();ResidentialStories s=w.residential();s.viktor=4;s.viktorDue=3;
        w.advanceMinutes(1440);s.update(w);assertEquals(4,s.viktor);w.advanceMinutes(1440);s.update(w);assertEquals(5,s.viktor);
        Fighter p=player();s.basementKey=true;s.basementKnown=true;
        s.basement(p,w,new Scanner("1 0"));assertTrue(s.basementOpen);assertTrue(s.basementLoot);
        long time=ShelterUpgrades.minute(w);int count=p.getInventory().size();s.basement(p,w,new Scanner("0"));
        assertEquals(time,ShelterUpgrades.minute(w));assertEquals(count,p.getInventory().size());
    }
    @Test public void cooperationCacheAndTradeHaveFiniteRewards() {
        WorldStories w=new WorldStories();ResidentialStories s=w.residential();s.kirill=2;s.toolInfo=true;Fighter p=player();
        s.run(2,p,35,w,new Scanner("0"),roll(0));assertTrue(s.toolCache);
        p.addItem(Shop.createItem(17));int size=p.getInventory().size();
        s.run(2,p,1,w,new Scanner("1 0"),roll(0));assertEquals(2,s.kirillTradeStock);assertEquals(size-1,p.getInventory().size());
    }
    @Test public void civilianSlotsBlockMovementAndEnemyCanTargetThemWithoutDamagingJohn() {
        Fighter p=player();TacticalBattle b=new TacticalBattle(p,1,roll(0),Arrays.asList(new Zombie(1,ZombieType.NORMAL)));
        Arrays.fill(b.obstacles,null);b.playerCell=56;b.units.get(0).cell=50;
        TacticalBattle.Civilian marina=new TacticalBattle.Civilian("Марина",50,49);b.civilians.add(marina);
        assertFalse(b.canMove(49));assertEquals(8,b.snapshot().cells.get(49).unit);
        assertEquals(49,b.enemyTargetCell(b.units.get(0)));b.strike(b.units.get(0),false);
        assertTrue(marina.person.getHealth()<50);assertEquals(100,p.getHealth());
        b.units.get(0).enemy.takeDamage(1000);b.escortToExit=true;assertFalse(b.won());marina.safe=true;assertTrue(b.won());
    }
    @Test public void nightmareIsTemporaryAndNoUnknownWitnessChangesReputation() {
        WorldStories w=new WorldStories();ResidentialStories s=w.residential();s.family=5;s.humanity=-20;s.nightmareUntil=3;
        assertEquals(2,s.sleepPenalty(w));int logs=w.journal().size();assertEquals(2,s.sleepPenalty(w));assertEquals(logs,w.journal().size());
        w.advanceMinutes(4320);assertEquals(0,s.sleepPenalty(w));assertEquals(0,s.reputation);
    }
    @Test public void completeChapterStateSurvivesSavingAndOldWorldDefaultsRemainValid() throws Exception {
        WorldStories w=new WorldStories();ResidentialStories s=w.residential();s.family=2;s.familyDue=4;s.familyCleared=true;
        s.kirill=4;s.kirillDue=5;s.kirillSuspicion=1;s.viktor=4;s.viktorDue=4;s.humanity=-5;
        Locations world=new Locations();ResidentialStories target=world.getStories().residential();
        target.family=s.family;target.familyDue=s.familyDue;target.familyCleared=true;target.kirill=4;target.kirillDue=5;target.viktor=4;target.viktorDue=4;target.humanity=-5;
        File file=Files.createTempFile("chapter-one",".dat").toFile();
        try {GameSave.save(file,player(),10,world);ResidentialStories restored=GameSave.load(file).locations.getStories().residential();
            assertTrue(restored.valid());assertEquals(2,restored.family);assertEquals(4,restored.kirill);assertEquals(4,restored.viktor);assertEquals(-5,restored.humanity);
        } finally {file.delete();}
        assertTrue(new WorldStories().residential().valid());
    }
}
