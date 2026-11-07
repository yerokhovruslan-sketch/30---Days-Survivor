package com.example.survivor.game;
import org.junit.Test;
import static org.junit.Assert.*;
public class IndustrialRouteArtworkTest {
 @Test public void assignsArtworkInRequestedOrder(){
  Locations l=new Locations();String[] names={"Химический завод","Очистные сооружения","Электроподстанция","Промышленный бункер","Военный блокпост","Казармы","Военный склад","Полевой госпиталь","Исследовательский комплекс","Центральная лаборатория"};
  for(int i=0;i<names.length;i++){int number=41+i;assertEquals(names[i],l.getLocation(number).getName());assertEquals("route_"+number,LocationPreview.artwork(number));}
 }
}
