package com.example.survivor;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.lifecycle.MutableLiveData;

/** Retained first-run introduction. No game session starts until continue is pressed. */
final class IntroSequence implements AutoCloseable {
    private final Context context;
    private final SharedPreferences preferences;
    private final AudioManager audio;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    private final AudioManager.OnAudioFocusChangeListener focusListener = this::focusChanged;
    private AudioFocusRequest focusRequest;
    private MediaPlayer recording, typing;
    private boolean foreground, focus, closed, running, audioEnded, typingFailed;
    private long anchor;
    private int elapsed, recordingDuration;
    final MutableLiveData<Integer> progress;
    final MutableLiveData<Boolean> visible;

    IntroSequence(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences("evacuation_intro_v1", Context.MODE_PRIVATE);
        elapsed = Math.max(0, Math.min(IntroTimeline.END, preferences.getInt("position", 0)));
        visible = new MutableLiveData<>(!preferences.getBoolean("completed", false));
        progress = new MutableLiveData<>(elapsed);
        audio = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
    }

    void resume() {
        foreground = true;
        if (closed || !Boolean.TRUE.equals(visible.getValue()) || IntroTimeline.ready(elapsed)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest == null) focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(focusListener, handler).build();
            focus = audio.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else focus = audio.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (focus) start();
    }

    private void start() {
        if (!foreground || !focus || closed || running || IntroTimeline.ready(elapsed)) return;
        if (recording == null && !audioEnded) {
            recording = MediaPlayer.create(context, R.raw.evacuation_intro);
            if (recording != null) {
                recording.setVolume(0.85f, 0.85f);
                recordingDuration = recording.getDuration();
                recording.setOnCompletionListener(player -> {
                    elapsed = Math.min(IntroTimeline.END, Math.max(elapsed, recordingDuration));
                    audioEnded = true;
                    anchor = SystemClock.uptimeMillis() - elapsed;
                });
                recording.setOnErrorListener((player, what, extra) -> {
                    audioEnded = true;
                    anchor = SystemClock.uptimeMillis() - elapsed;
                    recording = null;
                    player.release();
                    return true;
                });
                if (elapsed < recordingDuration) {
                    recording.seekTo(elapsed);
                    recording.setOnSeekCompleteListener(player -> {
                        if (foreground && focus && !closed && !running) beginTicks();
                    });
                    return;
                }
            }
            audioEnded = true;
        }
        beginTicks();
    }

    private void beginTicks() {
        running = true;
        anchor = SystemClock.uptimeMillis() - elapsed;
        if (recording != null && !audioEnded) recording.start();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running || closed) return;
            if (recording != null && !audioEnded) elapsed = Math.max(elapsed, recording.getCurrentPosition());
            else elapsed = Math.max(elapsed, (int) (SystemClock.uptimeMillis() - anchor));
            elapsed = Math.min(IntroTimeline.END, elapsed);
            progress.setValue(elapsed);
            if (IntroTimeline.ready(elapsed)) {
                stop();
                releasePlayers();
                abandonFocus();
                persist();
                return;
            }
            updateTyping();
            handler.postDelayed(this, 30);
        }
    };

    private void updateTyping() {
        if (elapsed < IntroTimeline.PICTURE_AT || typingFailed) return;
        if (typing == null) {
            typing = MediaPlayer.create(context, R.raw.intro_typing);
            if (typing != null) {
                typing.setLooping(true);
                typing.setVolume(0.28f, 0.28f);
                typing.setOnErrorListener((player, what, extra) -> {
                    typingFailed = true;
                    typing = null;
                    player.release();
                    return true;
                });
            }
        }
        if (typing != null && !typing.isPlaying()) typing.start();
    }

    private void stop() {
        if (running) {
            if (recording != null && !audioEnded) elapsed = Math.max(elapsed, recording.getCurrentPosition());
            else elapsed = Math.min(IntroTimeline.END, Math.max(elapsed, (int) (SystemClock.uptimeMillis() - anchor)));
        }
        running = false;
        handler.removeCallbacks(tick);
        if (recording != null && recording.isPlaying()) recording.pause();
        if (typing != null && typing.isPlaying()) typing.pause();
    }

    void pause() {
        foreground = false;
        stop();
        persist();
        abandonFocus();
    }

    private void focusChanged(int change) {
        if (closed) return;
        focus = change == AudioManager.AUDIOFOCUS_GAIN;
        if (focus && foreground) start(); else { stop(); persist(); }
    }

    private void persist() { preferences.edit().putInt("position", elapsed).apply(); }

    boolean continueToGame() {
        if (!IntroTimeline.ready(elapsed) || !Boolean.TRUE.equals(visible.getValue())) return false;
        preferences.edit().putBoolean("completed", true).putInt("position", IntroTimeline.END).apply();
        visible.setValue(false);
        return true;
    }

    private void abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) audio.abandonAudioFocusRequest(focusRequest);
        else audio.abandonAudioFocus(focusListener);
        focus = false;
    }

    private void releasePlayers() {
        if (recording != null) { recording.release(); recording = null; }
        if (typing != null) { typing.release(); typing = null; }
    }

    @Override public void close() {
        pause(); closed = true; releasePlayers();
    }
}
