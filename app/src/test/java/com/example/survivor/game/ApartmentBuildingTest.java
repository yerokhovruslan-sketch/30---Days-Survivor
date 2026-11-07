package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;
import static org.junit.Assert.*;
public class ApartmentBuildingTest {
 @Test public void keyAndHatchUnlockRoofAndSurviveSave() throws Exception {
  Locations l=new Locations();Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(27));WorldStories w=l.getStories();
  assertFalse(l.isAvailable(6));
  w.apartment().visit(p,l.getLocation(5),w,new Scanner("1 3 0 2 2 0 4 1 4 5 0 7 1 0 0"),new Random(1));
  assertTrue(p.getInventory().stream().anyMatch(i->i.getName().equals(ApartmentBuilding.ATTIC_KEY)));
  assertTrue(w.journal().stream().anyMatch(x->x.contains("Календарь")));
  assertTrue(l.isAvailable(6));
  File f=Files.createTempFile("apartment", ".dat").toFile();
  try {GameSave.save(f,p,0,l);GameSave saved=GameSave.load(f);assertTrue(saved.locations.isAvailable(6));
   Fighter q=(Fighter)saved.player;WorldStories loaded=saved.locations.getStories();
   loaded.apartment().visit(q,saved.locations.getLocation(5),loaded,new Scanner("4 4 0 0"),new Random(1));
   assertEquals(1,q.getInventory().stream().filter(i->i.getName().equals(ApartmentBuilding.ATTIC_KEY)).count());
  }finally{f.delete();}
 }
 @Test public void familyCanBeMetAtDoorAndWaitsAcrossVisits(){
  Locations l=new Locations();Fighter p=new Fighter("John","",100);WorldStories w=l.getStories();
  w.apartment().visit(p,l.getLocation(5),w,new Scanner("2 2 0 5 0 0"),new Random(1));
  assertEquals(1,w.residential().family);assertEquals(4,w.residential().familyDue);
  w.apartment().visit(p,l.getLocation(5),w,new Scanner("5 2 2 0"),new Random(1));
  assertEquals(3,w.residential().family);assertTrue(w.residential().clinicInfo);
 }
}
