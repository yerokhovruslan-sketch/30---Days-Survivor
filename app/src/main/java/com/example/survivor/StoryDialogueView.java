package com.example.survivor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.example.survivor.game.GameMenu;
import com.example.survivor.game.GameSession;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Presents a story frame sequentially without sending input until its actual choice. */
final class StoryDialogueView extends FrameLayout {
 private final IntConsumer choose;
 private final LinearLayout column,lines,actions;
 private final ScrollView textScroll,buttonScroll;
 private GameSession.Frame frame;
 private List<String> beats=new ArrayList<>();
 private long revision=-1,restoredRevision=-1;
 private int shown,restoredShown;
 private boolean submitted;
 StoryDialogueView(Context context,IntConsumer choose,Bundle saved){super(context);this.choose=choose;setBackgroundColor(Color.BLACK);setClickable(true);setFocusable(true);setVisibility(GONE);
  if(saved!=null){restoredRevision=saved.getLong("story_revision",-1);restoredShown=saved.getInt("story_shown",0);}
  column=new LinearLayout(context);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(20),dp(14),dp(20),dp(14));addView(column,new LayoutParams(-1,-1));
  textScroll=new ScrollView(context);textScroll.setFillViewport(false);textScroll.setVerticalScrollBarEnabled(true);
  lines=new LinearLayout(context);lines.setOrientation(LinearLayout.VERTICAL);textScroll.addView(lines);column.addView(textScroll,new LinearLayout.LayoutParams(-1,0,1));
  buttonScroll=new ScrollView(context);buttonScroll.setFillViewport(false);actions=new LinearLayout(context);actions.setOrientation(LinearLayout.VERTICAL);buttonScroll.addView(actions);column.addView(buttonScroll,new LinearLayout.LayoutParams(-1,-2));
 }
 private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
 static boolean matches(GameSession.Frame f){
  if(f==null||f.mode!=GameSession.Mode.CHOICE||!f.menu.scene.isEmpty()||f.quantity||f.menu.board!=null)return false;
  if(f.menu.title.equals("Гайд")||f.menu.title.equals("Точки интереса"))return false;
  if(!f.menu.story)return false;
  String text=f.text+"\n"+f.menu.hint;
  for(GameMenu.Card card:f.menu.cards)text+="\n"+card.title+"\n"+card.detail;
  return f.menu.title.equals("Кто-то ещё говорит") || f.menu.title.equals("Неустойчивый контакт") || text.matches("(?s).*(Джон|Семён|Марина|Артём|Кирилл|Виктор|Андрей|Ольга|Выживший|Рейдер)\\s*:.*");
 }
 void showFrame(GameSession.Frame f){
  if(!matches(f)){if(f!=null&&f.mode!=GameSession.Mode.BUSY){setVisibility(GONE);frame=null;revision=-1;}return;}
  if(revision==f.revision){frame=f;return;}
  boolean entering=getVisibility()!=VISIBLE;frame=f;revision=f.revision;submitted=false;beats=new ArrayList<>();
  appendBeats(f.text);appendBeats(f.menu.hint);
  for(GameMenu.Card card:f.menu.cards){
   if(card.detail!=null&&!card.detail.trim().isEmpty())appendBeats(card.detail);
   else if(card.title!=null&&!card.title.trim().isEmpty()&&!card.title.equals(f.menu.title))appendBeats(card.title);
  }
  shown=restoredRevision==revision?Math.max(1,Math.min(restoredShown,beats.size())):Math.min(1,beats.size());restoredRevision=-1;
  setVisibility(VISIBLE);if(entering){setAlpha(0);animate().alpha(1).setDuration(450).start();}
  render(false);
 }
 private void appendBeats(String text){if(text==null||text.trim().isEmpty())return;beats.addAll(StoryBeats.split(text));}
 private TextView label(String value,int size,int color,boolean bold){TextView v=new TextView(getContext());v.setText(value);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
 private void render(boolean animateLine){
  lines.removeAllViews();TextView title=label(frame.menu.title,16,0xff9c9993,true);title.setPadding(0,0,0,dp(22));lines.addView(title);
  for(int i=0;i<shown;i++){TextView line=label(beats.get(i),19,0xffeeeeee,false);line.setText(DialogueColors.decorate(beats.get(i)));line.setLineSpacing(dp(3),1.12f);line.setPadding(0,0,0,dp(18));lines.addView(line);
   if(animateLine&&i>=shown-2){line.setAlpha(0);line.animate().alpha(1).setDuration(350).start();}
  }
  actions.removeAllViews();
  if(shown<beats.size()){
   boolean john=StoryBeats.isJohn(beats.get(shown));String label=john?StoryBeats.reply(beats.get(shown)):"Продолжить";
   addButton(label,"",true,()->{if(submitted)return;int before=shown;shown++;if(john&&shown<beats.size()&&!StoryBeats.isJohn(beats.get(shown)))shown++;render(shown>before);},false);
  } else {
   for(GameSession.Choice c:frame.choices)addButton(c.label,c.detail,c.enabled,()->{if(submitted)return;submitted=true;choose.accept(c.number);},true);
  }
  int limit=dp(250);LinearLayout.LayoutParams bp=(LinearLayout.LayoutParams)buttonScroll.getLayoutParams();int estimate=actions.getChildCount()*dp(70);bp.height=Math.min(limit,Math.max(dp(64),estimate));buttonScroll.setLayoutParams(bp);
  textScroll.post(()->textScroll.fullScroll(View.FOCUS_DOWN));
 }
 private void addButton(String label,String detail,boolean enabled,Runnable click,boolean realChoice){
  LinearLayout button=new LinearLayout(getContext());button.setOrientation(LinearLayout.VERTICAL);button.setGravity(Gravity.CENTER_VERTICAL);button.setPadding(dp(16),dp(12),dp(16),dp(12));
  GradientDrawable bg=new GradientDrawable();bg.setColor(0xff202020);bg.setStroke(dp(1),0xff66615a);bg.setCornerRadius(dp(7));button.setBackground(bg);
  button.addView(label(label,16,0xfff0efeb,true));if(detail!=null&&!detail.trim().isEmpty())button.addView(label(detail,12,0xffb0aca6,false));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(7);actions.addView(button,lp);button.setEnabled(enabled);button.setAlpha(enabled?1f:.43f);button.setContentDescription(label+(detail==null||detail.isEmpty()?"":". "+detail));
  if(enabled)button.setOnClickListener(v->click.run());
 }
 void save(Bundle out){if(frame!=null&&getVisibility()==VISIBLE){out.putLong("story_revision",revision);out.putInt("story_shown",shown);}}
}
