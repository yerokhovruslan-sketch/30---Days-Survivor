package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;
public class ZombieDepartureTest {
 @Test public void deadlinesSurviveSaveAndOnlyApplyAfterOneOrTwoDays() throws Exception {
  TacticalBattle b=new TacticalBattle(new Fighter("John","",100),1,new Random(2),List.of(new Zombie(1,ZombieType.NORMAL)));
  b.scheduleDepartures(480,new Random(){@Override public int nextInt(int bound){return 0;}});
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(b);
  TacticalBattle loaded=(TacticalBattle)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
  loaded.expireDepartures(1919);assertTrue(loaded.hasWaitingZombies());
  loaded.expireDepartures(1920);assertFalse(loaded.hasWaitingZombies());
 }
 @Test public void twoDayDepartureUpdatesMapStateAndClearsEncounter(){
  WorldStories world=new WorldStories();TacticalBattle b=new TacticalBattle(new Fighter("John","",100),1,new Random(2),List.of(new Zombie(1,ZombieType.NORMAL)));
  b.scheduleDepartures(world.absoluteMinute(),new Random(){@Override public int nextInt(int bound){return bound==2?1:0;}});
  world.rememberBattle(1,b);assertTrue(world.waitingZombies(1));
  world.advanceMinutes(2879);assertTrue(world.waitingZombies(1));
  world.advanceMinutes(1);assertFalse(world.waitingZombies(1));assertNull(world.pendingBattle(1));
 }
 @Test public void scriptedAndSomeRandomEnemiesRemain(){
  TacticalBattle scripted=new TacticalBattle(new Fighter("John","",100),1,new Random(2),List.of(new Zombie(1,ZombieType.NORMAL)));
  scripted.scriptedEncounter=true;scripted.scheduleDepartures(0,new Random(){@Override public int nextInt(int bound){return 0;}});scripted.expireDepartures(10000);assertTrue(scripted.hasWaitingZombies());
  TacticalBattle survivor=new TacticalBattle(new Fighter("John","",100),1,new Random(2),List.of(new Zombie(1,ZombieType.NORMAL)));
  survivor.scheduleDepartures(0,new Random(){@Override public int nextInt(int bound){return bound-1;}});survivor.expireDepartures(10000);assertTrue(survivor.hasWaitingZombies());
 }
}