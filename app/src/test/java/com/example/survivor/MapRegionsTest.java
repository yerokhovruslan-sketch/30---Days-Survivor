package com.example.survivor;

import org.junit.Test;
import static org.junit.Assert.*;

public class MapRegionsTest {
 @Test public void regionsCoverMapWithoutOverlappingInteriors(){
  for(int y=0;y<197;y++)for(int x=0;x<199;x++){
   float nx=(x+.317f)/199,ny=(y+.419f)/197;int count=0;
   for(int i=0;i<MapRegions.SHAPES.length;i++)if(MapRegions.contains(i,nx,ny))count++;
   assertEquals("Map point "+nx+", "+ny,1,count);
  }
 }
 @Test public void outsideMapIsNotSelectable(){assertEquals(-1,MapRegions.at(-.1f,.5f));assertEquals(-1,MapRegions.at(.5f,1.1f));}
}
