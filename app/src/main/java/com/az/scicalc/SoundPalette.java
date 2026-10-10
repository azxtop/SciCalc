package com.az.scicalc;
final class SoundPalette {
 static final String[] GROUPS={"静音","点击音","滑音","音调","提示音","系统"};
 static final String[][] OPTIONS={{"None"},{"Tick","Click","Tap","Snap","Dot","Marble","Blip","Bounce","Wood","Typewriter","Button","Spark","Pluck","Knock","Thock","Double tick","Rise","Glass","Felt","Coil"},{"Pop","Bubble","Drop","Swipe","Erase","Deflate","Thud","Boom","Sweep down","Laser"},{"Beep","Buzz","Bell","Chime","Ding","Sparkle","Ring","Gong","Harp","Crystal","Marimba","Hum","Organ"},{"Success chirp","Coin","Fanfare","Complete","Error buzz","Denied","Clunk","Wrong","Ping","Double ding","Alarm","Siren","Warn","Tada","Rattle"},{"Key click","Standard","Spacebar","Delete","Return","Invalid","Navigate up","Navigate down","Navigate left","Navigate right","Back","System beep","System beep 2","Dial tone"}};
 static String asset(String name){return "feedback/"+name.toLowerCase(java.util.Locale.ROOT).replace(' ','_')+".wav";}
}
