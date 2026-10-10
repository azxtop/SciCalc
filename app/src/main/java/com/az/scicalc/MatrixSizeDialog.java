package com.az.scicalc;

import android.app.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import java.util.function.BiConsumer;

/** Size picker uses the reference's seven by seven keypad and live selection. */
final class MatrixSizeDialog {
 static void show(Activity app,BiConsumer<Integer,Integer> chosen){
  Dialog dialog=new Dialog(app);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  View grid=new View(app){
   final Paint p=new Paint(3);int rows=3,cols=3;float scale=1;int pointer=-1,target=-1;float downX,downY;boolean cancelled;final int slop=ViewConfiguration.get(app).getScaledTouchSlop();final android.graphics.drawable.Drawable background=ToolPage.texture(app);
   final RectF[][] cells=new RectF[7][7];
   void text(Canvas c,String text,float x,float y,float size,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(UiFonts.REGULAR);p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(text,x,y,p);}
   protected void onDraw(Canvas c){scale=getWidth()/872f;c.save();c.scale(scale,scale);background.setBounds(0,0,872,1190);background.draw(c);
    text(c,"点按单元格以选择矩阵大小",436,112,44,ToolPage.TEXT);
    float x=54;for(int col=0;col<7;col++){float w=col<3?118:64;float y=168;for(int row=0;row<7;row++){float h=row<3?126:68;RectF r=new RectF(x,y,x+w,y+h);cells[row][col]=r;p.setColor(row<rows&&col<cols?0xff302d31:0xff504d53);p.setStyle(Paint.Style.FILL);c.drawRoundRect(r,22,22,p);p.setColor(0xff6a646e);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawRoundRect(r,22,22,p);if(row==0||col==0)text(c,""+(row==0?col+1:row+1),r.centerX(),r.centerY()+20,58,row<rows&&col<cols?0xff77747b:0xfff3f4f3);y+=h+16;}x+=w+24;}
    text(c,rows+" × "+cols,436,1010,44,ToolPage.TEXT);p.setColor(ToolPage.ACCENT);p.setStyle(Paint.Style.FILL);c.drawRoundRect(52,1056,820,1152,5,5,p);text(c,"选择",436,1120,44,Color.BLACK);c.restore();
   }
   int hit(float x,float y){if(x>=52&&x<=820&&y>=1056&&y<=1152)return 49;for(int r=0;r<7;r++)for(int col=0;col<7;col++)if(cells[r][col]!=null&&cells[r][col].contains(x,y))return r*7+col;return -1;}
   public boolean onTouchEvent(MotionEvent e){int action=e.getActionMasked(),i=e.getActionIndex();if(action==MotionEvent.ACTION_DOWN){pointer=e.getPointerId(0);downX=e.getX();downY=e.getY();target=hit(downX/scale,downY/scale);cancelled=false;return true;}if(action==MotionEvent.ACTION_CANCEL){pointer=-1;cancelled=true;return true;}if(action==MotionEvent.ACTION_POINTER_DOWN){cancelled=true;return true;}int index=e.findPointerIndex(pointer);if(index<0)return true;if(action==MotionEvent.ACTION_MOVE&&Math.max(Math.abs(e.getX(index)-downX),Math.abs(e.getY(index)-downY))>slop)cancelled=true;if((action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP)&&e.getPointerId(i)==pointer){pointer=-1;if(!cancelled&&target>=0&&hit(e.getX(i)/scale,e.getY(i)/scale)==target){if(target==49){dialog.dismiss();chosen.accept(rows,cols);}else{rows=target/7+1;cols=target%7+1;setContentDescription(rows+" × "+cols);invalidate();}performClick();}}return true;}

   public boolean performClick(){super.performClick();return true;}
  };
  grid.setContentDescription("矩阵大小，点选行和列后选择");dialog.setContentView(grid);dialog.show();Window w=dialog.getWindow();w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));int width=Math.round(app.getResources().getDisplayMetrics().widthPixels*.81f);w.setLayout(width,Math.round(width*1190/872f));w.setDimAmount(.35f);
 }
}
