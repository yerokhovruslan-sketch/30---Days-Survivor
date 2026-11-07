package com.example.survivor;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.survivor.game.GameSession;

/** Keeps the current expedition and pending choice across Activity recreation. */
public final class GameViewModel extends AndroidViewModel {
    private final MutableLiveData<GameSession.Frame> frames = new MutableLiveData<>();
    private final MutableLiveData<String> searchScene = new MutableLiveData<>();
    private final MutableLiveData<String> travelScene = new MutableLiveData<>();
    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final java.util.Random searchRandom = new java.util.Random();
    private SearchNarrative searchNarrative;
    private GameSession.Frame pendingSearch;
    private int pendingSearchAction;
    private boolean waitingSearchResult;
    private long searchRemaining, searchDeadline, suppressSearchUntil;
    private final Runnable finishSearch = this::finishSearchTransition;
    private GameSession session;
    private int sessionGeneration;
    private final IntroSequence intro;
    private final RadioPlayback radio;
    private final GameEffects effects;
    private final AdaptiveMusic music;
    private final android.os.Handler musicHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final android.os.Handler effectsHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private GameSession.Frame radioFrame;
    private boolean foreground;
    String section = "SURVIVOR";

    public GameViewModel(@NonNull Application application) {
        super(application);
        intro = new IntroSequence(application);
        radio = new RadioPlayback(application);
        music=new AdaptiveMusic(application);
        effects = new GameEffects(application,music::audible);
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(application.getAssets().open("search_context_phrases.txt"), java.nio.charset.StandardCharsets.UTF_8))) {
            java.util.ArrayList<String> lines = new java.util.ArrayList<>(); String line;
            while ((line=reader.readLine())!=null) { line=line.replace("\uFEFF", "").trim(); if(!line.isEmpty())lines.add(line); }
            searchNarrative=new SearchNarrative(lines);
        } catch (java.io.IOException ignored) { searchNarrative=new SearchNarrative(java.util.Collections.emptyList()); }
        if (!Boolean.TRUE.equals(intro.visible.getValue())) restart();
    }

    LiveData<GameSession.Frame> frames() { return frames; }

    void restart() {
        music.restart();
        searchHandler.removeCallbacksAndMessages(null); pendingSearch=null;waitingSearchResult=false;searchScene.setValue(null);
        final int generation=++sessionGeneration;
        effects.clear();effectsHandler.removeCallbacksAndMessages(null);
        if (session != null) session.close();
        session = new GameSession(getApplication().getFilesDir(), frame->effectsHandler.post(()->{if(generation==sessionGeneration){frames.setValue(frame);if(waitingSearchResult && frame.mode!=GameSession.Mode.BUSY){waitingSearchResult=false;searchScene.setValue(null);}}}));
        session.setTravelListener(text->effectsHandler.post(()->{if(generation==sessionGeneration)travelScene.setValue(text);}));
        session.setEffectsListener(sound->effectsHandler.post(()->{
            if(sound==com.example.survivor.game.GameSound.SEARCH && android.os.SystemClock.uptimeMillis()<suppressSearchUntil){suppressSearchUntil=0;return;}
            if(foreground && !Boolean.TRUE.equals(intro.visible.getValue()) && !radio.active()){effects.play(sound);if(sound!=com.example.survivor.game.GameSound.CLICK)music.effect();}
        }));
        session.setForeground(foreground);
        session.start();
    }

    LiveData<String> searchScene() { return searchScene; }
    LiveData<String> travelScene() { return travelScene; }
    void clearTravelScene(){travelScene.setValue(null);}
    static boolean isSearchAction(GameSession.Frame frame, int number) {
        if(frame==null || frame.home || frame.place<=0 || frame.mode!=GameSession.Mode.CHOICE || !frame.menu.scene.isEmpty())return false;
        for(GameSession.Choice c:frame.choices) if(c.number==number && c.enabled) {
            if(frame.menu.board!=null && !c.label.toLowerCase(java.util.Locale.ROOT).contains("обыскать"))return false;
            return SearchActionClassifier.matches(c.label);
        }
        return false;
    }
    void submit(GameSession.Frame frame, int number) {
        if(session==null || pendingSearch!=null || waitingSearchResult)return;
        if(!isSearchAction(frame,number)){session.submit(frame.revision,number);return;}
        pendingSearch=frame;pendingSearchAction=number;searchRemaining=SearchDuration.holdMillis(searchRandom);session.setPaused(true);
        searchScene.setValue(searchNarrative.choose(searchRandom,frame.place,frame.menu.title));effects.play(com.example.survivor.game.GameSound.SEARCH);
        if(foreground)scheduleSearch();
    }
    private void scheduleSearch(){searchDeadline=android.os.SystemClock.uptimeMillis()+searchRemaining;searchHandler.removeCallbacks(finishSearch);searchHandler.postDelayed(finishSearch,searchRemaining);}
    private void finishSearchTransition(){
        if(session==null || pendingSearch==null)return;
        GameSession.Frame selected=pendingSearch;int action=pendingSearchAction;pendingSearch=null;searchRemaining=0;
        for(GameSession.Choice c:selected.choices)if(c.number==action && c.label.toLowerCase(java.util.Locale.ROOT).endsWith("поиск"))suppressSearchUntil=android.os.SystemClock.uptimeMillis()+2500;
        waitingSearchResult=true;session.setPaused(false);
        if(!session.submit(selected.revision,action)){waitingSearchResult=false;searchScene.setValue(null);}
    }
    void resumeMusic() {
        foreground = true;
        if(session!=null)session.setForeground(true);
        if(pendingSearch!=null)scheduleSearch();
        effects.resume();
        music.foreground(true);music.voice(Boolean.TRUE.equals(intro.visible.getValue()) || radio.active());
        if (Boolean.TRUE.equals(intro.visible.getValue())) intro.resume(); else if(radio.active())resumeRadioAfterFade();
    }
    void pauseMusic() { if(pendingSearch!=null){searchRemaining=Math.max(0,searchDeadline-android.os.SystemClock.uptimeMillis());searchHandler.removeCallbacks(finishSearch);} musicHandler.removeCallbacksAndMessages(null);music.foreground(false);foreground = false;if(session!=null)session.setForeground(false);effects.pause(); intro.pause();radio.pause(); }
    void pauseWorld(boolean value){if(value)effects.pause();else if(foreground)effects.resume();music.paused(value);if(session!=null)session.setPaused(value);}
    void bagSound(){effects.play(com.example.survivor.game.GameSound.BAG);}
    void unavailableSound(){effects.play(com.example.survivor.game.GameSound.ERROR);}
    void clickSound() {effects.play(com.example.survivor.game.GameSound.CLICK);}
    LiveData<RadioPlayback.State> radioState() {return radio.state;}
    void bindRadioScene(GameSession.Frame frame) {
        if(frame.mode==GameSession.Mode.BUSY)return;
        boolean wasActive=radio.active();
        if(!wasActive && !frame.menu.scene.isEmpty())effects.clear();
        effects.update(frame,!frame.menu.scene.isEmpty() || Boolean.TRUE.equals(intro.visible.getValue()));
        music.update(frame);music.voice(!frame.menu.scene.isEmpty() || Boolean.TRUE.equals(intro.visible.getValue()));
        radio.show(frame);radioFrame=frame;
        if(foreground) {
            if(radio.active())resumeRadioAfterFade();
        }
    }
    private void resumeRadioAfterFade(){musicHandler.removeCallbacksAndMessages(null);musicHandler.postDelayed(()->{if(foreground && radio.active())radio.resume();},music.voiceDelay());}
    void continueRadio() {
        if(radioFrame==null || !radio.canContinue())return;
        radio.blockContinue();submit(radioFrame,1);
    }
    LiveData<Boolean> introVisible() { return intro.visible; }
    LiveData<Integer> introProgress() { return intro.progress; }
    void continueIntro() {
        if (!intro.continueToGame()) return;
        music.voice(false);restart();
    }

    @Override protected void onCleared() { searchHandler.removeCallbacksAndMessages(null);if (session != null) session.close();effectsHandler.removeCallbacksAndMessages(null);effects.close(); intro.close();radio.close();musicHandler.removeCallbacksAndMessages(null);music.close(); }
}
