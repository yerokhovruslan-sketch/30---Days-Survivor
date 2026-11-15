package com.example.survivor;
import org.junit.Test;
import static org.junit.Assert.*;
public class HudTimeTest {
 @Test public void changesAtMorningEveningAndNight() {
  assertEquals("Ночь",HudTime.label(HudTime.phase("05:59")));
  assertEquals("День",HudTime.label(HudTime.phase("06:00")));
  assertEquals("День",HudTime.label(HudTime.phase("17:59")));
  assertEquals("Вечер",HudTime.label(HudTime.phase("18:00")));
  assertEquals("Вечер",HudTime.label(HudTime.phase("19:59")));
  assertEquals("Ночь",HudTime.label(HudTime.phase("20:00")));
  assertEquals("Ночь",HudTime.label(HudTime.phase("23:59")));
 }
}
