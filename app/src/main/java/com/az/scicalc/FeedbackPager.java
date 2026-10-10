package com.az.scicalc;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.view.*;
import android.widget.SeekBar;

/** Two persistent pages, with native child scrolling and independent tab state. */
final class FeedbackPager extends ViewGroup {
 private int selected,pointer=-1;private float position,startX,startY;private boolean dragging,slider;
 private final int slop,minVelocity;private VelocityTracker velocity;private ValueAnimator animator;
 private final java.util.function.IntConsumer changed;
 FeedbackPager(Context context,int tab,java.util.function.IntConsumer callback){super(context);selected=tab;position=tab;changed=callback;ViewConfiguration c=ViewConfiguration.get(context);slop=c.getScaledTouchSlop();minVelocity=Math.max(c.getScaledMinimumFlingVelocity(),Math.round(400*getResources().getDisplayMetrics().density));setClipChildren(true);}
 void select(int tab){settle(Math.max(0,Math.min(1,tab)));}
 private void settle(int tab){if(animator!=null)animator.cancel();selected=tab;changed.accept(tab);animator=ValueAnimator.ofFloat(position,tab);animator.setDuration(220);animator.setInterpolator(new android.view.animation.DecelerateInterpolator());animator.addUpdateListener(a->{position=(float)a.getAnimatedValue();requestLayout();});animator.start();}
 @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),MeasureSpec.getSize(h));for(int i=0;i<getChildCount();i++)getChildAt(i).measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(getMeasuredHeight(),MeasureSpec.EXACTLY));}
 @Override protected void onLayout(boolean changed,int l,int t,int r,int b){for(int i=0;i<getChildCount();i++){int left=Math.round((i-position)*getWidth());getChildAt(i).layout(left,0,left+getWidth(),getHeight());}}
 private boolean onSlider(View view,int x,int y){if(view instanceof SeekBar){Rect bounds=new Rect();return view.getGlobalVisibleRect(bounds)&&bounds.contains(x,y);}if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(onSlider(group.getChildAt(i),x,y))return true;}return false;}
 @Override public boolean onInterceptTouchEvent(MotionEvent e){
  if(e.getActionMasked()==MotionEvent.ACTION_DOWN){if(animator!=null)animator.cancel();pointer=e.getPointerId(0);startX=e.getX();startY=e.getY();dragging=false;slider=onSlider(this,(int)e.getRawX(),(int)e.getRawY());if(velocity!=null)velocity.recycle();velocity=VelocityTracker.obtain();velocity.addMovement(e);}
  else if(e.getActionMasked()==MotionEvent.ACTION_MOVE){int index=e.findPointerIndex(pointer);if(index>=0&&!slider){float dx=e.getX(index)-startX,dy=e.getY(index)-startY;if(Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.2f){dragging=true;getParent().requestDisallowInterceptTouchEvent(true);return true;}}}
  else if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){releaseVelocity();pointer=-1;if(position!=selected)settle(selected);}
  return dragging;
 }
 @Override public boolean onTouchEvent(MotionEvent e){
  if(velocity!=null)velocity.addMovement(e);int index=e.findPointerIndex(pointer);if(index<0)return true;
  if(e.getActionMasked()==MotionEvent.ACTION_MOVE){position=Math.max(0,Math.min(1,selected-(e.getX(index)-startX)/getWidth()));requestLayout();}
  else if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){int target=selected;if(e.getActionMasked()==MotionEvent.ACTION_UP){float v=0;if(velocity!=null){velocity.computeCurrentVelocity(1000);v=velocity.getXVelocity(pointer);}target=Math.abs(v)>minVelocity?(v<0?selected+1:selected-1):Math.round(position);}dragging=false;pointer=-1;releaseVelocity();settle(Math.max(0,Math.min(1,target)));}
  else if(e.getActionMasked()==MotionEvent.ACTION_POINTER_DOWN){dragging=false;pointer=-1;releaseVelocity();settle(selected);}
  return true;
 }
 private void releaseVelocity(){if(velocity!=null){velocity.recycle();velocity=null;}}
 @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();position=selected;releaseVelocity();super.onDetachedFromWindow();}
}
