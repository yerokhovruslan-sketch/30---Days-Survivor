package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;
public class SemenStoryTest {
 private long count(Fighter p,String name){return p.getInventory().stream().filter(x->x.getName().equals(name)).count();}
 @Test public void wholeStoryUsesExistingCarsAndApartmentAndPersists() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(27));
  w.courtyard().visit(p,l.getLocation(1),w,new Scanner("2 1 0 1 0 0 0"),new Random(1));SemenStory s=w.semen();assertTrue(s.semen_met);assertEquals(1,s.firstReaction);assertTrue(s.semen_quest_started);
  w.apartment().visit(p,l.getLocation(5),w,new Scanner("2 2 0 4 1 6 1 2 0 0 0"),new Random(1));assertTrue(s.red_scarf_found&&s.lena_note_found);assertEquals(1,count(p,SemenStory.SCARF));assertEquals(1,count(p,SemenStory.NOTE));
  File file=Files.createTempFile("semen-story", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave saved=GameSave.load(file);WorldStories w2=saved.locations.getStories();Fighter p2=(Fighter)saved.player;SemenStory s2=w2.semen();assertTrue(s2.semen_quest_started&&s2.lena_note_found);
   w2.courtyard().visit(p2,saved.locations.getLocation(1),w2,new Scanner("2 5 2 2 0 6 1 2 3 0 7 0 0"),new Random(1));
   assertTrue(s2.semen_story_completed&&s2.returnedNote&&s2.bonusTaken);assertEquals(2,s2.answer);assertEquals(0,count(p2,SemenStory.SCARF));assertEquals(0,count(p2,SemenStory.NOTE));assertTrue(s2.rewardFood&&s2.rewardWater&&s2.rewardParts);assertTrue(w2.journal().stream().anyMatch(x->x.contains("ящик за красной машиной")||x.contains("Ящик за красной машиной")));
   long before=count(p2,"Изолента");w2.courtyard().visit(p2,saved.locations.getLocation(1),w2,new Scanner("2 7 0 0"),new Random(1));assertEquals(before,count(p2,"Изолента"));
  }finally{file.delete();}
 }
 @Test public void scarfOnlyHasNoBonusAndNoteCanBeLeft(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);SemenStory s=w.semen();s.semen_met=true;s.semen_quest_started=true;
  s.wardrobe(p,w,new Scanner("1 0"));assertTrue(s.red_scarf_found);assertFalse(s.lena_note_found);s.talk(p,w,new Scanner("1"));assertTrue(s.semen_story_completed);assertFalse(s.returnedNote);assertFalse(s.bonusAvailable());assertTrue(s.rewardAvailable());
 }
 @Test public void deferEmotionalReplyAcrossSave() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);SemenStory s=w.semen();s.semen_met=true;s.semen_quest_started=true;s.wardrobe(p,w,new Scanner("1 2 0"));s.talk(p,w,new Scanner("2 0"));assertTrue(s.pendingResponse);assertFalse(s.semen_story_completed);
  File file=Files.createTempFile("semen-defer", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave saved=GameSave.load(file);SemenStory loaded=saved.locations.getStories().semen();loaded.talk((Fighter)saved.player,saved.locations.getStories(),new Scanner("5 0"));assertTrue(loaded.semen_story_completed);assertFalse(loaded.pendingResponse);assertEquals(5,loaded.answer);assertTrue(loaded.bonusAvailable());}finally{file.delete();}
 }
 @Test public void hostilityFirstAllowsReturnSecondMakesHimLeave(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);SemenStory s=w.semen();
  w.courtyard().visit(p,l.getLocation(1),w,new Scanner("2 3 0 0 0"),new Random(1));assertTrue(s.semen_met);assertFalse(s.semen_trust_lost);assertEquals(1,s.hostileResponses);
  w.courtyard().visit(p,l.getLocation(1),w,new Scanner("2 5 3 0 0 0"),new Random(1));assertTrue(s.semen_trust_lost);assertFalse(s.semen_quest_started);assertTrue(w.journal().stream().anyMatch(x->x.contains("Если проснёшься раньше меня")));
 }
 @Test public void abandonedScarfIsNotDuplicated(){
  WorldStories w=new WorldStories();Fighter p=new Fighter("John","",100);SemenStory s=w.semen();s.semen_quest_started=true;s.wardrobe(p,w,new Scanner("1 0"));p.getInventory().removeIf(x->x.getName().equals(SemenStory.SCARF));s.wardrobe(p,w,new Scanner("1 0"));assertEquals(0,count(p,SemenStory.SCARF));assertFalse(s.semen_scarf_returned);assertFalse(s.semen_story_completed);
 }
}
