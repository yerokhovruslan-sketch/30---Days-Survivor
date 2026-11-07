package com.example.survivor;


import android.os.Bundle;
import android.view.View;

import android.widget.Button;


import android.widget.ProgressBar;

import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import com.example.survivor.game.GameSession;

public class MainActivity extends AppCompatActivity {
    private GameViewModel game;
    private GameSession.Frame frame;
    private View panel;
    private MenuRenderer renderer;
    private WorldMapView worldMap;
    private StoryDialogueView storyView;
    private TerminalPuzzleView terminalPuzzle;

    private long dismissedRevision = -1;
    private android.widget.FrameLayout searchOverlay;
    private android.widget.FrameLayout travelOverlay;
    private TextView travelLabel;
    private final android.os.Handler travelHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private TextView searchText, searchDots;
    private final android.os.Handler dotsHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private int dotsStep;
    private final Runnable dotsTick = new Runnable() { @Override public void run() {
        if(searchOverlay==null || searchOverlay.getVisibility()!=View.VISIBLE)return;
        String[] dots={"·", "·  ·", "·  ·  ·", ""};searchDots.setText(dots[dotsStep++ % dots.length]);
        dotsHandler.postDelayed(this,440);
    }};

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(false);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        if (savedInstanceState != null) dismissedRevision = savedInstanceState.getLong("dismissed", -1);
        panel = findViewById(R.id.game_panel);


