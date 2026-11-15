package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.*;

public class BalanceRevisionTest {
 @Test public void fractionalNeedsAccumulateEquallyAcrossActionsAndSaves() throws Exception {
  Fighter many=new Fighter("John","",100),one=new Fighter("John","",100);many.chooseDifficulty(1);one.chooseDifficulty(1);
  for(int i=0;i<7;i++)many.increaseHunger(1);one.increaseHunger(7);assertEquals(one.getHunger(),many.getHunger());
  File f=Files.createTempDirectory("balance-save").resolve("save.dat").toFile();
  GameSave.save(f,many,0,new Locations());Fighter loaded=(Fighter)GameSave.load(f).player;
  for(int i=0;i<13;i++){many.increaseHunger(1);loaded.increaseHunger(1);}assertEquals(13,many.getHunger());assertEquals(many.getHunger(),loaded.getHunger());
  Files.deleteIfExists(f.toPath());Files.deleteIfExists(f.getParentFile().toPath());
 }
 @Test public void difficultyCoefficientsAndBattleNeedsAreBounded(){
  assertEquals(13,Difficulty.damage(20,1));assertEquals(20,Difficulty.damage(20,2));assertEquals(24,Difficulty.damage(20,3));
  assertEquals(8,Difficulty.encounterChance(10,1));assertEquals(10,Difficulty.encounterChance(10,2));assertEquals(12,Difficulty.encounterChance(10,3));
  Fighter p=new Fighter("John","",100);TacticalBattle battle=new TacticalBattle(p,1,new Random(1),List.of());
  for(int i=0;i<12;i++)battle.endTurn();assertEquals(3,p.getHunger());assertEquals(3,p.getSleepiness());
 }
 @Test public void lastChanceSavesWithoutSnapshotsAndDeathOnlyRemovesItsOwnAttempt() throws Exception {
  File dir=Files.createTempDirectory("ironman").toFile();File current=new File(dir,"survivor-save.dat");
  Fighter normal=new Fighter("John","",100);GameSave.save(current,normal,0,new Locations());SaveSlots.snapshot(current);File previous=SaveSlots.all(current).stream().filter(f->!f.equals(current)).findFirst().get();
  Fighter hard=new Fighter("John","",100);hard.chooseDifficulty(3);GameSave.save(current,hard,0,new Locations());
  assertEquals(3,GameSave.load(current).player.difficulty());int count=SaveSlots.all(current).size();SaveSlots.snapshot(current);assertEquals(count,SaveSlots.all(current).size());
  try(GameSession session=new GameSession(dir,frame->{})){
   GameIO.attach(session);try{session.preparePrologue(hard,new Locations());session.finishPrologue();for(int i=0;i<100 && hard.isAlive();i++)hard.takeDamage(1000);assertFalse(hard.isAlive());}finally{GameIO.detach();}
  }
  assertFalse(current.exists());assertTrue(previous.exists());assertEquals(2,GameSave.load(previous).player.difficulty());
  for(File f:dir.listFiles())f.delete();dir.delete();
 }
}
