package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Scanner;
import static org.junit.Assert.*;

public class RouteAfterArchiveTest {
    @Test public void newConversationUnlocksDriverLeadAndSurvivesSave() throws Exception {
        Locations world = new Locations();
        WorldStories stories = world.getStories();
        stories.archiveComplete = true;
        Fighter player = new Fighter("John", "", 100);
        RouteAfterArchive route = stories.routeAfterArchive();
        try (Scanner choices = new Scanner("4 3 0 1 0 1 0 6 0 0 0 0")) {
            route.visit(player,world.getLocation(13),stories,13,choices);
        }
        assertTrue(route.olgaIntroComplete);
        assertTrue(route.warehouseTalk);
        assertTrue(route.driverLead);
        assertFalse(route.kitRequested);
        File file = Files.createTempFile("route-after-archive", ".dat").toFile();
        try {
            GameSave.save(file, player, 0, world);
            RouteAfterArchive loaded = GameSave.load(file).locations.getStories().routeAfterArchive();
            assertTrue(loaded.olgaIntroComplete);
            assertTrue(loaded.warehouseTalk);
            assertTrue(loaded.driverLead);
        } finally { file.delete(); }
    }
    @Test public void garageClueUnlocksWorkshopAndPersistsTerminalDocuments() throws Exception {
        Locations world = new Locations(); WorldStories stories = world.getStories();
        Fighter player = new Fighter("John", "", 100); RouteAfterArchive route = stories.routeAfterArchive();
        route.driverLead = true;
        assertTrue(world.isAvailable(35)); assertFalse(world.isAvailable(36));
        try (Scanner choices = new Scanner("1 2 0 0 3 4 0 0 2 4 5 0 6 0 0 0")) { route.visit(player,world.getLocation(35),stories,35,choices); }
        assertTrue(route.garageNote); assertTrue(route.watchmanSearched); assertTrue(route.garageOpen);
        assertTrue(route.cabinetNote); assertTrue(route.repairReceipt); assertTrue(world.isAvailable(36));
        player.addItem(Shop.createItem(27));
        try (Scanner choices = new Scanner("3 8 2 0 3 0 4 0 0 0")) { route.visit(player,world.getLocation(36),stories,36,choices); }
        assertEquals(1,player.getInventory().stream().filter(i->i.getName().equals(RouteAfterArchive.PASS)).count());
        assertTrue(route.workshopJournal); assertTrue(route.terminalPass); assertTrue(route.yardMap);
        File file = Files.createTempFile("driver-clues", ".dat").toFile();
        try { GameSave.save(file,player,0,world); Locations loaded=GameSave.load(file).locations;
            assertTrue(loaded.getStories().routeAfterArchive().terminalPass); assertTrue(loaded.isAvailable(36));
            Fighter restored=(Fighter)GameSave.load(file).player;
            try(Scanner retry=new Scanner("3 3 3 0 0")){
                loaded.getStories().routeAfterArchive().visit(restored,loaded.getLocation(36),loaded.getStories(),36,retry);
            }
            assertEquals(1,restored.getInventory().stream().filter(i->i.getName().equals(RouteAfterArchive.PASS)).count());
        } finally {file.delete();}
    }
    @Test public void terminalPuzzleResetsAndCargoNeedsToolAndSurvivesReload() throws Exception {
        Locations world=new Locations(); WorldStories w=world.getStories(); Fighter p=new Fighter("John","",100);
        RouteAfterArchive route=w.routeAfterArchive(); route.driverLead=true; route.terminalPass=true; p.addItem(new Tools(RouteAfterArchive.PASS,"",1,0));
        assertTrue(world.isAvailable(37));
        assertFalse(route.cargoChoice(2)); assertEquals(1,route.cargoProgress);
        assertFalse(route.cargoChoice(7)); assertEquals(0,route.cargoProgress);
        assertFalse(route.cargoChoice(2)); assertFalse(route.cargoChoice(5)); assertFalse(route.cargoChoice(8)); assertTrue(route.cargoChoice(11));
        try(Scanner choices=new Scanner("0 7 8 5 0 0 0")){route.visit(p,world.getLocation(37),w,37,choices);}
        assertFalse(route.cargoFound);
        p.addItem(Shop.createItem(20));
        try(Scanner choices=new Scanner("7 5 0 0 0")){route.visit(p,world.getLocation(37),w,37,choices);}
        assertTrue(route.cargoFound); assertEquals(1,route.cargoDay);
        File file=Files.createTempFile("cargo-progress", ".dat").toFile();
        try{GameSave.save(file,p,0,world);RouteAfterArchive loaded=GameSave.load(file).locations.getStories().routeAfterArchive();assertTrue(loaded.puzzleSolved);assertTrue(loaded.cargoFound);assertEquals(4,loaded.cargoProgress);}finally{file.delete();}
    }
    @Test public void terminalReturnIsNotAvailableUntilLaterNightAndRunsOnce() {
        WorldStories w=new WorldStories(); RouteAfterArchive route=w.routeAfterArchive();route.cargoFound=true;route.cargoDay=w.day();
        try(Scanner input=new Scanner("0")){route.beforeTerminalExit(w,input);}assertTrue(route.terminalExitShown);
        w.advanceMinutes(720);try(Scanner input=new Scanner("0")){route.terminalArrival(w,input);}assertFalse(route.nightReturnShown);
        w.advanceMinutes(1440);try(Scanner input=new Scanner("0")){route.terminalArrival(w,input);}assertTrue(route.nightReturnShown);
    }

