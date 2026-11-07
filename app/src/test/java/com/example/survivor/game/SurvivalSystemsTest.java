package com.example.survivor.game;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SurvivalSystemsTest {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static class Rolls extends Random {
        final int[] rolls;
        int index;
        Rolls(int... rolls) { this.rolls = rolls; }
        @Override public int nextInt(int bound) { return (index < rolls.length ? rolls[index++] : 99) % bound; }
        // API 24 adapter draws damage with nextInt(bound); scripts include that draw.
        @Override public boolean nextBoolean() { return true; }
    }
    static void durabilityAndRepair() {
        Fighter fighter = new Fighter("test", "", 100);
        Weapons pipe = fighter.getEquippedWeapon();
        Zombie target = new Zombie("target", "", 1000);
        Survivor.attack(fighter, target, new Rolls(0, 0, 99));
        check(pipe.getDurability() == 98, "Melee wear");
        pipe.wear(98);
        int stamina = fighter.getStamina();
        check(Survivor.attack(fighter, target, new Rolls()).brokenWeapon, "Broken weapon rejected");
        check(fighter.getStamina() == stamina, "Rejected attack is free");
        Tools parts = new Tools("Запчасти", "", 1, 12);
        fighter.addItem(parts);
        check(!SurvivalSystems.repair(fighter, pipe), "Repair needs a tool");
        check(fighter.getInventory().contains(parts), "Failed repair preserves material");
        Tools kit = new Tools("Набор инструментов", "", 2, 35);
        fighter.addItem(kit);
        check(SurvivalSystems.repair(fighter, pipe), "Broken weapon is repairable");
        check(pipe.getDurability() == 30 && !fighter.getInventory().contains(parts), "Repair consumes one part");
        check(fighter.getInventory().contains(kit), "Tool preserved");
        pipe.repair(100);
        fighter.addItem(parts);
        check(!SurvivalSystems.repair(fighter, pipe) && fighter.getInventory().contains(parts), "No waste at full condition");
        pipe.wear(60);
        int before = target.getHealth();
        Survivor.attack(fighter, target, new Rolls(0, 0, 99));
        check(before - target.getHealth() == 9, "Wear reduces actual damage");
        Tools armor = new Tools("Защитный жилет", "", 5, 300);
        fighter.addItem(armor);
        fighter.takeDamage(10);
        check(fighter.getHealth() == 90 && armor.getDurability() == 100, "Backpack armor offers no protection");
        fighter.equipArmor(armor);
        fighter.takeDamage(10);
        check(fighter.getHealth() == 84 && armor.getDurability() == 98, "Equipped armor absorbs and wears");
        armor.wear(100);
        fighter.takeDamage(10);
        check(fighter.getHealth() == 74 && fighter.getArmorReduction() == 0, "Broken armor stops protecting");
        check(SurvivalSystems.repair(fighter, armor) && armor.getDurability() == 30, "Armor can be repaired");
        Fighter gunner = new Fighter("g", "", 100);
        Weapons gun = new Weapons("gun", "", 5, 350, WeaponStyle.REVOLVER);
        gunner.addItem(gun); gunner.equipWeapon(gun);
        check(Survivor.attack(gunner, target, new Rolls()).outOfAmmo && gun.getDurability() == 100, "Empty gun does not wear");
        gunner.addItem(new Ammunition(AmmoType.REVOLVER));
        int weight = gunner.getCarryWeight();
        Survivor.attack(gunner, target, new Rolls(99));
        check(gun.getDurability() == 99 && gunner.getAmmoCount(AmmoType.REVOLVER) == 5, "Miss still consumes condition and ammo");
        check(gunner.getCarryWeight() == weight - 15, "Consumed ammunition reduces weight");
    }
    static void weightAndLoot() {
        Fighter fighter = new Fighter("test", "", 100);
        for (int i = 0; i < 13; i++) check(fighter.addItem(new Tools("Груз", "", 1, 1)), "Fits comfort weight");
        check(fighter.getCarryWeight() == 15000 && fighter.getOverloadPercent() == 0, "Comfort threshold");
        fighter.addItem(new Food("food", "", 1, 1));
        check(fighter.getOverloadPercent() == 25 && fighter.staminaCost(8) == 10, "Overload affects stamina");
        check(fighter.spendStamina(8) && fighter.getStamina() == 90, "Stamina charged once");
        fighter.getInventory().remove(fighter.getInventory().size() - 1);
        for (int i = 0; i < 10; i++) fighter.addItem(new Tools("Груз", "", 1, 1));
        check(fighter.getCarryWeight() == 25000, "Hard capacity");
        ItemInfo extra = new Food("extra", "", 1, 1);
        check(!fighter.addItem(extra) && fighter.getCarryWeight() == 25000, "Cannot exceed capacity");
        SurvivalSystems.collect(fighter, List.of(extra), new Scanner("1 -1 2 0 1"));
        check(fighter.getInventory().contains(extra) && fighter.getCarryWeight() == 24500, "Can discard and then collect");
        Weapons equipped = fighter.getEquippedWeapon();
        SurvivalSystems.backpack(fighter, new Scanner("1 0"));
        check(!fighter.getInventory().contains(equipped) && fighter.getEquippedWeapon() == null, "Discard clears equipment");
    }
    static void stealth() {
        Fighter awake = new Fighter("awake", "", 100);
        Fighter tired = new Fighter("tired", "", 100);
        tired.increaseSleepiness(100); tired.addNoise(60);
        check(SurvivalSystems.detectionChance(awake) > SurvivalSystems.detectionChance(tired), "Sleepiness and noise reduce detection");
        check(SurvivalSystems.bypassChance(awake) > SurvivalSystems.bypassChance(tired), "Sleepiness and noise reduce bypass");
        // Detection succeeds, ambush succeeds, opening hit lands; EOF exits the following battle.
        Zombie target = new Zombie("target", "", 60);
        Survivor.approachEnemies(awake, new Scanner("2 1"), 1, new Rolls(0, 0, 0, 0, 99), new ArrayList<>(List.of(target)));
        check(target.getHealth() == 47 && awake.getHealth() == 100, "Ambush provides a free opening attack");
        check(awake.getEquippedWeapon().getDurability() == 98 && awake.getStamina() == 85, "Ambush spends actual resources");
        Fighter surprised = new Fighter("surprised", "", 100);
        Survivor.approachEnemies(surprised, new Scanner(""), 1, new Rolls(99, 0, 0, 99),
                new ArrayList<>(List.of(new Zombie(1, ZombieType.NORMAL))));
        check(surprised.getHealth() == 93, "Failed detection grants enemies first attack");
        Fighter failed = new Fighter("failed", "", 100);
        Survivor.approachEnemies(failed, new Scanner("2 1"), 1, new Rolls(0, 99, 0, 0, 99),
                new ArrayList<>(List.of(new Zombie(1, ZombieType.NORMAL))));
        check(failed.getHealth() == 93 && failed.getStamina() == 85, "Failed ambush costs stamina and grants enemy initiative");
        Fighter bypass = new Fighter("bypass", "", 100);
        check(Survivor.approachEnemies(bypass, new Scanner("1"), 1, new Rolls(0, 0),
                new ArrayList<>(List.of(new Zombie(1, ZombieType.NORMAL)))), "Successful bypass clears route");
        check(bypass.getHealth() == 100 && bypass.getStamina() == 95, "Bypass avoids combat");
        Fighter caught = new Fighter("caught", "", 100);
        Survivor.approachEnemies(caught, new Scanner("1"), 1, new Rolls(0, 99, 0, 0, 99),
                new ArrayList<>(List.of(new Zombie(1, ZombieType.NORMAL))));
        check(caught.getHealth() == 93, "Failed bypass grants enemy initiative");
    }
    static void persistenceAndEnemies() throws Exception {
        Fighter fighter = new Fighter("saved", "", 100);
        Tools armor = new Tools("Бронежилет рейдера", "", 5, 300);
        fighter.addItem(armor); fighter.equipArmor(armor); armor.wear(37);
        fighter.getEquippedWeapon().wear(52);
        Path file = Path.of(".build/systems-save-test.dat");
        GameSave.save(file.toFile(), fighter, 31, new Locations());
        Fighter loaded = (Fighter) GameSave.load(file.toFile()).player;
        check(loaded.getEquippedWeapon().getDurability() == 48, "Weapon condition saved");
        check(loaded.getEquippedArmor().getDurability() == 63 && loaded.getArmorReduction() == 25, "Armor state saved");
        check(loaded.getCarryWeight() == fighter.getCarryWeight(), "Weight stable after loading");
        HostileSurvivor raider = new HostileSurvivor(1, true, new Rolls());
        raider.takeDamage(20);
        check(raider.getHealth() == 148, "Enemy armor protects");
        raider.spendAttackAmmo();
        check(raider.getCombatDetails().contains("99/100"), "Enemy weapon wears");
        raider.takeDamage(1000);
        List<ItemInfo> loot = raider.claimLoot();
        check(loot.stream().anyMatch(item -> item.getArmorProtection() > 0 && item.getDurability() == 0), "Enemy armor damage persists in loot");
        check(loot.stream().anyMatch(item -> item instanceof Weapons && item.getDurability() == 99), "Enemy weapon condition persists in loot");
    }
    @org.junit.Test public void originalRegression() throws Exception { java.nio.file.Files.createDirectories(java.nio.file.Path.of(".build")); main(new String[0]); }
    public static void main(String[] args) throws Exception {
        PrintStream original = System.out;
        try {
            System.setOut(new PrintStream(OutputStream.nullOutputStream()));
            durabilityAndRepair(); weightAndLoot(); stealth(); persistenceAndEnemies();
        } finally { System.setOut(original); }
        System.out.println("Survival systems tests passed");
    }
}
