package com.example.survivor.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable snapshots: the UI never reads mutable game entities on its own thread. */
public final class GameStatus {
    public final String weapon, armor, load, ammo, injuries, combatInjuries;
    public final int weaponCondition, protection;
    public final List<Enemy> enemies;

    public static final class Enemy {
        public final int number, health, maxHealth, minDamage, maxDamage, accuracy;
        public final String name, details;
        public final boolean selected;
        public final int stamina, hunger;
        Enemy(int number, CombatEnemy enemy, CombatEnemy target) {
            this.number = number;
            name = enemy.getName();
            health = enemy.getHealth();
            stamina = enemy.getStamina();
            hunger = enemy.getHunger();
            maxHealth = enemy.getMaxHealth();
            boolean runner=enemy instanceof Zombie && ((Zombie)enemy).getType()==ZombieType.RUNNER;
            minDamage = runner?enemy.getMinDamage()*3/2:enemy.getMinDamage();
            maxDamage = runner?enemy.getMaxDamage()*3/2:enemy.getMaxDamage();
            accuracy = enemy.getAttackAccuracy();
            details = enemy.getCombatDetails();
            selected = enemy == target;
        }
    }

    GameStatus(SurvivorInfo player, List<? extends CombatEnemy> opponents, CombatEnemy target) {
        Fighter fighter = player instanceof Fighter ? (Fighter) player : null;
        injuries = fighter == null ? "" : fighter.injurySummary();
        combatInjuries = fighter == null ? "" : fighter.combatInjurySummary();
        Weapons equipped = fighter == null ? null : fighter.getEquippedWeapon();
        ItemInfo armorItem = fighter == null ? null : fighter.getEquippedArmor();
        weapon = fighter == null ? "Кулаки" : fighter.getWeaponName();
        weaponCondition = equipped == null ? 100 : equipped.getDurability();
        armor = fighter==null?"Нет брони":fighter.armorSummary();
        protection = fighter == null ? 0 : fighter.getArmorReduction();
        load = fighter == null ? "" : SurvivalSystems.load(fighter);
        ammo = equipped == null || equipped.getAmmoType() == null ? ""
                : equipped.getAmmoType().label + ": " + fighter.getAmmoCount(equipped.getAmmoType());
        List<Enemy> snapshot = new ArrayList<>();
        if (opponents != null) for (int i = 0; i < opponents.size(); i++)
            snapshot.add(new Enemy(i + 1, opponents.get(i), target));
        enemies = Collections.unmodifiableList(snapshot);
    }
}
