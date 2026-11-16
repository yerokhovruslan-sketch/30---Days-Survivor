package com.example.survivor;
import java.util.Locale;
/** An intentionally conservative default when a scene does not explicitly describe emotion. */
final class DialogueMood {
 enum Mood { NEUTRAL, WARY, SMILE, SAD, ANGRY }
 static Mood from(String line){
  String text=line==null?"":line.toLowerCase(Locale.ROOT);
  if(text.matches("(?s).*(стоять|угрожа|разозли|убью|придурок|гнев|злится|кричит|зло смотрит).*"))return Mood.ANGRY;
  if(text.matches("(?s).*(лен[аеуы]|жена|жены|шарф|груст|погиб|умер|потерял|сожале|печаль|опускает глаза).*"))return Mood.SAD;
  if(text.matches("(?s).*(усмех|сме[её]т|улыба|шут|ахаха|посимпатичнее|реалистичнее).*"))return Mood.SMILE;
  if(text.matches("(?s).*(термос|подозр|осторож|напряга|опас|кто ты|насторож|оружие).*"))return Mood.WARY;
  return Mood.NEUTRAL;
 }
 static String label(Mood mood,boolean female){return switch(mood){case ANGRY->"Сердится";case SAD->female?"Задумчивая":"Задумчивый";case SMILE->"Улыбается";case WARY->female?"Настороженная":"Настороженный";default->female?"Спокойная":"Спокойный";};}
}
