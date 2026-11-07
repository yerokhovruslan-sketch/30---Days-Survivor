package com.example.survivor.game;

/** Gameplay events, independent of Android audio playback. */
public enum GameSound {
    CLICK, TRADE, RELOAD, SHOT, BLUNT, BLADE, ZOMBIE, WALK, ERROR, QUEST, SAVE, SEARCH, BAG, WOOD_CONTAINER, METAL_CONTAINER, PAPER, WOOD_DOOR, METAL_DOOR, CONCRETE_STEPS, EARTH_STEPS, SCREAM, ZOMBIE_DEATH, SNORT, HURT;
    static GameSound weapon(WeaponStyle style) {
        return switch(style) {
            case SHOTGUN,REVOLVER,PISTOL,CARBINE -> SHOT;
            case KNIFE,AXE,LONG,BLADE,SCREWDRIVER,HATCHET,SAW,FIRE_AXE,HUNTING_KNIFE,MACHETE,KATANA,CROSSBOW -> BLADE;
            default -> BLUNT;
        };
    }
}
