package com.az.scicalc;
import android.content.*;
import android.media.*;
import android.os.*;
import android.view.View;

/** Original locally generated audio and the reference's observed haptic timing. */
final class FeedbackPlayer {
 private static ToneGenerator player;private static int playerVolume=-1;
 private static final Handler timer=new Handler(Looper.getMainLooper());
 private static final Runnable idleRelease=FeedbackPlayer::release;
 static void play(View view,String event,boolean sound,boolean vibration){
  SharedPreferences p=view.getContext().getSharedPreferences("calculator",0);
  String group=event.equals("evaluate")?"equals":event.equals("result")?"result":event.equals("error")?"error":"keys";
  String key=event.equals("delete")?"delete":event.equals("clear")?"clear":group;
  if(sound&&p.getBoolean("sound:"+group,true))tone(p.getString("tone:"+key,group.equals("result")?"Success chirp":group.equals("error")?"Error buzz":key.equals("keys")?"Tap":"Click"),p.getInt("soundVolume",100));
  // '=' belongs to keyboard vibration; only its sound has a separate switch.
  String hapticGroup=group.equals("equals")?"keys":group;
  if(vibration&&p.getBoolean("vibration:"+hapticGroup,!hapticGroup.equals("result")))vibrate(view,p.getString("vibrationPattern:"+hapticGroup,hapticGroup.equals("result")?"Double":hapticGroup.equals("error")?"Triple":"Click"),p.getInt("vibrationIntensity",20),hapticGroup.equals("keys"));
 }
 static void tone(String name,int volume){
  int level=Math.max(0,Math.min(100,volume));if(name.equals("None")||level==0)return;
  if(FeedbackSounds.play(name,level))return;
  try{timer.removeCallbacks(idleRelease);if(player==null||playerVolume!=level){release();player=new ToneGenerator(AudioManager.STREAM_MUSIC,level);playerVolume=level;}
   int type=name.equals("Tap")?ToneGenerator.TONE_PROP_BEEP:name.equals("Double")?ToneGenerator.TONE_PROP_ACK:name.equals("Triple")?ToneGenerator.TONE_PROP_NACK:ToneGenerator.TONE_DTMF_5;
   player.startTone(type,name.equals("Tap")?18:45);timer.postDelayed(idleRelease,1500);
  }catch(RuntimeException ignored){release();}
 }
 static void release(){timer.removeCallbacks(idleRelease);if(player!=null){player.release();player=null;}playerVolume=-1;FeedbackSounds.stop();}
 static void vibrate(View view,String name,int intensity){vibrate(view,name,intensity,true);}
 static void vibrate(View view,String name,int intensity,boolean keyboard){
  if(name.equals("None")||intensity<=0)return;
  try{Vibrator v=view.getContext().getSystemService(Vibrator.class);if(v==null||!v.hasVibrator())return;VibrationEffect effect;
   if(name.equals("Tick")||name.equals("Tap")||name.equals("Heavy")||name.equals("Double tap")){
    int id=name.equals("Tick")?VibrationEffect.EFFECT_TICK:name.equals("Heavy")?VibrationEffect.EFFECT_HEAVY_CLICK:name.equals("Double tap")?VibrationEffect.EFFECT_DOUBLE_CLICK:VibrationEffect.EFFECT_CLICK;effect=VibrationEffect.createPredefined(id);
   }else effect=VibrationEffect.createWaveform(HapticPatterns.timings(name,intensity),-1);
   boolean touch=name.equals("Click")||name.equals("Soft")||name.equals("Strong")||name.equals("Long")||name.equals("Tick")||name.equals("Tap")||name.equals("Heavy")||name.equals("Double tap");
   if(Build.VERSION.SDK_INT>=33)v.vibrate(effect,new VibrationAttributes.Builder().setUsage(touch?VibrationAttributes.USAGE_TOUCH:VibrationAttributes.USAGE_UNKNOWN).build());
   else v.vibrate(effect,new AudioAttributes.Builder().setUsage(touch?AudioAttributes.USAGE_ASSISTANCE_SONIFICATION:AudioAttributes.USAGE_UNKNOWN).build());
  }catch(RuntimeException ignored){}
 }
}
