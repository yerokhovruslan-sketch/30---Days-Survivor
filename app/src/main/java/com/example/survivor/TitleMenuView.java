package com.example.survivor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.survivor.game.GameSession;
import java.util.function.IntConsumer;

/** The title screen; choices are owned by the existing game session. */
final class TitleMenuView extends FrameLayout {
    private final IntConsumer choose;
    private final LinearLayout actions;
    private boolean submitted;

    TitleMenuView(Context context, IntConsumer choose) {
        super(context);
        this.choose=choose;
        setBackgroundColor(Color.BLACK);
        setClickable(true);
        ImageView background=new ImageView(context);
        background.setImageResource(R.drawable.opening_start);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(background,new LayoutParams(-1,-1));
        View shade=new View(context);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x33000000,0x11000000,0x99000000}));
        addView(shade,new LayoutParams(-1,-1));
        LinearLayout column=new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(dp(30),0,dp(30),0);
        addView(column,new LayoutParams(-1,-1));
        View top=new View(context);
        column.addView(top,new LinearLayout.LayoutParams(1,Math.round(getResources().getDisplayMetrics().heightPixels*.095f)));
        TextView title=new TextView(context);
        title.setText("SURVIVOR");title.setTextColor(0xffe4d7c3);
        title.setTypeface(Typeface.create("sans-serif-condensed-black",Typeface.BOLD));
        title.setGravity(Gravity.CENTER);title.setTextSize(49);title.setLetterSpacing(.035f);
        title.setShadowLayer(dp(3),0,dp(2),Color.BLACK);
        column.addView(title,new LinearLayout.LayoutParams(-1,-2));
        TextView subtitle=new TextView(context);
        subtitle.setText("—   Т Ы  Т У Т  О Д И Н   —");subtitle.setTextColor(0xffd1bba4);
        subtitle.setTypeface(Typeface.create("sans-serif",Typeface.ITALIC));
        subtitle.setGravity(Gravity.CENTER);subtitle.setTextSize(12);
        column.addView(subtitle,new LinearLayout.LayoutParams(-1,-2));
        actions=new LinearLayout(context);actions.setOrientation(LinearLayout.VERTICAL);
        LayoutParams actionsParams=new LayoutParams(-1,-2,Gravity.CENTER);
        actionsParams.leftMargin=dp(30);actionsParams.rightMargin=dp(30);
        addView(actions,actionsParams);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private Bitmap crop(int resource,int x,int y,int width,int height){
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        Bitmap source=BitmapFactory.decodeResource(getResources(),resource,options);
        Bitmap result=Bitmap.createBitmap(source,x,y,width,height);
        if(result!=source)source.recycle();
        return result;
    }
    void show(GameSession.Frame frame){
        submitted=false;actions.removeAllViews();
        for(GameSession.Choice choice:frame.choices){
            if(choice.number<1||choice.number>3)continue;
            boolean primary=choice.number==1;
            FrameLayout button=new FrameLayout(getContext());
            button.setBackground(new BitmapDrawable(getResources(),primary
                    ?crop(R.drawable.title_button_primary,95,175,1985,405)
                    :crop(R.drawable.title_button_secondary,68,168,2030,385)));
            button.setEnabled(choice.enabled);button.setAlpha(choice.enabled?1f:.48f);
            button.setClickable(choice.enabled);button.setFocusable(choice.enabled);
            button.setContentDescription(choice.label+(choice.detail.isEmpty()?"":". "+choice.detail));
            LinearLayout.LayoutParams row=new LinearLayout.LayoutParams(-1,dp(68));row.topMargin=dp(9);
            actions.addView(button,row);
            ImageView icon=new ImageView(getContext());
            Bitmap image=switch(choice.number){
                case 1 -> crop(R.drawable.title_icon_new,245,140,765,965);
                case 2 -> crop(R.drawable.title_icon_continue,168,216,915,885);
                default -> crop(R.drawable.title_icon_load,208,296,865,675);
            };
            icon.setImageBitmap(image);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LayoutParams iconParams=new LayoutParams(dp(38),dp(38),Gravity.START|Gravity.CENTER_VERTICAL);
            iconParams.leftMargin=dp(24);button.addView(icon,iconParams);
            ImageView divider=new ImageView(getContext());
            divider.setImageBitmap(crop(R.drawable.title_divider,336,332,48,1540));
            divider.setScaleType(ImageView.ScaleType.FIT_XY);
            LayoutParams div=new LayoutParams(dp(2),dp(38),Gravity.START|Gravity.CENTER_VERTICAL);
            div.leftMargin=dp(78);button.addView(divider,div);
            LinearLayout labels=new LinearLayout(getContext());labels.setOrientation(LinearLayout.VERTICAL);
            labels.setGravity(Gravity.CENTER_VERTICAL);
            LayoutParams textParams=new LayoutParams(-1,-1);textParams.leftMargin=dp(101);textParams.rightMargin=dp(12);button.addView(labels,textParams);
            TextView label=new TextView(getContext());label.setText(choice.label);label.setTextColor(0xffe9dbcb);
            label.setTypeface(Typeface.DEFAULT,Typeface.BOLD);label.setTextSize(20);label.setMaxLines(1);labels.addView(label);
            if(!choice.detail.isEmpty()){
                TextView detail=new TextView(getContext());detail.setText(choice.detail);detail.setTextColor(0xffcbb6a2);
                detail.setTypeface(Typeface.DEFAULT,Typeface.ITALIC);detail.setTextSize(12);detail.setMaxLines(1);labels.addView(detail);
            }
            button.setOnClickListener(v->{if(submitted)return;submitted=true;choose.accept(choice.number);});
        }
    }
}