    private GameSession.Frame next(java.util.concurrent.BlockingQueue<GameSession.Frame> frames) throws Exception {
        GameSession.Frame frame;
        do {
            frame=frames.poll(3,java.util.concurrent.TimeUnit.SECONDS);
            assertNotNull("Ожидался кадр игры",frame);
        } while(frame.mode==GameSession.Mode.BUSY);
        return frame;
    }
    private GameSession.Frame choose(GameSession session, java.util.concurrent.BlockingQueue<GameSession.Frame> frames,
                                     GameSession.Frame frame,int choice,String nextTitle) throws Exception {
        assertTrue(session.submit(frame.revision,choice));
        GameSession.Frame result=next(frames);
        assertEquals(nextTitle,result.menu.title);
        return result;
    }

    @Test public void earlyTerminalVisitCannotBypassGateAndPuzzleUsesRealMenuChoices() throws Exception {
        Locations world=new Locations();WorldStories w=world.getStories();
        Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(27));
        RouteAfterArchive route=w.routeAfterArchive();
        var frames=new java.util.concurrent.LinkedBlockingQueue<GameSession.Frame>();
        File folder=Files.createTempDirectory("terminal-session").toFile();
        try(GameSession session=new GameSession(folder,frames::offer,()->{
            GameIO.mainMenu(p,0,world);
            route.visit(p,world.getLocation(37),w,37,GameIO.scanner());
        })){
            session.start();
            var frame=choose(session,frames,next(frames),0,"Грузовой терминал");
            frame=choose(session,frames,frame,7,"Площадка Г · служебный сектор");
            assertFalse(frame.choices.stream().anyMatch(c->c.number==8&&c.enabled));
            assertFalse(session.submit(frame.revision,8));
            frame=choose(session,frames,frame,0,"Грузовой терминал");
            frame=choose(session,frames,frame,2,"Диспетчерская");
            frame=choose(session,frames,frame,1,"Распоряжение");
            frame=choose(session,frames,frame,0,"Диспетчерская");
            frame=choose(session,frames,frame,2,"Запись о доставке");
            frame=choose(session,frames,frame,0,"Диспетчерская");
            frame=choose(session,frames,frame,3,"Журнал транспорта");
            frame=choose(session,frames,frame,0,"Диспетчерская");
            frame=choose(session,frames,frame,4,"Восстановить грузовой маршрут");
            assertEquals(12,frame.choices.stream().filter(c->c.number>0).count());
            frame=choose(session,frames,frame,2,"Восстановить грузовой маршрут");
            assertTrue(frame.choices.stream().anyMatch(c->c.number==2&&c.selected));
            frame=choose(session,frames,frame,7,"Восстановить грузовой маршрут");
            assertFalse(frame.choices.stream().anyMatch(c->c.selected));
            for(int n:new int[]{2,5,8})frame=choose(session,frames,frame,n,"Восстановить грузовой маршрут");
            frame=choose(session,frames,frame,11,"Маршрут восстановлен");
            assertTrue(route.puzzleSolved);
            assertFalse(route.cargoFound);
        } finally {
            new File(folder,"survivor-save.dat").delete();
            folder.delete();
        }
    }

    @Test public void watchmanTriggersOneWoundedZombieInTheLiveGameSession() throws Exception {
        Locations world=new Locations();WorldStories w=world.getStories();
        Fighter p=new Fighter("John","",100);
        var frames=new java.util.concurrent.LinkedBlockingQueue<GameSession.Frame>();
        File folder=Files.createTempDirectory("garage-session").toFile();
        try(GameSession session=new GameSession(folder,frames::offer,()->{
            GameIO.mainMenu(p,0,world);
            w.beginVisit(35,"Гаражный кооператив",new java.util.Random(1));
            w.routeAfterArchive().visit(p,world.getLocation(35),w,35,GameIO.scanner());
        })){
            session.start();
            var frame=choose(session,frames,next(frames),2,"Металлический гараж");
            assertFalse(session.submit(frame.revision,3));
            assertFalse(session.submit(frame.revision,4));
            frame=choose(session,frames,frame,0,"Гаражный кооператив");
            frame=choose(session,frames,frame,3,"Сторожка");
            frame=choose(session,frames,frame,4,"Сторож");
            assertTrue(session.submit(frame.revision,0));
            frame=next(frames);
            assertNotNull(frame.menu.board);
            assertEquals(1,frame.status.enemies.size());
            assertEquals(15,frame.status.enemies.get(0).health);
            assertFalse(p.getInventory().stream().anyMatch(item->item.getName().equals(RouteAfterArchive.KEY)));
        }finally{
            new File(folder,"survivor-save.dat").delete();
            folder.delete();
        }
    }
    @Test public void previouslySearchedStoryPointDoesNotOfferLootAgain() throws Exception {
        Locations world=new Locations(); WorldStories stories=world.getStories();
        Fighter player=new Fighter("John","",100); stories.archiveComplete=true;
        Location station=world.getLocation(13);
        Location.SearchPoint dispatch=station.getSearchPoints().stream().filter(p->p.name.equals("Диспетчерская")).findFirst().orElseThrow();
        station.consumePoint(dispatch);
        var frames=new java.util.concurrent.LinkedBlockingQueue<GameSession.Frame>();
        File folder=Files.createTempDirectory("old-station").toFile();
        try(GameSession session=new GameSession(folder,frames::offer,()->{
            GameIO.mainMenu(player,0,world);
            stories.routeAfterArchive().visit(player,station,stories,13,GameIO.scanner());
        })) {
            session.start();
            var frame=choose(session,frames,next(frames),2,"Диспетчерская");
            assertTrue(frame.choices.stream().anyMatch(c->c.number==1&&c.enabled));
            assertTrue(frame.choices.stream().anyMatch(c->c.number==9&&!c.enabled));
        } finally {new File(folder,"survivor-save.dat").delete();folder.delete();}
    }
}
