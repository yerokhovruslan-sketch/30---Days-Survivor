package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;
import static org.junit.Assert.*;
public class GroceryStoreTest {
 @Test public void foodCashAndKeyArePersistentAndNotDuplicated() throws Exception {
  Locations l=new Locations(); Fighter p=new Fighter("John","",100); WorldStories w=l.getStories();
  int money=w.grocery().visit(p,l.getLocation(4),w,new Scanner("1 1 2 3 4 0 2 1 2 3 0 0"),new Random(3),0);
  assertTrue(money>=10 && money<=20);
  assertTrue(p.getInventory().stream().anyMatch(x->x.getName().equals(GroceryStore.KEY)));
  assertTrue(p.getInventory().stream().anyMatch(x->x.getName().equals("Галеты")));
  assertTrue(w.journal().stream().anyMatch(x->x.contains("180") && x.contains("200")));
  File f=Files.createTempFile("grocery", ".dat").toFile();
  try {GameSave.save(f,p,money,l);GameSave loaded=GameSave.load(f);Fighter q=(Fighter)loaded.player;
   int after=loaded.locations.getStories().grocery().visit(q,loaded.locations.getLocation(4),loaded.locations.getStories(),new Scanner("1 2 0 2 1 0 0"),new Random(4),loaded.balance);
   assertEquals(money,after);assertEquals(1,q.getInventory().stream().filter(x->x.getName().equals(GroceryStore.KEY)).count());
  }finally{f.delete();}
 }
 @Test public void openEntranceIsNotRandomlyBlocked() {
  for(int i=0;i<25;i++)assertEquals(LocationState.OPEN,new Locations().getLocation(4).getState());
 }
}
