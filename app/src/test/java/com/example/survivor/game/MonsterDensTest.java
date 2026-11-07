package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;
public class MonsterDensTest {
 @SuppressWarnings("unchecked") private void put(WorldStories w,int place,MonsterDens.Den den) throws Exception {Field f=MonsterDens.class.getDeclaredField("dens");f.setAccessible(true);Map<Integer,MonsterDens.Den> map=(Map<Integer,MonsterDens.Den>)f.get(w.monsterDens());if(map==null){map=new HashMap<>();f.set(w.monsterDens(),map);}map.put(place,den);}
 @Test public void locationAndTypeRestrictionsAndRarityBonus(){
  Locations l=new Locations();assertNull(l.getStories().monsterDens().ensure(l,1));assertNull(l.getStories().monsterDens().ensure(l,2));
  Location medium=l.getLocation(21),hard=l.getLocation(49);assertTrue(MonsterDens.eligible(21,medium));
  for(int seed=0;seed<300;seed++){MonsterDens.Den d=MonsterDens.roll(medium,l.explorationStage(21),new Random(seed));if(d.type!=0){assertEquals(MonsterDens.NORMAL,d.type);assertTrue(d.count>=6&&d.count<=8);}}
  Set<Integer> types=new HashSet<>();for(int seed=0;seed<2000;seed++){MonsterDens.Den d=MonsterDens.roll(hard,l.explorationStage(49),new Random(seed));if(d.type!=0)types.add(d.type);}assertEquals(Set.of(1,2,3),types);
  Random zero=new Random(){@Override public int nextInt(int bound){return 0;}};assertEquals(3,Item.denAdjustedRarity(2,true,zero));assertEquals(2,Item.denAdjustedRarity(2,false,zero));assertEquals(5,Item.denAdjustedRarity(5,true,zero));
 }
 @Test public void eightEnemiesAreFixedAndDefeatAwardsEarsAndValuableItem() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);MonsterDens.Den den=new MonsterDens.Den(1,8,9);put(w,26,den);
  int base=ExplorationEncounters.baseChance(l,l.getLocation(26),false);assertTrue(ExplorationEncounters.chance(l,l.getLocation(26),false)>base);
  TacticalBattle battle=TacticalBattle.createDenBattle(p,new Random(4),den);assertEquals(8,battle.units.size());assertTrue(battle.units.stream().allMatch(u->u.type()==ZombieType.NORMAL));
  for(int i=0;i<10;i++)battle.reinforce();assertEquals(8,battle.units.size());
  for(TacticalBattle.Unit u:battle.units)u.enemy.takeDamage(10000);battle.dropDead();assertTrue(battle.ground.isEmpty());
  w.monsterDens().complete(26,battle,new Random(2));assertTrue(den.cleared);assertEquals(base,ExplorationEncounters.chance(l,l.getLocation(26),false));
  assertTrue(den.rewards.stream().filter(x->x instanceof ZombieEar).count()>=8);ItemInfo weapon=den.rewards.stream().filter(x->x instanceof Weapons).findFirst().orElseThrow();assertTrue(weapon.getSalePrice()<=400);
  int count=den.rewards.size();w.monsterDens().complete(26,battle,new Random(3));assertEquals(count,den.rewards.size());
  File file=Files.createTempFile("den-rewards", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave save=GameSave.load(file);assertEquals(count,save.locations.getStories().monsterDens().existing(26).rewards.size());}finally{file.delete();}
 }
 @Test public void livingAndDeadEnemiesSurviveSaveWithExactPositions() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);MonsterDens.Den den=new MonsterDens.Den(1,8,8);put(w,26,den);
  TacticalBattle battle=TacticalBattle.createDenBattle(p,new Random(9),den);battle.units.get(0).enemy.takeDamage(10000);CombatEnemy other=battle.units.get(1).enemy;other.takeDamage(other.getHealth()-12);int cell=battle.units.get(1).cell;battle.escaped=true;w.rememberBattle(26,battle);
  File file=Files.createTempFile("den-battle", ".dat").toFile();try{GameSave.save(file,p,0,l);GameSave save=GameSave.load(file);TacticalBattle restored=save.locations.getStories().pendingBattle(26);assertTrue(restored.denEncounter);assertEquals(8,restored.units.size());assertFalse(restored.units.get(0).enemy.isAlive());assertEquals(12,restored.units.get(1).enemy.getHealth());assertEquals(cell,restored.units.get(1).cell);restored.prepareReturn();restored.reinforce();assertEquals(8,restored.units.size());assertTrue(save.locations.getStories().monsterDens().existing(26).active());}finally{file.delete();}
 }
 @Test public void actualEscapePersistsNestBattle() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);MonsterDens.Den den=new MonsterDens.Den(1,8,9);put(w,26,den);LinkedBlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();File dir=Files.createTempDirectory("den-escape").toFile();
  Random rng=new Random(5){@Override public int nextInt(int n){return n==100?0:super.nextInt(n);}};
  try(GameSession session=new GameSession(dir,frames::offer,()->{GameIO.mainMenu(p,0,l);w.beginVisit(26,"Лесничество",rng);w.monsterDens().visit(p,w,26,GameIO.scanner(),rng);})){session.start();GameSession.Frame menu=frames.poll(5,TimeUnit.SECONDS);assertNotNull(menu);assertTrue(session.submit(menu.revision,1));
   boolean sent=false;long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!sent&&System.nanoTime()<end){GameSession.Frame f=frames.poll(1,TimeUnit.SECONDS);if(f!=null&&f.mode==GameSession.Mode.CHOICE&&f.menu.board!=null){assertEquals(8,f.status.enemies.size());sent=session.submit(f.revision,7);}}assertTrue(sent);
   end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(w.pendingBattle(26)==null&&System.nanoTime()<end)Thread.sleep(10);assertNotNull(w.pendingBattle(26));assertEquals(8,w.pendingBattle(26).units.size());assertTrue(den.active());
  }finally{for(File f:Objects.requireNonNull(dir.listFiles()))f.delete();dir.delete();}
 }
 @Test public void runnerAndJumperDensContainOnlyTheirOwnType(){
  Fighter p=new Fighter("John","",100);
  for(int kind:new int[]{MonsterDens.RUNNER,MonsterDens.JUMPER}){MonsterDens.Den den=new MonsterDens.Den(kind,6,9);TacticalBattle battle=TacticalBattle.createDenBattle(p,new Random(kind),den);assertEquals(6,battle.units.size());assertTrue(battle.units.stream().allMatch(u->u.type()==den.zombieType()));battle.reinforce();assertEquals(6,battle.units.size());}
 }
 @Test public void resumedVictoryImmediatelyClearsDenAndRetainsRewards() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);MonsterDens.Den den=new MonsterDens.Den(1,6,9);put(w,26,den);TacticalBattle battle=TacticalBattle.createDenBattle(p,new Random(7),den);
  for(TacticalBattle.Unit unit:battle.units)unit.enemy.takeDamage(10000);battle.escaped=true;w.rememberBattle(26,battle);
  LinkedBlockingQueue<GameSession.Frame> frames=new LinkedBlockingQueue<>();File dir=Files.createTempDirectory("den-resume").toFile();
  try(GameSession session=new GameSession(dir,frames::offer,()->{GameIO.mainMenu(p,0,l);w.beginVisit(26,"Лесничество",new Random(1));TacticalBattle.resumePending(w,p,GameIO.scanner());})){session.start();
   boolean sent=false;long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!sent&&System.nanoTime()<end){GameSession.Frame f=frames.poll(1,TimeUnit.SECONDS);if(f!=null&&f.mode==GameSession.Mode.CHOICE&&f.menu.board!=null)sent=session.submit(f.revision,0);}assertTrue(sent);
   end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!den.cleared&&System.nanoTime()<end)Thread.sleep(10);assertTrue(den.cleared);assertNull(w.pendingBattle(26));assertTrue(den.rewards.stream().anyMatch(x->x instanceof ZombieEar));assertTrue(den.rewards.stream().anyMatch(x->x instanceof Weapons));
  }finally{for(File f:Objects.requireNonNull(dir.listFiles()))f.delete();dir.delete();}
 }
}
