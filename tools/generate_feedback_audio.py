"""Generate original short feedback sounds; reference recordings are not used."""
from pathlib import Path
import math,random,re,wave,struct
root=Path(__file__).resolve().parents[1]
source=(root/'app/src/main/java/com/az/scicalc/SoundPalette.java').read_text(encoding='utf-8')
names=re.findall(r'"([^"]+)"',source.split('OPTIONS=')[1].split('static String')[0])
out=root/'app/src/main/assets/feedback';out.mkdir(parents=True,exist_ok=True)
clicks=names[1:21];glides=names[21:31]
for ordinal,name in enumerate(names):
 if name=='None':continue
 rng=random.Random(ordinal);rate=22050
 duration=.038+ordinal%5*.008 if name in clicks else .12+ordinal%5*.025 if name in glides else .24+ordinal%4*.06
 if name in ['Success chirp','Error buzz','Double ding','Double tick']:duration=.18 if name=='Double tick' else .28
 if name in ['Fanfare','Tada','Complete','Alarm','Siren','Dial tone']:duration=.45
 samples=[];phase=0;frequency=240+ordinal*67%1500
 for i in range(int(duration*rate)):
  t=i/rate;u=t/duration;envelope=min(1,t/.002)*math.exp(-u*(7 if name in clicks else 4))
  f=frequency
  if name in glides:f*=1.8-1.5*u
  if name in ['Rise','Success chirp','Coin','Fanfare','Complete','Tada']:f*=1+math.floor(u*3)*.35
  if name in ['Sweep down','Denied','Wrong','Error buzz']:f=max(70,250*(1-u*.65))
  if name=='Siren':f=650+250*math.sin(t*30)
  phase+=2*math.pi*f/rate
  noise=rng.uniform(-1,1)
  value=.65*math.sin(phase)+.2*math.sin(phase*2.03)+.1*math.sin(phase*3.97)
  if name in clicks:value=value*.4+noise*.6
  if name in ['Swipe','Erase','Deflate','Rattle']:value=noise*.8
  if name in ['Buzz','Error buzz','Alarm','Hum']:value=math.tanh(3*math.sin(phase))*.7
  if name in ['Double ding','Double tick']:envelope*=math.exp(-(u% .5)*10)
  samples.append(int(max(-1,min(1,value*envelope*.5))*32767))
 with wave.open(str(out/(name.lower().replace(' ','_')+'.wav')),'wb') as stream:
  stream.setnchannels(1);stream.setsampwidth(2);stream.setframerate(rate);stream.writeframes(struct.pack('<'+'h'*len(samples),*samples))
print('Generated',len(names)-1,'original feedback sounds')
