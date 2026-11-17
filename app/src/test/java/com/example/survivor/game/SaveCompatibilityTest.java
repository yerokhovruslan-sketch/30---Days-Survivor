package com.example.survivor.game;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import static org.junit.Assert.*;
public class SaveCompatibilityTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 @Test public void fullGameStateRoundTripsThroughRealLoader() throws Exception {
  File save=new File(temp.getRoot(),"auto.dat");Locations locations=new Locations();WorldStories w=locations.getStories();Fighter p=new Fighter("John","",100);
  p.addItem(new JunkLoot.JunkItem(JunkLoot.CATEGORIES[1][14]));ItemInfo coat=new Tools("Зимняя куртка","",1,70);p.addItem(coat);p.wearClothing(coat);
  p.getUpgrades().pillow=new JunkLoot.JunkItem(JunkLoot.CATEGORIES[2][0]);p.getUpgrades().blanket=new JunkLoot.JunkItem(JunkLoot.CATEGORIES[2][4]);p.getUpgrades().sheet=new JunkLoot.JunkItem(JunkLoot.CATEGORIES[2][3]);p.getUpgrades().fireUntil=900;
  w.arkady().spawned=true;w.arkady().spawnPlace=3;w.arkady().stage=4;w.arkady().enemy=new ArkadyEnemy(3);w.arkady().enemy.spendAttackAmmo();
  w.vera().appeared=true;w.vera().stage=3;w.vera().child();w.vera().enemy=new VeraEnemy();w.chanceMeetings().startForTest(2,3);
  w.semen();w.lastLine();w.warehouseFinale();w.homeSurvivors();w.monsterDens();w.forest();w.hospital();w.pharmacy();w.clinic();w.school();w.schoolyard();w.rooftops();w.apartment();w.grocery();w.courtyard();w.residential();w.refreshMarket();w.archiveAttempt=new PoliceArchive.Attempt(p,new Random(3));
  TacticalBattle battle=new TacticalBattle(p,1,new Random(2),List.of(w.arkady().enemy));w.rememberBattle(3,battle);
  GameSave.save(save,p,33,locations);GameSave loaded=GameSave.load(save);Fighter q=(Fighter)loaded.player;WorldStories r=loaded.locations.getStories();
  assertEquals(33,loaded.balance);assertEquals("Зимняя куртка",q.worn(1).getName());assertEquals("0,5",((JunkLoot.JunkItem)HomeComfort.find(q,"Резиновая пробка для ванны")).shownPrice());
  assertNotNull(q.getUpgrades().pillow);assertNotNull(q.getUpgrades().blanket);assertNotNull(q.getUpgrades().sheet);assertEquals(900,q.getUpgrades().fireUntil);
  assertEquals(3,r.arkady().spawnPlace);assertEquals(2,r.arkady().enemy.roundsRemaining());assertEquals(50,r.vera().child().trauma(BodyPart.LEFT_LEG));assertEquals(45,r.vera().enemy.getHealth());
  assertNotNull(r.pendingBattle(3));assertSame(q,r.pendingBattle(3).player);assertSame(r.arkady().enemy,r.pendingBattle(3).units.get(0).enemy);assertNotNull(r.archiveAttempt);
  GameSave.save(save,q,loaded.balance,loaded.locations);assertEquals(33,GameSave.load(save).balance);
 }
 @Test public void loadsRealOldFixture() throws Exception {File fixture=new File("src/test/resources/survivor-before-storage.dat");assertTrue(fixture.isFile());File file=new File(temp.getRoot(),"legacy.dat");Files.copy(fixture.toPath(),file.toPath());GameSave state=GameSave.load(file);assertNotNull(state.player);assertNotNull(state.locations);GameSave.save(file,state.player,state.balance,state.locations);assertNotNull(GameSave.load(file));}
 @Test public void validBackupRestoresCorruptPrimaryWithoutDeletingIt() throws Exception {File current=new File(temp.getRoot(),"auto.dat"),bak=new File(current.getPath()+".bak");GameSave.save(bak,new Fighter("John","",100),42,new Locations());Files.write(current.toPath(),new byte[]{1,2,3});GameSave state=GameSave.load(current);assertEquals(42,state.balance);assertEquals(42,GameSave.load(current).balance);assertEquals(1,Objects.requireNonNull(temp.getRoot().listFiles((d,n)->n.startsWith("auto.dat.corrupt-"))).length);}
 @Test public void bothDamagedFilesArePreserved() throws Exception {File current=new File(temp.getRoot(),"auto.dat"),bak=new File(current.getPath()+".bak");Files.write(current.toPath(),new byte[]{1,2});Files.write(bak.toPath(),new byte[]{3,4});try{GameSave.load(current);fail("load must fail");}catch(IOException expected){}assertArrayEquals(new byte[]{1,2},Files.readAllBytes(current.toPath()));assertArrayEquals(new byte[]{3,4},Files.readAllBytes(bak.toPath()));}
 @Test public void backupOnlyManualSlotRemainsVisibleAndCanBeLoaded() throws Exception {File current=new File(temp.getRoot(),"auto.dat");File slot=new File(temp.getRoot(),"survivor-slot-123.dat");GameSave.save(new File(slot.getPath()+".bak"),new Fighter("John","",100),8,new Locations());assertTrue(SaveSlots.all(current).contains(slot));assertEquals(8,GameSave.load(slot).balance);}
 @Test public void snapshotFromRecoveredBackupCopiesVerifiedState() throws Exception {File current=new File(temp.getRoot(),"auto.dat"),bak=new File(current.getPath()+".bak");GameSave.save(bak,new Fighter("John","",100),12,new Locations());Files.write(current.toPath(),new byte[]{1});SaveSlots.snapshot(current);List<File> slots=SaveSlots.all(current);assertEquals(2,slots.size());for(File slot:slots)assertEquals(12,GameSave.load(slot).balance);}
 @Test public void validPrimaryDoesNotDestroyBackupOnLoad() throws Exception {File current=new File(temp.getRoot(),"auto.dat"),bak=new File(current.getPath()+".bak");GameSave.save(current,new Fighter("John","",100),20,new Locations());GameSave.save(bak,new Fighter("John","",100),10,new Locations());assertEquals(20,GameSave.load(current).balance);assertTrue(bak.exists());assertEquals(10,GameSave.load(bak).balance);}
 @Test public void nextSaveSafelyReplacesValidatedOldBackup() throws Exception {File current=new File(temp.getRoot(),"auto.dat"),bak=new File(current.getPath()+".bak");GameSave.save(current,new Fighter("John","",100),20,new Locations());GameSave.save(bak,new Fighter("John","",100),10,new Locations());GameSave.save(current,new Fighter("John","",100),30,new Locations());assertEquals(30,GameSave.load(current).balance);}
 @Test public void nextSaveRecoversBackupWhenPrimaryIsBroken() throws Exception {File current=new File(temp.getRoot(),"auto.dat"),bak=new File(current.getPath()+".bak");GameSave.save(bak,new Fighter("John","",100),12,new Locations());Files.write(current.toPath(),new byte[]{1});GameSave.save(current,new Fighter("John","",100),30,new Locations());assertEquals(30,GameSave.load(current).balance);assertEquals(1,Objects.requireNonNull(temp.getRoot().listFiles((d,n)->n.startsWith("auto.dat.corrupt-"))).length);}
}
