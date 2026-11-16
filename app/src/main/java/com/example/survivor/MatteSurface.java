package com.example.survivor;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import java.util.Random;
/** Subtle grain on translucent grey panels, only for buttons without bespoke artwork. */
final class MatteSurface {
 private static Bitmap grain;
 static Drawable create(Context context){
  float density=context.getResources().getDisplayMetrics().density;
  GradientDrawable base=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xee383838,0xf0252525});base.setCornerRadius(7*density);base.setStroke(Math.max(1,Math.round(density)),0xff777777);
  if(grain==null){grain=Bitmap.createBitmap(48,48,Bitmap.Config.ARGB_8888);Random random=new Random(351);for(int y=0;y<48;y++)for(int x=0;x<48;x++){int a=random.nextInt(9);int rgb=random.nextBoolean()?0xffffff:0;grain.setPixel(x,y,(a<<24)|rgb);}}
  BitmapDrawable texture=new BitmapDrawable(context.getResources(),grain);texture.setTileModeX(Shader.TileMode.REPEAT);texture.setTileModeY(Shader.TileMode.REPEAT);
  return new LayerDrawable(new Drawable[]{base,texture});
 }
}
