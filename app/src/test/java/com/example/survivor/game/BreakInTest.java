package com.example.survivor.game;
import org.junit.Test;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;
public class BreakInTest {
 @Test public void chanceAndGroupSizeHaveExactBoundaries(){
  Random yes=new Random(){public int nextInt(int b){return 39;}};
  Random no=new Random(){public int nextInt(int b){return 40;}};
  assertTrue(BreakIn.shouldAttack(yes));assertFalse(BreakIn.shouldAttack(no));
  assertEquals(1,BreakIn.count(new Random(){public int nextInt(int b){return 0;}}));
  assertEquals(4,BreakIn.count(new Random(){public int nextInt(int b){return 3;}}));
 }
 @Test public void breakInBattleContainsFourOrdinaryZombies() throws Exception {
  Locations l=new Locations();Fighter p=new Fighter("John","",100);LinkedBlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();
  java.io.File dir=Files.createTempDirectory("breakin").toFile();
  Random rng=new Random(5){int n;public int nextInt(int b){n++;if(n==1)return 0;if(n==2)return 3;return super.nextInt(b);}};
  try(GameSession session=new GameSession(dir,frames::offer,()->{GameIO.mainMenu(p,0,l);l.enter(l.getLocation(1));BreakIn.attack(p,GameIO.scanner(),rng);})){session.start();
   GameSession.Frame f=frames.poll(5,TimeUnit.SECONDS);assertNotNull(f);assertNotNull(f.menu.board);
   assertEquals(4,f.status.enemies.size());for(GameStatus.Enemy e:f.status.enemies)assertTrue(e.name.contains("Обычный"));
  }finally{dir.delete();}
 }
}

