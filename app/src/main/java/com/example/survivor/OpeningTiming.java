package com.example.survivor;

/** Shared countdown durations for display and game actions. */
final class OpeningTiming {
 private OpeningTiming(){}
 static int duration(String scene){return "opening_door".equals(scene)?10000:35000;}
}
