package com.example.survivor;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.SystemClock;

/** Audio is tied to the countdown lifecycle, never to the Activity lifecycle. */
final class OpeningTimerAudio {
    private final Context context;
    private final SoundPool hearts;
    private final int heartbeat;
    private MediaPlayer timer;
    private long nextBeat;

    OpeningTimerAudio(Context context) {
        this.context=context.getApplicationContext();
        hearts=new SoundPool.Builder().setMaxStreams(2).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        heartbeat=hearts.load(context,R.raw.opening_heartbeat,1);
    }
    void start(long offset) {
        try {
            if(timer==null){timer=MediaPlayer.create(context,R.raw.opening_timer);if(timer==null)return;timer.setVolume(.30f,.30f);timer.setLooping(true);}
            int duration=timer.getDuration();timer.seekTo((int)(Math.max(0,offset)%Math.max(1,duration)));timer.start();
        } catch(IllegalStateException ignored){stop();}
    }
    void heartbeat(long remaining) {
        long now=SystemClock.uptimeMillis();
        if(remaining>0 && remaining<=3000 && now>=nextBeat){
            hearts.play(heartbeat,.25f,.25f,0,0,1f);
            nextBeat=now+(remaining<=1000?330:remaining<=2000?450:600);
        }
    }
    void pause(){if(timer!=null)try{if(timer.isPlaying())timer.pause();}catch(IllegalStateException ignored){stop();}hearts.autoPause();}
    void stop(){if(timer!=null){timer.release();timer=null;}hearts.autoPause();nextBeat=0;}
    void close(){stop();hearts.release();}
}
