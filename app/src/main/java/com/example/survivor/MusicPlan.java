package com.example.survivor;

import java.util.*;

/** Active-play music policy. Tracks play forward once and then fade into a timed pause. */
final class MusicPlan {
    enum Area { HOME,CITY,MEDICAL,INDUSTRIAL,NATURE,NIGHT,ARCHIVE }
    static final class Context {
        Area area;boolean combat,archive,scripted,danger,alive=true;int enemies,health=100;String cue="";
    }
    private final Random random;
    Context context=new Context();
    private Area lastArea;
    String track="",lastTrack="",lastBattleTrack="";
    long due,windowEnd=Long.MAX_VALUE,homeSince=-1,homeBattleUntil=-1,postBattleUntil=-1;
    boolean fighting,battleStarted,escalated,story,hasBattleTrack;
    private long lastCue=-1,battleAllowedAt;
    private int encounter,previousBattle=-4;
    int battleInitial;
    MusicPlan(Random random){this.random=random;}
    long silence(){return 60_000L+random.nextInt(60_001);}
    static boolean battleTrack(String t){return Arrays.asList("anthem","countdown","frenzy","fury","wasteland","hunted").contains(t);}
    void silenceFrom(long now){track="";story=false;due=now+silence()+7_000;windowEnd=Long.MAX_VALUE;}
    void opening(long now){track="houslast";lastTrack=track;story=false;windowEnd=Long.MAX_VALUE;due=0;}
    private boolean eligible(long now){return now>=battleAllowedAt;}
    private void startBattle(Context c,long now){
        String[] choices=c.archive || c.scripted?new String[]{"countdown","anthem","hunted","fury"}:c.enemies>=4?new String[]{"wasteland","frenzy","fury","hunted","anthem"}:new String[]{"anthem","fury","hunted"};
        List<String> pool=new ArrayList<>(Arrays.asList(choices));pool.remove(lastBattleTrack);
        track=pool.get(random.nextInt(pool.size()));lastBattleTrack=track;story=false;hasBattleTrack=true;previousBattle=encounter;battleAllowedAt=now+90_000;windowEnd=Long.MAX_VALUE;
    }
    void update(Context next,long now,long revision){
        Area previous=lastArea;lastArea=next.area;context=next;
        if(!next.alive){track="";due=Long.MAX_VALUE;return;}
        if(next.area==Area.HOME && homeSince<0)homeSince=now;
        if(next.area!=Area.HOME)homeSince=-1;
        if(!next.cue.isEmpty() && lastCue!=revision){
            lastCue=revision;
            if(next.cue.equals("silence")){silenceFrom(now);return;}
            // A successful retreat is no longer a reason to force another combat track.
            if(!battleTrack(next.cue)){track=next.cue;story=true;windowEnd=Long.MAX_VALUE;return;}
        }
        if(next.combat && !fighting){
            fighting=true;battleStarted=true;escalated=false;battleInitial=next.enemies;encounter++;hasBattleTrack=false;homeBattleUntil=-1;postBattleUntil=-1;
            int chance=next.archive || next.scripted?50:next.enemies>=4?65:10;
            if(eligible(now) && (next.archive || next.scripted || next.enemies>=4 || encounter-previousBattle>=4) && random.nextInt(100)<chance)startBattle(next,now);
        }
        if(!next.combat && fighting){
            fighting=false;
            if(battleTrack(track)){
                story=false;if(next.area==Area.HOME)homeBattleUntil=now+5_000+random.nextInt(5_001);else postBattleUntil=now+5_000+random.nextInt(5_001);
            }
        }
        if(fighting){
            if(!escalated && next.enemies>=battleInitial+2){escalated=true;if(!hasBattleTrack && eligible(now))startBattle(next,now);}
            if(track.isEmpty() && now>=due)pick(now);
            return;
        }
        if(next.area==Area.HOME && previous!=Area.HOME && battleTrack(track) && homeBattleUntil<0){homeBattleUntil=now+5_000+random.nextInt(5_001);postBattleUntil=-1;story=false;}
        if(postBattleUntil>=0){if(now<postBattleUntil)return;postBattleUntil=-1;silenceFrom(now);}
        if(homeBattleUntil>=0){if(now<homeBattleUntil)return;homeBattleUntil=-1;silenceFrom(now);}
        // Backgrounds span menus and regions. Dreams alone fades out when leaving home.
        if(previous==Area.HOME && next.area!=Area.HOME && track.equals("dreams"))pick(now);
        if(next.area!=Area.NIGHT && track.equals("echoes"))pick(now);
        if(track.isEmpty() && due==0)due=now+silence();
        if(track.isEmpty() && now>=due)pick(now);
    }
    void pick(long now){
        String[] regional=switch(context.area){
            case HOME->new String[]{"dreams","ashfall","wanderer","houslast"};
            case MEDICAL->new String[]{"decay","forgotten"};
            case INDUSTRIAL,ARCHIVE->new String[]{"bunker","tense"};
            case NIGHT->new String[]{"echoes","graveyard","wanderer"};
            case NATURE->new String[]{"ash","light","wanderer"};
            default->new String[]{"concrete","ash","wanderer"};
        };
        String[] global={"wanderer","wanderer","wanderer","houslast","tense","ashfall","ashfall","elegy","forgotten","fading"};
        List<String> pool=new ArrayList<>(Arrays.asList(random.nextInt(100)<65?global:regional));pool.removeIf(t->t.equals(lastTrack));
        if(pool.isEmpty())pool.add("wanderer");
        track=pool.get(random.nextInt(pool.size()));lastTrack=track;story=false;windowEnd=Long.MAX_VALUE;due=0;
    }
    void naturalTrackEnd(long now){silenceFrom(now);}
    void naturalStoryEnd(long now){naturalTrackEnd(now);}
    float volume(){if(battleTrack(track))return .65f;return .65f*(story?.75f:context.area==Area.HOME?.65f:.75f);}
}
