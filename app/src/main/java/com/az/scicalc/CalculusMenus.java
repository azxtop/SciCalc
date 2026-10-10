package com.az.scicalc;

import android.graphics.*;
import android.view.*;
import android.widget.*;

/** Calculus selectors replace the lower keypad with numbered formula previews. */
final class CalculusMenus {
 static void integral(MainActivity app){
  ToolPage p=FunctionMenus.page(app,"积分",1045,2358);p.frame();p.dismissOnOutside=true;
  row(app,p,"1",sample("integral"),"计算 f(x) 关于 x 变量从 a 到 b 的积分",()->insert(app,"integral",""),"积分与原函数");
  row(app,p,"2",sample("indefinite"),"计算 f(x) 关于变量 x 的原函数。",()->insert(app,"indefinite",""),"积分与原函数");
  TextView warning=ToolPage.text(app,"⚠ 快速数值积分方法。在某些特殊情况下，结果可能不正确。",16);warning.setTextColor(ToolPage.ACCENT);warning.setPadding(p.dp(8),p.dp(5),p.dp(8),p.dp(5));p.body.addView(warning);p.divider();
  String[] names={"LegendreGauss Integral","Romberg Integral","Gauss-Kronrod Integral"},subtitles={"使用勒让德高斯方法计算积分","使用 Romberg 方法计算积分","使用高斯-Kronrod 方法计算积分"},methods={"LegendreGauss","Romberg","GaussKronrod"};
  for(int i=0;i<3;i++){final String method=methods[i];TextView title=p.listRow(""+(i+4),names[i],subtitles[i],"info",()->insert(app,"integral",method),()->help(app,"积分与原函数"));title.setTypeface(UiFonts.MONO);}
  app.menuTools.popup(p);
 }
 static void derivative(MainActivity app){
  ToolPage p=FunctionMenus.page(app,"导数",1045,2358);p.frame();p.dismissOnOutside=true;
  row(app,p,"1",sample("derivativeAt"),"计算 f(x) 在 x=a 处的导数",()->insert(app,"derivativeAt",""),"导数");
  row(app,p,"2",sample("derivative"),"计算 f(x) 的导数",()->insert(app,"derivative",""),"导数");
  app.menuTools.popup(p);
 }
 static void limit(MainActivity app){
  ToolPage p=FunctionMenus.page(app,"极限",400,1634);p.frame();String[] names={"Lim(expr, x→a)","Lim(expr, x→a⁺)","Lim(expr, x→a⁻)","∞"},subtitles={"当 x 趋近于 a 时，计算 expr 的极限","当 x 的值逐渐减小并接近 a 时，计算极限","当 x 的值逐渐接近 a 时，计算极限","Infinity symbol"};
  for(int i=0;i<4;i++){final int n=i;TextView title=p.listRow((i+1)+" ",names[i],subtitles[i],"info",()->{if(n==3)app.workbench.insert(()->app.formula.symbol("Infinity"));else insert(app,"limit",n==1?"-1":n==2?"1":"");},()->help(app,"极限"));title.setTypeface(UiFonts.MONO);}
  app.menuTools.show(p);
 }
 static void insert(MainActivity app,String kind,String method){app.workbench.insert(()->app.formula.template(kind,method));}
 static String sample(String kind){Formula f=new Formula();f.template(kind,"");Formula.Node n=f.root.nodes.get(0);String[] fields=kind.equals("integral")?new String[]{"f(x)","a","b","x"}:kind.equals("indefinite")?new String[]{"f(x)","x"}:kind.equals("derivativeAt")?new String[]{"f(x)","x","a"}:new String[]{"f(x)","x"};for(int i=0;i<n.rows.size();i++){n.rows.get(i).nodes.clear();Formula.addText(n.rows.get(i),fields[i]);}return f.save();}
 static void row(MainActivity app,ToolPage p,String number,String tree,String subtitle,Runnable action,String help){
  LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(p.dp(2),p.dp(5),p.dp(14),p.dp(5));
  row.addView(ToolPage.text(app,number+" ",14),new LinearLayout.LayoutParams(p.dp(20),-1));LinearLayout words=new LinearLayout(app);words.setOrientation(1);
  FormulaPreview preview=new FormulaPreview(app,tree,"",false,ToolPage.TEXT,16,3);preview.monospace();words.addView(preview);TextView description=ToolPage.text(app,subtitle,16);description.setPadding(0,p.dp(3),0,0);words.addView(description);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));
  row.addView(ToolPage.icon(app,"info","详情："+subtitle,()->help(app,help),.65f),new LinearLayout.LayoutParams(p.dp(36),p.dp(40)));row.setContentDescription(subtitle);row.setOnClickListener(v->action.run());p.body.addView(row,new LinearLayout.LayoutParams(-1,-2));p.divider();
 }
 static void usage(MainActivity app,boolean solve){
  String title=solve?"使用 SOLVE 求解方程":"使用 CALC",message=solve?"请先输入一个方程，再使用 SOLVE。要打开手册了解 SOLVE 的使用方法吗？":"没有可供 CALC 代入的数值。要打开手册了解 CALC 的使用方法吗？";
  android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(app,R.style.ReferenceHelpTheme).setTitle(title).setMessage(message).setNegativeButton("确定",(d,w)->help(app,solve?"SOLVE":"CALC")).setPositiveButton("取消",null).create();dialog.show();
 }
 static void help(MainActivity app,String title){
  boolean calculusPopup=title.equals("积分与原函数")||title.equals("导数");
  ToolPage p=new ToolPage(app,title,app.menuTools::back);
  if(calculusPopup){p.embedded(1045,2358);p.frame();p.dismissOnOutside=true;p.inset(10);}else p.inset(16);
  p.section(title);
  if(calculusPopup)p.row(title.equals("导数")?"打开导数菜单":"打开积分菜单","",()->{if(title.equals("导数"))derivative(app);else integral(app);});
  if(title.equals("CALC")){p.note("在主屏输入带变量的完整表达式，按 CALC。依次输入每个变量的数值，按 = 确认；所有变量确认后得到代入结果。");p.row("示例：x² + 2x","输入表达式后按 CALC，填写 x=3，再按 =，结果为 15。",()->app.workbench.insert(()->app.formula.insertExpression("x^2+2*x")));}
  else if(title.equals("SOLVE")){p.note("输入含未知数的方程，按 SHIFT → CALC（SOLVE）求解。没有等号的表达式按等于零处理。");p.row("示例：x² − 2 = 0","可返回实数或符号解。",()->app.workbench.insert(()->app.formula.insertExpression("x^2-2=0")));}
  else if(title.equals("极限")){p.note("依次填写表达式、变量与趋近值。a⁺ 从大于 a 的一侧趋近，a⁻ 从小于 a 的一侧趋近；双侧极限要求两侧一致。∞ 可插入无穷大符号。");}
  else{p.section("角度单位");p.note("含三角函数时使用当前 DEG、RAD 或 GRA 设置。");p.section(title.equals("导数")?"导数":"积分");p.note(title.equals("导数")?"第一项计算指定位置的导数；第二项返回导函数。填写函数、变量和求值位置后按 =。":"定积分需要函数、下界、上界和积分变量；原函数返回符号表达式。");if(!title.equals("导数")){p.section("快速数值方法");p.note("可选 LegendreGauss、Romberg 或 Gauss-Kronrod。间断点、奇点及振荡函数可能使数值积分不收敛，请检查区间与结果。");}}
  if(calculusPopup)app.menuTools.popup(p);else app.menuTools.show(p);
 }
}
