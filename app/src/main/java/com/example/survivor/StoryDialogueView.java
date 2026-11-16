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
import android.widget.ImageView;
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
 private final TextView personName,emotion;private final ImageView portrait;private String character="";
 private GameSession.Frame frame;
 private List<String> beats=new ArrayList<>();
 private long revision=-1,restoredRevision=-1;
 private int shown,restoredShown;
 private boolean submitted;
 StoryDialogueView(Context context,IntConsumer choose,Bundle saved){super(context);this.choose=choose;setBackgroundColor(Color.BLACK);setClickable(true);setFocusable(true);setVisibility(GONE);
  if(saved!=null){restoredRevision=saved.getLong("story_revision",-1);restoredShown=saved.getInt("story_shown",0);}
  column=new LinearLayout(context);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(20),dp(14),dp(20),dp(14));addView(column,new LayoutParams(-1,-1));
  LinearLayout header=new LinearLayout(context);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(0,0,0,dp(16));
  LinearLayout identity=new LinearLayout(context);identity.setOrientation(LinearLayout.VERTICAL);personName=label("",22,0xffeee7dc,true);emotion=label("",15,0xffb7a794,false);emotion.setPadding(0,dp(5),0,0);identity.addView(personName);identity.addView(emotion);header.addView(identity,new LinearLayout.LayoutParams(0,-2,1));
  portrait=new ImageView(context);portrait.setScaleType(ImageView.ScaleType.CENTER_CROP);LinearLayout.LayoutParams imageSize=new LinearLayout.LayoutParams(dp(110),dp(118));imageSize.leftMargin=dp(12);header.addView(portrait,imageSize);column.addView(header,new LinearLayout.LayoutParams(-1,dp(135)));
  textScroll=new ScrollView(context);textScroll.setFillViewport(false);textScroll.setVerticalScrollBarEnabled(true);
  lines=new LinearLayout(context);lines.setOrientation(LinearLayout.VERTICAL);textScroll.addView(lines);column.addView(textScroll,new LinearLayout.LayoutParams(-1,0,1));
  buttonScroll=new ScrollView(context);buttonScroll.setFillViewport(false);actions=new LinearLayout(context);actions.setOrientation(LinearLayout.VERTICAL);buttonScroll.addView(actions);column.addView(buttonScroll,new LinearLayout.LayoutParams(-1,-2));
 }
 private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
 static boolean matches(GameSession.Frame f){
  if(f==null||f.mode!=GameSession.Mode.CHOICE||!f.menu.scene.isEmpty()||f.quantity||f.menu.board!=null)return false;
  if(f.menu.title.equals("Гайд")||f.menu.title.equals("Точки интереса"))return false;
  if(!f.menu.story)return false;
  if((f.menu.title.equals("Чужая добыча") || f.menu.title.equals("Последняя банка") || f.menu.title.equals("Предложение Кирилла") || f.menu.title.equals("Кирилл вернулся")) && !f.menu.cards.isEmpty() || f.menu.title.equals("Предложение Кирилла") || f.menu.title.equals("Кирилл вернулся"))return true;
  String text=f.text+"\n"+f.menu.hint;
  for(GameMenu.Card card:f.menu.cards)text+="\n"+card.title+"\n"+card.detail;
  return f.menu.title.equals("Кто-то ещё говорит") || f.menu.title.equals("Неустойчивый контакт") || text.matches("(?s).*(Джон|Семён|Марина|Артём|Кирилл|Виктор|Андрей|Ольга|Оля|Неизвестная|Выживший|Рейдер)\\s*:.*");
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
  character=characterFromBeats(beats);
  if(character.equals("Собеседник")){if(f.menu.title.equals("Чужая добыча")||f.menu.title.contains("Кирилл"))character="Кирилл";else if(f.menu.title.equals("Последняя банка"))character="Виктор Павлович";}
  shown=restoredRevision==revision?Math.max(1,Math.min(restoredShown,beats.size())):Math.min(1,beats.size());restoredRevision=-1;
  setVisibility(VISIBLE);if(entering){setAlpha(0);animate().alpha(1).setDuration(450).start();}
  render(false);
 }
 private void appendBeats(String text){if(text==null||text.trim().isEmpty())return;beats.addAll(StoryBeats.split(text));}
 private TextView label(String value,int size,int color,boolean bold){TextView v=new TextView(getContext());v.setText(value);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
 private void render(boolean animateLine){
  lines.removeAllViews();
  int last=Math.max(0,Math.min(beats.size()-1,shown-1));int first=last;
  if(!beats.isEmpty()&&StoryBeats.isJohn(beats.get(last))&&last>0)first=last-1;
  String active=beats.isEmpty()?"":beats.get(last);if(StoryBeats.isJohn(active)&&last>0)active=beats.get(last-1);
  updateCharacter(active);
  for(int i=first;i<shown&&i<beats.size();i++){TextView line=label(beats.get(i),21,0xffeeeeee,false);line.setText(DialogueColors.decorate(beats.get(i)));line.setLineSpacing(dp(4),1.15f);line.setPadding(0,0,0,dp(18));lines.addView(line);
   if(animateLine&&i==last){line.setAlpha(0);line.animate().alpha(1).setDuration(220).start();}
  }
  actions.removeAllViews();
  if(shown<beats.size()){
   boolean john=StoryBeats.isJohn(beats.get(shown));String label=john?StoryBeats.reply(beats.get(shown)):"Продолжить";
   addButton(label,"",true,()->{if(submitted)return;int before=shown;shown++;render(shown>before);},false);
  } else {
   for(GameSession.Choice c:frame.choices)addButton(c.label,c.detail,c.enabled,()->{if(submitted)return;submitted=true;choose.accept(c.number);},true);
  }
  int limit=dp(250);LinearLayout.LayoutParams bp=(LinearLayout.LayoutParams)buttonScroll.getLayoutParams();int estimate=actions.getChildCount()*dp(70);bp.height=Math.min(limit,Math.max(dp(64),estimate));buttonScroll.setLayoutParams(bp);
  textScroll.post(()->textScroll.scrollTo(0,0));
 }
 private String characterFromBeats(List<String> text){
  java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("^(Семён|Андрей|Марина|Артём|Кирилл|Виктор(?: Павлович)?|Ольга|Оля|Неизвестная|Выживший|Рейдер|Незнакомец|Голос):");
  for(String line:text){java.util.regex.Matcher match=pattern.matcher(line);if(match.find())return match.group(1);}return "Собеседник";
 }
 private void updateCharacter(String line){
  String found=characterFromBeats(java.util.Collections.singletonList(line));if(!found.equals("Собеседник"))character=found;
  personName.setText(character);boolean female=character.equals("Оля")||character.equals("Ольга");
  DialogueMood.Mood mood=DialogueMood.from(line);int picture=0;
  if(character.equals("Семён"))picture=switch(mood){case ANGRY->R.drawable.semen_angry;case SAD->R.drawable.semen_sad;case SMILE->R.drawable.semen_smile;case WARY->R.drawable.semen_wary;default->R.drawable.semen_neutral;};
  else if(character.equals("Андрей"))picture=switch(mood){case ANGRY->R.drawable.andrey_angry;case SAD->R.drawable.andrey_sad;case SMILE->R.drawable.andrey_smile;case WARY->R.drawable.andrey_wary;default->R.drawable.andrey_neutral;};
  else if(character.equals("Кирилл"))picture=switch(mood){case ANGRY->R.drawable.kirill_angry;case SMILE->R.drawable.kirill_smile;case WARY->R.drawable.kirill_wary;default->R.drawable.kirill_neutral;};
  else if(character.equals("Виктор")||character.equals("Виктор Павлович"))picture=switch(mood){case ANGRY->R.drawable.viktor_angry;case SAD->R.drawable.viktor_sad;case SMILE->R.drawable.viktor_smile;case WARY->R.drawable.viktor_wary;default->R.drawable.viktor_neutral;};
  else if(female)picture=switch(mood){case ANGRY->R.drawable.olya_angry;case SAD->R.drawable.olya_sad;case SMILE->R.drawable.olya_smile;default->R.drawable.olya_wary;};
  String moodLabel=DialogueMood.label(mood,female);emotion.setText(picture!=0?moodLabel:"");portrait.setVisibility(picture!=0?View.VISIBLE:View.INVISIBLE);if(picture!=0){portrait.setImageResource(picture);portrait.setContentDescription(character+": "+moodLabel);}
 }
 private void addButton(String label,String detail,boolean enabled,Runnable click,boolean realChoice){
  LinearLayout button=new LinearLayout(getContext());button.setOrientation(LinearLayout.VERTICAL);button.setGravity(Gravity.CENTER_VERTICAL);button.setPadding(dp(16),dp(12),dp(16),dp(12));
  button.setBackground(MatteSurface.create(getContext()));
  button.addView(label(label,16,0xfff0efeb,true));if(detail!=null&&!detail.trim().isEmpty())button.addView(label(detail,12,0xffb0aca6,false));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(7);actions.addView(button,lp);button.setEnabled(enabled);button.setAlpha(enabled?1f:.43f);button.setContentDescription(label+(detail==null||detail.isEmpty()?"":". "+detail));
  if(enabled)button.setOnClickListener(v->click.run());
 }
 void save(Bundle out){if(frame!=null&&getVisibility()==VISIBLE){out.putLong("story_revision",revision);out.putInt("story_shown",shown);}}
}
