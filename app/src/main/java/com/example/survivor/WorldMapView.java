package com.example.survivor;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import com.example.survivor.game.GameSession;
import java.util.*;
import java.util.function.IntConsumer;

/** Map pins correspond to actual discovered locations, never invented route data. */
final class WorldMapView extends FrameLayout {
 private final IntConsumer choose;
 private final MapCanvas map;
 private final LinearLayout top,bottom;
 private GameSession.Frame frame;
 private int selected;
 WorldMapView(Context c,IntConsumer choose){super(c);this.choose=choose;setBackgroundColor(Color.BLACK);setClickable(true);
  map=new MapCanvas(c);addView(map,new LayoutParams(-1,-1));
  top=new LinearLayout(c);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(16),dp(12),dp(16),dp(12));top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xf5000000,0xe9141414,0x99111111}));addView(top,new LayoutParams(-1,-2,Gravity.TOP));
  bottom=new LinearLayout(c);bottom.setPadding(dp(8),dp(6),dp(8),dp(6));bottom.setBackgroundColor(0xcc080808);addView(bottom,new LayoutParams(-1,-2,Gravity.BOTTOM));
  control("← Назад",()->choose.accept(0),1);control("−",()->map.changeZoom(.8f),0);control("+",()->map.changeZoom(1.25f),0);control("Тело",()->choose.accept(100),0);setVisibility(GONE);
 }
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private TextView label(String text,int size,boolean bold){TextView t=new TextView(getContext());t.setText(text);t.setTextColor(Color.WHITE);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private void control(String text,Runnable action,int weight){TextView t=label(text,15,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(12),dp(12),dp(12),dp(12));t.setOnClickListener(v->action.run());bottom.addView(t,new LinearLayout.LayoutParams(weight==1?0:-2,-2,weight));}
 static boolean matches(GameSession.Frame f){return f!=null&&f.mode==GameSession.Mode.CHOICE&&f.home&&f.menu.title.equals("Карта локаций")&&f.choices.stream().anyMatch(c->c.artwork.startsWith("map_point_"));}
 void showFrame(GameSession.Frame f){
  if(!matches(f)){if(f.mode!=GameSession.Mode.BUSY){setVisibility(GONE);frame=null;selected=0;}return;}
  boolean entering=getVisibility()!=VISIBLE;frame=f;
  if(entering){selected=0;map.reset();setVisibility(VISIBLE);setAlpha(0);animate().alpha(1).setDuration(600).start();}
  if(selected!=0&&choice(selected)==null)selected=0;renderHeader();map.invalidate();
 }
 private GameSession.Choice choice(int n){if(frame==null)return null;for(GameSession.Choice c:frame.choices)if(c.number==n&&c.artwork.equals("map_point_"+n))return c;return null;}
 private void select(int number){if(choice(number)==null)return;if(selected==number){choose.accept(number);return;}selected=number;renderHeader();map.invalidate();}
 private void renderHeader(){top.animate().cancel();top.removeAllViews();GameSession.Choice c=choice(selected);
  top.addView(label(c==null?"КАРТА ЛОКАЦИЙ":c.label,23,true));TextView detail=label(c==null?"Выберите значок на карте":c.detail,14,false);detail.setTextColor(0xffded8cb);top.addView(detail);
  if(c!=null){TextView hint=label("Нажмите на лупу ещё раз, чтобы открыть",12,false);hint.setTextColor(0xffb7b0a3);top.addView(hint);}
  top.setAlpha(0);top.animate().alpha(1).setDuration(300).start();
 }
 private final class MapCanvas extends View {
  final Bitmap image=BitmapFactory.decodeResource(getResources(),R.drawable.world_map),pin=BitmapFactory.decodeResource(getResources(),R.drawable.world_search_pin);
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);final ScaleGestureDetector detector;
  float zoom=1,offsetX,offsetY,downX,downY,lastX,lastY;boolean moved,scaled;
  // Coordinates correspond to city, medical district, forest, quarry, roads and industrial sites in the map artwork.
  final float[][] xy={{0,0},{.19f,.64f},{.30f,.29f},{.37f,.56f},{.26f,.61f},{.14f,.59f},{.12f,.53f},{.13f,.70f},{.21f,.69f},{.30f,.72f},
   {.36f,.45f},{.42f,.48f},{.40f,.42f},{.31f,.46f},{.27f,.50f},{.21f,.48f},{.17f,.45f},{.27f,.44f},{.34f,.51f},
   {.22f,.22f},{.33f,.20f},{.36f,.16f},{.14f,.15f},{.22f,.11f},{.39f,.25f},{.43f,.31f},{.38f,.35f},
   {.57f,.27f},{.80f,.19f},{.64f,.17f},{.71f,.15f},{.79f,.14f},{.88f,.14f},
   {.48f,.68f},{.43f,.72f},{.48f,.76f},{.51f,.70f},{.57f,.61f},{.61f,.56f},{.64f,.61f},{.68f,.65f},
   {.81f,.48f},{.87f,.54f},{.91f,.43f},{.72f,.37f},{.79f,.32f},{.84f,.35f},{.74f,.31f},{.89f,.31f},{.81f,.29f},{.85f,.27f},
   {.52f,.82f},{.60f,.81f},{.68f,.83f}};
  MapCanvas(Context c){super(c);detector=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){scaled=true;float old=zoom;zoom=Math.max(1f,Math.min(1.8f,zoom*d.getScaleFactor()));float ratio=zoom/old;offsetX=d.getFocusX()-(d.getFocusX()-offsetX)*ratio;offsetY=d.getFocusY()-(d.getFocusY()-offsetY)*ratio;clamp();invalidate();return true;}});}
  void reset(){zoom=1;offsetX=offsetY=0;invalidate();}
  float base(){return Math.min(getWidth()/(float)image.getWidth(),getHeight()/(float)image.getHeight());}
  float left(){return (getWidth()-image.getWidth()*base())/2f;}
  float topY(){return (getHeight()-image.getHeight()*base())/2f;}
  void clamp(){float w=image.getWidth()*base()*zoom,h=image.getHeight()*base()*zoom;offsetX=w<=getWidth()?0:Math.max(getWidth()-w-left()*zoom,Math.min(-left()*zoom,offsetX));offsetY=h<=getHeight()?0:Math.max(getHeight()-h-topY()*zoom,Math.min(-topY()*zoom,offsetY));}
  void changeZoom(float factor){float old=zoom;zoom=Math.max(1,Math.min(1.8f,zoom*factor));float ratio=zoom/old;offsetX=getWidth()/2f-(getWidth()/2f-offsetX)*ratio;offsetY=getHeight()/2f-(getHeight()/2f-offsetY)*ratio;clamp();invalidate();}
  float x(int i){return (left()+xy[i][0]*image.getWidth()*base())*zoom+offsetX;}
  float y(int i){return (topY()+xy[i][1]*image.getHeight()*base())*zoom+offsetY;}
  @Override protected void onDraw(Canvas c){super.onDraw(c);float scale=base()*zoom;paint.setColorFilter(ArtworkTone.location());c.drawBitmap(image,null,new RectF(left()*zoom+offsetX,topY()*zoom+offsetY,left()*zoom+offsetX+image.getWidth()*scale,topY()*zoom+offsetY+image.getHeight()*scale),paint);paint.setColorFilter(null);
   if(frame==null)return;float radius=dp(15);for(GameSession.Choice item:frame.choices){int n=item.number;if(n<1||n>=xy.length||!item.artwork.equals("map_point_"+n))continue;float x=x(n),y=y(n);if(n==selected){paint.setColor(0xffffdb93);c.drawCircle(x,y,radius+dp(3),paint);paint.setColor(Color.WHITE);}c.drawBitmap(pin,null,new RectF(x-radius,y-radius,x+radius,y+radius),paint);}
  }
  @Override public boolean onTouchEvent(android.view.MotionEvent e){detector.onTouchEvent(e);switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:downX=lastX=e.getX();downY=lastY=e.getY();moved=false;scaled=false;return true;
   case MotionEvent.ACTION_POINTER_DOWN:scaled=true;return true;
   case MotionEvent.ACTION_MOVE:if(!detector.isInProgress()&&e.getPointerCount()==1&&!scaled){float dx=e.getX()-lastX,dy=e.getY()-lastY;if(Math.abs(e.getX()-downX)+Math.abs(e.getY()-downY)>dp(8))moved=true;if(moved&&zoom>1){offsetX+=dx;offsetY+=dy;clamp();invalidate();}lastX=e.getX();lastY=e.getY();}return true;
   case MotionEvent.ACTION_UP:if(!moved&&!scaled&&frame!=null){int best=0;float bestDist=dp(28)*dp(28);for(GameSession.Choice item:frame.choices){int n=item.number;if(n<1||n>=xy.length||!item.artwork.equals("map_point_"+n))continue;float dx=e.getX()-x(n),dy=e.getY()-y(n),d=dx*dx+dy*dy;if(d<bestDist){best=n;bestDist=d;}}if(best>0)select(best);}return true;
   default:return true;
  }}
 }
}
