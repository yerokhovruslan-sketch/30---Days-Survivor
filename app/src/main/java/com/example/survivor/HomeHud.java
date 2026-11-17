package com.example.survivor;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.survivor.game.GameSession;

/** Home-only status card, using the supplied artwork and live game values. */
public final class HomeHud extends LinearLayout {
    private static final int TEXT=0xffc7b9a9, MUTED=0xffb0a69b;
    private final TextView clock, balance, deadline;
    private final Metric stamina, sleep, hunger, noise, temperature;
    private final Bitmap icons;
    private final Bitmap[] clockIcons = new Bitmap[4];
    public HomeHud(Context c, AttributeSet attrs) {
        super(c,attrs);setOrientation(VERTICAL);setPadding(dp(23),dp(20),dp(23),dp(20));
        GradientDrawable background=new GradientDrawable();
        background.setColor(0xff1b1b1b);
        background.setCornerRadius(dp(10));
        background.setStroke(dp(1),0xff494949);
        setBackground(background);
        icons=BitmapFactory.decodeResource(getResources(),R.drawable.home_icon_sheet);
        for(int i=0;i<clockIcons.length;i++)clockIcons[i]=icon(i+5);
        LinearLayout top=row();top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=label("Джон",32,true);top.addView(name,new LayoutParams(0,-2,1));
        balance=label("Баланс: 0 $",16,false);balance.setPadding(dp(10),dp(5),dp(10),dp(5));
        GradientDrawable badge=new GradientDrawable();badge.setColor(0x582d2823);badge.setCornerRadius(dp(6));badge.setStroke(dp(1),0xff534a41);balance.setBackground(badge);top.addView(balance);addView(top);
        clock=label("День 1 · 08:00 · Выживание · День",15,false);clock.setTextColor(MUTED);clock.setPadding(0,dp(1),0,dp(4));addView(clock);
        deadline=label("",13,false);deadline.setTextColor(MUTED);deadline.setPadding(0,0,0,dp(8));addView(deadline);
        line();
        stamina=metric("Выносливость",0,100,true);addView(stamina.container);
        line();
        sleep=metric("Сонливость",1,100,false);hunger=metric("Голод",2,100,false);
        noise=metric("Шум",4,60,false);temperature=metric("Температура",3,100,false);
        LinearLayout first=row();first.addView(sleep.container,new LayoutParams(0,-2,1));first.addView(hunger.container,new LayoutParams(0,-2,1));addView(first);
        LinearLayout second=row();second.addView(noise.container,new LayoutParams(0,-2,1));second.addView(temperature.container,new LayoutParams(0,-2,1));addView(second);
        temperature.value.setText("—");
    }
    public void onClockClick(OnClickListener listener){clock.setOnClickListener(listener);}
    public void update(GameSession.Frame f){
        int phase=HudTime.phase(f.time);
        String value="\ufffc День "+f.day+" · "+f.time+" · "+f.difficulty+" · \ufffc "+HudTime.label(phase);
        SpannableString decorated=new SpannableString(value);
        applyIcon(decorated,0,clockIcons[0]);
        applyIcon(decorated,value.lastIndexOf('\ufffc'),clockIcons[phase+1]);
        clock.setText(decorated);
        int hour=0,minute=0;try{String[] parts=f.time.split(":");hour=Integer.parseInt(parts[0]);minute=Integer.parseInt(parts[1]);}catch(RuntimeException ignored){}
        int left=Math.max(0,(31-f.day)*1440-hour*60-minute);
        deadline.setText(left>0?"До 31-го дня: "+(left/1440)+" дн. "+((left%1440)/60)+" ч.":"31-й день наступил");
        balance.setText("Баланс: "+f.balance+" $");
        stamina.set(f.stamina,100);sleep.set(f.sleepiness,100);hunger.set(f.hunger,100);noise.set(f.noise,60);
        temperature.value.setText(f.status.temperature);
        temperature.bar.setProgress("Холодно".equals(f.status.temperature) ? 100 : "Зябко".equals(f.status.temperature) ? 50 : 0);
    }
    private Metric metric(String title,int icon,int max,boolean large){
        LinearLayout outer=row();outer.setGravity(Gravity.CENTER_VERTICAL);outer.setPadding(0,dp(large?15:12),dp(large?0:7),dp(large?15:8));
        ImageView picture=new ImageView(getContext()); Bitmap iconBitmap=icon(icon);if(iconBitmap!=null)picture.setImageBitmap(iconBitmap);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int iconSize=large?29:icon==4?30:24;
        LayoutParams ip=new LayoutParams(dp(iconSize),dp(iconSize));ip.setMarginEnd(dp(9));outer.addView(picture,ip);
        LinearLayout column=new LinearLayout(getContext());column.setOrientation(VERTICAL);outer.addView(column,new LayoutParams(0,-2,1));
        LinearLayout caption=row();TextView captionText=label(title,large?17:14,large);caption.addView(captionText,new LayoutParams(0,-2,1));
        TextView value=label("0/"+max,large?17:14,false);value.setGravity(Gravity.END);caption.addView(value);column.addView(caption);
        ProgressBar progress=new ProgressBar(getContext(),null,android.R.attr.progressBarStyleHorizontal);progress.setMax(max);
        GradientDrawable track=shape(0xff302d2a);track.setStroke(dp(1),0xff514a42);
        GradientDrawable fill=shape(large?0xff7e9faa:0xff9c9284);
        LayerDrawable layers=new LayerDrawable(new android.graphics.drawable.Drawable[]{track,new ClipDrawable(fill,Gravity.LEFT,ClipDrawable.HORIZONTAL)});
        layers.setId(0,android.R.id.background);layers.setId(1,android.R.id.progress);progress.setProgressDrawable(layers);
        LayoutParams bp=new LayoutParams(-1,dp(large?8:6));bp.topMargin=dp(4);column.addView(progress,bp);return new Metric(outer,value,progress);
    }
    private GradientDrawable shape(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(5));return d;}
    private void line(){View v=new View(getContext());v.setBackgroundColor(0xff544b42);addView(v,new LayoutParams(-1,dp(1)));}
    private LinearLayout row(){LinearLayout r=new LinearLayout(getContext());r.setOrientation(HORIZONTAL);return r;}
    private TextView label(String s,int size,boolean bold){TextView t=new TextView(getContext());t.setText(s);t.setTextSize(size);t.setTextColor(TEXT);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create("sans-serif-condensed",bold?Typeface.BOLD:Typeface.NORMAL));return t;}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void applyIcon(SpannableString text,int offset,Bitmap bitmap){
        if(bitmap==null)return;
        BitmapDrawable drawable=new BitmapDrawable(getResources(),bitmap);
        drawable.setBounds(0,0,dp(15),dp(15));
        text.setSpan(new ImageSpan(drawable,ImageSpan.ALIGN_BASELINE),offset,offset+1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    private static Bitmap trim(Bitmap source){if(source==null)return null;int left=source.getWidth(),top=source.getHeight(),right=0,bottom=0;for(int y=0;y<source.getHeight();y+=2)for(int x=0;x<source.getWidth();x+=2)if(Color.alpha(source.getPixel(x,y))>20){left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}return right>left&&bottom>top?Bitmap.createBitmap(source,left,top,Math.min(source.getWidth()-left,right-left+2),Math.min(source.getHeight()-top,bottom-top+2)):source;}
    private Bitmap icon(int index){
        if(icons==null)return null;
        int[][] rect={{100,0,550,390},{550,50,960,380},{960,0,1360,380},{1380,0,1660,390},{1660,20,2150,380},
                {320,395,660,724},{720,395,1050,724},{1130,395,1465,724},{1550,395,1850,724}};
        int[] r=rect[index];int x=r[0],y=r[1],w=Math.min(r[2],icons.getWidth())-x,h=Math.min(r[3],icons.getHeight())-y;
        Bitmap part=Bitmap.createBitmap(icons,x,y,w,h).copy(Bitmap.Config.ARGB_8888,true);int[] px=new int[w*h];part.getPixels(px,0,w,0,0,w,h);
        for(int i=0;i<px.length;i++){int c=px[i],red=Color.red(c),green=Color.green(c),blue=Color.blue(c);if((red>140&&red>green*1.45&&red>blue*1.5)||(red>165&&green>150&&blue<100))px[i]=Color.TRANSPARENT;}
        part.setPixels(px,0,w,0,0,w,h);return trim(part);
    }
    private static final class Metric {final LinearLayout container;final TextView value;final ProgressBar bar;Metric(LinearLayout c,TextView v,ProgressBar p){container=c;value=v;bar=p;}void set(int n,int max){value.setText(n+"/"+max);bar.setProgress(Math.max(0,Math.min(n,max)));}}
}
