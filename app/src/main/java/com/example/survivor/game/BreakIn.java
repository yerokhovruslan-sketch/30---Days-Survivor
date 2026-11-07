package com.example.survivor.game;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
final class BreakIn {
 private BreakIn(){}
 static boolean shouldAttack(Random random){return random.nextInt(100)<40;}
 static int count(Random random){return 1+random.nextInt(4);}
 static boolean attack(Fighter player,Scanner scanner,Random random){
  if(!GameIO.supportsTactics() || !shouldAttack(random))return true;
  int count=count(random);List<CombatEnemy> enemies=new ArrayList<>();
  for(int i=1;i<=count;i++)enemies.add(new Zombie(i,ZombieType.NORMAL));
  GameIO.out().println("Шум вскрытия привлёк заражённых: "+count+".");
  return TacticalBattle.story(player,scanner,random,enemies,"Шум вскрытия",java.util.Collections.emptyList(),false,false,false);
 }
}
