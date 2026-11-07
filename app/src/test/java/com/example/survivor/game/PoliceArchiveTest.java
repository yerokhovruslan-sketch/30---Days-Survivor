package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.nio.file.Files;
public class PoliceArchiveTest {
 @Test public void fixedBoardsAreReachableAndNeverOverlapAndReset() {
  Fighter p=new Fighter("John","",100);PoliceArchive.Attempt a=new PoliceArchive.Attempt(p,new Random(1));
  assertEquals(2,a.rooms[0].units.size());assertEquals(3,a.rooms[1].units.size());assertEquals(ZombieType.BRUTE,a.rooms[1].units.get(2).type());
  for(int r=0;r<2;r++){
   TacticalBattle b=a.rooms[r];Set<Integer> seen=new HashSet<>();ArrayDeque<Integer> q=new ArrayDeque<>();int start=r==0?PoliceArchive.ENTRY:PoliceArchive.LOWER_DOOR;q.add(start);seen.add(start);
   while(!q.isEmpty())for(int n:TacticalBattle.neighbours(q.remove()))if(b.obstacles[n]==null && seen.add(n))q.add(n);
   assertTrue(seen.contains(r==0?PoliceArchive.UPPER_DOOR:PoliceArchive.DOCUMENTS));
   Set<Integer> occupied=new HashSet<>();for(TacticalBattle.Unit u:b.units){assertTrue(seen.contains(u.cell));assertTrue(occupied.add(u.cell));assertNull(b.obstacles[u.cell]);}
   int count=b.units.size();b.reinforce();assertEquals(count,b.units.size());
  }
  a.rooms[0].units.get(0).enemy.takeDamage(10000);assertFalse(a.cleared());PoliceArchive.Attempt fresh=new PoliceArchive.Attempt(p,new Random(2));assertTrue(fresh.rooms[0].units.get(0).active());
 }
 @Test public void movementBetweenRoomsKeepsEnemiesAndTheyCanPursue() {
  PoliceArchive.Attempt a=new PoliceArchive.Attempt(new Fighter("John","",100),new Random(3));
  assertFalse(a.changeRoom());a.rooms[0].playerCell=PoliceArchive.UPPER_DOOR;
  assertTrue(a.changeRoom());assertEquals(1,a.room);assertEquals(PoliceArchive.LOWER_DOOR,a.rooms[1].playerCell);
  a.rooms[1].units.get(0).cell=PoliceArchive.LOWER_DOOR;a.rooms[1].playerCell=9;
  a.room=0;a.rooms[0].playerCell=PoliceArchive.ENTRY;a.visited[1]=true;
  TacticalBattle.Unit chasing=a.rooms[1].units.get(0);chasing.alerted=true;a.pursue();assertTrue(a.rooms[0].units.contains(chasing));assertEquals(PoliceArchive.UPPER_DOOR,chasing.cell);
 }
 @Test public void keyCanBeFoundEarlyCannotBeSoldAndGateRequiresCompletedSignal() throws Exception {
  Fighter p=new Fighter("John","",100);Locations world=new Locations();WorldStories w=world.getStories();
  assertTrue(w.findArchiveKey(p,15));assertFalse(w.findArchiveKey(p,15));assertTrue(PoliceArchive.hasKey(p));assertFalse(PoliceArchive.unlocked(p));
  ItemInfo key=p.getInventory().stream().filter(i->i.getName().equals(PoliceArchive.KEY)).findFirst().orElseThrow();assertTrue(SurvivalSystems.protectedRadioPart(p,key));
  p.getUpgrades().radio=1;p.getUpgrades().signalAttempts=2;p.getUpgrades().signalReceived=true;p.getUpgrades().signalStage=1;assertFalse(PoliceArchive.unlocked(p));p.getUpgrades().signalStage=0;assertTrue(PoliceArchive.unlocked(p));assertTrue(SurvivalSystems.protectedRadioPart(p,key));
  w.archiveComplete=true;w.archiveGoal=2;
  java.io.File f=Files.createTempFile("archive-state",".dat").toFile();
  try{GameSave.save(f,p,0,world);GameSave saved=GameSave.load(f);assertTrue(saved.locations.getStories().archiveComplete);assertEquals(2,saved.locations.getStories().archiveGoal);assertTrue(PoliceArchive.hasKey((Fighter)saved.player));assertFalse(saved.locations.getStories().findArchiveKey((Fighter)saved.player,15));}finally{f.delete();}
 }
 @Test public void cabinetRequiresThreeActionsOnItsCellButDoesNotRequireKilling() {
  PoliceArchive.Attempt a=new PoliceArchive.Attempt(new Fighter("John","",1000),new Random(4));
  a.room=1;assertFalse(a.searchCabinet());a.rooms[1].playerCell=PoliceArchive.DOCUMENTS;
  assertTrue(a.searchCabinet());assertEquals(1,a.searchProgress);assertFalse(a.papersCollected);
  assertTrue(a.searchCabinet());assertFalse(a.papersCollected);assertTrue(a.searchCabinet());assertTrue(a.papersCollected);
  assertFalse(a.cleared());assertFalse(a.searchCabinet());assertEquals(4,a.rooms[1].turn);
 }
 @Test public void enemiesDoNotKnowJohnThroughWallsButShotsAlertExistingNeighbours() {
  PoliceArchive.Attempt a=new PoliceArchive.Attempt(new Fighter("John","",100),new Random(5));
  TacticalBattle b=a.rooms[0];Arrays.fill(b.obstacles,null);b.obstacles[1]=TacticalBattle.Obstacle.WALL;b.playerCell=0;b.units.clear();
  TacticalBattle.Unit u=new TacticalBattle.Unit(new Zombie(1,ZombieType.NORMAL),2);b.units.add(u);b.enemyTurn();assertEquals(2,u.cell);assertFalse(u.alerted);
  b.lastActionNoise=25;b.enemyTurn();assertTrue(u.alerted);assertNotEquals(2,u.cell);
  b.lastActionNoise=0;int before=a.rooms[1].units.get(0).cell;a.pursue();assertEquals(before,a.rooms[1].units.get(0).cell);
  b.lastActionNoise=25;a.pursue();assertTrue(a.rooms[1].units.get(0).alerted);
 }
 @Test public void attemptSurvivesLeavingPoliceAndDoorCannotOverlapJohnOrEnemy() {
  WorldStories w=new WorldStories();PoliceArchive.Attempt a=new PoliceArchive.Attempt(new Fighter("John","",100),new Random(6));w.archiveAttempt=a;
  a.rooms[0].units.get(0).enemy.takeDamage(10000);assertFalse(w.archiveAttempt.rooms[0].units.get(0).active());
  a.rooms[0].playerCell=PoliceArchive.UPPER_DOOR;a.rooms[1].units.get(0).cell=PoliceArchive.LOWER_DOOR;assertFalse(a.changeRoom());assertEquals(0,a.room);
  a.rooms[1].units.get(0).cell=9;assertTrue(a.changeRoom());assertEquals(PoliceArchive.LOWER_DOOR,a.rooms[1].playerCell);
  w.leavePoliceLocation();assertSame(a,w.archiveAttempt);assertFalse(w.archiveAttempt.rooms[0].units.get(0).active());
 }
 @Test public void actualGunshotCallsExistingEnemiesWithoutSpawning() {
  Fighter p=new Fighter("John","",100);Weapons gun=(Weapons)Shop.createItem(9);p.addItem(gun);p.equipWeapon(gun);p.addItem(new Ammunition(AmmoType.REVOLVER));
  PoliceArchive.Attempt a=new PoliceArchive.Attempt(p,new Random(7));TacticalBattle b=a.rooms[0];Arrays.fill(b.obstacles,null);b.playerCell=16;b.units.get(0).cell=17;b.selected=0;
  assertTrue(b.attack());assertEquals(25,b.lastActionNoise);a.pursue();assertTrue(a.rooms[1].units.get(0).alerted);
  assertEquals(5,a.rooms[0].units.size()+a.rooms[1].units.size());
 }
}
