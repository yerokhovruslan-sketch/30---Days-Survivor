package com.example.survivor;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class StoryBeatsTest {
 @Test public void splitsEverySpeakerAndShowsJohnsReplyAsButton(){
  List<String> beats=StoryBeats.split("Семён: «Мне снится этот двор».\n\nДжон: «Ну и что?»\n\nСемён: «А что, если я не просыпаюсь?»\nСемён: «Ты видел эвакуацию?»\nДжон: «Пока нет».");
  assertEquals(5,beats.size());assertTrue(StoryBeats.isJohn(beats.get(1)));assertEquals("Ну и что?",StoryBeats.reply(beats.get(1)));
  assertEquals("Пока нет",StoryBeats.reply(beats.get(4)));
 }
 @Test public void proseAndInlineDialogueStayInOrder(){
  List<String> beats=StoryBeats.split("Он подходит к машине. Семён: «Привет». Джон: «Здравствуй». Семён: «Садись».");
  assertEquals(4,beats.size());assertEquals("Он подходит к машине.",beats.get(0));assertEquals("Здравствуй",StoryBeats.reply(beats.get(2)));
 }
}
