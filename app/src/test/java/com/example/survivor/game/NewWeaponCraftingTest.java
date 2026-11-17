package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
public class NewWeaponCraftingTest {
 @Test public void rangedWeaponsCraftIntoStorageAndSpendIngredientsAndTime(){
  for(WeaponStyle style:new WeaponStyle[]{WeaponStyle.SLINGSHOT,WeaponStyle.CROSSBOW,WeaponStyle.PISTOL,WeaponStyle.REVOLVER,WeaponStyle.SHOTGUN,WeaponStyle.CARBINE}){
   Fighter p=new Fighter("John","",100);p.getUpgrades().bench=2;p.getUpgrades().anvil=1;WorldStories w=new WorldStories();
   Crafting.Recipe r=Crafting.RECIPES.stream().filter(x->x.weapon==style).findFirst().orElseThrow();
   r.materials.forEach((name,count)->{for(int i=0;i<count;i++){ItemInfo item=null;for(int n=1;n<=Shop.LAST_PRODUCT;n++){ItemInfo candidate=Shop.createItem(n);if(candidate!=null&&candidate.getName().equals(name)){item=candidate;break;}}assertNotNull(name,item);p.getStorage().add(item);}});
   int before=w.absoluteMinute();assertTrue(Crafting.craft(p,w,r,1,true));assertEquals(before+r.minutes,w.absoluteMinute());
   assertEquals(1,p.getStorage().stream().filter(x->x instanceof Weapons && ((Weapons)x).getStyle()==style).count());
  }
 }
}