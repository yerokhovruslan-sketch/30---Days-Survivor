package com.example.survivor.game;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class BattleNoiseTest {
    @Test public void searchingDoesNotAccumulateBattleNoise() {
        for(SearchMode mode:SearchMode.values()) {
            Fighter player=new Fighter("John","",100);
            assertTrue(mode.begin(player,new WorldStories()));assertEquals(0,player.getNoise());
        }
    }
    @Test public void victoryClearsNoiseBeforeThePostBattleLootScreen() {
        Fighter player=new Fighter("John","",100);Zombie enemy=new Zombie(1,ZombieType.NORMAL);
        TacticalBattle battle=new TacticalBattle(player,1,new Random(4),Arrays.asList(enemy));
        player.addNoise(60);assertEquals(60,player.getNoise());enemy.takeDamage(999);
        battle.endTurn();assertTrue(battle.won());assertEquals(0,player.getNoise());
    }
    @Test public void interruptedCombatCannotLeakShotNoiseIntoNextEncounter() {
        Fighter player=new Fighter("John","",100);Weapons revolver=(Weapons)Shop.createItem(9);
        player.addItem(revolver);player.equipWeapon(revolver);player.addItem(new Ammunition(AmmoType.REVOLVER));
        Random broken=new Random() {@Override public int nextInt(int bound) {throw new IllegalStateException("Interrupted attack");}};
        try {
            Survivor.fightEnemies(player,new Scanner("1"),1,broken,new ArrayList<>(Arrays.asList(new Zombie(1,ZombieType.NORMAL))));
            fail("Attack was expected to be interrupted");
        } catch(IllegalStateException expected) {assertEquals("Interrupted attack",expected.getMessage());}
        assertEquals(0,player.getNoise());
    }
}
