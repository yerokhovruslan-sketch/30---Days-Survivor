from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/BattleBoardView.java');s=p.read_text(encoding='utf-8').replace('width+(int)(28*', 'width+(int)(44*')
s=s.replace('            if(c.unit>=0) {\n                if(c.unit==0)', '''            if(previous!=null && previous.cells.get(c.index).loot && !c.loot && now-changedAt<180){
                paint.setColor(0xffddddc5);paint.setAlpha((int)(255*(1-(now-changedAt)/180f)));canvas.drawRect(r.left+2,r.bottom-size*.22f,r.left+size*.23f,r.bottom-2,paint);paint.setAlpha(255);animating=true;
            }
            if(c.unit>=0) {
                if(c.unit==0)''')
s=s.replace('        if(animating)postInvalidateOnAnimation();', '        paint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);paint.setColor(0xffaaa998);canvas.drawText("✦ пропуск хода · ↓ замедление · ⊕ прицел",getWidth()/2f,getWidth()+29*getResources().getDisplayMetrics().density,paint);\n        if(animating)postInvalidateOnAnimation();')
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/BoardVisuals.java');s=p.read_text(encoding='utf-8').replace('final class BoardVisuals', 'public final class BoardVisuals').replace(' static List<Integer> path(', ' public static List<Integer> path(');p.write_text(s,encoding='utf-8')
