package com.example.survivor;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class SearchNarrativeTest {
 @Test public void weightedSelectionAndNoImmediateRepeats() {
  SearchNarrative n=new SearchNarrative(Arrays.asList("general\tТихо.","outdoor\tВетер.","indoor\tСырость."));
  Random r=new Random(123);int ordinary=0;String previous="";
  for(int i=0;i<10000;i++){String s=n.choose(r,2,"Лесная опушка");if(SearchNarrative.ORDINARY.contains(s))ordinary++;assertNotEquals(previous,s);previous=s;}
  assertTrue(ordinary>6800&&ordinary<7200);
 }
 @Test public void surroundingsAndSubroomsAreFiltered(){
  SearchNarrative n=new SearchNarrative(Arrays.asList("general\tТихо.","outdoor\tВетер.","indoor\tСырость.","medical\tБинты.","residential\tИгрушка.","industrial\tПровода."));
  assertTrue(n.atmospheric(2,"Лесная опушка").contains("Ветер."));
  assertFalse(n.atmospheric(2,"Лесная опушка").contains("Игрушка."));
  assertTrue(n.atmospheric(2,"Подвал").contains("Сырость."));
  assertFalse(n.atmospheric(2,"Подвал").contains("Ветер."));
  assertTrue(n.atmospheric(12,"Перевязочная").contains("Бинты."));
  assertFalse(n.atmospheric(12,"Перевязочная").contains("Игрушка."));
  assertTrue(n.atmospheric(1,"Детская площадка").contains("Игрушка."));
 }
}
