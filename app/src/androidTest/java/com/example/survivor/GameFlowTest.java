package com.example.survivor;

import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class GameFlowTest {
    private void completeIntro(ActivityScenario<MainActivity> scenario) throws Exception {
        long deadline = System.currentTimeMillis() + 45000;
        boolean[] ready = {false};
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> {
                View screen = activity.findViewById(R.id.intro_screen);
                View proceed = activity.findViewById(R.id.intro_continue);
                if (screen.getVisibility() != View.VISIBLE) ready[0] = true;
                else if (proceed.getVisibility() == View.VISIBLE && proceed.isEnabled()) {
                    proceed.performClick();
                    ready[0] = true;
                }
            });
            if (ready[0]) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Intro did not unlock continue after 30 seconds");
    }
    private void awaitText(ActivityScenario<MainActivity> scenario, int id, String text) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        boolean[] matched = {false};
        while (System.currentTimeMillis() < deadline) {
            scenario.onActivity(activity -> matched[0] = ((TextView) activity.findViewById(id)).getText().toString().contains(text));
            if (matched[0]) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Expected UI text: " + text);
    }

    @Test public void restRotateSaveAndContinue() throws Exception {
        // Run on a fresh test install; production saves are never touched by this test.
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            completeIntro(scenario);
            awaitText(scenario, R.id.section_text, "Ты — Джон");
            onView(withText(R.string.back_to_menu)).perform(click());
            onView(withId(R.id.action_rest)).perform(scrollTo(), click());
            awaitText(scenario, R.id.section_text, "Передышка");
            onView(withId(R.id.character_day)).check(matches(withText(org.hamcrest.Matchers.containsString("08:20"))));
            scenario.recreate();
            awaitText(scenario, R.id.section_text, "Передышка");
            onView(withId(R.id.character_day)).check(matches(withText(org.hamcrest.Matchers.containsString("08:20"))));
            onView(withText(R.string.back_to_menu)).perform(click());
            onView(withId(R.id.action_save)).perform(scrollTo(), click());
            awaitText(scenario, R.id.section_text, "Игра сохранена:");
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            completeIntro(scenario);
            awaitText(scenario, R.id.section_title, "SURVIVOR");
            onView(isAssignableFrom(android.widget.EditText.class)).check(doesNotExist());
            onView(withText("Продолжить сохранённую игру")).perform(scrollTo(), click());
            awaitText(scenario, R.id.section_text, "Прогресс загружен");
            onView(withId(R.id.character_day)).check(matches(withText(org.hamcrest.Matchers.containsString("08:20"))));
        }
    }
}
