package com.example.survivor.game;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class WeaponBalanceTest {
    @Test public void newGunsAndAmmoSurviveSavingAndScreamerCannotMoveWithDamagedLegs() throws Exception {
        Fighter player=new Fighter("John","",100);Weapons pistol=(Weapons)Shop.createItem(50);
        player.addItem(pistol);player.equipWeapon(pistol);player.addItem(Shop.createItem(52));player.addItem(Shop.createItem(53));
        java.io.File file=java.nio.file.Files.createTempFile("new-ammo",".dat").toFile();
        try {
            GameSave.save(file,player,7,new Locations());Fighter loaded=(Fighter)GameSave.load(file).player;
            assertEquals(WeaponStyle.PISTOL,loaded.getWeaponStyle());assertEquals(6,loaded.getAmmoCount(AmmoType.PISTOL));assertEquals(4,loaded.getAmmoCount(AmmoType.RIFLE));
        } finally {file.delete();}
        TacticalBattle b=battle(WeaponStyle.HUNTING_KNIFE,new Zombie(1,ZombieType.SCREAMER),0);
        b.units.get(0).cell=40;b.units.get(0).slow=2;int cell=b.units.get(0).cell;
        b.enemyTurn();assertEquals(cell,b.units.get(0).cell);assertEquals(1,b.units.get(0).slow);
    }
    private TacticalBattle battle(WeaponStyle style,CombatEnemy enemy,int roll) {
        Fighter p=new Fighter("John","",100);p.getInventory().clear();
        Weapons weapon=new Weapons("Test","",1,1,style);p.addItem(weapon);p.equipWeapon(weapon);
        if(weapon.getAmmoType()!=null){p.addItem(new Ammunition(weapon.getAmmoType()));p.addItem(new Ammunition(weapon.getAmmoType()));}
        Random rng=new Random(1) {@Override public int nextInt(int bound){return Math.min(roll,bound-1);}};
        TacticalBattle b=new TacticalBattle(p,1,rng,Arrays.asList(enemy));Arrays.fill(b.obstacles,null);
        b.playerCell=56;b.units.get(0).cell=57;return b;
    }
    @Test public void exhaustedJohnCanFireAndRevolverHasExactNewNoiseAndDamage() {
        TacticalBattle b=battle(WeaponStyle.REVOLVER,new Zombie("Target","",1000),0);
        b.player.spendAvailableStamina(100);int ammo=b.player.getAmmoCount(AmmoType.REVOLVER);
        assertEquals(0,b.player.getStamina());assertTrue(b.attack());
        assertEquals(0,b.player.getStamina());assertEquals(ammo-1,b.player.getAmmoCount(AmmoType.REVOLVER));
        assertEquals(962,b.units.get(0).enemy.getHealth());assertEquals(25,b.player.getNoise());assertEquals(5,b.loaded());
    }
    @Test public void twoKnifeStrikesCostElevenAndZombieEffectDoesNotBleed() {
        TacticalBattle b=battle(WeaponStyle.HUNTING_KNIFE,new Zombie("Target","",1000),0);
        assertTrue(b.attack());assertEquals(89,b.player.getStamina());assertEquals(984,b.units.get(0).enemy.getHealth());
        assertEquals(1,b.player.getNoise());assertEquals(2,b.units.get(0).slow);assertEquals(0,b.units.get(0).bleed);
        b.enemyTurn();assertEquals(984,b.units.get(0).enemy.getHealth());assertEquals(1,b.units.get(0).slow);
    }
    @Test public void bleedingDamagesHumansThroughArmorAndMissCannotApplyIt() {
        HostileSurvivor human=new HostileSurvivor("Raider",new Weapons("Club","",1,1,WeaponStyle.CLUB),55);
        Tools armor=(Tools)Shop.createItem(21);human.takeGroundItem(armor);
        TacticalBattle b=battle(WeaponStyle.HUNTING_KNIFE,human,0);assertTrue(b.attack());assertEquals(3,b.units.get(0).bleed);
        int hp=human.getHealth(),condition=armor.getDurability();b.enemyTurn();
        assertEquals(hp-4,human.getHealth());assertEquals(condition,armor.getDurability());
        TacticalBattle miss=battle(WeaponStyle.AXE,new Zombie("Target","",1000),99);assertTrue(miss.attack());
        assertEquals(1000,miss.units.get(0).enemy.getHealth());assertEquals(0,miss.units.get(0).slow);
    }
    @Test public void aimingIsConsumedByShotCancelledByMovementAndCappedAtNinetyFive() {
        TacticalBattle b=battle(WeaponStyle.PISTOL,new Zombie("Target","",1000),94);
        assertTrue(b.attack());assertEquals(1000,b.units.get(0).enemy.getHealth());
        assertTrue(b.aim());assertTrue(b.attack());assertTrue(b.units.get(0).enemy.getHealth()<1000);assertFalse(b.aimed);
        assertTrue(b.aim());assertTrue(b.move(48));assertFalse(b.aimed);
        TacticalBattle cap=battle(WeaponStyle.PISTOL,new Zombie("Target","",1000),95);
        cap.aim();cap.attack();assertEquals(1000,cap.units.get(0).enemy.getHealth());
    }
    @Test public void shotgunLosesDamageWithDistanceAndSpearIsWeakerAtPointBlank() {
        TacticalBattle near=battle(WeaponStyle.SHOTGUN,new Zombie("Target","",1000),0);
        TacticalBattle far=battle(WeaponStyle.SHOTGUN,new Zombie("Target","",1000),0);far.units.get(0).cell=60;
        near.attack();far.attack();assertEquals(945,near.units.get(0).enemy.getHealth());assertEquals(976,far.units.get(0).enemy.getHealth());
        assertEquals(100,far.player.getStamina());assertEquals(45,far.player.getNoise());
        TacticalBattle spear=battle(WeaponStyle.LONG,new Zombie("Target","",1000),0);spear.attack();assertEquals(991,spear.units.get(0).enemy.getHealth());
        TacticalBattle longSpear=battle(WeaponStyle.LONG,new Zombie("Target","",1000),0);longSpear.units.get(0).cell=58;longSpear.attack();
        assertEquals(988,longSpear.units.get(0).enemy.getHealth());assertEquals(87,longSpear.player.getStamina());
    }
    @Test public void obstacleBreakingCostsExactStaminaAndUsesCorrectTools() {
        TacticalBattle axe=battle(WeaponStyle.FIRE_AXE,new Zombie("Target","",1000),0);axe.units.get(0).cell=0;
        axe.obstacles[57]=TacticalBattle.Obstacle.FENCE;axe.selectedObstacle=57;
        assertTrue(axe.destroy());assertNotNull(axe.obstacles[57]);assertTrue(axe.destroy());assertNull(axe.obstacles[57]);assertEquals(44,axe.player.getStamina());
        TacticalBattle hammer=battle(WeaponStyle.SLEDGEHAMMER,new Zombie("Target","",1000),0);hammer.units.get(0).cell=0;
        hammer.obstacles[57]=TacticalBattle.Obstacle.BLOCK;hammer.selectedObstacle=57;
        assertTrue(hammer.destroy());assertTrue(hammer.destroy());assertTrue(hammer.destroy());assertNull(hammer.obstacles[57]);assertEquals(4,hammer.player.getStamina());
        TacticalBattle wrong=battle(WeaponStyle.FIRE_AXE,new Zombie("Target","",1000),0);wrong.units.get(0).cell=0;
        wrong.obstacles[57]=TacticalBattle.Obstacle.BLOCK;wrong.selectedObstacle=57;assertFalse(wrong.destroy());assertEquals(100,wrong.player.getStamina());
    }
    @Test public void newFirearmsUseSeparateAmmoAndCapacities() {
        Weapons pistol=(Weapons)Shop.createItem(50),carbine=(Weapons)Shop.createItem(51);
        assertEquals(AmmoType.PISTOL,pistol.getAmmoType());assertEquals(AmmoType.RIFLE,carbine.getAmmoType());
        assertEquals(12,TacticalBattle.capacity(pistol));assertEquals(5,TacticalBattle.capacity(carbine));
        assertEquals(AmmoType.PISTOL,((Ammunition)Shop.createItem(52)).getType());assertEquals(AmmoType.RIFLE,((Ammunition)Shop.createItem(53)).getType());
    }
}
