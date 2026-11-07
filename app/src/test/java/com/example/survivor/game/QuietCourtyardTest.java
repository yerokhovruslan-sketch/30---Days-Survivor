package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;
import static org.junit.Assert.*;
public class QuietCourtyardTest {
 @Test public void discoveriesAndCrowbarSurviveSaveAndAreNotDuplicated() throws Exception {
  Locations locations=new Locations(); Fighter p=new Fighter("John","",100); WorldStories w=locations.getStories();
  w.courtyard().visit(p,locations.getLocation(1),w,new Scanner("1 2 0 2 0 4 0 3 2 0 0"),new Random(1));
  assertTrue(w.journal().stream().anyMatch(x->x.contains("Красная машина")));
  assertEquals(1,p.getInventory().stream().filter(x->x.getName().equals("Лом")).count());
  File f=Files.createTempFile("courtyard", ".dat").toFile();
  try { GameSave.save(f,p,0,locations); GameSave loaded=GameSave.load(f);
   Fighter restored=(Fighter)loaded.player; WorldStories clock=loaded.locations.getStories();
   clock.courtyard().visit(restored,loaded.locations.getLocation(1),clock,new Scanner("3 2 0 0"),new Random(2));
   assertEquals(1,restored.getInventory().stream().filter(x->x.getName().equals("Лом")).count());
  } finally {f.delete();}
 }
 @Test public void leavingBeforeCrowbarKeepsItAvailable() {
  Locations l=new Locations(); Fighter p=new Fighter("John","",100); WorldStories w=l.getStories();
  w.courtyard().visit(p,l.getLocation(1),w,new Scanner("3 1 0 0"),new Random(1));
  assertFalse(p.getInventory().stream().anyMatch(i->i.getName().equals("Лом")));
  w.courtyard().visit(p,l.getLocation(1),w,new Scanner("3 2 0 0"),new Random(1));
  assertTrue(p.getInventory().stream().anyMatch(i->i.getName().equals("Лом")));
 }
}
