package com.example.survivor.game;

/** Monotonic active-play time. Paused/background intervals are never accrued. */
final class ActiveClock {
    private long remainder;
    void reset(){remainder=0;}
    int tick(long nanos,boolean active) {
        if(!active)return 0;
        remainder+=Math.max(0,nanos);
        int minutes=(int)(remainder/5_000_000_000L);
        remainder%=5_000_000_000L;
        return minutes;
    }
}
