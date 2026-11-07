package com.example.survivor;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class MusicPlanTest {
 private MusicPlan.Context state(MusicPlan.Area area){MusicPlan.Context c=new MusicPlan.Context();c.area=area;return c;}
 @Test public void silenceIs60To120SecondsAfterFadeAndTrackDoesNotLoop() {
  MusicPlan p=new MusicPlan(new Random(1));MusicPlan.Context c=state(MusicPlan.Area.HOME);p.update(c,0,1);
  assertTrue(p.due>=60000 && p.due<=120000);p.update(c,p.due,1);assertFalse(p.track.isEmpty());
  assertEquals(Long.MAX_VALUE,p.windowEnd);long now=200000;p.naturalTrackEnd(now);assertEquals("",p.track);
  assertTrue(p.due-now-7000>=60000 && p.due-now-7000<=120000);p.update(c,now+1000,2);assertEquals("",p.track);
 }
 @Test public void ordinaryFightsRarelyStartCombatMusicAndDoNotInterruptAmbient() {
  MusicPlan p=new MusicPlan(new Random(2));MusicPlan.Context c=state(MusicPlan.Area.CITY);p.opening(0);int musical=0;
  for(int i=0;i<100;i++){
   long now=i*200000L;c.combat=true;c.enemies=2;p.update(c,now,i*2+1);if(MusicPlan.battleTrack(p.track))musical++;
   else assertFalse(p.track.isEmpty());c.combat=false;p.update(c,now+10000,i*2+2);p.opening(now+11000);
  }
  assertTrue("ordinary battle tracks="+musical,musical>0 && musical<20);
 }
 @Test public void reinforcementsCanEscalateButMenusDoNotReroll() {
  MusicPlan p=new MusicPlan(new Random(3));MusicPlan.Context c=state(MusicPlan.Area.ARCHIVE);c.combat=true;c.archive=true;c.enemies=2;p.opening(0);p.update(c,0,1);
  c.enemies=4;p.update(c,200000,2);assertTrue(p.escalated);assertTrue(MusicPlan.battleTrack(p.track));String track=p.track;
  for(int i=0;i<50;i++)p.update(c,200100+i,3+i);assertEquals(track,p.track);
 }
 @Test public void combatTailAtHomeAndOneShotCues() {
  MusicPlan p=new MusicPlan(new Random(4));MusicPlan.Context c=state(MusicPlan.Area.CITY);c.combat=true;c.enemies=2;p.update(c,0,1);
  c.enemies=5;p.update(c,200000,2);assertTrue(MusicPlan.battleTrack(p.track));String track=p.track;
  c.area=MusicPlan.Area.HOME;c.combat=false;p.update(c,201000,3);assertEquals(track,p.track);assertTrue(p.homeBattleUntil>=206000 && p.homeBattleUntil<=211000);
  p.update(c,p.homeBattleUntil,4);assertEquals("",p.track);
  c.cue="elegy";p.update(c,300000,5);assertEquals("elegy",p.track);p.naturalTrackEnd(301000);p.update(c,301001,5);assertEquals("",p.track);
 }
 @Test public void broadBackgroundsAreAvailableAtHomeAndRepeatIsAvoided() {
  MusicPlan p=new MusicPlan(new Random(5));MusicPlan.Context c=state(MusicPlan.Area.HOME);p.update(c,0,1);Set<String> tracks=new HashSet<>();
  String last="";for(int i=0;i<100;i++){p.pick(i*200000L);assertNotEquals(last,p.track);last=p.track;tracks.add(last);}
  assertTrue(tracks.contains("wanderer"));assertTrue(tracks.contains("ashfall"));assertTrue(tracks.contains("elegy"));assertTrue(tracks.contains("houslast"));assertTrue(tracks.size()>5);
  p.opening(0);assertEquals("houslast",p.track);c.area=MusicPlan.Area.CITY;p.update(c,1,2);assertEquals("houslast",p.track);
 }
}
