package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;
public class ArkadyStoryTest {
 static Fighter fighter(){return new Fighter("John","",100);}
 @Test public void waterIsConsumedOnceAndRewardIsUnique(){Fighter p=fighter();p.addItem(ResidentialStories.waterBottle());WorldStories w=new WorldStories();ArkadyStory a=w.arkady();a.spawned=true;a.spawnPlace=3;assertTrue(a.interact(p,w,new Scanner("1 1 1 1 0"),new Random(1)));assertEquals(5,a.stage);assertNull(ArkadyStory.water(p));assertEquals(3,p.getAmmoCount(AmmoType.REVOLVER));a.interact(p,w,new Scanner("0"),new Random(1));assertEquals(3,p.getAmmoCount(AmmoType.REVOLVER));}
 @Test public void cancellingConfirmationPreservesWaterAndChoice(){Fighter p=fighter();p.addItem(ResidentialStories.waterBottle());WorldStories w=new WorldStories();ArkadyStory a=w.arkady();a.spawned=true;a.stage=2;a.interact(p,w,new Scanner("1 0 0"),new Random());assertEquals(2,a.stage);assertNotNull(ArkadyStory.water(p));assertEquals(0,p.getAmmoCount(AmmoType.REVOLVER));}
 @Test public void refusalKeepsWaterAndEncounterFinished(){Fighter p=fighter();p.addItem(ResidentialStories.waterBottle());WorldStories w=new WorldStories();ArkadyStory a=w.arkady();a.spawned=true;a.stage=2;a.interact(p,w,new Scanner("2 0"),new Random());assertEquals(6,a.stage);assertNotNull(ArkadyStory.water(p));assertEquals(0,p.getAmmoCount(AmmoType.REVOLVER));}
 @Test public void ammunitionIsFiniteAndOnlyRemainingIsLooted(){ArkadyEnemy enemy=new ArkadyEnemy(3);assertEquals(65,enemy.getHealth());enemy.spendAttackAmmo();enemy.spendAttackAmmo();assertEquals(1,enemy.roundsRemaining());enemy.takeDamage(200);List<ItemInfo> loot=enemy.claimLoot();assertEquals(1,loot.stream().filter(i->i instanceof Ammunition).mapToInt(i->((Ammunition)i).getQuantity()).sum());assertTrue(enemy.claimLoot().isEmpty());}
 @Test public void encounterStateAndLocationSurviveSerialization() throws Exception{WorldStories w=new WorldStories();ArkadyStory a=w.arkady();a.spawned=true;a.spawnPlace=3;a.stage=3;ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(w);WorldStories restored=(WorldStories)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();assertTrue(restored.arkady().atPlace(3));assertFalse(restored.arkady().atPlace(1));assertEquals(3,restored.arkady().stage);}
 @Test public void battleAlwaysHasCarCover(){for(int seed=0;seed<100;seed++){TacticalBattle battle=new TacticalBattle(fighter(),1,new Random(seed),List.of(new ArkadyEnemy(3)));BattleLayout.ensureCar(battle,1);assertTrue(Arrays.asList(battle.obstacles).contains(TacticalBattle.Obstacle.CAR));}}
}
