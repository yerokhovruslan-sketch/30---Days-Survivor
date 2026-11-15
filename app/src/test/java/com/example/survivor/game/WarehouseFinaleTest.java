package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Scanner;
import static org.junit.Assert.*;

public class WarehouseFinaleTest {
 @Test public void lonePlayerCanLeaveAndEndingSurvivesSave() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);
  assertTrue(w.warehouseFinale().visit(p,w,new Scanner("0 7 1 0")));
  File f=Files.createTempFile("warehouse-finale", ".dat").toFile();
  try{GameSave.save(f,p,0,l);assertTrue(GameSave.load(f).locations.getStories().warehouseFinale().evacuated);}finally{f.delete();}
 }
 @Test public void flightHasGoneByDay31AndCannotBeTaken(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);
  w.advanceMinutes(30*1440);assertEquals(31,w.day());
  assertFalse(w.warehouseFinale().visit(p,w,new Scanner("0")));
  assertTrue(w.warehouseFinale().missedFlight);assertFalse(w.warehouseFinale().evacuated);
 }
 @Test public void radioNoticeOnlyIfRadioWorks(){
  WorldStories w=new WorldStories();Fighter p=new Fighter("John","",100);w.advanceMinutes(30*1440);
  w.warehouseFinale().announceDeparture(w,p);assertFalse(w.warehouseFinale().departureAnnounced);
  p.getUpgrades().radio=1;w.warehouseFinale().announceDeparture(w,p);assertTrue(w.warehouseFinale().departureAnnounced);
 }
 @Test public void nightDocumentsPersistWhenLeaving() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);w.advanceMinutes(12*60);
  assertFalse(w.warehouseFinale().visit(p,w,new Scanner("1 0 0 1")));
  assertTrue(w.warehouseFinale().registryFound);assertTrue(w.warehouseFinale().nightTalk);
  assertTrue(w.journal().stream().anyMatch(s->s.contains("Убежище №6")));
  File f=Files.createTempFile("night-finale", ".dat").toFile();try{GameSave.save(f,p,0,l);WarehouseFinale loaded=GameSave.load(f).locations.getStories().warehouseFinale();assertTrue(loaded.registryFound);assertTrue(loaded.nightTalk);assertFalse(loaded.perimeterEnding);}finally{f.delete();}
 }
 @Test public void nightCanEndByOpeningPerimeter(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);w.advanceMinutes(12*60);
  assertTrue(w.warehouseFinale().visit(p,w,new Scanner("1 0 0 2 1 0")));
  assertTrue(w.warehouseFinale().perimeterEnding);assertFalse(w.warehouseFinale().evacuated);
 }
}
