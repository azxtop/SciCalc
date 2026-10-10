package com.az.scicalc;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Anchored result palette, including live previews and independent tape rows. */
final class ResultFormats {
 static void open(MainActivity app,int index){
  if(index>=app.view.tape.size())return;
  int navigation=app.menuTools.navigationToken();
  if(index>=0){load(app,app.view.tape.get(index).exact,index,navigation);return;}
  if(!app.exact.isEmpty()){load(app,app.exact,index,navigation);return;}
  if(app.formula.empty()||!app.formula.complete()||app.view.result.isEmpty())return;
  String source=app.vars(app.formula.calculationSource(app.angleMode,app.prefs.getBoolean("implicitPriority",true)));
  app.task(()->app.engine.evaluate(source,false),value->load(app,value,index,navigation));
 }
 private static void load(MainActivity app,String exact,int index,int navigation){
  if(!app.menuTools.acceptsNavigation(navigation))return;
  float anchor=app.view.resultMenuTop;int digits=app.prefs.getInt("digits",11);
  app.task(()->new org.json.JSONObject(ResultForms.values(app.engine,exact,digits)).toString(),json->{
   if(!app.menuTools.acceptsNavigation(navigation))return;
   try{org.json.JSONObject parsed=new org.json.JSONObject(json);LinkedHashMap<String,String> formats=new LinkedHashMap<>();for(String label:new String[]{"小数","循环小数","分数","带分数","质因数分解"})if(parsed.has(label))formats.put(label,parsed.getString(label));show(app,index,exact,formats,anchor,false);}catch(Exception e){app.error("无法显示结果格式");}
  });
 }
 private static void show(MainActivity app,int index,String exact,LinkedHashMap<String,String> formats,float anchor,boolean wide){
  ToolPage p=new ToolPage(app,"结果格式",app.menuTools::back);p.embedded(Math.max(254,Math.min(anchor,1050)),2358);p.frame();p.overlayWidth=wide?1060:966;p.overlayLeft=18;p.dismissOnOutside=true;
  p.header.setVisibility(View.VISIBLE);p.header.removeAllViews();p.header.setPadding(p.dp(8),0,p.dp(3),0);p.title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,16);p.header.addView(p.title,new LinearLayout.LayoutParams(0,p.dp(40),1));
  button(app,p,"expand","展开结果格式",true,()->{Formula input=new Formula();input.restore(index>=0?app.view.tape.get(index).tree:app.formula.save());app.menuTools.home();app.workbench.moreResult(input,exact);});
  button(app,p,"resize","切换结果窗口宽度",false,()->show(app,index,exact,formats,anchor,!wide));
  button(app,p,"close","关闭结果格式",false,app.menuTools::back);p.header.getLayoutParams().height=p.dp(40);
  for(Map.Entry<String,String> entry:formats.entrySet()){
   String display=entry.getKey().equals("小数")?app.formatted(entry.getValue()):entry.getValue();
   TextView label=ToolPage.text(app,entry.getKey(),16);label.setPadding(p.dp(8),0,0,0);p.body.addView(label,new LinearLayout.LayoutParams(-1,p.dp(32)));
   HorizontalScrollView scroll=new HorizontalScrollView(app);scroll.setHorizontalScrollBarEnabled(false);scroll.setFillViewport(true);scroll.setBackground(ToolPage.rounded(app,0xffd4e4e3,8));
   FormulaPreview preview=new FormulaPreview(app,"",display,true,Color.BLACK,24,4);preview.alignLeft();scroll.addView(preview,new FrameLayout.LayoutParams(-1,-2));
   preview.setOnLongClickListener(v->{context(app,preview,display,exact);return true;});
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(p.dp(6),p.dp(4),p.dp(6),p.dp(5));p.body.addView(scroll,lp);p.divider();
  }
  p.scroll.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));p.measure(View.MeasureSpec.makeMeasureSpec(Math.round(app.view.getWidth()*p.overlayWidth/1080f),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.round(app.view.getHeight()*(2358-p.overlayTop)/2400f),View.MeasureSpec.AT_MOST));p.overlayBottom=Math.min(2358,p.overlayTop+p.getMeasuredHeight()*2400f/app.view.getHeight());
  app.menuTools.popup(p);
 }
 private static void context(MainActivity app,View anchor,String display,String exact){
  ToolPage metrics=new ToolPage(app,"",()->{});LinearLayout menu=new LinearLayout(app);menu.setOrientation(1);menu.setBackground(ToolPage.texture(app));PopupWindow popup=new PopupWindow(menu,Math.round(app.view.getWidth()*.72f),-2,true);popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(ToolPage.BG));popup.setOutsideTouchable(true);popup.setElevation(metrics.dp(4));
  String[] labels={"分享","复制为文本","以LaTeX格式复制","以MathML格式复制","以图像格式导出","在程序编辑器中打开"},icons={"share","copy","copy","copy","image","code"};
  for(int i=0;i<labels.length;i++){final int n=i;Runnable action=()->{popup.dismiss();Formula f=new Formula();f.root=Formula.result(display);String value=ResultForms.clipboard(display);if(n==0){android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT,value);app.startActivity(android.content.Intent.createChooser(intent,"分享"));}else if(n==4){try{android.net.Uri uri=app.formulaImage(f);android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_SEND).setType("image/jpeg").putExtra(android.content.Intent.EXTRA_STREAM,uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);app.startActivity(android.content.Intent.createChooser(intent,"以图像格式导出"));}catch(Exception e){app.toast("导出图片失败");}}else if(n==5){app.prefs.edit().putString("scriptDraft",exact).apply();app.menuTools.handle("programming");}else{if(n==2)value=MathExport.latex(f);if(n==3)value=MathExport.mathml(f);((android.content.ClipboardManager)app.getSystemService(android.content.Context.CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText(labels[n],value));app.toast("已复制");}};
   LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(ToolPage.icon(app,icons[i],labels[i],action,.52f),new LinearLayout.LayoutParams(metrics.dp(40),metrics.dp(48)));row.addView(ToolPage.text(app,labels[i],18),new LinearLayout.LayoutParams(0,-1,1));row.setOnClickListener(v->action.run());menu.addView(row,new LinearLayout.LayoutParams(-1,metrics.dp(48)));View line=new View(app);line.setBackgroundColor(0xff3b4248);menu.addView(line,new LinearLayout.LayoutParams(-1,1));
  }
  int[] point=new int[2];anchor.getLocationOnScreen(point);int top=Math.min(point[1]+anchor.getHeight(),app.view.getHeight()-metrics.dp(6*48)-60);popup.showAtLocation(app.view,Gravity.TOP|Gravity.LEFT,point[0],Math.max(80,top));
 }
 private static void button(MainActivity app,ToolPage p,String icon,String description,boolean accent,Runnable action){View b=new View(app){final Paint paint=new Paint(3);@Override protected void onDraw(Canvas c){UiIcons.draw(c,paint,icon,getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())*.5f,accent?ToolPage.ACCENT:ToolPage.TEXT);}};b.setContentDescription(description);b.setOnClickListener(v->action.run());android.graphics.drawable.GradientDrawable shape=ToolPage.rounded(app,0xff272b31,16);shape.setStroke(p.dp(accent?2:1),accent?ToolPage.ACCENT:0xff53545e);b.setBackground(shape);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(p.dp(38),p.dp(36));lp.leftMargin=p.dp(7);p.header.addView(b,lp);}
}
