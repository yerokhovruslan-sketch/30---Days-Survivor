package com.example.survivor;

import org.junit.Test;
import static org.junit.Assert.*;

public class OpeningTimingTest {
 @Test public void quickAndLongChoicesUseRequestedDurations(){
  assertEquals(10000,OpeningTiming.duration("opening_door"));
  assertEquals(35000,OpeningTiming.duration("opening_wounded"));
 }
}
