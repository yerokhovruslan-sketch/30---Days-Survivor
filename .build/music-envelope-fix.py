from pathlib import Path
p=Path('app/src/main/java/com/example/survivor/MusicPlan.java');s=p.read_text(encoding='utf-8').replace('silence()+8_000','silence()+7_000').replace('if(next.archive){track="countdown";}', 'if(next.archive){track=random.nextInt(3)==0?"countdown":"fury";}');p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/com/example/survivor/AdaptiveMusic.java');s=p.read_text(encoding='utf-8').replace('void step(long delta){progress=', 'void step(long delta){if(!ready)return;progress=');p.write_text(s,encoding='utf-8')
