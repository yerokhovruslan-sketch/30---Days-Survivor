package com.example.survivor.game;

import org.junit.Test;
import java.io.*;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class EquipmentStateTest {
    @Test public void helmetAndVestUseIndependentSlotsAndBothProtectAndWear() {
        Fighter player=new Fighter("John","",100);
        ItemInfo vest=Shop.createItem(21),helmet=Shop.createItem(22);
        player.addItem(vest);player.addItem(helmet);
        assertTrue(player.equipArmor(helmet));assertTrue(player.equipArmor(vest));
        assertSame(vest,player.getEquippedArmor());assertSame(helmet,player.getEquippedHelmet());
        assertEquals(52,player.getArmorReduction());
        player.takeDamage(50);assertEquals(76,player.getHealth());
        assertEquals(90,vest.getDurability());assertEquals(90,helmet.getDurability());
        player.removeBodyArmor();assertSame(helmet,player.getEquippedHelmet());assertNull(player.getEquippedArmor());
        assertEquals(18,player.getArmorReduction());player.equipArmor(vest);player.removeHelmet();
        assertSame(vest,player.getEquippedArmor());assertNull(player.getEquippedHelmet());
    }
    @Test public void storingHelmetDoesNotUnequipBodyAndBothSlotsSurviveSaving() throws Exception {
        Fighter player=new Fighter("John","",100);ItemInfo vest=Shop.createItem(21),helmet=Shop.createItem(22);
        player.addItem(vest);player.addItem(helmet);player.equipArmor(vest);player.equipArmor(helmet);
        File file=Files.createTempFile("two-slots",".dat").toFile();
        try {
            GameSave.save(file,player,1,new Locations());Fighter restored=(Fighter)GameSave.load(file).player;
            assertEquals("Защитный жилет",restored.getEquippedArmor().getName());
            assertEquals("Боевой шлем",restored.getEquippedHelmet().getName());
            assertEquals(52,restored.getArmorReduction());
            assertTrue(Shelter.deposit(restored,restored.getInventory().indexOf(restored.getEquippedHelmet())));
            assertNull(restored.getEquippedHelmet());assertNotNull(restored.getEquippedArmor());assertEquals(40,restored.getArmorReduction());
        } finally {file.delete();}
    }
    @Test public void oldSingleSlotHelmetMigratesOnLoadWithoutLosingItem() throws Exception {
        Fighter player=new Fighter("John","",100);ItemInfo helmet=Shop.createItem(22);player.addItem(helmet);
        java.lang.reflect.Field oldSlot=Fighter.class.getDeclaredField("equippedArmor");oldSlot.setAccessible(true);oldSlot.set(player,helmet);
        File file=Files.createTempFile("old-helmet-slot",".dat").toFile();
        try {
            try(ObjectOutputStream output=new ObjectOutputStream(new FileOutputStream(file))) {output.writeObject(new GameSave(player,5,new Locations()));}
            Fighter restored=(Fighter)GameSave.load(file).player;
            assertNull(restored.getEquippedArmor());assertNotNull(restored.getEquippedHelmet());
            assertTrue(restored.getInventory().contains(restored.getEquippedHelmet()));assertEquals(20,restored.getArmorReduction());
        } finally {file.delete();}
    }
}
