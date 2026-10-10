package com.az.scicalc;
import android.graphics.Typeface;
import java.io.File;

/** Use the platform's original Roboto faces when OEM aliases replace sans-serif. */
public final class UiFonts {
 public static final Typeface REGULAR=load(new String[]{"/system/fonts/RobotoStatic-Regular.ttf","/system/fonts/Roboto-Regular.ttf"},"sans-serif");
 public static final Typeface MONO=load(new String[]{"/system/fonts/DroidSansMono.ttf"},"monospace");
 public static final Typeface MONO_BOLD=Typeface.create(MONO,Typeface.BOLD);
 public static final Typeface ITALIC=Typeface.create(REGULAR,Typeface.ITALIC);
 private static Typeface load(String[] paths,String family){for(String path:paths)try{if(new File(path).isFile())return Typeface.createFromFile(path);}catch(Exception ignored){}return Typeface.create(family,Typeface.NORMAL);}
}
