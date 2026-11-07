package com.example.survivor.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/** Encounter probabilities grow with distance along each location's unlock chain. */
final class ExplorationEncounters {
    private ExplorationEncounters() {}
    static int baseChance(Locations world, Location place, boolean night) {
        int number=world.getNumber(place);
        if(number==1)return 0; // The courtyard remains a safe introduction.
        int stage=world.explorationStage(number);
        int base=stage<=2?8:stage==3?12:stage==4?16:stage==5?24:stage==6?32:stage==7?40:48;
        if(place.getState()==LocationState.INFESTED)base+=7;
        if(night)base+=5;
        return Math.min(60,base);
    }
    static int chance(Locations world,Location place,boolean night) {
        int base=baseChance(world,place,night);
        // Preserve the existing human-encounter chance while increasing zombie probability by 30%.
        int bonus=world.getStories().monsterDens().active(world.getNumber(place))?(int)Math.round(base*.82*.30):0;
        return Math.min(100,base+bonus);
    }
    static int groupSize(int stage,Random random){
        if(stage<=3)return 1;
        if(stage<=5)return random.nextInt(100)<75?1:2;
        if(stage<=7)return 1+random.nextInt(2);
        return 1+random.nextInt(3);
    }
    static boolean tryEncounter(Fighter player, Locations locations, Location place, Scanner scanner, Random random) {
        if(!player.isAlive())return false;
        WorldStories clock=locations.getStories();int stage=locations.explorationStage(locations.getNumber(place));
        int roll=random.nextInt(100),base=baseChance(locations,place,clock.isNight());
        if(roll>=chance(locations,place,clock.isNight()))return true;
        boolean denBonus=roll>=base;
        int number=locations.getNumber(place);
        // Existing human encounters remain possible in the original search-based locations.
        if(!denBonus && (number<4 || number>8) && random.nextInt(100)<18){
            boolean raiders=random.nextInt(100)<(place.getEncounterDifficulty()<=1?15:place.getEncounterDifficulty()==2?30:45);
            int people=raiders?(place.getEncounterDifficulty()>=2?1+random.nextInt(2):1):1+random.nextInt(3);
            List<CombatEnemy> group=new ArrayList<>();
            for(int i=1;i<=people;i++)group.add(new HostileSurvivor(i,raiders,random));
            clock.observePeople(random);
            return Survivor.approachEnemies(player,scanner,place.getEncounterDifficulty(),random,group);
        }
        int count=groupSize(stage,random);List<CombatEnemy> enemies=new ArrayList<>();
        for(int i=1;i<=count;i++){
            Zombie z=stage<=4 ? new Zombie(i,random.nextInt(100)<85?ZombieType.NORMAL:ZombieType.RUNNER)
                    : Survivor.createZombie(i,random,clock.isNight());
            enemies.add(z);
        }
        GameIO.out().println(count==1?"Поблизости появился заражённый.":"Поблизости появились заражённые: "+count+".");
        return Survivor.approachEnemies(player,scanner,place.getEncounterDifficulty(),random,enemies);
    }
}
