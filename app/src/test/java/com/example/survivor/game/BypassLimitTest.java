package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
public class BypassLimitTest {
 @Test public void bypassChanceNeverExceedsFiftyEvenWithStealthBonus(){
  Fighter p=new Fighter("John","",100);WorldStories w=new WorldStories();
  for(int modifier=-50;modifier<=100;modifier+=5){p.searchStealthModifier=modifier;int c=SurvivalSystems.bypassChance(p,w,0);assertTrue(c>=10);assertTrue(c<=50);}
 }
}
