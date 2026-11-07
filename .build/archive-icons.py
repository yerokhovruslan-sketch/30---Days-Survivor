from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/BattleBoardView.java');s=p.read_text(encoding='utf-8');a=s.index('                paint.setColor(Color.BLACK);canvas.drawRect',s.index('if(c.label.startsWith("ВХОД")'));z=s.index('\n            }',a)
s=s[:a]+'''                int marker=c.label.startsWith("ДОКУМЕНТЫ")?R.drawable.archive_cabinet:R.drawable.archive_exit;
                RectF target=c.unit>=0?new RectF(r.left+2,r.top+2,r.left+size*.42f,r.top+size*.42f):cell(c.index,size*.10f);
                paint.setColor(0xff303030);canvas.drawRect(target,paint);paint.setColor(Color.WHITE);canvas.drawBitmap(bitmap(marker),null,target,paint);''' +s[z:]
s=s.replace('                if(c.move || c.unit>0', '''                if(c.index==board.playerCell && (c.label.startsWith("ВХОД") || c.label.startsWith("ПЕРЕХОД") || c.label.startsWith("ДОКУМЕНТЫ"))){action.accept(c.label.startsWith("ВХОД")?0:c.label.startsWith("ПЕРЕХОД")?20:21);performClick();return true;}
                if(c.move || c.unit>0''');p.write_text(s,encoding='utf-8')
# Preserve case records until actual location departure, without duplicating mid-visit.
p=Path('app/src/main/java/com/example/survivor/game/WorldStories.java');s=p.read_text(encoding='utf-8').replace('void leavePoliceLocation(){archiveAttempt=null;}', 'void leavePoliceLocation(){if(archiveAttempt!=null)for(TacticalBattle b:archiveAttempt.rooms)b.depositCases();archiveAttempt=null;}');p.write_text(s,encoding='utf-8')
