package com.example.survivor.game;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.*;
import static org.junit.Assert.*;
public class BodyRecoveryTest {
 private BodyInjury injury(Fighter p,BodyPart part) throws Exception {Method m=Fighter.class.getDeclaredMethod("injury",BodyPart.class);m.setAccessible(true);return (BodyInjury)m.invoke(p,part);}
 @Test public void treatedWoundsHealInGameHoursOnlyOnceBleedingStops() throws Exception {
  Fighter p=new Fighter("John","",100);BodyInjury wound=injury(p,BodyPart.LEFT_ARM);wound.trauma=24;wound.bleeding=4;
  p.treat(BodyPart.LEFT_ARM,new MedicalItem("Бинт",15,20));assertEquals(1,wound.bleeding);p.recoverInjuries(1440);assertEquals(24,wound.trauma);
  p.treat(BodyPart.LEFT_ARM,new MedicalItem("Бинт",15,20));p.recoverInjuries(1439);assertEquals(1,wound.trauma);p.recoverInjuries(1);assertEquals(0,wound.trauma);
 }
 @Test public void lethalPartsAndBleedingWorkWithoutSharedHealth() throws Exception {
  Fighter p=new Fighter("John","",100);injury(p,BodyPart.LEFT_LEG).trauma=100;assertTrue(p.isAlive());
  BodyInjury arm=injury(p,BodyPart.LEFT_ARM);arm.bleeding=4;injury(p,BodyPart.TORSO).trauma=98;
  assertEquals(2,p.sufferBleeding());assertFalse(p.isAlive());
  Fighter head=new Fighter("John","",100);injury(head,BodyPart.HEAD).trauma=100;assertFalse(head.isAlive());
 }
 @Test public void recoveryWorksAcrossClockAndSave() throws Exception {
  Fighter p=new Fighter("John","",100);BodyInjury arm=injury(p,BodyPart.RIGHT_ARM);arm.trauma=10;arm.treated=true;
  Locations loc=new Locations();loc.getStories().bindRecovery(p);loc.getStories().advanceMinutes(90);assertEquals(9,arm.trauma);
  File f=java.nio.file.Files.createTempFile("injury", ".dat").toFile();try{GameSave.save(f,p,0,loc);GameSave save=GameSave.load(f);Fighter loaded=(Fighter)save.player;save.locations.getStories().bindRecovery(loaded);save.locations.getStories().advanceMinutes(30);assertEquals(8,loaded.trauma(BodyPart.RIGHT_ARM));}finally{f.delete();}
 }
 @Test public void legacyHealthMigratesToTorsoWithoutHealing() throws Exception {
  Fighter p=new Fighter("John","",100);Field old=SurvivorInfo.class.getDeclaredField("health");old.setAccessible(true);old.setInt(p,65);
  Field version=Fighter.class.getDeclaredField("bodySystemV2");version.setAccessible(true);version.setBoolean(p,false);
  ByteArrayOutputStream bytes=new ByteArrayOutputStream();new ObjectOutputStream(bytes).writeObject(p);
  Fighter restored=(Fighter)new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();assertTrue(restored.isAlive());assertEquals(35,restored.trauma(BodyPart.TORSO));
 }
 @Test public void bandageTreatsChosenPartDuringTacticalBattle() throws Exception {
  Fighter p=new Fighter("John","",100);BodyInjury arm=injury(p,BodyPart.LEFT_ARM);arm.trauma=24;arm.bleeding=2;p.addItem(new MedicalItem("Бинт",15,20));
  Locations loc=new Locations();java.util.concurrent.LinkedBlockingQueue<GameSession.Frame> frames=new java.util.concurrent.LinkedBlockingQueue<>();File dir=java.nio.file.Files.createTempDirectory("treatment").toFile();
  try(GameSession session=new GameSession(dir,frames::offer,()->{GameIO.mainMenu(p,0,loc);TacticalBattle b=new TacticalBattle(p,1,new java.util.Random(1),java.util.List.of(new Zombie(1,ZombieType.NORMAL)));b.items(GameIO.scanner());})) {
   session.start();GameSession.Frame menu=frames.poll(5,java.util.concurrent.TimeUnit.SECONDS);assertNotNull(menu);assertTrue(session.submit(menu.revision,1));
   boolean sent=false; long limit=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5); while(!sent && System.nanoTime()<limit){GameSession.Frame select=frames.poll(1,java.util.concurrent.TimeUnit.SECONDS);if(select!=null)sent=session.submit(select.revision,1);} assertTrue(sent);
   long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);while(arm.bleeding!=0 && System.nanoTime()<end)Thread.sleep(10);
   assertEquals(0,arm.bleeding);assertTrue(arm.treated);assertEquals(24,arm.trauma);
  }finally{for(File f:java.util.Objects.requireNonNull(dir.listFiles()))f.delete();dir.delete();}
 }
}
