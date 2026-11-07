package com.example.survivor.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class HostileSurvivor extends CombatEnemy {
    private static final long serialVersionUID = 1L;
    private final boolean raider;
    private final Weapons weapon;
    private final List<ItemInfo> loot = new ArrayList<>();
    private final Ammunition ammunition;
    private final Weapons backup;
    private Tools armor;
    private Weapons scavenged;
    private boolean lootClaimed;
    HostileSurvivor(String name,Weapons weapon,int health) {
        super(name,"Раненый выживший",55);this.raider=false;this.weapon=weapon;
        ammunition=null;backup=null;armor=null;loot.add(weapon);
        if(health<55)takeDamage(55-health);
    }

    HostileSurvivor(int number, boolean raider, Random random) {
        super((raider ? "Рейдер " : "Бродяга ") + number,
                raider ? "Бронированный налётчик" : "Враждебный бродяга", raider ? 160 : 55);
        this.raider = raider;
        WeaponStyle style = raider ? (random.nextBoolean() ? WeaponStyle.SHOTGUN : WeaponStyle.REVOLVER)
                : (random.nextBoolean() ? WeaponStyle.CLUB : WeaponStyle.KNIFE);
        weapon = new Weapons(switch (style) {
            case SHOTGUN -> "Ружьё рейдера";
            case REVOLVER -> "Револьвер рейдера";
            case KNIFE -> "Нож бродяги";
            default -> "Дубинка бродяги";
        }, "Трофей с побеждённого противника", raider ? 5 : 1, raider ? 400 : 12, style);
        loot.add(weapon);
        ammunition = raider ? new Ammunition(weapon.getAmmoType()) : null;
        backup = raider ? new Weapons("Охотничий нож рейдера", "Запасное оружие налётчика", 3, 50, WeaponStyle.HUNTING_KNIFE) : null;
        armor = raider ? new Tools("Бронежилет рейдера", "Защита 40% при полной прочности; можно надеть и ремонтировать", 5, 300) : null;
        if (raider) {
            loot.add(ammunition);
            loot.add(new Ammunition(weapon.getAmmoType()));
            loot.add(new Ammunition(weapon.getAmmoType()));
            loot.add(backup);
            loot.add(armor);
            loot.add(new MedicalItem("Аптечка", 40, 50));
            loot.add(new MedicalItem("Аптечка", 40, 50));
            loot.add(new Food("Армейский сухпаёк", "Припасы рейдера", 3, 45, 70));
            loot.add(new Food("Тушёнка", "Припасы рейдера", 2, 8, 40));
        } else {
            loot.add(new Food("Галеты", "Последние припасы бродяги", 1, 3, 20));
            if (random.nextBoolean()) loot.add(new MedicalItem("Бинт", 15, 20));
        }
    }

    boolean isRaider() { return raider; }
    private Weapons mainWeapon() { return scavenged == null ? weapon : scavenged; }
    private Weapons activeWeapon() { return isRanged() || mainWeapon().getAmmoType()==null ? mainWeapon() : (raider ? backup : weapon); }
    private WeaponStyle attackStyle() { return activeWeapon().getDurability() == 0 ? WeaponStyle.FISTS : activeWeapon().getStyle(); }
    WeaponStyle soundStyle() {return attackStyle();}
    private int remainingAmmo() {
        int count = 0;
        for (ItemInfo item : loot) if (item instanceof Ammunition && ((Ammunition) item).getType() == mainWeapon().getAmmoType()) count += ((Ammunition) item).getQuantity();
        return count;
    }
    @Override boolean isRanged() { return mainWeapon().getAmmoType() != null && remainingAmmo() > 0 && mainWeapon().getDurability() > 0; }
    @Override int getAttackAccuracy() { return raider ? (isRanged() ? 95 : 90) : 65; }
    private int condition() { return activeWeapon().getDurability() == 0 ? 100 : activeWeapon().getConditionPercent(); }
    @Override int getMinDamage() { return Math.max(1, attackStyle().minDamage * attackStyle().strikes * condition() / 100); }
    @Override int getMaxDamage() { return Math.max(1, attackStyle().maxDamage * attackStyle().strikes * condition() / 100); }
    @Override void spendAttackAmmo() {
        Weapons active = activeWeapon();
        boolean ranged = isRanged();
        if (ranged) {
            for (ItemInfo item : loot) if (item instanceof Ammunition && ((Ammunition) item).getType() == mainWeapon().getAmmoType() && ((Ammunition) item).consume()) break;
        }
        active.wear(ranged ? 1 : 2);
    }
    @Override public void takeDamage(int damage) {
        if (damage <= 0) return;
        int reduction = armor == null ? 0 : armor.getArmorProtection() * armor.getDurability() / 100;
        super.takeDamage(Math.max(1, (damage * (100 - reduction) + 99) / 100));
        if (armor != null) armor.wear(Math.max(1, (damage + 4) / 5));
    }
    @Override int armorProtection() {return armor==null?0:armor.getArmorProtection()*armor.getDurability()/100;}
    @Override String getCombatDetails() {
        return (isRanged() ? mainWeapon().getName() + ", зарядов: " + remainingAmmo()
                : activeWeapon().getDurability() == 0 ? "Кулаки (оружие сломано)" : activeWeapon().getName())
                + "; оружие: " + activeWeapon().getDurability() + "/100"
                + (armor != null ? "; броня: " + armor.getDurability() + "/100, защита "
                + armor.getArmorProtection() * armor.getDurability() / 100 + "%" : "; без брони");
    }

    List<ItemInfo> claimLoot() {
        if (isAlive() || lootClaimed) return java.util.Arrays.asList();
        lootClaimed = true;
        List<ItemInfo> result = new ArrayList<>();
        for (ItemInfo item : loot)
            if (!(item instanceof Ammunition) || ((Ammunition) item).getQuantity() > 0) result.add(item);
        return result;
    }
    boolean takeGroundItem(ItemInfo item) {
        if (item instanceof Weapons && ((Weapons) item).canEquip() && item.getDurability() > 0) {
            Weapons candidate = (Weapons) item;
            if (candidate.getStyle().maxDamage <= mainWeapon().getStyle().maxDamage) return false;
            scavenged = candidate;
        } else if (item instanceof Ammunition) {
            if (((Ammunition) item).getType() != mainWeapon().getAmmoType()) return false;
        } else if (item instanceof Tools && item.getArmorProtection() > 0 && armor == null) armor=(Tools)item;
        else return false;
        loot.add(item);
        return true;
    }
}
