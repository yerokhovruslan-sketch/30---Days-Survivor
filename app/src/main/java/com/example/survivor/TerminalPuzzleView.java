package com.example.survivor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.example.survivor.game.GameSession;
import java.util.function.IntConsumer;

/** Full-screen document puzzle and the timed terminal exit scene. */
final class TerminalPuzzleView extends android.widget.FrameLayout {
    private final IntConsumer submit;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long revision=-1;
    private boolean pending;
    TerminalPuzzleView(Context context,IntConsumer submit){super(context);this.submit=submit;setBackgroundColor(Color.BLACK);setClickable(true);setVisibility(GONE);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    void showFrame(GameSession.Frame frame){
        if(frame==null || frame.mode==GameSession.Mode.BUSY)return;
        boolean puzzle=frame.mode==GameSession.Mode.CHOICE && frame.menu.title.equals("Восстановить грузовой маршрут");
        boolean exit=frame.mode==GameSession.Mode.CHOICE && frame.menu.title.equals("После терминала");
        if(!puzzle&&!exit){handler.removeCallbacksAndMessages(null);setVisibility(GONE);revision=-1;return;}
        if(revision==frame.revision)return;
        revision=frame.revision;pending=false;handler.removeCallbacksAndMessages(null);removeAllViews();setVisibility(VISIBLE);
        if(exit){TextView line=new TextView(getContext());line.setText(frame.menu.hint);line.setTextColor(Color.WHITE);line.setTextSize(21);line.setGravity(Gravity.CENTER);line.setPadding(dp(28),0,dp(28),0);addView(line,new LayoutParams(-1,-1));long expected=revision;handler.postDelayed(()->{if(revision==expected&&getVisibility()==VISIBLE){pending=true;submit.accept(0);}},5000);return;}
        ScrollView scroll=new ScrollView(getContext());addView(scroll,new LayoutParams(-1,-1));
        LinearLayout column=new LinearLayout(getContext());column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(16),dp(20),dp(16),dp(24));scroll.addView(column);
        TextView heading=new TextView(getContext());heading.setText("Груз, которого не было");heading.setTextColor(Color.WHITE);heading.setTextSize(23);heading.setTypeface(null,Typeface.BOLD);column.addView(heading);
        TextView hint=new TextView(getContext());hint.setText(frame.menu.hint);hint.setTextColor(0xffcccccc);hint.setTextSize(15);hint.setPadding(0,dp(10),0,dp(16));column.addView(hint);
        int count=0;for(GameSession.Choice choice:frame.choices)if(choice.number>=1&&choice.number<=12&&choice.selected)count++;
        TextView progress=new TextView(getContext());progress.setText("Выбрано: "+count+" / 4");progress.setTextColor(0xff87d49c);progress.setTextSize(15);column.addView(progress);
        LinearLayout row=null;int index=0;final int step=count;
        for(GameSession.Choice choice:frame.choices){if(choice.number<1||choice.number>12)continue;
            if(index%2==0){row=new LinearLayout(getContext());row.setOrientation(LinearLayout.HORIZONTAL);column.addView(row);}
            Button button=new Button(getContext());button.setAllCaps(false);button.setText(choice.label);button.setTextSize(14);button.setTextColor(choice.selected?Color.WHITE:Color.BLACK);button.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);button.setPadding(dp(14),dp(8),dp(14),dp(8));
            GradientDrawable bg=new GradientDrawable();bg.setColor(choice.selected?0xff28854b:Color.WHITE);bg.setCornerRadius(dp(14));button.setBackground(bg);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(92),1);lp.setMargins(dp(3),dp(6),dp(3),dp(2));row.addView(button,lp);
            final int action=choice.number;long expected=revision;button.setOnClickListener(v->{if(pending||revision!=expected)return;pending=true;int[] sequence={2,5,8,11};if(step<4&&sequence[step]!=action){button.animate().translationX(dp(9)).setDuration(55).withEndAction(()->button.animate().translationX(-dp(9)).setDuration(55).withEndAction(()->button.animate().translationX(0).setDuration(55).withEndAction(()->{if(revision==expected)submit.accept(action);}).start()).start()).start();}else submit.accept(action);});index++;
        }
        Button back=new Button(getContext());back.setAllCaps(false);back.setText("Отложить расследование");back.setOnClickListener(v->{if(!pending){pending=true;submit.accept(0);}});column.addView(back);
    }
    void close(){handler.removeCallbacksAndMessages(null);}
}
