package com.example.survivor;
import java.util.Random;
/** Includes the 450 ms fade back to the result in the total duration. */
final class SearchDuration {
 static final int FADE_OUT_MS=450;
 static int holdMillis(Random random){return 2000-FADE_OUT_MS+random.nextInt(1001);}
 static int holdMillis(Random random,String phrase){if(SearchNarrative.ORDINARY.contains(phrase))return holdMillis(random);return Math.max(4000-FADE_OUT_MS, Math.min(11000-FADE_OUT_MS, 1200+phrase.length()*48));}
}
