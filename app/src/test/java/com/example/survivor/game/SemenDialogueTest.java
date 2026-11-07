package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class SemenDialogueTest {
 @Test public void allFiveReactionsAndSixResponsesArePresent(){
  assertEquals(6,SemenDialogue.FIRST.length);assertEquals(7,SemenDialogue.ANSWERS.length);
  for(int i=1;i<SemenDialogue.FIRST.length;i++)assertFalse(SemenDialogue.FIRST[i].isEmpty());
  for(int i=1;i<SemenDialogue.ANSWERS.length;i++)assertFalse(SemenDialogue.ANSWERS[i].isEmpty());
  assertTrue(SemenDialogue.INTRO.contains("Откуда ты знаешь моё имя?"));assertTrue(SemenDialogue.QUESTION.contains("А если мы уже мертвы?"));assertTrue(SemenDialogue.BONUS.contains("ящик сам")||SemenDialogue.BONUS.contains("сам чуть сдвинулся"));
 }
 @Test public void reactionsKeepExistingConsequencesAndSelectedAnswer(){
  for(int choice=1;choice<=5;choice++){
   WorldStories w=new WorldStories();Fighter p=new Fighter("John","",100);SemenStory story=w.semen();story.beginVisit();
   story.firstMeeting(p,w,new Scanner(choice+" 0 0"));assertTrue(story.semen_met);assertEquals(choice,story.firstReaction);assertEquals(choice==3||choice==4?1:0,story.hostileResponses);assertFalse(story.semen_quest_started);
  }
  for(int c=1;c<=6;c++){
   WorldStories w=new WorldStories();Fighter p=new Fighter("John","",100);SemenStory story=w.semen();story.semen_met=true;story.semen_quest_started=true;story.wardrobe(p,w,new Scanner("1 2 0"));story.talk(p,w,new Scanner("2 "+c+" 0"));assertTrue(story.semen_story_completed);assertEquals(c,story.answer);assertTrue(story.bonusAvailable());
  }
 }
}
