package com.example.survivor;

import android.content.Context;
import android.media.MediaPlayer;
import com.example.survivor.game.GameSession;

/** Intro music continues through the opening fight; the cry replaces it at the shelter. */
final class OpeningSound implements AutoCloseable {
    private final Context context;
    private MediaPlayer player;
    private boolean foreground,active;
    private String current="";
    OpeningSound(Context context){this.context=context.getApplicationContext();}
    boolean active(){return active;}
    void frame(GameSession.Frame frame){
        if(frame.mode==GameSession.Mode.BUSY)return;
        String scene=frame.menu.scene;
        if(scene.equals("opening_start") || frame.mode==GameSession.Mode.ENDED){active=false;stop();return;}
        if(scene.equals("opening_room") && !active){active=true;play("music",R.raw.opening_pursuit,true);}
        if(active && scene.equals("opening_cry")){play("cry",R.raw.opening_child_crying,false);return;}
        if(active && (scene.equals("opening_shelter") || scene.equals("opening_radio"))){active=false;stop();}
    }
    private void play(String kind,int resource,boolean loop){
        if(current.equals(kind))return;stop();current=kind;
        try{player=MediaPlayer.create(context,resource);if(player==null){current="";return;}player.setLooping(loop);if(!loop)player.setOnCompletionListener(done->{if(player==done){player=null;done.release();}});player.setVolume(kind.equals("music")?.32f:.8f,kind.equals("music")?.32f:.8f);if(foreground)player.start();}
        catch(RuntimeException ignored){stop();}
    }
    void foreground(boolean value){foreground=value;if(player==null)return;try{if(value && !player.isPlaying())player.start();else if(!value && player.isPlaying())player.pause();}catch(IllegalStateException ignored){stop();}}
    private void stop(){if(player!=null){player.release();player=null;}current="";}
    @Override public void close(){stop();}
}
