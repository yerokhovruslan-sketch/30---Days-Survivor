package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
public class JournalAndNpcCooldownTest {
 @Test public void bothJournalFormatsAndTaskProgress() {
  assertArrayEquals(new String[]{"Шарф","День 1","Текст"}, WorldStories.journalEntry("День 1 — Шарф\nТекст"));
  assertArrayEquals(new String[]{"Записка","День 2 — Двор","Содержимое"}, WorldStories.journalEntry("День 2 — Двор\nЗаписка\nСодержимое"));
  WorldStories world=new WorldStories(); Fighter player=new Fighter("John","",100);
  assertTrue(world.currentTasks(player).get(0).contains("радиостанцию"));
  world.semen().semen_quest_started=true;
  assertTrue(world.currentTasks(player).stream().anyMatch(t->t.contains("шарф")));
  world.semen().semen_story_completed=true;
  assertFalse(world.currentTasks(player).stream().anyMatch(t->t.contains("шарф")));
 }
 @Test public void cooldownSurvivesSaveAndRequiresFullDay() throws Exception {
  WorldStories world=new WorldStories(); SemenStory semen=world.semen(); ForestStories forest=world.forest();
  semen.heartFinishedAt=world.absoluteMinute()+1; forest.campfireFinishedAt=world.absoluteMinute()+1;
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(world);
  WorldStories copy=(WorldStories)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
  assertTrue(copy.semen().heartResting(copy));assertTrue(copy.forest().campfireResting(copy));
  copy.advanceMinutes(1439);assertTrue(copy.semen().heartResting(copy));assertTrue(copy.forest().campfireResting(copy));
  copy.advanceMinutes(1);assertFalse(copy.semen().heartResting(copy));assertFalse(copy.forest().campfireResting(copy));
 }
 @Test public void journalSessionReadsBothEntryFormats() throws Exception {
  Locations locations=new Locations();WorldStories w=locations.getStories();Fighter player=new Fighter("John","",100);
  w.note("Первая записка","Содержание первой");
  java.lang.reflect.Field f=WorldStories.class.getDeclaredField("journal");f.setAccessible(true);
  ((java.util.List<String>)f.get(w)).add("День 2 — Двор\nВторая записка\nСодержание второй");
  java.util.concurrent.LinkedBlockingQueue<GameSession.Frame> frames=new java.util.concurrent.LinkedBlockingQueue<>();
  File directory=java.nio.file.Files.createTempDirectory("journal-test").toFile();
  try(GameSession session=new GameSession(directory,frames::offer,()->{GameIO.mainMenu(player,0,locations);w.showJournal(GameIO.scanner(),player);})){ 
   session.start();GameSession.Frame frame=next(frames);assertEquals("Журнал истории",frame.menu.title);
   frame=choose(session,frames,frame,1);assertEquals("Задачи",frame.menu.title);
   frame=choose(session,frames,frame,0);frame=choose(session,frames,frame,2);assertEquals("Записки",frame.menu.title);
   frame=choose(session,frames,frame,1);assertEquals("Первая записка",frame.menu.title);assertEquals("Содержание первой",frame.menu.cards.get(0).detail);
   frame=choose(session,frames,frame,0);frame=choose(session,frames,frame,2);assertEquals("Вторая записка",frame.menu.title);assertEquals("Содержание второй",frame.menu.cards.get(0).detail);
   frame=choose(session,frames,frame,0);frame=choose(session,frames,frame,0);assertEquals("Журнал истории",frame.menu.title);
   frame=choose(session,frames,frame,0);assertEquals(GameSession.Mode.ENDED,frame.mode);assertFalse(frame.text.contains("Не удалось продолжить"));
  }
 }
 private static GameSession.Frame next(java.util.concurrent.LinkedBlockingQueue<GameSession.Frame> frames) throws Exception {
  GameSession.Frame frame;do{frame=frames.poll(5,java.util.concurrent.TimeUnit.SECONDS);assertNotNull(frame);}while(frame.mode==GameSession.Mode.BUSY);return frame;
 }
 private static GameSession.Frame choose(GameSession session,java.util.concurrent.LinkedBlockingQueue<GameSession.Frame> frames,GameSession.Frame frame,int choice) throws Exception {
  assertTrue(session.submit(frame.revision,choice));return next(frames);
 }
}
