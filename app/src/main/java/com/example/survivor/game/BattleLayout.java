package com.example.survivor.game;

import java.util.*;

/** Related objects are grouped around a focal obstacle; accessibility is checked after every placement. */
final class BattleLayout {
 private static final int SECONDARY_CHANCE=58, STRAY_CHANCE=22, MAX_PRIMARY_TRIES=64;
 private enum Scene {
  BUS("Брошенный автобус",TacticalBattle.Obstacle.BUS,new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.TRASH,TacticalBattle.Obstacle.CRATE,TacticalBattle.Obstacle.BARREL,TacticalBattle.Obstacle.RUBBLE,TacticalBattle.Obstacle.PALLETS}),
  CAR("Заброшенная машина",TacticalBattle.Obstacle.CAR,new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.TRASH,TacticalBattle.Obstacle.CRATE,TacticalBattle.Obstacle.BARREL,TacticalBattle.Obstacle.RUBBLE}),
  BARRICADE("Разрушенная баррикада",TacticalBattle.Obstacle.ROADBLOCK,new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.BLOCK,TacticalBattle.Obstacle.SANDBAGS,TacticalBattle.Obstacle.BRICK_WALL,TacticalBattle.Obstacle.RUBBLE}),
  SUPPLIES("Складской участок",TacticalBattle.Obstacle.CRATES,new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.PALLETS,TacticalBattle.Obstacle.BARREL,TacticalBattle.Obstacle.CRATE,TacticalBattle.Obstacle.PIPE}),
  ROCKS("Скопление валунов",TacticalBattle.Obstacle.ROCK,new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.ROCK,TacticalBattle.Obstacle.BUSH,TacticalBattle.Obstacle.STUMP}),
  THICKET("Лесные заросли",TacticalBattle.Obstacle.TREE,new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.BUSH,TacticalBattle.Obstacle.ROCK,TacticalBattle.Obstacle.STUMP,TacticalBattle.Obstacle.FALLEN_TREE}),
  CAMP("Следы лагеря",TacticalBattle.Obstacle.TREE,new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.BUSH,TacticalBattle.Obstacle.TRASH,TacticalBattle.Obstacle.CRATE,TacticalBattle.Obstacle.ROCK});
  final String label;final TacticalBattle.Obstacle main;final TacticalBattle.Obstacle[] nearby;
  Scene(String label,TacticalBattle.Obstacle main,TacticalBattle.Obstacle[] nearby){this.label=label;this.main=main;this.nearby=nearby;}
 }
 static void generate(TacticalBattle b,int area,int enemies){
  b.clearObstacles();b.layoutHasSecondary=false;b.layoutPrimary=-1;
  int target=7+b.random.nextInt(6);
  boolean nature=area==2;Scene[] choices=nature?new Scene[]{Scene.ROCKS,Scene.THICKET,Scene.CAMP}
       :area==1||area==5?new Scene[]{Scene.SUPPLIES,Scene.CAR,Scene.BARRICADE}
       :new Scene[]{Scene.BUS,Scene.CAR,Scene.BARRICADE};
  Scene scene=choices[b.random.nextInt(choices.length)];b.layoutProfile=scene.label;
  List<Integer> anchors=candidates();Collections.shuffle(anchors,b.random);
  // Biased toward the middle, while allowing edge placements on some boards.
  if(b.random.nextInt(100)<70)anchors.sort(Comparator.comparingInt(c->Math.abs(c%8-3)+Math.abs(c/8-3)));
  int tries=0;for(int cell:anchors){if(tries++>=MAX_PRIMARY_TRIES)break;boolean vertical=b.random.nextBoolean();
   if(tryPlace(b,cell,scene.main,vertical,enemies)){b.layoutPrimary=cell;break;}
   if((scene.main==TacticalBattle.Obstacle.CAR||scene.main==TacticalBattle.Obstacle.BUS)&&tryPlace(b,cell,scene.main,!vertical,enemies)){b.layoutPrimary=cell;break;}
  }
  if(b.layoutPrimary<0){for(int cell:anchors)if(tryPlace(b,cell,TacticalBattle.Obstacle.ROCK,false,enemies)){b.layoutPrimary=cell;break;}}
  if(b.layoutPrimary<0)return;
  int cluster=Math.min(target-1,3+b.random.nextInt(4));addAround(b,b.layoutPrimary,scene.nearby,cluster,enemies);
  if(b.random.nextInt(100)<SECONDARY_CHANCE){
   TacticalBattle.Obstacle[] extras=nature?new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.ROCK,TacticalBattle.Obstacle.TREE,TacticalBattle.Obstacle.BUSH}
      :scene==Scene.BUS?new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.CAR,TacticalBattle.Obstacle.ROADBLOCK,TacticalBattle.Obstacle.BARREL}
      :new TacticalBattle.Obstacle[]{TacticalBattle.Obstacle.BLOCK,TacticalBattle.Obstacle.BARREL,TacticalBattle.Obstacle.WALL};
   Collections.shuffle(anchors,b.random);
   for(int cell:anchors){if(TacticalBattle.distance(cell,b.layoutPrimary)<4)continue;
    TacticalBattle.Obstacle obj=extras[b.random.nextInt(extras.length)];boolean vertical=b.random.nextBoolean();
    if(count(b)>=target)break;
    if(tryPlace(b,cell,obj,vertical,enemies)){b.layoutHasSecondary=true;addAround(b,cell,scene.nearby,Math.min(target-count(b),b.random.nextInt(3)),enemies);break;}
   }
  }
  if(count(b)<target&&b.random.nextInt(100)<STRAY_CHANCE){Collections.shuffle(anchors,b.random);for(int cell:anchors){if(TacticalBattle.distance(cell,b.layoutPrimary)<3)continue;if(tryPlace(b,cell,scene.nearby[b.random.nextInt(scene.nearby.length)],false,enemies))break;}}
  Collections.shuffle(anchors,b.random);
  for(int cell:anchors){if(count(b)>=target)break;tryPlace(b,cell,scene.nearby[b.random.nextInt(scene.nearby.length)],false,enemies);}
 }
 private static int count(TacticalBattle b){int n=0;for(int i=0;i<64;i++)if(b.obstacles[i]!=null&&b.obstaclePart(i)==0)n++;return n;}
 private static void addAround(TacticalBattle b,int center,TacticalBattle.Obstacle[] types,int requested,int enemies){
  List<Integer> ring=new ArrayList<>();int span=b.obstacleSpan(center),step=b.obstacleIsVertical(center)?8:1;
  for(int c=0;c<64;c++){
   int min=99;for(int i=0;i<span;i++){int a=center+i*step;int d=Math.max(Math.abs(c/8-a/8),Math.abs(c%8-a%8));min=Math.min(min,d);}
   if(min>0&&min<=2)ring.add(c);
  }
  Collections.shuffle(ring,b.random);int placed=0;
  for(int cell:ring){if(placed>=requested)break;TacticalBattle.Obstacle o=types[b.random.nextInt(types.length)];if(tryPlace(b,cell,o,b.random.nextBoolean(),enemies))placed++;}
 }
 private static boolean tryPlace(TacticalBattle b,int cell,TacticalBattle.Obstacle obstacle,boolean vertical,int enemies){
  if(!b.placeObstacle(cell,obstacle,vertical))return false;
  boolean valid=b.connected();int adjacent=0,spawn=0;
  for(int n:b.neighbours(b.playerCell))if(b.obstacles[n]==null&&b.unitAt(n)==null&&b.civilianAt(n)==null)adjacent++;
  for(int i=0;i<64;i++)if(b.free(i)&&TacticalBattle.distance(i,b.playerCell)>=4)spawn++;
  if(!valid||adjacent<1||spawn<Math.max(2,Math.min(8,enemies))){b.removeObstacle(cell);return false;}
  return true;
 }
 private static List<Integer> candidates(){List<Integer> list=new ArrayList<>();for(int i=0;i<64;i++)list.add(i);return list;}
}
