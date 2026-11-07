package com.example.survivor.game;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class EarlyLootBalanceTest {
 @Test public void earlyLootAllowsValuablesOnlyOnOnePercentGate() {
  List<ItemInfo> items=Item.lootCatalogue(); Random random=new Random(137); int allowed=0;
  for(int i=0;i<10000;i++) if(Item.eligibleLoot(items,LootProfile.GENERAL,false,true,random).stream().anyMatch(x->x.getName().equals("Солнечная панель")))allowed++;
  assertTrue(allowed>=65 && allowed<=135);
  assertTrue(Item.eligibleLoot(items,LootProfile.GENERAL,false,false,new Random(1)).stream().anyMatch(x->x.getName().equals("Солнечная панель")));
 }
 @Test public void earlyPoolsKeepBasicSuppliesAndNoProfileIsEmpty() {
  List<ItemInfo> items=Item.lootCatalogue();Random noValuables=new Random(){@Override public int nextInt(int bound){return bound-1;}};
  for(LootProfile profile:LootProfile.values()) {
   List<ItemInfo> pool=Item.eligibleLoot(items,profile,true,true,noValuables);
   assertFalse(profile.name(),pool.isEmpty());assertTrue(pool.stream().noneMatch(Item::valuableEarlyItem));
  }
  assertTrue(Item.eligibleLoot(items,LootProfile.FOOD,true,true,noValuables).stream().anyMatch(x->x instanceof Food));
  Locations l=new Locations();for(int n:new int[]{1,2,3,4,5,6,7,8,9})assertTrue(l.getLocation(n).getDifficulty()<=2);
  assertEquals(3,l.getLocation(47).getDifficulty());
 }
}
