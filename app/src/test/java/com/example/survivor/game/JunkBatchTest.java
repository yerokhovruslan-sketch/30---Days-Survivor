package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class JunkBatchTest {
 @Test public void batchCollectionRespectsSelectionAndWeight(){
  Fighter p=new Fighter("John","",100); ItemInfo a=new JunkLoot.JunkItem(JunkLoot.CATEGORIES[0][0]); ItemInfo b=new JunkLoot.JunkItem(JunkLoot.CATEGORIES[2][0]);
  SurvivalSystems.collect(p,Arrays.asList(a,b),new Scanner("1 2 900"));assertTrue(p.getInventory().contains(a));assertTrue(p.getInventory().contains(b));assertEquals(1100,a.getWeightGrams()+b.getWeightGrams());
 }
 @Test public void fractionalSaleRequiresWholeDollars(){
  Fighter p=new Fighter("John","",100);for(int i=0;i<3;i++)p.addItem(new JunkLoot.JunkItem(JunkLoot.CATEGORIES[1][14]));
  int money=Shop.sellFractional(p,new Scanner("1 2 3 900 3 900"),0); assertEquals(1,money);assertEquals(1,p.getInventory().stream().filter(i -> i instanceof JunkLoot.JunkItem).count());
 }
 @Test public void categoriesAndMass(){
  assertEquals(5,JunkLoot.category("Поликлиника", "шкаф"));assertEquals(6,JunkLoot.category("Гараж", ""));assertEquals(8,JunkLoot.category("Карьер", ""));
  assertEquals(2500,new JunkLoot.JunkItem(JunkLoot.CATEGORIES[8][1]).getWeightGrams());
 }
}

