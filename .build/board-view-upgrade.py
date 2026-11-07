from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/BattleBoardView.java');s=p.read_text(encoding='utf-8').replace('private final BattleBoard board;', 'private BattleBoard board;\n    private BattleBoard previous;\n    private long changedAt,lastEffect;\n    private int preview=-1;\n    private final java.util.List<Pop> pops=new java.util.ArrayList<>();\n    private static final class Pop {final BattleBoard.Effect effect;final long at;Pop(BattleBoard.Effect e,long at){effect=e;this.at=at;}}')
s=s.replace('        super(context);this.board=board;this.action=action;', '        super(context);this.board=board;this.action=action;lastEffect=board.effects.isEmpty()?0:board.effects.get(board.effects.size()-1).id;')
at=s.index('    private int[] ids(')
s=s[:at]+'''    void update(BattleBoard next){
        if(board==next)return;
        long now=android.os.SystemClock.uptimeMillis();
        if(board.epoch!=next.epoch){previous=null;pops.clear();preview=-1;lastEffect=next.effects.isEmpty()?0:next.effects.get(next.effects.size()-1).id;}
        else {
            previous=board;int order=0;
            for(BattleBoard.Effect e:next.effects)if(e.id>lastEffect){pops.add(new Pop(e,now+order++*90L));lastEffect=e.id;}
            if(board.playerCell!=next.playerCell || preview>=0 && !next.cells.get(preview).move)preview=-1;
        }
        board=next;changedAt=now;invalidate();
    }
    private BattleBoard.Cell oldUnit(int identity){if(previous==null || identity<0)return null;for(BattleBoard.Cell c:previous.cells)if(c.identity==identity)return c;return null;}
    private RectF figure(BattleBoard.Cell c,long now,float inset){
        RectF result=cell(c.index,inset);BattleBoard.Cell old=oldUnit(c.identity);
        if(old==null || old.index==c.index || now-changedAt>=240)return result;
        java.util.List<Integer> route=BoardVisuals.path(board,old.index,c.index);
        if(route.size()<2)return result;
        float progress=Math.min(1,(now-changedAt)/240f)*(route.size()-1);int segment=Math.min(route.size()-2,(int)progress);float fraction=progress-segment;
        RectF from=cell(route.get(segment),inset),to=cell(route.get(segment+1),inset);
        result.offset(from.left+(to.left-from.left)*fraction-result.left,from.top+(to.top-from.top)*fraction-result.top);return result;
    }
    private void drawRoute(Canvas canvas){
        if(preview<0)return;java.util.List<Integer> path=BoardVisuals.path(board,board.playerCell,preview);if(path.size()<2)return;
        paint.setColor(0xffc9c0a0);paint.setStrokeWidth(3*getResources().getDisplayMetrics().density);paint.setStyle(Paint.Style.STROKE);Path line=new Path();
        for(int i=0;i<path.size();i++){RectF r=cell(path.get(i),0);if(i==0)line.moveTo(r.centerX(),r.centerY());else line.lineTo(r.centerX(),r.centerY());}canvas.drawPath(line,paint);
        canvas.drawRoundRect(cell(preview,4),5,5,paint);paint.setStyle(Paint.Style.FILL);
    }
''' +s[at:]
s=s.replace('setMeasuredDimension(width,width);', 'setMeasuredDimension(width,width+(int)(28*getResources().getDisplayMetrics().density));')
s=s.replace('        super.onDraw(canvas);float size=tile();', '        super.onDraw(canvas);float size=tile();long now=android.os.SystemClock.uptimeMillis();boolean animating=previous!=null && now-changedAt<260;')
s=s.replace('        canvas.drawBitmap(bitmap(R.drawable.battle_board),null,new RectF(gutter,gutter,getWidth()-gutter,getWidth()-gutter),paint);', '''        for(int i=0;i<64;i++){paint.setColor(((i/8+i%8)&1)==0?0xff303636:0xff555b58);canvas.drawRect(cell(i,0),paint);}
        paint.setColor(0xff737974);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);for(int i=0;i<64;i++)canvas.drawRect(cell(i,0),paint);paint.setStyle(Paint.Style.FILL);
        drawRoute(canvas);
        if(board.shotTarget>=0){RectF a=cell(board.playerCell,0),b=cell(board.shotTarget,0);paint.setColor(board.shotBlocked?0xffb76058:0xffa6b5aa);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setPathEffect(new DashPathEffect(new float[]{7,6},0));canvas.drawLine(a.centerX(),a.centerY(),b.centerX(),b.centerY(),paint);paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
            if(board.shotBlocked){float x=(a.centerX()+b.centerX())/2,y=(a.centerY()+b.centerY())/2;canvas.drawLine(x-6,y-6,x+6,y+6,paint);canvas.drawLine(x+6,y-6,x-6,y+6,paint);}}
''')
s=s.replace('RectF r=cell(c.index,1);', 'RectF r=c.unit>=0?figure(c,now,1):cell(c.index,1);')
s=s.replace('if(c.move) {paint.setAlpha(120);', 'if(c.move) {paint.setColor(0xff9aaea0);paint.setAlpha(75);canvas.drawRect(cell(c.index,2),paint);paint.setAlpha(180);')
s=s.replace('            if(resource!=0)canvas.drawBitmap(bitmap(resource),null,cell(c.index,size*.07f),paint);', '''            if(resource==0 && previous!=null && previous.cells.get(c.index).obstacle>=0 && now-changedAt<200)resource=obstacles[previous.cells.get(c.index).obstacle];
            RectF sprite=c.unit>=0?figure(c,now,size*.07f):cell(c.index,size*.07f);
            for(Pop pop:pops)if(pop.effect.cell==c.index && pop.effect.kind.equals("shake") && now>=pop.at && now-pop.at<220){sprite.offset((float)Math.sin((now-pop.at)/220f*Math.PI*6)*3,0);animating=true;}
            if(resource!=0)canvas.drawBitmap(bitmap(resource),null,sprite,paint);''')
