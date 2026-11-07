package com.example.survivor;

import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.LruCache;
import android.widget.Button;
import androidx.core.view.ViewCompat;

/** Real text above a cropped picture, with the same alignment on every menu tile. */
final class MenuArtwork {
    private static final LruCache<Integer,Bitmap> cache=new LruCache<Integer,Bitmap>(8*1024) {
        @Override protected int sizeOf(Integer key,Bitmap image) {return Math.max(1,image.getByteCount()/1024);}
    };
    static int forButton(int id) {
        if(id==R.id.action_status)return R.drawable.menu_art_character;
        if(id==R.id.action_explore)return R.drawable.menu_art_expedition;
        if(id==R.id.action_inventory)return R.drawable.menu_art_inventory;
        if(id==R.id.action_shop)return R.drawable.menu_art_shop;
        if(id==R.id.action_heal)return R.drawable.menu_art_healing;
        if(id==R.id.action_equipment)return R.drawable.menu_art_equipment;
        if(id==R.id.action_rest)return R.drawable.menu_art_rest;
        if(id==R.id.action_notes)return R.drawable.menu_art_notes;
        if(id==R.id.action_eat)return R.drawable.menu_art_eat;
        if(id==R.id.action_sleep)return R.drawable.menu_art_sleep;
        if(id==R.id.action_save)return R.drawable.menu_art_save;
        if(id==R.id.action_load)return R.drawable.menu_art_load;
        if(id==R.id.action_unload)return R.drawable.menu_art_unload;
        if(id==R.id.action_repair)return R.drawable.menu_art_repair;
        if(id==R.id.action_armor)return R.drawable.menu_art_armor;
        if(id==R.id.action_journal)return R.drawable.menu_art_journal;
        if(id==R.id.action_shelter)return R.drawable.menu_art_shelter;
        return 0;
    }
    static int route(String key) {
        return switch(key) {
            case "district_0" -> R.drawable.district_0;
            case "district_1" -> R.drawable.district_1;
            case "district_2" -> R.drawable.district_2;
            case "district_3" -> R.drawable.district_3;
            case "district_4" -> R.drawable.district_4;
            case "district_5" -> R.drawable.district_5;
            case "district_6" -> R.drawable.district_6;
            case "route_1" -> R.drawable.route_1;
            case "route_2" -> R.drawable.route_2;
            case "route_19" -> R.drawable.route_19;
            case "route_20" -> R.drawable.route_20;
            case "route_21" -> R.drawable.route_21;
            case "route_22" -> R.drawable.route_22;
            case "route_23" -> R.drawable.route_23;
            case "route_24" -> R.drawable.route_24;
            case "route_25" -> R.drawable.route_25;
            case "route_26" -> R.drawable.route_26;
            case "route_27" -> R.drawable.route_27;
            case "route_28" -> R.drawable.route_28;
            case "route_29" -> R.drawable.route_29;
            case "route_30" -> R.drawable.route_30;
            case "route_31" -> R.drawable.route_31;
            case "route_32" -> R.drawable.route_32;
            case "route_41" -> R.drawable.route_41;
            case "route_42" -> R.drawable.route_42;
            case "route_43" -> R.drawable.route_43;
            case "route_44" -> R.drawable.route_44;
            case "route_45" -> R.drawable.route_45;
            case "route_46" -> R.drawable.route_46;
            case "route_47" -> R.drawable.route_47;
            case "route_48" -> R.drawable.route_48;
            case "route_49" -> R.drawable.route_49;
            case "route_50" -> R.drawable.route_50;
            case "route_4" -> R.drawable.route_4;
            case "route_5" -> R.drawable.route_5;
            case "route_6" -> R.drawable.route_6;
            case "route_7" -> R.drawable.route_7;
            case "route_8" -> R.drawable.route_8;
            case "route_9" -> R.drawable.route_9;
            case "route_10" -> R.drawable.route_10;
            case "route_11" -> R.drawable.route_11;
            case "route_12" -> R.drawable.route_12;
            case "route_13" -> R.drawable.route_13;
            case "route_14" -> R.drawable.route_14;
            case "route_15" -> R.drawable.route_15;
            case "route_16" -> R.drawable.route_16;
            case "route_17" -> R.drawable.route_17;
            case "route_18" -> R.drawable.route_18;
            default -> 0;
        };
    }
    static void apply(Button button,int resource) {
        if(resource==0)return;
        Bitmap image=cache.get(resource);
        if(image==null) {image=BitmapFactory.decodeResource(button.getResources(),resource);cache.put(resource,image);}
        float density=button.getResources().getDisplayMetrics().density;
        GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(7*density);
        float focus=resource==R.drawable.menu_art_equipment || resource==R.drawable.menu_art_armor ? .96f
                :resource==R.drawable.menu_art_character?.86f:resource==R.drawable.menu_art_notes?.82f:.72f;
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30ffffff),new Picture(image,7*density,focus),mask));
        ViewCompat.setBackgroundTintList(button,null);
        button.setCompoundDrawablesRelative(null,null,null,null);
        button.setTextColor(0xffeeeeee);
        button.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));
        button.setShadowLayer(3*density,0,density,Color.BLACK);
        button.setPadding(Math.round(12*density),Math.round(12*density),Math.round(12*density),Math.round(12*density));
    }
    private static final class Picture extends Drawable {
        final Bitmap image;
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
        final float radius;
        final float focus;
        final ColorMatrixColorFilter tone;
        Picture(Bitmap image,float radius,float focus) {
            this.image=image;this.radius=radius;this.focus=focus;
            ColorMatrix monochrome=new ColorMatrix();monochrome.setSaturation(.80f);
            monochrome.postConcat(new ColorMatrix(new float[]{
                    1.08f,0,0,0,9, 0,1.08f,0,0,9, 0,0,1.08f,0,9, 0,0,0,1,0}));
            tone=new ColorMatrixColorFilter(monochrome);
        }
        @Override public void draw(Canvas canvas) {
            RectF bounds=new RectF(getBounds());if(bounds.isEmpty())return;
            int restore=canvas.save();Path clip=new Path();clip.addRoundRect(bounds,radius,radius,Path.Direction.CW);canvas.clipPath(clip);
            float scale=Math.max(bounds.width()/image.getWidth(),bounds.height()/image.getHeight());
            float w=image.getWidth()*scale,h=image.getHeight()*scale;
            // Subject is on the right in these banners; keep it visible on narrow phones.
            float left=bounds.left+(bounds.width()-w)*focus,top=bounds.top+(bounds.height()-h)*.5f;
            paint.setShader(null);paint.setStyle(Paint.Style.FILL);
            paint.setColorFilter(tone);
            canvas.drawBitmap(image,null,new RectF(left,top,left+w,top+h),paint);
            paint.setColorFilter(null);
            paint.setShader(new LinearGradient(bounds.left,0,bounds.right,0,
                    new int[]{0xb0000000,0x68000000,0x28000000},new float[]{0f,.6f,1f},Shader.TileMode.CLAMP));
            canvas.drawRect(bounds,paint);paint.setShader(null);
            paint.setColor(0xff454545);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);
            canvas.drawRoundRect(new RectF(bounds.left+.5f,bounds.top+.5f,bounds.right-.5f,bounds.bottom-.5f),radius,radius,paint);
            paint.setStyle(Paint.Style.FILL);canvas.restoreToCount(restore);
        }
        @Override public void setAlpha(int alpha) {paint.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter) {paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity() {return PixelFormat.TRANSLUCENT;}
    }
}
