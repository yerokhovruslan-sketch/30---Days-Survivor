package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;
import static org.junit.Assert.*;
public class SchoolyardTest {
 @Test public void scoutMakesFenceApproachFaster(){
  Locations l=new Locations();Fighter p=new Fighter("John","",100);WorldStories w=l.getStories();
  w.schoolyard().visit(p,l.getLocation(7),w,new Scanner("1 0"),new Random(1));assertEquals("08:10",w.time());
  Locations m=new Locations();Fighter q=new Fighter("John","",100);WorldStories v=m.getStories();v.schoolyard().scoutSchool(v);
  v.schoolyard().visit(q,m.getLocation(7),v,new Scanner("1 0"),new Random(1));assertEquals("08:05",v.time());
 }
 @Test public void gatesAndBusListsPersistAndDoNotRevealEvacuationOutcome() throws Exception {
  Locations l=new Locations();Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(27));WorldStories w=l.getStories();
  w.schoolyard().visit(p,l.getLocation(7),w,new Scanner("2 1 1 0 3 1 0 0"),new Random(1));
  assertTrue(w.schoolyard().gateOpened());assertTrue(w.schoolyard().busListsRead());
  assertTrue(w.journal().stream().anyMatch(s->s.contains("Группа А")&&s.contains("Группа Б")&&s.contains("Неизвестно")));
  File f=Files.createTempFile("schoolyard", ".dat").toFile();
  try{GameSave.save(f,p,0,l);GameSave loaded=GameSave.load(f);WorldStories q=loaded.locations.getStories();assertTrue(q.schoolyard().gateOpened());assertTrue(q.schoolyard().busListsRead());
   String before=q.time();q.schoolyard().visit((Fighter)loaded.player,loaded.locations.getLocation(7),q,new Scanner("0"),new Random(1));assertEquals(before,q.time());
  }finally{f.delete();}
 }
 @Test public void oldWorldRemainsAccessibleFromResidentialBuilding(){
  Locations l=new Locations();l.visit(1);l.visit(4);l.visit(5);assertTrue(l.isAvailable(7));assertEquals(LocationState.OPEN,l.getLocation(7).getState());
 }
}