        game = new ViewModelProvider(this).get(GameViewModel.class);
        worldMap = new WorldMapView(this, number -> { game.clickSound(); submit(number); });
        ((android.widget.FrameLayout)findViewById(R.id.main)).addView(worldMap, new android.widget.FrameLayout.LayoutParams(-1,-1));
        storyView = new StoryDialogueView(this, number -> { game.clickSound(); submit(number); }, savedInstanceState);
        ((android.widget.FrameLayout)findViewById(R.id.main)).addView(storyView, new android.widget.FrameLayout.LayoutParams(-1,-1));
        terminalPuzzle=new TerminalPuzzleView(this,this::submit);
        ((android.widget.FrameLayout)findViewById(R.id.main)).addView(terminalPuzzle,new android.widget.FrameLayout.LayoutParams(-1,-1));
        installTravelOverlay();
        game.travelScene().observe(this,this::showTravelScene);
        installSearchOverlay();
        game.searchScene().observe(this, this::showSearchScene);
        findViewById(R.id.character_day).setOnClickListener(v->{
            game.pauseWorld(true);
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Пауза")
                .setMessage("Время расходуется только на действия: дорогу, поиск, бой, отдых и изготовление. Чтение и меню не продвигают часы.")
                .setPositiveButton("Продолжить",(d,w)->{}).setOnDismissListener(d->game.pauseWorld(false)).show();
        });
        findViewById(R.id.panel_world_clock).setOnClickListener(v->findViewById(R.id.character_day).performClick());
        findViewById(R.id.radio_continue).setSoundEffectsEnabled(false);
        findViewById(R.id.radio_continue).setOnClickListener(view->{if(TapFeedback.run(view,()->game.continueRadio()))game.clickSound();});
        game.radioState().observe(this,state->{
            View screen=findViewById(R.id.radio_screen);
            screen.setVisibility(state.visible?View.VISIBLE:View.GONE);screen.setKeepScreenOn(state.visible);
            TextView heading=findViewById(R.id.radio_heading);heading.setText(state.heading);
            heading.setVisibility(state.heading.isEmpty()?View.GONE:View.VISIBLE);
            ((TextView)findViewById(R.id.radio_body)).setText(state.body);
            View button=findViewById(R.id.radio_continue);
            button.setEnabled(state.ready);
            if(state.ready && button.getVisibility()!=View.VISIBLE) {
                button.setAlpha(0);button.setVisibility(View.VISIBLE);button.animate().alpha(1).setDuration(700).start();
            } else if(!state.ready) {button.animate().cancel();button.setVisibility(View.INVISIBLE);button.setAlpha(0);}
        });
        View introScreen = findViewById(R.id.intro_screen);
        findViewById(R.id.intro_continue).setSoundEffectsEnabled(false);
        findViewById(R.id.intro_continue).setOnClickListener(view -> {if(TapFeedback.run(view,()->game.continueIntro()))game.clickSound();});
        game.introVisible().observe(this, visible -> {
            introScreen.setVisibility(Boolean.TRUE.equals(visible) ? View.VISIBLE : View.GONE);
            introScreen.setKeepScreenOn(Boolean.TRUE.equals(visible));
        });
        game.introProgress().observe(this, elapsed -> {
            findViewById(R.id.intro_picture).setAlpha(IntroTimeline.pictureAlpha(elapsed));
            TextView text = findViewById(R.id.intro_message);
            int count = IntroTimeline.characters(elapsed);
            String value = IntroTimeline.TEXT.substring(0, count);
            if (elapsed >= IntroTimeline.PICTURE_AT && !IntroTimeline.ready(elapsed)) value += "▌";
            text.setText(value);
            text.setVisibility(elapsed >= IntroTimeline.PICTURE_AT ? View.VISIBLE : View.INVISIBLE);
            View proceed = findViewById(R.id.intro_continue);
            proceed.setVisibility(IntroTimeline.ready(elapsed) ? View.VISIBLE : View.INVISIBLE);
            proceed.setEnabled(IntroTimeline.ready(elapsed));
        });
        renderer = new MenuRenderer(panel, new MenuRenderer.Listener() {
            @Override public void action(int number) { submit(number); }
            @Override public void dismiss() { dismissResult(); }
            @Override public void restart() { game.section = "SURVIVOR"; game.restart(); }
            @Override public void close() { finish(); }
            @Override public void click() {game.clickSound();}
            @Override public void unavailable(){game.unavailableSound();}
        }, savedInstanceState);
        int[] actions = {R.id.action_status, R.id.action_explore, R.id.action_inventory,
                R.id.action_shop, R.id.action_heal, R.id.action_equipment, R.id.action_rest,
                R.id.action_notes, R.id.action_eat, R.id.action_sleep, R.id.action_save,
                R.id.action_load, R.id.action_unload, R.id.action_repair, R.id.action_armor,
                R.id.action_journal, R.id.action_exit, R.id.action_shelter};
        for (int i = 0; i < actions.length; i++) {
            final int action = i == 16 ? 0 : i == 17 ? 17 : i + 1;
            Button button = findViewById(actions[i]);
            button.setSoundEffectsEnabled(false);
            MenuArtwork.apply(button, MenuArtwork.forButton(actions[i]));
            android.graphics.ColorMatrix monochrome = new android.graphics.ColorMatrix();
            monochrome.setSaturation(0);
            for (android.graphics.drawable.Drawable icon : button.getCompoundDrawablesRelative()) {
                if (icon != null) icon.mutate().setColorFilter(new android.graphics.ColorMatrixColorFilter(monochrome));
            }
            button.setOnClickListener(view -> {
                if (frame == null || frame.mode != GameSession.Mode.MAIN) return;
                GameSession.Frame expected=frame;
                if(TapFeedback.run(view,()->{if(frame!=expected)return;game.section=button.getText().toString();submit(action);}))game.clickSound();
            });
        }


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (terminalPuzzle != null && terminalPuzzle.getVisibility() == View.VISIBLE) return;
                if (travelOverlay != null && travelOverlay.getVisibility() == View.VISIBLE) return;
                if (searchOverlay != null && searchOverlay.getVisibility() == View.VISIBLE) return;
                if (storyView != null && storyView.getVisibility() == View.VISIBLE) return;
                if (Boolean.TRUE.equals(game.introVisible().getValue())) { moveTaskToBack(true); return; }
                if(frame!=null && !frame.menu.scene.isEmpty()) {moveTaskToBack(true);return;}
                if (frame == null || frame.mode == GameSession.Mode.BUSY) return;
                if (frame.mode == GameSession.Mode.ENDED) { finish(); return; }
                if (frame.mode == GameSession.Mode.MAIN) {
                    if (panel.getVisibility() == View.VISIBLE) dismissResult();
                    else submit(0);
                } else if (frame.quantity || frame.choices.stream().anyMatch(choice -> choice.number == 0 && choice.enabled)) submit(0);
                else Toast.makeText(MainActivity.this, R.string.finish_choice, Toast.LENGTH_SHORT).show();
            }
        });
        game.frames().observe(this, this::render);
    }

    private void installTravelOverlay(){
        travelOverlay=new android.widget.FrameLayout(this);travelOverlay.setBackgroundColor(android.graphics.Color.BLACK);travelOverlay.setVisibility(View.GONE);travelOverlay.setClickable(true);travelOverlay.setFocusable(true);
        travelLabel=new TextView(this);travelLabel.setTextColor(android.graphics.Color.WHITE);travelLabel.setTextSize(22);travelLabel.setGravity(android.view.Gravity.CENTER);
        travelOverlay.addView(travelLabel,new android.widget.FrameLayout.LayoutParams(-1,-2,android.view.Gravity.CENTER));
        ((android.widget.FrameLayout)findViewById(R.id.main)).addView(travelOverlay,new android.widget.FrameLayout.LayoutParams(-1,-1));
    }
    private void showTravelScene(String text){
        travelHandler.removeCallbacksAndMessages(null);travelOverlay.animate().cancel();
        if(text==null){travelOverlay.setVisibility(View.GONE);return;}
        travelLabel.setText(text+"  ·");travelOverlay.setVisibility(View.VISIBLE);travelOverlay.setAlpha(0);travelOverlay.animate().alpha(1).setDuration(450).start();
        travelHandler.postDelayed(()->travelLabel.setText(text+"  ·  ·"),700);
        travelHandler.postDelayed(()->travelLabel.setText(text+"  ·  ·  ·"),1300);
        travelHandler.postDelayed(()->travelOverlay.animate().alpha(0).setDuration(450).withEndAction(()->{travelOverlay.setVisibility(View.GONE);game.clearTravelScene();}).start(),2350);
    }
    private void installSearchOverlay() {
        searchOverlay = new android.widget.FrameLayout(this);
        searchOverlay.setBackgroundColor(android.graphics.Color.BLACK);
        searchOverlay.setVisibility(View.GONE);searchOverlay.setClickable(true);searchOverlay.setFocusable(true);
        android.widget.LinearLayout content = new android.widget.LinearLayout(this);
        content.setOrientation(android.widget.LinearLayout.VERTICAL);content.setGravity(android.view.Gravity.CENTER);
        float density=getResources().getDisplayMetrics().density;
        int padding=(int)(28*density);content.setPadding(padding,padding,padding,padding);
        searchText = new TextView(this);searchText.setTextColor(android.graphics.Color.WHITE);searchText.setTextSize(20);
        searchText.setGravity(android.view.Gravity.CENTER);searchText.setLineSpacing(6*density,1.12f);
        searchText.setFontFeatureSettings("kern");
        content.addView(searchText,new android.widget.LinearLayout.LayoutParams(-1,-2));
        searchDots = new TextView(this);searchDots.setTextColor(0xFFCCCCCC);searchDots.setTextSize(26);searchDots.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams dotsParams=new android.widget.LinearLayout.LayoutParams(-1,(int)(50*density));dotsParams.topMargin=(int)(28*density);
        content.addView(searchDots,dotsParams);
        searchOverlay.addView(content,new android.widget.FrameLayout.LayoutParams(-1,-2,android.view.Gravity.CENTER));
        ((android.widget.FrameLayout)findViewById(R.id.main)).addView(searchOverlay,new android.widget.FrameLayout.LayoutParams(-1,-1));
    }
    private void showSearchScene(String phrase) {
        dotsHandler.removeCallbacks(dotsTick);searchOverlay.animate().cancel();searchText.animate().cancel();
        if(phrase==null){
            if(searchOverlay.getVisibility()==View.VISIBLE)searchOverlay.animate().alpha(0f).setDuration(450).withEndAction(()->{searchOverlay.setVisibility(View.GONE);searchOverlay.setAlpha(1f);}).start();
            return;
        }
        searchText.setText(phrase);searchText.setAlpha(0f);searchOverlay.setAlpha(0f);searchOverlay.setVisibility(View.VISIBLE);
        searchOverlay.animate().alpha(1f).setDuration(500).start();
        searchText.animate().alpha(1f).setStartDelay(350).setDuration(750).start();
        dotsStep=0;dotsHandler.post(dotsTick);
    }
    @Override protected void onDestroy(){if(terminalPuzzle!=null)terminalPuzzle.close();dotsHandler.removeCallbacksAndMessages(null);travelHandler.removeCallbacksAndMessages(null);super.onDestroy();}

    private void submit(int action) {
        GameSession.Frame pending = frame;
        if (pending == null || pending.mode == GameSession.Mode.BUSY) return;


        game.submit(pending, action);
    }

    private void render(GameSession.Frame current) {
        frame = current;
        worldMap.showFrame(current);
        storyView.showFrame(current);
        terminalPuzzle.showFrame(current);
        findViewById(R.id.action_save).setVisibility(current.savesAllowed ? View.VISIBLE : View.GONE);
        findViewById(R.id.action_load).setVisibility(current.savesAllowed ? View.VISIBLE : View.GONE);
        ((Button) findViewById(R.id.action_exit)).setText(current.savesAllowed ? R.string.menu_exit : R.string.menu_exit_without_save);
        game.bindRadioScene(current);
        View sleepButton = findViewById(R.id.action_sleep);
        boolean canSleep = current.sleepiness > 40 && current.health > 0;
        sleepButton.setEnabled(canSleep);
        sleepButton.setAlpha(canSleep ? 1f : 0.65f);
        if (canSleep) ((Button) sleepButton).setText(R.string.menu_sleep);
        else {
            String title = getString(R.string.menu_sleep);
            android.text.SpannableString label = new android.text.SpannableString(title + "\n" + getString(R.string.sleep_requirement));
            label.setSpan(new android.text.style.AbsoluteSizeSpan(11,true),title.length()+1,label.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            label.setSpan(new android.text.style.ForegroundColorSpan(0xffb8b8b8),title.length()+1,label.length(),android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((Button) sleepButton).setText(label);
        }
        ((TextView) findViewById(R.id.character_day)).setText(getString(R.string.day_format, current.day) + " · " + current.time + " · " + current.difficulty + " · " + getString(current.night ? R.string.night_phase : R.string.day_phase));
        ((TextView)findViewById(R.id.panel_world_clock)).setText("День "+current.day+" · "+current.time+" · Время действий");
        ((TextView) findViewById(R.id.character_balance)).setText(getString(R.string.balance_format, current.balance));

        stat(R.id.stat_stamina, R.id.value_stamina, current.stamina, 100);
        stat(R.id.stat_hunger, R.id.value_hunger, current.hunger, 100);
        stat(R.id.stat_sleepiness, R.id.value_sleepiness, current.sleepiness, 100);
        stat(R.id.stat_noise, R.id.value_noise, current.noise, 60);
        if (current.mode == GameSession.Mode.ENDED && (current.text.contains("Игра сохранена:") || current.text.contains("Игра завершена без сохранения."))) {
            finish();
            return;
        }
        // A successful load goes directly to the main menu; retain real result and error screens.
        boolean loadConfirmationOnly = current.mode == GameSession.Mode.MAIN
                && current.menu.cards.isEmpty() && current.text.trim().equals("Прогресс загружен: John");
        boolean show = current.mode != GameSession.Mode.MAIN || (!loadConfirmationOnly && (!current.text.isEmpty() || !current.menu.cards.isEmpty()) && dismissedRevision != current.revision);
        panel.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        renderer.render(current, game.section);
    }

    private void stat(int barId, int labelId, int value, int max) {
        ProgressBar bar = findViewById(barId);
        bar.setMax(max);
        bar.setProgress(value);
        ((TextView) findViewById(labelId)).setText(getString(R.string.stat_format, value, max));
    }

    private void dismissResult() {
        if(frame!=null && frame.menu.title.equals("Инвентарь"))game.bagSound();
        dismissedRevision = frame.revision;
        panel.setVisibility(View.GONE);
    }

    @Override protected void onResume() {
        super.onResume();
        game.resumeMusic();
    }

    @Override protected void onPause() {
        game.pauseMusic();
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putLong("dismissed", dismissedRevision);
        renderer.save(state);
        storyView.save(state);
        super.onSaveInstanceState(state);
    }
}
