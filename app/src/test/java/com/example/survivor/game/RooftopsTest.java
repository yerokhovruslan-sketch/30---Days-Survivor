package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;
import static org.junit.Assert.*;
public class RooftopsTest {
 @Test public void materialsAreConsumedOnceAndPassageSurvivesSave() throws Exception {
  Locations l=new Locations();Fighter p=new Fighter("John","",100);WorldStories w=l.getStories();
  w.rooftops().visit(p,l.getLocation(6),w,new Scanner("1 2 1 3 3 1 3 3 2 1 0"),new Random(1));
  assertTrue(w.rooftops().passageCleared());
  assertFalse(p.getInventory().stream().anyMatch(i->i.getName().equals("Деревянные доски")||i.getName().equals("Гвозди")));
  assertTrue(p.getInventory().stream().anyMatch(i->i.getName().equals("Медная проволока")));
  File f=Files.createTempFile("rooftops", ".dat").toFile();
  try {GameSave.save(f,p,0,l); GameSave restored=GameSave.load(f); assertTrue(restored.locations.getStories().rooftops().passageCleared());
    Fighter q=(Fighter)restored.player;WorldStories rw=restored.locations.getStories();
    rw.rooftops().visit(q,restored.locations.getLocation(6),rw,new Scanner("3 3 2 1 0"),new Random(1));
    assertEquals(1,q.getInventory().stream().filter(i->i.getName().equals("Медная проволока")).count());
  }finally{f.delete();}
 }
 @Test public void ordinaryRouteDoesNotConsumeResources(){
  Locations l=new Locations();Fighter p=new Fighter("John","",100);WorldStories w=l.getStories();
  w.rooftops().visit(p,l.getLocation(6),w,new Scanner("3 2 0"),new Random(1));
  assertFalse(w.rooftops().passageCleared());assertEquals("08:30",w.time());
 }
}

