package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class StoryMenuMarkerTest {
 @Test public void marksStoryAndPreservesAllChoices() throws Exception {
  File dir=Files.createTempDirectory("story-frame").toFile();LinkedBlockingQueue<GameSession.Frame> q=new LinkedBlockingQueue<>();
  try(GameSession s=new GameSession(dir,q::offer,()->{GameIO.storyMenu("Встреча","Семён: «Привет».\nДжон: «Здравствуй».");GameIO.action(1,"Помочь");GameIO.action(2,"Помочь позже","Нужна вода",false);GameIO.action(0,"Уйти");SurvivalSystems.choice(GameIO.scanner());})){
   s.start();GameSession.Frame f=q.poll(5,TimeUnit.SECONDS);assertNotNull(f);assertTrue(f.menu.story);assertEquals(3,f.choices.size());assertFalse(f.choices.get(1).enabled);assertFalse(s.submit(f.revision,2));assertTrue(s.submit(f.revision,1));
  }finally{dir.delete();}
 }
}
