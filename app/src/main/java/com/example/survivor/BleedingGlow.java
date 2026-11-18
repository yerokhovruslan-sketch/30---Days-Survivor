package com.example.survivor;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/** Non-interactive, softly pulsing edge glow while a wound is bleeding. */
final class BleedingGlow extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator pulse;
    private boolean active, foreground;
    BleedingGlow(Context context){super(context);setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setVisibility(GONE);}
    void showBleeding(boolean value){active=value;sync();}
    void foreground(boolean value){foreground=value;sync();}
    private void sync(){
        if(!active || !foreground){if(pulse!=null){pulse.cancel();pulse=null;}setVisibility(GONE);return;}
        if(pulse!=null)return;
        setVisibility(VISIBLE);pulse=ValueAnimator.ofFloat(.35f,.8f);pulse.setDuration(1800);
        pulse.setInterpolator(new AccelerateDecelerateInterpolator());pulse.setRepeatMode(ValueAnimator.REVERSE);pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.addUpdateListener(a->setAlpha((float)a.getAnimatedValue()));pulse.start();
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float w=getWidth(),h=getHeight(),x=w*.16f,y=h*.10f;
        int edge=Color.argb(110,105,4,20),clear=Color.TRANSPARENT;
        paint.setShader(new LinearGradient(0,0,x,0,edge,clear,Shader.TileMode.CLAMP));canvas.drawRect(0,0,x,h,paint);
        paint.setShader(new LinearGradient(w,0,w-x,0,edge,clear,Shader.TileMode.CLAMP));canvas.drawRect(w-x,0,w,h,paint);
        paint.setShader(new LinearGradient(0,0,0,y,edge,clear,Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,y,paint);
        paint.setShader(new LinearGradient(0,h,0,h-y,edge,clear,Shader.TileMode.CLAMP));canvas.drawRect(0,h-y,w,h,paint);
        paint.setShader(null);
    }
    @Override protected void onDetachedFromWindow(){foreground=false;sync();super.onDetachedFromWindow();}
}
