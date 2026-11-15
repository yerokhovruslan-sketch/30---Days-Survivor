package com.example.survivor;
import org.junit.Test;
import static org.junit.Assert.*;
public class MapCloudsTest {
 @Test public void fadeIsSmoothAndFullyClearsMapAtCloseZoom(){assertEquals(1f,MapClouds.opacity(1f),.0001);assertTrue(MapClouds.opacity(1.5f)<1);assertTrue(MapClouds.opacity(1.5f)>0);assertEquals(0f,MapClouds.opacity(2.2f),.0001);}
 @Test public void cloudsHaveDifferentSizeAndTransparency(){assertTrue(MapClouds.width(0)!=MapClouds.width(1));assertTrue(MapClouds.alpha(0)!=MapClouds.alpha(1));assertTrue(MapClouds.tone(0)!=MapClouds.tone(1));}
 @Test public void cloudsDriftThroughAndBeyondMapBoundaries(){for(int i=0;i<MapClouds.count();i++){float min=1,max=-1;for(int t=0;t<240000;t+=1000){float x=MapClouds.x(i,t);min=Math.min(min,x);max=Math.max(max,x);}assertTrue(min<0);assertTrue(max>.95f);}}
}
