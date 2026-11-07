package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.Files;
import java.util.concurrent.*;
public class DifficultyTest {
 @Test public void rulesAreDifferentAndLockedAndPersisted() throws Exception {
  Fighter easy=new Fighter("John","",100),hard=new Fighter("John","",100);
  easy.chooseDifficulty(1);hard.chooseDifficulty(3);easy.takeDamage(20);hard.takeDamage(20);
  assertEquals(87,easy.getHealth());assertEquals(75,hard.getHealth());
  easy.increaseHunger(10);hard.increaseHunger(10);assertEquals(5,easy.getHunger());assertEquals(15,hard.getHunger());
  try{easy.chooseDifficulty(2);fail();}catch(IllegalStateException expected){}
  java.io.File file=Files.createTempFile("difficulty",".dat").toFile();
  try{GameSave.save(file,easy,0,new Locations());assertEquals(1,GameSave.load(file).player.difficulty());}finally{file.delete();}
  assertEquals(2,new Fighter("Legacy","",100).difficulty());assertEquals(15,Difficulty.loot(1));assertEquals(-10,Difficulty.loot(3));
 }
 @Test public void newGameSelectsDifficultyAndMenusDoNotAdvanceTime() throws Exception {
  BlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();java.io.File dir=Files.createTempDirectory("new-difficulty").toFile();
  try(GameSession s=new GameSession(dir,frames::offer)) {
   s.setForeground(true);s.start();GameSession.Frame f=frames.poll(3,TimeUnit.SECONDS);
   assertEquals("Уровень сложности",f.menu.title);assertTrue(f.menu.hint.contains("изменить сложность нельзя"));
   assertTrue(s.submit(f.revision,1));do{f=frames.poll(3,TimeUnit.SECONDS);assertNotNull(f);}while(f.mode==GameSession.Mode.BUSY);
   assertEquals("Начать: История",f.menu.title);assertTrue(s.submit(f.revision,1));
   do{f=frames.poll(3,TimeUnit.SECONDS);assertNotNull(f);}while(f.mode==GameSession.Mode.BUSY);
   assertEquals(GameSession.Mode.MAIN,f.mode);assertEquals("История",f.difficulty);assertEquals("08:00",f.time);
   assertNull(frames.poll(6,TimeUnit.SECONDS));
   assertTrue(s.submit(f.revision,11));
   do{f=frames.poll(3,TimeUnit.SECONDS);assertNotNull(f);}while(f.mode==GameSession.Mode.BUSY);
   GameSave saved=GameSave.load(new java.io.File(dir,"survivor-save.dat"));Fighter p=(Fighter)saved.player;
   assertEquals(1,p.difficulty());assertEquals(2,p.getInventory().stream().filter(i->i.getName().equals("Галеты")).count());
   assertEquals(1,p.getInventory().stream().filter(i->i.getName().equals("Бинт")).count());assertEquals("08:00",saved.locations.getStories().time());
  }finally{new java.io.File(dir,"survivor-save.dat").delete();dir.delete();}
 }
}
