package com.example.survivor;
import java.util.Random;
/** Environmental accents in pauses; map flock is deliberately occasional. */
final class AmbientPlan {
 enum Cue { NONE,WIND,NIGHT_TENSION,CROW,FLOCK,FOREST,CITY }
 static Cue choose(Random random,boolean night,boolean forest,boolean city,boolean map){
  int n=random.nextInt(100);
  if(map)return n<30?Cue.FLOCK:Cue.NONE;
  if(night){if(forest && n<25)return Cue.FOREST;if(n<60)return Cue.WIND;if(n<76)return Cue.CROW;if(n<87)return Cue.FLOCK;return Cue.NIGHT_TENSION;}
  if(city && n<30)return Cue.CITY;
  return n<(city?50:25)?Cue.WIND:Cue.NONE;
 }
 static int resource(Cue cue){return switch(cue){case WIND->R.raw.map_wind;case NIGHT_TENSION->R.raw.ambient_night_tension;case CROW->R.raw.ambient_crow_single;case FLOCK->R.raw.ambient_crows_flock;case FOREST->R.raw.ambient_forest_night;case CITY->R.raw.ambient_city_day;default->0;};}
 static float volume(Cue cue){return switch(cue){case WIND->.08f;case CROW,FLOCK->.18f;default->.16f;};}
}
