package com.az.scicalc;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Native, accessible page chrome matching the reference tool screens. */
class ToolPage extends LinearLayout {
 static final int BG=0xff20282d, TEXT=0xffd7d3de, MUTED=0xffa9a6b0, ACCENT=0xffb488ff;
 final LinearLayout header,body;final ScrollView scroll;final TextView title;
 Runnable onReturn;
 boolean embedded=false,closing=false,dismissOnOutside=false;float overlayTop=400,overlayBottom=1820,overlayWidth=0,overlayLeft=-1;
 @Override public boolean dispatchTouchEvent(MotionEvent event){return closing||super.dispatchTouchEvent(event);}
 ToolPage(Context c,String name,Runnable back){
  super(c);setOrientation(VERTICAL);setBackgroundColor(BG);setPadding(0,dp(30),0,dp(16));
  header=new LinearLayout(c);header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(8),0,dp(6),0);
  header.addView(icon(c,"back","返回",back,.44f),new LayoutParams(dp(48),dp(64)));
  title=text(c,name,24);header.addView(title,new LayoutParams(0,dp(64),1));addView(header,new LayoutParams(-1,dp(64)));
  scroll=new ScrollView(c);scroll.setFillViewport(true);body=new LinearLayout(c);body.setOrientation(VERTICAL);body.setPadding(0,0,0,dp(18));scroll.addView(body);addView(scroll,new LayoutParams(-1,0,1));
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(!embedded){WindowInsets insets=getRootWindowInsets();if(insets!=null){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());setPadding(bars.left,bars.top,bars.right,bars.bottom);}}}
 int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 static TextView text(Context c,String s,float size){TextView t=new TextView(c);t.setText(s);t.setTextColor(TEXT);t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,size);t.setTypeface(UiFonts.REGULAR);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
 static GradientDrawable rounded(Context c,int color,float radius){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(radius*c.getResources().getDisplayMetrics().density);b.setStroke(1,0xff53545e);return b;}
 static View icon(Context c,String name,String description,Runnable action){return icon(c,name,description,action,.58f);}
 static View icon(Context c,String name,String description,Runnable action,float scale){View v=new View(c){final Paint p=new Paint(3);protected void onDraw(Canvas canvas){UiIcons.draw(canvas,p,name,getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())*scale,TEXT);}};v.setContentDescription(description);v.setFocusable(true);v.setOnClickListener(w->action.run());return v;}
 void action(String icon,String description,Runnable runnable){header.addView(icon(getContext(),icon,description,runnable,.46f),new LayoutParams(dp(44),dp(64)));}
 TextView note(String s){TextView t=text(getContext(),s,15);t.setTextColor(MUTED);t.setPadding(0,dp(8),0,dp(14));body.addView(t);return t;}
 TextView section(String s){TextView t=text(getContext(),s,21);t.setTextColor(ACCENT);t.setPadding(dp(embedded?8:16),dp(embedded?6:18),0,dp(embedded?6:12));t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,embedded?16:21);body.addView(t);return t;}
 LinearLayout card(){LinearLayout l=new LinearLayout(getContext());l.setOrientation(VERTICAL);l.setPadding(dp(12),dp(10),dp(12),dp(10));l.setBackground(rounded(getContext(),0xff293137,14));LayoutParams lp=new LayoutParams(-1,-2);lp.bottomMargin=dp(12);body.addView(l,lp);return l;}
 TextView row(String heading,String subtitle,Runnable action){return listRow("",heading,subtitle,"",action,null);}
 TextView listRow(String number,String heading,String subtitle,String trailing,Runnable action,Runnable detail){
  LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(embedded||subtitle.isEmpty()?58:60));row.setPadding(dp(embedded?4:16),dp(embedded?5:8),dp(embedded?14:16),dp(embedded?5:8));
  if(!number.isEmpty()){TextView index=text(getContext(),number,14);row.addView(index,new LayoutParams(dp(18),-1));}
  LinearLayout words=new LinearLayout(getContext());words.setOrientation(VERTICAL);TextView t=text(getContext(),heading,embedded?16:18);words.addView(t);if(!subtitle.isEmpty()){TextView sub=text(getContext(),subtitle,16);sub.setTextColor(TEXT);if(embedded){sub.setMaxLines(3);sub.setEllipsize(android.text.TextUtils.TruncateAt.END);}sub.setPadding(0,dp(4),0,0);words.addView(sub);}row.addView(words,new LayoutParams(0,-2,1));
  if(!trailing.isEmpty())row.addView(icon(getContext(),trailing,"详情："+heading,detail==null?action:detail,.65f),new LayoutParams(dp(36),dp(40)));
  row.setContentDescription(heading);row.setFocusable(true);row.setOnClickListener(v->action.run());body.addView(row,new LayoutParams(-1,-2));divider();return t;
 }
 void divider(){View line=new View(getContext());line.setBackgroundColor(0xff3b4248);body.addView(line,new LayoutParams(-1,dp(.6f)));}
 void inset(float padding){body.setPadding(dp(padding),0,dp(padding),dp(18));}
 void embedded(float top,float bottom){embedded=true;overlayTop=top;overlayBottom=bottom;setPadding(dp(2),0,dp(2),dp(2));header.setVisibility(GONE);body.setPadding(0,0,0,0);setBackground(texture(getContext()));setClickable(true);}
 void frame(){GradientDrawable border=new GradientDrawable();border.setColor(Color.TRANSPARENT);border.setStroke(2,0xff686c71);setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{texture(getContext()),border}));}
 static android.graphics.drawable.Drawable texture(Context c){Bitmap tile=Bitmap.createBitmap(12,12,Bitmap.Config.ARGB_8888);tile.setDensity(Bitmap.DENSITY_NONE);Canvas canvas=new Canvas(tile);canvas.drawColor(0xff161c20);Paint dot=new Paint(3);dot.setColor(0xff050707);canvas.drawOval(1,1,4,3,dot);canvas.drawOval(7,7,10,9,dot);dot.setColor(0xff25292b);canvas.drawOval(1,3,4,4,dot);canvas.drawOval(7,9,10,10,dot);android.graphics.drawable.BitmapDrawable d=new android.graphics.drawable.BitmapDrawable(c.getResources(),tile);d.setTileModeXY(Shader.TileMode.REPEAT,Shader.TileMode.REPEAT);return d;}
 TextView button(LinearLayout parent,String label,Runnable action){TextView t=text(getContext(),label,17);t.setGravity(Gravity.CENTER);t.setTextColor(Color.BLACK);t.setBackground(rounded(getContext(),ACCENT,18));t.setMinHeight(dp(44));t.setPadding(dp(12),dp(6),dp(12),dp(6));LayoutParams lp=new LayoutParams(-1,-2);lp.topMargin=dp(8);lp.bottomMargin=dp(8);parent.addView(t,lp);t.setOnClickListener(v->action.run());return t;}
 EditText input(LinearLayout parent,String hint,String value,boolean multiline){EditText e=new EditText(getContext());e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,19);e.setTypeface(UiFonts.REGULAR);e.setHint(hint);e.setText(value);e.setSingleLine(!multiline);e.setSelectAllOnFocus(true);e.setPadding(dp(12),dp(12),dp(12),dp(12));e.setBackground(rounded(getContext(),BG,12));e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS|(multiline?android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));LayoutParams lp=new LayoutParams(-1,-2);lp.topMargin=dp(6);lp.bottomMargin=dp(8);parent.addView(e,lp);return e;}
 EditText field(LinearLayout parent,String label,String hint,String value,boolean multiline){
  TextView caption=text(getContext(),label,16);if(label.startsWith("★")){android.text.SpannableString required=new android.text.SpannableString(label);required.setSpan(new android.text.style.ForegroundColorSpan(0xffec407a),0,1,0);caption.setText(required);}caption.setPadding(0,dp(parent.getChildCount()==0?8:6),0,dp(4));parent.addView(caption);
  EditText e=new EditText(getContext());e.setTypeface(multiline?UiFonts.MONO:UiFonts.REGULAR);e.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,16);e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setHint(hint);e.setText(value);e.setSingleLine(!multiline);e.setSelectAllOnFocus(true);e.setIncludeFontPadding(false);e.setPadding(dp(4),dp(10),dp(4),dp(8));e.setMinHeight(dp(40));e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS|(multiline?android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));e.setBackgroundTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[]{}},new int[]{ACCENT,MUTED}));parent.addView(e,new LayoutParams(-1,-2));return e;
 }
 void tabs(String[] names,int selected,java.util.function.IntConsumer action){
  HorizontalScrollView sc=new HorizontalScrollView(getContext());sc.setHorizontalScrollBarEnabled(false);sc.setFillViewport(true);
  LinearLayout strip=new LinearLayout(getContext());strip.setGravity(Gravity.CENTER);
  for(int i=0;i<names.length;i++){final int n=i;LinearLayout cell=new LinearLayout(getContext());cell.setOrientation(VERTICAL);
   TextView t=text(getContext(),names[i],embedded?16:19);t.setGravity(Gravity.CENTER);t.setSingleLine(true);t.setTextColor(i==selected?ACCENT:MUTED);t.setPadding(dp(12),0,dp(12),0);
   cell.addView(t,new LayoutParams(-1,dp(44)));View line=new View(getContext());line.setBackgroundColor(i==selected?ACCENT:Color.TRANSPARENT);cell.addView(line,new LayoutParams(-1,dp(2)));
   cell.setOnClickListener(v->action.accept(n));LayoutParams lp=!embedded&&names.length<=3?new LayoutParams(0,-2,1):new LayoutParams(dp(embedded&&names.length==3?82:embedded&&names.length==2?92:Math.max(72,names[i].length()*15)),-2);strip.addView(cell,lp);
  }
  sc.addView(strip,new HorizontalScrollView.LayoutParams(-1,-2));addView(sc,1,new LayoutParams(-1,dp(46)));
 }
}
