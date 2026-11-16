package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
public class SleepClockTest {
 @Test public void sleepinessFollowsElapsedTimeAndSleepDoesNotAddIt(){
  for(int difficulty=1;difficulty<=3;difficulty++){
   Fighter john=new Fighter("John","",100);john.chooseDifficulty(difficulty);WorldStories clock=new WorldStories();clock.bindRecovery(john);
   clock.advanceMinutes(480);assertEquals(24,john.getSleepiness());clock.advanceMinutes(480);assertEquals(48,john.getSleepiness());
   assertTrue(john.canSleep());ShelterUpgrades.sleep(john,clock);assertEquals(0,john.getSleepiness());
   clock.advanceMinutes(19);assertEquals(0,john.getSleepiness());clock.advanceMinutes(1);assertEquals(1,john.getSleepiness());
  }
 }
}
