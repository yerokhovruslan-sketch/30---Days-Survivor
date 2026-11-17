package com.example.survivor.game;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.Scanner;
import static org.junit.Assert.*;
public class TreatmentUpdateTest {
 private BodyInjury injury(Fighter p,BodyPart part)throws Exception{Method m=Fighter.class.getDeclaredMethod("injury",BodyPart.class);m.setAccessible(true);return (BodyInjury)m.invoke(p,part);}
 @Test public void untreatedBruiseHealsAndShowsEstimatedDays() throws Exception {
  Fighter p=new Fighter("John","",100);BodyInjury arm=injury(p,BodyPart.LEFT_ARM);arm.trauma=24;
  assertTrue(p.injurySummary().contains("заживёт самостоятельно"));assertFalse(p.injurySummary().contains("требуется обработка"));assertFalse(p.requiresTreatment(BodyPart.LEFT_ARM));
  p.recoverInjuries(1439);assertEquals(1,arm.trauma);p.recoverInjuries(1);assertEquals(0,arm.trauma);
 }
 @Test public void bleedingBruiseNeedsTreatmentAndOldSavedBandageWorks() throws Exception {
  Fighter p=new Fighter("John","",100);BodyInjury arm=injury(p,BodyPart.LEFT_ARM);arm.trauma=12;arm.bleeding=5;
  ItemInfo old=new Tools("Кровоостанавливающий бинт","",3,35);p.addItem(old);assertTrue(MedicalItem.supports(old));assertTrue(p.requiresTreatment(BodyPart.LEFT_ARM));
  assertTrue(Survivor.inspectBody(p,new Scanner("1 1")));assertEquals(0,arm.bleeding);assertTrue(arm.treated);assertFalse(p.getInventory().contains(old));
 }
 @Test public void otherExistingDressingsAreRecognisedAndToolsAreNot(){
  for(String name:new String[]{"Эластичный бинт","Стерильная повязка","Перевязочный комплект","Медицинский набор"})assertTrue(MedicalItem.supports(new Tools(name,"",1,1)));
  assertFalse(MedicalItem.supports(new Tools("Медицинские ножницы","",1,1)));
 }
 @Test public void oldHaemostaticBandageCanBeUsedInCombat() throws Exception {
  Fighter p=new Fighter("John","",100);BodyInjury arm=injury(p,BodyPart.LEFT_ARM);arm.trauma=12;arm.bleeding=5;
  ItemInfo old=new Tools("Кровоостанавливающий бинт","",3,35);p.addItem(old);
  TacticalBattle b=new TacticalBattle(p,1,new java.util.Random(1),java.util.List.of(new Zombie(1,ZombieType.NORMAL)));
  assertTrue(b.items(new Scanner("1 1")));assertEquals(0,arm.bleeding);assertFalse(p.getInventory().contains(old));
 }
}