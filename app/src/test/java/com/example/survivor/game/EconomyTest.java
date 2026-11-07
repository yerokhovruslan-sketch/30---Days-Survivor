package com.example.survivor.game;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class EconomyTest {
    @Test public void identicalWeaponsIgnoreOriginAndLegacyMultipliers() {
        Weapons found=new Weapons("Старый карабин","",5,1800,WeaponStyle.CARBINE);
        ItemInfo bought=Shop.createItem(51);
        assertEquals(600,found.getPrice());assertEquals(300,found.getSalePrice());
        assertEquals(bought.getPrice(),found.getPrice());assertEquals(bought.getSalePrice(),found.getSalePrice());
        found.wear(50);assertEquals(210,found.getSalePrice());found.wear(50);assertEquals(120,found.getSalePrice());
        assertEquals(14,new Tools("Металлические пластины","",3,60).getPrice());
        assertNull(Shop.createItem(30));
    }
    @Test public void allPurchasableGoodsLoseMoneyOnResaleEvenDuringEvents() {
        for(int n=1;n<=Shop.LAST_PRODUCT;n++) {
            ItemInfo item=Shop.createItem(n);if(item==null)continue;
            assertTrue(item.getName(),item.getSalePrice()<item.getPrice());
            // Lowest event ask is 80%; highest bid for tools is 60%.
            assertTrue(item.getName(),item.getSalePrice()*6/5<item.getPrice()*80/100 || item.getPrice()<3);
        }
        Fighter p=new Fighter("John","",100);p.getUpgrades().bench=2;p.getUpgrades().anvil=1;
        for(Crafting.Recipe r:Crafting.RECIPES) {
            int resale=Crafting.output(r,1).stream().mapToInt(ItemInfo::getSalePrice).sum();
            int discountedCost=0;
            for(Map.Entry<String,Integer> part:r.materials.entrySet()) {
                ItemInfo material=null;
                for(int n=1;n<=61;n++){ItemInfo candidate=Shop.createItem(n);if(candidate!=null && candidate.getName().equals(part.getKey())){material=candidate;break;}}
                assertNotNull(part.getKey(),material);
                discountedCost+=Math.max(1,material.getPrice()*80/100)*part.getValue();
            }
            assertTrue(r.name+" resale="+resale+" costs="+discountedCost,resale<discountedCost);
        }
        Weapons craft=Crafting.weapon(Crafting.RECIPES.get(6));
        assertEquals(100,craft.getPrice());assertEquals(50,craft.getSalePrice());
    }
    @Test public void marketCannotRerollByReopeningAndFoodHasCategoryCap() {
        WorldStories w=new WorldStories();int[] stock=new int[62];int food=0,med=0;
        for(int n=1;n<=61;n++)if(Shop.createItem(n)!=null) {
            stock[n]=w.craftStock(n);assertTrue(stock[n]>=0 && stock[n]<=12);
            if(Economy.category(n)==0)food+=stock[n];if(Economy.category(n)==1)med+=stock[n];
        }
        assertTrue(food<=12);assertTrue(med<=5);assertEquals(2,stock[16]);
        w.buyCraftStock(16,1);assertEquals(1,w.craftStock(16));
        for(int i=0;i<100;i++)w.refreshMarket();assertEquals(1,w.craftStock(16));
        assertEquals(8640,Economy.period(51));assertEquals(5760,Economy.period(20));assertEquals(2880,Economy.period(16));
    }
    @Test public void savedMarketAndGuaranteedChipsDoNotDuplicate() throws Exception {
        Locations world=new Locations();WorldStories w=world.getStories();
        assertEquals(1,w.guaranteedChips(15).size());assertTrue(w.guaranteedChips(15).isEmpty());
        w.buyCraftStock(16,1);int ask=Economy.buyPrice(w,51);String status=w.marketStatus();
        File file=Files.createTempFile("economy",".dat").toFile();
        try {
            GameSave.save(file,new Fighter("John","",100),100,world);
            WorldStories loaded=GameSave.load(file).locations.getStories();
            assertEquals(1,loaded.craftStock(16));assertEquals(status,loaded.marketStatus());assertEquals(ask,Economy.buyPrice(loaded,51));
            assertTrue(loaded.guaranteedChips(15).isEmpty());assertEquals(1,loaded.guaranteedChips(43).size());assertTrue(loaded.guaranteedChips(43).isEmpty());
        }finally{file.delete();}
    }
    @Test public void delayedSupplyHasAnExplanationAndEventuallyMovesForward() {
        boolean sawDelay=false;
        WorldStories w=new WorldStories(73);w.refreshMarket();
        for(int i=0;i<300;i++) {
            w.advanceMinutes(2880);w.refreshMarket();
            if(w.marketEvent()==1){sawDelay=true;assertTrue(w.marketStatus().contains("перекрытую дорогу"));assertTrue(w.marketStatus().contains("Ориентировочное ожидание"));}
            for(int n=1;n<=61;n++)if(Shop.createItem(n)!=null)assertTrue(w.craftStock(n)<=12);
        }
        assertTrue(sawDelay);
    }
    @Test public void activeClockUsesOneWorldAndNeverCountsPausedIntervals() {
        ActiveClock c=new ActiveClock();WorldStories w=new WorldStories();
        assertEquals(0,c.tick(4_000_000_000L,true));assertEquals(0,c.tick(100_000_000_000L,false));
        w.advanceMinutes(c.tick(1_000_000_000L,true));assertEquals("08:01",w.time());
        w.advanceMinutes(90);assertEquals("09:31",w.time());
        w.advanceMinutes(c.tick(3_600_000_000_000L,true));assertEquals(1,w.day());assertEquals("21:31",w.time());
    }
}
