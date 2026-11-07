package com.example.survivor;
import java.util.*;
import java.util.regex.Pattern;
/** Splits story prose into readable beats without changing the underlying choices. */
final class StoryBeats {
 private static final Pattern SPEAKER=Pattern.compile("(?=(?:Джон|Семён|Андрей|Марина|Артём|Кирилл|Виктор(?: Павлович)?|Ольга|Выживший|Заведующий|Рейдер|Незнакомец|Голос):\\s*)");
 static List<String> split(String text){
  List<String> result=new ArrayList<>();if(text==null)return result;
  for(String paragraph:text.split("\\n+"))for(String part:SPEAKER.split(paragraph)){
   String beat=part.trim();if(!beat.isEmpty())result.add(beat);
  }return result;
 }
 static boolean isJohn(String line){return line!=null&&line.startsWith("Джон:");}
 static String reply(String line){
  String text=line.substring("Джон:".length()).trim();
  if(text.startsWith("«")||text.startsWith("„")||text.startsWith("\""))text=text.substring(1);
  text=text.replaceFirst("[»“\"]\\.?$","").trim();
  if(text.endsWith("."))text=text.substring(0,text.length()-1);
  return text.isEmpty()?"Продолжить":text;
 }
}
