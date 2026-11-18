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
    private MediaPlayer groan, weatherLoop, fireLoop, forestLoop, heartbeatLoop;
    private int weatherResource;
    private boolean wantFire, wantForest, wantHeartbeat, ambientHome, ambientNight, ambientExploring, ambientCombat;
    private boolean foreground,closed,low,zombies,sceneMuted;
    private long next,nextHurt,clock,stamp,nextGroan,nextSnort,nextEnvironmental;
    GameEffects(Context context){this(context,()->false);}
    GameEffects(Context context,BooleanSupplier musicAudible) {
        app=context.getApplicationContext();this.musicAudible=musicAudible;
        pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)ready.add(id);});
        int[] resources={R.raw.fx_click,R.raw.fx_trade,R.raw.fx_reload,R.raw.fx_shot,R.raw.fx_blunt,R.raw.fx_blade,R.raw.fx_zombie,R.raw.fx_walk,
            R.raw.fx_error,R.raw.fx_quest,R.raw.fx_save,R.raw.fx_search,R.raw.fx_bag,R.raw.fx_wood_container,R.raw.fx_metal_container,R.raw.fx_paper,R.raw.fx_wood_door,R.raw.fx_metal_door,
            R.raw.fx_concrete_steps,R.raw.fx_earth_steps,R.raw.fx_scream,R.raw.fx_zombie_death,R.raw.fx_snort,R.raw.fx_hurt,R.raw.fx_wash,R.raw.fx_brush_teeth,R.raw.ambient_house_creak,R.raw.ambient_distant_zombie,R.raw.ambient_wolf};
        for(GameSound sound:GameSound.values())samples.put(sound,pool.load(context,resources[sound.ordinal()],1));
    }
    void update(GameSession.Frame f,boolean muted) {
        boolean wasLow=low;low=f.hasPlayer && f.health>0 && f.health*5<=f.maxHealth && f.mode!=GameSession.Mode.ENDED;
        zombies=f.hasPlayer && f.health>0 && f.mode!=GameSession.Mode.ENDED && f.status.enemies.stream().anyMatch(e->e.health>0 && (e.name.contains("Зомби") || e.name.contains("Крикун") || e.name.contains("Громила") || e.name.contains("Бегун") || e.name.contains("Прыгун")));
        sceneMuted=muted;if(!low || muted)stopGroan();
        int rain=f.hasPlayer && f.rain && f.mode!=GameSession.Mode.ENDED ? (f.home?R.raw.ambient_rain_inside:R.raw.ambient_rain_outside):0;
        if(weatherResource!=rain){stopWeather();weatherResource=rain;}
        boolean active=f.hasPlayer && f.health>0 && f.mode!=GameSession.Mode.ENDED;
        wantHeartbeat=active && f.status.bleedingDistress(f.health,f.maxHealth);
        wantFire=active && f.fire;
        ambientHome=active && f.home;
        ambientNight=f.night;
        ambientExploring=active && !f.home;
        ambientCombat=f.menu.board!=null && f.menu.board.combatActive && !f.status.enemies.isEmpty();
        wantForest=ambientExploring && (f.place==2 || f.place>=19 && f.place<=26);
        if(nextEnvironmental==0)nextEnvironmental=clock+35000+random.nextInt(30001);
        syncLoops();
        if(low && !wasLow)nextGroan=clock;
        if(muted)clearEvents();
    }
    void resume() {foreground=true;syncLoops();stamp=SystemClock.uptimeMillis();handler.removeCallbacks(ambience);handler.post(ambience);if(groan!=null && low && !sceneMuted)groan.start();}
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
            case SCREAM->musicAudible.getAsBoolean()?1f:.6f;case ZOMBIE_DEATH->.7f;case SNORT->.35f;case HURT->.8f;case WASH,BRUSH_TEETH->.6f;case HOUSE_CREAK->.16f;case DISTANT_ZOMBIE->.12f;case WOLF->.16f;
        };
        int stream=pool.play(id,volume,volume,sound==GameSound.CLICK?0:sound==GameSound.HURT?3:1,0,1f);
        if(stream!=0)streams.add(stream);if(streams.size()>96)streams.remove(0);
    }
    private void syncLoops(){
        if(!foreground || closed || sceneMuted){stopWeatherPlayer();stopFire();stopForest();stopHeartbeat();return;}
        if(weatherResource!=0 && weatherLoop==null){weatherLoop=MediaPlayer.create(app,weatherResource);if(weatherLoop!=null){weatherLoop.setLooping(true);weatherLoop.setVolume(.22f,.22f);weatherLoop.start();}}
        if(wantFire && fireLoop==null){fireLoop=MediaPlayer.create(app,R.raw.ambient_fire);if(fireLoop!=null){fireLoop.setLooping(true);fireLoop.setVolume(.28f,.28f);fireLoop.start();}}
        if(!wantFire)stopFire();
        if(wantForest && forestLoop==null){forestLoop=MediaPlayer.create(app,R.raw.ambient_forest_loop);if(forestLoop!=null){forestLoop.setLooping(true);forestLoop.setVolume(.11f,.11f);forestLoop.start();}}
        if(!wantForest)stopForest();
        if(wantHeartbeat && heartbeatLoop==null){heartbeatLoop=MediaPlayer.create(app,R.raw.ambient_heartbeat);if(heartbeatLoop!=null){heartbeatLoop.setLooping(true);heartbeatLoop.setVolume(.35f,.35f);heartbeatLoop.start();}}
        if(!wantHeartbeat)stopHeartbeat();
    }
    private void stopWeatherPlayer(){if(weatherLoop!=null){weatherLoop.release();weatherLoop=null;}}
    private void stopWeather(){stopWeatherPlayer();weatherResource=0;}
    private void stopFire(){if(fireLoop!=null){fireLoop.release();fireLoop=null;}}
    private void stopForest(){if(forestLoop!=null){forestLoop.release();forestLoop=null;}}
    private void stopHeartbeat(){if(heartbeatLoop!=null){heartbeatLoop.release();heartbeatLoop=null;}}
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
            if(!ambientCombat && clock>=nextEnvironmental && (ambientHome || ambientExploring)){
                GameSound cue=null;int choice=random.nextInt(100);
                if(ambientHome){if(ambientNight && choice<30)cue=GameSound.WOLF;else if(choice<70)cue=GameSound.HOUSE_CREAK;else cue=GameSound.DISTANT_ZOMBIE;}
                else if(wantForest && ambientNight && choice<30)cue=GameSound.WOLF;
                else if(choice<55)cue=GameSound.DISTANT_ZOMBIE;
                if(cue!=null)play(cue);
                nextEnvironmental=clock+80000+random.nextInt(100001);
            }
            if(zombies && clock>=nextSnort){play(GameSound.SNORT);nextSnort=clock+18000+random.nextInt(17001);}else if(!zombies)nextSnort=clock+18000+random.nextInt(17001);
        }
        handler.postDelayed(this,250);
    }};
    private void clearEvents(){handler.removeCallbacksAndMessages(null);for(int stream:streams)pool.stop(stream);streams.clear();next=0;nextHurt=0;if(foreground)handler.post(ambience);}
    void clear(){clearEvents();stopGroan();stopWeather();stopFire();stopForest();stopHeartbeat();wantFire=false;wantForest=false;wantHeartbeat=false;ambientHome=false;ambientExploring=false;nextEnvironmental=0;low=false;zombies=false;sceneMuted=false;}
    void pause(){foreground=false;stopWeatherPlayer();stopFire();stopForest();stopHeartbeat();clearEvents();if(groan!=null && groan.isPlaying())groan.pause();}
    @Override public void close(){closed=true;foreground=false;clearEvents();stopGroan();stopWeather();stopFire();stopForest();stopHeartbeat();pool.release();}
}
