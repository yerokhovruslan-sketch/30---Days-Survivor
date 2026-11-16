package com.example.survivor;
import org.junit.Test;
import static org.junit.Assert.*;
public class DialogueMoodTest {
 @Test public void moodLabelsAndExpressionsFollowConversation(){
  assertEquals(DialogueMood.Mood.ANGRY,DialogueMood.from("Андрей: Стоять!"));
  assertEquals(DialogueMood.Mood.SAD,DialogueMood.from("Оля опускает глаза"));
  assertEquals(DialogueMood.Mood.SMILE,DialogueMood.from("Оля улыбалась"));
  assertEquals(DialogueMood.Mood.WARY,DialogueMood.from("Андрей настороженно посмотрел"));
  assertEquals("Задумчивая",DialogueMood.label(DialogueMood.Mood.SAD,true));
  assertEquals("Настороженный",DialogueMood.label(DialogueMood.Mood.WARY,false));
 }
}
