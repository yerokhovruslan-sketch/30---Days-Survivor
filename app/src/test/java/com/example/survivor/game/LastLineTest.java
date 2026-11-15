package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.Files;
import java.io.File;
import java.util.Random;
import java.util.Scanner;

public class LastLineTest {
 @Test public void routeAndGateStateSurviveSave() throws Exception {
  Locations locations=new Locations();WorldStories world=locations.getStories();Fighter p=new Fighter("John","",100);
  LastLine line=world.lastLine();line.serviceClue=true;line.bypassCleared=true;line.bypassOpen=true;line.raidersDefeated=true;line.powerRestored=true;line.eastGateOpen=true;
  File save=Files.createTempFile("lastline", ".dat").toFile();
  try{GameSave.save(save,p,0,locations);GameSave loaded=GameSave.load(save);LastLine restored=loaded.locations.getStories().lastLine();
   assertTrue(restored.bypassOpen);assertTrue(restored.raidersDefeated);assertTrue(restored.powerRestored);assertTrue(restored.eastGateOpen);assertTrue(loaded.locations.isAvailable(47));
  }finally{save.delete();}
 }
 @Test public void resumedBattlesOnlyResolveMatchingRoute(){
  LastLine line=new LastLine();line.battleWon(LastLine.INFECTED);assertTrue(line.bypassCleared);assertFalse(line.raidersDefeated);assertFalse(line.bypassOpen);
  line.battleWon(LastLine.RAIDERS);assertTrue(line.raidersDefeated);
 }
 @Test public void noCalendarGateAndBypassRequiresTool(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);LastLine line=w.lastLine();
  assertEquals(1,w.day());line.serviceClue=true;line.bypassCleared=true;
  line.visit(p,l,42,w,new Scanner("3 0"),new Random(2));assertFalse(line.bypassOpen);
  p.addItem(Shop.createItem(27));line.visit(p,l,42,w,new Scanner("3 0 0"),new Random(2));assertTrue(line.bypassOpen);assertFalse(line.raidersDefeated);
 }
 @Test public void bypassPreservesRaidersAndCanPowerCircuit(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(27));p.addItem(Shop.createItem(62));p.addItem(Shop.createItem(63));p.addItem(Shop.createItem(64));LastLine line=w.lastLine();line.bypassOpen=true;
  line.visit(p,l,43,w,new Scanner("2 0 3 1 0 2 0 0 4 1 0 0 5 1 0 0 0"),new Random(4));
  assertTrue(line.powerRestored);assertFalse(line.raidersDefeated);assertTrue(w.journal().stream().anyMatch(t->t.contains("Контур А включён")));
 }
 @Test public void noPowerWithoutAccessOrTool(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);LastLine line=w.lastLine();line.raidersDefeated=true;
  line.visit(p,l,43,w,new Scanner("3 0"),new Random(4));assertFalse(line.powerRestored);
 }
 @Test public void poweredGateOpensAndUnlocksWarehouse() {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);w.lastLine().powerRestored=true;
  w.routeAfterArchive().open(p,w,41,new Scanner("1 0 0"));
  assertTrue(w.lastLine().eastGateOpen);assertTrue(l.isAvailable(47));
 }
 @Test public void previouslyExhaustedSubstationRemainsCleared() throws Exception {
  Locations l=new Locations();l.isAvailable(43);Location site=l.getLocation(43);
  for(Location.SearchPoint point:site.getSearchPoints())point.markSearched();
  java.lang.reflect.Field field=Locations.class.getDeclaredField("visited");field.setAccessible(true);
  ((java.util.Set<Integer>)field.get(l)).add(43);((java.util.Set<Integer>)field.get(l)).add(42);
  assertTrue("A depleted substation must remain visitable",LocationPreview.canDepart(l,43));
  LastLine line=l.getStories().lastLine();line.migrate(l);assertTrue(line.raidersDefeated);
 }
 @Test public void missingBlockDoesNotConsumeToolOrRestorePower(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(27));LastLine line=w.lastLine();line.raidersDefeated=true;
  int before=p.getInventory().size();line.visit(p,l,43,w,new Scanner("3 0"),new Random(1));assertFalse(line.powerRestored);assertEquals(before,p.getInventory().size());
 }
 @Test public void allPartsAvailableInLootAndTrade(){
  for(int n=62;n<=64;n++){
   ItemInfo item=Shop.createItem(n);assertNotNull(item);
   assertTrue(Item.lootCatalogue().stream().anyMatch(i->i.getName().equals(item.getName())));
  }
 }
 @Test public void generatorAndOtherCircuitsAreSeparateAndPersistent() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);LastLine line=w.lastLine();line.raidersDefeated=true;
  p.addItem(Shop.createItem(27));p.addItem(Shop.createItem(62));p.addItem(Shop.createItem(63));p.addItem(Shop.createItem(64));
  line.visit(p,l,43,w,new Scanner("3 1 0 2 0 0 4 1 0 0 0"),new Random(1));
  assertTrue(line.panelRepaired);assertTrue(line.generatorRunning);assertFalse(line.powerRestored);
  File file=Files.createTempFile("lastline-stages", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave saved=GameSave.load(file);WorldStories loaded=saved.locations.getStories();LastLine next=loaded.lastLine();Fighter player=(Fighter)saved.player;
   assertTrue(next.panelRepaired);assertTrue(next.generatorRunning);assertFalse(next.powerRestored);
   next.visit(player,saved.locations,43,loaded,new Scanner("5 2 4 0 3 5 0 0"),new Random(2));
   assertTrue(next.circuitB);assertTrue(next.circuitC);assertTrue(next.technicalLoot);assertFalse(next.powerRestored);
   next.visit(player,saved.locations,43,loaded,new Scanner("5 1 0 0 0"),new Random(2));assertTrue(next.powerRestored);
  }finally{file.delete();}
 }
 @Test public void oldPoweredSaveNeedsNoRepeatedRepair(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);LastLine line=w.lastLine();line.raidersDefeated=true;line.powerRestored=true;line.migrated=true;
  int stamina=p.getStamina();line.visit(p,l,43,w,new Scanner("3 0 4 0 5 0 0"),new Random(2));
  assertTrue(line.panelRepaired);assertTrue(line.generatorRunning);assertTrue(line.powerRestored);assertEquals(stamina,p.getStamina());
 }
 @Test public void earlyDiscoveryDoesNotRevealWarehouseUntilDocuments(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);LastLine line=w.lastLine();line.raidersDefeated=true;
  line.visit(p,l,43,w,new Scanner("3 1 0 0 0"),new Random(1));assertTrue(line.panelInspected);assertFalse(line.knowledgeRecorded);
  assertFalse(w.journal().stream().anyMatch(t->t.contains("военного склада")));
  w.routeAfterArchive().cargoFound=true;line.syncKnowledge(w);assertTrue(line.knowledgeRecorded);assertTrue(w.journal().stream().anyMatch(t->t.contains("военного склада")));
 }
}
