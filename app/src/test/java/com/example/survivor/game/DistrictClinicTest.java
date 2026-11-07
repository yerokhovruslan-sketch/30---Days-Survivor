package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;
public class DistrictClinicTest {
 private Random safe(){return new Random(){@Override public int nextInt(int bound){return bound-1;}};}
 @Test public void registryCanBeRevisitedAndArchiveWorksBeforeRegistry() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);DistrictClinic c=w.clinic();Location loc=l.getLocation(10);
  c.visit(p,loc,w,new Scanner("0 1 1 0 4 2 3 0 0"),safe());
  assertTrue(c.registryLoot);assertTrue(c.patientLists);assertFalse(c.clinic_patient_registry_found);assertFalse(c.archiveConnection);
  assertEquals(1,p.getInventory().stream().filter(x->x.getName().equals("Бинт")).count());
  File file=Files.createTempFile("clinic", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave save=GameSave.load(file);DistrictClinic loaded=save.locations.getStories().clinic();
   loaded.visit((Fighter)save.player,save.locations.getLocation(10),save.locations.getStories(),new Scanner("1 2 3 0 0"),safe());
   assertTrue(loaded.clinic_patient_registry_found);assertTrue(loaded.archiveConnection);assertTrue(loaded.keyJournal);
   assertTrue(save.locations.getStories().journal().stream().anyMatch(x->x.contains("копия документов")));
  }finally{file.delete();}
 }
 @Test public void safeMedicineKeyAndCabinetManifestDoNotDuplicate() {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);DistrictClinic c=w.clinic();Location loc=l.getLocation(10);
  c.visit(p,loc,w,new Scanner("0 1 1 0 2 1 2 3 4 0 3 1 3 4 5 6 0 0"),safe());
  assertTrue(c.keyTaken);assertTrue(c.cabinetOpen);assertTrue(c.cabinetManifest);
  assertEquals(5,p.getInventory().stream().filter(x->x.getName().equals("Бинт")).count());assertEquals(1,p.getInventory().stream().filter(x->x.getName().equals("Аптечка")).count());
  assertTrue(w.journal().stream().anyMatch(x->x.contains("М-24")));assertTrue(SurvivalSystems.protectedRadioPart(p,p.getInventory().stream().filter(x->x.getName().equals(DistrictClinic.CABINET_KEY)).findFirst().orElseThrow()));
  c.visit(p,loc,w,new Scanner("3 3 4 5 6 0 0"),safe());assertEquals(5,p.getInventory().stream().filter(x->x.getName().equals("Бинт")).count());
 }
 @Test public void crowbarAlternativeAndMarinaNote() {
  Locations l=new Locations();WorldStories w=l.getStories();w.residential().family=2;Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(27));DistrictClinic c=w.clinic();
  c.visit(p,l.getLocation(10),w,new Scanner("0 1 2 0 3 2 6 0 0"),safe());
  assertTrue(c.cabinetOpen);assertFalse(c.keyTaken);assertTrue(c.registryMarinaNote);assertTrue(c.cabinetManifest);
 }
 @Test public void entryIsAlwaysOpenAndExhaustionDoesNotBlockRevisit(){
  for(int i=0;i<20;i++){Locations l=new Locations();Location loc=l.getLocation(10);assertEquals(LocationState.OPEN,loc.getState());for(Location.SearchPoint point:loc.getSearchPoints())loc.consumePoint(point);assertFalse(loc.isDepleted());}
 }
}

