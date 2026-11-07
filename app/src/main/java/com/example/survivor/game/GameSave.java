package com.example.survivor.game;
import java.io.*;
import java.util.*;
final class GameSave implements Serializable {
private static final long serialVersionUID = 1L;
private final int version = 1;
final SurvivorInfo player; final int balance; final Locations locations;
GameSave(SurvivorInfo player, int balance, Locations locations) { this.player = player; this.balance = balance; this.locations = locations; }
    static boolean exists(File target) { return target.isFile() || new File(target.getPath() + ".bak").isFile(); }

    static synchronized void save(File target, SurvivorInfo player, int balance, Locations locations) throws IOException {
        if (player != null && player.difficulty() == 3)
            throw new IOException("На сложности «Последний шанс» сохранения недоступны.");
        GameSave state = new GameSave(player, balance, locations);
        state.validate();
        File temporary = new File(target.getPath() + ".tmp");
        File backup = new File(target.getPath() + ".bak");
        recover(target, backup);
        try {
            try (FileOutputStream file = new FileOutputStream(temporary);
                 ObjectOutputStream output = new ObjectOutputStream(file)) {
                output.writeObject(state);
                output.flush();
                file.getFD().sync();
            }
            if (target.exists() && !target.renameTo(backup)) throw new IOException("Не удалось обновить сохранение.");
            if (!temporary.renameTo(target)) {
                recover(target, backup);
                throw new IOException("Не удалось записать сохранение.");
            }
            backup.delete();
        } finally { temporary.delete(); }
    }

    private static void recover(File target, File backup) throws IOException {
        if (backup.exists() && !target.exists() && !backup.renameTo(target))
            throw new IOException("Не удалось восстановить предыдущее сохранение.");
        if (backup.exists() && target.exists() && !backup.delete())
            throw new IOException("Не удалось обновить резервную копию.");
    }

    static synchronized GameSave load(File target) throws IOException {
        recover(target, new File(target.getPath() + ".bak"));
        if (!target.isFile()) throw new IOException("Сохранение ещё не создано.");
        if (target.length() > 5_000_000) throw new IOException("Слишком большой файл сохранения.");
        Set<String> allowed = new HashSet<>(Arrays.asList("GameSave", "SurvivorInfo", "Fighter", "Medic", "Engineer",
                "Locations", "Location", "Location$SearchPoint", "LocationState", "LootProfile",
                "BodyPart", "BodyInjury", "ItemInfo", "Food", "WildFood", "ForestStories", "SemenStory", "MonsterDens", "MonsterDens$Den", "Tools", "Weapons", "MedicalItem",
                "Ammunition", "AmmoType", "WeaponStyle", "WorldStories", "ShelterUpgrades", "ResidentialStories", "DistrictClinic", "PharmacyWarehouse", "CityHospital", "QuietCourtyard", "GroceryStore", "ApartmentBuilding", "Rooftops", "Schoolyard", "School", "ZombieEar", "TacticalBattle", "TacticalBattle$Unit", "TacticalBattle$Civilian", "TacticalBattle$Obstacle", "PoliceArchive$Attempt", "RouteAfterArchive", "CombatEnemy", "Zombie", "ZombieType", "HostileSurvivor"));
        Set<String> standard = new HashSet<>(Arrays.asList("java.lang.Enum", "java.lang.String", "java.lang.Object",
                "java.lang.Integer", "java.lang.Number", "java.util.ArrayList", "java.util.HashSet", "java.util.HashMap",
                "java.util.EnumMap", "java.util.IdentityHashMap"));
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(target)) {
            @Override protected Class<?> resolveClass(ObjectStreamClass description) throws IOException, ClassNotFoundException {
                String name = description.getName();
                String component = name;
                while (component.startsWith("[")) component = component.substring(1);
                boolean primitiveArray = name.startsWith("[") && component.length() == 1 && "ZBCSIJFD".contains(component);
                if (component.startsWith("L") && component.endsWith(";")) component = component.substring(1, component.length() - 1);
                String prefix = "com.example.survivor.game.";
                if (!primitiveArray && !standard.contains(component)
                        && !(component.startsWith(prefix) && allowed.contains(component.substring(prefix.length()))))
                    throw new InvalidClassException("Неизвестный класс в сохранении", name);
                return super.resolveClass(description);
            }
        }) {
            Object value = input.readObject();
            if (!(value instanceof GameSave)) throw new IOException("Это не сохранение игры.");
            GameSave state = (GameSave) value;
            state.validate();
            if (state.player.difficulty() == 3)
                throw new IOException("На сложности «Последний шанс» загрузка недоступна. Начните новую игру.");
            return state;
        } catch (ClassNotFoundException | RuntimeException error) {
            throw new IOException("Сохранение повреждено или несовместимо.", error);
        }
    }

    private void validate() throws IOException {
        if (locations != null && (locations.getStories().day() < 1 || locations.getStories().minuteOfDay() < 0
                || locations.getStories().minuteOfDay() >= 1440)) throw new IOException("Некорректные игровые часы.");
        if(locations!=null && !locations.getStories().residential().valid())throw new IOException("Некорректное состояние сюжетных событий.");
        if (version != 1 || player == null || locations == null || balance < 0
                || !(player instanceof Fighter || player instanceof Medic || player instanceof Engineer)
                || player.difficulty()<1 || player.difficulty()>3 || player.getName() == null || player.getMaxHealth() <= 0
                || player.getHealth() < 0 || player.getHealth() > player.getMaxHealth()
                || player.getStamina() < 0 || player.getStamina() > player.getMaxStamina()
                || player.getHunger() < 0 || player.getHunger() > 100
                || player.getSleepiness() < 0 || player.getSleepiness() > 100
                || player.getNoise() < 0 || player.getNoise() > 60)
            throw new IOException("Некорректные данные сохранения.");
        if (player instanceof Fighter) {
            Fighter fighter = (Fighter) player;
            if (!fighter.getUpgrades().valid()) throw new IOException("Некорректные улучшения убежища.");
            List<ItemInfo> possessions = new ArrayList<>(fighter.getInventory());
            possessions.addAll(fighter.getStorage());
            Set<ItemInfo> unique = Collections.newSetFromMap(new IdentityHashMap<>());
            for (ItemInfo item : possessions) {
                if (!unique.add(item)) throw new IOException("Предмет находится сразу в нескольких местах.");
                if (item == null || item.getName() == null || item.getPrice() < 0
                        || item.getRarity() < 1 || item.getRarity() > 5)
                    throw new IOException("Некорректный предмет в сохранении.");
                if (item.getDurability() < 0 || item.getDurability() > 100 || item.getWeightGrams() < 0)
                    throw new IOException("Некорректные вес или прочность предмета.");
                if (item instanceof Weapons && ((Weapons) item).getStyle() == null)
                    throw new IOException("Некорректное оружие.");
                if (item instanceof Ammunition) {
                    Ammunition ammo = (Ammunition) item;
                    if (ammo.getType() == null || ammo.getQuantity() < 1 || ammo.getQuantity() > ammo.getType().packSize)
                        throw new IOException("Некорректные боеприпасы.");
                }
            }
            fighter.getEquippedWeapon();
            ItemInfo armor = fighter.getEquippedArmor();
            if (armor != null && armor.getArmorProtection() <= 0) throw new IOException("Некорректная броня.");
            ItemInfo helmet=fighter.getEquippedHelmet();
            if(helmet!=null && (!Fighter.isHelmet(helmet) || helmet.getArmorProtection()<=0))throw new IOException("Некорректный шлем.");
        }
    }
}
