package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ZombieDisplacementTest {
 private TacticalBattle battle(Fighter p){TacticalBattle b=new TacticalBattle(p,1,new Random(42),List.of(new Zombie(1,ZombieType.NORMAL)));Arrays.fill(b.obstacles,null);b.playerCell=27;b.units.get(0).cell=28;b.selected=0;return b;}
 @Test public void swapMovesBothWithoutMovingThroughObstacles(){Fighter p=new Fighter("John","",100);TacticalBattle b=battle(p);assertTrue(b.canSwapWithZombie());assertTrue(b.swapWithZombie());assertEquals(28,b.playerCell);assertEquals(27,b.units.get(0).cell);assertEquals(94,p.getStamina());}
 @Test public void pushUsesStaminaAndRespectsObstacles(){Fighter p=new Fighter("John","",100);TacticalBattle b=battle(p);assertEquals(2,b.pushDistance());assertTrue(b.pushZombie());assertEquals(30,b.units.get(0).cell);assertEquals(92,p.getStamina());assertEquals(1,b.units.get(0).skip);
  p=new Fighter("John","",100);p.spendStamina(50);b=battle(p);assertEquals(1,b.pushDistance());assertTrue(b.pushZombie());assertEquals(29,b.units.get(0).cell);
  p=new Fighter("John","",100);b=battle(p);b.obstacles[29]=TacticalBattle.Obstacle.ROCK;assertFalse(b.canPushZombie());assertTrue(b.canSwapWithZombie());}
 @Test public void pushStopsAtEdgeOrSecondObstacle(){Fighter p=new Fighter("John","",100);TacticalBattle b=battle(p);b.obstacles[30]=TacticalBattle.Obstacle.ROCK;assertTrue(b.pushZombie());assertEquals(29,b.units.get(0).cell);b=battle(new Fighter("John","",100));b.playerCell=30;b.units.get(0).cell=31;assertFalse(b.canPushZombie());assertTrue(b.canSwapWithZombie());}
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
