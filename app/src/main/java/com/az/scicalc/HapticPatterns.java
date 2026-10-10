package com.az.scicalc;

/** Timing sequences measured from the reference on the test handset. */
final class HapticPatterns {
 static final String[] NAMES={"None","Click","Soft","Strong","Long","Double","Triple","Stutter","Knock","Ramp up","Ramp down","Heartbeat","Thump","Tick","Tap","Heavy","Double tap"};
 static long[] timings(String name,int intensity){
  long[] base;
  switch(name){
   case "None":return new long[0];
   case "Soft":base=new long[]{0,10};break;
   case "Strong":base=new long[]{0,40};break;
   case "Long":base=new long[]{0,80};break;
   case "Double":base=new long[]{0,20,70,20};break;
   case "Triple":base=new long[]{0,20,70,20,70,20};break;
   case "Stutter":base=new long[]{0,10,30,10,30,10};break;
   case "Knock":base=new long[]{0,20,140,20};break;
   case "Ramp up":base=new long[]{0,10,40,20,40,40};break;
   case "Ramp down":base=new long[]{0,40,40,20,40,10};break;
   case "Heartbeat":base=new long[]{0,30,90,15};break;
   case "Thump":base=new long[]{0,10,60,50};break;
   default:base=new long[]{0,20};
  }
  int strength=Math.max(0,Math.min(100,intensity));if(strength==0)return new long[0];
  for(int i=1;i<base.length;i+=2)base[i]=Math.max(1,Math.round(base[i]*strength/20f));return base;
 }
}
