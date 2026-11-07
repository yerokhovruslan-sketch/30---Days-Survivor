package com.example.survivor.game;

import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.Files;
import java.util.concurrent.*;

public class LiveClockTest {
    @Test public void foregroundTicksKeepChoicesStableAndStoryAndBackgroundPause() throws Exception {
        BlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();
        WorldStories[] clock=new WorldStories[1];
        java.io.File directory=Files.createTempDirectory("live-clock").toFile();
        try(GameSession session=new GameSession(directory,frames::offer,()->{
            Locations world=new Locations();clock[0]=world.getStories();
            GameIO.mainMenu(new Fighter("John","",100),0,world);
            GameIO.scanner().nextInt();
            GameIO.storyMenu("Сюжет","Время на чтение не ограничено.");GameIO.action(0,"Продолжить");
            GameIO.scanner().nextInt();
        })) {
            session.setForeground(true);session.start();GameSession.Frame first=frames.poll(3,TimeUnit.SECONDS);assertNotNull(first);
            GameSession.Frame tick=frames.poll(7,TimeUnit.SECONDS);assertNotNull(tick);assertEquals("08:01",tick.time);assertEquals(first.revision,tick.revision);
            session.setForeground(false);frames.clear();assertNull(frames.poll(3,TimeUnit.SECONDS));assertEquals("08:01",clock[0].time());
            session.setForeground(true);session.setPaused(true);assertNull(frames.poll(3,TimeUnit.SECONDS));assertEquals("08:01",clock[0].time());
            session.setPaused(false);assertTrue(session.submit(tick.revision,1));
            GameSession.Frame story;do{story=frames.poll(3,TimeUnit.SECONDS);assertNotNull(story);}while(story.mode==GameSession.Mode.BUSY);
            assertEquals("Сюжет",story.menu.title);assertNull(frames.poll(3,TimeUnit.SECONDS));assertEquals("08:01",clock[0].time());
        }finally{new java.io.File(directory,"survivor-save.dat").delete();directory.delete();}
    }
    @Test public void loadingAfterDeathRestoresTheSavedClockAndDoesNotOverwriteIt() throws Exception {
        BlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();
        java.io.File directory=Files.createTempDirectory("death-clock").toFile();
        try(GameSession session=new GameSession(directory,frames::offer,()->{
            Locations world=new Locations();Fighter p=new Fighter("John","",100);
            GameIO.mainMenu(p,77,world);GameIO.checkpoint();
            world.getStories().advanceMinutes(4*1440+56);p.takeDamage(1000);GameIO.checkpoint();
            GameIO.scanner().nextInt();
            try {
                GameSave saved=GameSave.load(GameIO.saveFile());
                GameIO.mainMenu(saved.player,saved.balance,saved.locations);
                GameIO.scanner().nextInt();
            }catch(java.io.IOException e){throw new RuntimeException(e);}
        })) {
            session.start();GameSession.Frame dead=frames.poll(3,TimeUnit.SECONDS);assertNotNull(dead);assertEquals(0,dead.health);assertEquals(5,dead.day);
            assertTrue(session.submit(dead.revision,12));
            GameSession.Frame restored;do{restored=frames.poll(3,TimeUnit.SECONDS);assertNotNull(restored);}while(restored.mode==GameSession.Mode.BUSY);
            assertEquals(1,restored.day);assertEquals("08:00",restored.time);assertEquals(100,restored.health);assertEquals(77,restored.balance);
            assertEquals(1,GameSave.load(new java.io.File(directory,"survivor-save.dat")).locations.getStories().day());
        }finally{new java.io.File(directory,"survivor-save.dat").delete();directory.delete();}
    }
}
