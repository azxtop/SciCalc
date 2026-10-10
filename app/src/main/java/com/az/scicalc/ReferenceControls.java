package com.az.scicalc;
import android.graphics.drawable.*;
import android.widget.*;
import android.view.Gravity;

final class ReferenceControls {
 static GradientDrawable shape(int color,int border,int w,int h,float radius,float density){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius*density);d.setSize(Math.round(w*density),Math.round(h*density));if(border!=0)d.setStroke(Math.round(2*density),border);return d;}
 static StateListDrawable states(android.graphics.drawable.Drawable on,android.graphics.drawable.Drawable off){StateListDrawable d=new StateListDrawable();d.addState(new int[]{android.R.attr.state_checked},on);d.addState(new int[]{},off);return d;}
 static void toggle(Switch s){float d=s.getResources().getDisplayMetrics().density;s.setTrackTintList(null);s.setThumbTintList(null);s.setSwitchMinWidth(Math.round(52*d));s.setTrackDrawable(states(shape(ToolPage.ACCENT,ToolPage.ACCENT,52,32,18,d),shape(0xff343339,0xff53545e,52,32,18,d)));s.setThumbDrawable(states(shape(0xff000000,0,24,24,12,d),shape(0xff777681,0,16,16,8,d)));s.setAlpha(s.isEnabled()?1:.3f);}
 static void slider(SeekBar s,int active,int background,int thumb){float d=s.getResources().getDisplayMetrics().density;GradientDrawable bg=shape(background,0,320,16,8,d),fg=shape(active,0,320,16,8,d);LayerDrawable progress=new LayerDrawable(new android.graphics.drawable.Drawable[]{bg,new ClipDrawable(fg,Gravity.LEFT,ClipDrawable.HORIZONTAL)});progress.setId(0,android.R.id.background);progress.setId(1,android.R.id.progress);s.setProgressTintList(null);s.setProgressBackgroundTintList(null);s.setThumbTintList(null);s.setProgressDrawable(progress);s.setMinHeight(Math.round(16*d));s.setMaxHeight(Math.round(16*d));s.setThumb(shape(thumb,0,4,44,2,d));s.setThumbOffset(0);s.setAlpha(s.isEnabled()?1:.3f);}
}
