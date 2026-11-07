package com.example.survivor.game;


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

class SurvivorInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String description;
    private int health;
    private final int maxHealth;
    private int stamina = 100;
    private final int maxStamina = 100;
    private int hunger = 0;
    private int sleepiness = 0;
    private int noise = 0;
    private int campaignDifficulty;
    int difficulty(){return campaignDifficulty==0?2:campaignDifficulty;}
    void chooseDifficulty(int level){if(level<1 || level>3 || campaignDifficulty!=0)throw new IllegalStateException("Сложность уже выбрана");campaignDifficulty=level;}

    public int getNoise() { return noise; }
    public void addNoise(int amount) { noise = Math.min(60, noise + Math.max(0, amount)); }
    public void fadeNoise() { noise = Math.max(0, noise - 20); }
    public void reduceNoise(int amount) { noise = Math.max(0, noise - Math.max(0, amount)); }
    void settleAtHome() { noise=0; }
    public int getSleepiness() { return sleepiness; }
    public boolean canSleep() { return isAlive() && sleepiness > 40; }
    public void increaseSleepiness(int amount) { sleepiness = Math.min(100, sleepiness + Difficulty.needs(amount,difficulty())); }
    public void sleep() {
        if (!canSleep()) return;
        sleepiness = 0;
        increaseHunger(10);
        restoreStamina(100);
    }

    public int getStamina() { return stamina; }
    public int getMaxStamina() { return maxStamina; }
    public int getHunger() { return hunger; }
    public void increaseHunger(int amount) { hunger = Math.min(100, hunger + Difficulty.needs(amount,difficulty())); }
    public void eat(int nutrition) { if (isAlive()) hunger = Math.max(0, hunger - Math.max(0, nutrition)); }
    public int getDamagePercent() { return Math.max(25, 100 - hunger / 2 - sleepiness / 4); }
    public int getAccuracyPenalty() { return hunger / 3 + sleepiness / 4; }
    public int getStunChance() { return Math.min(45, hunger / 4 + sleepiness / 5); }
    public boolean spendStamina(int amount) {
        if (amount < 0 || amount > stamina) return false;
        stamina -= amount;
        return true;
    }
    public void restoreStamina(int amount) {
        if (amount > 0 && isAlive()) stamina = Math.min(maxStamina, stamina + Math.max(1, amount * (100 - sleepiness / 2) / 100));
    }

    SurvivorInfo(String name, String description, int health) {
        this.name = name;
        this.description = description;
        this.health = health;
        this.maxHealth = health;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void takeDamage(int damage) {
        health = Math.max(0, health - damage);
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void heal(int amount) {
        if (health > 0) {
            health = Math.min(maxHealth, health + amount);
        }
    }

    public String getDescription() {
        return description;
    }

    public void showResult() {
        GameIO.out().println("Выживший: " + name + " (" + description + ")");
    }
}

/** Six deliberately broad zones: readable in the UI and meaningful in combat. */
enum BodyPart {
    HEAD("Голова"),
    TORSO("Тело"),
    LEFT_ARM("Левая рука"),
    RIGHT_ARM("Правая рука"),
    LEFT_LEG("Левая нога"),
    RIGHT_LEG("Правая нога");
    final String label;
    BodyPart(String label) { this.label = label; }
}

class BodyInjury implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    int trauma;
    int bleeding;
    boolean treated;
    int recoveryMinutes;
    BodyInjury(int trauma, int bleeding) { this.trauma = trauma; this.bleeding = bleeding; }
}

