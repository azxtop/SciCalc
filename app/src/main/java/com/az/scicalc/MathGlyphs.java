package com.az.scicalc;
import android.graphics.*;

/** Thin mathematical capitals with explicit serifs, independent of Android fallback fonts. */
final class MathGlyphs {
 static void draw(Canvas c,Paint p,boolean product,float x,float y,float w,float h,int color){
  c.save();c.translate(x,y);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(1.2f,h*.037f));p.setStrokeCap(Paint.Cap.BUTT);p.setStrokeJoin(Paint.Join.MITER);Path q=new Path();
  if(product){
   c.drawLine(0,0,w,0,p);c.drawLine(w*.08f,h,w*.38f,h,p);c.drawLine(w*.62f,h,w*.92f,h,p);
   p.setStyle(Paint.Style.FILL);c.drawRect(w*.19f,0,w*.27f,h,p);c.drawRect(w*.73f,0,w*.81f,h,p);
  }else{
   q.moveTo(w*.96f,h*.10f);q.lineTo(w*.91f,0);q.lineTo(w*.04f,0);q.lineTo(w*.49f,h*.49f);q.lineTo(w*.02f,h);q.lineTo(w*.92f,h);q.lineTo(w,h*.87f);c.drawPath(q,p);
   p.setStyle(Paint.Style.FILL);Path thick=new Path();thick.moveTo(w*.04f,0);thick.lineTo(w*.12f,0);thick.lineTo(w*.55f,h*.49f);thick.lineTo(w*.49f,h*.49f);thick.close();c.drawPath(thick,p);
  }
  p.setStyle(Paint.Style.FILL);c.restore();
 }
}
