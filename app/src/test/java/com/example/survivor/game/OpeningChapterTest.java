package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class OpeningChapterTest {
 @Test public void helpAndBackpackPersistAsActualConsequences(){
  Fighter p=new Fighter("John","",100);Locations l=new Locations();
  assertTrue(OpeningChapter.play(p,l,new Scanner("1 0 1 1 1 1 1 1 1 1"),new Random(1)));
  assertTrue(l.getStories().openingChoiceRecorded);assertEquals(1,l.getStories().openingChoice);assertEquals(2,l.getStories().residential().humanity);
  assertTrue(p.getInventory().stream().anyMatch(i->i.getName().equals("Бинт")));
  assertTrue(p.getInventory().stream().anyMatch(i->i.getName().contains("воды")));
  assertTrue(p.getStamina()<100);assertEquals("Дубинка",p.getEquippedWeapon().getName());
 }
 @Test public void timedOutChoiceDoesNotCountAsDeliberateCruelty(){
  Fighter p=new Fighter("John","",100);Locations l=new Locations();
  assertTrue(OpeningChapter.play(p,l,new Scanner("2 0 0 1 1 1 1 1 1 1"),new Random(1)));
  assertEquals(0,l.getStories().openingChoice);assertEquals(0,l.getStories().residential().humanity);
  assertEquals("Металлическая труба",p.getEquippedWeapon().getName());
  assertFalse(p.getInventory().stream().anyMatch(i->i.getName().equals("Пистолет")));
 }
 @Test public void shelterDoorAndCryLeadIntoRadioInOrder() throws Exception {
  File dir=Files.createTempDirectory("opening-door").toFile();BlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();
  try(GameSession session=new GameSession(dir,frames::offer)){
   session.start();GameSession.Frame f=next(frames);assertTrue(session.submit(f.revision,1));
   f=next(frames);assertTrue(session.submit(f.revision,1));f=next(frames);assertTrue(session.submit(f.revision,1));
   f=next(frames);assertEquals("opening_room",f.menu.scene);assertTrue(session.submit(f.revision,1));
   f=next(frames);assertEquals("opening_door",f.menu.scene);assertTrue(session.submit(f.revision,0));
   f=next(frames);assertEquals("opening_wounded",f.menu.scene);assertTrue(session.submit(f.revision,1));
   f=next(frames);assertEquals("opening_help",f.menu.scene);assertTrue(session.submit(f.revision,1));
   f=next(frames);assertEquals("opening_street",f.menu.scene);assertTrue(session.submit(f.revision,1));
   f=next(frames);assertEquals("opening_shelter_door",f.menu.scene);assertTrue(session.submit(f.revision,1));
   f=next(frames);assertEquals("opening_cry",f.menu.scene);assertTrue(session.submit(f.revision,1));
   f=next(frames);assertEquals("opening_shelter",f.menu.scene);assertTrue(session.submit(f.revision,1));
   f=next(frames);assertEquals("opening_radio",f.menu.scene);
  }finally{File[] files=dir.listFiles();if(files!=null)for(File file:files)file.delete();dir.delete();}
 }
 @Test public void snapshotsPreservePreviousProgress() throws Exception {
  File dir=Files.createTempDirectory("survivor-slots").toFile();File main=new File(dir,"survivor-save.dat");
  Fighter p=new Fighter("John","",100);Locations l=new Locations();
  try{GameSave.save(main,p,12,l);SaveSlots.snapshot(main);assertEquals(2,SaveSlots.all(main).size());GameSave.save(main,p,7,l);
   File slot=SaveSlots.all(main).stream().filter(f->!f.equals(main)).findFirst().get();assertEquals(12,GameSave.load(slot).balance);assertEquals(7,GameSave.load(main).balance);
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
 }
 @Test public void choosingBattleStartsThreeEnemiesWithoutOverwritingSave() throws Exception {
  File dir=Files.createTempDirectory("opening-fight").toFile();BlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();
  try(GameSession s=new GameSession(dir,frames::offer)){
   s.start();GameSession.Frame f=next(frames);assertEquals("opening_start",f.menu.scene);assertTrue(s.submit(f.revision,1));
   f=next(frames);assertEquals("Уровень сложности",f.menu.title);assertTrue(s.submit(f.revision,1));
   f=next(frames);assertTrue(s.submit(f.revision,1));f=next(frames);assertEquals("opening_room",f.menu.scene);assertTrue(s.submit(f.revision,2));
   f=next(frames);assertEquals("opening_door",f.menu.scene);assertTrue(s.submit(f.revision,1));
   f=next(frames);assertNotNull(f.menu.board);assertEquals(3,f.status.enemies.size());assertFalse(new File(dir,"survivor-save.dat").exists());
  }finally{File[] files=dir.listFiles();if(files!=null)for(File f:files)f.delete();dir.delete();}
 }
 private static GameSession.Frame next(BlockingQueue<GameSession.Frame> frames)throws Exception{GameSession.Frame f;do{f=frames.poll(5,TimeUnit.SECONDS);assertNotNull(f);}while(f.mode==GameSession.Mode.BUSY);return f;}
}
