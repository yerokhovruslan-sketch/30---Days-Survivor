package com.example.survivor.game;
import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;
public class ForestStoneTest {
 @Test public void stonesAreFinitePerLocationAndSurviveSerialization() throws Exception {
  Fighter p=new Fighter("John","",100);WorldStories world=new WorldStories();ForestStories forest=world.forest();
  assertEquals(10,forest.collectStones(p,19,world));assertEquals(0,forest.stonesRemaining(19));assertEquals(10,forest.stonesRemaining(21));
  assertEquals(0,forest.collectStones(p,19,world));assertEquals(10,p.getInventory().stream().filter(i->i.getName().equals(Crafting.STONE)).count());
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(world);
  WorldStories saved=(WorldStories)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
  assertEquals(0,saved.forest().stonesRemaining(19));assertEquals(10,saved.forest().stonesRemaining(21));
 }
 @Test public void handCraftRequiresAllMaterialsAndNoBench() {
  Fighter p=new Fighter("John","",100);WorldStories world=new WorldStories();ForestStories forest=world.forest();forest.collectStones(p,19,world);
  assertFalse(Crafting.fieldPellets(p,world));assertEquals(10,p.getInventory().stream().filter(i->i.getName().equals(Crafting.STONE)).count());
  p.addItem(Shop.createItem(42));int before=world.absoluteMinute();assertTrue(Crafting.fieldPellets(p,world));assertEquals(10,p.getAmmoCount(AmmoType.PELLET));assertEquals(before+10,world.absoluteMinute());
  assertFalse(Crafting.fieldPellets(p,world));assertEquals(10,p.getAmmoCount(AmmoType.PELLET));
 }
 @Test public void handCraftLeavesUncollectedStonesWhenBagIsFull(){
  Fighter p=new Fighter("John","",100);WorldStories world=new WorldStories();ForestStories forest=world.forest();
  while(p.getCarryWeight()+100<=Fighter.MAX_WEIGHT && p.addItem(new Tools("Груз","",1,1))){}
  int got=forest.collectStones(p,19,world);assertEquals(10-got,forest.stonesRemaining(19));assertTrue(p.getCarryWeight()<=Fighter.MAX_WEIGHT);
 }
}