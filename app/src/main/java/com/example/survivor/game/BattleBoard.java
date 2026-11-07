package com.example.survivor.game;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/** Immutable board shared with the Android renderer. Coordinates are row-major, zero-based. */
public final class BattleBoard {
    public static final int SIZE = 8;
    public final List<Cell> cells;
    public final int turn, playerCell;
    public int epoch,shotTarget=-1;
    public boolean shotBlocked;
    public boolean woodFloor,grassFloor;
    public List<Effect> effects=Collections.emptyList();
    public static final class Effect {
        public final long id;public final int cell,amount;public final String kind;
        Effect(long id,int cell,int amount,String kind){this.id=id;this.cell=cell;this.amount=amount;this.kind=kind;}
    }
    public final String terrain;
    public final boolean combatActive,storyEncounter;
    public static final class Cell {
        public final int index, unit, obstacle, health, maxHealth;
        public final boolean move, selected, hazard, loot;
        public final String label;
        public int identity=-1,stun,slow,obstacleDamage;
        public int obstaclePart, obstacleSpan=1;
        public boolean obstacleVertical;
        public boolean aimed;
        Cell(int index, int unit, int obstacle, int health, int maxHealth, boolean move,
             boolean selected, boolean hazard, boolean loot, String label) {
            this.index=index; this.unit=unit; this.obstacle=obstacle; this.health=health;
            this.maxHealth=maxHealth; this.move=move; this.selected=selected;
            this.hazard=hazard; this.loot=loot; this.label=label;
        }
    }
    BattleBoard(List<Cell> cells, int turn, int playerCell, String terrain,boolean combatActive,boolean storyEncounter) {
        this.cells=Collections.unmodifiableList(new ArrayList<>(cells));
        this.combatActive=combatActive;this.storyEncounter=storyEncounter;
        this.turn=turn; this.playerCell=playerCell; this.terrain=terrain;
    }
}
