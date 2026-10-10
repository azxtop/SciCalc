package com.az.scicalc;
import android.content.Context;
import android.media.*;
import java.util.*;

/** Preloaded samples avoid allocating audio devices for each rapid key tap. */
final class FeedbackSounds {
 private static SoundPool pool;private static final Map<String,Integer> ids=new HashMap<>();
 private static final Set<Integer> ready=new HashSet<>();private static final ArrayList<Integer> streams=new ArrayList<>();
 static synchronized void prepare(Context context){
  if(pool!=null)return;
  pool=new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build()).build();
  pool.setOnLoadCompleteListener((source,id,status)->{synchronized(FeedbackSounds.class){if(source==pool&&status==0)ready.add(id);}});
  for(String[] group:SoundPalette.OPTIONS)for(String name:group)if(!name.equals("None")){
   try(android.content.res.AssetFileDescriptor asset=context.getAssets().openFd(SoundPalette.asset(name))){ids.put(name,pool.load(asset,1));}catch(java.io.IOException ignored){}
  }
 }
 static synchronized boolean play(String name,int volume){
  Integer id=ids.get(name);if(pool==null||id==null||!ready.contains(id))return false;
  float gain=Math.max(0,Math.min(100,volume))/100f;int stream=pool.play(id,gain,gain,1,0,1);streams.add(stream);while(streams.size()>8)streams.remove(0);return stream!=0;
 }
 static synchronized void stop(){if(pool!=null)for(int id:streams)pool.stop(id);streams.clear();}
}
