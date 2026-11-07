package com.example.survivor.game;

import org.junit.Test;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class SoundEventsTest {
    private List<GameSound> events(Runnable program) throws Exception {
        BlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();
        List<GameSound> sounds=Collections.synchronizedList(new ArrayList<>());
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        try(GameSession session=new GameSession(Files.createTempDirectory("sound-events").toFile(),frames::offer,()->{
            try {program.run();}catch(Throwable error){failure.set(error);}
        })) {
            session.setEffectsListener(sounds::add);session.start();
            GameSession.Frame frame=frames.poll(10,TimeUnit.SECONDS);assertNotNull(frame);
            assertEquals(GameSession.Mode.ENDED,frame.mode);
            assertFalse(frame.text,frame.text.contains("Ошибка игры"));
            if(failure.get()!=null)throw new AssertionError("Sound-event program failed",failure.get());
        }
        return sounds;
    }
    @Test public void cashRegisterSoundsOnlyOnCommittedPurchasesAndSales() throws Exception {
        List<GameSound> sounds=events(()->{
            Fighter p=new Fighter("John","",100);
            int money=Shop.buyQuantity(p,100,16,2);
            Shop.buyQuantity(p,money,8,2); // Cannot afford this; no sound.
            Shop.buyQuantity(p,money,16,0); // Cancelled quantity; no sound.
            Shop.sell(p,new Scanner("2 0"),money);
        });
        assertEquals(Arrays.asList(GameSound.TRADE,GameSound.ERROR,GameSound.TRADE),sounds);
    }
    @Test public void attackSoundsFollowWeaponAndInvalidAttacksStaySilent() throws Exception {
        List<GameSound> sounds=events(()->{
            Fighter p=new Fighter("John","",100);Zombie enemy=new Zombie("Target","",1000);
            Survivor.attack(p,enemy,new Random(1));
            Weapons knife=(Weapons)Shop.createItem(4);p.addItem(knife);p.equipWeapon(knife);Survivor.attack(p,enemy,new Random(1));
            Weapons gun=(Weapons)Shop.createItem(9);p.addItem(gun);p.equipWeapon(gun);
            Survivor.attack(p,enemy,new Random(1)); // Empty gun.
            p.addItem(new Ammunition(AmmoType.REVOLVER));Survivor.attack(p,enemy,new Random(1));
            gun.wear(100);Survivor.attack(p,enemy,new Random(1)); // Broken gun.
        });
        assertEquals(Arrays.asList(GameSound.BLUNT,GameSound.BLADE,GameSound.SHOT),sounds);
    }
    @Test public void reloadZombieAndTravelAreDistinctAndBoardMovementHasNoFootsteps() throws Exception {
        List<GameSound> sounds=events(()->{
            Fighter p=new Fighter("John","",100);Weapons gun=(Weapons)Shop.createItem(9);
            p.addItem(gun);p.equipWeapon(gun);p.addItem(new Ammunition(AmmoType.REVOLVER));
            p.addItem(new Ammunition(AmmoType.REVOLVER)); // Spare rounds for an actual reload.
            TacticalBattle b=new TacticalBattle(p,1,new Random(2),Arrays.asList(new Zombie("Target","",500)));
            Arrays.fill(b.obstacles,null);b.playerCell=56;b.units.get(0).cell=59;
            assertFalse(b.reload());assertTrue(b.attack());assertTrue(b.reload());
            assertTrue(b.move(57));b.units.get(0).cell=58;b.strike(b.units.get(0),false);
            Survivor.travel(p,"Возвращение домой",5);
        });
        assertEquals(Arrays.asList(GameSound.SHOT,GameSound.RELOAD,GameSound.ZOMBIE,GameSound.CONCRETE_STEPS),sounds);
    }
    @Test public void woundsDeathsAndScreamsAreDiscreteNotRenderingEvents() throws Exception {
        List<GameSound> sounds=events(()->{
            Fighter p=new Fighter("John","",100);GameIO.player(p);p.takeDamage(5);p.takeDamage(5);
            Zombie z=new Zombie(1,ZombieType.NORMAL);z.takeDamage(10000);z.takeDamage(10000);
            TacticalBattle b=new TacticalBattle(p,1,new Random(4),Arrays.asList(new Zombie(1,ZombieType.SCREAMER)));
            Arrays.fill(b.obstacles,null);b.playerCell=56;b.units.get(0).cell=3;b.enemyTurn();
        });
        assertEquals(2,Collections.frequency(sounds,GameSound.HURT));assertEquals(1,Collections.frequency(sounds,GameSound.ZOMBIE_DEATH));assertEquals(1,Collections.frequency(sounds,GameSound.SCREAM));
    }
    @Test public void searchingCollectingAndQuestTransitionsEmitOnce() throws Exception {
        List<GameSound> sounds=events(()->{
            Fighter p=new Fighter("John","",100);SearchMode.CAREFUL.begin(p,new WorldStories());
            SurvivalSystems.collect(p,Arrays.asList(Shop.createItem(16)),new Scanner("1"));
            p.getUpgrades().radio=1;assertTrue(p.getUpgrades().accept());assertFalse(p.getUpgrades().accept());
            p.addItem(Shop.createItem(1));p.addItem(Shop.createItem(1));assertTrue(p.getUpgrades().deliver(p));assertFalse(p.getUpgrades().deliver(p));
        });
        assertEquals(1,Collections.frequency(sounds,GameSound.SEARCH));assertEquals(1,Collections.frequency(sounds,GameSound.BAG));assertEquals(2,Collections.frequency(sounds,GameSound.QUEST));
        assertEquals(GameSound.EARTH_STEPS,GameIO.footsteps(2));assertEquals(GameSound.CONCRETE_STEPS,GameIO.footsteps(15));
    }
}
