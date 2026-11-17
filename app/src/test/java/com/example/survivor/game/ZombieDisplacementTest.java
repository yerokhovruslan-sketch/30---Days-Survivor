package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ZombieDisplacementTest {
 private TacticalBattle battle(Fighter p){TacticalBattle b=new TacticalBattle(p,1,new Random(42),List.of(new Zombie(1,ZombieType.NORMAL)));Arrays.fill(b.obstacles,null);b.playerCell=27;b.units.get(0).cell=28;b.selected=0;return b;}
 @Test public void swapMovesBothWithoutMovingThroughObstacles(){Fighter p=new Fighter("John","",100);TacticalBattle b=battle(p);assertTrue(b.canSwapWithZombie());assertTrue(b.swapWithZombie());assertEquals(28,b.playerCell);assertEquals(27,b.units.get(0).cell);assertEquals(94,p.getStamina());}
 @Test public void pushUsesStaminaAndRespectsObstacles(){Fighter p=new Fighter("John","",100);TacticalBattle b=battle(p);assertEquals(2,b.pushDistance());assertTrue(b.pushZombie());assertEquals(30,b.units.get(0).cell);assertEquals(92,p.getStamina());assertEquals(1,b.units.get(0).skip);
  p=new Fighter("John","",100);p.spendStamina(50);b=battle(p);assertEquals(1,b.pushDistance());assertTrue(b.pushZombie());assertEquals(29,b.units.get(0).cell);
  p=new Fighter("John","",100);b=battle(p);b.obstacles[29]=TacticalBattle.Obstacle.ROCK;assertTrue(b.canPushZombie());assertTrue(b.canSwapWithZombie());}
 @Test public void pushStopsAtEdgeOrSecondObstacle(){Fighter p=new Fighter("John","",100);TacticalBattle b=battle(p);b.obstacles[30]=TacticalBattle.Obstacle.ROCK;assertTrue(b.pushZombie());assertEquals(29,b.units.get(0).cell);b=battle(new Fighter("John","",100));b.playerCell=30;b.units.get(0).cell=31;assertFalse(b.canPushZombie());assertTrue(b.canSwapWithZombie());}
 @Test public void collisionStunsForRollsBelow65AndConsumesOnlyEnemyTurn(){
  for(int roll:new int[]{0,64,65,99}){
   Fighter p=new Fighter("John","",100);TacticalBattle b=battle(p);b.obstacles[29]=TacticalBattle.Obstacle.ROCK;
   b.random=new Random(){@Override public int nextInt(int bound){return roll%bound;}};
   assertTrue(b.canPushZombie());assertTrue(b.pushZombie());assertEquals(28,b.units.get(0).cell);
   assertEquals(roll<65?2:0,b.units.get(0).skip);
   assertTrue(b.snapshot().effects.stream().anyMatch(e->e.kind.equals("collision")));
   assertEquals(roll<65,b.snapshot().effects.stream().anyMatch(e->e.kind.equals("stun")));
   if(roll<65){int health=p.getHealth();b.enemyTurn();assertEquals(health,p.getHealth());assertEquals(1,b.units.get(0).skip);b.random=new Random(){@Override public int nextInt(int bound){return 64%bound;}};assertTrue(b.attack());b.enemyTurn();assertEquals(health,p.getHealth());assertEquals(0,b.units.get(0).skip);}
  }
 }
 @Test public void pushIntoSecondObstacleChecksStun(){
  TacticalBattle b=battle(new Fighter("John","",100));b.obstacles[30]=TacticalBattle.Obstacle.ROCK;
  b.random=new Random(){@Override public int nextInt(int bound){return 99%bound;}};
  assertTrue(b.pushZombie());assertEquals(29,b.units.get(0).cell);assertEquals(0,b.units.get(0).skip);
 }
 @Test public void bruteMovesOnlyOneCellEvenAtFullStamina(){
  Fighter p=new Fighter("John","",100);TacticalBattle b=new TacticalBattle(p,1,new Random(42),List.of(new Zombie(1,ZombieType.BRUTE)));
  Arrays.fill(b.obstacles,null);b.playerCell=27;b.units.get(0).cell=28;b.selected=0;
  assertEquals(1,b.pushDistance());assertTrue(b.pushZombie());assertEquals(29,b.units.get(0).cell);assertEquals(92,p.getStamina());
 }
 @Test public void bruteDestroysEntireObstacleAndCollisionKeepsStunChance(){
  for(int roll:new int[]{64,65}){
   Fighter p=new Fighter("John","",100);TacticalBattle b=new TacticalBattle(p,1,new Random(42),List.of(new Zombie(1,ZombieType.BRUTE)));
   Arrays.fill(b.obstacles,null);b.playerCell=27;b.units.get(0).cell=28;b.selected=0;
   assertTrue(b.placeObstacle(29,TacticalBattle.Obstacle.CAR));b.obstacleDamage[29]=20;b.obstacleDamage[30]=30;
   b.random=new Random(){@Override public int nextInt(int bound){return roll%bound;}};
   assertTrue(b.canPushZombie());assertTrue(b.pushZombie());assertNull(b.obstacles[29]);assertNull(b.obstacles[30]);
   assertEquals(0,b.obstacleDamage[29]);assertEquals(0,b.obstacleDamage[30]);assertEquals(29,b.units.get(0).cell);
   assertEquals(roll<65?2:0,b.units.get(0).skip);
   assertTrue(b.snapshot().effects.stream().anyMatch(e->e.kind.equals("collision")));
  }
 }
 @Test public void lighterInjuriesApplyDuringBattleOnly() throws Exception {
  java.io.File dir=java.nio.file.Files.createTempDirectory("battle-wounds").toFile();int grazes=0;
  try(GameSession session=new GameSession(dir,frame->{})){
   GameIO.attach(session);try{
    for(int i=0;i<200;i++){Fighter fighter=new Fighter("John","",100);session.player(fighter);session.battle(fighter,List.of(new Zombie(1,ZombieType.NORMAL)),null);fighter.takeDamage(1);if(fighter.lastHitGlanced())grazes++;}
    session.battle(null,null,null);for(int i=0;i<25;i++){Fighter fighter=new Fighter("John","",100);session.player(fighter);fighter.takeDamage(1);assertFalse(fighter.lastHitGlanced());}
   }finally{GameIO.detach();}
  }finally{dir.delete();}assertTrue("battle grazes: "+grazes,grazes>10&&grazes<75);
 }
 @Test public void humanEnemiesCannotBeDisplaced(){Fighter p=new Fighter("John","",100);TacticalBattle b=new TacticalBattle(p,1,new Random(42),List.of(new HostileSurvivor(1,false,new Random(1))));Arrays.fill(b.obstacles,null);b.playerCell=27;b.units.get(0).cell=28;b.selected=0;assertFalse(b.canSwapWithZombie());assertFalse(b.canPushZombie());}
}
