package com.example.survivor;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Colors speaker labels without changing the color or contents of their words. */
final class DialogueColors {
 private static final Pattern SPEAKER=Pattern.compile("(?m)^\\s*(Джон|Сем[её]н|Андрей|Марина|Арт[её]м|Кирилл|Виктор(?: Павлович)?|Ольга|Заведующий|Рейдер|Выживший|Незнакомец|Голос)\\s*:",Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
 private static final int[] NPC={0xff80d6cb,0xffffa589,0xff9dc5ff,0xffa7da8d,0xfff2adcf,0xffffc995};
 static CharSequence decorate(CharSequence text){
  if(text==null)return "";
  SpannableString result=new SpannableString(text);Matcher matcher=SPEAKER.matcher(text);
  while(matcher.find()){
   String name=matcher.group(1).toLowerCase(Locale.ROOT).replace('ё','е');
   int color=name.equals("джон")?0xffd3a0ff:name.equals("семен")?0xffffdf76:NPC[Math.floorMod(name.hashCode(),NPC.length)];
   result.setSpan(new ForegroundColorSpan(color),matcher.start(1),matcher.end(1),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }
  return result;
 }
 private DialogueColors(){}
}
