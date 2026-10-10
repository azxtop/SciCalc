package com.az.scicalc;
import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

/** One detached, read-only history formula with readable horizontal overflow. */
final class FormulaPreview extends View {
 private final Formula.Row row;
 private final Formula.Renderer renderer=new Formula.Renderer();
 private boolean right;private final float font,pad;
 void alignLeft(){right=false;}
 void monospace(){renderer.typeface=UiFonts.MONO;}
 FormulaPreview(Context c,String tree,String source,boolean result){
  this(c,tree,source,result,ToolPage.TEXT,21);
 }
 FormulaPreview(Context c,String tree,String source,boolean result,int color,float size){
  this(c,tree,source,result,color,size,8);
 }
 FormulaPreview(Context c,String tree,String source,boolean result,int color,float size,float padding){
  super(c);right=result;float density=getResources().getDisplayMetrics().density;font=size*density;pad=padding*density;
  if(result)row=Formula.result(c.getSharedPreferences("calculator",0).getBoolean("descendingPowers",false)?ResultFormat.orderPolynomial(source):source);else {Formula f=new Formula();if(tree!=null&&!tree.isEmpty())f.restore(tree);else f.root=Formula.result(source);row=f.root;}
  android.content.SharedPreferences prefs=c.getSharedPreferences("calculator",0);renderer.groupThousands=prefs.getBoolean("groupThousands",true);renderer.groupDecimals=prefs.getBoolean("groupDecimals",true);renderer.decimalComma=prefs.getBoolean("decimalComma",false);renderer.typeface=prefs.getBoolean("displayMono",true)?UiFonts.MONO:UiFonts.REGULAR;renderer.fractionMultiply=prefs.getBoolean("fractionMultiply",true);renderer.showCursor=false;renderer.color=color;renderer.fractionScale=1;renderer.stretchRoots=true;renderer.strokeRatio=.045f;
  setContentDescription((result?"结果：":"公式：")+source);
 }
 @Override protected void onMeasure(int w,int h){Formula.Box b=renderer.measure(row,font);setMeasuredDimension(Math.max(MeasureSpec.getMode(w)==MeasureSpec.EXACTLY?MeasureSpec.getSize(w):0,Math.round(b.w+2*pad)),Math.round(b.a+b.d+2*pad+(right?4:1)*getResources().getDisplayMetrics().density));}
 @Override protected void onDraw(Canvas c){renderer.spots.clear();Formula.Box b=renderer.measure(row,font);renderer.draw(c,row,right?getWidth()-pad-b.w:pad,pad+b.a,font);}
}
