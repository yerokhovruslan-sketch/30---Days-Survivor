package com.example.survivor;
import android.view.View;

/** One pending tap globally; detached screens never execute delayed actions. */
final class TapFeedback {
    private static boolean pending;
    static boolean run(View view,Runnable action){
        if(pending || !view.isEnabled())return false;pending=true;
        view.animate().cancel();view.animate().scaleX(1.025f).scaleY(1.025f).setDuration(90).start();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(()->{
            pending=false;view.animate().scaleX(1).scaleY(1).setDuration(120).start();
            if(view.isAttachedToWindow() && view.hasWindowFocus() && view.isEnabled())action.run();
        },200);return true;
    }
}
