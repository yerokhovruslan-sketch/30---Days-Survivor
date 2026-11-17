package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;
public class NewEncountersTest {
 @Test public void helpConsumesOneBandageAndPreservesSeriousInjury(){Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(1));VeraStory v=new VeraStory();assertTrue(v.treat(p,new Scanner("2")));assertNull(ResidentialStories.find(p,MedicalItem::supports));assertEquals(50,v.child().trauma(BodyPart.LEFT_LEG));assertFalse(v.child().requiresTreatment(BodyPart.LEFT_LEG));}
 @Test public void postponeDoesNotConsumeAnything(){Fighter p=new Fighter("John","",100);p.addItem(Shop.createItem(1));VeraStory v=new VeraStory();assertFalse(v.treat(p,new Scanner("3")));assertNotNull(ResidentialStories.find(p,MedicalItem::supports));assertTrue(v.child().requiresTreatment(BodyPart.LEFT_LEG));}
 @Test public void giveKeyOnlyOnce(){Fighter p=new Fighter("John","",100);VeraStory v=new VeraStory();v.giveKey(p);v.giveKey(p);assertEquals(1,p.getInventory().stream().filter(i->i.getName().equals(VeraStory.KEY)).count());}
 @Test public void askingExchangeAndBackingOffPreservesStory(){Fighter p=new Fighter("John","",100);WorldStories w=new WorldStories();VeraStory v=w.vera();v.appeared=true;v.stage=4;v.interact(p,w,new Scanner("2 1"),new Random(2));assertEquals(5,v.stage);assertTrue(v.available());}
 @Test public void fourShortEncountersFinishOnceWithoutLoot(){for(int id=1;id<=4;id++){Fighter p=new Fighter("John","",100);int before=p.getInventory().size();WorldStories w=new WorldStories();ChanceMeetings c=w.chanceMeetings();c.startForTest(id,3);c.run(p,w,new Scanner("1 2 1"),new Random(1));assertTrue(c.completed(id));assertEquals(before,p.getInventory().size());}}
 @Test public void pendingAndTreatmentSurviveSave() throws Exception {WorldStories w=new WorldStories();w.vera().appeared=true;w.vera().stage=3;w.vera().child();w.chanceMeetings().startForTest(2,3);ByteArrayOutputStream b=new ByteArrayOutputStream();new ObjectOutputStream(b).writeObject(w);WorldStories loaded=(WorldStories)new ObjectInputStream(new ByteArrayInputStream(b.toByteArray())).readObject();assertEquals(3,loaded.vera().stage);assertEquals(50,loaded.vera().child().trauma(BodyPart.LEFT_LEG));assertFalse(loaded.chanceMeetings().completed(2));}
 @Test public void veraUsesLowHealthAndNoCompanion(){VeraEnemy e=new VeraEnemy();assertEquals(45,e.getHealth());assertEquals(45,e.getMaxHealth());assertFalse(e.isRaider());}
}
