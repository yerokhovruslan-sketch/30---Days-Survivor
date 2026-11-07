package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;
public class BattlePersistenceTest {
 @Test public void enemiesHealthPositionsAndTerrainSurviveSaveAndRespawn() throws Exception {
  Locations l=new Locations();Fighter p=new Fighter("John","",100);WorldStories w=l.getStories();
  Zombie a=new Zombie(1,ZombieType.NORMAL),b=new Zombie(2,ZombieType.NORMAL);
  TacticalBattle battle=new TacticalBattle(p,1,new Random(4),Arrays.asList(a,b));battle.units.get(0).cell=20;battle.units.get(1).cell=21;
  a.takeDamage(a.getHealth());b.takeDamage(b.getHealth()-12);battle.escaped=true;w.rememberBattle(4,battle);
  File f=Files.createTempFile("battle-persist", ".dat").toFile();
  try{GameSave.save(f,p,0,l);GameSave saved=GameSave.load(f);TacticalBattle loaded=saved.locations.getStories().pendingBattle(4);
   assertNotNull(loaded);assertFalse(loaded.units.get(0).enemy.isAlive());assertEquals(12,loaded.units.get(1).enemy.getHealth());
   assertEquals(20,loaded.units.get(0).cell);assertEquals(21,loaded.units.get(1).cell);
   assertSame(saved.player,loaded.player);loaded.prepareReturn();assertNotEquals(21,loaded.playerCell);assertEquals(21,loaded.units.get(1).cell);
  }finally{f.delete();}
 }
 @Test public void archiveAttemptSurvivesLeavingAndSave() throws Exception {
  Locations l=new Locations();Fighter p=new Fighter("John","",100);WorldStories w=l.getStories();
  w.archiveAttempt=new PoliceArchive.Attempt(p,new Random(1));TacticalBattle.Unit u=w.archiveAttempt.rooms[0].units.get(0);u.cell=18;u.enemy.takeDamage(5);
  int hp=u.enemy.getHealth();w.leavePoliceLocation();assertNotNull(w.archiveAttempt);
  File f=Files.createTempFile("archive-persist", ".dat").toFile();try{GameSave.save(f,p,0,l);GameSave saved=GameSave.load(f);TacticalBattle.Unit v=saved.locations.getStories().archiveAttempt.rooms[0].units.get(0);assertEquals(18,v.cell);assertEquals(hp,v.enemy.getHealth());}finally{f.delete();}
 }
 @Test public void escapeActionActuallyStoresBattleForLocation() throws Exception {
  Locations l=new Locations();Fighter p=new Fighter("John","",100);java.util.concurrent.LinkedBlockingQueue<GameSession.Frame> frames=new java.util.concurrent.LinkedBlockingQueue<>();
  File dir=Files.createTempDirectory("escape-test").toFile();
  Random rng=new Random(7){@Override public int nextInt(int bound){return bound==100?0:super.nextInt(bound);}};
  try(GameSession session=new GameSession(dir,frames::offer,()->{GameIO.mainMenu(p,0,l);l.enter(l.getLocation(1));TacticalBattle.play(p,GameIO.scanner(),1,rng,Arrays.asList(new Zombie(1,ZombieType.NORMAL)),false);})){session.start();
   GameSession.Frame frame=frames.poll(5,java.util.concurrent.TimeUnit.SECONDS);assertNotNull(frame);assertNotNull(frame.menu.board);assertTrue(session.submit(frame.revision,7));
   long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);while(l.getStories().pendingBattle(1)==null && System.nanoTime()<deadline)Thread.sleep(10);
   TacticalBattle saved=l.getStories().pendingBattle(1);assertNotNull(saved);assertTrue(saved.escaped);assertEquals(1,saved.units.size());
  }finally{for(File file:Objects.requireNonNull(dir.listFiles()))file.delete();dir.delete();}
 }
}
