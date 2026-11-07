package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import com.example.survivor.BoardVisuals;
public class BattleVisualTest {
 private TacticalBattle battle(){TacticalBattle b=new TacticalBattle(new Fighter("John","",100),1,new Random(4),Arrays.asList(new Zombie(1,ZombieType.NORMAL)));Arrays.fill(b.obstacles,null);b.playerCell=56;b.units.get(0).cell=59;return b;}
 @Test public void snapshotHasStableIdentityEffectsAndBlockedShotData(){
  TacticalBattle b=battle();Weapons gun=(Weapons)Shop.createItem(9);b.player.addItem(gun);b.player.equipWeapon(gun);b.player.addItem(new Ammunition(AmmoType.REVOLVER));b.units.get(0).skip=1;b.units.get(0).slow=2;b.aimed=true;
  BattleBoard first=b.snapshot();assertEquals(0,first.cells.get(56).identity);assertTrue(first.cells.get(56).aimed);assertEquals(1,first.cells.get(59).stun);assertEquals(2,first.cells.get(59).slow);
  assertEquals(59,first.shotTarget);assertFalse(first.shotBlocked);b.obstacles[58]=TacticalBattle.Obstacle.WALL;assertTrue(b.snapshot().shotBlocked);
  int id=first.cells.get(59).identity;b.obstacles[58]=null;b.units.get(0).cell=60;assertEquals(id,b.snapshot().cells.get(60).identity);
 }
 @Test public void damageAndMissEventsAreNotDuplicatedBySnapshots(){
  TacticalBattle b=battle();b.units.get(0).cell=57;assertTrue(b.attack());BattleBoard first=b.snapshot();assertTrue(first.effects.size()>=1);
  BattleBoard.Effect e=first.effects.get(0);assertEquals(57,e.cell);assertTrue(e.kind.equals("damage") || e.kind.equals("miss"));assertEquals(e.id,b.snapshot().effects.get(0).id);
  b.visual("miss",b.playerCell,0);assertTrue(b.snapshot().effects.get(b.snapshot().effects.size()-1).id>e.id);
 }
 @Test public void pathIsOrthogonalAndCannotCrossWalls(){
  TacticalBattle b=battle();BattleBoard snap=b.snapshot();assertEquals(Arrays.asList(56,57,58),BoardVisuals.path(snap,56,58));
  b.obstacles[57]=TacticalBattle.Obstacle.WALL;List<Integer> path=BoardVisuals.path(b.snapshot(),56,58);assertFalse(path.contains(57));
  for(int i=1;i<path.size();i++)assertEquals(1,TacticalBattle.distance(path.get(i-1),path.get(i)));
  b.obstacles[58]=TacticalBattle.Obstacle.WALL;assertTrue(BoardVisuals.path(b.snapshot(),56,58).isEmpty());
 }
}
