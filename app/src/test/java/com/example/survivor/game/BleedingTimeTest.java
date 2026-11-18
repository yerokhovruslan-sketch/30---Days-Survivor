package com.example.survivor.game;

import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.EnumMap;
import static org.junit.Assert.*;

public class BleedingTimeTest {
    @SuppressWarnings("unchecked")
    private static void wound(Fighter p, BodyPart part, int level) throws Exception {
        java.lang.reflect.Field field=Fighter.class.getDeclaredField("injuries");field.setAccessible(true);
        EnumMap<BodyPart,BodyInjury> wounds=(EnumMap<BodyPart,BodyInjury>)field.get(p);
        if(wounds==null){wounds=new EnumMap<>(BodyPart.class);field.set(p,wounds);}
        wounds.put(part,new BodyInjury(10,level));
    }
    @Test public void intervalsAndDifficulty() throws Exception {
        for(int level=1;level<=3;level++){
            Fighter p=new Fighter("John","",100);p.chooseDifficulty(level);wound(p,BodyPart.LEFT_ARM,2);
            int interval=level==1?60:level==2?30:23;
            assertEquals(0,p.advanceBleeding(interval-1));assertEquals(1,p.advanceBleeding(1));
        }
    }
    @Test public void progressSurvivesSaveAndTreatmentStopsLoss() throws Exception {
        Fighter p=new Fighter("John","",100);wound(p,BodyPart.LEFT_ARM,1);Locations locations=new Locations();
        assertEquals(0,p.advanceBleeding(59));File file=Files.createTempFile("bleeding-progress",".dat").toFile();
        try{
            GameSave.save(file,p,0,locations);Fighter loaded=(Fighter)GameSave.load(file).player;
            assertEquals(1,loaded.advanceBleeding(1));loaded.treat(BodyPart.LEFT_ARM,Shop.createItem(1));
            assertEquals(0,loaded.advanceBleeding(120));
        }finally{file.delete();}
    }
    @Test public void bleedingAccumulatesAcrossWoundsWithHourlyLimit() throws Exception {
        Fighter p=new Fighter("John","",100);wound(p,BodyPart.LEFT_ARM,5);wound(p,BodyPart.RIGHT_ARM,5);
        assertEquals(6,p.advanceBleeding(60));assertEquals(2,p.advanceBleeding(10));
    }
    @Test public void worldTimeAppliesBleeding() throws Exception {
        Fighter p=new Fighter("John","",100);wound(p,BodyPart.LEFT_ARM,1);
        WorldStories world=new WorldStories();world.bindRecovery(p);world.advanceMinutes(60);
        assertEquals(1,p.trauma(BodyPart.TORSO));
        world.advanceMinutes(60);assertEquals(98,p.getHealth());
    }
    @Test public void bloodLossReducesHealthWhenHeadIsAlreadyTheWorstInjury() throws Exception {
        Fighter p=new Fighter("John","",100);wound(p,BodyPart.HEAD,0);wound(p,BodyPart.LEFT_ARM,1);
        WorldStories world=new WorldStories();world.bindRecovery(p);int before=p.getHealth();
        world.advanceMinutes(60);assertEquals(before-1,p.getHealth());
    }
    @Test public void elapsedCombatTimeDoesNotApplyTheOutOfCombatBleeding() throws Exception {
        Fighter p=new Fighter("John","",100);wound(p,BodyPart.LEFT_ARM,1);Locations locations=new Locations();
        try(GameSession session=new GameSession(Files.createTempDirectory("bleeding-combat").toFile(),frame->{})){
            GameIO.attach(session);
            try{
                session.mainMenu(p,0,locations);session.battle(p,java.util.List.of(new Zombie(1,ZombieType.NORMAL)),null);
                locations.getStories().advanceMinutes(60);assertEquals(100,p.getHealth());
                GameIO.clearBattle();locations.getStories().advanceMinutes(60);assertEquals(99,p.getHealth());
            }finally{GameIO.detach();}
        }
    }
    @Test public void heartbeatAndGlowNeedSevereWoundOrBleedingWithLowHealth() throws Exception {
        Fighter p=new Fighter("John","",100);
        assertFalse(new GameStatus(p,null,null).bleedingDistress(20,100));
        wound(p,BodyPart.LEFT_ARM,2);wound(p,BodyPart.RIGHT_ARM,2);
        GameStatus moderate=new GameStatus(p,null,null);
        assertFalse(moderate.bleedingDistress(21,100));
        assertTrue(moderate.bleedingDistress(20,100));
        wound(p,BodyPart.LEFT_LEG,3);
        assertTrue(new GameStatus(p,null,null).bleedingDistress(100,100));
        assertFalse(new GameStatus(p,null,null).bleedingDistress(0,100));
    }
}
