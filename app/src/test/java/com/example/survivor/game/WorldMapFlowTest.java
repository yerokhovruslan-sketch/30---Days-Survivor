package com.example.survivor.game;
import org.junit.Test;
import java.nio.file.Files;
import java.io.File;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class WorldMapFlowTest {
 private GameSession.Frame next(LinkedBlockingQueue<GameSession.Frame> q) throws Exception {GameSession.Frame f;do{f=q.poll(5,TimeUnit.SECONDS);assertNotNull(f);}while(f.mode==GameSession.Mode.BUSY);return f;}
 @Test public void mapShowsOnlyDiscoveredLocationsAndOpensPreview() throws Exception {
  Locations w=new Locations();Fighter p=new Fighter("John","",100);LinkedBlockingQueue<GameSession.Frame> q=new LinkedBlockingQueue<>();File dir=Files.createTempDirectory("world-map").toFile();
  try(GameSession session=new GameSession(dir,q::offer,()->{GameIO.mainMenu(p,0,w);w.choose(GameIO.scanner(),p);})){session.start();GameSession.Frame f=next(q);
   assertEquals("Карта локаций",f.menu.title);assertTrue(f.home);
   assertTrue(f.choices.stream().anyMatch(c->c.label.equals("Тихий двор")&&c.artwork.equals("map_point_1")&&c.detail.contains("Не посещена")));
   assertTrue(f.choices.stream().anyMatch(c->c.label.equals("Лесная опушка")));
   assertFalse(f.choices.stream().anyMatch(c->c.label.equals("Школа")));
   assertTrue(session.submit(f.revision,1));f=next(q);assertEquals("Локация",f.menu.title);assertTrue(f.menu.cards.stream().anyMatch(c->c.title.equals("Тихий двор")));assertFalse(w.isVisited(1));
   assertTrue(session.submit(f.revision,0));f=next(q);assertEquals("Карта локаций",f.menu.title);
  }finally{dir.delete();}
 }
 @Test public void visitingCourtyardUnlocksTheStore(){Locations w=new Locations();assertFalse(w.isAvailable(4));w.visit(1);assertTrue(w.isAvailable(4));}
 @Test public void travelSignalsOutboundAndReturn() throws Exception {
  LinkedBlockingQueue<String> events=new LinkedBlockingQueue<>();LinkedBlockingQueue<GameSession.Frame> q=new LinkedBlockingQueue<>();Fighter p=new Fighter("John","",100);File dir=Files.createTempDirectory("travel-map").toFile();
  try(GameSession session=new GameSession(dir,q::offer,()->{GameIO.mainMenu(p,0,new Locations());Survivor.travel(p,"Путь к локации: Тихий двор",5);Survivor.travel(p,"Возвращение домой",5);})){session.setTravelListener(events::offer);session.start();assertEquals("Ты идёшь",events.poll(5,TimeUnit.SECONDS));assertEquals("Ты возвращаешься домой",events.poll(5,TimeUnit.SECONDS));}finally{dir.delete();}
 }
}
