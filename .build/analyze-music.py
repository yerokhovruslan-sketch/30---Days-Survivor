import pathlib,subprocess,re,json,concurrent.futures,math
from mutagen.mp3 import MP3
import imageio_ffmpeg
folder=pathlib.Path(r'C:/Users/Полина/Desktop/Урезанные'); exe=imageio_ffmpeg.get_ffmpeg_exe()
def inspect(p):
 duration=MP3(p).info.length
 result=subprocess.run([exe,'-hide_banner','-i',str(p),'-af','silencedetect=noise=-50dB:d=0.35,volumedetect','-f','null','-'],capture_output=True,text=True,encoding='utf-8',errors='replace')
 if result.returncode:raise RuntimeError(p.name)
 log=result.stderr;starts=list(map(float,re.findall(r'silence_start: ([0-9.]+)',log)));ends=list(map(float,re.findall(r'silence_end: ([0-9.]+)',log)))
 start=ends[0] if starts and starts[0]<.1 and ends else 0
 end=starts[-1] if starts and ends and ends[-1]>=duration-.2 else duration
 mean=float(re.search(r'mean_volume: ([-0-9.]+)',log).group(1));peak=float(re.search(r'max_volume: ([-0-9.]+)',log).group(1))
 gain=min(1.,10**((-22-mean)/20))
 return p.stem,dict(durationMs=round(duration*1000),startMs=round(start*1000),endMs=round(end*1000),gain=round(gain,4),meanDb=mean,peakDb=peak)
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:data=dict(pool.map(inspect,folder.glob('*.mp3')))
pathlib.Path('output/music-analysis.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
print('Analyzed',len(data),'tracks; trailing silence >350ms:',[(k,round((v['durationMs']-v['endMs'])/1000,1)) for k,v in data.items() if v['durationMs']-v['endMs']>350])
selected='Anthem Ash Ashfall Bunker Concrete Countdown Decay Digital Dreams Echoes Elegy Fading Forgotten Frenzy Fury Graveyard Hunted Light'.split()
import shutil
for name in selected:shutil.copyfile(folder/(name+'.mp3'),pathlib.Path('app/src/main/res/raw')/('music_'+name.lower()+'.mp3'))
pathlib.Path('app/src/main/res/raw/music_catalogue.json').write_text(json.dumps({k.lower():data[k] for k in selected}),encoding='utf-8')
print('Included',len(selected),'described tracks; others remain in source folder')
