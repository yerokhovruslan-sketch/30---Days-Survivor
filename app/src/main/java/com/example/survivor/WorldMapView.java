package com.example.survivor;

import android.content.Context;
import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.view.*;
import android.widget.*;
import com.example.survivor.game.GameSession;
import java.util.*;
import java.util.function.IntConsumer;

/** Map pins correspond to actual discovered locations, never invented route data. */
final class WorldMapView extends FrameLayout {
 private final IntConsumer choose;
 private final Runnable unavailable;
 private final MapCanvas map;
 private final LinearLayout top,bottom;
 private GameSession.Frame frame;
 private int selected;
 private int selectedRegion=-1;
 private MediaPlayer wind;
 private boolean foreground=true;
 WorldMapView(Context c,IntConsumer choose,Runnable unavailable){super(c);this.choose=choose;this.unavailable=unavailable;setBackgroundColor(Color.BLACK);setClickable(true);
  map=new MapCanvas(c);addView(map,new LayoutParams(-1,-1));
  top=new LinearLayout(c);top.setOrientation(LinearLayout.VERTICAL);top.setPadding(dp(12),dp(4),dp(12),dp(4));top.setVisibility(GONE);
  LinearLayout footer=new LinearLayout(c);footer.setOrientation(LinearLayout.VERTICAL);footer.setBackgroundColor(0xcc080808);addView(footer,new LayoutParams(-1,-2,Gravity.BOTTOM));footer.addView(top);
  bottom=new LinearLayout(c);bottom.setPadding(dp(8),dp(6),dp(8),dp(6));footer.addView(bottom);
  control("← Назад",()->{if(selectedRegion>=0){selectedRegion=-1;selected=0;map.animateOverview();renderHeader();}else choose.accept(0);},1);control("Тело",()->choose.accept(100),0);setVisibility(GONE);
 }
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private TextView label(String text,int size,boolean bold){TextView t=new TextView(getContext());t.setText(text);t.setTextColor(Color.WHITE);t.setTextSize(size);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 private void control(String text,Runnable action,int weight){TextView t=label(text,15,true);t.setGravity(Gravity.CENTER);t.setPadding(dp(12),dp(12),dp(12),dp(12));GradientDrawable bg=new GradientDrawable();bg.setColor(0xe62a2a2a);bg.setCornerRadius(dp(8));bg.setStroke(dp(1),0xff888888);t.setBackground(bg);t.setOnClickListener(v->action.run());bottom.addView(t,new LinearLayout.LayoutParams(weight==1?0:-2,-2,weight));}
 static boolean matches(GameSession.Frame f){return f!=null&&f.mode==GameSession.Mode.CHOICE&&f.home&&f.menu.title.equals("Карта локаций")&&f.choices.stream().anyMatch(c->c.artwork.startsWith("map_point_"));}
 void showFrame(GameSession.Frame f){
  if(!matches(f)){stopWind();if(f!=null&&f.mode!=GameSession.Mode.BUSY){setVisibility(GONE);frame=null;selected=0;}return;}
  boolean entering=getVisibility()!=VISIBLE;frame=f;
  if(entering){selected=0;selectedRegion=-1;map.loadImages();map.reset();setVisibility(VISIBLE);setAlpha(0);animate().alpha(1).setDuration(600).start();}startWind();
  if(selected!=0&&choice(selected)==null)selected=0;renderHeader();map.invalidate();
 }
 void foreground(boolean active){foreground=active;if(active&&getVisibility()==VISIBLE&&frame!=null)startWind();else stopWind();}
 private void startWind(){if(!foreground||wind!=null)return;try{wind=MediaPlayer.create(getContext(),R.raw.map_wind);if(wind==null)return;wind.setVolume(.30f,.30f);wind.setLooping(true);int duration=wind.getDuration();if(duration>15000)wind.seekTo(new Random().nextInt(Math.max(1,duration-12000)));wind.start();}catch(RuntimeException ignored){stopWind();}}
 private void stopWind(){if(wind!=null){try{wind.stop();}catch(RuntimeException ignored){}wind.release();wind=null;}}
 @Override protected void onDetachedFromWindow(){stopWind();super.onDetachedFromWindow();}
 private GameSession.Choice choice(int n){if(frame==null)return null;for(GameSession.Choice c:frame.choices)if(c.number==n&&c.artwork.equals("map_point_"+n))return c;return null;}
 private void select(int number){if(choice(number)==null)return;if(choice(number).detail.startsWith("Сейчас закрыта")){unavailable.run();map.shake(number);return;}if(selected==number){choose.accept(number);return;}selected=number;renderHeader();map.invalidate();}
 private void selectRegion(int region){if(region<0)return;if(region==selectedRegion){selectedRegion=-1;selected=0;renderHeader();map.animateOverview();return;}selectedRegion=region;selected=0;renderHeader();map.animateRegion(region);}
 private void renderHeader(){top.removeAllViews();GameSession.Choice c=choice(selected);top.setVisibility(c==null?GONE:VISIBLE);
  if(c!=null){top.addView(label(c.label,18,true));TextView detail=label(c.detail+"\nНажмите ещё раз, чтобы открыть",12,false);detail.setTextColor(0xffded8cb);top.addView(detail);}
 }
 private final class MapCanvas extends View {
  Bitmap image,pin;final Bitmap[] zoneIcons=new Bitmap[4],clouds=new Bitmap[3],cloudPieces=new Bitmap[MapClouds.count()];
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);final ScaleGestureDetector detector;
  float zoom=1,offsetX,offsetY,downX,downY,lastX,lastY,pinAlpha,shakeOffset;int shaking;boolean moved,scaled;ValueAnimator animation;
  // Coordinates correspond to city, medical district, forest, quarry, roads and industrial sites in the map artwork.
  final float[][] xy={{0,0},{.19f,.64f},{.30f,.29f},{.37f,.56f},{.26f,.61f},{.14f,.59f},{.12f,.53f},{.13f,.70f},{.21f,.69f},{.30f,.72f},
   {.36f,.45f},{.42f,.48f},{.40f,.42f},{.31f,.46f},{.27f,.50f},{.21f,.48f},{.17f,.45f},{.27f,.44f},{.34f,.51f},
   {.22f,.22f},{.33f,.20f},{.36f,.16f},{.14f,.15f},{.22f,.11f},{.39f,.25f},{.43f,.31f},{.38f,.35f},
   {.57f,.27f},{.80f,.19f},{.64f,.17f},{.71f,.15f},{.79f,.14f},{.88f,.14f},
   {.48f,.68f},{.43f,.72f},{.48f,.76f},{.51f,.70f},{.57f,.61f},{.61f,.56f},{.64f,.61f},{.68f,.65f},
   {.81f,.48f},{.87f,.54f},{.91f,.43f},{.72f,.37f},{.79f,.32f},{.84f,.35f},{.74f,.31f},{.89f,.31f},{.81f,.29f},{.85f,.27f},
   {.52f,.82f},{.60f,.81f},{.68f,.83f}};
  MapCanvas(Context c){super(c);detector=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){scaled=true;float old=zoom;zoom=Math.max(1f,Math.min(4f,zoom*d.getScaleFactor()));float ratio=zoom/old;offsetX=d.getFocusX()-(d.getFocusX()-offsetX)*ratio;offsetY=d.getFocusY()-(d.getFocusY()-offsetY)*ratio;clamp();invalidate();return true;}});}
  void loadImages(){
   if(image!=null&&pin!=null&&zoneIcons[0]!=null)return;
   BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;options.inPreferredConfig=Bitmap.Config.RGB_565;
   try{if(image==null)image=BitmapFactory.decodeResource(getResources(),R.drawable.world_map_vector,options);options.inPreferredConfig=Bitmap.Config.ARGB_8888;if(pin==null)pin=BitmapFactory.decodeResource(getResources(),R.drawable.world_search_pin,options);}
   catch(OutOfMemoryError error){image=null;pin=null;}
   if(image==null){options.inSampleSize=4;options.inPreferredConfig=Bitmap.Config.RGB_565;image=BitmapFactory.decodeResource(getResources(),R.drawable.world_map_vector,options);}
   if(pin==null){options.inSampleSize=4;options.inPreferredConfig=Bitmap.Config.ARGB_8888;pin=BitmapFactory.decodeResource(getResources(),R.drawable.world_search_pin,options);}
   int[] icons={R.drawable.map_zone_forest,R.drawable.map_zone_quarry,R.drawable.map_zone_city,R.drawable.map_zone_industry};
   int[] cloudIds={R.drawable.map_cloud_a,R.drawable.map_cloud_b,R.drawable.map_cloud_c};
   for(int i=0;i<icons.length;i++)if(zoneIcons[i]==null)zoneIcons[i]=keyed(icons[i],false);
   for(int i=0;i<cloudIds.length;i++)if(clouds[i]==null)clouds[i]=keyed(cloudIds[i],true);
   for(int i=0;i<cloudPieces.length;i++)if(cloudPieces[i]==null && clouds[MapClouds.SOURCE[i]]!=null)cloudPieces[i]=cloudPiece(clouds[MapClouds.SOURCE[i]],MapClouds.CUTS[i]);
  }
  Bitmap keyed(int resource,boolean cloud){
   BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=cloud?4:2;o.inPreferredConfig=Bitmap.Config.ARGB_8888;
   Bitmap raw=BitmapFactory.decodeResource(getResources(),resource,o);if(raw==null)return null;
   int width=raw.getWidth(),height=raw.getHeight();int[] pixels=new int[width*height];raw.getPixels(pixels,0,width,0,0,width,height);raw.recycle();
   // copy() can preserve an opaque source flag, silently discarding all alpha
   // written below. Allocate a fresh alpha-capable bitmap instead.
   Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);result.setHasAlpha(true);
   for(int i=0;i<pixels.length;i++){int v=pixels[i],r=Color.red(v),g=Color.green(v),b=Color.blue(v),old=Color.alpha(v);
    if(cloud){int darkness=255-Math.min(r,Math.min(g,b));int a=Math.min(190,Math.max(0,(darkness-3)*2));pixels[i]=Color.argb(a*old/255,230,232,230);}
    else {int white=Math.min(r,Math.min(g,b));int a=white>=249?0:white<=225?255:(249-white)*255/24;pixels[i]=(v&0x00ffffff)|((a*old/255)<<24);}
   }result.setPixels(pixels,0,width,0,0,width,height);return result;
  }
  Bitmap cloudPiece(Bitmap source,float[] cut){
   int left=Math.round(source.getWidth()*cut[0]),top=Math.round(source.getHeight()*cut[1]);
   int width=Math.max(1,Math.min(source.getWidth()-left,Math.round(source.getWidth()*(cut[2]-cut[0]))));
   int height=Math.max(1,Math.min(source.getHeight()-top,Math.round(source.getHeight()*(cut[3]-cut[1]))));
   int[] pixels=new int[width*height];source.getPixels(pixels,0,width,left,top,width,height);
   float fadeX=Math.max(1,width*.09f),fadeY=Math.max(1,height*.10f);
   for(int y=0;y<height;y++)for(int x=0;x<width;x++){
    int index=y*width+x,color=pixels[index];float edge=Math.min(1f,Math.min(Math.min((x+.5f)/fadeX,(width-x-.5f)/fadeX),Math.min((y+.5f)/fadeY,(height-y-.5f)/fadeY)));
    pixels[index]=(color&0x00ffffff)|(Math.round(Color.alpha(color)*edge)<<24);
   }
   Bitmap result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);result.setHasAlpha(true);result.setPixels(pixels,0,width,0,0,width,height);return result;
  }
  void shake(int number){shaking=number;ValueAnimator wiggle=ValueAnimator.ofFloat(0,1);wiggle.setDuration(330);wiggle.addUpdateListener(a->{float f=(float)a.getAnimatedValue();shakeOffset=(float)Math.sin(f*Math.PI*6)*dp(5)*(1-f);if(f>=1)shaking=0;invalidate();});wiggle.start();}
  void reset(){if(animation!=null)animation.cancel();zoom=1;offsetX=offsetY=0;pinAlpha=0;invalidate();}
  void animateOverview(){animateTo(1,0,0,false);}
  void animateRegion(int region){float target=2.2f;float cx=MapRegions.center(region,true),cy=MapRegions.center(region,false);
   float tx=getWidth()/2f-(left()+cx*image.getWidth()*base())*target;
   float ty=(getHeight()+top.getHeight()-bottom.getHeight())/2f-(topY()+cy*image.getHeight()*base())*target;
   animateTo(target,tx,ty,true);
  }
  void animateTo(float target,float tx,float ty,boolean showPins){if(animation!=null)animation.cancel();float fromZoom=zoom,fromX=offsetX,fromY=offsetY;pinAlpha=0;
   animation=ValueAnimator.ofFloat(0,1);animation.setDuration(850);animation.setInterpolator(new android.view.animation.DecelerateInterpolator());
   animation.addUpdateListener(a->{float f=(float)a.getAnimatedValue();zoom=fromZoom+(target-fromZoom)*f;offsetX=fromX+(tx-fromX)*f;offsetY=fromY+(ty-fromY)*f;clamp();pinAlpha=showPins?Math.max(0,(f-.45f)/.55f):0;invalidate();});animation.start();
  }
  float base(){return Math.min(getWidth()/(float)image.getWidth(),getHeight()/(float)image.getHeight());}
  float left(){return (getWidth()-image.getWidth()*base())/2f;}
  float topY(){return (getHeight()-image.getHeight()*base())/2f;}
  void clamp(){float w=image.getWidth()*base()*zoom,h=image.getHeight()*base()*zoom;offsetX=w<=getWidth()?(getWidth()-w)/2f-left()*zoom:Math.max(getWidth()-w-left()*zoom,Math.min(-left()*zoom,offsetX));offsetY=h<=getHeight()?(getHeight()-h)/2f-topY()*zoom:Math.max(getHeight()-h-topY()*zoom,Math.min(-topY()*zoom,offsetY));}
  void changeZoom(float factor){float old=zoom;zoom=Math.max(1,Math.min(4f,zoom*factor));float ratio=zoom/old;offsetX=getWidth()/2f-(getWidth()/2f-offsetX)*ratio;offsetY=getHeight()/2f-(getHeight()/2f-offsetY)*ratio;clamp();invalidate();}
  float x(int i){return (left()+xy[i][0]*image.getWidth()*base())*zoom+offsetX;}
  float y(int i){return (topY()+xy[i][1]*image.getHeight()*base())*zoom+offsetY;}
  @Override protected void onDraw(Canvas c){super.onDraw(c);if(image==null||pin==null)return;float scale=base()*zoom;float originX=left()*zoom+offsetX,originY=topY()*zoom+offsetY;paint.setAlpha(255);paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);paint.setColorFilter(ArtworkTone.location());c.drawBitmap(image,null,new RectF(originX,originY,originX+image.getWidth()*scale,originY+image.getHeight()*scale),paint);paint.setColorFilter(null);
   if(frame==null)return;
   // Clouds drift in map coordinates and fade as the camera descends toward the ground.
   long time=android.os.SystemClock.uptimeMillis();int save=c.save();float mapWidth=image.getWidth()*scale,mapHeight=image.getHeight()*scale;
   c.clipRect(originX,originY,originX+mapWidth,originY+mapHeight);
   float visibility=MapClouds.opacity(zoom);
   if(visibility>0)for(int i=0;i<cloudPieces.length;i++){
    Bitmap cloud=cloudPieces[i];if(cloud==null)continue;
    float width=MapClouds.width(i)*mapWidth,px=originX+MapClouds.x(i,time)*mapWidth,py=originY+MapClouds.y(i)*mapHeight;
    RectF bounds=new RectF(px,py,px+width,py+width*cloud.getHeight()/cloud.getWidth());
    if(!RectF.intersects(bounds,new RectF(0,0,getWidth(),getHeight())))continue;
    int tone=MapClouds.tone(i);paint.setColor(Color.WHITE);paint.setColorFilter(new LightingColorFilter(Color.rgb(tone,tone,tone),0));paint.setAlpha(Math.round(MapClouds.alpha(i)*visibility));c.drawBitmap(cloud,null,bounds,paint);
    paint.setColorFilter(null);
   }
   c.restoreToCount(save);
   for(int zone=0;zone<MapRegions.SHAPES.length;zone++){float[] points=MapRegions.SHAPES[zone];Path path=new Path();path.moveTo(originX+points[0]*image.getWidth()*scale,originY+points[1]*image.getHeight()*scale);for(int j=2;j<points.length;j+=2)path.lineTo(originX+points[j]*image.getWidth()*scale,originY+points[j+1]*image.getHeight()*scale);path.close();
    paint.setStyle(Paint.Style.STROKE);paint.setColor(Color.WHITE);paint.setStrokeWidth(dp(1.5f));paint.setAlpha(150);paint.setPathEffect(new CornerPathEffect(dp(3)));c.drawPath(path,paint);paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
   }
   for(int zone=0;zone<MapRegions.SHAPES.length;zone++){int icon=zone==0||zone==6?0:zone==1?1:zone==2||zone==5?2:3;Bitmap badge=zoneIcons[icon];if(badge==null)continue;float px=originX+MapRegions.center(zone,true)*image.getWidth()*scale,py=originY+MapRegions.center(zone,false)*image.getHeight()*scale;float r=dp(selectedRegion==zone?24:20);paint.setColor(Color.WHITE);paint.setAlpha(selectedRegion==zone?65:200);c.drawBitmap(badge,null,new RectF(px-r,py-r,px+r,py+r),paint);boolean hasNew=false;for(GameSession.Choice item:frame.choices){int n=item.number;if(n>0&&n<xy.length&&item.artwork.equals("map_point_"+n)&&MapRegions.at(xy[n][0],xy[n][1])==zone&&item.detail.startsWith("Доступна")&&item.detail.contains("Не посещена")){hasNew=true;break;}}if(hasNew)drawNew(c,px+r*.7f,py-r*.7f,1);boolean waiting=false;for(GameSession.Choice item:frame.choices){int n=item.number;if(n>0&&n<xy.length&&item.artwork.equals("map_point_"+n)&&MapRegions.at(xy[n][0],xy[n][1])==zone&&item.detail.contains("Заражённые остались на локации")){waiting=true;break;}}if(waiting){paint.setColor(0xffff2020);paint.setAlpha((int)(200+55*Math.sin(time/250.0)));c.drawCircle(px+r*.72f,py+r*.72f,dp(5),paint);paint.setAlpha(255);}}
   if(selectedRegion>=0){float radius=dp(15);paint.setAlpha(Math.round(255*pinAlpha));for(GameSession.Choice item:frame.choices){int n=item.number;if(n<1||n>=xy.length||!item.artwork.equals("map_point_"+n)||MapRegions.at(xy[n][0],xy[n][1])!=selectedRegion)continue;float x=x(n)+(shaking==n?shakeOffset:0),y=y(n);boolean closed=item.detail.startsWith("Сейчас закрыта"),visited=item.detail.contains(" · Посещена");if(n==selected){paint.setColor(0xffffdb93);paint.setAlpha(Math.round(255*pinAlpha));c.drawCircle(x,y,radius+dp(3),paint);}paint.setColor(Color.WHITE);paint.setAlpha(Math.round(255*pinAlpha));if(closed){ColorMatrix matrix=new ColorMatrix();matrix.setSaturation(0);paint.setColorFilter(new ColorMatrixColorFilter(matrix));paint.setAlpha(Math.round(165*pinAlpha));}c.drawBitmap(pin,null,new RectF(x-radius,y-radius,x+radius,y+radius),paint);paint.setColorFilter(null);
    if(item.detail.contains("Заражённые остались на локации")){float pulse=.55f+.45f*(float)Math.sin(time/250.0);paint.setColor(0xffff2020);paint.setAlpha(Math.round(255*pinAlpha*pulse));c.drawCircle(x+radius*.75f,y-radius*.75f,dp(5),paint);paint.setAlpha(255);}
    if(visited){float markerY=item.detail.contains("Заражённые остались на локации")?y+radius*.72f:y-radius*.72f;paint.setAlpha(Math.round(255*pinAlpha));paint.setColor(Color.WHITE);c.drawCircle(x+radius*.72f,markerY,dp(5),paint);paint.setColor(Color.BLACK);c.drawCircle(x+radius*.72f,markerY,dp(3.5f),paint);}else if(!closed){drawNew(c,x+radius*.72f,y-radius*.72f,pinAlpha);}}}
   paint.setAlpha(255);if(getVisibility()==VISIBLE && WorldMapView.this.getVisibility()==VISIBLE)postInvalidateDelayed(80);
  }
  void drawNew(Canvas c,float x,float y,float alpha){paint.setAlpha(Math.round(255*alpha));paint.setColor(0xffffca70);c.drawCircle(x,y,dp(7),paint);paint.setColor(Color.BLACK);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextSize(dp(11));paint.setTextAlign(Paint.Align.CENTER);c.drawText("!",x,y+dp(4),paint);paint.setTextAlign(Paint.Align.LEFT);}
  @Override public boolean onTouchEvent(android.view.MotionEvent e){if(image==null||pin==null)return true;detector.onTouchEvent(e);switch(e.getActionMasked()){
   case MotionEvent.ACTION_DOWN:if(animation!=null&&animation.isRunning()){animation.cancel();pinAlpha=selectedRegion<0?0:1;}downX=lastX=e.getX();downY=lastY=e.getY();moved=false;scaled=false;return true;
   case MotionEvent.ACTION_POINTER_DOWN:scaled=true;return true;
   case MotionEvent.ACTION_MOVE:if(!detector.isInProgress()&&e.getPointerCount()==1&&!scaled){float dx=e.getX()-lastX,dy=e.getY()-lastY;if(Math.abs(e.getX()-downX)+Math.abs(e.getY()-downY)>dp(8))moved=true;if(moved&&zoom>1){offsetX+=dx;offsetY+=dy;clamp();invalidate();}lastX=e.getX();lastY=e.getY();}return true;
   case MotionEvent.ACTION_UP:if(!moved&&!scaled&&frame!=null){int best=0;float bestDist=dp(28)*dp(28);if(selectedRegion>=0&&pinAlpha>.5f)for(GameSession.Choice item:frame.choices){int n=item.number;if(n<1||n>=xy.length||!item.artwork.equals("map_point_"+n)||MapRegions.at(xy[n][0],xy[n][1])!=selectedRegion)continue;float dx=e.getX()-x(n),dy=e.getY()-y(n),d=dx*dx+dy*dy;if(d<bestDist){best=n;bestDist=d;}}if(best>0)select(best);else{float scale=base()*zoom;float ox=left()*zoom+offsetX,oy=topY()*zoom+offsetY;int badge=-1;float distance=dp(26)*dp(26);for(int z=0;z<MapRegions.SHAPES.length;z++){float dx=e.getX()-(ox+MapRegions.center(z,true)*image.getWidth()*scale),dy=e.getY()-(oy+MapRegions.center(z,false)*image.getHeight()*scale),d=dx*dx+dy*dy;if(d<distance){badge=z;distance=d;}}float nx=(e.getX()-ox)/(image.getWidth()*scale),ny=(e.getY()-oy)/(image.getHeight()*scale);selectRegion(badge>=0?badge:MapRegions.at(nx,ny));}}return true;
   default:return true;
  }}
 }
}
