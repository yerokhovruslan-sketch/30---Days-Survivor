package com.example.survivor;

import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.example.survivor.game.GameSession;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Difficulty selection keeps the game session waiting until the player starts. */
final class DifficultySelectionView extends FrameLayout {
    private final IntConsumer choose;
    private final LinearLayout options;
    private final List<GameSession.Choice> levels=new ArrayList<>();
    private int selected;
    private boolean submitted;
    private boolean changing;
    private View expandedDetail;
    private TextView startButton;
    private View blackout;
    private android.media.MediaPlayer thunder;
    DifficultySelectionView(Context context, IntConsumer choose){
        super(context);this.choose=choose;setVisibility(GONE);setClickable(true);setBackgroundColor(Color.BLACK);
        ImageView background=new ImageView(context);background.setImageResource(R.drawable.opening_start);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);addView(background,new LayoutParams(-1,-1));
        View shade=new View(context);shade.setBackgroundColor(0xdd080706);addView(shade,new LayoutParams(-1,-1));
        LinearLayout outer=new LinearLayout(context);outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(16),dp(12),dp(16),dp(12));addView(outer,new LayoutParams(-1,-1));
        TextView title=text("SURVIVOR",30,0xffe5d7c6,true);title.setGravity(Gravity.CENTER);outer.addView(title);
        TextView heading=text("ВЫБОР СЛОЖНОСТИ",21,0xffe5d7c6,true);heading.setGravity(Gravity.CENTER);heading.setLetterSpacing(.12f);outer.addView(heading);
        TextView hint=text("От сложности зависит урон, количество ресурсов и скорость роста потребностей.",14,0xffc1b1a1,false);
        hint.setGravity(Gravity.CENTER);hint.setPadding(0,dp(8),0,dp(8));outer.addView(hint);
        LinearLayout info=new LinearLayout(context);info.setGravity(Gravity.CENTER_VERTICAL);info.setPadding(dp(8),dp(5),dp(8),dp(5));info.setBackground(border(0xbb171513,0xff655447));
        info.addView(icon(R.drawable.difficulty_icon_info,23));TextView warning=text("После начала игры изменить сложность нельзя. Для другого уровня потребуется новая игра.",13,0xffd2c1ae,false);
        LinearLayout.LayoutParams warningParams=new LinearLayout.LayoutParams(0,-2,1);warningParams.leftMargin=dp(8);info.addView(warning,warningParams);outer.addView(info);
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(false);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        options=new LinearLayout(context);options.setOrientation(LinearLayout.VERTICAL);options.setPadding(0,dp(8),0,dp(8));
        LayoutTransition transition=new LayoutTransition();transition.enableTransitionType(LayoutTransition.CHANGING);transition.setDuration(250);options.setLayoutTransition(transition);scroll.addView(options);
        LinearLayout footer=new LinearLayout(context);footer.setGravity(Gravity.CENTER_VERTICAL);outer.addView(footer);
        ImageView back=icon(R.drawable.difficulty_icon_back,42);back.setContentDescription("Назад в главное меню");back.setBackground(border(0xee171411,0xff655447));footer.addView(back);back.setOnClickListener(v->{if(!submitted){submitted=true;choose.accept(0);}});
        TextView start=text("НАЧАТЬ ИГРУ",20,0xff211b15,true);start.setGravity(Gravity.CENTER);start.setPadding(dp(12),dp(12),dp(12),dp(12));start.setBackground(border(0xffc9b49b,0xffebd3b2));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,-2,1);sp.leftMargin=dp(12);footer.addView(start,sp);startButton=start;
        blackout=new View(context);blackout.setBackgroundColor(Color.BLACK);blackout.setAlpha(0f);blackout.setVisibility(GONE);addView(blackout,new LayoutParams(-1,-1));
        start.setOnClickListener(v->{if(selected==0||submitted)return;submitted=true;blackout.setVisibility(VISIBLE);blackout.setClickable(true);
            try{thunder=android.media.MediaPlayer.create(getContext(),R.raw.difficulty_thunder);if(thunder!=null){thunder.setOnCompletionListener(player->{player.release();if(thunder==player)thunder=null;});thunder.start();}}catch(RuntimeException ignored){if(thunder!=null){thunder.release();thunder=null;}}
            blackout.animate().alpha(1f).setDuration(900).withEndAction(()->blackout.postDelayed(()->choose.accept(selected),450)).start();
        });
    }
    @Override protected void onDetachedFromWindow(){if(thunder!=null){thunder.release();thunder=null;}super.onDetachedFromWindow();}
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable border(int color,int line){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setStroke(dp(1),line);d.setCornerRadius(dp(8));return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(getContext());t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private ImageView icon(int res,int size){ImageView v=new ImageView(getContext());v.setImageResource(res);v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return v;}
    void show(GameSession.Frame frame){
        levels.clear();for(GameSession.Choice c:frame.choices)if(c.number>=1&&c.number<=3)levels.add(c);
        blackout.animate().cancel();blackout.setAlpha(0f);blackout.setVisibility(GONE);submitted=false;rebuild(false);
    }
    private void rebuild(boolean animate){
        options.removeAllViews();expandedDetail=null;
        startButton.setAlpha(selected==0?.45f:1f);
        for(GameSession.Choice choice:levels){
            LinearLayout group=new LinearLayout(getContext());group.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams groupParams=new LinearLayout.LayoutParams(-1,-2);groupParams.bottomMargin=dp(9);options.addView(group,groupParams);
            FrameLayout card=new FrameLayout(getContext());
            card.setBackground(border(0xff171411,Color.TRANSPARENT));
            card.setClipToOutline(true);
            card.setForeground(border(Color.TRANSPARENT,choice.number==selected?0xffd9a471:0xff5e554e));
            card.setContentDescription(choice.label);card.setClickable(true);card.setFocusable(true);group.addView(card,new LinearLayout.LayoutParams(-1,dp(88)));
            LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);card.addView(row,new FrameLayout.LayoutParams(-1,-1));
            FrameLayout artwork=new FrameLayout(getContext());row.addView(artwork,new LinearLayout.LayoutParams(dp(130),-1));
            ImageView art=new ImageView(getContext());art.setImageResource(choice.number==1?R.drawable.difficulty_story:choice.number==2?R.drawable.difficulty_survival:R.drawable.difficulty_last);
            art.setScaleType(ImageView.ScaleType.CENTER_CROP);artwork.addView(art,new FrameLayout.LayoutParams(-1,-1));
            View fade=new View(getContext());fade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0x00171411,0x00171411,0xff171411}));artwork.addView(fade,new FrameLayout.LayoutParams(-1,-1));
            TextView name=text(choice.label.toUpperCase(java.util.Locale.ROOT),18,0xffe7d8c5,true);name.setPadding(0,0,dp(6),0);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            if(choice.number==selected){ImageView mark=icon(R.drawable.difficulty_icon_selected,30);LinearLayout.LayoutParams m=(LinearLayout.LayoutParams)mark.getLayoutParams();m.rightMargin=dp(10);row.addView(mark,m);}
            if(choice.number==selected){
                LinearLayout detail=new LinearLayout(getContext());detail.setOrientation(LinearLayout.VERTICAL);detail.setPadding(dp(12),dp(10),dp(12),dp(12));detail.setBackground(border(0xee171411,0xff5e554e));group.addView(detail);expandedDetail=detail;
                TextView description=text(choice.detail,15,0xffe4d7c8,false);description.setLineSpacing(dp(3),1.08f);detail.addView(description);
                LinearLayout stats=new LinearLayout(getContext());stats.setPadding(0,dp(10),0,0);detail.addView(stats);
                String[] labels=choice.number==1?new String[]{"Урон ниже","Добычи больше","Потребности медленнее"}:choice.number==2?new String[]{"Обычный урон","Обычная добыча","Обычные потребности"}:new String[]{"Урон выше","Добычи меньше","Потребности быстрее"};
                int[] icons={R.drawable.difficulty_icon_damage,R.drawable.difficulty_icon_resources,R.drawable.difficulty_icon_needs};
                for(int i=0;i<3;i++){LinearLayout stat=new LinearLayout(getContext());stat.setGravity(Gravity.CENTER_VERTICAL);stats.addView(stat,new LinearLayout.LayoutParams(0,-2,1));stat.addView(icon(icons[i],21));TextView label=text(labels[i],11,0xffd4c1ae,false);label.setPadding(dp(4),0,dp(2),0);stat.addView(label,new LinearLayout.LayoutParams(0,-2,1));}
                if(animate){detail.setAlpha(0);detail.setTranslationY(-dp(6));detail.animate().alpha(1).translationY(0).setDuration(250).start();}
            }
            card.setOnClickListener(v->{
                if(submitted||changing||selected==choice.number)return;
                if(expandedDetail==null){selected=choice.number;rebuild(true);return;}
                changing=true;View old=expandedDetail;
                old.animate().alpha(0).translationY(-dp(6)).setDuration(160).withEndAction(()->{
                    selected=choice.number;changing=false;rebuild(true);
                }).start();
            });
        }
    }
}
