package com.az.scicalc;

import android.app.*;
import android.content.*;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.text.*;
import org.json.*;
import java.util.*;
import java.util.function.*;

/** Complete menu destinations, with one navigation stack and local persistence. */
final class MenuTools {
 final MainActivity app;private final ArrayDeque<ToolPage> stack=new ArrayDeque<>();private ToolPage current;private ListView historyList;
 private final MathEngine scripts=new MathEngine();private int job=0,navigation=0;
 private java.util.concurrent.Future<?> activeWork;private ToolPage workingPage;private TextView cancelButton;
 private void cancelWork(){app.cancelFuture(activeWork);activeWork=null;if(cancelButton!=null)cancelButton.setVisibility(View.GONE);}

 int navigationToken(){return navigation;}
 boolean acceptsNavigation(int token){return !app.isDestroyed()&&token==navigation&&(app.tabs==null||app.tabs.foreground());}
 void invalidatePendingNavigation(){navigation++;}
 private static final String[] VARIABLE_NAMES={"a","b","c","d","e","f","m","r","s","t","u","v","x","y","z"};
 MenuTools(MainActivity a){app=a;}
 boolean isOpen(){return current!=null;}
 void resetForTabs(){cancelWork();job++;navigation++;stack.clear();current=null;hideKeyboard();}
 void settingsFormat(){settings(4);}
 boolean handle(String action){switch(action){
  case "history":history();break;case "variables":variables();break;case "skin":skin(app.prefs.getInt("skin",0));break;
  case "sound":sound();break;
  case "settings":settings(0);break;case "manual":manual("");break;case "onlineManual":onlineManual();break;
  case "feedback":feedback();break;case "share":share();break;case "features":features();break;
  case "programming":programming(0);break;
  case "checkUpdate":checkUpdate();break;
  default:return false;
 }return true;}
 private ToolPage page(String name){return new ToolPage(app,name,this::back);}
 void show(ToolPage p){if(app.tabs!=null&&!app.tabs.foreground())return;cancelWork();navigation++;hideKeyboard();if(current!=null)stack.addLast(current);current=p;app.showTool(p);}
 void replace(ToolPage p){if(app.tabs!=null&&!app.tabs.foreground())return;cancelWork();navigation++;hideKeyboard();current=p;app.replaceTool(p);}
 // Selectors and their help share one transient destination; back closes it.
 void popup(ToolPage p){if(app.tabs!=null&&!app.tabs.foreground())return;cancelWork();job++;navigation++;hideKeyboard();stack.clear();current=p;app.replaceTool(p);}
 void back(){cancelWork();job++;navigation++;hideKeyboard();if(stack.isEmpty()){current=null;app.save();app.calculator();}else{current=stack.removeLast();app.showTool(current,true);if(current.onReturn!=null)current.onReturn.run();}}
 void home(){cancelWork();if(app.tabs!=null&&!app.tabs.foreground()){app.save();return;}job++;navigation++;stack.clear();current=null;hideKeyboard();app.save();app.calculator();}
 private void hideKeyboard(){View v=app.getCurrentFocus();if(v!=null)((InputMethodManager)app.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);}
 private void copy(String text){((ClipboardManager)app.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("SciCalc",text));Toast.makeText(app,"已复制",Toast.LENGTH_SHORT).show();}
 private String clipboard(){ClipboardManager c=(ClipboardManager)app.getSystemService(Context.CLIPBOARD_SERVICE);return c.hasPrimaryClip()?c.getPrimaryClip().getItemAt(0).coerceToText(app).toString():"";}
 private void confirm(String title,Runnable action){ReferenceDialogs.builder(app).setTitle(title).setNegativeButton("取消",null).setPositiveButton("确定",(d,w)->action.run()).show();}
 private void watch(EditText e,Consumer<String> action){e.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int a){}public void onTextChanged(CharSequence s,int st,int before,int n){action.accept(s.toString());}public void afterTextChanged(Editable e){}});}
 private void draft(EditText e,String key){watch(e,s->app.prefs.edit().putString(key,s).apply());}
 void work(TextView output,MainActivity.Work task,Consumer<String> success){
  cancelWork();int token=++job;ToolPage owner=current;output.setTextColor(ToolPage.MUTED);output.setText("等待中…");
  if(workingPage!=owner){workingPage=owner;cancelButton=owner.button(owner.body,"取消计算",()->{cancelWork();job++;output.setText("已取消计算");});}else cancelButton.setVisibility(View.VISIBLE);
  activeWork=app.worker.submit(()->{
   app.main.post(()->{if(!app.isDestroyed()&&token==job&&current==owner)output.setText("计算中…");});
   try{String result=task.run();CalculationBudget.remainingNanos();app.main.post(()->{if(app.isDestroyed()||token!=job||current!=owner)return;activeWork=null;cancelButton.setVisibility(View.GONE);output.setTextColor(ToolPage.TEXT);output.setText(result);if(success!=null)success.accept(result);});}
   catch(Throwable e){app.main.post(()->{if(app.isDestroyed()||token!=job||current!=owner)return;activeWork=null;cancelButton.setVisibility(View.GONE);output.setTextColor(0xffeea2a2);output.setText("计算失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));});}
  });
 }

 private TextView output(ToolPage p){TextView t=ToolPage.text(app,"",19);t.setTextIsSelectable(true);t.setPadding(p.dp(12),p.dp(16),p.dp(12),p.dp(16));t.setMinHeight(p.dp(120));t.setBackground(ToolPage.rounded(app,ToolPage.BG,12));p.body.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;}
 private LinearLayout actions(ToolPage p){LinearLayout l=new LinearLayout(app);l.setOrientation(LinearLayout.HORIZONTAL);p.body.addView(l);return l;}
 private void smallButton(ToolPage p,LinearLayout row,String label,Runnable action){TextView t=ToolPage.text(app,label,15);t.setGravity(Gravity.CENTER);t.setTextColor(ToolPage.TEXT);t.setTypeface(UiFonts.MONO);t.setMinHeight(p.dp(40));t.setOnClickListener(v->action.run());row.addView(t,new LinearLayout.LayoutParams(0,-2,1));}
 private void use(String expression,String result){app.checkpoint();app.formula.load(expression);app.changed();if(result!=null){app.exact=result;app.view.result=result;}home();}

 void historyLoaded(){if(current!=null&&current.title.getText().toString().equals("计算历史"))replaceHistory(false);}
 private void history(){
  if(!app.historyReady||!app.historyError.isEmpty()){ToolPage pending=page("计算历史");pending.inset(16);pending.note(app.historyReady?app.historyError:"正在读取计算历史…");show(pending);return;}
  ToolPage p=page("计算历史");historyList=null;p.header.setPadding(p.dp(12),0,0,0);p.title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,23);boolean oldest=app.prefs.getBoolean("historyOldest",false);
  p.body.setPadding(p.dp(8),p.dp(9),p.dp(8),p.dp(18));if(app.history.isEmpty())p.scroll.setBackground(ToolPage.texture(app));
  p.action("sort","排列次序",()->ReferenceDialogs.radio(app,"排列次序",new String[]{"降序（新数据在上）","升序（旧数据在上）"},oldest?1:0,i->{app.prefs.edit().putBoolean("historyOldest",i==1).apply();replaceHistory(false);}));
  p.action("more","历史操作",()->ReferenceDialogs.choose(app,"计算历史",new String[]{"复制全部","导出计算历史","全部删除"},i->{if(i==0)copy(historyText());else if(i==1)exportHistory();else confirm("清空全部计算历史？",()->{app.clearHistory();app.save();replaceHistory();});}));
  /*TextView summary=ToolPage.text(app,"已保存 "+app.history.size()+" 条记录 · 上限 "+app.historyLimit()+" 条",16);
  summary.setTextColor(ToolPage.MUTED);summary.setPadding(p.dp(8),0,p.dp(8),0);p.addView(summary,1,new LinearLayout.LayoutParams(-1,p.dp(40)));
  View separator=new View(app);separator.setBackgroundColor(0xff53545e);p.addView(separator,2,new LinearLayout.LayoutParams(-1,1));*/
  if(app.history.isEmpty()){
   LinearLayout empty=new LinearLayout(app);empty.setOrientation(1);empty.setGravity(Gravity.CENTER);
   empty.addView(ToolPage.icon(app,"hourglass","没有历史记录",()->{}),new LinearLayout.LayoutParams(p.dp(74),p.dp(74)));
   TextView message=ToolPage.text(app,"没有历史记录",21);message.setGravity(Gravity.CENTER);empty.addView(message);

   p.body.addView(empty,new LinearLayout.LayoutParams(-1,-1));
  }
  List<JSONObject> records=new ArrayList<>(app.history);if(oldest)Collections.reverse(records);
  if(!records.isEmpty()){
   ListView list=new ListView(app);historyList=list;list.setContentDescription("历史记录列表");list.setDivider(null);list.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));list.setCacheColorHint(Color.TRANSPARENT);list.setItemsCanFocus(true);list.setPadding(p.dp(8),p.dp(9),p.dp(8),p.dp(18));list.setClipToPadding(false);
   list.setAdapter(new BaseAdapter(){public int getCount(){return records.size();}public JSONObject getItem(int position){return records.get(position);}public long getItemId(int position){return position;}public View getView(int position,View recycled,ViewGroup parent){return historyCard(p,getItem(position),recycled);}});
   p.removeView(p.scroll);p.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  }
  LinearLayout footer=new LinearLayout(app);footer.setPadding(p.dp(16),p.dp(6),p.dp(16),p.dp(6));
  TextView export=ToolPage.text(app,"↗    导出计算历史",18);export.setTextColor(ToolPage.ACCENT);export.setGravity(Gravity.CENTER);export.setBackground(ToolPage.rounded(app,ToolPage.BG,4));export.setOnClickListener(v->exportHistory());footer.addView(export,new LinearLayout.LayoutParams(-1,-1));p.addView(footer,new LinearLayout.LayoutParams(-1,p.dp(56)));
  show(p);
 }
 private View historyCard(ToolPage p,JSONObject j,View recycled){
  LinearLayout host=recycled instanceof LinearLayout?(LinearLayout)recycled:new LinearLayout(app);host.setOrientation(1);host.removeAllViews();host.setPadding(0,0,0,p.dp(17));
   LinearLayout c=new LinearLayout(app);c.setOrientation(1);c.setPadding(0,p.dp(1),0,0);
   android.graphics.drawable.GradientDrawable outline=ToolPage.rounded(app,ToolPage.BG,9);outline.setStroke(p.dp(1),0xff53545e);c.setBackground(outline);
   host.addView(c,new LinearLayout.LayoutParams(-1,-2));
   if(j.has("toolValues")){TextView input=ToolPage.text(app,historyInput(j),18);input.setPadding(p.dp(8),p.dp(10),p.dp(8),p.dp(10));input.setOnClickListener(v->editHistory(j));c.addView(input);}else addHistoryFormula(p,c,j.optString("tree"),j.optString("source"),false,()->editHistory(j));historyLine(p,c);
   addHistoryFormula(p,c,"",j.optString("displayResult",j.optString("result")),true,()->copy(j.optString("displayResult",j.optString("result"))));historyLine(p,c);
   LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(p.dp(8),0,p.dp(5),0);c.addView(row,new LinearLayout.LayoutParams(-1,p.dp(44)));
   long time=j.optLong("time",0);String date=time>0?new java.text.SimpleDateFormat("yyyy年M月d日",Locale.CHINA).format(new Date(time)):"日期未记录";
   TextView dateView=ToolPage.text(app,date,14);dateView.setSingleLine(true);row.addView(dateView,new LinearLayout.LayoutParams(0,-1,1));
   TextView mode=ToolPage.text(app,j.optString("mode","基本算术"),17);mode.setSingleLine(true);mode.setGravity(Gravity.CENTER);row.addView(mode,new LinearLayout.LayoutParams(p.dp(78),-1));
   row.addView(ToolPage.icon(app,"trash","删除记录",()->confirm("删除这条记录？",()->{app.removeHistory(j);app.save();replaceHistory();}),.46f),new LinearLayout.LayoutParams(p.dp(44),-1));
   row.addView(ToolPage.icon(app,"copy","复制记录",()->ReferenceDialogs.choose(app,"复制",new String[]{"公式","结果","公式与结果"},i->copy(i==0?historyInput(j):i==1?j.optString("displayResult",j.optString("result")):recordText(j))),.46f),new LinearLayout.LayoutParams(p.dp(44),-1));
   View edit=ToolPage.icon(app,"historyEdit","编辑或在新标签中打开",()->ReferenceDialogs.choose(app,"打开历史记录",new String[]{"在当前标签中编辑","在新标签中打开"},i->{if(i==1&&!app.tabs.create(false))return;editHistory(j);}),.46f);row.addView(edit,new LinearLayout.LayoutParams(p.dp(44),-1));
   if(!j.optString("tabName").isEmpty()){TextView origin=ToolPage.text(app,"标签："+j.optString("tabName"),12);origin.setPadding(p.dp(8),0,p.dp(8),p.dp(4));c.addView(origin);}
  return host;
 }
 private void addHistoryFormula(ToolPage p,LinearLayout c,String tree,String source,boolean result,Runnable action){
  HorizontalScrollView sc=new HorizontalScrollView(app);sc.setFillViewport(true);sc.setHorizontalScrollBarEnabled(false);
  FormulaPreview preview=new FormulaPreview(app,tree,source,result);preview.setOnClickListener(v->action.run());sc.addView(preview,new android.widget.FrameLayout.LayoutParams(-1,-2));c.addView(sc,new LinearLayout.LayoutParams(-1,-2));
 }
 private void historyLine(ToolPage p,LinearLayout c){View line=new View(app);line.setBackgroundColor(0xff373f44);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,1);lp.leftMargin=lp.rightMargin=p.dp(8);c.addView(line,lp);}
 private String historyInput(JSONObject j){JSONArray labels=j.optJSONArray("toolLabels"),values=j.optJSONArray("toolValues");if(labels==null||values==null)return j.optString("source");String[] l=new String[values.length()],v=new String[values.length()];for(int i=0;i<v.length;i++){l[i]=labels.optString(i);v[i]=values.optString(i);}return ToolInput.readable(j.optString("toolTitle"),l,v);}
 private String recordText(JSONObject j){return (j.optString("tabName").isEmpty()?"":"["+j.optString("tabName")+"] ")+historyInput(j)+" = "+j.optString("displayResult",j.optString("result"));}
 private String historyText(){StringBuilder text=new StringBuilder();List<JSONObject> records=new ArrayList<>(app.history);if(app.prefs.getBoolean("historyOldest",false))Collections.reverse(records);for(JSONObject j:records)text.append(recordText(j)).append('\n');return text.toString();}
 private void exportHistory(){
  if(app.history.isEmpty()){app.toast("没有可导出的记录");return;}
  List<JSONObject> records=new ArrayList<>(app.history);if(app.prefs.getBoolean("historyOldest",false))Collections.reverse(records);
  int token=navigationToken();app.toast("正在生成历史文件…");
  app.historyIO.execute(()->{java.io.File file=new java.io.File(app.getCacheDir(),"SciCalc-history-"+java.util.UUID.randomUUID()+".txt");
   try{
    try(java.io.BufferedWriter writer=java.nio.file.Files.newBufferedWriter(file.toPath(),java.nio.charset.StandardCharsets.UTF_8)){for(JSONObject record:records){writer.write(recordText(record));writer.newLine();}}
    app.main.post(()->{if(!acceptsNavigation(token))return;android.net.Uri uri=android.net.Uri.parse("content://com.az.scicalc.images/"+file.getName());Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT,"SciCalc 计算历史").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(ClipData.newRawUri("计算历史",uri));
     try{app.startActivity(Intent.createChooser(share,"导出计算历史"));}catch(ActivityNotFoundException e){app.toast("没有可接收 TXT 文件的应用");}
    });
    java.io.File[] old=app.getCacheDir().listFiles((dir,name)->name.startsWith("SciCalc-history-")&&name.endsWith(".txt"));if(old!=null)for(java.io.File f:old)if(System.currentTimeMillis()-f.lastModified()>86400000)f.delete();
   }catch(Exception e){file.delete();app.main.post(()->{if(acceptsNavigation(token))app.toast("导出失败："+e.getMessage());});}
  });
 }
 private void editHistory(JSONObject j){if(j.has("toolValues")){home();app.reopenTool(j);return;}app.checkpoint();if(!j.optString("tree").isEmpty())app.formula.restore(j.optString("tree"));else app.formula.load(j.optString("source"));app.changed();app.angleMode=j.optInt("angleMode",j.optBoolean("degrees",app.degrees)?0:1);app.degrees=app.angleMode==0;app.decimal=j.optBoolean("decimal",app.decimal);app.exact=j.optString("result");app.view.result=j.optString("displayResult",app.exact);home();}
 private void replaceHistory(){replaceHistory(true);}
 private void replaceHistory(boolean keepPosition){int position=historyList==null?0:historyList.getFirstVisiblePosition(),top=historyList==null||historyList.getChildCount()==0?0:historyList.getChildAt(0).getTop()-historyList.getPaddingTop();ToolPage old=current;history();stack.removeLastOccurrence(old);if(keepPosition&&historyList!=null)historyList.setSelectionFromTop(Math.min(position,Math.max(0,historyList.getCount()-1)),top);}
 private void variables(){ToolPage p=page("变量值");p.embedded(130,2310);p.overlayWidth=984;p.header.setVisibility(View.VISIBLE);p.header.setBackgroundColor(ToolPage.BG);p.header.removeViewAt(0);p.header.setPadding(p.dp(16),0,0,0);p.title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,24);p.onReturn=this::replaceVariables;p.action("trash","清除全部变量",()->confirm("清除全部变量？",()->{app.variables.clear();app.save();replaceVariables();}));p.action("close","关闭变量值",this::back);p.body.setPadding(p.dp(6),p.dp(6),p.dp(6),0);
  for(String name:VARIABLE_NAMES){LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.CENTER_VERTICAL);row.setBackground(ToolPage.rounded(app,0xffd4e4e3,10));TextView label=ToolPage.text(app,name+"="+app.variables.getOrDefault(name,""),24);label.setTextColor(Color.BLACK);label.setSingleLine(true);label.setPadding(p.dp(5),0,0,0);row.addView(label,new LinearLayout.LayoutParams(0,-1,1));View trash=new View(app){android.graphics.Paint paint=new android.graphics.Paint(3);protected void onDraw(android.graphics.Canvas c){UiIcons.draw(c,paint,"trashClear",getWidth()/2f,getHeight()/2f,p.dp(21),Color.BLACK);}};trash.setContentDescription("清除变量 "+name);row.addView(trash,new LinearLayout.LayoutParams(p.dp(42),-1));trash.setOnClickListener(v->{app.variables.remove(name);app.save();label.setText(name+"=");});row.setContentDescription(name+" 变量");row.setOnClickListener(v->{app.checkpoint();app.formula.symbol(name);app.changed();home();});row.setOnLongClickListener(v->{editVariable(name);return true;});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,p.dp(42));lp.bottomMargin=p.dp(13);p.body.addView(row,lp);}
  show(p);
 }

 private void replaceVariables(){ToolPage old=current;variables();stack.removeLastOccurrence(old);}
 private void editVariable(String name){ToolPage p=page("变量 "+name);EditText e=p.input(p.body,"数值或表达式",app.variables.getOrDefault(name,""),false);TextView out=output(p);p.button(p.body,"保存变量",()->{String source=e.getText().toString().trim();if(source.isEmpty()){e.setError("请填写表达式");return;}work(out,()->{String result=app.engine.evaluate(source,false);if(!app.engine.evaluate("NumericQ("+result+")",false).equals("True"))throw new IllegalArgumentException("请填写有限的实数或复数表达式");return result;},r->{app.variables.put(name,r);app.save();out.setText(name+" = "+r);});});p.button(p.body,"使用当前结果",()->{if(app.exact.isEmpty()){out.setText("主计算器还没有结果");return;}e.setText(app.exact);});show(p);}

 private void sound(){FeedbackMenus.show(app,0);}
 private void toggle(ToolPage p,String label,String description,String key,boolean fallback){LinearLayout card=p.card();Switch sw=new Switch(app);sw.setText(label);sw.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,19);sw.setTextColor(ToolPage.TEXT);sw.setChecked(app.prefs.getBoolean(key,fallback));card.addView(sw);TextView note=ToolPage.text(app,description,14);note.setTextColor(ToolPage.MUTED);card.addView(note);sw.setOnCheckedChangeListener((b,on)->{app.prefs.edit().putBoolean(key,on).apply();app.applyPreferences();});}
 private void settings(int tab){ToolPage p=page("设置");p.onReturn=()->replaceSettings(tab);p.action("more","设置操作",()->ReferenceDialogs.choose(app,"设置",new String[]{"恢复默认设置","关于与开源组件"},i->{if(i==1)features();else confirm("恢复默认设置？",()->{app.prefs.edit().putInt("skin",0).putBoolean("sound",false).putBoolean("vibration",true).putString("numberFormat","normal").putInt("digits",11).putFloat("displayScale",1).apply();app.setAngle(0);app.decimal=false;app.applyPreferences();replaceSettings(tab);});}));p.tabs(new String[]{"常规","应用","显示","键盘","格式","计算"},tab,i->{ToolPage old=current;settings(i);stack.removeLastOccurrence(old);});
  if(tab==0||tab==5){
   if(tab==5){settingCheck(p,"CAS 模式","计算器可执行因式分解、求解等代数运算，并在含变量时给出精确的化简结果。","casEnabled",true);settingCheck(p,"自动计算","完成输入时查看结果","autoPreview",true);settingCheck(p,"自动计算积分∫dx","","previewIntegral",false);}
   settingSegments(p,"角度单位","",new String[]{"DEGREE","RADIAN","GRADIAN"},app.angleMode,app::setAngle);
   settingSegments(p,"默认输出类型","",new String[]{"分数","小数"},app.decimal?1:0,i->{app.decimal=i==1;app.view.approximate=app.decimal;app.refreshResult();app.save();});
   p.divider();settingSegments(p,"省略乘号","设置在计算中隐式乘法的优先级，以及它是否比显式的乘法和除法具有更高的运算顺序。",new String[]{"1/2π = 1/2*π","1/2π = 1/(2*π)"},app.prefs.getBoolean("implicitPriority",true)?1:0,i->{app.prefs.edit().putBoolean("implicitPriority",i==1).apply();app.changed();});p.divider();
   p.listRow("","小数显示格式","Fixed,ENG,ENG SI和SCI模式；小数精度","right",()->app.press("engineering"),null);
   settingSegments(p,"百分比计算模式","选择在使用加号和减号运算时，百分比的计算方式。此设置不会影响乘法、除法等其他运算符；对于这些运算符，百分比始终将该项除以 100 来计算。",new String[]{"100 + 20% = 100.2","100 + 20% = 120"},app.prefs.getBoolean("relativePercent",true)?1:0,i->{app.prefs.edit().putBoolean("relativePercent",i==1).apply();app.changed();});
  }
  if(tab==1){
   settingLink(p,"palette","计算器皮肤","",()->skin(app.prefs.getInt("skin",0)));
   settingLink(p,"font","字体",app.view.displayMono?"monospace":"sans-serif",()->ReferenceDialogs.radio(app,"字体",new String[]{"sans-serif","monospace"},app.view.displayMono?1:0,i->{app.prefs.edit().putBoolean("displayMono",i==1).apply();app.applyPreferences();replaceSettings(tab);}));
   settingLink(p,"sound","声音与振动","为每个按键和每个结果选择声音",this::sound);p.divider();
   settingLink(p,"globe","语言","系统语言",()->app.startActivity(new Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS)));
   settingLink(p,"rotate","屏幕方向","",()->ReferenceDialogs.radio(app,"屏幕方向",new String[]{"跟随系统","竖屏","横屏"},app.prefs.getInt("orientation",0),i->{app.prefs.edit().putInt("orientation",i).apply();app.applyPreferences();}));
   settingCheck(p,"保持屏幕常亮","","keepScreenOn",false);settingCheck(p,"显示状态栏","","showStatusBar",true);
  }
  if(tab==2){
   settingCheck(p,"清空屏幕","退出前清空屏幕","clearOnExit",false);settingCheck(p,"光标闪烁","","blinkCursor",true);p.divider();
   LinearLayout group=settingGroup(p,"显示字体大小","");SeekBar bar=new SeekBar(app);bar.setMax(95);bar.setProgress(Math.round((app.view.displayScale-.65f)*100));group.addView(bar,new LinearLayout.LayoutParams(-1,p.dp(58)));TextView sample=ToolPage.text(app,"1 234.781 23+33",24*app.view.displayScale);sample.setGravity(Gravity.CENTER);group.addView(sample,new LinearLayout.LayoutParams(-1,p.dp(78)));bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int v,boolean user){if(user){float size=.65f+v/100f;app.prefs.edit().putFloat("displayScale",size).apply();app.applyPreferences();sample.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,24*size);}}});p.divider();
   settingSegments(p,"选择如何显示“Ans”变量。","Ans+3                12.123 4+3",new String[]{"0","1","2"},app.prefs.getInt("ansStyle",2),i->{app.prefs.edit().putInt("ansStyle",i).apply();app.applyPreferences();});p.divider();settingCheck(p,"在结果下方显示计算信息","将在结果下方显示更多计算信息，例如隐式乘法和百分比计算。","showCalcInfo",true);
  }
  if(tab==3){p.section("按键标签");settingSegments(p,"除号","",new String[]{"/","÷"},app.prefs.getBoolean("divisionSlash",false)?0:1,i->{app.prefs.edit().putBoolean("divisionSlash",i==0).apply();app.applyPreferences();});settingSegments(p,"乘法符号","",new String[]{"×","*"},app.prefs.getBoolean("multiplicationStar",false)?1:0,i->{app.prefs.edit().putBoolean("multiplicationStar",i==1).apply();app.applyPreferences();});settingSegments(p,"按钮标签样式","ˣ√y                         ▪√□",new String[]{"0","1"},app.prefs.getInt("keyLabelStyle",0),i->{app.prefs.edit().putInt("keyLabelStyle",i).apply();app.applyPreferences();});settingLink(p,"font","键盘文字大小","在大尺寸屏幕上调节字体大小",()->ReferenceDialogs.radio(app,"键盘文字大小",new String[]{"小","默认","大"},app.prefs.getInt("keyFontSize",1),i->{app.prefs.edit().putInt("keyFontSize",i).apply();app.applyPreferences();}));p.divider();p.section("键盘布局");settingSegments(p,"键位图","",new String[]{"完整键盘","基础键盘"},app.view.basic?1:0,i->{app.view.basic=i==1;app.save();app.view.invalidate();});p.divider();p.section("自动完成");settingCheck(p,"在分数前插入乘号","区分带分数","fractionMultiply",true);settingCheck(p,"自动补全右括号","输入左括号时自动补上右括号。","closeParen",false);}
  if(tab==4){settingCheck(p,"按指数降序显示多项式","显示 x³+2x²−x+6","descendingPowers",false);p.listRow("","小数显示格式","Fixed,ENG,ENG SI和SCI模式；小数精度","right",()->app.press("engineering"),null);p.divider();p.section("分隔符号");p.note("123 456 789.123 456 789");p.listRow("","小数分隔符",app.prefs.getBoolean("decimalComma",false)?"逗号 123,123":"小数点 123.123","",()->ReferenceDialogs.radio(app,"小数分隔符",new String[]{"小数点","逗号"},app.prefs.getBoolean("decimalComma",false)?1:0,i->{app.prefs.edit().putBoolean("decimalComma",i==1).apply();app.applyPreferences();replaceSettings(tab);}),null);settingCheck(p,"千位分隔符","空格 123 123 123","groupThousands",true);settingCheck(p,"千分位分隔符","空格 123 123 123","groupDecimals",true);settingSegments(p,"科学计数法","",new String[]{"1.12341×10³","1.12341E3"},app.prefs.getBoolean("scientificE",false)?1:0,i->{app.prefs.edit().putBoolean("scientificE",i==1).apply();app.refreshResult();});p.divider();p.section("计算历史");p.row("排列次序",app.prefs.getBoolean("historyOldest",false)?"升序（旧数据在上）":"降序（新数据在上）",()->ReferenceDialogs.radio(app,"排列次序",new String[]{"降序（新数据在上）","升序（旧数据在上）"},app.prefs.getBoolean("historyOldest",false)?1:0,i->{app.prefs.edit().putBoolean("historyOldest",i==1).apply();replaceSettings(tab);}));p.row("历史记录上限",app.historyLimit()+" 条",()->ReferenceDialogs.radio(app,"历史记录上限",new String[]{"1000","3000","10000"},app.historyLimit()==3000?1:app.historyLimit()==10000?2:0,i->{app.prefs.edit().putInt("historyLimit",new int[]{1000,3000,10000}[i]).apply();replaceSettings(tab);}));}
  show(p);
 }
 private LinearLayout settingGroup(ToolPage p,String title,String description){LinearLayout group=new LinearLayout(app);group.setOrientation(1);group.setPadding(p.dp(16),p.dp(8),p.dp(12),p.dp(14));TextView heading=ToolPage.text(app,title,18);group.addView(heading);if(!description.isEmpty()){TextView note=ToolPage.text(app,description,16);note.setTextColor(ToolPage.TEXT);group.addView(note);}p.body.addView(group);return group;}
 private void settingSegments(ToolPage p,String title,String description,String[] labels,int selected,IntConsumer callback){LinearLayout group=settingGroup(p,title,description);segments(p,group,labels,selected,callback);}
 private void settingLink(ToolPage p,String icon,String title,String description,Runnable callback){LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(p.dp(16),p.dp(12),p.dp(14),p.dp(12));row.addView(ToolPage.icon(app,icon,title,callback,.6f),new LinearLayout.LayoutParams(p.dp(54),p.dp(44)));LinearLayout words=new LinearLayout(app);words.setOrientation(1);TextView label=ToolPage.text(app,title,18);words.addView(label);if(!description.isEmpty())words.addView(ToolPage.text(app,description,16));row.addView(words,new LinearLayout.LayoutParams(0,-2,1));row.addView(ToolPage.icon(app,"right","打开"+title,callback,.35f),new LinearLayout.LayoutParams(p.dp(28),p.dp(40)));row.setOnClickListener(v->callback.run());p.body.addView(row,new LinearLayout.LayoutParams(-1,-2));}
 private void settingCheck(ToolPage p,String label,String description,String key,boolean fallback){LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(p.dp(16),p.dp(14),p.dp(24),p.dp(14));LinearLayout words=new LinearLayout(app);words.setOrientation(1);words.addView(ToolPage.text(app,label,18));if(!description.isEmpty())words.addView(ToolPage.text(app,description,16));row.addView(words,new LinearLayout.LayoutParams(0,-2,1));CheckBox check=new CheckBox(app);check.setButtonTintList(android.content.res.ColorStateList.valueOf(ToolPage.ACCENT));check.setChecked(app.prefs.getBoolean(key,fallback));check.setContentDescription(label);row.addView(check,new LinearLayout.LayoutParams(p.dp(40),p.dp(44)));check.setOnCheckedChangeListener((v,on)->{app.prefs.edit().putBoolean(key,on).apply();app.applyPreferences();if(key.equals("autoPreview")||key.equals("previewIntegral"))app.changed();});row.setOnClickListener(v->check.toggle());p.body.addView(row);}

 private void replaceSettings(int tab){ToolPage old=current;settings(tab);stack.removeLastOccurrence(old);}
 private void segments(ToolPage p,String[] labels,int selected,IntConsumer callback){segments(p,p.body,labels,selected,callback);}
 private void segments(ToolPage p,LinearLayout parent,String[] labels,int selected,IntConsumer callback){LinearLayout row=new LinearLayout(app);android.graphics.drawable.GradientDrawable border=ToolPage.rounded(app,ToolPage.BG,24);border.setStroke(p.dp(1),0xff53545e);row.setBackground(border);row.setClipToOutline(true);row.setPadding(p.dp(1),p.dp(1),p.dp(1),p.dp(1));final ArrayList<TextView> cells=new ArrayList<>();for(int i=0;i<labels.length;i++){final int n=i;TextView t=ToolPage.text(app,labels[i],17);t.setGravity(Gravity.CENTER);t.setMinHeight(p.dp(38));t.setBackgroundColor(i==selected?ToolPage.ACCENT:ToolPage.BG);t.setTextColor(i==selected?Color.BLACK:ToolPage.TEXT);row.addView(t,new LinearLayout.LayoutParams(0,-1,1));cells.add(t);if(i<labels.length-1){View line=new View(app);line.setBackgroundColor(0xff53545e);row.addView(line,new LinearLayout.LayoutParams(p.dp(1),-1));}t.setOnClickListener(v->{for(int k=0;k<cells.size();k++){cells.get(k).setBackgroundColor(k==n?ToolPage.ACCENT:ToolPage.BG);cells.get(k).setTextColor(k==n?Color.BLACK:ToolPage.TEXT);}callback.accept(n);});}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,p.dp(40));lp.topMargin=p.dp(4);parent.addView(row,lp);}


 private void skin(int selected){show(SkinPicker.create(app,selected,this::back,()->app.press("font"),()->settings(1)));}

 private static final String[][] HANDBOOK={
 {"快速入门","输入数字和运算符，按 = 计算。SHIFT 使用黄色副标，ALPHA 使用紫色副标。三横杠打开工具菜单，MODE 打开计算模式。"},
 {"自然公式与光标","分数、根号、幂、积分都有独立输入区。方向键切换分子分母、上下限，左右键退出模板。DEL 删除光标前内容，AC 清空当前公式。"},
 {"精确结果与角度","S⇌D 切换精确和小数结果；DEG 菜单选择 DEG/RAD/GRAD。ENG 选择 NORM/FIX/SCI/ENG/SI 格式与精度。图形与脚本使用弧度。"},
 {"历史、Ans 与变量","每次主计算器计算会保存历史。点历史公式返回编辑。Ans 是上次结果，PreAns 是前一次结果。变量值面板支持编辑、插入和清除；SHIFT → RCL 可存入当前结果。"},
 {"复制与粘贴","SHIFT → 0 选择复制公式、结果或公式图片，公式保留结构。ALPHA → 0 粘贴公式或图片。相机按钮拍照或选图，可裁剪、旋转并修改识别结果后插入。"},
 {"CALC 与 SOLVE","输入含变量的表达式，CALC 依次填写各变量值，用 = 确认；AC 退出代入并恢复原公式。SHIFT → CALC 求解当前方程；没有等号时按表达式 = 0 求解。历史保存输入公式。"},
 {"存取变量与自定义函数","RCL 后按 ALPHA 变量键取值；SHIFT → RCL 后按 ALPHA 变量键存入当前表达式的结果。SHIFT → 6 打开函数管理，支持命名函数和多个参数。"},
 {"数据表、矩阵与向量","MODE 选择 STAT、矩阵或向量。DATA/EDIT 在主显示区打开网格，数字键编辑，左右切列，上下或 = 切行。MODE 保存数据并打开计算模式。STAT 支持频数和七种统计/回归类型。"},
 {"第二键盘与长按","2nd 切到代数、关系符、矩阵操作和函数分类键盘，1st 返回。长按按键执行黄色副功能；长按 DEL 连续删除。ALPHA → x² 插入循环小数模板。"},
 {"图形","支持函数、参数、极坐标和隐式曲线。编辑按钮填写表达式；拖动平移、双指缩放。求根要求区间两端异号且连续，积分和切线工具用于函数模式。"},
 {"编程","编辑器保存命名脚本，控制台运行一次表达式。使用 Symja 语法：赋值 a=2，列表 {1,2}，Table(n^2,{n,1,10})，求导 D(x^2,x)。分号分隔语句；Print 输出会显示在结果区，末句可以没有返回值。脚本变量与主计算器隔离。"},
 {"声音、皮肤与设置","声音和振动可分别开关，皮肤可预览后应用。完整/基础键盘、角度、结果格式和历史上限会自动保存，重启应用继续使用。"},
 {"错误与计算限制","空输入、括号未配对或不完整模板会提示修改。单次符号计算超过 8 秒会结束。无法化简的符号结果可能保留原式；曲线在不连续点断开。"}
 };
 private void manual(String filter){ToolPage p=page("文档");p.title.setText("文档\nSciCalc");p.title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,22);p.action("search","搜索文档",()->{EditText search=new EditText(app);search.setText(filter);ReferenceDialogs.builder(app).setTitle("搜索文档").setView(search).setPositiveButton("搜索",(d,w)->{ToolPage old=current;manual(search.getText().toString());stack.removeLastOccurrence(old);}).setNegativeButton("取消",null).show();});p.action("more","文档操作",this::onlineManual);LinearLayout button=new LinearLayout(app);button.setPadding(p.dp(16),0,p.dp(16),0);p.body.addView(button);TextView online=p.button(button,"在线手册",this::onlineManual);online.setBackground(ToolPage.rounded(app,ToolPage.ACCENT,50));p.section("快速入门");p.divider();int number=1;for(String[] chapter:HANDBOOK)if((chapter[0]+chapter[1]).toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))){if(number==8){p.section("数字、算术与函数");p.divider();}p.row(number+++". "+chapter[0],chapter[1].split("。")[0]+"。",()->{ToolPage detail=page(chapter[0]);detail.inset(16);TextView content=ToolPage.text(app,chapter[1],19);content.setPadding(0,p.dp(16),0,0);detail.body.addView(content);show(detail);});}show(p);}

 private void onlineManual(){ToolPage p=page("在线手册");p.row("Symja 官方文档","语言教程与函数参考",()->browser("https://github.com/axkr/symja_android_library/blob/master/symja_android_library/doc/index.md"));p.row("本地应用手册","无需联网",()->manual(""));show(p);}
 private void browser(String url){try{app.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException e){copy(url);}}
 private void checkUpdate(){
  ToolPage p=page("检查更新");p.inset(16);p.note("当前版本：SciCalc "+BuildConfig.VERSION_NAME);TextView status=p.note("正在检查 GitHub 正式发布…");LinearLayout results=new LinearLayout(app);results.setOrientation(1);p.body.addView(results);show(p);
  Runnable check=()->{int token=++job;results.removeAllViews();status.setText("正在检查 GitHub 正式发布…");java.util.concurrent.CompletableFuture.supplyAsync(()->{try{return UpdateChecker.latest();}catch(Exception e){throw new java.util.concurrent.CompletionException(e);}}).whenComplete((release,failure)->app.main.post(()->{if(app.isDestroyed()||token!=job||current!=p)return;if(failure!=null){Throwable cause=failure.getCause()==null?failure:failure.getCause();status.setText("检查失败："+(cause instanceof java.io.IOException?cause.getMessage():"请检查网络连接"));return;}String tag=release.optString("tag_name");int compared=ReleaseVersion.compare(tag,BuildConfig.VERSION_NAME);status.setText(compared>0?"发现新版本："+tag:compared==0?"当前已是最新正式版（"+tag+"）":"当前安装版本高于最新正式版（"+tag+"）");TextView notes=ToolPage.text(app,release.optString("body","暂无更新说明"),15);notes.setTextIsSelectable(true);results.addView(notes);p.button(results,"打开正式发布页",()->browser(release.optString("html_url")));}));};p.button(p.body,"重新检查",check);p.note("仅在点按检查时获取公开版本信息；下载通过浏览器打开发布页。");check.run();
 }
 private void features(){
  ToolPage p=page("关于 SciCalc");
  p.note("SciCalc "+BuildConfig.VERSION_NAME+"\n全部计算工具在此版本中开放。\n\n原生公式编辑、科学计算、图形、历史、变量、数学脚本。");
  p.section("项目源码");
  p.row("GitHub 仓库",BuildConfig.GITHUB_REPOSITORY_URL,()->browser(BuildConfig.GITHUB_REPOSITORY_URL));
  p.section("开源组件");
  p.note("SciCalc 源码：GPL-3.0-or-later\nSymja 3.2.0：GPL / LGPL\nGuava、Log4j：Apache-2.0\n\nOCR 使用 Google ML Kit，遵循其独立 SDK 与模型条款。完整第三方声明见 GitHub 仓库。\n\n本应用独立实现界面，不包含 CalcES 的程序代码或素材。");
  p.row("操作手册","查看每个工具的使用方法",()->manual(""));show(p);
 }
 private void feedback(){ToolPage p=page("反馈");EditText text=p.input(p.body,"遇到的问题、复现步骤或功能建议",app.prefs.getString("feedbackDraft",""),true);text.setMinLines(6);draft(text,"feedbackDraft");p.note("反馈内容保存在本机。复制后可直接发给开发者。");p.button(p.body,"复制反馈",()->copy("SciCalc "+BuildConfig.VERSION_NAME+" · Android "+android.os.Build.VERSION.RELEASE+"\n"+text.getText()));p.button(p.body,"保存草稿",()->{app.prefs.edit().putString("feedbackDraft",text.getText().toString()).apply();Toast.makeText(app,"反馈草稿已保存",Toast.LENGTH_SHORT).show();});show(p);}
 private void share(){ToolPage p=page("分享");String message=app.formula.empty()?"SciCalc "+BuildConfig.VERSION_NAME+" · 科学计算器":app.formula.source(app.degrees)+(app.exact.isEmpty()?"":" = "+app.exact);p.note(message);p.button(p.body,"复制计算内容",()->copy(message));p.button(p.body,"打开系统分享",()->{Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/plain");intent.putExtra(Intent.EXTRA_TEXT,message);app.startActivity(Intent.createChooser(intent,"分享计算"));});show(p);}

 private JSONArray json(String key){try{return new JSONArray(app.prefs.getString(key,"[]"));}catch(Exception e){return new JSONArray();}}
 private void addRecord(String key,JSONObject record,int limit){JSONArray old=json(key),fresh=new JSONArray();fresh.put(record);for(int i=0;i<Math.min(old.length(),limit-1);i++)fresh.put(old.opt(i));app.prefs.edit().putString(key,fresh.toString()).apply();}
 private static final String[][] SCRIPT_DOCS={
 {"入门与算术","加减乘除 + - * /，幂 ^，精确数 1/3。函数可以用圆括号或方括号。", "Sqrt(8)+1/3"},
 {"符号与赋值","使用 = 赋值，分号分隔语句。Clear(x) 清除赋值。脚本状态在本次应用会话中保留，与主计算器的变量隔离。", "a=2; a^3"},
 {"比较与逻辑","相等使用 ==，比较 <、>、<=、>=，逻辑 And、Or、Not。", "If(2<3,10,20)"},
 {"字符串","字符串放在双引号内，Print 可以输出文本到控制台。", "Print(\"Hello SciCalc\"); 2+2"},
 {"列表与表格","列表使用花括号，Table 生成列表，Part 读取元素。", "Table(n^2,{n,1,10})"},
 {"函数定义","f(x_):=x^2 为延迟定义；下划线表示参数模式。", "f(x_):=x^2; f(5)"},
 {"条件与循环","If 条件分支，Do 重复执行，Sum、Product 进行求和和连乘。", "Sum(n,{n,1,100})"},
 {"局部变量","Module 为局部变量建立独立作用域。", "Module({a=2},a^3)"},
 {"微积分","D 求导，Integrate 积分，Limit 极限。", "Integrate(x^2,{x,0,1})"},
 {"方程","Solve 求精确根，Reduce 化简不等式。", "Solve(x^2-5*x+6==0,x)"},
 {"线性代数","矩阵用嵌套列表，Det 行列式，Inverse 逆矩阵，Dot 点积。", "Inverse({{1,2},{3,4}})"},
 {"统计","Mean、Median、StandardDeviation 处理数值列表。", "Mean({1,2,3,4,5})"}
 };
 private void programming(int tab){ToolPage p=page("编程");p.tabs(new String[]{"编辑器","控制台","文档"},tab,i->{ToolPage old=current;programming(i);stack.removeLastOccurrence(old);});
  p.action("more","脚本操作",()->ReferenceDialogs.choose(app,"脚本",new String[]{"保存为…","打开脚本","删除脚本","加载示例"},i->{if(i==0)saveScript(tab==1?"consoleDraft":"scriptDraft");if(i==1)loadScripts(false);if(i==2)loadScripts(true);if(i==3)ReferenceDialogs.choose(app,"示例",Arrays.stream(SCRIPT_DOCS).map(a->a[0]).toArray(String[]::new),n->{app.prefs.edit().putString("scriptDraft",SCRIPT_DOCS[n][2]).apply();ToolPage old=current;programming(0);stack.removeLastOccurrence(old);});}));
  if(tab==2){EditText search=p.input(p.body,"搜索文档","",false);LinearLayout list=new LinearLayout(app);list.setOrientation(1);p.body.addView(list);Runnable render=()->{list.removeAllViews();String q=search.getText().toString();for(String[] doc:SCRIPT_DOCS)if((doc[0]+doc[1]+doc[2]).contains(q)){TextView t=ToolPage.text(app,doc[0]+"\n\n"+doc[1]+"\n\n"+doc[2]+"\n\n点击加载到编辑器",18);t.setPadding(0,p.dp(16),0,p.dp(16));list.addView(t);t.setOnClickListener(v->{app.prefs.edit().putString("scriptDraft",doc[2]).apply();ToolPage old=current;programming(0);stack.removeLastOccurrence(old);});}};watch(search,s->render.run());render.run();p.row("完整函数参考","打开 Symja 官方文档",()->browser("https://github.com/axkr/symja_android_library/blob/master/symja_android_library/doc/index.md"));}
  else{p.body.setPadding(p.dp(6),0,p.dp(6),0);String key=tab==0?"scriptDraft":"consoleDraft";LinearLayout terminal=new LinearLayout(app);terminal.setOrientation(1);terminal.setBackgroundColor(0xff101010);p.body.addView(terminal);LinearLayout row=new LinearLayout(app);terminal.addView(row,new LinearLayout.LayoutParams(-1,p.dp(40)));EditText editor=p.input(terminal,tab==0?"输入 Symja 脚本":"Input",app.prefs.getString(key,""),tab==0);editor.setTypeface(UiFonts.MONO);editor.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,16);editor.setMinLines(tab==0?7:2);editor.setGravity(Gravity.TOP|Gravity.LEFT);draft(editor,key);editor.setBackgroundColor(0xff101010);scriptButton(p,row,"Copy",()->copy(editor.getText().toString()));scriptButton(p,row,"Paste",()->editor.getText().insert(Math.max(0,editor.getSelectionStart()),clipboard()));scriptButton(p,row,"Clear",()->editor.setText(""));terminal.removeView(editor);terminal.addView(editor);TextView out=ToolPage.text(app,"",17);out.setPadding(p.dp(8),p.dp(8),p.dp(8),p.dp(8));out.setTextIsSelectable(true);p.body.addView(out);out.setTypeface(UiFonts.MONO);out.setText(app.prefs.getString("scriptOutput", ""));out.setVisibility(out.getText().length()==0?View.GONE:View.VISIBLE);LinearLayout resultRow=actions(p);resultRow.setVisibility(out.getText().length()==0?View.GONE:View.VISIBLE);final String[] value={app.prefs.getString("scriptValue","")};scriptButton(p,row,"Run",()->{String source=editor.getText().toString().trim();if(source.isEmpty()){editor.setError("请输入表达式");return;}hideKeyboard();out.setVisibility(View.VISIBLE);resultRow.setVisibility(View.VISIBLE);value[0]="";final MathEngine.ScriptResult[] result={null};work(out,()->{result[0]=scripts.runScript(source);return result[0].output+(result[0].value.isEmpty()?"（无返回值）":result[0].value);},r->{value[0]=result[0].value;app.prefs.edit().putString("scriptOutput",r).putString("scriptValue",value[0]).apply();try{JSONObject j=new JSONObject();j.put("source",source);j.put("result",r);addRecord("consoleHistory",j,40);}catch(Exception ignored){}});});smallButton(p,resultRow,"复制输出",()->copy(out.getText().toString()));smallButton(p,resultRow,"使用结果",()->{if(!value[0].isEmpty())use(value[0],value[0]);});smallButton(p,resultRow,"运行记录",this::consoleHistory);}
  show(p);
 }
 private void scriptButton(ToolPage p,LinearLayout row,String label,Runnable action){if(label.equals("Run"))row.addView(new View(app),new LinearLayout.LayoutParams(0,-1,1));TextView text=ToolPage.text(app,label,17);text.setTypeface(UiFonts.MONO);text.setGravity(Gravity.CENTER);text.setOnClickListener(v->action.run());row.addView(text,new LinearLayout.LayoutParams(p.dp(label.equals("Run")?48:64),-1));}
 private void saveScript(String draftKey){ToolPage p=page("保存脚本");EditText name=p.input(p.body,"脚本名称","",false);p.button(p.body,"保存",()->{String title=name.getText().toString().trim();if(title.isEmpty()){name.setError("填写名称");return;}JSONObject all;try{all=new JSONObject(app.prefs.getString("scripts","{}"));all.put(title,app.prefs.getString(draftKey,draftKey.equals("scriptDraft")?"Table(n^2,{n,1,10})":""));app.prefs.edit().putString("scripts",all.toString()).apply();back();Toast.makeText(app,"脚本已保存",Toast.LENGTH_SHORT).show();}catch(Exception e){name.setError("保存失败");}});show(p);}
 private void loadScripts(boolean delete){JSONObject all;try{all=new JSONObject(app.prefs.getString("scripts","{}"));}catch(Exception e){all=new JSONObject();}final JSONObject saved=all;ArrayList<String> names=new ArrayList<>();Iterator<String> it=all.keys();while(it.hasNext())names.add(it.next());Collections.sort(names);if(names.isEmpty()){Toast.makeText(app,"还没有保存的脚本",Toast.LENGTH_SHORT).show();return;}ReferenceDialogs.choose(app,delete?"删除脚本":"打开脚本",names.toArray(new String[0]),i->{String name=names.get(i);if(delete)confirm("删除脚本“"+name+"”？",()->{saved.remove(name);app.prefs.edit().putString("scripts",saved.toString()).apply();});else{app.prefs.edit().putString("scriptDraft",saved.optString(name)).apply();ToolPage old=current;programming(0);stack.removeLastOccurrence(old);}});}
 private void consoleHistory(){ToolPage p=page("运行记录");p.action("more","清空运行记录",()->confirm("清空运行记录？",()->{app.prefs.edit().remove("consoleHistory").apply();ToolPage old=current;consoleHistory();stack.removeLastOccurrence(old);}));JSONArray records=json("consoleHistory");if(records.length()==0)p.note("还没有运行记录。");for(int i=0;i<records.length();i++){JSONObject j=records.optJSONObject(i);if(j!=null)p.row(j.optString("source"),j.optString("result"),()->{app.prefs.edit().putString("consoleDraft",j.optString("source")).apply();programming(1);});}show(p);}
}
