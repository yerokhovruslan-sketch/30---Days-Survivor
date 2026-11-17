package com.example.survivor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import com.example.survivor.game.BattleBoard;
import android.util.LruCache;
import java.util.function.IntConsumer;

/** Drawing and touch handling use the very same square grid rectangle. */
final class BattleBoardView extends View {
    private BattleBoard board;
    private BattleBoard previous;
    private long changedAt,lastEffect;
    private int preview=-1;
    private final java.util.List<Pop> pops=new java.util.ArrayList<>();
    private static final class Pop {final BattleBoard.Effect effect;final long at;final BattleBoard.Cell victim;final boolean zombie;Pop(BattleBoard.Effect e,long at,BattleBoard.Cell victim){effect=e;this.at=at;this.victim=victim;zombie=victim!=null && victim.unit>=3 && victim.unit<=7;}}
    private final IntConsumer action;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private static final LruCache<Integer,Bitmap> images=new LruCache<Integer,Bitmap>(12*1024) {
        @Override protected int sizeOf(Integer key,Bitmap bitmap) {return Math.max(1,bitmap.getByteCount()/1024);}
    };
    private final int[] units,obstacles;
    private final float gutter;
    private float downX,downY;

    BattleBoardView(Context context,BattleBoard board,IntConsumer action) {
        super(context);this.board=board;this.action=action;lastEffect=board.effects.isEmpty()?0:board.effects.get(board.effects.size()-1).id;
        gutter=18*getResources().getDisplayMetrics().density;
        units=ids(R.array.battle_units);obstacles=ids(R.array.battle_obstacles);
        setContentDescription("Поле 8 на 8. Джон: "+coordinate(board.playerCell)+". Нажмите клетку для движения или врага для выбора цели.");
        setFocusable(true);setClickable(true);
        setSoundEffectsEnabled(false);
    }
    void update(BattleBoard next){
        if(board==next)return;
        long now=android.os.SystemClock.uptimeMillis();
        if(board.epoch!=next.epoch){previous=null;pops.clear();preview=-1;lastEffect=next.effects.isEmpty()?0:next.effects.get(next.effects.size()-1).id;}
        else {
            previous=board;
            for(BattleBoard.Effect e:next.effects)if(e.id>lastEffect){pops.add(new Pop(e,now,board.cells.get(e.cell)));lastEffect=e.id;}
            if(board.playerCell!=next.playerCell || preview>=0 && !next.cells.get(preview).move)preview=-1;
        }
        board=next;changedAt=now;invalidate();
    }
    private BattleBoard.Cell oldUnit(int identity){if(previous==null || identity<0)return null;for(BattleBoard.Cell c:previous.cells)if(c.identity==identity)return c;return null;}
    private RectF figure(BattleBoard.Cell c,long now,float inset){
        RectF result=cell(c.index,inset);BattleBoard.Cell old=oldUnit(c.identity);
        if(old==null || old.index==c.index || now-changedAt>=400)return result;
        java.util.List<Integer> route=BoardVisuals.path(board,old.index,c.index);
        if(route.size()<2)return result;
        float progress=Math.min(1,(now-changedAt)/400f)*(route.size()-1);int segment=Math.min(route.size()-2,(int)progress);float fraction=progress-segment;
        RectF from=cell(route.get(segment),inset),to=cell(route.get(segment+1),inset);
        result.offset(from.left+(to.left-from.left)*fraction-result.left,from.top+(to.top-from.top)*fraction-result.top);return result;
    }
    private void drawRoute(Canvas canvas){
        if(preview<0)return;java.util.List<Integer> path=BoardVisuals.path(board,board.playerCell,preview);if(path.size()<2)return;
        paint.setColor(0xffc9c0a0);paint.setStrokeWidth(3*getResources().getDisplayMetrics().density);paint.setStyle(Paint.Style.STROKE);Path line=new Path();
        for(int i=0;i<path.size();i++){RectF r=cell(path.get(i),0);if(i==0)line.moveTo(r.centerX(),r.centerY());else line.lineTo(r.centerX(),r.centerY());}canvas.drawPath(line,paint);
        canvas.drawRoundRect(cell(preview,4),5,5,paint);paint.setStyle(Paint.Style.FILL);
    }
    private int[] ids(int resource) {
        TypedArray a=getResources().obtainTypedArray(resource);int[] values=new int[a.length()];
        for(int i=0;i<values.length;i++)values[i]=a.getResourceId(i,0);
        a.recycle();return values;
    }
    private Bitmap bitmap(int id) {
        Bitmap image=images.get(id);
        if(image==null) {image=BitmapFactory.decodeResource(getResources(),id);images.put(id,image);}
        return image;
    }
    private static RectF fitSprite(Bitmap picture,RectF bounds){
        float scale=Math.min(bounds.width()/picture.getWidth(),bounds.height()/picture.getHeight());
        float w=picture.getWidth()*scale,h=picture.getHeight()*scale;
        return new RectF(bounds.centerX()-w/2,bounds.centerY()-h/2,bounds.centerX()+w/2,bounds.centerY()+h/2);
    }
    static String coordinate(int cell) {return "АБВГДЕЖЗ".charAt(cell%8)+Integer.toString(cell/8+1);}
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int width=MeasureSpec.getSize(widthSpec);setMeasuredDimension(width,width+(int)(44*getResources().getDisplayMetrics().density));
    }
    private float tile() {return (getWidth()-2*gutter)/8f;}
    private RectF cell(int index,float inset) {
        float size=tile(),x=gutter+(index%8)*size,y=gutter+(index/8)*size;
        return new RectF(x+inset,y+inset,x+size-inset,y+size-inset);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);float size=tile();long now=android.os.SystemClock.uptimeMillis();boolean animating=previous!=null && now-changedAt<420;
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setAlpha(255);
        RectF first=cell(0,0),last=cell(63,0);
        canvas.drawBitmap(bitmap(board.woodFloor?R.drawable.field_wood:board.grassFloor?R.drawable.field_grass:R.drawable.field_asphalt),null,new RectF(first.left,first.top,last.right,last.bottom),paint);
        for(Pop pop:pops)if(pop.effect.kind.equals("break") && pop.victim!=null && pop.victim.obstacle>=0){
            long age=now-pop.at;if(age>=0 && age<500){RectF impact=cell(pop.effect.cell,0);int res=obstacles[pop.victim.obstacle];
                paint.setColor(Color.WHITE);paint.setAlpha(age<220?230:(int)(230*(500-age)/280f));
                if(res!=0)canvas.drawBitmap(bitmap(res),null,impact,paint);paint.setAlpha(255);
                if(age>180){float t=(age-180)/320f;paint.setColor(0xffae9c7c);paint.setAlpha((int)(170*(1-t)));
                    for(int i=0;i<6;i++){double angle=i*2.4;canvas.drawCircle(impact.centerX()+(float)Math.cos(angle)*size*.25f*t,impact.centerY()+(float)Math.sin(angle)*size*.2f*t,Math.max(1,size*.035f*(1-t)),paint);}paint.setAlpha(255);}
                animating=true;
            }
        }
        // Blood stains remain briefly on the ground, then fade away.
        for(Pop pop:pops)if(pop.effect.kind.equals("damage") && pop.effect.amount>0 && pop.zombie){
            long age=now-pop.at;if(age<180 || age>12000)continue;
            RectF stain=cell(pop.effect.cell,0);paint.setColor(0xff4d090b);paint.setAlpha((int)(125*Math.min(1f,(12000-age)/3000f)));
            canvas.drawOval(stain.centerX()-size*.13f,stain.bottom-size*.20f,stain.centerX()+size*.13f,stain.bottom-size*.10f,paint);paint.setAlpha(255);animating=true;
        }
        for(Pop pop:pops)if(pop.effect.kind.equals("damage") && pop.victim!=null && pop.victim.unit>0 && pop.victim.unit<units.length && now-pop.at<400){
            boolean alive=false;for(BattleBoard.Cell c:board.cells)if(c.identity==pop.victim.identity){alive=true;break;}
            if(!alive){long age=now-pop.at;RectF ghost=cell(pop.effect.cell,0);if(age>=180)ghost.offset((float)Math.sin((age-180)/220f*Math.PI*4)*size*.07f,0);
                paint.setColor(Color.WHITE);paint.setAlpha(age<300?255:(int)(255*(400-age)/100f));canvas.drawBitmap(bitmap(units[pop.victim.unit]),null,ghost,paint);paint.setAlpha(255);animating=true;}
        }
        drawRoute(canvas);
        if(board.shotTarget>=0){RectF a=cell(board.playerCell,0),b=cell(board.shotTarget,0);paint.setColor(board.shotBlocked?0xffb76058:0xffa6b5aa);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setPathEffect(new DashPathEffect(new float[]{7,6},0));canvas.drawLine(a.centerX(),a.centerY(),b.centerX(),b.centerY(),paint);paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
            if(board.shotBlocked){float x=(a.centerX()+b.centerX())/2,y=(a.centerY()+b.centerY())/2;canvas.drawLine(x-6,y-6,x+6,y+6,paint);canvas.drawLine(x+6,y-6,x-6,y+6,paint);}}

        paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(gutter*.65f);
        paint.setColor(0xffcccccc);
        for(int i=0;i<8;i++) {
            canvas.drawText(String.valueOf("АБВГДЕЖЗ".charAt(i)),gutter+(i+.5f)*size,gutter*.72f,paint);
            canvas.drawText(Integer.toString(i+1),gutter*.4f,gutter+(i+.65f)*size,paint);
        }
        for(BattleBoard.Cell c:board.cells) {
            RectF r=c.unit>=0?figure(c,now,1):cell(c.index,1);
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xffa0a0a0);
            if(c.move) {paint.setColor(0xff9aaea0);paint.setAlpha(75);canvas.drawRect(cell(c.index,2),paint);paint.setAlpha(180);canvas.drawCircle(r.centerX(),r.centerY(),size*.10f,paint);paint.setAlpha(255);}
            int resource=c.unit>=8?R.drawable.human_wanderer:c.unit>=0 && c.unit<units.length?units[c.unit]:c.obstacle>=0?obstacles[c.obstacle]:0;
            if(resource==0 && previous!=null && previous.cells.get(c.index).obstacle>=0 && now-changedAt<200)resource=obstacles[previous.cells.get(c.index).obstacle];
            if(c.unit<0 && (c.index&1)==1){
                if(c.obstacle==1)resource=R.drawable.field_barrel_blue;
                else if(c.obstacle==2)resource=R.drawable.field_boulder;
                else if(c.obstacle==3)resource=R.drawable.field_fallen_tree;
                else if(c.obstacle==16)resource=R.drawable.field_hedgehog2;
                else if(c.obstacle==20)resource=R.drawable.field_trash_pile;
                else if(c.obstacle==22)resource=R.drawable.field_bush_berries;
            }
            if(c.unit<0 && board.grassFloor){
                if(c.obstacle==2)resource=(c.index&1)==0?R.drawable.forest_rock:R.drawable.forest_stones;
                else if(c.obstacle==3)resource=(c.index&1)==0?R.drawable.forest_spruce:R.drawable.forest_tree;
                else if(c.obstacle==18)resource=R.drawable.forest_log;
                else if(c.obstacle==19)resource=R.drawable.forest_stump;
                else if(c.obstacle==22)resource=(c.index&1)==0?R.drawable.forest_bush:R.drawable.forest_berries;
            }
            // Keep each two-cell vehicle visually consistent across both halves.
            if(c.unit<0 && c.obstacle==12 && c.obstacleSpan==2){
                int variant=Math.floorMod(c.index-c.obstaclePart*(c.obstacleVertical?8:1),3);
                resource=variant==0?R.drawable.field_car:variant==1?R.drawable.field_truck:R.drawable.field_van;
            }
            RectF sprite=c.unit>=0?figure(c,now,0):cell(c.index,0);
            for(Pop pop:pops){
                long age=now-pop.at;
                if(pop.effect.kind.equals("swing") && pop.effect.cell==c.index && age>=0 && age<180){
                    RectF target=cell(pop.effect.amount,0);float dx=target.centerX()-sprite.centerX(),dy=target.centerY()-sprite.centerY();float len=(float)Math.hypot(dx,dy);
                    float k=age<120?-.05f*age/120f:.18f*(1-(age-120)/60f);
                    if(len>0)sprite.offset(dx/len*size*k,dy/len*size*k);animating=true;
                }
                if(pop.effect.kind.equals("collision") && pop.effect.cell==c.index && age>=120 && age<600){
                    RectF obstacleCell=cell(pop.effect.amount,0);float dx=obstacleCell.centerX()-sprite.centerX(),dy=obstacleCell.centerY()-sprite.centerY();float len=(float)Math.hypot(dx,dy);
                    float t=(age-120)/480f;float bounce=t<.35f?t/.35f:(float)(Math.sin((t-.35f)*Math.PI*3)*(1-t)*.6);
                    if(len>0)sprite.offset(dx/len*size*.14f*bounce,dy/len*size*.14f*bounce);animating=true;
                }
                if(pop.effect.kind.equals("damage") && pop.effect.cell==c.index && age>=180 && age<400){
                    sprite.offset((float)Math.sin((age-180)/220f*Math.PI*4)*size*.07f*(1-(age-180)/220f),0);animating=true;
                }
            }
            for(Pop pop:pops)if(pop.effect.cell==c.index && pop.effect.kind.equals("shake") && now>=pop.at && now-pop.at<220){sprite.offset((float)Math.sin((now-pop.at)/220f*Math.PI*6)*3,0);animating=true;}
            for(Pop pop:pops)if(pop.effect.cell==c.index && pop.effect.kind.equals("impact") && now-pop.at>=250 && now-pop.at<480){float f=(now-pop.at-250)/230f;sprite.offset((float)Math.sin(f*Math.PI*6)*size*.025f*(1-f),0);animating=true;}
            if(resource!=0){
                Bitmap picture=bitmap(resource);
                if(c.unit<0 && c.obstacleSpan>1){
                    if(c.obstaclePart==0){
                        RectF occupied=new RectF(sprite);
                        if(c.obstacleVertical)occupied.bottom+=size*(c.obstacleSpan-1);
                        else occupied.right+=size*(c.obstacleSpan-1);
                        if(c.obstacleVertical){
                            int save=canvas.save();canvas.rotate(90,occupied.centerX(),occupied.centerY());
                            RectF horizontal=new RectF(occupied.centerX()-occupied.height()/2,occupied.centerY()-occupied.width()/2,occupied.centerX()+occupied.height()/2,occupied.centerY()+occupied.width()/2);
                            canvas.drawBitmap(picture,null,horizontal,paint);canvas.restoreToCount(save);
                        }else canvas.drawBitmap(picture,null,occupied,paint);
                    }
                } else canvas.drawBitmap(picture,null,sprite,paint);
            }
            if(c.unit>=8 && !c.label.isEmpty()) {
                paint.setColor(0xff222222);canvas.drawRect(r.right-size*.25f,r.top,r.right,r.top+size*.25f,paint);
                paint.setColor(Color.WHITE);paint.setTextSize(size*.18f);canvas.drawText(c.label.substring(0,1),r.right-size*.12f,r.top+size*.20f,paint);
            }
            if(c.label.startsWith("ВХОД") || c.label.startsWith("ПЕРЕХОД") || c.label.startsWith("ДОКУМЕНТЫ")) {
                int marker=c.label.startsWith("ДОКУМЕНТЫ")?R.drawable.archive_cabinet:R.drawable.archive_exit;
                RectF target=c.unit>=0?new RectF(r.left+2,r.top+2,r.left+size*.42f,r.top+size*.42f):cell(c.index,size*.10f);
                paint.setColor(0xff303030);canvas.drawRect(target,paint);paint.setColor(Color.WHITE);canvas.drawBitmap(bitmap(marker),null,target,paint);
            }
            if(c.loot) {
                paint.setColor(0xffeeeeee);canvas.drawRect(r.left+2,r.bottom-size*.22f,r.left+size*.23f,r.bottom-2,paint);
                paint.setColor(Color.BLACK);paint.setTextSize(size*.2f);canvas.drawText("+",r.left+size*.12f,r.bottom-3,paint);
            }
            if(previous!=null && previous.cells.get(c.index).loot && !c.loot && now-changedAt<180){
                paint.setColor(0xffddddc5);paint.setAlpha((int)(255*(1-(now-changedAt)/180f)));canvas.drawRect(r.left+2,r.bottom-size*.22f,r.left+size*.23f,r.bottom-2,paint);paint.setAlpha(255);animating=true;
            }
            if(c.unit>=0) {
                if(c.unit==0) {
                    paint.setColor(Color.BLACK);canvas.drawRect(r.left+2,r.top+2,r.left+size*.32f,r.top+size*.3f,paint);
                    paint.setColor(Color.WHITE);paint.setTextSize(size*.23f);canvas.drawText("Д",r.left+size*.17f,r.top+size*.25f,paint);
                }
                if(c.unit!=0){paint.setColor(0xff333333);canvas.drawRect(r.left+3,r.bottom-5,r.right-3,r.bottom-2,paint);
                int shownHealth=c.health;BattleBoard.Cell old=oldUnit(c.identity);
                if(old!=null && old.health>c.health)for(Pop pop:pops)if(pop.effect.kind.equals("damage")&&pop.effect.cell==c.index){long age=now-pop.at;if(age<400){float f=Math.max(0,Math.min(1,(age-180)/220f));shownHealth=Math.round(old.health+(c.health-old.health)*f);break;}}
                paint.setColor(0xffff2929);canvas.drawRect(r.left+3,r.bottom-5,r.left+3+(r.width()-6)*shownHealth/Math.max(1,c.maxHealth),r.bottom-2,paint);}
            }
            if(c.selected || c.unit==0) {
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(6);paint.setColor(0xff111717);canvas.drawRect(r,paint);
                paint.setStrokeWidth(c.selected?3:2);paint.setColor(c.selected?0xffd8cbaa:0xffa8bcae);canvas.drawRect(r,paint);
                if(c.selected){float k=size*.18f;canvas.drawLine(r.left,r.top,r.left+k,r.top,paint);canvas.drawLine(r.left,r.top,r.left,r.top+k,paint);canvas.drawLine(r.right,r.bottom,r.right-k,r.bottom,paint);canvas.drawLine(r.right,r.bottom,r.right,r.bottom-k,paint);}paint.setStyle(Paint.Style.FILL);
            }
            if(c.stun>0 || c.slow>0 || c.aimed){
                String badges=(c.stun>0?"✦ ":"")+(c.slow>0?"↓ ":"")+(c.aimed?"⊕":"");
                paint.setColor(0xff1c2322);canvas.drawRect(r.left,r.top,r.right,r.top+size*.25f,paint);
                paint.setColor(0xffe4d5ae);paint.setTextSize(size*.23f);canvas.drawText(badges.trim(),r.centerX(),r.top+size*.20f,paint);
            }
            if(c.hazard) {
                paint.setColor(0xffeeeeee);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);
                canvas.drawRect(cell(c.index,5),paint);
                canvas.drawLine(r.left+5,r.top+5,r.right-5,r.bottom-5,paint);
                canvas.drawLine(r.right-5,r.top+5,r.left+5,r.bottom-5,paint);paint.setStyle(Paint.Style.FILL);
            }
        }
        pops.removeIf(p->now-p.at>12000);
        for(Pop pop:pops){long age=now-pop.at;if(age<0){animating=true;continue;}if(pop.effect.kind.equals("shake")||pop.effect.kind.equals("swing")||pop.effect.kind.equals("collision")||pop.effect.kind.equals("impact")||pop.effect.kind.equals("break")){if(age<400)animating=true;continue;}
            RectF r=cell(pop.effect.cell,0);
            if(pop.effect.kind.equals("damage") && pop.effect.amount>0 && pop.zombie && age>=120 && age<650){
                float t=(age-120)/530f;paint.setColor(0xffa3141a);paint.setAlpha((int)(255*(1-t)));
                for(int i=0;i<9;i++){double angle=(i*2.399963+pop.effect.id);float speed=size*(.08f+(i%4)*.045f);float px=r.centerX()+(float)Math.cos(angle)*speed*t*2;
                    float py=r.centerY()+(float)Math.sin(angle)*speed*t+size*.32f*t*t;canvas.drawCircle(px,py,Math.max(1,size*.025f*(1-t*.5f)),paint);}paint.setAlpha(255);animating=true;
            }
            if(age<180 || age>800)continue;animating=true;
            String value=pop.effect.kind.equals("miss")?"Мимо":pop.effect.kind.equals("stun")?"Оглушён":"−"+pop.effect.amount;
            paint.setTextSize(size*.34f);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(pop.effect.kind.equals("damage")?0xffef8c7b:0xffe0ddce);paint.setAlpha((int)(255*(1-(age-180)/620f)));paint.setShadowLayer(3,0,1,Color.BLACK);canvas.drawText(value,r.centerX(),r.centerY()-(age-180)/620f*size*.45f,paint);paint.clearShadowLayer();paint.setAlpha(255);
        }
        paint.setColor(0xffc6c8bb);paint.setTextSize(11*getResources().getDisplayMetrics().scaledDensity);paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(preview<0?"Клетка → маршрут. Повторное нажатие → идти":"Маршрут к "+coordinate(preview)+" · нажмите клетку ещё раз",getWidth()/2f,getWidth()+12*getResources().getDisplayMetrics().density,paint);
        paint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);paint.setColor(0xffaaa998);canvas.drawText("✦ пропуск хода · ↓ замедление · ⊕ прицел",getWidth()/2f,getWidth()+29*getResources().getDisplayMetrics().density,paint);
        if(animating)postInvalidateOnAnimation();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if(!isEnabled())return false;
        if(event.getAction()==MotionEvent.ACTION_DOWN) {downX=event.getX();downY=event.getY();return true;}
        if(event.getAction()==MotionEvent.ACTION_UP) {
            if(Math.abs(event.getX()-downX)>tile()/2 || Math.abs(event.getY()-downY)>tile()/2)return true;
            int x=(int)Math.floor((event.getX()-gutter)/tile()),y=(int)Math.floor((event.getY()-gutter)/tile());
            if(x>=0 && x<8 && y>=0 && y<8) {
                BattleBoard.Cell c=board.cells.get(y*8+x);
                if(c.index==board.playerCell && (c.label.startsWith("ВХОД") || c.label.startsWith("ПЕРЕХОД") || c.label.startsWith("ДОКУМЕНТЫ"))){action.accept(c.label.startsWith("ВХОД")?0:c.label.startsWith("ПЕРЕХОД")?20:21);performClick();return true;}
                if(c.move){if(preview==c.index){preview=-1;action.accept(100+c.index);}else{preview=c.index;announceForAccessibility("Маршрут к "+coordinate(c.index)+". Нажмите ещё раз для подтверждения.");invalidate();}}
                else if(c.unit>0 && c.unit<8 || c.obstacle>=0){preview=-1;action.accept(100+c.index);}
            }
            performClick();return true;
        }
        return true;
    }
    @Override public boolean performClick() {super.performClick();return true;}
}
