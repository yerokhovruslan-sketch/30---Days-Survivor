package com.example.survivor.game;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class ZombieTrophyTest {
    @Test public void exactCountSplitAndInclusivePricesForAllTypes() {
        int[][] limits={{4,12},{10,19},{60,80},{15,25},{20,40},{80,120}};
        for(ZombieType type:ZombieType.values()) {
            for(int countRoll=0;countRoll<100;countRoll++) {
                final int roll=countRoll;
                Random low=new Random(){boolean first=true;@Override public int nextInt(int bound){if(first){first=false;return roll;}return 0;}};
                List<ItemInfo> ears=ZombieEar.roll(type,low);
                assertEquals(countRoll<40?2:1,ears.size());
                for(ItemInfo ear:ears)assertEquals(limits[type.ordinal()][0],ear.getSalePrice());
            }
            Random high=new Random(){boolean first=true;@Override public int nextInt(int bound){if(first){first=false;return 99;}return bound-1;}};
            assertEquals(limits[type.ordinal()][1],ZombieEar.roll(type,high).get(0).getSalePrice());
        }
    }
    @Test public void corpsesDropOnceOnTheirCellAndLivingZombiesGiveNothing() {
        Zombie z=new Zombie(1,ZombieType.BRUTE);
        assertTrue(z.claimEars(new Random(1)).isEmpty());
        TacticalBattle b=new TacticalBattle(new Fighter("John","",100),1,new Random(2),Arrays.asList(z));
        z.takeDamage(10000);b.dropDead();
        List<ItemInfo> pile=b.ground.get(b.units.get(0).cell);
        int count=pile.size();assertTrue(count==1 || count==2);
        b.dropDead();assertEquals(count,pile.size());assertTrue(z.claimEars(new Random(3)).isEmpty());
    }
    @Test public void earPriceSurvivesSavingAndSellingWithoutHalfPricePenalty() throws Exception {
        File file=Files.createTempFile("ears", ".dat").toFile();
        try {
            Fighter p=new Fighter("John","",100);p.addItem(new ZombieEar(ZombieType.HUNTER,117));
            Locations world=new Locations();GameSave.save(file,p,0,world);
            Fighter loaded=(Fighter)GameSave.load(file).player;
            ItemInfo ear=loaded.getInventory().stream().filter(i->i instanceof ZombieEar).findFirst().get();
            assertEquals(117,ear.getSalePrice());assertEquals(117,Economy.salePrice(world.getStories(),ear));
        } finally {file.delete();}
    }
    @Test public void hardModeCannotWriteOrLoadAndDoesNotOverwriteOtherRuns() throws Exception {
        File file=Files.createTempFile("hard-mode", ".dat").toFile();
        try {
            Fighter normal=new Fighter("John","",100),hard=new Fighter("John","",100);hard.chooseDifficulty(3);
            Locations world=new Locations();GameSave.save(file,normal,25,world);
            byte[] original=Files.readAllBytes(file.toPath());
            try{GameSave.save(file,hard,0,world);fail();}catch(IOException expected){}
            assertArrayEquals(original,Files.readAllBytes(file.toPath()));
            // Older versions allowed hard saves. Those must no longer be loadable.
            try(ObjectOutputStream out=new ObjectOutputStream(new FileOutputStream(file))){out.writeObject(new GameSave(hard,0,world));}
            try{GameSave.load(file);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("загрузка недоступна"));}
        } finally {file.delete();}
    }
    @Test public void huntersAreDistinctAndOnlyGeneratedAtNight() {
        EnumSet<ZombieType> night=EnumSet.noneOf(ZombieType.class);
        for(int roll=0;roll<100;roll++) {
            final int value=roll;
            Random r=new Random(){@Override public int nextInt(int bound){return value;}};
            night.add(Survivor.createZombie(1,r,true).getType());
            ZombieType day=Survivor.createZombie(1,r,false).getType();
            assertNotEquals(ZombieType.HUNTER,day);assertNotEquals(ZombieType.JUMPER,day);
        }
        assertTrue(night.contains(ZombieType.HUNTER));assertTrue(night.contains(ZombieType.JUMPER));
    }
}
