package com.example.survivor.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** A trophy's sale value is rolled once, when the corpse is looted. */
final class ZombieEar extends ItemInfo {
    private static final long serialVersionUID = 1L;
    ZombieEar(ZombieType type, int value) {
        super("Ухо: " + type.label, "Трофей заражённого. Можно продать торговцу.", 1, value);
    }
    @Override public int getWeightGrams() { return 20; }
    @Override public int getSalePrice() { return rawPrice(); }
    @Override public int getPrice() { return rawPrice(); }

    static List<ItemInfo> roll(ZombieType type, Random random) {
        int min, max;
        switch (type) {
            case NORMAL -> { min=4; max=12; }
            case RUNNER -> { min=10; max=19; }
            case SCREAMER -> { min=15; max=25; }
            case BRUTE -> { min=60; max=80; }
            case HUNTER -> { min=80; max=120; }
            case JUMPER -> { min=20; max=40; }
            default -> throw new IllegalArgumentException("Unknown zombie type");
        }
        int count=random.nextInt(100)<40?2:1;
        List<ItemInfo> result=new ArrayList<>();
        for(int i=0;i<count;i++)result.add(new ZombieEar(type,min+random.nextInt(max-min+1)));
        return result;
    }
}
