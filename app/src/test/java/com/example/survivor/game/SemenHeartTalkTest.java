package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
public class SemenHeartTalkTest {
 @Test public void questAndTimeGate() {
  WorldStories w=new WorldStories(1); SemenStory s=w.semen();
  assertFalse(SemenHeartTalk.available(s,w));
  assertTrue(SemenHeartTalk.hint(s,w).contains("задание"));
  s.semen_story_completed=true; s.pendingResponse=true;
  assertFalse(SemenHeartTalk.available(s,w));
  s.pendingResponse=false; assertFalse(SemenHeartTalk.available(s,w));
  w.advanceMinutes(600); assertTrue(SemenHeartTalk.available(s,w));
  s.semen_trust_lost=true; assertFalse(SemenHeartTalk.available(s,w));
 }
 @Test public void stateSurvivesSerialization() throws Exception {
  SemenStory s=new SemenStory();s.semen_story_completed=true;s.heartStage=3;s.heartAnswers[0]=2;
  ByteArrayOutputStream b=new ByteArrayOutputStream();new ObjectOutputStream(b).writeObject(s);
  SemenStory copy=(SemenStory)new ObjectInputStream(new ByteArrayInputStream(b.toByteArray())).readObject();
  assertEquals(3,copy.heartStage);assertEquals(2,copy.heartAnswers[0]);
  assertTrue(SemenHeartTalk.available(copy,new WorldStories(2)));
  copy.heartStage=5;assertFalse(SemenHeartTalk.available(copy,new WorldStories(2)));
 }
}
