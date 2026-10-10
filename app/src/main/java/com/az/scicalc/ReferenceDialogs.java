package com.az.scicalc;
import android.app.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;

/** Purple heading and dark list surface used throughout the tool selectors. */
public final class ReferenceDialogs {
 public static void radio(Activity app,String title,String[] labels,int selected,java.util.function.IntConsumer action){new AlertDialog.Builder(app,com.az.scicalc.R.style.ToolDialogTheme).setTitle(title).setSingleChoiceItems(labels,selected,(d,i)->{d.dismiss();action.accept(i);}).setNegativeButton("取消",null).show();}
 public static AlertDialog.Builder builder(android.content.Context context){return new Builder(context);}
 private static final class Builder extends AlertDialog.Builder {
  private CharSequence title="";
  Builder(android.content.Context c){super(c,com.az.scicalc.R.style.ToolDialogTheme);}
  @Override public AlertDialog.Builder setTitle(CharSequence t){title=t;return this;}
  @Override public AlertDialog create(){AlertDialog dialog=super.create();android.content.Context context=getContext();float density=context.getResources().getDisplayMetrics().density;LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);header.setBackgroundColor(0xffb488ff);TextView text=new TextView(context);text.setText(title);text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,22);text.setTextColor(Color.BLACK);text.setPadding((int)(12*density),0,0,0);header.addView(text,new LinearLayout.LayoutParams(0,(int)(50*density),1));View close=new View(context){Paint p=new Paint(3);@Override protected void onDraw(Canvas c){UiIcons.draw(c,p,"close",getWidth()/2f,getHeight()/2f,getHeight()*.56f,Color.BLACK);}};header.addView(close,new LinearLayout.LayoutParams((int)(48*density),(int)(50*density)));close.setOnClickListener(v->dialog.dismiss());dialog.setCustomTitle(header);return dialog;}
 }
 public static void choose(Activity activity,String title,String[] labels,java.util.function.IntConsumer action){
  Dialog dialog=new Dialog(activity);LinearLayout content=new LinearLayout(activity);content.setOrientation(1);content.setBackgroundColor(0xff20282d);float density=activity.getResources().getDisplayMetrics().density;
  LinearLayout header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);header.setBackgroundColor(0xffb488ff);TextView heading=new TextView(activity);heading.setText(title);heading.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,23);heading.setTextColor(Color.BLACK);heading.setPadding((int)(10*density),0,0,0);header.addView(heading,new LinearLayout.LayoutParams(0,(int)(50*density),1));View close=new View(activity){Paint p=new Paint(3);protected void onDraw(Canvas c){UiIcons.draw(c,p,"close",getWidth()/2f,getHeight()/2f,getHeight()*.57f,Color.BLACK);}};header.addView(close,new LinearLayout.LayoutParams((int)(50*density),(int)(50*density)));close.setOnClickListener(v->dialog.dismiss());content.addView(header);
  ScrollView scroll=new ScrollView(activity);LinearLayout list=new LinearLayout(activity);list.setOrientation(1);for(int i=0;i<labels.length;i++){final int n=i;TextView row=new TextView(activity);row.setText(labels[i]);row.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,20);row.setTextColor(0xffd7d3de);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding((int)(10*density),(int)(12*density),(int)(10*density),(int)(12*density));row.setMinHeight((int)(66*density));row.setBackgroundResource(android.R.drawable.list_selector_background);list.addView(row);View line=new View(activity);line.setBackgroundColor(0xff3b4248);list.addView(line,new LinearLayout.LayoutParams(-1,1));row.setOnClickListener(v->{dialog.dismiss();action.accept(n);});}scroll.addView(list);content.addView(scroll,new LinearLayout.LayoutParams(-1,-2));dialog.setContentView(content);dialog.show();Window w=dialog.getWindow();w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));int width=activity.getResources().getDisplayMetrics().widthPixels;w.setLayout((int)(width*.87),Math.min((int)(activity.getResources().getDisplayMetrics().heightPixels*.9),(int)((50+labels.length*67)*density)));w.setDimAmount(.35f);
 }
}
