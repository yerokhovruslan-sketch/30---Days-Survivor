package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class SearchedPointTest {
 static void set(Object o,String name,boolean value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.setBoolean(o,value);}
 static GameSession.Frame next(LinkedBlockingQueue<GameSession.Frame> q)throws Exception{GameSession.Frame f;do{f=q.poll(5,TimeUnit.SECONDS);assertNotNull(f);}while(f.mode==GameSession.Mode.BUSY);return f;}
 @Test public void onlyFullySearchedPointIsMarkedAndCanStillBeOpened() throws Exception {
  Locations w=new Locations();Fighter p=new Fighter("John","",100);ApartmentBuilding building=w.getStories().apartment();
  set(building,"underStairs",true);set(building,"mailbox",true);set(building,"notice",true);set(building,"stairsCleared",true);set(building,"kitchen",true);set(building,"bedroom",true);
  File dir=Files.createTempDirectory("searched-points").toFile();LinkedBlockingQueue<GameSession.Frame> q=new LinkedBlockingQueue<>();
  try(GameSession s=new GameSession(dir,q::offer,()->{GameIO.mainMenu(p,0,w);building.visit(p,w.getLocation(5),w.getStories(),GameIO.scanner(),new Random(1));})){
   s.start();GameSession.Frame f=next(q);GameSession.Choice lobby=f.choices.stream().filter(c->c.number==1).findFirst().get();
   assertEquals("point_searched",lobby.artwork);assertTrue(lobby.enabled);assertTrue(lobby.detail.contains("Полностью обыскано"));
   GameSession.Choice apt=f.choices.stream().filter(c->c.number==3).findFirst().get();assertEquals("",apt.artwork);
   assertTrue(s.submit(f.revision,1));assertEquals("Вестибюль",next(q).menu.title);
  }finally{dir.delete();}
 }
}
