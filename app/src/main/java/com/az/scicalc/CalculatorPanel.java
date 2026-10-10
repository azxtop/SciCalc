package com.az.scicalc;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.OverScroller;
import android.animation.*;
import java.util.*;

/** Scrollable overlays preserve the calculator beneath, including its exact geometry. */
public final class CalculatorPanel extends View {
 public interface Listener {void selected(String action);void close();void position(float progress);}
 private final Listener listener;private final boolean drawer;private final Paint p=new Paint(3);public boolean basic=false,sound=false;
 private float sx=1,sy=1,offset,startY,lastY,startX,startProgress,progress=0;private boolean dragged,closing;private final ArrayList<Row> rows=new ArrayList<>();
 private ValueAnimator animator;private final OverScroller scroller;private VelocityTracker velocity;private final int touchSlop,minVelocity,maxVelocity;
 private int activePointer=-1;private boolean clickCancelled,held;
 private Hit pressedHit;private android.graphics.drawable.RippleDrawable ripple;
 private final android.os.Handler holdTimer=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable soundHold=this::onSoundHold;
 private void onSoundHold(){if(!closing&&!dragged&&!clickCancelled&&pressedHit!=null&&pressedHit.action.equals("toggleSound")){held=true;listener.selected("sound");}}
 private static final class Hit {final String action;final RectF bounds;Hit(String a,RectF b){action=a;bounds=b;}}
 private static class Row {String title,subtitle,action;boolean sigma;float height;Row(String a,String b,String c,boolean s,float h){title=a;subtitle=b;action=c;sigma=s;height=h;}}
 public CalculatorPanel(Context c,boolean drawer,Listener l){super(c);this.drawer=drawer;listener=l;scroller=new OverScroller(c);ViewConfiguration config=ViewConfiguration.get(c);touchSlop=config.getScaledTouchSlop();minVelocity=config.getScaledMinimumFlingVelocity();maxVelocity=config.getScaledMaximumFlingVelocity();setContentDescription(drawer?"主菜单":"计算模式");
  rows.add(new Row("基本算术","基本算术计算","normal",false,173));rows.add(new Row("复数","复数计算","complex",true,170));rows.add(new Row("统计","统计计算","stats",true,170));rows.add(new Row("基数","基数计算（二进制、八进制、十进制、\n十六进制）","bases",false,226));rows.add(new Row("数字表","根据表达式生成数字表","table",false,170));rows.add(new Row("矩阵","矩阵计算","matrix",true,170));rows.add(new Row("向量","向量计算","vector",false,170));
  for(int n=2;n<=5;n++)rows.add(new Row(new String[]{"","","ax²+bx+c = 0","ax³+bx²+cx+ … = 0","ax⁴+bx³+cx²+ … = 0","ax⁵+bx⁴+cx³+ … = 0"}[n],"解"+n+"次多项式","poly:"+n,n<=3,170));
  for(int n=2;n<=4;n++)rows.add(new Row(n==2?"aₙx + bₙy = cₙ":n==3?"aₙx + bₙy + cₙz = dₙ":"aₙx + bₙy + cₙz + dₙt = eₙ","解"+n+"元方程组","system:"+n,n==2,170));
  String[] op={">","<","≥","≤"};for(int n=2;n<=4;n++)for(int i=0;i<4;i++)rows.add(new Row((n==2?"ax²+bx+c":n==3?"ax³+bx²+cx+ …":"ax⁴+bx³+cx²+ …")+" "+op[i]+" 0","解"+n+"次不等式","inequality:"+n+":"+i,n==2&&i==0,170));
  rows.add(new Row("微积分","求导、积分、极限与符号运算","calculus",false,170));rows.add(new Row("概率分布","正态、二项与泊松分布","distribution",false,170));rows.add(new Row("图形","函数与曲线","graph",false,170));
 }
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){sx=w/1080f;sy=h/2400f;}
 private void rect(Canvas c,float x,float y,float w,float h,float r,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
 private void text(Canvas c,String s,float x,float y,float size,int color){p.setTypeface(Typeface.create("sans-serif",0));p.setTextSize(size);p.setTextAlign(Paint.Align.LEFT);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawText(s,x,y,p);}
 private void icon(Canvas c,String s,float x,float y,float size,int color){UiIcons.draw(c,p,s,x,y,size,color);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();animateTo(1,null);}
 void prepareDrag(){if(animator!=null){animator.removeAllListeners();animator.cancel();}progress=0;listener.position(0);postInvalidateOnAnimation();}
 void dismiss(Runnable done){if(closing)return;closing=true;scroller.forceFinished(true);animateTo(0,done);}
 private void animateTo(float target,Runnable done){
  if(animator!=null){animator.removeAllListeners();animator.cancel();}float from=progress;
  animator=ValueAnimator.ofFloat(from,target);animator.setDuration(Math.max(80,Math.round((drawer?250:150)*Math.abs(target-from))));animator.setInterpolator(drawer?new android.view.animation.DecelerateInterpolator(2.5f):new android.view.animation.DecelerateInterpolator());
  animator.addUpdateListener(a->{progress=(float)a.getAnimatedValue();if(drawer)listener.position(progress);postInvalidateOnAnimation();});animator.addListener(new AnimatorListenerAdapter(){public void onAnimationEnd(Animator a){if(done!=null)done.run();}});animator.start();
 }
 @Override protected void onDetachedFromWindow(){holdTimer.removeCallbacksAndMessages(null);if(animator!=null){animator.removeAllListeners();animator.cancel();}scroller.forceFinished(true);if(velocity!=null){velocity.recycle();velocity=null;}super.onDetachedFromWindow();}
 @Override public void computeScroll(){if(scroller.computeScrollOffset()){offset=scroller.getCurrY()/sy;postInvalidateOnAnimation();}}
 private float maxOffset(){float total=0;for(Row r:rows)total+=r.height;return Math.max(0,total-2077);}
 @Override protected void onDraw(Canvas c){c.save();c.scale(sx,sy);c.drawColor(Color.argb(Math.round(102*progress),0,0,0));if(drawer){c.translate(-866*(1-progress),0);drawDrawer(c);if(ripple!=null)ripple.draw(c);}else{int layer=c.saveLayerAlpha(70,120,1010,2317,Math.round(255*progress));drawMode(c);if(ripple!=null){c.save();c.clipRect(70,240,1010,2317);ripple.draw(c);c.restore();}c.restoreToCount(layer);}c.restore();}
 private void drawMode(Canvas c){
  rect(c,70,120,940,2197,4,0xff20282d);c.save();c.clipRect(70,240,1010,2317);float y=240-offset;
  for(int i=0;i<rows.size();i++){Row r=rows.get(i);if(y+r.height>=240&&y<=2317){text(c,r.title,93,y+74,50,0xffd7d3de);String[] lines=r.subtitle.split("\n");for(int n=0;n<lines.length;n++)text(c,lines[n],93,y+137+n*57,44,0xffc9c5d1);if(i>0){if(r.sigma)icon(c,"sigma",831,y+r.height/2,60,0xffb488ff);icon(c,"help",937,y+r.height/2,60,0xffc9c5d1);}p.setColor(0xff3b4248);p.setStrokeWidth(3);c.drawLine(70,y+r.height,1010,y+r.height,p);}y+=r.height;}
  c.restore();rect(c,70,120,940,120,4,0xffb488ff);text(c,"计算模式",93,200,56,Color.BLACK);icon(c,"close",957,172,60,Color.BLACK);
 }
 private void item(Canvas c,String icon,String label,float y){icon(c,icon,89,y-17,55,0xffc9c5d1);text(c,label,148,y,49,0xffc9c5d1);}
 private void drawDrawer(Canvas c){
  rect(c,0,78,866,2322,0,0xff323239);
  String[] icons={"diamond",sound?"sound":"soundOff","feedback","share","star"},labels={"功能","声音","反馈","分享","关于应用"};for(int i=0;i<5;i++){icon(c,icons[i],86+174*i,128,62,i==0?0xff44b8ed:0xffd7d3de);text(c,labels[i],52+174*i,219,i==4?31:36,0xffc9c5d1);}
  rect(c,20,251,826,779,44,0xff20282d);item(c,"history","计算历史",342);item(c,"variables","变量值",466);item(c,"palette","计算器皮肤",591);p.setColor(0xffb488ff);c.drawCircle(654,577,33,p);text(c,"新",633,590,36,Color.BLACK);icon(c,"more",745,577,49,0xffc9c5d1);item(c,"gear","设置",716);item(c,"help","应用手册",841);
  rect(c,64,908,738,93,28,0xff393e44);p.setColor(0xff596068);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawRoundRect(64,908,802,1001,28,28,p);p.setStyle(Paint.Style.FILL);rect(c,basic?433:68,912,361,85,24,0xffb488ff);text(c,"完整键盘",174,970,38,basic?0xffd7d3de:Color.BLACK);text(c,"基础键盘",545,970,38,basic?Color.BLACK:0xffd7d3de);
  rect(c,20,1052,826,140,44,0xff20282d);item(c,"graph","图形",1140);
  rect(c,20,1212,826,272,44,0xff20282d);item(c,"code","编程",1302);item(c,"help","在线手册",1427);
  rect(c,20,1504,826,140,44,0xff20282d);item(c,"history","检查更新",1592);
 }
 private Hit hit(float x,float y){
  if(!drawer){
   if(x<70||x>1010||y<120||y>2317)return new Hit("close",new RectF(x-1,y-1,x+1,y+1));
   if(y<240)return x>900?new Hit("close",new RectF(900,120,1010,240)):null;
   float top=240-offset;for(Row r:rows){if(y>=top&&y<top+r.height){boolean help=x>900;return new Hit(help?"help:"+r.action:r.action,new RectF(help?900:70,Math.max(240,top),help?1010:900,Math.min(2317,top+r.height)));}top+=r.height;}return null;
  }
  if(x>866)return new Hit("close",new RectF(866,78,1080,2400));if(x<0||y<78)return null;
  if(y<183){int column=Math.min(4,(int)(x/174));return new Hit(new String[]{"features","toggleSound","feedback","share","features"}[column],new RectF(column*174,78,Math.min(866,(column+1)*174),183));}
  if(x<20||x>846)return null;
  if(y>=280&&y<880){int row=(int)((y-280)/125);if(row<5)return new Hit(new String[]{"history","variables","skin","settings","manual"}[row],new RectF(20,280+125*row,846,Math.min(880,405+125*row)));}
  if(y>=908&&y<1001&&x>=64&&x<=802)return new Hit(x<433?"fullKeyboard":"basicKeyboard",new RectF(x<433?64:433,908,x<433?433:802,1001));
  if(y>=1052&&y<1192)return new Hit("graph",new RectF(20,1052,846,1192));
  if(y>=1212&&y<1484)return new Hit(y<1348?"programming":"onlineManual",new RectF(20,y<1348?1212:1348,846,y<1348?1348:1484));if(y>=1504&&y<1644)return new Hit("checkUpdate",new RectF(20,1504,846,1644));return null;
 }
 private void pressed(boolean state,float x,float y){
  if(pressedHit==null||pressedHit.action.equals("close"))return;
  if(ripple==null){ripple=new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x30ffffff),null,new android.graphics.drawable.ColorDrawable(Color.WHITE));ripple.setCallback(this);}
  RectF b=pressedHit.bounds;ripple.setBounds((int)b.left,(int)b.top,(int)b.right,(int)b.bottom);ripple.setHotspot(x,y);ripple.setState(state?new int[]{android.R.attr.state_enabled,android.R.attr.state_pressed}:new int[]{android.R.attr.state_enabled});postInvalidateOnAnimation();
 }
 @Override protected boolean verifyDrawable(android.graphics.drawable.Drawable drawable){return drawable==ripple||super.verifyDrawable(drawable);}
 @Override public void invalidateDrawable(android.graphics.drawable.Drawable drawable){postInvalidateOnAnimation();}
 void cancelTouch(){cancelClick(startX,startY);activePointer=-1;if(drawer&&dragged&&!closing)animateTo(1,null);dragged=false;if(velocity!=null){velocity.recycle();velocity=null;}}
 private void cancelClick(float x,float y){clickCancelled=true;holdTimer.removeCallbacks(soundHold);pressed(false,x,y);}
 @Override public boolean onTouchEvent(MotionEvent e){
  int eventAction=e.getActionMasked();if(closing)return true;
  if(eventAction==MotionEvent.ACTION_DOWN){
   activePointer=e.getPointerId(0);float x=e.getX()/sx,y=e.getY()/sy;startX=x;startY=lastY=y;startProgress=progress;dragged=!scroller.isFinished();held=false;clickCancelled=dragged;scroller.forceFinished(true);
   pressedHit=hit(x,y);pressed(true,x,y);if(drawer&&pressedHit!=null&&pressedHit.action.equals("toggleSound"))holdTimer.postDelayed(soundHold,ViewConfiguration.getLongPressTimeout());
   if(velocity!=null)velocity.recycle();velocity=VelocityTracker.obtain();velocity.addMovement(e);return true;
  }
  if(eventAction==MotionEvent.ACTION_CANCEL){cancelClick(startX,startY);activePointer=-1;if(velocity!=null){velocity.recycle();velocity=null;}if(drawer&&dragged)animateTo(1,null);return true;}
  int index=e.findPointerIndex(activePointer);if(index<0)return true;float x=e.getX(index)/sx,y=e.getY(index)/sy;
  if(velocity!=null)velocity.addMovement(e);
  if(eventAction==MotionEvent.ACTION_MOVE){
   if(drawer){
    if(!dragged&&Math.abs(x-startX)*sx>touchSlop&&Math.abs(x-startX)>Math.abs(y-startY)){dragged=true;cancelClick(x,y);if(animator!=null){animator.removeAllListeners();animator.cancel();}}
    if(dragged){progress=Math.max(0,Math.min(1,startProgress+(x-startX)/866));listener.position(progress);postInvalidateOnAnimation();}
    else if(Math.abs(y-startY)*sy>touchSlop)cancelClick(x,y);
   }else if(startY>240&&startY<2317&&startX>=70&&startX<=1010){
    if(!dragged&&Math.abs(y-startY)*sy>touchSlop){dragged=true;cancelClick(x,y);lastY=startY+(y>startY?touchSlop/sy:-touchSlop/sy);}
    if(dragged){offset=Math.max(0,Math.min(maxOffset(),offset+lastY-y));lastY=y;postInvalidateOnAnimation();}
   }
   if(pressedHit!=null&&!pressedHit.bounds.contains(x,y))cancelClick(x,y);return true;
  }
  if((eventAction==MotionEvent.ACTION_UP||eventAction==MotionEvent.ACTION_POINTER_UP)&&e.getPointerId(e.getActionIndex())==activePointer){
   int owner=activePointer;activePointer=-1;holdTimer.removeCallbacks(soundHold);pressed(false,x,y);
   if(dragged){float v=0;if(velocity!=null){velocity.computeCurrentVelocity(1000,maxVelocity);v=drawer?velocity.getXVelocity(owner):velocity.getYVelocity(owner);velocity.recycle();velocity=null;}
    if(drawer){if(v<-minVelocity||Math.abs(v)<=minVelocity&&progress<.5f)listener.close();else animateTo(1,null);}else if(Math.abs(v)>minVelocity){scroller.fling(0,Math.round(offset*sy),0,Math.round(-v),0,0,0,Math.round(maxOffset()*sy));postInvalidateOnAnimation();}return true;
   }
   if(velocity!=null){velocity.recycle();velocity=null;}Hit released=hit(x,y);
   if(!held&&!clickCancelled&&pressedHit!=null&&released!=null&&pressedHit.action.equals(released.action)){
    if(released.action.equals("close"))listener.close();else listener.selected(released.action);performClick();
   }return true;
  }return true;
 }
 @Override public boolean performClick(){super.performClick();return true;}
}
