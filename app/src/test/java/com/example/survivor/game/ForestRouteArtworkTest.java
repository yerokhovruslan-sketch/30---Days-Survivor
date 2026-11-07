package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
public class ForestRouteArtworkTest {
 @Test public void mapsEachForestLocationToItsOwnTile(){
  Locations l=new Locations();int[] ids={2,19,20,21,22,23,24,25,26,27};
  String[] names={"Лесная опушка","Лесная тропа","Охотничья избушка","Заброшенный кемпинг","Берег озера","Лодочная станция","Рыбацкий остров","Болотная тропа","Лесничество","Лесопилка"};
  for(int i=0;i<ids.length;i++){assertEquals(names[i],l.getLocation(ids[i]).getName());assertEquals("route_"+ids[i],LocationPreview.artwork(ids[i]));}
 }
}
