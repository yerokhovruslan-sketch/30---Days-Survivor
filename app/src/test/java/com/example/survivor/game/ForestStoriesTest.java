package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;
public class ForestStoriesTest {
 private Random poison(){return new Random(){@Override public int nextInt(int n){return 0;}@Override public boolean nextBoolean(){return true;}};}
 private Random safe(){return new Random(){@Override public int nextInt(int n){return n-1;}@Override public boolean nextBoolean(){return false;}};}
 @Test public void poisonedMushroomsSatisfyHungerAndPoisonPersistsAndExpires() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);w.bindRecovery(p);ForestStories f=w.forest();
  f.forage(p,2,w,poison());WildFood mushroom=(WildFood)p.getInventory().stream().filter(x->x instanceof WildFood).findFirst().orElseThrow();assertTrue(mushroom.poisonous());
  File initial=Files.createTempFile("wild-food", ".dat").toFile();try{GameSave.save(initial,p,0,l);GameSave restored=GameSave.load(initial);WildFood held=(WildFood)((Fighter)restored.player).getInventory().stream().filter(x->x instanceof WildFood).findFirst().orElseThrow();assertTrue(held.poisonous());assertEquals(f.availableAt(2),restored.locations.getStories().forest().availableAt(2));}finally{initial.delete();}
  int before=w.absoluteMinute();f.forage(p,2,w,poison());assertEquals(before,w.absoluteMinute());assertEquals(1,p.getInventory().stream().filter(x->x instanceof WildFood).count());
  p.increaseHunger(40);p.eatFood(mushroom);p.getInventory().remove(mushroom);assertEquals(22,p.getHunger());assertEquals(8,p.trauma(BodyPart.TORSO));assertEquals(360,p.poisoningMinutes());assertTrue(p.staminaCost(10)>10);
  File file=Files.createTempFile("forest-poison", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave saved=GameSave.load(file);Fighter loaded=(Fighter)saved.player;assertEquals(360,loaded.poisoningMinutes());saved.locations.getStories().bindRecovery(loaded);saved.locations.getStories().advanceMinutes(360);assertEquals(0,loaded.poisoningMinutes());assertEquals(8,loaded.trauma(BodyPart.TORSO));loaded.treat(BodyPart.TORSO,new MedicalItem("Бинт",15,20));saved.locations.getStories().advanceMinutes(480);assertEquals(0,loaded.trauma(BodyPart.TORSO));}finally{file.delete();}
 }
 @Test public void andreyQuestAndHeartToHeartPersist() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);ForestStories f=w.forest();int[] wallet={0};
  f.menu(p,l.getLocation(20),20,w,new Scanner("3 1 1"),safe(),wallet);assertEquals(2,f.andrey);assertTrue(f.questStarted);
  f.menu(p,l.getLocation(24),24,w,new Scanner("3 1"),safe(),wallet);assertEquals(1,f.boxState);assertTrue(p.getInventory().stream().anyMatch(i->i.getName().equals(ForestStories.BOX)));
  f.menu(p,l.getLocation(20),20,w,new Scanner("3 1"),safe(),wallet);assertEquals(3,f.andrey);assertEquals(15,wallet[0]);assertFalse(p.getInventory().stream().anyMatch(i->i.getName().equals(ForestStories.BOX)));
  f.menu(p,l.getLocation(20),20,w,new Scanner("3 1 2 0 0 0"),safe(),wallet);assertTrue(f.talked);assertTrue(w.journal().stream().anyMatch(x->x.contains("Пока горит костёр")));
  File file=Files.createTempFile("forest-story", ".dat").toFile();try{GameSave.save(file,p,wallet[0],l);GameSave saved=GameSave.load(file);assertEquals(3,saved.locations.getStories().forest().andrey);assertTrue(saved.locations.getStories().forest().talked);}finally{file.delete();}
 }
 @Test public void openedBoxLosesTrustAndDeadlineCanEndQuest(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);ForestStories f=w.forest();int[] wallet={0};
  f.menu(p,l.getLocation(20),20,w,new Scanner("3 1 1"),safe(),wallet);f.menu(p,l.getLocation(24),24,w,new Scanner("3 2"),safe(),wallet);f.menu(p,l.getLocation(20),20,w,new Scanner("3 1"),safe(),wallet);assertEquals(6,f.andrey);assertEquals(0,wallet[0]);
  Locations other=new Locations();WorldStories ow=other.getStories();ForestStories of=ow.forest();of.menu(p,other.getLocation(20),20,ow,new Scanner("3 1 1"),safe(),wallet);ow.advanceMinutes(5*1440);of.menu(p,other.getLocation(20),20,ow,new Scanner("3 0"),safe(),wallet);assertEquals(5,of.andrey);assertTrue(ow.journal().stream().anyMatch(x->x.contains("Ждал сколько мог")));
 }
 @Test public void campAndCacheAreOneTimeAndForestCanBeRevisited(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);ForestStories f=w.forest();int[] wallet={0};
  f.menu(p,l.getLocation(21),21,w,new Scanner("3"),safe(),wallet);assertTrue(f.campSeen);
  f.menu(p,l.getLocation(20),20,w,new Scanner("4"),safe(),wallet);assertTrue(f.cacheKnown);
  f.menu(p,l.getLocation(19),19,w,new Scanner("3 1 2 3 0"),safe(),wallet);assertTrue(f.cacheOpened&&f.cacheAmmo&&f.cacheBandage);
  Location loc=l.getLocation(19);for(Location.SearchPoint point:loc.getSearchPoints())loc.consumePoint(point);assertFalse(loc.isDepleted());
 }
 @Test public void berriesAreSafeAndStolenSuppliesAreOneTime(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);ForestStories f=w.forest();int[] wallet={0};
  Random berries=new Random(){@Override public int nextInt(int n){return 0;}@Override public boolean nextBoolean(){return false;}};
  f.forage(p,2,w,berries);WildFood food=(WildFood)p.getInventory().stream().filter(x->x instanceof WildFood).findFirst().orElseThrow();assertFalse(food.poisonous());p.increaseHunger(30);p.eatFood(food);assertEquals(18,p.getHunger());assertEquals(0,p.poisoningMinutes());
  f.andrey=7;f.menu(p,l.getLocation(20),20,w,new Scanner("3 1 2 3 0"),safe(),wallet);assertTrue(f.robFood&&f.robWater&&f.robBandage);long count=p.getInventory().size();f.menu(p,l.getLocation(20),20,w,new Scanner("3 1 2 3 0"),safe(),wallet);assertEquals(count,p.getInventory().size());
 }
}
