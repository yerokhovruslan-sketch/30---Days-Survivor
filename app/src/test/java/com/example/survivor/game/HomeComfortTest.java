package com.example.survivor.game;
import org.junit.Test;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;
public class HomeComfortTest {
 @Test public void coldAccumulatesEvenInOneMinuteSteps(){Fighter p=new Fighter("John","",100);for(int i=0;i<180;i++)p.updateCold(1,true,false);assertEquals("Холодно",p.coldState());p.updateCold(300,false,true);assertEquals("Комфортно",p.coldState());}
 @Test public void coatAndArmorAreIndependent(){Fighter p=new Fighter("John","",100);ItemInfo coat=new Tools("Зимняя куртка","",1,70);ItemInfo armor=Shop.createItem(21);p.addItem(coat);p.addItem(armor);p.wearClothing(coat);assertTrue(p.equipArmor(armor));assertSame(coat,p.worn(1));assertSame(armor,p.getEquippedArmor());}
 @Test public void clothingAndBeddingPersist() throws Exception {Fighter p=new Fighter("John","",100);ItemInfo coat=new Tools("Зимняя куртка","",1,70);p.addItem(coat);p.wearClothing(coat);p.getUpgrades().pillow=new JunkLoot.JunkItem(JunkLoot.CATEGORIES[2][0]);p.getUpgrades().fireUntil=5000;ByteArrayOutputStream out=new ByteArrayOutputStream();new ObjectOutputStream(out).writeObject(p);Fighter q=(Fighter)new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();assertEquals("Зимняя куртка",q.worn(1).getName());assertNotNull(q.getUpgrades().pillow);assertEquals(5000,q.getUpgrades().fireUntil);}
 @Test public void bedOnlyConsumesOneItemPerSlot(){Fighter p=new Fighter("John","",100);p.getInventory().add(new JunkLoot.JunkItem(JunkLoot.CATEGORIES[2][0]));p.getInventory().add(new JunkLoot.JunkItem(JunkLoot.CATEGORIES[2][1]));HomeComfort.bed(p,new Scanner("1 1 1 0"));assertNotNull(p.getUpgrades().pillow);assertNotNull(HomeComfort.find(p,"Чистая подушка"));}
 @Test public void fireConsumesWoodAndExtendsDuration(){Fighter p=new Fighter("John","",100);WorldStories w=new WorldStories();p.getInventory().add(Crafting.component(Crafting.WOOD));p.getInventory().add(Crafting.component(Crafting.WOOD));HomeComfort.fire(p,w,new Scanner("1 1 0"));assertEquals(ShelterUpgrades.minute(w)+120,p.getUpgrades().fireUntil);assertNull(HomeComfort.find(p,Crafting.WOOD));}
}
