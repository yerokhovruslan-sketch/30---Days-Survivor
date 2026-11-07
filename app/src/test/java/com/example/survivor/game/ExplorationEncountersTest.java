package com.example.survivor.game;
import org.junit.Test;
import java.util.Random;
import java.util.Scanner;
import static org.junit.Assert.*;
public class ExplorationEncountersTest {
 @Test public void progressionRaisesRiskAndGroupsOnlyLater() {
  Locations l=new Locations();int[] route={1,4,5,6,7,8,9};int previous=-1;
  for(int number:route){Location p=l.getLocation(number);p.setState(LocationState.OPEN);int chance=ExplorationEncounters.chance(l,p,false);assertTrue(chance>previous);previous=chance;}
  for(int i=0;i<100;i++)assertEquals(1,ExplorationEncounters.groupSize(3,new Random(i)));
  Random random=new Random(2);boolean multiple=false;for(int i=0;i<50;i++)if(ExplorationEncounters.groupSize(7,random)>1)multiple=true;assertTrue(multiple);
 }
 @Test public void infestationDoesNotShowEntryChoiceOrSpendTime(){
  Locations l=new Locations();Location place=l.getLocation(2);place.setState(LocationState.INFESTED);Fighter p=new Fighter("John","",100);WorldStories w=l.getStories();
  assertTrue(place.prepareForVisit(p,w,new Scanner(""),new Random(1)));assertEquals("08:00",w.time());assertEquals(100,p.getStamina());
 }
 @Test public void encounterCanBeAvoidedByRetreat(){
  Locations l=new Locations();Location place=l.getLocation(8);Fighter p=new Fighter("John","",100);
  Random forced=new Random(){@Override public int nextInt(int bound){return 0;}};
  assertFalse(ExplorationEncounters.tryEncounter(p,l,place,new Scanner("0"),forced));assertEquals(100,p.getHealth());
 }
}
