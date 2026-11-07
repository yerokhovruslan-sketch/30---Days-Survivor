package com.example.survivor.game;
import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;
public class CombatInjurySummaryTest {
 private BodyInjury injury(Fighter p,BodyPart part) throws Exception {Method m=Fighter.class.getDeclaredMethod("injury",BodyPart.class);m.setAccessible(true);return (BodyInjury)m.invoke(p,part);}
 @Test public void summaryIsCompactAndRetainsSeverityAndBleeding() throws Exception {
  Fighter p=new Fighter("John","",100);assertEquals("Травм нет",p.combatInjurySummary());
  BodyInjury arm=injury(p,BodyPart.LEFT_ARM);arm.trauma=10;assertEquals("Ушиб",p.combatInjurySummary());
  BodyInjury leg=injury(p,BodyPart.LEFT_LEG);leg.trauma=12;arm.bleeding=1;
  assertEquals("Множественные ушибы + кровотечение",p.combatInjurySummary());
  leg.trauma=35;assertEquals("Множественные травмы + кровотечение",p.combatInjurySummary());
  assertTrue(p.injurySummary().contains("Левая нога"));
 }
}
