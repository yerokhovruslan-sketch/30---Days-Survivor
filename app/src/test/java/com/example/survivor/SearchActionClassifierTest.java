package com.example.survivor;
import org.junit.Test;
import static org.junit.Assert.*;
public class SearchActionClassifierTest {
 @Test public void onlyExplorationSearchChoicesTriggerTransition() {
  assertTrue(SearchActionClassifier.matches("Обыскать шкаф"));
  assertTrue(SearchActionClassifier.matches("Войти и обыскать"));
  assertTrue(SearchActionClassifier.matches("Проверить рюкзак"));
  assertTrue(SearchActionClassifier.matches("Тщательный поиск"));
  assertFalse(SearchActionClassifier.matches("Осмотреть тело"));
  assertFalse(SearchActionClassifier.matches("Осмотреть место стрельбы"));
  assertFalse(SearchActionClassifier.matches("Вернуться домой"));
 }
}
