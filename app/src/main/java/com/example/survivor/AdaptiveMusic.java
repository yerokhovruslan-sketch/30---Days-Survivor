package com.example.survivor;

import android.content.Context;
import android.media.*;
import android.os.*;
import org.json.*;
import java.util.*;
import java.io.*;
import com.example.survivor.game.GameSession;

/** Two streamed players; gain envelopes and scheduling run only on the main looper. */
final class AdaptiveMusic implements AutoCloseable {
    private final Context app;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AudioManager audio;
    private final AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    private final AudioManager.OnAudioFocusChangeListener focusListener=this::focusChanged;
    private AudioFocusRequest request;
    private MusicPlan plan=new MusicPlan(new Random());
    private final JSONObject catalogue;
    private final android.content.SharedPreferences preferences;
    private boolean openingPending;
    private final List<Slot> slots=new ArrayList<>();
    private final Set<String> failed=new HashSet<>();
    private GameSession.Frame frame;
    private boolean foreground,paused,voice,focus,closed;
    private long stamp,clock,voiceReadyAt;
    private String applied="";
    private float duck=1;
    private long duckUntil;
    private final class Slot {
        final String track;final MediaPlayer player;final int start,end;final float gain,baseVolume;
        boolean ready;float level,from,target;long fade,progress;
        Slot(String name)throws Exception {
            baseVolume=plan.volume();track=name;JSONObject spec=catalogue.getJSONObject(name);start=spec.getInt("startMs");end=spec.getInt("endMs");gain=(float)spec.getDouble("gain");
            player=new MediaPlayer();player.setAudioAttributes(attrs);player.setLooping(false);
            int resource=app.getResources().getIdentifier("music_"+name,"raw",app.getPackageName());
            try(android.content.res.AssetFileDescriptor file=app.getResources().openRawResourceFd(resource)) {player.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());}
            player.setVolume(0,0);
            player.setOnPreparedListener(p->{if(closed || !slots.contains(this)){p.release();return;}ready=true;p.seekTo(start);if(foreground && focus && !paused && !voice){p.start();markStarted(track);}});
            player.setOnErrorListener((p,w,e)->{failed.add(track);remove(this);if(applied.equals(track))applied="";return true;});
            player.setOnCompletionListener(p->{if(target>0)plan.naturalTrackEnd(clock);});
        }
        void fade(float value,long ms){from=level;target=value;fade=Math.max(1,ms);progress=0;}
        void step(long delta){if(!ready)return;progress=Math.min(fade,progress+delta);level=from+(target-from)*Math.min(1f,progress/(float)Math.max(1,fade));if(ready){float v=Math.max(0,Math.min(1,level*gain*baseVolume*duck));player.setVolume(v,v);}}
    }
    AdaptiveMusic(Context context) {
        app=context.getApplicationContext();audio=(AudioManager)app.getSystemService(Context.AUDIO_SERVICE);
        JSONObject loaded;
        try(InputStream in=app.getResources().openRawResource(R.raw.music_catalogue);ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] data=new byte[4096];int n;while((n=in.read(data))>=0)bytes.write(data,0,n);loaded=new JSONObject(bytes.toString("UTF-8"));
        }catch(Exception error){loaded=new JSONObject();}
        catalogue=loaded;
        preferences=app.getSharedPreferences("adaptive_music_v2",Context.MODE_PRIVATE);openingPending=!preferences.getBoolean("opening_played",false);
    }
    private void markStarted(String track){if(track.equals("houslast"))preferences.edit().putBoolean("opening_played",true).apply();}
    void restart(){openingPending=!preferences.getBoolean("opening_played",false);fadeOut(1200);plan=new MusicPlan(new Random());frame=null;clock=0;duck=1;duckUntil=0;}
    void update(GameSession.Frame next){
        if(next.mode==GameSession.Mode.BUSY)return;frame=next;
        if(!next.hasPlayer || next.mode==GameSession.Mode.ENDED){fadeOut(2000);return;}
        if(openingPending && next.mode!=GameSession.Mode.MAIN)return;
        plan.update(context(next),clock,next.revision);
        if(openingPending && next.mode==GameSession.Mode.MAIN && next.home && !next.reading && next.menu.scene.isEmpty() && next.health>0){openingPending=false;plan.opening(clock);}
    }
    private MusicPlan.Context context(GameSession.Frame f){
        MusicPlan.Context c=new MusicPlan.Context();c.alive=f.health>0;c.health=f.health;c.enemies=f.status.enemies.size();
        c.archive=f.menu.board!=null && f.menu.board.terrain.contains("Архив");
        c.scripted=f.menu.board!=null && f.menu.board.storyEncounter;
        c.combat=f.menu.board!=null && f.menu.board.combatActive && c.enemies>0;
        c.danger=f.place>=10;c.cue=f.musicCue;
        if(f.home)c.area=MusicPlan.Area.HOME;
        else if(c.archive)c.area=MusicPlan.Area.ARCHIVE;
        else if(f.night)c.area=MusicPlan.Area.NIGHT;
        else if(f.place>=10 && f.place<=13 || f.place==5 || f.place==8 || f.place==18)c.area=MusicPlan.Area.MEDICAL;
        else if(f.place>=35 && f.place<=50)c.area=MusicPlan.Area.INDUSTRIAL;
        else if(f.place==2 || f.place>=19 && f.place<=32)c.area=MusicPlan.Area.NATURE;
        else c.area=MusicPlan.Area.CITY;
        return c;
    }
    void foreground(boolean value){
        foreground=value;handler.removeCallbacks(tick);stamp=SystemClock.uptimeMillis();
        if(value){if(!voice)requestFocus();resumePlayers();handler.post(tick);}else{pausePlayers();abandonFocus();}
    }
    void paused(boolean value){paused=value;if(value)pausePlayers();else{stamp=SystemClock.uptimeMillis();if(foreground){requestFocus();resumePlayers();}}}
    void voice(boolean value){
        if(voice==value)return;voice=value;
        if(value){voiceReadyAt=SystemClock.uptimeMillis()+1100;fadeOut(1000);}
        else {applied="";if(foreground){requestFocus();resumePlayers();}}
    }
    long voiceDelay(){return Math.max(0,voiceReadyAt-SystemClock.uptimeMillis());}
    boolean audible(){for(Slot s:slots)if(s.ready && s.player.isPlaying() && s.level*s.gain*s.baseVolume*duck>.01f)return true;return false;}
    void effect(){duckUntil=clock+450;}
    private void requestFocus(){
        if(closed || voice)return;
        if(Build.VERSION.SDK_INT>=26){if(request==null)request=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setOnAudioFocusChangeListener(focusListener,handler).build();focus=audio.requestAudioFocus(request)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;}
        else focus=audio.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
    private void focusChanged(int value){focus=value==AudioManager.AUDIOFOCUS_GAIN;if(focus){stamp=SystemClock.uptimeMillis();resumePlayers();}else pausePlayers();}
    private void abandonFocus(){if(Build.VERSION.SDK_INT>=26 && request!=null)audio.abandonAudioFocusRequest(request);else audio.abandonAudioFocus(focusListener);focus=false;}
    private void pausePlayers(){for(Slot s:slots)if(s.ready && s.player.isPlaying())s.player.pause();}
    private void resumePlayers(){if(!foreground || paused || voice || !focus)return;for(Slot s:slots)if(s.ready && !s.player.isPlaying() && s.player.getCurrentPosition()<s.end){s.player.start();markStarted(s.track);}}
    private void fadeOut(long ms){for(Slot s:slots)s.fade(0,ms);applied="";}
    private void remove(Slot s){slots.remove(s);s.player.setOnPreparedListener(null);s.player.setOnCompletionListener(null);s.player.setOnErrorListener(null);s.player.release();}
    private void start(String name,long fadeMs){
        if(failed.contains(name))return;
        try{
            while(slots.size()>=2){Slot quiet=slots.get(0).level<=slots.get(1).level?slots.get(0):slots.get(1);remove(quiet);}
            Slot s=new Slot(name);slots.add(s);s.fade(1,fadeMs);s.player.prepareAsync();
        }catch(Exception failure){failed.add(name);}
    }
    private final Runnable tick=new Runnable(){public void run(){
        if(closed || !foreground)return;
        long now=SystemClock.uptimeMillis(),delta=Math.min(250,Math.max(0,now-stamp));stamp=now;
        if(!paused && focus){
            if(!voice){clock+=delta;
                if(frame!=null && frame.hasPlayer && frame.mode!=GameSession.Mode.ENDED && !openingPending){MusicPlan.Context c=context(frame);c.cue="";plan.update(c,clock,-1);}
                duck+=((clock<duckUntil?.62f:1f)-duck)*Math.min(1,delta/120f);
                if(frame!=null && frame.hasPlayer && frame.mode!=GameSession.Mode.ENDED && !openingPending && !applied.equals(plan.track)){
                    String previous=applied;long fade=plan.track.isEmpty()?7000:MusicPlan.battleTrack(plan.track)?1200:5000;
                    for(Slot s:slots)s.fade(0,fade);applied=plan.track;if(!applied.isEmpty())start(applied,fade);
                }
                Slot current=null;for(Slot s:slots)if(s.target>0 && s.track.equals(applied))current=s;
                if(current!=null && current.ready && current.player.getCurrentPosition()>=current.end-7000){
                    plan.naturalTrackEnd(clock);
                }
            }
            for(Slot s:new ArrayList<>(slots)){s.step(delta);if(s.target==0 && s.progress>=s.fade)remove(s);}
        }
        handler.postDelayed(this,100);
    }};
    @Override public void close(){closed=true;handler.removeCallbacksAndMessages(null);for(Slot s:new ArrayList<>(slots))remove(s);abandonFocus();}
}
