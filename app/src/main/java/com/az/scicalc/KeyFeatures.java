package com.az.scicalc;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Main-key workflows operate on the visible formula, including stored variables. */
final class KeyFeatures {
 final MainActivity app;
 private String memoryState="";
 private String calcSource="",calcTree="";
 private final LinkedHashMap<String,String> substitutions=new LinkedHashMap<>();
 private final ArrayList<String> calcVariables=new ArrayList<>();
 private int calcIndex;
 KeyFeatures(MainActivity app){this.app=app;}
 void cancelEntry(){memoryState="";calcSource="";calcVariables.clear();substitutions.clear();app.view.memoryState="";app.view.prompt="";}
 boolean handle(String action){
  if(!memoryState.isEmpty()&&action.startsWith("var:")){String key=action.substring(4),state=memoryState;memoryState="";app.view.memoryState="";
   if(state.equals("RCL")){app.checkpoint();app.formula.variable(key);app.changed();}
   else app.computeCurrent(value->{app.variables.put(key,value);app.save();app.toast(key+" = "+value);});return true;}
  if(action.equals("clear")&&(!memoryState.isEmpty()||!calcSource.isEmpty())){memoryState="";app.view.memoryState="";if(!calcSource.isEmpty()){app.formula.restore(calcTree);calcSource="";app.view.prompt="";app.changed();}else app.view.invalidate();return true;}
  if(action.equals("evaluate")&&!calcSource.isEmpty()){submitSubstitution();return true;}
  if(action.startsWith("var:")){edit(()->app.formula.variable(action.substring(4)));return true;}
  if(action.startsWith("symbol:")){edit(()->app.formula.symbol(action.substring(7)));return true;}
  if(action.startsWith("token:")){edit(()->app.formula.token(action.substring(6)));return true;}
  if(action.startsWith("call:")){String[] parts=action.substring(5).split(":");edit(()->app.formula.call(parts[0],Integer.parseInt(parts[1])));return true;}
  if(action.startsWith("binary:")){edit(()->{answerOnEmpty();app.formula.template("binary",action.substring(7));});return true;}
  if(action.startsWith("template:")){String[] p=action.substring(9).split(":",2);edit(()->app.formula.template(p[0],p.length>1?p[1]:""));return true;}
  if(action.startsWith("category:")){catalog(action.substring(9));return true;}
  if(action.startsWith("vectorGrid:")){edit(()->app.formula.grid(1,Integer.parseInt(action.substring(11)),true));return true;}
  if(action.equals("inputcontext")||action.startsWith("inputcontext:")){Formula f=action.contains(":")?app.view.tape.get(Integer.parseInt(action.substring(13))).formula:app.formula;ExpressionContext.open(app,f,true);return true;}
  if(action.equals("outputcontext")||action.startsWith("outputcontext:")){String value=action.contains(":")?app.view.tape.get(Integer.parseInt(action.substring(14))).display:app.view.result;if(!value.isEmpty()){Formula f=new Formula();f.root=Formula.result(value);ExpressionContext.open(app,f,false);}return true;}
  if(action.startsWith("resultmenu:")){resultMenu(Integer.parseInt(action.substring(11)));return true;}
  if(action.startsWith("tapeedit:")){int i=Integer.parseInt(action.substring(9));app.checkpoint();app.formula.restore(app.view.tape.get(i).tree);app.changed();return true;}
  switch(action){
   case "none":return true;
   case "second":app.view.second=!app.view.second;app.view.invalidate();return true;
   case "searchFunctions":app.workbench.catalog("");return true;
   case "font":fontMenu();return true;
   case "angle":DisplayMenus.angle(app);return true;
   case "engineering":numberFormat();return true;
   case "resultmenu":resultMenu(-1);return true;
   case "calc":beginSubstitution();return true;
   case "solve":solveCurrent();return true;
   case "recall":case "store":memoryState=action.equals("recall")?"RCL":"STO";app.view.memoryState=memoryState;app.view.alpha=true;app.view.invalidate();return true;
   case "clearVars":new AlertDialog.Builder(app,R.style.ReferenceHelpTheme).setMessage("你想要清除所有的变量值吗？").setNegativeButton("确定",(d,w)->{app.variables.clear();app.save();}).setPositiveButton("取消",null).show();return true;
   case "previous":edit(()->app.formula.answer(app.previous));return true;
   case "answer":edit(()->app.formula.answer(app.ans));return true;
   case "logbase":edit(()->app.formula.call("LogBase",2));return true;
   case "mixed":edit(()->app.formula.template("mixed",""));return true;
   case "mixedFormat":if(app.formula.empty()&&!app.exact.isEmpty())app.mixedResult();else edit(()->app.formula.template("mixed",""));return true;
   case "nthroot":edit(()->app.formula.template("nthroot",""));return true;
   case "product":edit(()->app.formula.template("product",""));return true;
   case "recurring":edit(()->app.formula.template("recurring",""));return true;
   case "random":edit(()->app.formula.symbol("RandomReal()"));return true;
   case "randomInteger":edit(()->app.formula.call("RanInt#",2));return true;
   case "gcd":case "lcm":edit(()->app.formula.template("func",action.equals("gcd")?"GCD":"LCM"));return true;
   case "modulo":case "remainder":edit(()->{answerOnEmpty();app.formula.template("binary",action.equals("modulo")?"mod":"÷R");});return true;
   case "permutation":case "combination":edit(()->{answerOnEmpty();app.formula.template("binary",action.equals("permutation")?"nPr":"nCr");});return true;
   case "polar":case "rectangular":edit(()->app.formula.call(action.equals("polar")?"Pol":"Rec",2));if(!app.prefs.getBoolean("trigAngleChosen",false))DisplayMenus.angle(app);return true;
   case "polarSymbol":edit(()->{answerOnEmpty();app.formula.template("binary","∠");});return true;
   case "integralMenu":CalculusMenus.integral(app);return true;
   case "derivativeMenu":CalculusMenus.derivative(app);return true;
   case "limit":CalculusMenus.limit(app);return true;
   case "expPower":edit(()->{app.formula.symbol("E");app.formula.template("pow","");});return true;
   case "factorial":edit(()->{answerOnEmpty();app.formula.text("!");});return true;
   case "scientific":edit(()->{if(app.formula.empty())app.formula.text("1");app.formula.template("scientific","");});return true;
   case "percent":edit(()->{answerOnEmpty();app.formula.text("%");});if(!app.prefs.getBoolean("percentModeChosen",false))DisplayMenus.percent(app);return true;
   case "hyp":app.workbench.trigonometry(0);return true;
   case "complex":app.workbench.complexMenu();return true;
   case "dms":if(app.formula.nextDms()){app.changed();}else if(app.formula.empty()&&!app.exact.isEmpty())app.dmsResult();else edit(()->app.formula.template("dms",""));return true;
   case "factor":factor();return true;
   case "expand":case "simplify":app.transformCurrent(action.equals("expand")?"Expand":"FullSimplify");return true;
   case "factorAlgebra":app.transformCurrent("Factor");return true;
   case "graphCurrent":app.graphCurrent();return true;
   case "more":more();return true;
   case "matrixGrid":matrixSize();return true;
   case "functions":app.workbench.functions();return true;
   case "statsShortcut":if(app.view.mode.equals("STAT"))app.workbench.statistics();return true;
   case "vectorShortcut":if(app.view.mode.equals("向量")||app.view.mode.equals("VECTOR"))app.workbench.vectors();return true;
   case "matrix":app.workbench.matrices();return true;
   case "matrixShortcut":FunctionMenus.matrix(app);return true;
   case "vector":app.workbench.vectors();return true;
   case "stats":app.workbench.statistics();return true;
   case "table":app.workbench.table();return true;
   case "bases":app.workbench.bases();return true;
   case "distribution":app.workbench.distribution(0);return true;
   case "constants":app.workbench.constants();return true;
   case "copy":copyMenu();return true;
   case "paste":paste();return true;
   case "ocr":app.ocr.open();return true;
   case "help":app.menuTools.handle("manual");return true;
   case "formatStatus":numberFormat();return true;
   case "modeStatus":app.press("mode");return true;
  }
  return false;
 }
 static final String[] VARIABLES={"a","b","c","d","e","f","m","r","s","t","u","v","w","x","y","z"};
 private static final String[] TRIG={"Sinh","Cosh","Tanh","Coth","Sech","Csch","ArcSinh","ArcCosh","ArcTanh","ArcCoth","ArcSech","ArcCsch","Sin","Cos","Tan","Cot","Sec","Csc","ArcSin","ArcCos","ArcTan","ArcCot","ArcSec","ArcCsc"};
 void edit(Runnable change){app.checkpoint();change.run();app.changed();}
 private void answerOnEmpty(){if(app.formula.empty())app.formula.answer(app.ans);}
 private void fontMenu(){DisplayMenus.font(app,i->{if(i<3){float z=app.prefs.getFloat("displayScale",1);z=i==0?Math.max(.65f,z-.1f):i==1?Math.min(1.6f,z+.1f):1;app.prefs.edit().putFloat("displayScale",z).apply();app.applyPreferences();}else if(i==3)ReferenceDialogs.choose(app,"显示字体",new String[]{"标准","等宽"},n->{app.prefs.edit().putBoolean("displayMono",n==1).apply();app.applyPreferences();});else app.menuTools.handle(i==4?"skin":"sound");});}
 private void numberFormat(){FormatSheet.show(app);}
 private void beginSubstitution(){
  if(app.formula.empty()||!app.formula.complete()){CalculusMenus.usage(app,false);return;}
  calcVariables.clear();substitutions.clear();String source=app.formula.source(app.angleMode);for(String v:VARIABLES){String symbol=v.equals("e")?"scvE":v;if(java.util.regex.Pattern.compile("\\b"+symbol+"\\b").matcher(source).find())calcVariables.add(v);}
  if(calcVariables.isEmpty()){CalculusMenus.usage(app,false);return;}
  calcSource=source;calcTree=app.formula.save();calcIndex=0;nextSubstitution();
 }
 private void nextSubstitution(){String v=calcVariables.get(calcIndex);app.formula.load(app.variables.getOrDefault(v,"0"));app.formula.cursor.defaultValue=true;app.view.prompt=v+"=?";app.changed();}
 private void submitSubstitution(){if(!app.formula.complete()||app.formula.empty()){app.error("请填入变量的值");return;}String current=app.vars(app.formula.source(app.angleMode));app.task(()->app.engine.evaluate(current,false),value->{substitutions.put(calcVariables.get(calcIndex),value);if(++calcIndex<calcVariables.size()){nextSubstitution();return;}String source=calcSource;for(Map.Entry<String,String> e:substitutions.entrySet())source=source.replaceAll("\\b"+(e.getKey().equals("e")?"scvE":e.getKey())+"\\b",java.util.regex.Matcher.quoteReplacement("("+e.getValue()+")"));app.formula.restore(calcTree);calcSource="";app.view.prompt="";app.runResult(source,app.decimal,true);});}
 private void solveCurrent(){
  if(app.formula.empty()||!app.formula.complete()){CalculusMenus.usage(app,true);return;}
  String source=app.formula.source(app.angleMode);ArrayList<String> unknown=new ArrayList<>();for(String v:VARIABLES){String symbol=v.equals("e")?"scvE":v;if(java.util.regex.Pattern.compile("\\b"+symbol+"\\b").matcher(source).find())unknown.add(symbol);}
  if(unknown.isEmpty()){CalculusMenus.usage(app,true);return;}
  unknown.removeIf(v->!v.equals("x")&&app.variables.containsKey(v.equals("scvE")?"e":v));if(unknown.isEmpty()){app.error("请保留至少一个未赋值的未知数，或使用 x");return;}
  LinkedHashMap<String,String> coefficients=new LinkedHashMap<>();for(Map.Entry<String,String> e:app.variables.entrySet()){String key=e.getKey().equals("e")?"scvE":e.getKey();if(!unknown.contains(key))coefficients.put(key,e.getValue());}source=ScopedValues.apply(source,coefficients);
  String equation=source.replaceAll("(?<![<>=!])=(?!=)","==");if(!equation.contains("==")&&!equation.matches(".*[<>].*"))equation="("+equation+")==0";
  String vars=unknown.size()==1?unknown.get(0):"{"+String.join(",",unknown)+"}";app.runResult("Solve("+equation+","+vars+")",app.decimal,true);
 }
 private void factor(){if(app.formula.empty()){new AlertDialog.Builder(app,R.style.ReferenceHelpTheme).setTitle("质因数分解").setMessage("请先输入要分解的数字。要打开手册了解质因数分解的原理吗？").setNegativeButton("确定",(d,w)->app.menuTools.handle("manual")).setPositiveButton("取消",null).show();return;}app.runFactored(app.vars(app.formula.source(app.angleMode)));}

 private void matrixSize(){MatrixSizeDialog.show(app,(rows,cols)->edit(()->app.formula.grid(rows,cols,false)));}
 private void catalog(String category){app.workbench.catalog(category);}
 private void resultMenu(int index){app.resultFormat(index);}
 private void more(){app.workbench.more();}
 private void copyMenu(){if(!app.formula.empty()||!app.view.tape.isEmpty())app.copyMenu();}
 private void paste(){ClipboardManager clipboard=(ClipboardManager)app.getSystemService(Context.CLIPBOARD_SERVICE);if(!clipboard.hasPrimaryClip())return;ClipData clip=clipboard.getPrimaryClip();if(clip.getDescription().hasMimeType("application/vnd.scicalc.formula+json")&&clip.getItemCount()>1&&clip.getItemAt(1).getIntent()!=null){String tree=clip.getItemAt(1).getIntent().getStringExtra("formulaTree");edit(()->app.formula.insertSaved(tree));}else if(clip.getItemAt(0).getUri()!=null)app.ocr.image(clip.getItemAt(0).getUri());else edit(()->app.formula.insertExpression(clip.getItemAt(0).coerceToText(app).toString()));}
}
