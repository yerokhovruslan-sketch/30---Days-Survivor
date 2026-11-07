package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.Assert.*;
public class CityHospitalTest {
 private Random safe(){return new Random(){@Override public int nextInt(int bound){return bound-1;}};}
 @Test public void safeEntryCodeAndKeyPersistAndDoNotDuplicate() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);CityHospital h=w.hospital();Location loc=l.getLocation(12);
  h.visit(p,loc,w,new Scanner("0 1 1 2 3 4 0 2 1 3 0 4 1 3 4 5 0 0"),safe());
  assertTrue(h.receptionLoot&&h.keyTaken&&h.registry&&h.request&&h.dressingBandage&&h.codeFound&&h.dispatchOpen&&h.route17);
  assertTrue(w.journal().stream().anyMatch(x->x.contains("17 / Б / 04")));assertFalse(w.journal().stream().anyMatch(x->x.contains("Сбор сохранить")||x.contains("отмена выделения колонны")));
  assertEquals(2,p.getInventory().stream().filter(x->x.getName().equals("Бинт")).count());
  File file=Files.createTempFile("hospital", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave saved=GameSave.load(file);CityHospital q=saved.locations.getStories().hospital();assertTrue(q.codeFound);assertTrue(saved.locations.getStories().journal().stream().anyMatch(x->x.contains(CityHospital.CODE)));q.visit((Fighter)saved.player,saved.locations.getLocation(12),saved.locations.getStories(),new Scanner("2 3 0 0"),safe());assertEquals(1,saved.locations.getStories().journal().stream().filter(x->x.contains(CityHospital.CODE)).count());}finally{file.delete();}
 }
 @Test public void personalStoryAndConditionalConnections() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Field field=Schoolyard.class.getDeclaredField("busLists");field.setAccessible(true);field.setBoolean(w.schoolyard(),true);
  Fighter p=new Fighter("John","",100);p.getUpgrades().signalReceived=true;CityHospital h=w.hospital();h.visit(p,l.getLocation(12),w,new Scanner("0 3 1 2 0 1 2 0 4 1 5 0 0"),safe());
  assertTrue(h.schoolConnection&&h.patientNotebook&&h.radioConnection);assertTrue(w.journal().stream().anyMatch(x->x.contains("Машина приезжала утром")));assertTrue(w.journal().stream().anyMatch(x->x.contains("вернулась ли машина позже")));
 }
 @Test public void crowbarAlternativeAndLegacyMigration() {
  Locations l=new Locations();Location old=l.getLocation(12);Location.SearchPoint op=new Location.SearchPoint("Операционная","",false,false);op.markSearched();old.setSearchPoints(Arrays.asList(new Location.SearchPoint("Приёмное отделение","",false,false),op,new Location.SearchPoint("Аптечный блок","",true,true),new Location.SearchPoint("Морг","",false,false)));old.setState(LocationState.LOCKED);
  Location current=l.getLocation(12);assertEquals(LocationState.OPEN,current.getState());assertFalse(current.isDepleted());assertEquals("Перевязочная",current.getSearchPoints().get(1).name);assertTrue(current.getSearchPoints().get(1).isSearched());
  Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(27));CityHospital h=l.getStories().hospital();h.visit(p,current,l.getStories(),new Scanner("0 2 3 0 4 2 5 0 0"),safe());assertTrue(h.dressingBandage&&h.dressingKit&&h.codeFound&&h.dispatchOpen&&h.route17);assertFalse(h.keyTaken);
 }
}
