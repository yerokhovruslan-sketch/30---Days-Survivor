package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;
public class PharmacyWarehouseTest {
 private Random safe(){return new Random(){@Override public int nextInt(int bound){return bound-1;}};}
 @Test public void safeLootKeyAndCluesWorkWithoutClinicAndSurviveSave() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);PharmacyWarehouse a=w.pharmacy();Location loc=l.getLocation(11);
  a.visit(p,loc,w,new Scanner("0 2 1 2 0 1 1 2 3 0 3 1 3 4 5 6 0 4 1 2 3 4 0 0"),safe());
  assertTrue(a.frontBandage);assertTrue(a.keyTaken);assertTrue(a.journalRead);assertTrue(a.coldOpen);assertTrue(a.coldManifest);assertFalse(a.journalConnection);assertFalse(a.coldConnection);
  assertTrue(a.truckDocs);assertTrue(a.dispatchDocs);assertTrue(a.notice);assertTrue(w.journal().stream().anyMatch(x->x.contains("городской больнице")));
  assertTrue(p.getInventory().stream().anyMatch(x->x.getName().equals(PharmacyWarehouse.KEY)));
  long bandages=p.getInventory().stream().filter(x->x.getName().equals("Бинт")).count();assertEquals(3,bandages);
  File file=Files.createTempFile("pharmacy", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave save=GameSave.load(file);PharmacyWarehouse saved=save.locations.getStories().pharmacy();saved.visit((Fighter)save.player,save.locations.getLocation(11),save.locations.getStories(),new Scanner("2 1 0 3 3 0 0"),safe());assertEquals(bandages,save.player instanceof Fighter?((Fighter)save.player).getInventory().stream().filter(x->x.getName().equals("Бинт")).count():0);}finally{file.delete();}
 }
 @Test public void clinicClueIsOptionalAndConnectedInEitherOrder(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);PharmacyWarehouse a=w.pharmacy();w.clinic().cabinetManifest=true;
  a.visit(p,l.getLocation(11),w,new Scanner("0 1 2 3 0 3 1 6 0 0"),safe());
  assertTrue(a.journalConnection);assertTrue(a.coldConnection);assertTrue(w.journal().stream().anyMatch(x->x.contains("запись в поликлинике не ошибалась")));
  assertTrue(SurvivalSystems.protectedRadioPart(p,p.getInventory().stream().filter(x->x.getName().equals(PharmacyWarehouse.KEY)).findFirst().orElseThrow()));
 }
 @Test public void crowbarOpensColdWithoutKey(){Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(27));PharmacyWarehouse a=w.pharmacy();a.visit(p,l.getLocation(11),w,new Scanner("0 3 2 6 0 0"),safe());assertTrue(a.coldOpen);assertTrue(a.coldManifest);assertFalse(a.keyTaken);}
 @Test public void oldLootMigratesWithoutDuplicatingAndLocationRemainsOpen(){
  Locations l=new Locations();Location old=l.getLocation(11);Location.SearchPoint oldCold=new Location.SearchPoint("Холодильная камера","",true,true);oldCold.markSearched();Location.SearchPoint oldFront=new Location.SearchPoint("Основной склад","",false,false);oldFront.markSearched();old.setSearchPoints(Arrays.asList(oldFront,oldCold,new Location.SearchPoint("Бухгалтерия","",false,false)));old.setState(LocationState.LOCKED);
  Location current=l.getLocation(11);assertEquals(LocationState.OPEN,current.getState());assertEquals(4,current.getSearchPoints().size());assertFalse(current.isDepleted());
  PharmacyWarehouse a=l.getStories().pharmacy();a.visit(new Fighter("John","",100),current,l.getStories(),new Scanner("0 0"),safe());assertTrue(a.frontBandage);assertTrue(a.coldBandage1&&a.coldBandage2&&a.coldKit);assertFalse(a.coldManifest);
 }
}
