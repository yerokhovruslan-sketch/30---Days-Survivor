package com.example.survivor.game;
import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
public class VehicleFootprintTest {
 private TacticalBattle battle(){TacticalBattle b=new TacticalBattle(new Fighter("John","",100),1,new Random(4),Collections.emptyList());Arrays.fill(b.obstacles,null);b.playerCell=56;b.units.clear();return b;}
 @Test public void vehicleOccupiesEveryCellAndBlocksFire(){
  TacticalBattle b=battle();assertTrue(b.placeObstacle(24,TacticalBattle.Obstacle.CAR));
  assertFalse(b.free(24));assertFalse(b.free(25));assertEquals(2,b.obstacleSpan(25));assertEquals(1,b.obstaclePart(25));
  assertFalse(b.clearShot(16,32));assertFalse(b.clearShot(17,33));
  assertTrue(b.placeObstacle(34,TacticalBattle.Obstacle.BUS));for(int i=34;i<38;i++){assertFalse(b.free(i));assertEquals(4,b.obstacleSpan(i));assertEquals(i-34,b.obstaclePart(i));}
  assertFalse(b.placeObstacle(7,TacticalBattle.Obstacle.CAR));assertFalse(b.placeObstacle(38,TacticalBattle.Obstacle.BUS));
 }
 @Test public void footprintSurvivesSaveAndBlocksMovementAfterReturn() throws Exception {
  TacticalBattle b=battle();assertTrue(b.placeObstacle(17,TacticalBattle.Obstacle.CAR));assertTrue(b.placeObstacle(35,TacticalBattle.Obstacle.BUS));
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(b);
  TacticalBattle restored=(TacticalBattle)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();restored.prepareReturn();
  assertEquals(2,restored.obstacleSpan(18));assertEquals(4,restored.obstacleSpan(38));assertFalse(restored.free(38));
 }
 @Test public void generatedBoardsKeepVehiclesAndReachableGround(){
  for(int seed=0;seed<200;seed++){TacticalBattle b=new TacticalBattle(new Fighter("John","",100),1,new Random(seed),Collections.emptyList());assertTrue(b.obstacles[b.playerCell]==null);
   for(int i=0;i<64;i++)if(b.obstacles[i]==TacticalBattle.Obstacle.CAR||b.obstacles[i]==TacticalBattle.Obstacle.BUS)assertEquals(b.obstacles[i]==TacticalBattle.Obstacle.CAR?2:4,b.obstacleSpan(i));}
 }
}
