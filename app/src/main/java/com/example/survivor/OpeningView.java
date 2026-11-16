package com.example.survivor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.content.res.ColorStateList;
import com.example.survivor.game.GameSession;
import java.util.function.IntConsumer;

/** Full-screen opening and main title; game logic stays on the session worker. */
final class OpeningView extends FrameLayout {
 private final ImageView background;private final View pulse;private final TextView title,body,timer;private final LinearLayout buttons;private final IntConsumer choose;
 private boolean instantNextScene;
 private long revision=-1;private String scene="";private final ProgressBar progress;private final View bottomSpacer;private final TitleMenuView titleMenu;private int pulseMillis=600;private boolean submitted;
 OpeningView(Context c,IntConsumer choose){super(c);this.choose=choose;setClickable(true);setFocusable(true);setVisibility(GONE);setBackgroundColor(Color.BLACK);
  background=new ImageView(c);background.setScaleType(ImageView.ScaleType.CENTER_CROP);addView(background,new LayoutParams(-1,-1));
  View shade=new View(c);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x88000000,0x33000000,0xf2000000}));addView(shade,new LayoutParams(-1,-1));
  FrameLayout glow=new FrameLayout(c);View vertical=new View(c);vertical.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x779d1515,0x00100000,0x779d1515}));glow.addView(vertical,new LayoutParams(-1,-1));View horizontal=new View(c);horizontal.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0x889d1515,0x00100000,0x889d1515}));glow.addView(horizontal,new LayoutParams(-1,-1));pulse=glow;pulse.setVisibility(GONE);addView(pulse,new LayoutParams(-1,-1));
  LinearLayout content=new LinearLayout(c);content.setOrientation(LinearLayout.VERTICAL);int pad=dp(22);content.setPadding(pad,dp(18),pad,dp(18));addView(content,new LayoutParams(-1,-1));
  timer=new TextView(c);timer.setTextColor(0xffffcccc);timer.setTextSize(27);timer.setGravity(Gravity.CENTER);timer.setTypeface(Typeface.DEFAULT,Typeface.BOLD);content.addView(timer,new LinearLayout.LayoutParams(-1,-2));
  progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setProgressTintList(ColorStateList.valueOf(0xffce7770));progress.setProgressBackgroundTintList(ColorStateList.valueOf(0x665f4542));LinearLayout.LayoutParams bar=new LinearLayout.LayoutParams(-1,dp(3));bar.bottomMargin=dp(12);content.addView(progress,bar);
  title=new TextView(c);title.setTextColor(0xffede7df);title.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));title.setTextSize(30);content.addView(title,new LinearLayout.LayoutParams(-1,-2));
  ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);body=new TextView(c);body.setTextColor(Color.WHITE);body.setTextSize(19);body.setLineSpacing(dp(4),1.08f);scroll.addView(body);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  buttons=new LinearLayout(c);buttons.setOrientation(LinearLayout.VERTICAL);buttons.setGravity(Gravity.CENTER);content.addView(buttons,new LinearLayout.LayoutParams(-1,-2));bottomSpacer=new View(c);content.addView(bottomSpacer,new LinearLayout.LayoutParams(-1,0));
  titleMenu=new TitleMenuView(c,number->{if(submitted)return;submitted=true;choose.accept(number);});addView(titleMenu,new LayoutParams(-1,-1));titleMenu.setVisibility(GONE);
 }
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 static boolean matches(GameSession.Frame f){return f!=null&&f.mode==GameSession.Mode.CHOICE&&f.menu.scene.startsWith("opening_")&&!f.menu.scene.equals("opening_radio");}
 void show(GameSession.Frame f){
  if(!matches(f)){if(f!=null&&f.mode==GameSession.Mode.BUSY){pulse.animate().cancel();pulse.animate().alpha(0).setDuration(180).start();}if(f!=null&&f.mode!=GameSession.Mode.BUSY){setVisibility(GONE);pulse.animate().cancel();revision=-1;}return;}
  if(revision==f.revision)return;revision=f.revision;scene=f.menu.scene;submitted=false;
  int art=switch(scene){case "opening_start"->R.drawable.opening_start;case "opening_room"->R.drawable.opening_room;case "opening_door"->R.drawable.opening_door;case "opening_wounded"->R.drawable.opening_wounded;case "opening_help"->R.drawable.opening_help;case "opening_street"->R.drawable.opening_street;case "opening_shelter_door"->R.drawable.opening_shelter_door;default->0;};
  background.setVisibility(art==0?GONE:VISIBLE);if(art!=0)background.setImageResource(art);
  boolean start=scene.equals("opening_start");titleMenu.setVisibility(start?VISIBLE:GONE);if(start)titleMenu.show(f);title.setText(start?"SURVIVOR":f.menu.title);title.setTextSize(start?38:28);title.setGravity(start?Gravity.CENTER:Gravity.START);
  body.setText(f.menu.hint);body.setGravity(start||scene.equals("opening_cry")?Gravity.CENTER:Gravity.TOP|Gravity.START);buttons.removeAllViews();
  for(GameSession.Choice choice:f.choices){if(scene.equals("opening_wounded")&&choice.number==0)continue;Button button=new Button(getContext());button.setAllCaps(false);button.setText(choice.label+(choice.detail.isEmpty()?"":"\n"+choice.detail));button.setTextColor(Color.WHITE);button.setTextSize(16);button.setEnabled(choice.enabled);button.setAlpha(choice.enabled?1f:.42f);
   button.setBackground(MatteSurface.create(getContext()));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(choice.detail.isEmpty()?52:65));lp.topMargin=dp(7);buttons.addView(button,lp);button.setOnClickListener(v->{if(submitted)return;submitted=true;button.setEnabled(false);pulse.animate().cancel();pulse.animate().alpha(0).setDuration(180).start();choose.accept(choice.number);});}
  boolean timed=scene.equals("opening_door")||scene.equals("opening_wounded");timer.setVisibility(timed?VISIBLE:GONE);if(timed)timer.setText(String.format(java.util.Locale.ROOT,"00:%02d",OpeningTiming.duration(scene)/1000));progress.setVisibility(timed?VISIBLE:GONE);progress.setProgress(1000);pulseMillis=600;pulse.animate().cancel();pulse.setVisibility(timed?VISIBLE:GONE);if(timed){pulse.setAlpha(.1f);pulse.animate().alpha(.8f).setDuration(pulseMillis).withEndAction(this::pulseBack).start();}
  LinearLayout.LayoutParams spacer=(LinearLayout.LayoutParams)bottomSpacer.getLayoutParams();spacer.height=0;bottomSpacer.setLayoutParams(spacer);if(start||scene.equals("opening_cry"))post(()->{if(!scene.equals("opening_start")&&!scene.equals("opening_cry"))return;LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)bottomSpacer.getLayoutParams();p.height=Math.max(0,Math.round(getHeight()*.25f-buttons.getHeight()/2f)-dp(18));bottomSpacer.setLayoutParams(p);});
  animate().cancel();setVisibility(VISIBLE);if(instantNextScene){setAlpha(1);instantNextScene=false;}else{setAlpha(0);animate().alpha(1).setDuration(350).start();}
 }
 void showNextSceneInstantly(){instantNextScene=true;}
 private void pulseBack(){if(getVisibility()==VISIBLE&&pulse.getVisibility()==VISIBLE)pulse.animate().alpha(.1f).setDuration(pulseMillis).withEndAction(()->{if(getVisibility()==VISIBLE&&pulse.getVisibility()==VISIBLE)pulse.animate().alpha(.8f).setDuration(pulseMillis).withEndAction(this::pulseBack).start();}).start();}
 void seconds(int value){pulseMillis=value<=3?250:value<=6?400:600;if(timer.getVisibility()==VISIBLE)timer.setText(String.format(java.util.Locale.ROOT,"00:%02d",Math.max(0,value)));}
 void remaining(int value){int total=OpeningTiming.duration(scene);progress.setProgress(Math.max(0,Math.min(1000,value*1000/total)));}
}
