package com.az.scicalc;
import org.junit.Test;
import static org.junit.Assert.*;
public class HapticPatternsTest {
 @Test public void handsetWaveforms(){
  assertArrayEquals(new long[]{0,20},HapticPatterns.timings("Click",20));
  assertArrayEquals(new long[]{0,20,70,20},HapticPatterns.timings("Double",20));
  assertArrayEquals(new long[]{0,20,70,20,70,20},HapticPatterns.timings("Triple",20));
  assertArrayEquals(new long[]{0,10,30,10,30,10},HapticPatterns.timings("Stutter",20));
  assertArrayEquals(new long[]{0,30,90,15},HapticPatterns.timings("Heartbeat",20));
 }
 @Test public void durationSliderKeepsSilentGaps(){
  assertArrayEquals(new long[]{0,50,70,50},HapticPatterns.timings("Double",50));
  assertArrayEquals(new long[]{0,25},HapticPatterns.timings("Soft",50));
 }
 @Test public void disabledAndBounds(){assertEquals(0,HapticPatterns.timings("None",20).length);assertEquals(0,HapticPatterns.timings("Click",0).length);assertArrayEquals(new long[]{0,100},HapticPatterns.timings("Click",200));}
}
