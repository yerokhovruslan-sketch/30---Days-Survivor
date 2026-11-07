package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
public class MountainRouteArtworkTest {
 @Test public void assignsEachImageToTheSpecifiedLocation(){
  Locations l=new Locations();String[] names={"Лесопилка","Каменный карьер","Шахтёрский посёлок","Старая шахта","Подземный склад","Горная станция связи"};
  for(int i=0;i<names.length;i++){int number=27+i;assertEquals(names[i],l.getLocation(number).getName());assertEquals("route_"+number,LocationPreview.artwork(number));}
 }
}
