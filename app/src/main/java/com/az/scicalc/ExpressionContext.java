package com.az.scicalc;

import android.content.*;
import android.view.*;
import android.widget.*;

/** The selected expression is captured before the popup can change focus. */
final class ExpressionContext {
 static void open(MainActivity app,Formula selected,boolean input){
  Formula formula=new Formula();formula.restore(selected.save());String source=formula.source(app.angleMode);
  app.view.cancelTouches();ToolPage metrics=new ToolPage(app,"",()->{});
  String[] labels=input?new String[]{"粘贴","分享","复制为文本","以LaTeX格式复制","以MathML格式复制","以图像格式导出","创建数学函数","在程序编辑器中打开"}:new String[]{"分享","复制为文本","以LaTeX格式复制","以MathML格式复制","以图像格式导出","在程序编辑器中打开"};
  String[] icons=input?new String[]{"paste","share","copy","copy","copy","image","plus","code"}:new String[]{"share","copy","copy","copy","image","code"};
  LinearLayout menu=new LinearLayout(app);menu.setOrientation(1);menu.setBackground(ToolPage.texture(app));
  PopupWindow popup=new PopupWindow(menu,Math.round(app.view.getWidth()*.72f),-2,true);popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(ToolPage.BG));popup.setOutsideTouchable(true);popup.setElevation(metrics.dp(4));
  for(int i=0;i<labels.length;i++){String label=labels[i];Runnable action=()->{popup.dismiss();
   if(label.equals("粘贴")){app.press("paste");return;}
   if(label.equals("创建数学函数")){app.workbench.editFunction("",source);return;}
   if(label.equals("在程序编辑器中打开")){app.prefs.edit().putString("scriptDraft",source).apply();app.menuTools.handle("programming");return;}
   if(label.equals("分享")){app.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,source),"分享"));return;}
   if(label.equals("以图像格式导出")){try{app.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM,app.formulaImage(formula)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),label));}catch(Exception e){app.toast("导出图片失败");}return;}
   String value=label.equals("以LaTeX格式复制")?MathExport.latex(formula):label.equals("以MathML格式复制")?MathExport.mathml(formula):source;
   ClipData clip=new ClipData("SciCalc",new String[]{"text/plain","application/vnd.scicalc.formula+json"},new ClipData.Item(value));
   if(label.equals("复制为文本"))clip.addItem(new ClipData.Item(new Intent().putExtra("formulaTree",formula.save())));
   else clip=ClipData.newPlainText(label,value);
   ((ClipboardManager)app.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(clip);app.toast("已复制");
  };
   LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(ToolPage.icon(app,icons[i],label,action,.52f),new LinearLayout.LayoutParams(metrics.dp(40),metrics.dp(48)));row.addView(ToolPage.text(app,label,18),new LinearLayout.LayoutParams(0,-1,1));row.setOnClickListener(v->action.run());
   android.util.TypedValue background=new android.util.TypedValue();app.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,background,true);row.setBackgroundResource(background.resourceId);
   menu.addView(row,new LinearLayout.LayoutParams(-1,metrics.dp(48)));View line=new View(app);line.setBackgroundColor(0xff3b4248);menu.addView(line,new LinearLayout.LayoutParams(-1,1));
  }
  int top=Math.round(app.view.contextMenuTop*app.view.getHeight()/2400f);top=Math.min(top,app.view.getHeight()-metrics.dp(labels.length*48)-60);
  popup.showAtLocation(app.view,Gravity.TOP|Gravity.LEFT,Math.round(18*app.view.getWidth()/1080f),Math.max(80,top));
 }
}
