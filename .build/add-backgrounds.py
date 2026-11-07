from pathlib import Path
from mutagen.mp3 import MP3
import imageio_ffmpeg,subprocess,re,json,math,shutil
sources={'wanderer':'imia-rek-wanderer-393778 (1).mp3','houslast':'houslast-post-apocalypse-music-238598 (1).mp3','tense':'the_mountain-tense-492813 (mp3cut.net) (1).mp3'}
p=Path('app/src/main/res/raw/music_catalogue.json');data=json.loads(p.read_text());exe=imageio_ffmpeg.get_ffmpeg_exe()
for name,file in sources.items():
 src=Path('C:/Users/Полина/Downloads')/file;duration=MP3(src).info.length
 r=subprocess.run([exe,'-hide_banner','-i',str(src),'-af','silencedetect=noise=-50dB:d=0.35,volumedetect','-f','null','-'],capture_output=True,text=True,encoding='utf-8',errors='replace');assert r.returncode==0
 starts=list(map(float,re.findall(r'silence_start: ([0-9.]+)',r.stderr)));ends=list(map(float,re.findall(r'silence_end: ([0-9.]+)',r.stderr)))
 mean=float(re.search(r'mean_volume: ([-0-9.]+)',r.stderr).group(1));peak=float(re.search(r'max_volume: ([-0-9.]+)',r.stderr).group(1))
 start=ends[0] if starts and starts[0]<.1 and ends else 0;end=starts[-1] if starts and ends and ends[-1]>=duration-.2 else duration
 data[name]=dict(durationMs=round(duration*1000),startMs=round(start*1000),endMs=round(end*1000),gain=round(min(1,10**((-22-mean)/20)),4),meanDb=mean,peakDb=peak)
 shutil.copyfile(src,Path('app/src/main/res/raw')/('music_'+name+'.mp3'));print(name,round(duration,1),'sec',src.stat().st_size,'bytes')
p.write_text(json.dumps(data,indent=2),encoding='utf-8')