s=s.replace('canvas.drawRect(cell(c.index,2),paint);', 'canvas.drawRect(r,paint);',1) if False else s
s=s.replace('paint.setStrokeWidth(4);paint.setColor(Color.BLACK);canvas.drawRect(cell(c.index,2),paint);', 'paint.setStrokeWidth(6);paint.setColor(0xff111717);canvas.drawRect(r,paint);')
s=s.replace('paint.setStrokeWidth(2);paint.setColor(Color.WHITE);canvas.drawRect(cell(c.index,3),paint);', 'paint.setStrokeWidth(c.selected?3:2);paint.setColor(c.selected?0xffd8cbaa:0xffa8bcae);canvas.drawRect(r,paint);\n                if(c.selected){float k=size*.18f;canvas.drawLine(r.left,r.top,r.left+k,r.top,paint);canvas.drawLine(r.left,r.top,r.left,r.top+k,paint);canvas.drawLine(r.right,r.bottom,r.right-k,r.bottom,paint);canvas.drawLine(r.right,r.bottom,r.right,r.bottom-k,paint);}')
needle='            if(c.hazard) {'
s=s.replace(needle,'''            if(c.stun>0 || c.slow>0 || c.aimed){
                String badges=(c.stun>0?"✦ ":"")+(c.slow>0?"↓ ":"")+(c.aimed?"⊕":"");
                paint.setColor(0xff1c2322);canvas.drawRect(r.left,r.top,r.right,r.top+size*.25f,paint);
                paint.setColor(0xffe4d5ae);paint.setTextSize(size*.23f);canvas.drawText(badges.trim(),r.centerX(),r.top+size*.20f,paint);
            }
''' +needle)
end=s.index('\n    @Override public boolean onTouchEvent(')
s=s[:end].rstrip()[:-1]+'''        pops.removeIf(p->now-p.at>850);
        for(Pop pop:pops){long age=now-pop.at;if(age<0){animating=true;continue;}if(age>800 || pop.effect.kind.equals("shake"))continue;animating=true;
            RectF r=cell(pop.effect.cell,0);String value=pop.effect.kind.equals("miss")?"Мимо":"−"+pop.effect.amount;
            paint.setTextSize(size*.34f);paint.setTypeface(Typeface.DEFAULT_BOLD);paint.setTextAlign(Paint.Align.CENTER);paint.setAlpha((int)(255*(1-age/800f)));
            paint.setColor(pop.effect.kind.equals("miss")?0xffe0ddce:0xffef8c7b);paint.setAlpha((int)(255*(1-age/800f)));paint.setShadowLayer(3,0,1,Color.BLACK);canvas.drawText(value,r.centerX(),r.centerY()-age/800f*size*.45f,paint);paint.clearShadowLayer();paint.setAlpha(255);
        }
        paint.setColor(0xffc6c8bb);paint.setTextSize(11*getResources().getDisplayMetrics().scaledDensity);paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(preview<0?"Клетка → маршрут. Повторное нажатие → идти":"Маршрут к "+coordinate(preview)+" · нажмите клетку ещё раз",getWidth()/2f,getWidth()+12*getResources().getDisplayMetrics().density,paint);
        if(animating)postInvalidateOnAnimation();
    }
''' +s[end:]
s=s.replace('                if(c.move || c.unit>0 && c.unit<8 || c.obstacle>=0)action.accept(100+c.index);', '''                if(c.move){if(preview==c.index){preview=-1;action.accept(100+c.index);}else{preview=c.index;announceForAccessibility("Маршрут к "+coordinate(c.index)+". Нажмите ещё раз для подтверждения.");invalidate();}}
                else if(c.unit>0 && c.unit<8 || c.obstacle>=0){preview=-1;action.accept(100+c.index);}''')
p.write_text(s,encoding='utf-8')
