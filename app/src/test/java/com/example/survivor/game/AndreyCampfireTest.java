package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class AndreyCampfireTest {
 private ForestStories completed(WorldStories w){ForestStories f=w.forest();f.andrey=3;f.questStarted=true;f.boxState=1;return f;}
 @Test public void onlyIntactBoxAndSuccessfulQuestUnlockDialogue(){
  WorldStories w=new WorldStories();ForestStories f=w.forest();assertFalse(AndreyCampfire.available(f));f.andrey=3;assertFalse(AndreyCampfire.available(f));f.questStarted=true;f.boxState=1;assertTrue(AndreyCampfire.available(f));f.boxOpened=true;assertFalse(AndreyCampfire.available(f));f.boxOpened=false;f.andrey=6;assertFalse(AndreyCampfire.available(f));
 }
 @Test public void choicesAreResumableSavedAndNoReward() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);ForestStories f=completed(w);int count=p.getInventory().size(),start=w.absoluteMinute();
  AndreyCampfire.open(f,p,w,new Scanner("2 0 5 1 0 0"));assertTrue(f.campfireIntro);assertEquals(2,f.campfireIntroChoice);assertEquals(16,f.campfireTopics);assertEquals(1,f.campfireAnswers[4]);assertEquals(8,w.absoluteMinute()-start);assertEquals(count,p.getInventory().size());
  File file=Files.createTempFile("campfire", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave saved=GameSave.load(file);ForestStories f2=saved.locations.getStories().forest();assertEquals(16,f2.campfireTopics);AndreyCampfire.open(f2,(Fighter)saved.player,saved.locations.getStories(),new Scanner("1 2 0 0"));assertEquals(17,f2.campfireTopics);assertEquals(2,f2.campfireAnswers[0]);assertEquals(1,saved.locations.getStories().journal().stream().filter(x->x.contains("Андрей пригласил Джона")).count());}finally{file.delete();}
 }
 @Test public void radioChoiceRequiresHeardBroadcastAndPriorPeopleAreConditional() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);ForestStories f=completed(w);f.campfireIntro=true;
  assertFalse(AndreyCampfire.heardRadio(p,w));AndreyCampfire.open(f,p,w,new Scanner("3 1 0"));assertEquals(0,f.campfireTopics);
  Field flag=WorldStories.class.getDeclaredField("radioOpeningSeen");flag.setAccessible(true);flag.setBoolean(w,true);assertTrue(AndreyCampfire.heardRadio(p,w));
  AndreyCampfire.open(f,p,w,new Scanner("3 1 0 0"));assertEquals(4,f.campfireTopics);assertEquals(1,f.campfireAnswers[2]);
  WorldStories other=new WorldStories();Fighter listener=new Fighter("John","",100);ForestStories next=completed(other);next.campfireIntro=true;AndreyCampfire.open(next,listener,other,new Scanner("4 6 0"));assertEquals(0,next.campfireTopics);
  other.residential().family=2;AndreyCampfire.open(next,listener,other,new Scanner("4 6 0 0"));assertEquals(8,next.campfireTopics);assertEquals(6,next.campfireAnswers[3]);
 }
 @Test public void endingDoesNotPreventUnfinishedTopicsAndIsNotRewardedTwice(){
  WorldStories w=new WorldStories();Fighter p=new Fighter("John","",100);ForestStories f=completed(w);f.campfireIntro=true;int before=w.absoluteMinute(),count=p.getInventory().size();
  AndreyCampfire.open(f,p,w,new Scanner("6 4 0"));assertTrue(f.campfireEnded);assertEquals(4,f.campfireFinishChoice);assertEquals(2,w.absoluteMinute()-before);assertFalse(w.journal().stream().anyMatch(x->x.contains("Судьба брата")));
  AndreyCampfire.open(f,p,w,new Scanner("2 5 0 6 2 0"));assertEquals(2,f.campfireTopics);assertEquals(2,f.campfireFinishChoice);assertEquals(6,w.absoluteMinute()-before);assertEquals(count,p.getInventory().size());assertEquals(1,w.journal().stream().filter(x->x.contains("Джон и Андрей поговорили у костра")).count());assertTrue(w.journal().stream().anyMatch(x->x.contains("Судьба брата неизвестна")));
 }
}
