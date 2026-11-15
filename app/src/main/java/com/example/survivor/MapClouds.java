package com.example.survivor;

/** Independent cloud pieces use map coordinates, so they move naturally with the camera. */
final class MapClouds {
 private MapClouds(){}
 static final float[][] CUTS={{0f,.08f,.38f,.91f},{.28f,.08f,.68f,.92f},{.60f,.05f,1f,.88f},
  {.02f,.14f,.40f,.90f},{.34f,.10f,.72f,.93f},{.65f,.04f,1f,.90f},
  {.03f,.06f,.64f,.63f},{.40f,.48f,.98f,.91f}};
 static final int[] SOURCE={0,0,0,1,1,1,2,2};
 private static final float[] WIDTH={.25f,.39f,.21f,.31f,.22f,.37f,.29f,.19f};
 private static final float[] Y={.08f,.22f,.42f,.55f,.69f,.82f,.32f,.75f};
 private static final int[] ALPHA={76,98,56,85,65,105,72,52};
 private static final int[] TONE={245,220,250,232,210,242,224,250};
 private static final long[] PERIOD={170000,210000,145000,195000,235000,185000,155000,225000};
 static float opacity(float zoom){float t=Math.max(0f,Math.min(1f,(zoom-1f)/1.1f));return 1f-t*t*(3f-2f*t);}
 static int count(){return WIDTH.length;}
 static float width(int index){return WIDTH[index];}
 static int alpha(int index){return ALPHA[index];}
 static int tone(int index){return TONE[index];}
 static float x(int index,long time){float span=1f+width(index);double phase=(time/(double)PERIOD[index]+index/(double)count())%1.0;return (float)(phase*span)-width(index);}
 static float y(int index){return Y[index];}
}
