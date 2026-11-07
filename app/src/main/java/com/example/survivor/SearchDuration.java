package com.example.survivor;
import java.util.Random;
/** Includes the 450 ms fade back to the result in the total duration. */
final class SearchDuration {
 static final int FADE_OUT_MS=450;
 static int holdMillis(Random random){return 4000-FADE_OUT_MS+random.nextInt(5001);}
}
