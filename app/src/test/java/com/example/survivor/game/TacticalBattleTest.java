package com.example.survivor.game;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class TacticalBattleTest {
    @Test public void playerAndTargetSnapshotsNeverShareHealthAndLogsNameTheAttacker() {
        TacticalBattle b=battle(new Zombie(1,ZombieType.NORMAL));
        b.player.takeDamage(23);b.player.increaseHunger(15);
        TacticalBattle.Unit enemy=b.units.get(0);enemy.cell=57;enemy.enemy.takeDamage(19);
        GameStatus status=new GameStatus(b.player,Arrays.asList(enemy.enemy),enemy.enemy);
        assertEquals(77,b.player.getHealth());assertEquals(15,b.player.getHunger());
        assertEquals(41,status.enemies.get(0).health);assertEquals(60,status.enemies.get(0).maxHealth);
        assertTrue(status.enemies.get(0).selected);
        BattleBoard board=b.snapshot();assertEquals(0,board.cells.get(b.playerCell).unit);
        assertEquals(77,board.cells.get(b.playerCell).health);assertEquals(41,board.cells.get(57).health);
        assertTrue(b.attack());assertTrue(b.log.stream().anyMatch(line->line.startsWith("Джон атакует ")));
        b.strike(enemy,false);
        assertTrue(b.log.get(b.log.size()-1).contains("Джона") || b.log.get(b.log.size()-1).contains("Джону"));
    }
    @Test public void survivorCanScavengeGunAmmoAndArmorAndReturnsSameObjectsOnDeath() {
        HostileSurvivor human=new HostileSurvivor(1,false,new Random(3));
        Weapons gun=(Weapons)Shop.createItem(8);Ammunition ammo=new Ammunition(AmmoType.SHELL);
        ItemInfo armor=Shop.createItem(21);
        assertTrue(human.takeGroundItem(gun));assertFalse(human.isRanged());
        assertTrue(human.takeGroundItem(ammo));assertTrue(human.isRanged());assertTrue(human.takeGroundItem(armor));
        int before=ammo.getQuantity();human.spendAttackAmmo();assertEquals(before-1,ammo.getQuantity());
        human.takeDamage(10000);List<ItemInfo> drops=human.claimLoot();
        assertTrue(drops.contains(gun));assertTrue(drops.contains(ammo));assertTrue(drops.contains(armor));
        assertTrue(human.claimLoot().isEmpty());
    }
    private TacticalBattle battle(CombatEnemy enemy) {
        TacticalBattle b=new TacticalBattle(new Fighter("John","",100),1,new Random(42),Arrays.asList(enemy));
        Arrays.fill(b.obstacles,null);b.playerCell=56;b.units.get(0).cell=0;return b;
    }
    @Test public void generatedFieldsHaveValidConnectedSquaresAndNoOverlaps() {
        for(int seed=0;seed<200;seed++) {
            Fighter p=new Fighter("John","",100);p.addNoise(seed%61);
            TacticalBattle b=new TacticalBattle(p,3,new Random(seed),Arrays.asList(new Zombie(1,ZombieType.NORMAL)));
            long count=Arrays.stream(b.obstacles).filter(Objects::nonNull).count();
            assertTrue(count>=3 && count<=7);assertTrue(b.connected());assertTrue(TacticalBattle.edge(b.playerCell));
            Set<Integer> occupied=new HashSet<>();occupied.add(b.playerCell);
            for(TacticalBattle.Unit u:b.units) {assertNull(b.obstacles[u.cell]);assertTrue(occupied.add(u.cell));}
            assertTrue(b.units.size()>=1 && b.units.size()<=6);
            assertEquals(64,b.snapshot().cells.size());
        }
    }
    @Test public void movementIsStraightAndCannotCrossObstaclesOrRepeatBeforeAction() {
        TacticalBattle b=battle(new Zombie(1,ZombieType.NORMAL));
        assertFalse(b.canMove(49));assertTrue(b.canMove(58));
        b.obstacles[57]=TacticalBattle.Obstacle.CRATE;assertFalse(b.canMove(58));b.obstacles[57]=null;
        int stamina=b.player.getStamina();assertTrue(b.move(58));assertEquals(stamina-6,b.player.getStamina());
        assertFalse(b.canMove(59));assertEquals(56+2,b.playerCell);
        b.moved=false;b.player.increaseSleepiness(60);assertEquals(1,b.moveLimit());assertFalse(b.canMove(60));
    }
    @Test public void wallsBlockShotsBushesPenalizeAndCornersCannotBeShotThrough() {
        TacticalBattle b=battle(new Zombie(1,ZombieType.NORMAL));
        b.obstacles[57]=TacticalBattle.Obstacle.WALL;assertFalse(b.clearShot(56,59));
        b.obstacles[57]=TacticalBattle.Obstacle.BUSH;assertTrue(b.clearShot(56,59));assertTrue(b.shotPenalty(56,59)>=20);
        b.obstacles[57]=TacticalBattle.Obstacle.WALL;assertFalse(b.clearShot(56,49));
    }
    @Test public void magazineMustBeReloadedAndAmmoIsConsumedOnlyByShots() {
        TacticalBattle b=battle(new Zombie("Target","",1000));b.units.get(0).cell=59;
        Weapons gun=(Weapons)Shop.createItem(8);b.player.addItem(gun);b.player.equipWeapon(gun);
        b.player.addItem(new Ammunition(AmmoType.SHELL));
        assertEquals(2,b.loaded());assertTrue(b.attack());assertTrue(b.attack());assertEquals(0,b.loaded());
        int ammo=b.player.getAmmoCount(AmmoType.SHELL),stamina=b.player.getStamina();
        assertFalse(b.attack());assertEquals(ammo,b.player.getAmmoCount(AmmoType.SHELL));assertEquals(stamina,b.player.getStamina());
        assertTrue(b.reload());assertEquals(ammo,b.player.getAmmoCount(AmmoType.SHELL));assertEquals(2,b.loaded());
    }
    @Test public void jumpIsTelegraphedAndPlayerCanDodgeMarkedCell() {
        TacticalBattle b=battle(new Zombie(1,ZombieType.JUMPER));b.units.get(0).cell=32;
        b.enemyTurn();assertEquals(56,b.units.get(0).jump);
        assertTrue(b.snapshot().cells.get(56).hazard);assertTrue(b.move(57));
        int health=b.player.getHealth();b.enemyTurn();assertEquals(health,b.player.getHealth());
        assertEquals(-1,b.units.get(0).jump);assertNotEquals(b.playerCell,b.units.get(0).cell);
    }
    @Test public void bruteDestroysLightObstacleAndDeathDropsActualObjectsOnlyOnce() {
        TacticalBattle b=battle(new Zombie(1,ZombieType.BRUTE));b.units.get(0).cell=40;b.obstacles[48]=TacticalBattle.Obstacle.CRATE;
        b.enemyTurn();assertNull(b.obstacles[48]);assertEquals(48,b.units.get(0).cell);assertTrue(b.player.getNoise()>0);
        HostileSurvivor human=new HostileSurvivor(2,true,new Random(4));
        b.units.clear();b.units.add(new TacticalBattle.Unit(human,57));human.takeDamage(10000);b.dropDead();
        List<ItemInfo> pile=b.ground.get(57);assertNotNull(pile);int count=pile.size();ItemInfo original=pile.get(0);
        b.dropDead();assertEquals(count,pile.size());assertTrue(b.pickup(new Scanner("1")));
        assertTrue(b.player.getInventory().contains(original));assertFalse(pile.contains(original));assertTrue(human.claimLoot().isEmpty());
    }
    @Test public void escapingRequiresEdgeAndInjuredWandererCanFlee() {
        TacticalBattle b=battle(new Zombie(1,ZombieType.NORMAL));b.playerCell=27;
        assertFalse(b.escape());assertFalse(b.escaped);
        HostileSurvivor wanderer=new HostileSurvivor(1,false,new Random(2));wanderer.takeDamage(45);
        b.units.clear();TacticalBattle.Unit unit=new TacticalBattle.Unit(wanderer,0);b.units.add(unit);
        b.enemyTurn();assertTrue(unit.fled);assertTrue(b.won());
    }
    @Test public void deadScreamerCannotSummonAndReinforcementsRespectSixLiveEnemies() {
        TacticalBattle b=battle(new Zombie(1,ZombieType.SCREAMER));b.units.get(0).cell=32;b.enemyTurn();
        assertTrue(b.units.get(0).screamAt>0);b.units.get(0).enemy.takeDamage(1000);b.enemyTurn();assertEquals(1,b.units.size());
        b.units.clear();for(int i=0;i<6;i++)b.units.add(new TacticalBattle.Unit(new Zombie(i,ZombieType.NORMAL),i));
        b.reinforce();assertEquals(6,b.units.size());
    }
}
