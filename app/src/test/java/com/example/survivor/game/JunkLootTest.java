package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class JunkLootTest {
 @Test public void allProvidedItemsAreUniqueAndFirstPricesAreCorrect(){
  Set<String> names=new HashSet<>();for(JunkLoot.Entry[] group:JunkLoot.CATEGORIES){assertEquals(20,group.length);for(JunkLoot.Entry e:group)assertTrue(e.name,names.add(e.name));}
  assertEquals(200,names.size());assertEquals(0,JunkLoot.CATEGORIES[0][1].price);assertEquals(8,JunkLoot.CATEGORIES[0][19].price);
  assertEquals("0,5",new JunkLoot.JunkItem(JunkLoot.CATEGORIES[1][14]).shownPrice());
 }
 @Test public void countDistributionAndKitchenExceptionAreCorrect(){
  int[] n=new int[5],k=new int[6];Random r=new Random(1977);
  for(int i=0;i<100000;i++){n[JunkLoot.count(r,false)]++;k[JunkLoot.count(r,true)]++;}
  int[] expected={15000,35000,30000,15000,5000};
  for(int i=0;i<5;i++)assertTrue(i+": "+n[i],Math.abs(n[i]-expected[i])<700);
  assertEquals(0,k[1]);assertTrue(k[5]>4300&&k[5]<5700);
 }
 @Test public void contextsPreferActualObjectAndCheapItemsDominate(){
  assertEquals(0,JunkLoot.category("Кухня","Стол и посуда"));assertEquals(1,JunkLoot.category("Ванная комната","Шкаф"));
  assertEquals(2,JunkLoot.category("Спальня","Тумбочка"));assertEquals(3,JunkLoot.category("Касса магазина","Ящик"));
  Random r=new Random(3);int cheap=0,thermos=0;for(int i=0;i<10000;i++){ItemInfo item=JunkLoot.pick(0,r);if(item.getPrice()<=1)cheap++;if(item.getName().equals("Металлический термос"))thermos++;}
  assertTrue(cheap>thermos*10);
 }
 @Test public void oldSaveCompatibleOrdinaryItemAndIconExclusion() throws Exception {
  ItemInfo item=JunkLoot.pick(1,new Random(2));java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();new java.io.ObjectOutputStream(out).writeObject(item);
  ItemInfo restored=(ItemInfo)new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(out.toByteArray())).readObject();
  assertEquals(item.getName(),restored.getName());assertTrue(JunkLoot.isJunkText("Забрать: "+restored.getName()));
 }
}