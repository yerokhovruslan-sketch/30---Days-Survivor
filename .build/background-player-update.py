from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/AdaptiveMusic.java');s=p.read_text(encoding='utf-8').replace('    private final JSONObject catalogue;', '    private final JSONObject catalogue;\n    private final android.content.SharedPreferences preferences;\n    private boolean openingPending;')
s=s.replace('if(foreground && focus && !paused && !voice)p.start();', 'if(foreground && focus && !paused && !voice){p.start();markStarted(track);}')
s=s.replace('player.setOnCompletionListener(p->{if(target>0 && plan.story){plan.naturalStoryEnd(clock);}else if(target>0)applied="";});', 'player.setOnCompletionListener(p->{if(target>0)plan.naturalTrackEnd(clock);});')
s=s.replace('        catalogue=loaded;', '        catalogue=loaded;\n        preferences=app.getSharedPreferences("adaptive_music_v2",Context.MODE_PRIVATE);openingPending=!preferences.getBoolean("opening_played",false);')
s=s.replace('    void restart(){fadeOut(1200);', '    private void markStarted(String track){if(track.equals("houslast"))preferences.edit().putBoolean("opening_played",true).apply();}\n    void restart(){openingPending=!preferences.getBoolean("opening_played",false);fadeOut(1200);')
s=s.replace('        plan.update(context(next),clock,next.revision);', '        plan.update(context(next),clock,next.revision);\n        if(openingPending && next.mode==GameSession.Mode.MAIN && next.home && !next.reading && next.menu.scene.isEmpty() && next.health>0){openingPending=false;plan.opening(clock);}')
s=s.replace('for(Slot s:slots)if(s.ready && !s.player.isPlaying())s.player.start();', 'for(Slot s:slots)if(s.ready && !s.player.isPlaying() && s.player.getCurrentPosition()<s.end){s.player.start();markStarted(s.track);}')
s=s.replace('current.player.getCurrentPosition()>=current.end-1500', 'current.player.getCurrentPosition()>=current.end-7000')
a=s.index('                    if(plan.story){plan.naturalStoryEnd(clock);}')
z=s.index('\n                }',a)
s=s[:a]+'                    plan.naturalTrackEnd(clock);'+s[z:]
p.write_text(s,encoding='utf-8')
