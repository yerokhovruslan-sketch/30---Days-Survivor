package com.example.survivor;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
/** Gentle color and shadow lift, applied only when drawing location artwork. */
final class ArtworkTone {
 private static final ColorMatrixColorFilter LOCATION=create();
 private static ColorMatrixColorFilter create(){
  ColorMatrix matrix=new ColorMatrix();matrix.setSaturation(1.15f);
  matrix.postConcat(new ColorMatrix(new float[]{1.03f,0,0,0,3, 0,1.03f,0,0,3, 0,0,1.03f,0,3, 0,0,0,1,0}));
  return new ColorMatrixColorFilter(matrix);
 }
 static ColorMatrixColorFilter location(){return LOCATION;}
}
