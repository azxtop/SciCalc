package com.az.scicalc;
import android.graphics.*;
/** Original vector paths shared by keypad, drawer and plot. */
public final class UiIcons {
 public static void draw(Canvas c,Paint p,String name,float x,float y,float size,int color){
  c.save();c.translate(x,y);c.scale(size/60,size/60);p.setShader(null);p.setColor(color);p.setStrokeWidth(4);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);Path q=new Path();
  switch(name){
   case "image":c.drawRect(-25,-22,25,22,p);p.setStyle(Paint.Style.FILL);q.moveTo(-22,19);q.lineTo(-6,-4);q.lineTo(5,8);q.lineTo(13,-3);q.lineTo(22,19);q.close();c.drawPath(q,p);c.drawCircle(13,-12,4,p);break;
   case "expand":q.moveTo(0,-22);q.lineTo(-23,-22);q.lineTo(-23,23);q.lineTo(23,23);q.lineTo(23,0);c.drawPath(q,p);c.drawLine(-8,8,23,-23,p);c.drawLine(23,-23,7,-23,p);c.drawLine(23,-23,23,-7,p);break;
   case "resize":c.drawLine(-23,0,23,0,p);c.drawLine(-23,0,-13,-10,p);c.drawLine(-23,0,-13,10,p);c.drawLine(23,0,13,-10,p);c.drawLine(23,0,13,10,p);break;
   case "menu":for(int a=-1;a<=1;a++)c.drawRoundRect(-30,a*18-4,30,a*18+4,4,4,p);break;
   case "sigma":c.drawCircle(0,0,28,p);q.moveTo(12,-16);q.lineTo(-10,-16);q.lineTo(3,0);q.lineTo(-10,16);q.lineTo(12,16);c.drawPath(q,p);break;
   case "gear":for(int i=0;i<24;i++){double a=i*Math.PI/12;float r=i%3==0||i%3==1?28:21,xx=(float)Math.sin(a)*r,yy=(float)Math.cos(a)*r;if(i==0)q.moveTo(xx,yy);else q.lineTo(xx,yy);}q.close();c.drawPath(q,p);c.drawCircle(0,0,10,p);break;
   case "palette":q.moveTo(14,-23);q.cubicTo(-20,-36,-33,-7,-24,18);q.cubicTo(-15,38,20,24,14,12);q.cubicTo(2,8,19,-3,14,-23);c.drawPath(q,p);c.drawCircle(-12,-11,4,p);c.drawCircle(-17,4,4,p);c.drawCircle(-8,18,4,p);c.drawCircle(3,-17,4,p);c.drawLine(16,22,27,-27,p);break;
   case "feedback":c.drawRect(-24,-27,24,27,p);for(int i=0;i<3;i++)c.drawLine(-16,-16+i*13,6,-16+i*13,p);c.drawLine(6,20,20,-18,p);c.drawLine(13,23,27,-15,p);break;
   case "sign":c.drawCircle(0,0,28,p);c.drawLine(-15,16,15,-16,p);c.drawLine(-17,-12,-5,-12,p);c.drawLine(-11,-18,-11,-6,p);c.drawLine(6,12,19,12,p);break;
   case "camera":q.moveTo(-25,-17);q.lineTo(-10,-17);q.lineTo(-6,-23);q.lineTo(6,-23);q.lineTo(11,-17);q.lineTo(25,-17);q.lineTo(25,20);q.lineTo(-25,20);q.close();c.drawPath(q,p);c.drawCircle(0,0,12,p);break;
   case "diamond":c.drawCircle(0,0,29,p);q.moveTo(-20,-12);q.lineTo(-10,-20);q.lineTo(13,-20);q.lineTo(21,-12);q.lineTo(0,21);q.close();c.drawPath(q,p);c.drawLine(-20,-12,21,-12,p);c.drawLine(-10,-20,0,-12,p);c.drawLine(13,-20,0,-12,p);c.drawLine(0,-12,0,21,p);break;
   case "cross":c.drawLine(-15,-15,15,15,p);c.drawLine(-15,15,15,-15,p);break;
   case "paste":c.drawRoundRect(-19,-24,19,26,2,2,p);c.drawRoundRect(-8,-28,8,-18,2,2,p);break;
   case "vibration":c.drawRect(-16,-26,16,26,p);c.drawLine(-9,-19,9,-19,p);c.drawLine(-6,19,6,19,p);q.moveTo(-24,-19);q.lineTo(-29,-10);q.lineTo(-24,0);q.lineTo(-29,10);q.lineTo(-24,19);q.moveTo(24,-19);q.lineTo(29,-10);q.lineTo(24,0);q.lineTo(29,10);q.lineTo(24,19);c.drawPath(q,p);break;
   case "close":c.drawCircle(0,0,28,p);c.drawLine(-12,-12,12,12,p);c.drawLine(-12,12,12,-12,p);break;
   case "help":c.drawCircle(0,0,28,p);p.setStyle(Paint.Style.FILL);p.setTextSize(37);p.setTypeface(Typeface.create("sans-serif",1));p.setTextAlign(Paint.Align.CENTER);c.drawText("?",0,13,p);break;
   case "history":c.drawRoundRect(-24,-25,24,25,4,4,p);c.drawLine(-24,-16,24,-16,p);c.drawCircle(0,5,16,p);c.drawLine(0,-5,0,5,p);c.drawLine(0,5,8,10,p);break;
   case "variables":c.drawCircle(0,0,28,p);p.setStyle(Paint.Style.FILL);p.setTextSize(25);p.setTextAlign(Paint.Align.CENTER);c.drawText("abc",0,9,p);break;
   case "graph":c.drawLine(-27,22,-27,-22,p);c.drawLine(-27,3,27,3,p);q.moveTo(-26,15);q.cubicTo(-16,-40,-4,45,5,-3);q.cubicTo(13,-40,19,28,27,-18);c.drawPath(q,p);break;
   case "code":c.drawRect(-25,-25,25,25,p);c.drawLine(-12,-11,0,0,p);c.drawLine(0,0,-12,11,p);c.drawLine(4,13,15,13,p);break;
   case "info":c.drawCircle(0,0,28,p);p.setStyle(Paint.Style.FILL);p.setTypeface(UiFonts.REGULAR);p.setTextSize(44);p.setTextAlign(Paint.Align.CENTER);c.drawText("i",0,16,p);break;
   case "right":c.drawLine(-7,-16,9,0,p);c.drawLine(9,0,-7,16,p);break;
   case "font":c.drawRect(-24,-24,24,24,p);for(int i=-1;i<=1;i+=2)for(int j=-1;j<=1;j+=2)c.drawRect(i*24-4,j*24-4,i*24+4,j*24+4,p);p.setStyle(Paint.Style.FILL);p.setTextSize(36);p.setTextAlign(Paint.Align.CENTER);c.drawText("T",0,13,p);break;
   case "globe":c.drawCircle(0,0,26,p);c.drawOval(-13,-26,13,26,p);c.drawLine(-26,0,26,0,p);c.drawLine(-21,-14,21,-14,p);c.drawLine(-21,14,21,14,p);break;
   case "rotate":c.drawArc(-26,-26,26,26,20,150,false,p);c.drawArc(-26,-26,26,26,200,150,false,p);c.drawLine(-26,0,-28,15,p);c.drawLine(-26,0,-12,5,p);c.drawLine(26,0,28,-15,p);c.drawLine(26,0,12,-5,p);break;
   case "back":c.drawLine(21,0,-20,0,p);c.drawLine(-20,0,-3,-17,p);c.drawLine(-20,0,-3,17,p);break;
   case "home":q.moveTo(-24,0);q.lineTo(0,-22);q.lineTo(24,0);q.lineTo(24,25);q.lineTo(-24,25);q.close();c.drawPath(q,p);break;
   case "reset":c.drawArc(-24,-24,24,24,-55,285,false,p);q.moveTo(-26,-22);q.lineTo(-26,0);q.lineTo(-6,-8);c.drawPath(q,p);break;
   case "circlePlus":case "circleMinus":c.drawCircle(0,0,28,p);c.drawLine(-14,0,14,0,p);if(name.equals("circlePlus"))c.drawLine(0,-14,0,14,p);break;
   case "plus":c.drawLine(-18,0,18,0,p);c.drawLine(0,-18,0,18,p);break;
   case "minus":c.drawLine(-18,0,18,0,p);break;
   case "sort":c.drawLine(-20,23,-20,-23,p);c.drawLine(-20,-23,-29,-12,p);c.drawLine(-20,-23,-11,-12,p);for(int i=0;i<4;i++)c.drawLine(0,-22+i*15,3+i*7,-22+i*15,p);break;
   case "trashClear":case "trash":c.drawLine(-17,-17,17,-17,p);c.drawLine(-6,-23,6,-23,p);p.setStyle(Paint.Style.FILL);c.drawRoundRect(-14,-11,14,24,3,3,p);if(name.equals("trashClear")){p.setColor(0xffd4e4e3);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawLine(-7,-3,7,11,p);c.drawLine(7,-3,-7,11,p);}break;
   case "copy":c.drawRoundRect(-11,-15,23,25,1,1,p);q.moveTo(10,-25);q.lineTo(-23,-25);q.lineTo(-23,14);c.drawPath(q,p);break;
   case "historyEdit":q.moveTo(-1,-22);q.lineTo(-23,-22);q.lineTo(-23,25);q.lineTo(23,25);q.lineTo(23,5);c.drawPath(q,p);draw(c,p,"edit",9,-8,45,color);break;
   case "hourglass":c.drawLine(-19,-27,19,-27,p);c.drawLine(-19,27,19,27,p);q.moveTo(-16,-26);q.lineTo(-16,-14);q.lineTo(13,15);q.lineTo(13,25);q.moveTo(16,-26);q.lineTo(16,-14);q.lineTo(-13,15);q.lineTo(-13,25);c.drawPath(q,p);break;
   case "edit":q.moveTo(-19,23);q.lineTo(-14,6);q.lineTo(16,-23);q.lineTo(26,-13);q.lineTo(-4,17);q.close();c.drawPath(q,p);break;
   case "chevron":c.drawLine(-20,9,0,-11,p);c.drawLine(0,-11,20,9,p);break;
   case "more":p.setStyle(Paint.Style.FILL);for(int a=-1;a<=1;a++)c.drawCircle(0,a*16,4,p);break;
   case "search":c.drawCircle(-6,-6,18,p);c.drawLine(8,8,26,26,p);break;
   case "table":c.drawRoundRect(-25,-24,25,24,3,3,p);c.drawLine(-25,-8,25,-8,p);c.drawLine(-25,8,25,8,p);c.drawLine(-8,-24,-8,24,p);break;
   case "calculator":c.drawRoundRect(-22,-28,22,28,4,4,p);c.drawRect(-15,-21,15,-10,p);for(int a=0;a<3;a++)for(int b=0;b<3;b++)c.drawRect(-15+a*12,-2+b*9,-10+a*12,3+b*9,p);break;
   case "tools":q.moveTo(-23,25);q.lineTo(0,-28);q.lineTo(23,25);c.drawPath(q,p);c.drawLine(-17,11,17,11,p);p.setColor(0xffffd600);c.drawCircle(-23,25,4,p);p.setColor(0xffff515b);c.drawCircle(23,25,4,p);break;
   case "crosshair":c.drawLine(-27,0,27,0,p);c.drawLine(0,-27,0,27,p);p.setColor(0xffff4d59);c.drawCircle(0,0,12,p);break;
   case "sound":q.moveTo(-22,-10);q.lineTo(-12,-10);q.lineTo(3,-24);q.lineTo(3,24);q.lineTo(-12,10);q.lineTo(-22,10);q.close();c.drawPath(q,p);c.drawArc(-6,-21,33,21,-60,120,false,p);break;
   case "soundOff":draw(c,p,"sound",0,0,60,color);p.setStyle(Paint.Style.STROKE);c.drawLine(-27,-27,27,27,p);break;
   case "star":for(int i=0;i<10;i++){double a=i*Math.PI/5-Math.PI/2;float r=i%2==0?29:13;if(i==0)q.moveTo((float)Math.cos(a)*r,(float)Math.sin(a)*r);else q.lineTo((float)Math.cos(a)*r,(float)Math.sin(a)*r);}q.close();c.drawPath(q,p);break;
   case "share":c.drawRoundRect(-25,-5,20,27,4,4,p);q.moveTo(-7,10);q.quadTo(-7,-16,12,-16);q.lineTo(12,-28);q.lineTo(28,-13);q.lineTo(12,1);q.lineTo(12,-10);q.quadTo(-2,-10,-7,10);c.drawPath(q,p);break;
   default:c.drawCircle(0,0,25,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(35);c.drawText("?",0,12,p);
  }p.setStyle(Paint.Style.FILL);c.restore();
 }
}
