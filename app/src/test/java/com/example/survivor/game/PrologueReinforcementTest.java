package com.example.survivor.game;

import org.junit.Test;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.*;

public class PrologueReinforcementTest {
    @Test public void onlyExplicitlyDisabledBattleRejectsReinforcements() {
        Fighter player=new Fighter("John","",100);
        TacticalBattle battle=new TacticalBattle(player,1,new Random(42),List.of(new Zombie(1,ZombieType.NORMAL),new Zombie(2,ZombieType.NORMAL),new Zombie(3,ZombieType.NORMAL)));
        int initial=battle.units.size();
        assertEquals(3,initial);
        battle.disableReinforcements=true;
        for(int i=0;i<30;i++)battle.reinforce();
        assertEquals(initial,battle.units.size());
        battle.disableReinforcements=false;
        battle.reinforce();
        assertEquals(initial+1,battle.units.size());
    }
}
