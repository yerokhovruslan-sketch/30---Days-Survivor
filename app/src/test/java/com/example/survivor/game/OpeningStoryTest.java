package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.nio.file.Files;
public class OpeningStoryTest {
 @Test public void openingIsOnceAndInspectionCostsNoTime() {
  Fighter p=new Fighter("John","",100);WorldStories w=new WorldStories();
  w.radioOpening(p,new Scanner("0"));int notes=w.journal().size();
  w.radioOpening(p,new Scanner("0"));assertEquals(notes,w.journal().size());assertEquals("08:00",w.time());
  assertTrue(w.journal().stream().anyMatch(t->t.contains("устойчивый приём")));
 }
 @Test public void cluesAreSpacedUniqueAndSurviveSave() throws Exception {
  Locations locations=new Locations();WorldStories w=locations.getStories();Random r=new Random(1);
  assertNull(w.discover(3,"Остановка",r,true));assertTrue(w.hasSeen(201));
  assertNull(w.discover(5,"Подъезд",r,true));assertNull(w.discover(5,"Подъезд",r,true));
  assertNull(w.discover(5,"Подъезд",r,true));assertTrue(w.hasSeen(202));
  java.io.File f=Files.createTempFile("opening",".dat").toFile();
  try {GameSave.save(f,new Fighter("John","",100),0,locations);WorldStories loaded=GameSave.load(f).locations.getStories();
   for(int i=0;i<5;i++)assertNull(loaded.discover(3,"Остановка",r,true));assertTrue(loaded.hasSeen(201));
  }finally{f.delete();}
 }
 @Test public void controllerIsUniqueAndNeedsWorkbenchAndWorkingTool() {
  WorldStories w=new WorldStories();List<ItemInfo> found=w.guaranteedChips(35);assertEquals(1,found.size());assertTrue(w.guaranteedChips(35).isEmpty());
  Fighter p=new Fighter("John","",100);ItemInfo box=found.get(0);p.getStorage().add(box);
  assertFalse(Crafting.dismantle(p,box,w));p.getUpgrades().bench=1;assertFalse(Crafting.dismantle(p,box,w));
  ItemInfo tool=Shop.createItem(20);p.getStorage().add(tool);assertTrue(Crafting.dismantle(p,box,w));
  assertTrue(p.getStorage().contains(tool));assertEquals(1,p.getStorage().stream().filter(i->i.getName().equals("Электрочип")).count());assertFalse(Crafting.dismantle(p,box,w));assertTrue(Crafting.salvage(p,Shop.createItem(49)).isEmpty());
 }
 @Test public void uncollectedEquipmentStaysAtSourceAndNeverTeleportsHome() {
  WorldStories w=new WorldStories();Fighter p=new Fighter("John","",100);
  List<ItemInfo> first=w.guaranteedChips(9);assertEquals(1,first.size());
  w.collectEquipment(9,p,first,new Scanner("0"));assertTrue(p.getStorage().isEmpty());
  List<ItemInfo> again=w.guaranteedChips(9);assertEquals(1,again.size());
  w.collectEquipment(9,p,again,new Scanner("1"));assertTrue(p.getInventory().contains(again.get(0)));assertTrue(p.getStorage().isEmpty());
  assertTrue(w.guaranteedChips(9).isEmpty());assertTrue(w.guaranteedChips(35).isEmpty());
  assertTrue(SurvivalSystems.protectedRadioPart(p,again.get(0)));p.getUpgrades().radio=1;assertFalse(SurvivalSystems.protectedRadioPart(p,again.get(0)));
 }
 @Test public void earlyCluesStopWhenFullSignalIsRecorded() {
  WorldStories w=new WorldStories();w.recordRadioSignal();assertTrue(w.signalKnown());assertNull(w.signalClue(3,"Остановка"));
 }
}
