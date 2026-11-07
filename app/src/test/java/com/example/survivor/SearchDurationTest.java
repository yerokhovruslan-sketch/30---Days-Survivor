package com.example.survivor;
import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.*;
public class SearchDurationTest {
 @Test public void totalDurationAlwaysBetweenFourAndNineSeconds(){
  Random rng=new Random(27);for(int i=0;i<20000;i++){int total=SearchDuration.holdMillis(rng)+SearchDuration.FADE_OUT_MS;assertTrue(total>=4000&&total<=9000);}
 }
}
