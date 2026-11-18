package com.example.survivor.game;

import java.util.HashMap;
import java.util.Random;

/** Stable one-time rolls: an item left behind remains the same after a save. */
final class EarlyLoot {
    private EarlyLoot() {}

    static boolean search(Fighter player, HashMap<String, ItemInfo> finds, String key,
                          Random random, int normalChance, int itemNumber, int junkCategory) {
        return searchItem(player, finds, key, random, normalChance, Shop.createItem(itemNumber), junkCategory);
    }
    static boolean searchWater(Fighter player, HashMap<String, ItemInfo> finds, String key,
                               Random random, int normalChance, int junkCategory) {
        return searchItem(player, finds, key, random, normalChance,
                new Food("Бутылка минеральной воды", "Чистая питьевая вода", 1, 4, 0), junkCategory);
    }
    private static boolean searchItem(Fighter player, HashMap<String, ItemInfo> finds, String key,
                                      Random random, int normalChance, ItemInfo usefulItem, int junkCategory) {
        if (!finds.containsKey(key)) {
            int adjustment = player.difficulty() == 1 ? 20 : player.difficulty() == 3 ? -15 : 0;
            int chance = Math.max(0, Math.min(100, normalChance + adjustment));
            ItemInfo item = random.nextInt(100) < chance ? usefulItem
                    : junkCategory >= 0 && random.nextBoolean() ? JunkLoot.pick(junkCategory, random) : null;
            finds.put(key, item);
            GameIO.checkpoint();
        }
        ItemInfo item = finds.get(key);
        if (item == null) {
            GameIO.out().println("Здесь уже всё забрали. Ничего полезного не осталось.");
            return true;
        }
        if (!player.addItem(item)) {
            GameIO.out().println("В рюкзаке нет места. Находка останется здесь — можно вернуться позже.");
            return false;
        }
        GameIO.sound(GameSound.BAG);
        GameIO.out().println("Найдено: " + item.getName() + " · " + item.getPrice() + " $");
        finds.remove(key);
        return true;
    }
}
