package com.example.survivor.game;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.util.Scanner;
import static org.junit.Assert.*;

public class HomeSurvivorsTest {
 @Test public void invitationAndCooldownSurviveSave() throws Exception {
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);
  assertFalse(w.homeSurvivors().invite(HomeSurvivors.SEMEN,w,new Scanner("0")));
  assertTrue(w.homeSurvivors().invite(HomeSurvivors.SEMEN,w,new Scanner("1")));
  assertEquals(w.absoluteMinute()+1440,w.homeSurvivors().nextTalk[HomeSurvivors.SEMEN]);
  File f=Files.createTempFile("home-people", ".dat").toFile();try{GameSave.save(f,p,0,l);HomeSurvivors h=GameSave.load(f).locations.getStories().homeSurvivors();assertTrue(h.atHome(HomeSurvivors.SEMEN));assertTrue(h.travelling[HomeSurvivors.SEMEN]);}finally{f.delete();}
 }
 @Test public void alternateEndingRequiresCoordinatesRadioAndSupplies(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);HomeSurvivors h=w.homeSurvivors();
  assertFalse(h.ready(p,w));w.warehouseFinale().registryFound=true;p.getUpgrades().radio=1;h.radio(p,w,new Scanner("0"));assertTrue(h.radioConfirmed);assertFalse(h.ready(p,w));
  p.getStorage().add(Shop.createItem(16));p.getStorage().add(Shop.createItem(16));p.getStorage().add(ResidentialStories.waterBottle());p.getStorage().add(ResidentialStories.waterBottle());p.getInventory().add(Shop.createItem(1));p.getStorage().add(Shop.createItem(19));
  assertTrue(h.ready(p,w));assertFalse(h.depart(p,w,new Scanner("0")));assertEquals(5,p.getStorage().size());
  assertTrue(h.depart(p,w,new Scanner("1 0")));assertTrue(h.ownLandEnding);assertTrue(p.getStorage().isEmpty());assertFalse(p.getInventory().stream().anyMatch(i->i instanceof MedicalItem));
 }
 @Test public void partyNeedsMoreSuppliesAndLateFlightIsNotAVoluntaryChoice(){
  Locations l=new Locations();WorldStories w=l.getStories();Fighter p=new Fighter("John","",100);HomeSurvivors h=w.homeSurvivors();w.warehouseFinale().registryFound=true;h.radioConfirmed=true;
  h.home[HomeSurvivors.MARINA]=true;h.travelling[HomeSurvivors.MARINA]=true;assertEquals(3,h.people());assertTrue(h.requirements(p).contains("еда ×6"));
  w.advanceMinutes(30*1440);assertFalse(h.ready(p,w));
 }
}
