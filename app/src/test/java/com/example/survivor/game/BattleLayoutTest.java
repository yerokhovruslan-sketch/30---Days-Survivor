package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;
public class BattleLayoutTest {
 private TacticalBattle board(int seed){TacticalBattle b=new TacticalBattle(new Fighter("John","",100),1,new Random(seed),Collections.emptyList());b.units.clear();b.playerCell=56;b.random=new Random(seed*17L+3);return b;}
 @Test public void generatesConnectedVariedGroupsForCityAndNature(){
  Set<String> city=new HashSet<>(),nature=new HashSet<>();Set<Integer> anchors=new HashSet<>();int withSecond=0,withoutSecond=0,vertical=0,horizontal=0;
  for(int seed=0;seed<300;seed++)for(int area:new int[]{0,2}){
   TacticalBattle b=board(seed);BattleLayout.generate(b,area,4);assertTrue(b.layoutPrimary>=0);assertTrue(b.connected());assertNull(b.obstacles[b.playerCell]);
   int total=0;for(int i=0;i<64;i++)if(b.obstacles[i]!=null){total++;assertNotEquals(b.playerCell,i);if(b.obstacles[i]==TacticalBattle.Obstacle.CAR||b.obstacles[i]==TacticalBattle.Obstacle.BUS){assertTrue(b.obstacleSpan(i)>1);if(b.obstacleIsVertical(i))vertical++;else horizontal++;}}
   assertTrue(total>=4);assertTrue(total<40);if(area==0){city.add(b.layoutProfile);anchors.add(b.layoutPrimary);if(b.layoutHasSecondary)withSecond++;else withoutSecond++;}
   else {nature.add(b.layoutProfile);for(TacticalBattle.Obstacle o:b.obstacles)assertTrue(o!=TacticalBattle.Obstacle.CAR&&o!=TacticalBattle.Obstacle.BUS);}
  }
  assertEquals(3,city.size());assertEquals(3,nature.size());assertTrue(anchors.size()>8);assertTrue(withSecond>30);assertTrue(withoutSecond>30);assertTrue(vertical>0);assertTrue(horizontal>0);
 }
 @Test public void verticalVehiclesSurviveSerializationAndBlockEveryCell()throws Exception{
  TacticalBattle b=board(2);b.clearObstacles();assertTrue(b.placeObstacle(9,TacticalBattle.Obstacle.BUS,true));
  for(int i=0;i<4;i++){int cell=9+i*8;assertFalse(b.free(cell));assertEquals(4,b.obstacleSpan(cell));assertEquals(i,b.obstaclePart(cell));assertTrue(b.obstacleIsVertical(cell));}
  assertFalse(b.clearShot(8,10));BattleBoard visual=b.snapshot();assertTrue(visual.cells.get(17).obstacleVertical);assertEquals(1,visual.cells.get(17).obstaclePart);
  ByteArrayOutputStream data=new ByteArrayOutputStream();new ObjectOutputStream(data).writeObject(b);TacticalBattle loaded=(TacticalBattle)new ObjectInputStream(new ByteArrayInputStream(data.toByteArray())).readObject();loaded.prepareReturn();
  assertTrue(loaded.obstacleIsVertical(33));assertEquals(4,loaded.obstacleSpan(33));loaded.removeObstacle(25);for(int i=0;i<4;i++)assertNull(loaded.obstacles[9+i*8]);
 }
 @Test public void respectsExistingEnemiesCiviliansAndMissionMarkers(){
  for(int seed=0;seed<100;seed++){TacticalBattle b=board(seed);b.units.add(new TacticalBattle.Unit(new Zombie(1,ZombieType.NORMAL),10));b.civilians.add(new TacticalBattle.Civilian("Выживший",50,20));b.markers.put(30,"ДОКУМЕНТЫ");BattleLayout.generate(b,0,1);assertNull(b.obstacles[10]);assertNull(b.obstacles[20]);assertNull(b.obstacles[30]);assertTrue(b.connected());}
 }
}