class Fighter extends SurvivorInfo {
    private static final long serialVersionUID = 1L;
    private final List<ItemInfo> inventory = new ArrayList<>();
    // Missing in old serialized saves: initialize lazily to preserve existing progress.
    private List<ItemInfo> storage = new ArrayList<>();
    private ShelterUpgrades upgrades;
    public ShelterUpgrades getUpgrades() {
        if (upgrades == null) upgrades = new ShelterUpgrades();
        return upgrades;
    }
    void inheritHomeState(Fighter previous) { upgrades = previous.getUpgrades(); flashlightOn = previous.isFlashlightOn();
        if(previous.revolverCases!=null)revolverCases=new java.util.HashMap<>(previous.revolverCases); }
    private boolean flashlightOn;
    transient int searchStealthModifier;
    public boolean hasFlashlight() { return inventory.stream().anyMatch(item -> item.getName().toLowerCase(java.util.Locale.ROOT).contains("фонар")); }
    public boolean isFlashlightOn() { return flashlightOn && hasFlashlight(); }
    public void toggleFlashlight() { flashlightOn = hasFlashlight() && !isFlashlightOn(); }
    public List<ItemInfo> getStorage() {
        if (storage == null) storage = new ArrayList<>();
        return storage;
    }
    private Weapons equippedWeapon;
    private ItemInfo equippedArmor;
    private ItemInfo equippedHelmet;
    private java.util.Map<Weapons,Integer> revolverCases;
    // Not final so saves made before the body system continue to load.
    private java.util.EnumMap<BodyPart, BodyInjury> injuries;
    private boolean bodySystemV2 = true;
    private int poisoningMinutes;
    int poisoningMinutes(){return poisoningMinutes;}
    void eatFood(Food food){
        eat(food.getNutrition());
        if(food instanceof WildFood && ((WildFood)food).poisonous()){
            poisoningMinutes=Math.max(poisoningMinutes,360);
            BodyInjury torso=injury(BodyPart.TORSO);torso.trauma=Math.min(100,torso.trauma+8);torso.treated=false;torso.recoveryMinutes=0;
            GameIO.out().println("После еды Джону становится плохо: отравление на 6 игровых часов, состояние корпуса ухудшилось. Действия требуют больше сил.");GameIO.hurt(this);
        }
    }
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (!bodySystemV2) {
            int previousHealth = super.getHealth();
            if (previousHealth > 0) {
                BodyInjury torso = injury(BodyPart.TORSO);
                torso.trauma = Math.min(99, Math.max(torso.trauma, getMaxHealth() - previousHealth));
                for (BodyInjury wound : injuries().values()) wound.trauma = Math.min(99, wound.trauma);
            } else injury(BodyPart.TORSO).trauma = 100;
            bodySystemV2 = true;
        }
    }
    @Override public int getHealth() { return Math.max(0, 100 - Math.max(trauma(BodyPart.HEAD), trauma(BodyPart.TORSO))); }
    @Override public boolean isAlive() { return getHealth() > 0; }
    @Override public void heal(int amount) {
        if (amount <= 0 || !isAlive()) return;
        BodyPart worst = null;
        for (BodyPart part : BodyPart.values()) if (worst == null || trauma(part) > trauma(worst)) worst = part;
        if (worst != null) injury(worst).trauma = Math.max(0, trauma(worst) - amount);
    }
    void recoverInjuries(int minutes) {
        if (minutes <= 0 || !isAlive()) return;
        poisoningMinutes=Math.max(0,poisoningMinutes-minutes);
        for (BodyInjury wound : injuries().values()) {
            if (!wound.treated || wound.bleeding > 0 || wound.trauma <= 0) continue;
            long total = (long) wound.recoveryMinutes + minutes;
            int restored = (int)Math.min(wound.trauma, total / 60);
            wound.trauma -= restored;
            wound.recoveryMinutes = wound.trauma == 0 ? 0 : (int)(total % 60);
        }
    }

    private java.util.EnumMap<BodyPart, BodyInjury> injuries() {
        if (injuries == null) injuries = new java.util.EnumMap<>(BodyPart.class);
        return injuries;
    }
    private BodyInjury injury(BodyPart part) {
        return injuries().computeIfAbsent(part, ignored -> new BodyInjury(0, 0));
    }
    int trauma(BodyPart part) { return injuries().containsKey(part) ? injuries().get(part).trauma : 0; }
    int bleed(BodyPart part) { return injuries().containsKey(part) ? injuries().get(part).bleeding : 0; }
    private int armsTrauma() { return Math.max(trauma(BodyPart.LEFT_ARM), trauma(BodyPart.RIGHT_ARM)); }
    private int legsTrauma() { return Math.max(trauma(BodyPart.LEFT_LEG), trauma(BodyPart.RIGHT_LEG)); }
    private BodyPart randomBodyPart() {
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < 8) return BodyPart.HEAD;
        if (roll < 42) return BodyPart.TORSO;
        if (roll < 56) return BodyPart.LEFT_ARM;
        if (roll < 70) return BodyPart.RIGHT_ARM;
        if (roll < 85) return BodyPart.LEFT_LEG;
        return BodyPart.RIGHT_LEG;
    }
    private void wound(BodyPart part, int received) {
        BodyInjury wound = injury(part);
        int severity = Math.max(2, received * 2);
        if (part == BodyPart.HEAD && getEquippedHelmet() != null) severity = Math.max(1, severity / 2);
        if (part == BodyPart.TORSO && getEquippedArmor() != null) severity = Math.max(1, severity * 2 / 3);
        wound.trauma = Math.min(100, wound.trauma + severity);
        wound.treated = false; wound.recoveryMinutes = 0;
        int bleedingChance = Math.min(75, 15 + received * 3);
        if (ThreadLocalRandom.current().nextInt(100) < bleedingChance)
            wound.bleeding = Math.min(5, wound.bleeding + (received >= 16 ? 2 : 1));
    }
    @Override public int getAccuracyPenalty() {
        return super.getAccuracyPenalty() + trauma(BodyPart.HEAD) / 12 + armsTrauma() / 14;
    }
    @Override public int getDamagePercent() {
        return Math.max(20, super.getDamagePercent() - trauma(BodyPart.TORSO) / 8 - armsTrauma() / 16);
    }
    @Override public int getStunChance() { return Math.min(70, super.getStunChance() + trauma(BodyPart.HEAD) / 8); }
    int heldCases(Weapons weapon) {
        if(revolverCases==null)return 0;revolverCases.keySet().removeIf(w->!inventory.contains(w));return revolverCases.getOrDefault(weapon,0);
    }
    void noteShot(Weapons weapon) {
        if(weapon==null || weapon.getStyle()!=WeaponStyle.REVOLVER)return;
        if(revolverCases==null)revolverCases=new java.util.HashMap<>();revolverCases.put(weapon,Math.min(6,heldCases(weapon)+1));
    }
    int releaseCases(Weapons weapon) {int count=heldCases(weapon);if(revolverCases!=null)revolverCases.remove(weapon);return count;}
    public static final int COMFORT_WEIGHT = 15000;
    public static final int MAX_WEIGHT = 25000;
    public int getCarryWeight() { return inventory.stream().mapToInt(ItemInfo::getWeightGrams).sum(); }
    public boolean canCarry(ItemInfo item) { return item != null && getCarryWeight() + item.getWeightGrams() <= MAX_WEIGHT; }
    public int getOverloadPercent() { return Math.max(0, (getCarryWeight() - COMFORT_WEIGHT + 1999) / 2000) * 25; }
    public int staminaCost(int base) {
        if (base <= 0) return 0;
        int fatigue = getOverloadPercent() + legsTrauma() / 2 + (poisoningMinutes>0?25:0);
        return (base * (100 + fatigue) + 99) / 100;
    }
    @Override public boolean spendStamina(int base) { return super.spendStamina(staminaCost(base)); }
    public void spendAvailableStamina(int base) { super.spendStamina(Math.min(getStamina(), staminaCost(base))); }
    public ItemInfo getEquippedArmor() {
        if (equippedArmor != null && !inventory.contains(equippedArmor)) equippedArmor = null;
        if (equippedHelmet != null && !inventory.contains(equippedHelmet)) equippedHelmet = null;
        // Old saves used a single armor field, including for helmets.
        if (isHelmet(equippedArmor)) {
            if(equippedHelmet==null)equippedHelmet=equippedArmor;
            equippedArmor=null;
        }
        return equippedArmor;
    }
    static boolean isHelmet(ItemInfo item) {
        if(item==null)return false;
        String name=item.getName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("шлем") || name.contains("каск");
    }
    ItemInfo getEquippedHelmet() {getEquippedArmor();return equippedHelmet;}
    boolean isArmorEquipped(ItemInfo item) {return item!=null && (item==getEquippedArmor() || item==getEquippedHelmet());}
    void removeBodyArmor() {getEquippedArmor();equippedArmor=null;}
    void removeHelmet() {getEquippedArmor();equippedHelmet=null;}
    String armorSummary() {
        ItemInfo body=getEquippedArmor(),head=getEquippedHelmet();
        return "Корпус: "+(body==null?"нет":body.getName()+" · "+body.getDurability()+"/100")
                +"\nГолова: "+(head==null?"нет":head.getName()+" · "+head.getDurability()+"/100");
    }
    public boolean equipArmor(ItemInfo armor) {
        if (armor != null && (!inventory.contains(armor) || armor.getArmorProtection() <= 0)) return false;
        getEquippedArmor();
        if(armor==null){equippedArmor=null;equippedHelmet=null;}
        else if(isHelmet(armor))equippedHelmet=armor;
        else equippedArmor = armor;
        return true;
    }
    public int getArmorReduction() {
        ItemInfo armor=getEquippedArmor(),head=getEquippedHelmet();
        int body=armor==null?0:armor.getArmorProtection()*armor.getDurability()/100;
        int helmet=head==null?0:head.getArmorProtection()*head.getDurability()/100;
        return 100-(100-body)*(100-helmet)/100;
    }
    @Override public void takeDamage(int damage) {
        if (damage <= 0) return;
        ItemInfo armor = getEquippedArmor();
        ItemInfo helmet = getEquippedHelmet();
        int reduction = getArmorReduction();
        int received = Difficulty.damage(Math.max(1, (damage * (100 - reduction) + 99) / 100),difficulty());
        if(received > 0) {
            wound(randomBodyPart(), received);
            GameIO.hurt(this);
        }
        if (armor != null && armor.getDurability() > 0) armor.wear(Math.max(1, (damage + 4) / 5));
        if (helmet != null && helmet.getDurability() > 0) helmet.wear(Math.max(1, (damage + 4) / 5));
    }

    int sufferBleeding() {
        int total = 0;
        for (BodyInjury value : injuries().values()) total += value.bleeding;
        if (total <= 0 || !isAlive()) return 0;
        int damage = Math.min(8, Math.max(1, (total + 1) / 2));
        BodyInjury torso = injury(BodyPart.TORSO);
        torso.trauma = Math.min(100, torso.trauma + damage);
        GameIO.hurt(this);
        return damage;
    }
    boolean hasInjuries() {
        for (BodyInjury value : injuries().values()) if (value.trauma > 0 || value.bleeding > 0) return true;
        return false;
    }
    List<BodyPart> injuredParts() {
        List<BodyPart> result = new ArrayList<>();
        for (BodyPart part : BodyPart.values()) if (trauma(part) > 0 || bleed(part) > 0) result.add(part);
        return result;
    }
    String bodyDiagramData() {
        StringBuilder result = new StringBuilder();
        for (BodyPart part : BodyPart.values()) if (trauma(part) > 0 || bleed(part) > 0) {
            if (result.length() > 0) result.append(';');
            result.append(part.name()).append(':').append(Math.max(trauma(part), bleed(part) * 20));
        }
        return result.toString();
    }
    String combatInjurySummary() {
        int count = 0, worst = 0;
        boolean bleeding = false;
        for (BodyPart part : BodyPart.values()) {
            int trauma = trauma(part);
            if (trauma > 0) { count++; worst = Math.max(worst, trauma); }
            if (bleed(part) > 0) bleeding = true;
        }
        String result;
        if (count == 0) result = bleeding ? "Кровотечение" : "Травм нет";
        else if (count > 1) result = worst >= 30 ? "Множественные травмы" : "Множественные ушибы";
        else result = worst >= 65 ? "Тяжёлая травма" : worst >= 30 ? "Ранение" : "Ушиб";
        if (bleeding && count > 0) result += " + кровотечение";
        if (poisoningMinutes > 0) result += " · отравление";
        return result;
    }
    String injurySummary() {
        if (!hasInjuries() && poisoningMinutes<=0) return "Ран нет. Пока что.";
        StringBuilder result = new StringBuilder();
        if(poisoningMinutes>0)result.append("Отравление: ещё ").append((poisoningMinutes+59)/60).append(" ч · действия требуют больше сил.\n");
        for (BodyPart part : injuredParts()) {
            int t = trauma(part), b = bleed(part);
            String condition = t >= 65 ? "тяжёлая травма" : t >= 30 ? "ранение" : "ушиб";
            result.append(part.label).append(": ").append(condition);
            if (b > 0) result.append(" · кровотечение ").append(b).append("/5");
            else if (t > 0) result.append(injury(part).treated ? " · заживает" : " · требуется обработка");
            result.append('\n');
        }
        if (trauma(BodyPart.HEAD) > 0) result.append("Голова: ниже точность и выше риск оглушения.\n");
        if (trauma(BodyPart.TORSO) > 0) result.append("Тело: ниже урон и переносимость нагрузки.\n");
        if (armsTrauma() > 0) result.append("Руки: ниже точность и сила удара.\n");
        if (legsTrauma() > 0) result.append("Ноги: движение и отход требуют больше сил.\n");
        return result.toString().trim();
    }
    String treat(BodyPart part, MedicalItem medicine) {
        BodyInjury wound = injury(part);
        boolean bandage = medicine.getName().toLowerCase(java.util.Locale.ROOT).contains("бинт");
        wound.bleeding = Math.max(0, wound.bleeding - (bandage ? 3 : 5));
        wound.treated = true;
        wound.recoveryMinutes = 0;
        return part.label + ": кровотечение " + (wound.bleeding == 0 ? "остановлено; рана заживает: 1 единица в игровой час." : "ослаблено; для заживления остановите кровь полностью.");
    }

    public Fighter(String name, String description, int health) {
        super(name, description, health);
        equippedWeapon = new Weapons("Металлическая труба", "Стартовое оружие бойца", 2, 15, WeaponStyle.PIPE);
        inventory.add(equippedWeapon);
    }

    public boolean addItem(ItemInfo item) {
        if (!canCarry(item)) return false;
        inventory.add(item);
        return true;
    }

    public List<ItemInfo> getInventory() {
        return inventory;
    }
    public int getAmmoCount(AmmoType type) {
        int count = 0;
        for (ItemInfo item : inventory) {
            if (item instanceof Ammunition && ((Ammunition) item).getType() == type)
                count += ((Ammunition) item).getQuantity();
        }
        return count;
    }
    public boolean consumeAmmo(AmmoType type) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemInfo item = inventory.get(i);
            if (item instanceof Ammunition) {
                Ammunition ammo = (Ammunition) item;
                if (ammo.getType() == type && ammo.consume()) {
                    if (ammo.getQuantity() == 0) inventory.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    public Weapons getEquippedWeapon() {
        if (equippedWeapon != null && !inventory.contains(equippedWeapon)) equippedWeapon = null;
        return equippedWeapon;
    }
    public boolean equipWeapon(Weapons weapon) {
        if (weapon != null && (!inventory.contains(weapon) || !weapon.canEquip())) return false;
        equippedWeapon = weapon;
        return true;
    }
    public WeaponStyle getWeaponStyle() {
        Weapons weapon = getEquippedWeapon();
        return weapon == null || weapon.getDurability() == 0 ? WeaponStyle.FISTS : weapon.getStyle();
    }
    public String getWeaponName() {
        Weapons weapon = getEquippedWeapon();
        return weapon == null ? "Кулаки" : weapon.getName();
    }

    @Override
    public void showResult() {

    }
}

class Medic extends SurvivorInfo {
    private static final long serialVersionUID = 1L;
    public Medic(String name, String description, int health) {
        super(name, description, health);
    }

    @Override
    public void showResult() {
        GameIO.out().println("Медик ищет лекарства для группы!");
    }
}

class Engineer extends SurvivorInfo {
    private static final long serialVersionUID = 1L;
    public Engineer(String name, String description, int health) {
        super(name, description, health);
    }

    @Override
    public void showResult() {
        GameIO.out().println("Инженер чинит оборудование!");
    }
}

enum ZombieType {
    NORMAL("Обычный", 60, 70, 7, 12),
    RUNNER("Бегун", 35, 90, 5, 9),
    BRUTE("Громила", 110, 50, 14, 22),
    SCREAMER("Крикун", 45, 60, 4, 8),
    JUMPER("Прыгун", 165, 90, 22, 36),
    HUNTER("Ночной охотник", 185, 90, 26, 38);
    final String label;
    final int health, accuracy, minDamage, maxDamage;
    ZombieType(String label, int health, int accuracy, int minDamage, int maxDamage) {
        this.label = label;
        this.health = health;
        this.accuracy = accuracy;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
    }
}

abstract class CombatEnemy extends SurvivorInfo {
    private static final long serialVersionUID = 1L;
    CombatEnemy(String name, String description, int health) { super(name, description, health); }
    abstract int getAttackAccuracy();
    abstract int getMinDamage();
    abstract int getMaxDamage();
    boolean isRanged() { return false; }
    int armorProtection() {return 0;}
    void takeBleedingDamage(int amount) {super.takeDamage(amount);}
    void spendAttackAmmo() { }
    String getCombatDetails() { return getDescription(); }
}

class Zombie extends CombatEnemy {
    private boolean earsClaimed;
    List<ItemInfo> claimEars(java.util.Random random) {
        if (isAlive() || earsClaimed) return java.util.Collections.emptyList();
        earsClaimed=true;
        return ZombieEar.roll(type, random);
    }
    @Override public void takeDamage(int amount){boolean living=isAlive();super.takeDamage(amount);if(living && !isAlive())GameIO.sound(GameSound.ZOMBIE_DEATH);}

    private static final long serialVersionUID = 1L;
    private final ZombieType type;
    public Zombie(String name, String description, int health) {
        super(name, description, health);
        type = ZombieType.NORMAL;
    }
    public Zombie(int number, ZombieType type) {
        super("Зомби " + number + " (" + type.label + ")", type.label, type.health);
        this.type = type;
    }
    public ZombieType getType() { return type; }
    @Override int getAttackAccuracy() { return type.accuracy; }
    @Override int getMinDamage() { return type.minDamage; }
    @Override int getMaxDamage() { return type.maxDamage; }
    @Override String getCombatDetails() { return type == ZombieType.JUMPER
            ? "Прыжком преодолевает дистанцию: отталкивание оружием не защищает от его ответа."
            : type == ZombieType.HUNTER ? "Ночной преследователь: 2 клетки за ход, высокая живучесть и сильный удар. Не прыгает."
            : super.getCombatDetails(); }

    @Override
    public void showResult() {
        GameIO.out().println("Зомби напали на выжившего!");
    }
}

class MedicalItem extends ItemInfo {
    private static final long serialVersionUID = 1L;
    private final int healing;

    public MedicalItem(String name, int price, int healing) {
        super(name, "Восстанавливает " + healing + " здоровья", 1, price);
        this.healing = healing;
    }

    @Override public String getDescription() {
        return getName().toLowerCase(java.util.Locale.ROOT).contains("бинт")
                ? "Ослабляет кровотечение на 3 и обрабатывает одну часть тела."
                : "Останавливает кровотечение и обрабатывает одну часть тела.";
    }
    public int getHealing() {
        return healing;
    }
    @Override public int getWeightGrams() { return healing <= 20 ? 100 : 700; }
}

class AttackResult {
    boolean outOfAmmo;
    boolean brokenWeapon;
    final boolean exhausted;
    final int hits;
    final int damage;
    final boolean stunned;
    final boolean keptDistance;

    AttackResult(boolean exhausted, int hits, int damage, boolean stunned, boolean keptDistance) {
        this.exhausted = exhausted;
        this.hits = hits;
        this.damage = damage;
        this.stunned = stunned;
        this.keptDistance = keptDistance;
    }
}

class Survivor {
    public static void travel(SurvivorInfo player,String message,int steps){travel(player,message,steps,GameIO.footsteps());}
    static void travel(SurvivorInfo player, String message, int steps,GameSound surface) {
        if(message.startsWith("Путь к локации:"))GameIO.travelTransition("Ты идёшь");
        else if(message.startsWith("Возвращение"))GameIO.travelTransition("Ты возвращаешься домой");
        if (player instanceof Fighter) ((Fighter) player).spendAvailableStamina(steps);
        else player.spendStamina(Math.min(steps, player.getStamina()));
        GameIO.sound(surface);GameIO.out().println(message+"…");
        GameIO.advanceMinutes(steps * 5);
    }
    public static void showMovement(String message, int steps) {
        GameIO.sound(GameIO.footsteps());
        GameIO.out().println(message + "…" );
    }
    public static void useFood(SurvivorInfo player, Scanner scanner) {
        if (!(player instanceof Fighter) || !player.isAlive()) {
            GameIO.out().println("Еда доступна живому бойцу с инвентарём.");
            return;
        }
        if (player.getHunger() == 0) {
            GameIO.out().println("Вы сыты. Приберегите припасы.");
            return;
        }
        Fighter fighter = (Fighter) player;
        List<Food> food = new ArrayList<>();
        for (ItemInfo item : fighter.getInventory()) if (item instanceof Food) food.add((Food) item);
        if (food.isEmpty()) {
            GameIO.out().println("Еды нет. Ищите припасы на вылазке или купите в магазине.");
            return;
        }
        GameIO.menu("Поесть", "Выберите еду из рюкзака.");
        for (int i = 0; i < food.size(); i++) GameIO.item(i + 1, food.get(i), "", true);
        GameIO.action(0, "Отмена");
        if (!scanner.hasNextInt()) { if (scanner.hasNext()) scanner.next(); return; }
        int choice = scanner.nextInt();
        if (choice == 0) return;
        if (choice < 1 || choice > food.size()) { GameIO.out().println("Неверный номер."); return; }
        Food meal = food.get(choice - 1);
        int before = player.getHunger();
        fighter.eatFood(meal);
        fighter.getInventory().remove(meal);
        GameIO.out().println("Съедено: " + meal.getName() + ". Голод: " + before + " → " + player.getHunger() + "/100.");
    }

    public static void showGuide() { SurvivorGuide.show(); }

    public static AttackResult attack(SurvivorInfo player, SurvivorInfo target, java.util.Random random) {
        return attack(player, target, random, 0, 100);
    }
    static AttackResult attack(SurvivorInfo player, SurvivorInfo target, java.util.Random random, int accuracyPenalty, int damagePercent) {
        if (!player.isAlive() || !target.isAlive()) return new AttackResult(false, 0, 0, false, false);
        WeaponStyle style = player instanceof Fighter ? ((Fighter) player).getWeaponStyle() : WeaponStyle.FISTS;
        Fighter fighter = player instanceof Fighter ? (Fighter) player : null;
        Weapons weapon = fighter == null ? null : fighter.getEquippedWeapon();
        AmmoType ammoType = weapon == null ? null : weapon.getAmmoType();
        if (weapon != null && weapon.getDurability() == 0) {
            AttackResult result = new AttackResult(false, 0, 0, false, false);
            result.brokenWeapon = true;
            return result;
        }
        if (ammoType != null && fighter.getAmmoCount(ammoType) == 0) {
            AttackResult result = new AttackResult(false, 0, 0, false, false);
            result.outOfAmmo = true;
            return result;
        }
        if (!player.spendStamina(style.staminaCost)) return new AttackResult(true, 0, 0, false, false);
        if (ammoType != null) fighter.consumeAmmo(ammoType);
        if(fighter!=null)fighter.noteShot(weapon);
        GameIO.sound(GameSound.weapon(style));
        player.addNoise(style.noise());
        int hits = 0;
        int damage = 0;
        for (int i = 0; i < style.strikes && target.isAlive(); i++) {
            if (random.nextInt(100) < Math.min(95,Math.max(5, style.accuracy - player.getAccuracyPenalty() - accuracyPenalty))) {
                int hit = style.minDamage + random.nextInt(style.maxDamage - style.minDamage + 1);
                hit = Math.max(1, hit * player.getDamagePercent() / 100);
                hit = Math.max(1, hit * damagePercent / 100);
                if (weapon != null) hit = Math.max(1, hit * weapon.getConditionPercent() / 100);
                int before = target.getHealth();
                target.takeDamage(hit);
                damage += before - target.getHealth();
                hits++;
            }
        }
        boolean stunned = hits > 0 && target.isAlive() && style.stunChance > 0
                && random.nextInt(100) < style.stunChance;
        boolean distance = hits > 0 && target.isAlive() && (!(target instanceof Zombie) || ((Zombie) target).getType() != ZombieType.JUMPER)
                && (!(target instanceof CombatEnemy) || !((CombatEnemy) target).isRanged()) && style.distanceChance > 0
                && random.nextInt(100) < style.distanceChance;
        if (weapon != null) weapon.wear(ammoType == null ? 2 : 1);
        return new AttackResult(false, hits, damage, stunned, distance);
    }

    public static void showHealth(SurvivorInfo survivor) {
        if (GameIO.player(survivor)) return;
        if (survivor instanceof Fighter) GameIO.out().println(((Fighter)survivor).injurySummary());
        else GameIO.out().println("Здоровье: " + survivor.getHealth() + "/" + survivor.getMaxHealth());
        GameIO.out().println("Выносливость: " + survivor.getStamina() + "/" + survivor.getMaxStamina()
                + " | Голод: " + survivor.getHunger() + "/100");
        GameIO.out().println("Шум: " + survivor.getNoise() + "/60");
        if (survivor instanceof Fighter) {
            Fighter fighter = (Fighter) survivor;
            GameIO.out().println(SurvivalSystems.load(fighter));
            GameIO.out().println("Броня: " + fighter.armorSummary()
                    + "; защита " + fighter.getArmorReduction() + "%");
        }
        GameIO.out().println("Сонливость: " + survivor.getSleepiness() + "/100");
        if (survivor instanceof Fighter) GameIO.out().println("Оружие: " + ((Fighter) survivor).getWeaponName());
        if (survivor instanceof Fighter) {
            Fighter fighter = (Fighter) survivor;
            Weapons weapon = fighter.getEquippedWeapon();
            if (weapon != null && weapon.getAmmoType() != null)
                GameIO.out().println("Боеприпасы: " + weapon.getAmmoType().label + " — " + fighter.getAmmoCount(weapon.getAmmoType()));
        }
    }

    public static boolean chooseEquipment(SurvivorInfo survivor, Scanner scanner) {
        if (!(survivor instanceof Fighter)) {
            GameIO.out().println("Экипировка доступна бойцам.");
            return false;
        }
        Fighter fighter = (Fighter) survivor;
        List<Weapons> weapons = new ArrayList<>();
        showHealth(fighter);
        for (ItemInfo item : fighter.getInventory()) {
            if (item instanceof Weapons && ((Weapons) item).canEquip()) weapons.add((Weapons) item);
        }
        GameIO.menu("Экипировка", "Выберите оружие. В бою смена оружия занимает ход.");
        for (int i = 0; i < weapons.size(); i++) {
            boolean equipped=weapons.get(i)==fighter.getEquippedWeapon();
            GameIO.item(i+1,weapons.get(i),equipped?"Уже экипировано: ":"",!equipped,equipped);
        }
        if(fighter.getEquippedWeapon()==null)GameIO.selectedAction(-1,"Кулаки","Уже экипировано",false);
        else GameIO.action(-1, "Кулаки", "Убрать оружие");
        GameIO.action(0, "Отмена");
        int choice = scanner.nextInt();
        if (choice == 0) return false;
        if (choice < -1 || choice > weapons.size()) {
            GameIO.out().println("Неверный номер.");
            return false;
        }
        Weapons next = choice == -1 ? null : weapons.get(choice - 1);
        if (next == fighter.getEquippedWeapon()) return false;
        fighter.equipWeapon(next);
        GameIO.out().println("Экипировано: " + fighter.getWeaponName());
        return true;
    }

    public static void rest(SurvivorInfo survivor, int amount) {
        GameIO.advanceMinutes(20);
        int before = survivor.getStamina();
        survivor.increaseHunger(1);
        survivor.restoreStamina(amount);
        GameIO.out().println("Передышка: +" + (survivor.getStamina() - before) + " выносливости.");
    }

    public static void useMedicine(SurvivorInfo survivor, Scanner scanner) {
        showHealth(survivor);
        if (!(survivor instanceof Fighter)) {
            GameIO.out().println("У этого персонажа нет инвентаря.");
            return;
        }
        if (survivor.getHealth() == 0 || survivor.getHealth() == survivor.getMaxHealth()) {
            GameIO.out().println("Сейчас лечение невозможно или не требуется.");
            return;
        }
        List<ItemInfo> inventory = ((Fighter) survivor).getInventory();
        List<MedicalItem> medicines = new ArrayList<>();
        for (ItemInfo item : inventory) {
            if (item instanceof MedicalItem) {
                medicines.add((MedicalItem) item);
            }
        }
        if (medicines.isEmpty()) {
            GameIO.out().println("Нет бинтов или аптечек. Их можно купить в магазине.");
            return;
        }
        GameIO.menu("Лечение", "Выберите лекарство из рюкзака.");
        for (int i = 0; i < medicines.size(); i++) GameIO.item(i + 1, medicines.get(i), "", true);
        GameIO.action(0, "Отмена");
        int choice = scanner.nextInt();
        if (choice == 0) return;
        if (choice < 1 || choice > medicines.size()) {
            GameIO.out().println("Неверный номер.");
            return;
        }
        MedicalItem medicine = medicines.get(choice - 1);
        int previousHealth = survivor.getHealth();
        survivor.heal(medicine.getHealing());
        inventory.remove(medicine);
        GameIO.out().println("Использован предмет: " + medicine.getName()
                + ". Восстановлено: " + (survivor.getHealth() - previousHealth));
        showHealth(survivor);
    }

    /** A treatment screen always starts with the body diagram; medicine is applied to a concrete wound. */
    public static boolean inspectBody(Fighter fighter, Scanner scanner) {
        GameIO.menu("Осмотреть тело", "Раны влияют на бой, а кровотечение отнимает здоровье после хода врагов.");
        GameIO.illustratedCard("Состояние Джона", fighter.injurySummary(), "body:" + fighter.bodyDiagramData());
        List<MedicalItem> medicines = new ArrayList<>();
        for (ItemInfo item : fighter.getInventory()) if (item instanceof MedicalItem) medicines.add((MedicalItem) item);
        if (medicines.isEmpty()) {
            GameIO.action(0, "Назад", "Нет бинтов или аптечек");
            scanner.nextInt();
            return false;
        }
        if (!fighter.hasInjuries()) {
            GameIO.action(0, "Назад", "Ран, требующих обработки, нет");
            scanner.nextInt();
            return false;
        }
        for (int i = 0; i < medicines.size(); i++)
            GameIO.item(i + 1, medicines.get(i), "Обработать рану: ", true);
        GameIO.action(0, "Назад");
        if (!scanner.hasNextInt()) { if (scanner.hasNext()) scanner.next(); return false; }
        int medicineChoice = scanner.nextInt();
        if (medicineChoice < 1 || medicineChoice > medicines.size()) return false;
        List<BodyPart> parts = fighter.injuredParts();
        GameIO.menu("Куда наложить средство?", "Выберите повреждённую часть тела.");
        for (int i = 0; i < parts.size(); i++) {
            BodyPart part = parts.get(i);
            GameIO.action(i + 1, part.label, "Травма " + fighter.trauma(part) + "/100"
                    + (fighter.bleed(part) > 0 ? " · кровотечение " + fighter.bleed(part) + "/5" : ""));
        }
        GameIO.action(0, "Назад");
        if (!scanner.hasNextInt()) { if (scanner.hasNext()) scanner.next(); return false; }
        int partChoice = scanner.nextInt();
        if (partChoice < 1 || partChoice > parts.size()) return false;
        MedicalItem medicine = medicines.get(medicineChoice - 1);
        String result = fighter.treat(parts.get(partChoice - 1), medicine);
        fighter.getInventory().remove(medicine);
        GameIO.out().println("Использован предмет: " + medicine.getName() + ". " + result);
        return true;
    }

    public static boolean continueExploring(SurvivorInfo survivor, Scanner scanner) {
        return continueExploring(survivor, scanner, null);
    }
    public static boolean continueExploring(SurvivorInfo survivor, Scanner scanner, WorldStories stories) {
        while (true) {
            showHealth(survivor);
            GameIO.clearBattle();
            GameIO.menu("На вылазке", "Продолжить поиск или подготовиться к возвращению?");
            GameIO.action(1, "Идти дальше", "Продолжить поиск в этой локации");
            GameIO.action(2, "Осмотреть тело", "Травмы, кровотечение и лечение");
            GameIO.action(3, "Отдохнуть", "Восстановить до 30 выносливости");
            GameIO.action(4, "Экипировка");
            GameIO.action(5, "Поесть");
            GameIO.action(6, "Разгрузить рюкзак");
            GameIO.action(7, "Ремонт");
            GameIO.action(8, "Броня");
            if(stories!=null && stories.caseCount(stories.currentPlace())>0)GameIO.action(11,"Осмотреть место стрельбы","Собрать гильзы после боя");
            if (stories != null) GameIO.action(9, "Помочь выжившим", "Оставить припасы или вещь для других");
            if(stories!=null && !stories.residential().points(stories.currentPlace(),stories).isEmpty())
                GameIO.action(10,"Точки интереса","Вернуться к известным историям этой локации");
            GameIO.action(0, "Вернуться домой");
            int choice = scanner.nextInt();
            if (choice == 1) {GameIO.sound(GameIO.footsteps());return true;}
            if (choice == 0) return false;
            if (choice == 2 && survivor instanceof Fighter) inspectBody((Fighter) survivor, scanner);
            else if (choice == 3) rest(survivor, 30);
            else if (choice == 4) chooseEquipment(survivor, scanner);
            else if (choice == 5) useFood(survivor, scanner);
            else if (choice == 6 && survivor instanceof Fighter) SurvivalSystems.backpack((Fighter) survivor, scanner);
            else if (choice == 7 && survivor instanceof Fighter) SurvivalSystems.repairMenu((Fighter) survivor, scanner);
            else if (choice == 8 && survivor instanceof Fighter) SurvivalSystems.armorMenu((Fighter) survivor, scanner);
            else if(choice==11 && stories!=null && survivor instanceof Fighter) {
                if(!Crafting.searchCases((Fighter)survivor,stories,scanner,ThreadLocalRandom.current()))return false;
            }
            else if (choice == 9 && stories != null && survivor instanceof Fighter) stories.leaveSupplies((Fighter) survivor, scanner);
            else if(choice==10 && stories!=null && survivor instanceof Fighter) {
                stories.residential().pointsMenu((Fighter)survivor,stories.currentPlace(),stories,scanner,ThreadLocalRandom.current());
                if(stories.residential().aborted)return false;
            }
            else GameIO.out().println("Неверный ввод.");
        }
    }
    // true: путь свободен или зомби побеждён; false: игрок отступил или погиб.
    private static void showBattle(SurvivorInfo player, List<? extends CombatEnemy> zombies, CombatEnemy target,
                                   List<String> messages) {
        if (GameIO.battle(player, zombies, target)) {
            for (String message : messages) GameIO.out().println(message);
            messages.clear();
            return;
        }
        List<String> left = new ArrayList<>();
        List<String> right = new ArrayList<>();
        left.add("ПРОТИВНИКИ");
        for (CombatEnemy enemy : zombies) {
            left.add((zombies.indexOf(enemy) + 1) + " - " + enemy.getName() + ": " + enemy.getHealth() + "/" + enemy.getMaxHealth() + " HP"
                    + (enemy.isAlive() ? "" : " [уничтожен]"));
        }
        if (target != null) {
            left.add("Цель: " + target.getName());
            left.add("Урон: " + target.getMinDamage() + "–" + target.getMaxDamage() + "; точность: " + target.getAttackAccuracy() + "%");
            left.add(target.getCombatDetails());
        }
        right.add("ВЫЖИВШИЙ: " + player.getName());
        if (player instanceof Fighter) {
            Fighter fighter = (Fighter) player;
            right.add(SurvivalSystems.load(fighter));
            ItemInfo armor = fighter.getEquippedArmor();
            right.add("Броня: " + (armor == null ? "нет" : armor.getName() + " " + armor.getDurability() + "/100")
                    + "; защита " + fighter.getArmorReduction() + "%");
        }
        right.add(player instanceof Fighter ? ((Fighter)player).injurySummary() : "Здоровье: " + player.getHealth() + "/" + player.getMaxHealth());
        right.add("Выносливость: " + player.getStamina() + "/" + player.getMaxStamina());
        right.add("Шум: " + player.getNoise() + "/60");
        right.add("Сонливость: " + player.getSleepiness() + "/100");
        right.add("Голод: " + player.getHunger() + "/100");
        right.add("Урон: " + player.getDamagePercent() + "%; точность: -" + player.getAccuracyPenalty()
                + " п.п.; риск оглушения: " + player.getStunChance() + "%");
        Fighter fighter = player instanceof Fighter ? (Fighter) player : null;
        right.add("Оружие: " + (fighter == null ? "Кулаки" : fighter.getWeaponName()));
        if (fighter != null && fighter.getEquippedWeapon() != null)
            right.add("Прочность оружия: " + fighter.getEquippedWeapon().getDurability() + "/100");
        Weapons weapon = fighter == null ? null : fighter.getEquippedWeapon();
        if (weapon != null && weapon.getAmmoType() != null)
            right.add("Боеприпасы: " + weapon.getAmmoType().label + " — " + fighter.getAmmoCount(weapon.getAmmoType()));
        int width = 36;
        for (String line : left) width = Math.max(width, line.length());
        GameIO.out().println("\n" + GameIO.repeat("=", width) + "   " + GameIO.repeat("=", 48));
        for (int i = 0; i < Math.max(left.size(), right.size()); i++) {
            String a = i < left.size() ? left.get(i) : "";
            String b = i < right.size() ? right.get(i) : "";
            GameIO.out().printf("%-" + width + "s   %s%n", a, b);
        }
        GameIO.out().println();
        for (String message : messages) GameIO.out().println(message);
        messages.clear();
    }

    public static int rollZombieCount(int difficulty, java.util.Random random) {
        int roll = random.nextInt(100);
        if (difficulty <= 1) return roll < 3 ? 3 : roll < 13 ? 2 : 1;
        int max = difficulty == 2 ? 6 : 10;
        return roll < 45 ? 1 : random.nextInt(max - 1) + 2;
    }
    public static Zombie createZombie(int number, java.util.Random random) {
        return createZombie(number, random, GameIO.clock() != null && GameIO.clock().isNight());
    }
    static Zombie createZombie(int number, java.util.Random random, boolean night) {
        int roll = random.nextInt(100);
        ZombieType type = night && roll < 20 ? ZombieType.JUMPER : night && roll < 35 ? ZombieType.HUNTER : roll < 55 ? ZombieType.NORMAL : roll < 75 ? ZombieType.RUNNER
                : roll < 90 ? ZombieType.BRUTE : ZombieType.SCREAMER;
        return new Zombie(number, type);
    }

    public static boolean addReinforcement(List<? super Zombie> zombies, int max, java.util.Random random) {
        if (zombies.size() >= max) return false;
        zombies.add(createZombie(zombies.size() + 1, random));
        return true;
    }

    public static boolean zombie_random(SurvivorInfo player, Scanner scanner) {
        return zombie_random(player, scanner, 1);
    }

    public static boolean zombie_random(SurvivorInfo player, Scanner scanner, int difficulty) {
        return zombie_random(player, scanner, difficulty, ThreadLocalRandom.current());
    }

    public static boolean zombie_random(SurvivorInfo player, Scanner scanner, int difficulty, java.util.Random random) {
        return zombieEncounter(player, scanner, difficulty, random, false);
    }
    private static boolean zombieEncounter(SurvivorInfo player, Scanner scanner, int difficulty, java.util.Random random, boolean stealth) {
        if (!player.isAlive()) return false;
        player.reduceNoise(60);
        int quietChance = Math.max(0, 21 - player.getNoise());
        player.fadeNoise();
        if (random.nextInt(100) < quietChance) {
            GameIO.out().println("Зомби не встретился.");
            showHealth(player);
            return true;
        }
        int zombieCount = rollZombieCount(difficulty, random);
        List<Zombie> zombies = new ArrayList<>();
        for (int i = 1; i <= zombieCount; i++) {
            zombies.add(createZombie(i, random));
        }
        List<CombatEnemy> enemies = new ArrayList<>(zombies);
        return stealth ? approachEnemies(player, scanner, difficulty, random, enemies)
                : fightEnemies(player, scanner, difficulty, random, enemies);
    }

    public static boolean encounterRandom(SurvivorInfo player, Scanner scanner, int difficulty, java.util.Random random) {
        return encounterRandom(player, scanner, difficulty, random, null);
    }
    public static boolean encounterRandom(SurvivorInfo player, Scanner scanner, int difficulty, java.util.Random random, WorldStories stories) {
        if (!player.isAlive()) return false;
        player.reduceNoise(60);
        if (random.nextInt(100) >= 18) return zombieEncounter(player, scanner, difficulty, random, true);
        player.fadeNoise();
        boolean raiders = random.nextInt(100) < (difficulty <= 1 ? 15 : difficulty == 2 ? 30 : 45);
        int count = raiders ? (difficulty >= 2 ? random.nextInt(2) + 1 : 1) : random.nextInt(3) + 1;
        List<CombatEnemy> enemies = new ArrayList<>();
        for (int i = 1; i <= count; i++) enemies.add(new HostileSurvivor(i, raiders, random));
        if (stories != null) stories.observePeople(random);
        return approachEnemies(player, scanner, difficulty, random, enemies);
    }

    static void searchLocation(Fighter player, Location location, Locations world, Scanner scanner, java.util.Random random) {
        searchLocation(player, location, world, scanner, random, SearchMode.CAREFUL);
    }
    static void searchLocation(Fighter player, Location location, Locations world, Scanner scanner, java.util.Random random, SearchMode mode) {
        searchLocation(player, location, world, scanner, random, mode, null);
    }
    static void searchLocation(Fighter player, Location location, Locations world, Scanner scanner, java.util.Random random,
                               SearchMode mode, Location.SearchPoint point) {
        if(world.getNumber(location)==16){PoliceArchive.enter(player,world.getStories(),scanner,random);return;}
        if (location.isDepleted()) return;
        world.getStories().markSearched(world.getNumber(location));
        world.getStories().findArchiveKey(player,world.getNumber(location));
        List<ItemInfo> chips=world.getStories().guaranteedChips(world.getNumber(location));
        if(!chips.isEmpty()) {
            if (point == null) location.searchSupplies(random); else location.consumePoint(point);
            GameIO.out().println("Найдено служебное оборудование. Заберите его в рюкзак; оставленное можно будет найти здесь снова.");
            world.getStories().collectEquipment(world.getNumber(location),player,chips,scanner);return;
        }
        if (point != null && (world.getNumber(location) == 1 && point.name.equals("Мусорные контейнеры")
)) {
            ItemInfo guaranteed = location.searchPoint(point, random, mode.lootBonus);
            if (guaranteed != null) SurvivalSystems.collect(player, java.util.Arrays.asList(guaranteed), scanner);
            return;
        }
        int event=world.getStories().residential().discover(world.getNumber(location),mode,world.getStories(),random);
        if(event!=0) {
            if (point == null) location.searchSupplies(random); else location.consumePoint(point);
            world.getStories().residential().run(event,player,world.getNumber(location),world.getStories(),scanner,random);
            return;
        }
        WorldStories.Discovery discovery = world.getStories().discover(world.getNumber(location), location.getName(), random, true, mode.storyBonus);
        if (discovery == null) {
            ItemInfo found = point == null ? Item.createItems(location, random, mode.lootBonus)
                    : location.searchPoint(point, random, mode.lootBonus + Difficulty.loot(GameIO.difficulty()));
            if (found != null) SurvivalSystems.collect(player, java.util.Arrays.asList(found), scanner);
        } else {
            // A narrative discovery occupies one search site too; it is not a free extra loot roll.
            if (point == null) location.searchSupplies(random); else location.consumePoint(point);
            WorldStories.print(discovery);
            if (!discovery.items().isEmpty()) SurvivalSystems.collect(player, discovery.items(), scanner);
            GameIO.out().println("Запасы: " + location.getSuppliesStatus());
        }
    }

    static boolean approachEnemies(SurvivorInfo player, Scanner scanner, int difficulty, java.util.Random random,
                                   List<CombatEnemy> enemies) {
        try { return approachEnemiesInternal(player, scanner, difficulty, random, enemies); }
        finally { player.reduceNoise(60); GameIO.clearBattle(); }
    }

    private static boolean approachEnemiesInternal(SurvivorInfo player, Scanner scanner, int difficulty, java.util.Random random,
                                   List<CombatEnemy> enemies) {
        if (random.nextInt(100) >= SurvivalSystems.detectionChance(player)) {
            GameIO.out().println("Вы поздно заметили противников. Они атакуют первыми!");
            return fightEnemies(player, scanner, difficulty, random, enemies, true);
        }
        while (true) {
            GameIO.out().println("Вы заметили врагов раньше, чем они вас:");
            GameIO.battle(player, enemies, enemies.get(0));
            int ambushChance = Math.max(15, 70 - player.getSleepiness() / 3 - player.getNoise() / 3);
            GameIO.menu("Встреча с противниками", "Выберите, как действовать.");
            GameIO.action(1, "Обойти", "Шанс успеха: " + SurvivalSystems.bypassChance(player) + "%");
            GameIO.action(2, "Засада", "Шанс: " + ambushChance + "% · дополнительная атака без ответа");
            GameIO.action(3, "Атаковать открыто");
            GameIO.action(0, "Тихо вернуться домой");
            if (!scanner.hasNext()) return false;
            int choice = SurvivalSystems.choice(scanner);
            if (choice == 0) {
                player.increaseHunger(2); player.increaseSleepiness(2);
                if (player instanceof Fighter) ((Fighter) player).spendAvailableStamina(3);
                showMovement("Тихое возвращение домой", 5);
                return false;
            }
            if (choice == 3) return fightEnemies(player, scanner, difficulty, random, enemies);
            if (choice == 1) {
                int chance = SurvivalSystems.bypassChance(player);
                if (!player.spendStamina(5)) { GameIO.out().println("Не хватает сил для обхода. Атакуйте или возвращайтесь."); continue; }
                player.increaseHunger(2); player.increaseSleepiness(2);
                showMovement("Обходите противников", 3);
                if (random.nextInt(100) < chance) { GameIO.out().println("Вы прошли незамеченными. Поиск можно продолжить."); return true; }
                GameIO.out().println("Вас заметили при обходе! Противники атакуют первыми.");
                return fightEnemies(player, scanner, difficulty, random, enemies, true);
            }
            if (choice == 2) {
                GameIO.targets("Цель для засады", enemies);
                int target = SurvivalSystems.choice(scanner);
                if (target == 0) continue;
                if (target < 1 || target > enemies.size()) { GameIO.out().println("Неверная цель."); continue; }
                Fighter fighter = player instanceof Fighter ? (Fighter) player : null;
                Weapons weapon = fighter == null ? null : fighter.getEquippedWeapon();
                if (weapon != null && weapon.getDurability() == 0) { GameIO.out().println("Оружие сломано: сначала смените его в открытом бою."); continue; }
                if (weapon != null && weapon.getAmmoType() != null && fighter.getAmmoCount(weapon.getAmmoType()) == 0) {
                    GameIO.out().println("Для засады нет боеприпасов."); continue;
                }
                WeaponStyle style = fighter == null ? WeaponStyle.FISTS : fighter.getWeaponStyle();
                int cost = fighter == null ? style.staminaCost : fighter.staminaCost(style.staminaCost);
                if (player.getStamina() < cost) { GameIO.out().println("Для засады не хватает сил."); continue; }
                if (fighter != null && GameIO.supportsTactics()) {
                    boolean concealed = random.nextInt(100) < ambushChance;
                    GameIO.out().println(concealed ? "Вы заняли позицию для засады. Выберите цель и линию атаки на поле."
                            : "Вас заметили при подготовке засады. Враги ходят первыми.");
                    return fightEnemies(player, scanner, difficulty, random, enemies, !concealed);
                }
                boolean success = random.nextInt(100) < ambushChance;
                player.increaseHunger(4); player.increaseSleepiness(2);
                if (!success) {
                    player.spendStamina(style.staminaCost);
                    GameIO.out().println("Засада сорвалась. Потрачены силы на подготовку; враги атакуют первыми.");
                    return fightEnemies(player, scanner, difficulty, random, enemies, true);
                }
                AttackResult result = attack(player, enemies.get(target - 1), random);
                GameIO.out().println("Засада удалась: попаданий " + result.hits + ", урон " + result.damage + ". Ответа на эту атаку нет.");
                if (weapon != null && weapon.isLoud() && random.nextInt(100) < 25
                        && addReinforcement(enemies, difficulty <= 1 ? 3 : difficulty == 2 ? 6 : 10, random))
                    GameIO.out().println("Выстрел из засады привлёк подкрепление: " + enemies.get(enemies.size() - 1).getName());
                return fightEnemies(player, scanner, difficulty, random, enemies);
            }
            GameIO.out().println("Выберите 0–3.");
        }
    }

    static boolean fightEnemies(SurvivorInfo player, Scanner scanner, int difficulty, java.util.Random random,
                                List<CombatEnemy> zombies) {
        return fightEnemies(player, scanner, difficulty, random, zombies, false);
    }
    static boolean fightEnemies(SurvivorInfo player, Scanner scanner, int difficulty, java.util.Random random,
                                List<CombatEnemy> zombies, boolean enemiesFirst) {
        player.reduceNoise(60);
        try {
            if (player instanceof Fighter && GameIO.supportsTactics())
                return TacticalBattle.play((Fighter) player, scanner, difficulty, random, zombies, enemiesFirst);
            return fightEnemiesInternal(player, scanner, difficulty, random, zombies, enemiesFirst);
        }
        finally { player.reduceNoise(60); GameIO.clearBattle(); }
    }

    private static boolean fightEnemiesInternal(SurvivorInfo player, Scanner scanner, int difficulty, java.util.Random random,
                                List<CombatEnemy> zombies, boolean enemiesFirst) {
        List<String> messages = new ArrayList<>();
        boolean humans = zombies.stream().anyMatch(enemy -> enemy instanceof HostileSurvivor);
        messages.add(humans ? "Враждебные выжившие перекрыли путь. Они атакуют без переговоров!"
                : "Встретились зомби: " + zombies.size() + ". Идёт бой...");
        if (humans && zombies.stream().anyMatch(enemy -> enemy instanceof HostileSurvivor && enemy.isRanged()))
            messages.add("ОПАСНО: рейдеры в броне и с огнестрелом. Дистанция не защищает от пуль!");
        int battleHunger = 0;
        boolean playerStunned = enemiesFirst;
        int targetIndex = 0;
        int maxZombies = difficulty <= 1 ? 3 : difficulty == 2 ? 6 : 10;
        while (player.isAlive() && zombies.stream().anyMatch(CombatEnemy::isAlive)) {
            if (!zombies.get(targetIndex).isAlive()) {
                for (int i = 0; i < zombies.size(); i++) {
                    if (zombies.get(i).isAlive()) { targetIndex = i; break; }
                }
            }
            CombatEnemy zombie = zombies.get(targetIndex);
            showBattle(player, zombies, zombie, messages);
            boolean blockCounter = false;
            boolean guarded = false;
            boolean loudShot = false;
            boolean skipTurn = playerStunned;
            playerStunned = false;
            if (!skipTurn) {
                WeaponStyle style = player instanceof Fighter ? ((Fighter) player).getWeaponStyle() : WeaponStyle.FISTS;
                boolean ranged = player instanceof Fighter && ((Fighter) player).getEquippedWeapon() != null
                        && ((Fighter) player).getEquippedWeapon().getAmmoType() != null;
                int attackCost = player instanceof Fighter ? ((Fighter) player).staminaCost(style.staminaCost) : style.staminaCost;
                int escapeCost = player instanceof Fighter ? ((Fighter) player).staminaCost(10) : 10;
                GameIO.menu("Бой", "Ваш ход");
                GameIO.action(1, ranged ? "Стрелять" : "Атаковать", attackCost + " выносливости" + (ranged ? " · 1 боеприпас" : ""));
                GameIO.action(2, "Отступить", escapeCost + " выносливости");
                GameIO.action(3, "Осмотреть тело", "Травмы, кровотечение и лечение");
                GameIO.action(4, "Передышка");
                GameIO.action(5, "Сменить оружие", "Занимает ход");
                GameIO.action(6, "Выбрать цель", "Не тратит ход");
                if (!scanner.hasNextInt()) {
                    if (!scanner.hasNext()) {
                        player.increaseHunger(Math.max(0, 12 - battleHunger));
                        return false;
                    }
                    scanner.next();
                    messages.add("Введите номер действия.");
                    continue;
                }
                int action = scanner.nextInt();
                if (action == 1) {
                    AttackResult result = attack(player, zombie, random);
                    if (result.brokenWeapon) { messages.add("Оружие сломано. Смените его или выберите кулаки."); continue; }
                    if (result.outOfAmmo) {
                        messages.add("Нет подходящих боеприпасов. Смените оружие.");
                        continue;
                    }
                    if (result.exhausted) {
                        messages.add("Недостаточно выносливости. Выберите передышку или более лёгкое оружие.");
                        continue;
                    }
                    Weapons firedWeapon = player instanceof Fighter ? ((Fighter) player).getEquippedWeapon() : null;
                    loudShot = firedWeapon != null && firedWeapon.isLoud();
                    if (loudShot) messages.add("> Грохот выстрела привлекает заражённых. Шум: " + player.getNoise() + "/60.");
                    messages.add(result.hits == 0 ? "> Промах! Выносливость потрачена."
                            : "> Попаданий: " + result.hits + ". Урон: " + result.damage + ".");
                    if (result.stunned) messages.add("> Противник оглушён и пропускает ответную атаку.");
                    if (result.keptDistance) messages.add("> Вы удержали дистанцию: цель не сможет атаковать.");
                    blockCounter = result.stunned || result.keptDistance;
                    if (firedWeapon != null && firedWeapon.getDurability() == 0) messages.add("Оружие сломалось!");
                } else if (action == 2) {
                    showMovement("Пытаетесь отступить", 3);
                    player.increaseHunger(4);
                    player.increaseSleepiness(2);
                    battleHunger += 4;
                    boolean tired = player.getStamina() < escapeCost;
                    if (player instanceof Fighter) ((Fighter) player).spendAvailableStamina(10);
                    else player.spendStamina(Math.min(10, player.getStamina()));
                    if (tired) messages.add("Мало сил: шанс побега снижен с 60% до 30%.");
                    if (random.nextInt(100) < 40) {
                        int damage = (1 + random.nextInt(25));
                        player.takeDamage(damage);
                        messages.add("> При попытке отступить противник нанёс удар! -" + damage + " HP.");
                        if (!player.isAlive()) break;
                    } else {
                        messages.add("> Вы избежали удара при попытке отступить.");
                    }
                    if (random.nextInt(100) < (tired ? 30 : 60)) {
                        messages.add("> Вы отступили. Вылазка завершена без новой добычи.");
                        player.increaseHunger(Math.max(0, 12 - battleHunger));
                        showBattle(player, zombies, zombie, messages);
                        travel(player, "Возвращение в убежище после отступления", 5);
                        return false;
                    }
                    messages.add("> Отступить не удалось!");
                    continue;
                } else if (action == 3) {
                    if (!(player instanceof Fighter) || !inspectBody((Fighter) player, scanner)) continue;
                    messages.add("Рана обработана. Противники не будут ждать.");
                } else if (action == 4) {
                    GameIO.menu("Передышка в бою", "Противники продолжат атаковать.");
                    GameIO.action(1, "Обычная передышка", "До +25 выносливости · без защиты");
                    GameIO.action(2, "Передышка под защитой", "До +15 выносливости · урон −60% · без оглушения на этот ход");
                    GameIO.action(0, "Отмена");
                    if (!scanner.hasNextInt()) {
                        if (scanner.hasNext()) scanner.next();
                        messages.add("Выберите вариант передышки.");
                        continue;
                    }
                    int restChoice = scanner.nextInt();
                    if (restChoice == 0) continue;
                    if (restChoice != 1 && restChoice != 2) {
                        messages.add("Неверный вариант передышки.");
                        continue;
                    }
                    guarded = restChoice == 2;
                    int before = player.getStamina();
                    player.restoreStamina(guarded ? 15 : 25);
                    messages.add((guarded ? "Передышка под защитой: +" : "Обычная передышка: +")
                            + (player.getStamina() - before) + " выносливости.");
                    if (guarded) messages.add("Защита на этот ход: входящий урон -60%, иммунитет к оглушению.");
                    else messages.add("Противники могут атаковать во время передышки.");
                } else if (action == 5) {
                    if (!chooseEquipment(player, scanner)) continue;
                    messages.add("Экипировано: " + ((Fighter) player).getWeaponName());
                } else if (action == 6) {
                    GameIO.targets("Выбрать цель", zombies);
                    if (!scanner.hasNextInt()) {
                        if (scanner.hasNext()) scanner.next();
                        messages.add("Введите номер цели.");
                        continue;
                    }
                    int chosen = scanner.nextInt();
                    if (chosen == 0) continue;
                    if (chosen < 1 || chosen > zombies.size() || !zombies.get(chosen - 1).isAlive()) {
                        messages.add("Эту цель выбрать нельзя.");
                        continue;
                    }
                    targetIndex = chosen - 1;
                    messages.add("Цель: " + zombies.get(targetIndex).getName());
                    continue;
                } else {
                    messages.add("Неверное действие.");
                    continue;
                }
            }
            GameIO.advanceMinutes(1);
            player.increaseHunger(4);
            player.increaseSleepiness(2);
            battleHunger += 4;
            if (!zombie.isAlive()) {
                messages.add("> " + zombie.getName() + " уничтожен.");

            }
            boolean scream = false;
            for (CombatEnemy enemy : zombies) {
                if (!player.isAlive()) break;
                if (!enemy.isAlive()) continue;
                if (enemy == zombie && blockCounter) continue;
                if (enemy instanceof Zombie && ((Zombie) enemy).getType() == ZombieType.SCREAMER && random.nextInt(100) < 20) {
                    scream = true;
                    messages.add("> " + enemy.getName() + " зовёт подкрепление!");
                    continue;
                }
                int accuracy = enemy.getAttackAccuracy();
                int minimum = enemy.getMinDamage();
                int maximum = enemy.getMaxDamage();
                boolean enemyShot = enemy.isRanged();
                enemy.spendAttackAmmo();
                if (enemyShot) {
                    player.addNoise(20);
                    loudShot = true;
                    messages.add("> " + enemy.getName() + " стреляет! Шум: " + player.getNoise() + "/60.");
                }
                if (random.nextInt(100) < accuracy) {
                    int damage = (minimum + random.nextInt(maximum - minimum + 1));
                    if (guarded) damage = (damage * 40 + 99) / 100;
                    player.takeDamage(damage);
                    messages.add("> " + enemy.getName() + " атакует! -" + damage + " HP.");
                    if (!guarded && player.isAlive() && !playerStunned
                            && random.nextInt(100) < player.getStunChance()) {
                        playerStunned = true;
                        messages.add("> Вы ослабли от голода и усталости: удар оглушил вас! Вы пропускаете следующий ход.");
                    }
                } else {
                    messages.add("> " + enemy.getName() + " промахнулся.");
                }
            }
            if (player instanceof Fighter && player.isAlive()) {
                int bleeding = ((Fighter) player).sufferBleeding();
                if (bleeding > 0) messages.add("> Кровотечение: -" + bleeding + " HP. Осмотрите тело и наложите бинт.");
            }
            if (player.isAlive() && (scream || loudShot && random.nextInt(100) < 25)) {
                if (addReinforcement(zombies, maxZombies, random))
                    messages.add("> На шум пришёл " + zombies.get(zombies.size() - 1).getName() + ". Атакует со следующего хода.");
                else messages.add("> Шум разнёсся по округе, но новых врагов пока не видно.");
            }
        }
        player.increaseHunger(Math.max(0, 12 - battleHunger));
        if (player.isAlive() && player instanceof Fighter) {
            Fighter fighter = (Fighter) player;
            List<ItemInfo> trophies = new ArrayList<>();
            for (CombatEnemy enemy : zombies) {
                if (enemy instanceof Zombie) trophies.addAll(((Zombie) enemy).claimEars(random));
                if (enemy instanceof HostileSurvivor) {
                    for (ItemInfo item : ((HostileSurvivor) enemy).claimLoot()) {
                        trophies.add(item);
                    }
                }
            }
            if (!trophies.isEmpty()) {
                showBattle(player, zombies, null, messages);
                GameIO.out().println("Победа! Выберите трофеи, которые сможете унести:");
                SurvivalSystems.collect(fighter, trophies, scanner);
            }
        }
        messages.add(player.isAlive()
                ? "> Все противники уничтожены. Ты выжил!" : "> Вы погибли в бою.");
        showBattle(player, zombies, null, messages);
        return player.isAlive();
    }

    static Fighter asJohn(SurvivorInfo saved) {
        if (saved instanceof Fighter && "John".equals(saved.getName())) return (Fighter) saved;
        Fighter john = new Fighter("John", "Боец", 100);
        john.takeDamage(100 - Math.min(100, saved.getHealth()));
        john.spendStamina(100 - saved.getStamina());
        john.increaseHunger(saved.getHunger());
        john.increaseSleepiness(saved.getSleepiness());
        john.addNoise(saved.getNoise());
        if (saved instanceof Fighter) {
            Fighter previous = (Fighter) saved;
            john.getInventory().clear();
            john.getInventory().addAll(previous.getInventory());
            john.getStorage().addAll(previous.getStorage());
            john.inheritHomeState(previous);
            john.equipWeapon(previous.getEquippedWeapon());
            john.equipArmor(previous.getEquippedArmor());
            if(previous.getEquippedHelmet()!=null)john.equipArmor(previous.getEquippedHelmet());
        }
        john.chooseDifficulty(saved.difficulty());
        return john;
    }

    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = GameIO.scanner();
        GameSave loaded = null;
        if (GameSave.exists(GameIO.saveFile())) {
            while (true) {
                GameIO.menu("SURVIVOR", "Продолжить путь Джона или начать заново?");
                GameIO.action(1, "Продолжить сохранённую игру");
                GameIO.action(2, "Новая игра", "Предыдущее сохранение будет заменено при следующем сохранении");
                GameIO.action(0, "Выход");
                if (!scanner.hasNext()) return;
                if (!scanner.hasNextInt()) { scanner.next(); continue; }
                int start = scanner.nextInt();
                if (start == 0) return;
                if (start == 2) break;
                if (start != 1) { GameIO.out().println("Выберите 1, 2 или 0."); continue; }
                try {
                    loaded = GameSave.load(GameIO.saveFile());
                    if (!loaded.player.isAlive()) {
                        GameIO.out().println("Этот выживший погиб. Начните новую игру или выйдите.");
                        loaded = null;
                        continue;
                    }
                    GameIO.sound(GameSound.SAVE);GameIO.out().println("Прогресс загружен: John");
                    break;
                } catch (java.io.IOException e) {
                    GameIO.out().println("Не удалось загрузить игру: " + e.getMessage());
                }
            }
        }
        SurvivorInfo selectedSurvivor = loaded == null ? new Fighter("John", "Боец", 100) : asJohn(loaded.player);
        if(loaded==null) {
            selectedSurvivor.chooseDifficulty(Difficulty.choose(scanner));
            Fighter beginner=(Fighter)selectedSurvivor;
            beginner.addItem(Shop.createItem(16));beginner.addItem(Shop.createItem(16));beginner.addItem(Shop.createItem(1));
        }
        if (loaded == null) GameIO.out().println("Ты — Джон. Труба есть, планов нет. Добро пожаловать в конец света.");
        boolean running = true;
        int balance = loaded == null ? 0 : loaded.balance;
        Locations locations = loaded == null ? new Locations() : loaded.locations;
        if(loaded==null){GameIO.mainMenu(selectedSurvivor,balance,locations);locations.getStories().radioOpening((Fighter)selectedSurvivor,scanner);GameIO.menu("","");}
        while (running) {
            GameIO.mainMenu(selectedSurvivor, balance, locations);

            int action1 = scanner.nextInt();
            if (selectedSurvivor.difficulty() == 3) {
                if (action1 == 0) { GameIO.out().println("Игра завершена без сохранения."); break; }
                if (action1 == 11 || action1 == 12) {
                    GameIO.out().println("На сложности «Последний шанс» сохранение и загрузка недоступны.");
                    continue;
                }
            }

            if (action1 == 1) {
                showHealth(selectedSurvivor);
                GameIO.out().println("Баланс: " + balance + " $; день " + locations.getStories().day());
                GameIO.menu("Состояние Джона", "Подробности о персонаже");
                GameIO.card("Боеспособность", "Урон: " + selectedSurvivor.getDamagePercent() + "%\nШтраф к точности: "
                        + selectedSurvivor.getAccuracyPenalty() + " п.п.\nРиск оглушения: " + selectedSurvivor.getStunChance() + "%");
            } else if (action1 == 2) {

                if (selectedSurvivor instanceof Fighter) {
                    Location destination = locations.choose(scanner, selectedSurvivor);
                    if (destination == null) {
                        continue;
                    }
                    GameIO.out().println(selectedSurvivor.getName() + " Отправился на вылазку");

                    boolean exploring = true;

                    GameIO.leaveHome();
                    selectedSurvivor.increaseHunger(2);
                    selectedSurvivor.increaseSleepiness(2);
                    travel(selectedSurvivor, "Путь к локации: " + destination.getName(), 5,GameIO.footsteps(locations.getNumber(destination)));
                    locations.enter(destination);
                    boolean returnedToBattle = locations.getStories().pendingBattle(locations.getNumber(destination))!=null;
                    if (locations.getStories().pendingBattle(locations.getNumber(destination))!=null) {
                        boolean cleared=TacticalBattle.resumePending(locations.getStories(),(Fighter)selectedSurvivor,scanner);
                        if(!selectedSurvivor.isAlive()){running=false;continue;}
                        if(!cleared){travel(selectedSurvivor,"Возвращение после отступления",5);continue;}
                    }
                    if (!destination.prepareForVisit((Fighter) selectedSurvivor, locations.getStories(), scanner, ThreadLocalRandom.current())) {
                        travel(selectedSurvivor, "Возвращение от входа", 5);
                        continue;
                    }
                    locations.getStories().residential().aborted=false;
                    locations.getStories().residential().onVisit(locations.getNumber(destination),locations.getStories(),(Fighter)selectedSurvivor);

                    int locationNumber = locations.getNumber(destination);
                    if (!returnedToBattle && locationNumber >= 4 && locationNumber <= 8 && selectedSurvivor instanceof Fighter) {
                        boolean safe = ExplorationEncounters.tryEncounter((Fighter)selectedSurvivor, locations, destination, scanner, ThreadLocalRandom.current());
                        if (!selectedSurvivor.isAlive()) { running=false; continue; }
                        if (!safe) { travel(selectedSurvivor,"Возвращение после встречи",5); continue; }
                    }
                    if (locations.getNumber(destination) == 12) {
                        locations.getStories().hospital().visit((Fighter)selectedSurvivor,destination,locations.getStories(),scanner,ThreadLocalRandom.current());
                        if(!selectedSurvivor.isAlive()){running=false;continue;}
                        travel(selectedSurvivor,"Возвращение домой",5);
                        continue;
                    }
                    if (locations.getNumber(destination) == 11) {
                        locations.getStories().pharmacy().visit((Fighter)selectedSurvivor,destination,locations.getStories(),scanner,ThreadLocalRandom.current());
                        if(!selectedSurvivor.isAlive()){running=false;continue;}
                        travel(selectedSurvivor,"Возвращение домой",5);
                        continue;
                    }
                    if (locations.getNumber(destination) == 10) {
                        locations.getStories().clinic().visit((Fighter)selectedSurvivor,destination,locations.getStories(),scanner,ThreadLocalRandom.current());
                        if(!selectedSurvivor.isAlive()){running=false;continue;}
                        travel(selectedSurvivor,"Возвращение домой",5);
                        continue;
                    }
                    if (locations.getNumber(destination) == 8) {
                        locations.getStories().school().visit((Fighter) selectedSurvivor, destination, locations.getStories(), scanner, ThreadLocalRandom.current());
                        if (!selectedSurvivor.isAlive()) { running = false; continue; }
                        travel(selectedSurvivor, "Возвращение домой", 5);
                        continue;
                    }
                    if (locations.getNumber(destination) == 7) {
                        locations.getStories().schoolyard().visit((Fighter) selectedSurvivor, destination, locations.getStories(), scanner, ThreadLocalRandom.current());
                        if (!selectedSurvivor.isAlive()) { running = false; continue; }
                        travel(selectedSurvivor, "Возвращение домой", 5);
                        continue;
                    }
                    if (locations.getNumber(destination) == 6) {
                        locations.getStories().rooftops().visit((Fighter) selectedSurvivor, destination, locations.getStories(), scanner, ThreadLocalRandom.current());
                        if (!selectedSurvivor.isAlive()) { running = false; continue; }
                        travel(selectedSurvivor, "Возвращение домой", 5);
                        continue;
                    }
                    if (locations.getNumber(destination) == 5) {
                        locations.getStories().apartment().visit((Fighter) selectedSurvivor, destination, locations.getStories(), scanner, ThreadLocalRandom.current());
                        if (!selectedSurvivor.isAlive()) { running = false; continue; }
                        travel(selectedSurvivor, "Возвращение домой", 5);
                        continue;
                    }
                    if (locations.getNumber(destination) == 4) {
                        balance = locations.getStories().grocery().visit((Fighter) selectedSurvivor, destination, locations.getStories(), scanner, ThreadLocalRandom.current(), balance);
                        GameIO.balance(balance);
                        if (!selectedSurvivor.isAlive()) { running = false; continue; }
                        travel(selectedSurvivor, "Возвращение домой", 5);
                        continue;
                    }
                    if (locations.getNumber(destination) == 1) {
                        locations.getStories().courtyard().visit((Fighter) selectedSurvivor, destination, locations.getStories(), scanner, ThreadLocalRandom.current());
                        if (!selectedSurvivor.isAlive()) { running = false; continue; }
                        travel(selectedSurvivor, "Возвращение домой", 5);
                        continue;
                    }
                    while (exploring) {
                        if (destination.isNightOnly() && !locations.getStories().isNight()) {
                            GameIO.out().println("Рассвело. Ночной маршрут закрывается — возвращаемся домой.");
                            travel(selectedSurvivor, "Возвращение домой", 5);
                            break;
                        }
                        if(ForestStories.supports(locations.getNumber(destination))){
                            int[] wallet={balance};
                            int forestChoice=locations.getStories().forest().menu((Fighter)selectedSurvivor,destination,locations.getNumber(destination),locations.getStories(),scanner,ThreadLocalRandom.current(),wallet);
                            balance=wallet[0];GameIO.balance(balance);
                            if(!selectedSurvivor.isAlive()){running=false;break;}
                            if(locations.getStories().pendingBattle(locations.getNumber(destination))!=null){travel(selectedSurvivor,"Возвращение после отступления",5);break;}
                            if(forestChoice<0){travel(selectedSurvivor,"Возвращение домой",5);break;}
                            if(forestChoice==0)continue;
                        }
                        if(locations.getNumber(destination)==16){PoliceArchive.enter((Fighter)selectedSurvivor,locations.getStories(),scanner,ThreadLocalRandom.current());travel(selectedSurvivor,"Возвращение из участка",5);break;}
                        if (destination.isDepleted() && !locations.getStories().monsterDens().accessible(locations.getNumber(destination))) {
                            locations.getStories().findArchiveKey((Fighter)selectedSurvivor,locations.getNumber(destination));
                            if(locations.getNumber(destination)==15 && PoliceArchive.unlocked((Fighter)selectedSurvivor)) {
                                GameIO.storyMenu("Полицейский участок","Обычные запасы исчерпаны. Архив доступен отдельно.");GameIO.action(1,"Попасть в архив");GameIO.action(0,"Вернуться домой");
                                if(SurvivalSystems.choice(scanner)==1)PoliceArchive.enter((Fighter)selectedSurvivor,locations.getStories(),scanner,ThreadLocalRandom.current());
                            }

                            if(locations.getStories().archiveComplete && (locations.getNumber(destination)==13 || locations.getNumber(destination)==41)){
                                GameIO.storyMenu(destination.getName(),"Обычные запасы исчерпаны. Сюжетная зацепка остаётся доступной.");GameIO.action(1,locations.getNumber(destination)==13?"Проверить медицинский экипаж №17":"Разведать резервный маршрут");GameIO.action(0,"Назад");
                                if(SurvivalSystems.choice(scanner)==1)PoliceArchive.followup((Fighter)selectedSurvivor,locations.getStories(),locations.getNumber(destination),scanner);
                            }
                            List<ItemInfo> recovered=locations.getStories().guaranteedChips(locations.getNumber(destination));
                            if(!recovered.isEmpty()){locations.getStories().collectEquipment(locations.getNumber(destination),(Fighter)selectedSurvivor,recovered,scanner);}

                            if (locations.getStories().caseCount(locations.getStories().currentPlace()) > 0
                                    && !Crafting.searchCases((Fighter) selectedSurvivor, locations.getStories(), scanner, ThreadLocalRandom.current())) {
                                if (!selectedSurvivor.isAlive()) running = false;
                                else travel(selectedSurvivor, "Возвращение после осмотра", 5);
                                break;
                            }
                            if(!locations.getStories().residential().points(locations.getNumber(destination),locations.getStories()).isEmpty())
                                locations.getStories().residential().pointsMenu((Fighter)selectedSurvivor,locations.getNumber(destination),locations.getStories(),scanner,ThreadLocalRandom.current());
                            if(!selectedSurvivor.isAlive()){running=false;break;}
                            if(locations.getStories().residential().aborted){travel(selectedSurvivor,"Возвращение после сюжетной встречи",5);break;}
                            WorldStories.print(locations.getStories().discover(locations.getNumber(destination), destination.getName(), ThreadLocalRandom.current(), false));
                            GameIO.out().println("Здесь всё обыскано. Запасы исчерпаны — пора выбрать другую локацию.");
                            travel(selectedSurvivor, "Возвращение домой", 5);
                            break;
                        }
                        Fighter explorer = (Fighter) selectedSurvivor;
                        Location.SearchPoint point = destination.chooseSearchPoint(explorer, scanner);
                        if (destination.hasDetailedPoints() && point == null) {
                            if(!selectedSurvivor.isAlive())running=false;
                            else travel(selectedSurvivor, "Возвращение домой", 5);
                            break;
                        }
                        SearchMode mode = SearchMode.choose(explorer, locations.getStories(), scanner, destination);
                        if (mode == null) {
                            if(!selectedSurvivor.isAlive()){running=false;break;}
                            travel(selectedSurvivor, "Возвращение домой", 5);
                            break;
                        }
                        if (!mode.begin(explorer, locations.getStories())) continue;
                        GameIO.out().println(mode.label + " · " + mode.minutes + " минут · " + locations.getStories().time());
                        boolean pathClear;
                        try {
                            pathClear = ExplorationEncounters.tryEncounter(explorer, locations, destination, scanner, ThreadLocalRandom.current());
                        } finally { explorer.searchStealthModifier = 0; }
                        if (selectedSurvivor.getHealth() == 0) {
                            GameIO.out().println(selectedSurvivor.getName() + " погиб.");
                            running = false;
                            break;

                        }
                        if (!pathClear) {
                            exploring = false;
                            continue;
                        }
                        if (destination.isNightOnly() && !locations.getStories().isNight()) {
                            GameIO.out().println("За время встречи рассвело. Проход закрывается; добычу искать уже нельзя.");
                            travel(selectedSurvivor, "Возвращение домой", 5);
                            break;
                        }
                        searchLocation((Fighter) selectedSurvivor, destination, locations, scanner, ThreadLocalRandom.current(), mode, point);
                        if(!selectedSurvivor.isAlive()) {running=false;break;}
                        if(locations.getStories().residential().aborted) {travel(selectedSurvivor,"Возвращение после сюжетной встречи",5);break;}
                        boolean exhausted=destination.isDepleted() && !locations.getStories().monsterDens().accessible(locations.getNumber(destination));
                        if (exhausted) GameIO.out().println("Последнее место обыскано. Возвращаемся домой.");
                        if (exhausted || !continueExploring(selectedSurvivor, scanner, locations.getStories())) {
                            if(!selectedSurvivor.isAlive()){running=false;break;}
                            selectedSurvivor.increaseHunger(2);
                            selectedSurvivor.increaseSleepiness(2);
                            travel(selectedSurvivor, "Возвращение домой", 5);
                            ExplorationEncounters.tryEncounter(explorer, locations, destination, scanner, ThreadLocalRandom.current());
                            if (selectedSurvivor.getHealth() == 0) {
                                GameIO.out().println(selectedSurvivor.getName() + " погиб.");
                                running = false;
                            }
                            exploring = false;
                        }
                    }

                } else {
                    GameIO.out().println(selectedSurvivor.getName() + " не может отправляться на вылазку");
                }
            } else if (action1 == 3) {
                GameIO.sound(GameSound.BAG);
                if (selectedSurvivor instanceof Fighter) {
                    List<ItemInfo> items = ((Fighter) selectedSurvivor).getInventory();
                    if (items.isEmpty()) {
                        GameIO.out().println("У персонажа нет предметов.");
                    } else {
                        GameIO.menu("Инвентарь", "Всё, что Джон несёт с собой");
                        for (ItemInfo item : items) {
                            GameIO.card(item.getName(), item.getDescription() + "\n" + item.physicalStats()
                                    + (item instanceof Weapons ? "\n" + ((Weapons) item).combatStats() : ""));
                        }
                    }
                } else {
                    GameIO.out().println("У этого персонажа нет инвентаря.");
                }
            } else if (action1 == 4) {
                GameIO.leaveHome();
                selectedSurvivor.increaseHunger(2);
                selectedSurvivor.increaseSleepiness(2);
                travel(selectedSurvivor, "Путь в магазин", 3);
                boolean in_the_store = true;
                while (in_the_store) {
                    showHealth(selectedSurvivor);
                    GameIO.balance(balance);
                    GameIO.menu("Магазин", "Торговля и подготовка к следующей вылазке");
                    GameIO.action(1, "Продать", "Обменять вещи из рюкзака на деньги");
                    GameIO.action(2, "Купить", "Еда, лекарства и снаряжение");
                    GameIO.action(3, "Осмотреть тело", "Травмы, кровотечение и лечение");
                    GameIO.action(0, "Вернуться домой");
                    int num = scanner.nextInt();
                    if (num == 1 || num == 2) {
                        if (!(selectedSurvivor instanceof Fighter)) {
                            GameIO.out().println("У этого персонажа нет инвентаря.");
                            continue;
                        }
                        Fighter buyer = (Fighter) selectedSurvivor;
                        balance = num == 1 ? Shop.sell(buyer, scanner, balance) : Shop.buy(buyer, scanner, balance);
                    } else if (num == 3) {
                        if (selectedSurvivor instanceof Fighter) inspectBody((Fighter) selectedSurvivor, scanner);
                    } else if (num == 0) {
                        travel(selectedSurvivor, "Возвращение из магазина", 3);
                        in_the_store = false;
                    } else {
                        GameIO.out().println("Неверный ввод. Попробуйте снова.");
                    }
                }

            } else if (action1 == 5) {
                if (selectedSurvivor instanceof Fighter) inspectBody((Fighter) selectedSurvivor, scanner);
            } else if (action1 == 6) {
                chooseEquipment(selectedSurvivor, scanner);
            } else if (action1 == 7) {
                rest(selectedSurvivor, 30);
            } else if (action1 == 8) {
                showGuide();
                GameIO.action(0, "Назад");
                SurvivalSystems.choice(scanner);
                GameIO.menu("", "");
            } else if (action1 == 9) {
                useFood(selectedSurvivor, scanner);
            } else if (action1 == 10) {
                int healed = ShelterUpgrades.sleep((Fighter) selectedSurvivor, locations.getStories());
                if (healed < 0) GameIO.out().println("Спать можно только при сонливости выше 40. Сейчас: " + selectedSurvivor.getSleepiness() + "/100.");
                else GameIO.out().println("Сон 8 часов. Обработанные раны без кровотечения заживают. Сонливость: 0, выносливость восстановлена, голод +10.");
            } else if (action1 == 17) {
                if (selectedSurvivor instanceof Fighter) Shelter.menu((Fighter) selectedSurvivor, scanner);
            } else if (action1 == 16) {
                locations.getStories().showJournal(scanner);
            } else if (action1 >= 13 && action1 <= 15) {
                if (!(selectedSurvivor instanceof Fighter)) { GameIO.out().println("Нужен боец с инвентарём."); continue; }
                Fighter fighter = (Fighter) selectedSurvivor;
                if (action1 == 13) SurvivalSystems.backpack(fighter, scanner);
                if (action1 == 14) SurvivalSystems.repairMenu(fighter, scanner);
                if (action1 == 15) SurvivalSystems.armorMenu(fighter, scanner);
            } else if (action1 == 11 || action1 == 0) {
                try {
                    GameSave.save(GameIO.saveFile(), selectedSurvivor, balance, locations);
                    GameIO.sound(GameSound.SAVE);
                    GameIO.out().println("Игра сохранена: " + "survivor-save.dat");
                    if (action1 == 0) running = false;
                } catch (java.io.IOException e) {
                    GameIO.out().println("Не удалось сохранить игру: " + e.getMessage());
                }
            } else if (action1 == 12) {
                try {
                    GameSave state = GameSave.load(GameIO.saveFile());
                    if (!state.player.isAlive()) {
                        GameIO.out().println("В сохранении персонаж погиб.");
                        continue;
                    }
                    selectedSurvivor = asJohn(state.player);
                    balance = state.balance;
                    locations = state.locations;
                    GameIO.sound(GameSound.SAVE);GameIO.out().println("Игра загружена.");
                } catch (java.io.IOException e) {
                    GameIO.out().println("Не удалось загрузить игру: " + e.getMessage());
                }
            } else {
                GameIO.out().println("Неверный ввод. Попробуйте снова.");
            }
        }
        scanner.close();
    }
}
