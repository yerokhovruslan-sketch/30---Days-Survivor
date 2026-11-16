package com.example.survivor;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class AmbientPlanTest {
 @Test public void mapOnlyGetsFlocksAndDayNeverGetsNightSounds(){Random r=new Random(4);for(int i=0;i<1000;i++){
  AmbientPlan.Cue map=AmbientPlan.choose(r,false,false,false,true);assertTrue(map==AmbientPlan.Cue.NONE||map==AmbientPlan.Cue.FLOCK);
  AmbientPlan.Cue day=AmbientPlan.choose(r,false,false,true,false);assertTrue(day==AmbientPlan.Cue.CITY||day==AmbientPlan.Cue.WIND||day==AmbientPlan.Cue.NONE);
 }}
 @Test public void nightForestCanHaveCricketsWindAndCrows(){Random r=new Random(8);Set<AmbientPlan.Cue> heard=EnumSet.noneOf(AmbientPlan.Cue.class);for(int i=0;i<200;i++)heard.add(AmbientPlan.choose(r,true,true,false,false));assertTrue(heard.contains(AmbientPlan.Cue.FOREST));assertTrue(heard.contains(AmbientPlan.Cue.WIND));assertTrue(heard.contains(AmbientPlan.Cue.CROW));assertTrue(heard.contains(AmbientPlan.Cue.FLOCK));assertTrue(heard.contains(AmbientPlan.Cue.NIGHT_TENSION));}
}
