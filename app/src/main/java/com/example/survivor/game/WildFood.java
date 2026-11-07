package com.example.survivor.game;

/** A collected mushroom may be harmful; its effect is fixed when found and saved. */
final class WildFood extends Food {
    private static final long serialVersionUID=1L;
    private final boolean poisonous;
    WildFood(boolean mushroom,boolean poisonous){
        super(mushroom?"Собранные грибы":"Лесные ягоды",mushroom?"Собранные в лесу грибы. Среди них могут оказаться опасные.":"Небольшая порция лесных ягод.",1,0,mushroom?18:12);
        this.poisonous=mushroom&&poisonous;
    }
    boolean poisonous(){return poisonous;}
}
