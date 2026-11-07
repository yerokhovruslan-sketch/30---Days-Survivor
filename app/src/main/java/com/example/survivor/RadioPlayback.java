package com.example.survivor;

import android.content.Context;
import android.media.*;
import android.os.*;
import androidx.lifecycle.MutableLiveData;
import com.example.survivor.game.GameSession;

/** A modal radio scene retained with the game across rotation. */
final class RadioPlayback implements AutoCloseable {
    static final String TRANSCRIPT="…Гражданским, принимающим этот сигнал. Пункт сбора — военный склад. При себе иметь запас воды и медикаментов.\n\n"
            +"Проход через северный блокпост закрыт. Используйте резервный маршрут…\n\n"
            +"…Схема прохода передана в районный отдел полиции. Архив эвакуационных документов. Позывной — «Маяк».\n\n"
            +"Не приближайтесь к ограждению после наступления темноты.\nПриём…";
    static final class State {
        final boolean visible,ready;
        final String heading,body;
        State(boolean visible,boolean ready,String heading,String body) {this.visible=visible;this.ready=ready;this.heading=heading;this.body=body;}
    }
    final MutableLiveData<State> state=new MutableLiveData<>(new State(false,false,"",""));
    private final Context context;
    private final AudioManager audio;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    private final AudioManager.OnAudioFocusChangeListener focusListener=this::focusChanged;
    private AudioFocusRequest request;
    private MediaPlayer recording,typing;
    private String scene="",heading="",text="";
    private long revision=-1,elapsed,anchor;
    private boolean foreground,focus,running,ready,closed,waitingForNext;

    RadioPlayback(Context context) {
        this.context=context.getApplicationContext();audio=(AudioManager)this.context.getSystemService(Context.AUDIO_SERVICE);
    }
    boolean active() {return !scene.isEmpty();}
    boolean canContinue() {return active() && ready && !waitingForNext;}
    void blockContinue() {waitingForNext=true;ready=false;emit();}
    void show(GameSession.Frame frame) {
        if(frame.mode==GameSession.Mode.BUSY)return;
        if(frame.menu.scene.isEmpty()) {if(active())hide();return;}
        if(revision==frame.revision)return;
        release();revision=frame.revision;scene=frame.menu.scene;
        heading=scene.equals("radio_fail")?frame.menu.title:"";text=frame.menu.hint;
        elapsed=0;waitingForNext=false;ready=scene.equals("radio_fail");emit();
    }
    void resume() {
        foreground=true;if(!active() || ready || closed || waitingForNext)return;
        if(Build.VERSION.SDK_INT>=26) {
            if(request==null)request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
                    .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(focusListener,handler).build();
            focus=audio.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else focus=audio.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        start();
    }
    private void start() {
        if(!foreground || !active() || ready || closed || waitingForNext)return;
        if(scene.equals("radio_audio")) {
            if(!focus)return;
            if(recording==null) {
                recording=MediaPlayer.create(context,R.raw.radio_mayak);
                if(recording==null) {text=TRANSCRIPT;complete();return;}
                recording.setVolume(.9f,.9f);
                recording.setOnCompletionListener(player->complete());
                recording.setOnErrorListener((player,what,extra)-> {text=TRANSCRIPT;recording=null;player.release();complete();return true;});
            }
            if(!recording.isPlaying())recording.start();
        } else if(scene.equals("radio_thought")) {
            if(!running) {running=true;anchor=SystemClock.uptimeMillis()-elapsed;handler.post(tick);}
            if(focus && typing==null) {
                typing=MediaPlayer.create(context,R.raw.intro_typing);
                if(typing!=null) {typing.setLooping(true);typing.setVolume(.24f,.24f);typing.setOnErrorListener((p,w,e)->{typing=null;p.release();return true;});}
            }
            if(focus && typing!=null && !typing.isPlaying())typing.start();
        }
    }
    private final Runnable tick=new Runnable() {
        @Override public void run() {
            if(!running || closed)return;
            elapsed=SystemClock.uptimeMillis()-anchor;
            if(elapsed>=text.length()*45L) {complete();return;}
            emit();handler.postDelayed(this,30);
        }
    };
    private void emit() {
        String body=text;
        if(scene.equals("radio_thought") && !ready)body=text.substring(0,(int)Math.min(text.length(),elapsed/45))+"▌";
        state.setValue(new State(active(),ready,heading,body));
    }
    private void complete() {
        ready=true;running=false;handler.removeCallbacks(tick);
        if(typing!=null){typing.release();typing=null;}
        if(recording!=null){recording.release();recording=null;}
        abandonFocus();emit();
    }
    void pause() {
        foreground=false;
        if(running)elapsed=SystemClock.uptimeMillis()-anchor;
        running=false;handler.removeCallbacks(tick);
        if(recording!=null && recording.isPlaying())recording.pause();
        if(typing!=null && typing.isPlaying())typing.pause();
        abandonFocus();
    }
    private void focusChanged(int change) {
        focus=change==AudioManager.AUDIOFOCUS_GAIN;
        if(focus)start();
        else {
            if(running)elapsed=SystemClock.uptimeMillis()-anchor;
            running=false;handler.removeCallbacks(tick);
            if(recording!=null && recording.isPlaying())recording.pause();
            if(typing!=null && typing.isPlaying())typing.pause();
        }
    }
    private void abandonFocus() {
        if(Build.VERSION.SDK_INT>=26 && request!=null)audio.abandonAudioFocusRequest(request);
        else audio.abandonAudioFocus(focusListener);focus=false;
    }
    private void release() {
        running=false;handler.removeCallbacks(tick);
        if(recording!=null){recording.release();recording=null;}
        if(typing!=null){typing.release();typing=null;}
        abandonFocus();
    }
    void hide() {release();scene="";revision=-1;ready=false;emit();}
    @Override public void close() {closed=true;release();}
}
