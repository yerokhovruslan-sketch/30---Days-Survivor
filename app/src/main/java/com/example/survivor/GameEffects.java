package com.example.survivor;

import android.content.Context;
import android.media.*;
import android.os.*;
import com.example.survivor.game.GameSound;
import com.example.survivor.game.GameSession;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Discrete events are queued once; low-health audio streams to avoid SoundPool truncation. */
final class GameEffects implements AutoCloseable {
    private final Context app;
    private final BooleanSupplier musicAudible;
    private final SoundPool pool=new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final EnumMap<GameSound,Integer> samples=new EnumMap<>(GameSound.class);
    private final Set<Integer> ready=new HashSet<>();
    private final List<Integer> streams=new ArrayList<>();
    private final Random random=new Random();
    private MediaPlayer groan;
    private boolean foreground,closed,low,zombies,sceneMuted;
    private long next,nextHurt,clock,stamp,nextGroan,nextSnort;
    GameEffects(Context context){this(context,()->false);}
    GameEffects(Context context,BooleanSupplier musicAudible) {
        app=context.getApplicationContext();this.musicAudible=musicAudible;
        pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)ready.add(id);});
        int[] resources={R.raw.fx_click,R.raw.fx_trade,R.raw.fx_reload,R.raw.fx_shot,R.raw.fx_blunt,R.raw.fx_blade,R.raw.fx_zombie,R.raw.fx_walk,
            R.raw.fx_error,R.raw.fx_quest,R.raw.fx_save,R.raw.fx_search,R.raw.fx_bag,R.raw.fx_wood_container,R.raw.fx_metal_container,R.raw.fx_paper,R.raw.fx_wood_door,R.raw.fx_metal_door,
            R.raw.fx_concrete_steps,R.raw.fx_earth_steps,R.raw.fx_scream,R.raw.fx_zombie_death,R.raw.fx_snort,R.raw.fx_hurt};
        for(GameSound sound:GameSound.values())samples.put(sound,pool.load(context,resources[sound.ordinal()],1));
    }
    void update(GameSession.Frame f,boolean muted) {
        boolean wasLow=low;low=f.hasPlayer && f.health>0 && f.health*5<=f.maxHealth && f.mode!=GameSession.Mode.ENDED;
        zombies=f.hasPlayer && f.health>0 && f.mode!=GameSession.Mode.ENDED && f.status.enemies.stream().anyMatch(e->e.health>0 && (e.name.contains("Зомби") || e.name.contains("Крикун") || e.name.contains("Громила") || e.name.contains("Бегун") || e.name.contains("Прыгун")));
        sceneMuted=muted;if(!low || muted)stopGroan();
        if(low && !wasLow)nextGroan=clock;
        if(muted)clearEvents();
    }
    void resume() {foreground=true;stamp=SystemClock.uptimeMillis();handler.removeCallbacks(ambience);handler.post(ambience);if(groan!=null && low && !sceneMuted)groan.start();}
    void play(GameSound sound) {
        if(!foreground || closed || sceneMuted)return;
        if(sound==GameSound.CLICK || sound==GameSound.ERROR || sound==GameSound.SAVE){start(sound);return;}
        long now=SystemClock.uptimeMillis();long when;
        if(sound==GameSound.HURT){when=Math.max(Math.max(now,Math.min(next-210,now+1400)),nextHurt);nextHurt=when+400;}
        else {when=Math.max(now,Math.min(next,now+1400));next=when+210;}
        handler.postAtTime(()->{if(foreground && !closed && !sceneMuted)start(sound);},when);
    }
    private void start(GameSound sound) {
        int id=samples.get(sound);if(!ready.contains(id))return;
        float volume=switch(sound){
            case CLICK->.24f;case TRADE->.55f;case RELOAD->.65f;case SHOT->.85f;case BLUNT,BLADE->.75f;case ZOMBIE->.65f;
            case WALK,CONCRETE_STEPS,EARTH_STEPS->.55f;case ERROR->.45f;case QUEST->.55f;case SAVE->.16f;
            case SEARCH,BAG,PAPER->.55f;case WOOD_CONTAINER,METAL_CONTAINER,WOOD_DOOR,METAL_DOOR->.65f;
            case SCREAM->musicAudible.getAsBoolean()?1f:.6f;case ZOMBIE_DEATH->.7f;case SNORT->.35f;case HURT->.8f;
        };
        int stream=pool.play(id,volume,volume,sound==GameSound.CLICK?0:sound==GameSound.HURT?3:1,0,1f);
        if(stream!=0)streams.add(stream);if(streams.size()>96)streams.remove(0);
    }
    private void startGroan(){
        if(groan!=null)return;
        groan=MediaPlayer.create(app,R.raw.fx_low_health);if(groan==null){nextGroan=clock+10000;return;}
        groan.setLooping(false);groan.setVolume(.5f,.5f);
        groan.setOnCompletionListener(p->{stopGroan();nextGroan=clock+10000;});
        groan.setOnErrorListener((p,w,e)->{stopGroan();nextGroan=clock+10000;return true;});groan.start();
    }
    private void stopGroan(){if(groan!=null){groan.setOnCompletionListener(null);groan.setOnErrorListener(null);groan.release();groan=null;}}
    private final Runnable ambience=new Runnable(){public void run(){
        if(!foreground || closed)return;long now=SystemClock.uptimeMillis();clock+=Math.min(1000,Math.max(0,now-stamp));stamp=now;
        if(!sceneMuted){if(low && groan==null && clock>=nextGroan)startGroan();
            if(zombies && clock>=nextSnort){play(GameSound.SNORT);nextSnort=clock+18000+random.nextInt(17001);}else if(!zombies)nextSnort=clock+18000+random.nextInt(17001);
        }
        handler.postDelayed(this,250);
    }};
    private void clearEvents(){handler.removeCallbacksAndMessages(null);for(int stream:streams)pool.stop(stream);streams.clear();next=0;nextHurt=0;if(foreground)handler.post(ambience);}
    void clear(){clearEvents();stopGroan();low=false;zombies=false;sceneMuted=false;}
    void pause(){foreground=false;clearEvents();if(groan!=null && groan.isPlaying())groan.pause();}
    @Override public void close(){closed=true;foreground=false;clearEvents();stopGroan();pool.release();}
}
