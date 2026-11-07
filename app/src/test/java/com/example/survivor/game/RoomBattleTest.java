package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;
public class RoomBattleTest {
 @Test public void locationAndContextSelectIndoorWithoutPaintingCourtyards(){
  assertTrue(TacticalBattle.isWoodInterior(5,"Лестничный пролёт"));
  assertTrue(TacticalBattle.isWoodInterior(20,"Охотничья избушка"));
  assertFalse(TacticalBattle.isWoodInterior(5,"Двор возле подъезда"));
  assertFalse(TacticalBattle.isWoodInterior(1,"Подвальный вход"));
 }
 @Test public void roomsKeepPassagesAndDontOverlapPeopleAcrossManyLayouts() throws Exception {
  for(int seed=0;seed<100;seed++){
   Fighter p=new Fighter("John","",100);TacticalBattle b=new TacticalBattle(p,1,new Random(seed),List.of(new Zombie(1,ZombieType.NORMAL)));
   b.playerCell=56;b.units.get(0).cell=5;b.civilians.add(new TacticalBattle.Civilian("Семья",50,3));b.buildRoom("Кухня");
   assertTrue(b.woodFloor);assertTrue(b.connected());assertNull(b.obstacles[56]);assertNull(b.obstacles[5]);assertNull(b.obstacles[3]);
   assertEquals(TacticalBattle.Obstacle.FRIDGE,b.obstacles[1]);assertTrue(b.snapshot().woodFloor);
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(b);
   TacticalBattle copy=(TacticalBattle)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();assertTrue(copy.woodFloor);assertEquals(b.obstacles[1],copy.obstacles[1]);
  }
 }
 @Test public void forestUsesGrassAndIndoorWallKeepsDoorway() throws Exception {
  assertTrue(TacticalBattle.isGrassland(2,""));assertTrue(TacticalBattle.isGrassland(22,"Берег озера"));assertFalse(TacticalBattle.isGrassland(5,""));
  Fighter p=new Fighter("John","",100);TacticalBattle b=new TacticalBattle(p,1,new Random(4),Collections.emptyList());b.units.clear();b.playerCell=56;b.buildRoom("Кухня");
  assertTrue(b.woodFloor);assertFalse(b.grassFloor);assertTrue(b.connected());assertEquals(TacticalBattle.Obstacle.ROOM_WALL,b.obstacles[32]);
  assertNull(b.obstacles[35]);assertNull(b.obstacles[36]);assertFalse(b.clearShot(24,40));assertTrue(b.clearShot(27,43));
  b.grassFloor=true;b.woodFloor=false;assertTrue(b.snapshot().grassFloor);
  ByteArrayOutputStream out=new ByteArrayOutputStream();new ObjectOutputStream(out).writeObject(b);
  TacticalBattle copy=(TacticalBattle)new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();assertTrue(copy.grassFloor);assertEquals(TacticalBattle.Obstacle.ROOM_WALL,copy.obstacles[32]);
 }
}
