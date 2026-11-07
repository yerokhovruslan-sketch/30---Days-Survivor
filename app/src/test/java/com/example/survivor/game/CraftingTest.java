package com.example.survivor.game;

import org.junit.Test;
import java.util.*;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class CraftingTest {
    private Fighter player() {Fighter p=new Fighter("John","",100);p.getUpgrades().bench=2;return p;}
    private void stock(Fighter p,Crafting.Recipe r,int batches) {
        for(Map.Entry<String,Integer> part:r.materials.entrySet())for(int i=0;i<part.getValue()*batches;i++) {
            ItemInfo item=Crafting.isComponent(part.getKey())?Crafting.component(part.getKey()):new Tools(part.getKey(),"",1,12);
            p.getStorage().add(item);
        }
    }
    @Test public void suppliedExamplesProduceExactRoundsCostAndTime() {
        Crafting.Recipe revolver=Crafting.RECIPES.get(0),shell=Crafting.RECIPES.get(2);
        assertEquals(240,revolver.cost(6));assertEquals(600,revolver.cost(15));assertEquals(180,shell.cost(3));
        Fighter p=player();WorldStories clock=new WorldStories();stock(p,revolver,6);
        assertTrue(Crafting.craft(p,clock,revolver,6,false));assertEquals(24,p.getAmmoCount(AmmoType.REVOLVER));
        assertEquals("12:00",clock.time());assertTrue(p.getStorage().isEmpty());
        assertFalse(Crafting.craft(p,clock,revolver,6,false));assertEquals(24,p.getAmmoCount(AmmoType.REVOLVER));assertEquals("12:00",clock.time());
    }
    @Test public void missingIngredientsAndOverweightNeverConsumeResourcesAndStorageWorks() {
        Crafting.Recipe r=Crafting.RECIPES.get(0);Fighter p=player();WorldStories clock=new WorldStories();stock(p,r,1);
        ItemInfo missing=p.getStorage().remove(0);int count=p.getStorage().size();
        assertFalse(Crafting.craft(p,clock,r,1,false));assertEquals(count,p.getStorage().size());assertEquals("08:00",clock.time());
        p.getStorage().add(missing);for(int i=0;i<23;i++)p.addItem(new Tools("Груз","",1,1));
        assertEquals(25000,p.getCarryWeight());assertFalse(Crafting.craft(p,clock,r,1,false));assertEquals(count+1,p.getStorage().size());
        assertTrue(Crafting.craft(p,clock,r,1,true));assertTrue(p.getStorage().stream().anyMatch(i->i instanceof Ammunition));assertEquals(25000,p.getCarryWeight());
    }
    @Test public void levelsAndAnvilGateAdvancedWeaponsAndShopCannotSellThem() {
        Fighter p=player();WorldStories clock=new WorldStories();p.getUpgrades().bench=1;
        Crafting.Recipe bolts=Crafting.RECIPES.get(4);stock(p,bolts,1);assertTrue(Crafting.craft(p,clock,bolts,1,false));assertEquals(4,p.getAmmoCount(AmmoType.BOLT));
        Crafting.Recipe katana=Crafting.RECIPES.get(7);stock(p,katana,1);
        assertFalse(Crafting.craft(p,clock,katana,1,false));p.getUpgrades().bench=2;assertFalse(Crafting.craft(p,clock,katana,1,false));
        p.getUpgrades().anvil=1;assertTrue(Crafting.craft(p,clock,katana,1,false));
        assertTrue(p.getInventory().stream().anyMatch(i->i instanceof Weapons && ((Weapons)i).getStyle()==WeaponStyle.KATANA));
        assertNull(Shop.createItem(5));assertNull(Shop.createItem(30));assertNull(Shop.createItem(32));
    }
    @Test public void benchTwoRecipeCostsSixHoursAndKeepsTool() {
        Fighter p=player();p.getUpgrades().bench=1;WorldStories clock=new WorldStories();
        int[] products={19,44,43,42},counts={6,3,4,2};
        for(int j=0;j<products.length;j++)for(int i=0;i<counts[j];i++)p.getStorage().add(Shop.createItem(products[j]));
        ItemInfo tool=Shop.createItem(20);p.getStorage().add(tool);
        assertTrue(p.getUpgrades().build(p,2,clock));assertEquals(2,p.getUpgrades().bench);assertEquals("14:00",clock.time());assertTrue(p.getStorage().contains(tool));assertEquals(1,p.getStorage().size());
    }
    @Test public void dismantlingCannotDuplicateOrMakePurchasableAmmoComponentsFromCans() {
        Fighter p=player();WorldStories clock=new WorldStories();ItemInfo pipe=p.getEquippedWeapon();
        assertTrue(Crafting.dismantle(p,pipe,clock));assertNull(p.getEquippedWeapon());assertFalse(Crafting.dismantle(p,pipe,clock));
        assertFalse(p.getStorage().isEmpty());assertTrue(Crafting.salvage(p,Shop.createItem(17)).isEmpty());
        assertTrue(Crafting.salvage(p,Crafting.component(Crafting.POWDER)).isEmpty());
        assertTrue(p.getStorage().stream().noneMatch(i->i.getName().equals(Crafting.POWDER)||i.getName().equals(Crafting.KIT)));
    }
    @Test public void resaleCannotTurnCraftingIntoMoneyFactoryAndRetailMatchesTable() {
        int[] prices={64,66,96,104,36},products={13,52,12,53,14};
        for(int i=0;i<products.length;i++)assertEquals(prices[i],Shop.createItem(products[i]).getPrice());
        for(Crafting.Recipe r:Crafting.RECIPES)assertTrue(Crafting.output(r,1).stream().mapToInt(ItemInfo::getSalePrice).sum()<r.cost(1));
    }
    @Test public void rareShopStocksAreFiniteAndDifferentDaysHaveDifferentSupplies() {
        WorldStories w=new WorldStories();int powder=w.craftStock(54),kits=w.craftStock(59);
        assertTrue(powder>=0 && powder<=5);assertTrue(kits>=0 && kits<=3);
        w.buyCraftStock(54,powder);w.buyCraftStock(59,kits);assertEquals(0,w.craftStock(54));assertEquals(0,w.craftStock(59));
        w.advanceMinutes(60);assertEquals(0,w.craftStock(54));assertEquals(0,w.craftStock(59));
        w.advanceMinutes(2880);assertTrue(w.craftStock(54)<=5);

    }
    @Test public void revolverCasesReturnOnReloadOnceAndOtherGunsDropCases() {
        Fighter p=player();Weapons gun=(Weapons)Shop.createItem(9);p.addItem(gun);p.equipWeapon(gun);p.addItem(new Ammunition(AmmoType.REVOLVER));p.addItem(new Ammunition(AmmoType.REVOLVER));
        TacticalBattle b=new TacticalBattle(p,1,new Random(3),Arrays.asList(new Zombie("Target","",1000)));Arrays.fill(b.obstacles,null);b.playerCell=56;b.units.get(0).cell=57;
        assertTrue(b.attack());assertEquals(1,p.heldCases(gun));assertTrue(b.spentCases.isEmpty());assertTrue(b.reload());assertEquals(0,p.heldCases(gun));
        assertEquals(1,p.getInventory().stream().filter(i->i.getName().equals(Crafting.casing(AmmoType.REVOLVER))).count());assertFalse(b.extractCases());
        b.casingShot(WeaponStyle.PISTOL);b.casingShot(WeaponStyle.SHOTGUN);assertEquals(Integer.valueOf(1),b.spentCases.get(AmmoType.PISTOL));assertEquals(Integer.valueOf(1),b.spentCases.get(AmmoType.SHELL));
    }
    @Test public void oldAmmoStacksRemainValidAndCraftingStatePersists() throws Exception {
        Fighter p=player();p.getUpgrades().anvil=1;Weapons gun=(Weapons)Shop.createItem(9);p.addItem(gun);p.equipWeapon(gun);p.noteShot(gun);
        p.addItem(new Ammunition(AmmoType.REVOLVER,6));Locations world=new Locations();int remaining=Math.max(0,world.getStories().craftStock(54)-2);world.getStories().buyCraftStock(54,2);world.getStories().addCases(1,AmmoType.PISTOL,4);
        File file=Files.createTempFile("craft-state",".dat").toFile();
        try {GameSave.save(file,p,0,world);GameSave save=GameSave.load(file);Fighter loaded=(Fighter)save.player;
            assertEquals(1,loaded.getUpgrades().anvil);assertEquals(1,loaded.heldCases(loaded.getEquippedWeapon()));assertEquals(6,loaded.getAmmoCount(AmmoType.REVOLVER));
            assertEquals(remaining,save.locations.getStories().craftStock(54));assertEquals(4,save.locations.getStories().caseCount(1));assertEquals(4,save.locations.getStories().takeCases(1)[AmmoType.PISTOL.ordinal()]);assertEquals(0,save.locations.getStories().caseCount(1));
        } finally {file.delete();}
    }
}
