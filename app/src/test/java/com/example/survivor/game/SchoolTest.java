package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;
import static org.junit.Assert.*;
public class SchoolTest {
 @Test public void yardEntranceAndSchoolFindingsSurviveSave() throws Exception {
  Locations l=new Locations();Fighter p=new Fighter("John","",100);WorldStories w=l.getStories();
  assertFalse(l.isAvailable(8));
  w.schoolyard().visit(p,l.getLocation(7),w,new Scanner("1 6 1 0 0"),new Random(1));
  assertTrue(l.isAvailable(8));
  w.school().visit(p,l.getLocation(8),w,new Scanner("1 2 3 0 2 1 3 0 3 1 3 4 0 7 1 3 4 0 4 1 0 0"),new Random(1));
  assertTrue(w.journal().stream().anyMatch(x->x.contains("Группа Б остаётся")));
  assertTrue(w.journal().stream().anyMatch(x->x.contains("аварийного предохранителя")));
  assertTrue(p.getInventory().stream().anyMatch(i->i.getName().equals("Бинт")));
  assertTrue(p.getInventory().stream().anyMatch(i->i.getName().equals("Галеты")));
  File f=Files.createTempFile("school", ".dat").toFile();
  try{GameSave.save(f,p,0,l);GameSave saved=GameSave.load(f);assertTrue(saved.locations.isAvailable(8));
   Fighter q=(Fighter)saved.player;WorldStories sw=saved.locations.getStories();
   sw.school().visit(q,saved.locations.getLocation(8),sw,new Scanner("3 3 0 4 1 0 0"),new Random(2));
   assertEquals(1,q.getInventory().stream().filter(i->i.getName().equals("Бинт")).count());
   assertEquals(1,q.getInventory().stream().filter(i->i.getName().equals("Галеты")).count());
  }finally{f.delete();}
 }
 @Test public void diaryCanBeComparedWithBusListOnLaterReturn(){
  Locations l=new Locations();Fighter p=new Fighter("John","",100);WorldStories w=l.getStories();
  w.school().visit(p,l.getLocation(8),w,new Scanner("2 1 0 0"),new Random(1));
  assertFalse(w.journal().stream().anyMatch(x->x.contains("Дневник и списки автобуса")));
  w.schoolyard().visit(p,l.getLocation(7),w,new Scanner("1 3 1 0 0"),new Random(1));
  w.school().visit(p,l.getLocation(8),w,new Scanner("2 0 0"),new Random(1));
  assertTrue(w.journal().stream().anyMatch(x->x.contains("Дневник и списки автобуса") && x.contains("Неизвестно")));
 }
 @Test public void oldSchoolVisitStillAllowsEntry(){Locations l=new Locations();l.visit(1);l.visit(4);l.visit(5);l.visit(7);l.getStories().schoolyard().visit(new Fighter("John","",100),l.getLocation(7),l.getStories(),new Scanner("1 6 1 0 0"),new Random(1));l.visit(8);assertTrue(l.isAvailable(8));}
}
