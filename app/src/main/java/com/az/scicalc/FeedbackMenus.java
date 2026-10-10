package com.az.scicalc;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import android.view.*;
import android.content.res.ColorStateList;
import java.util.*;
import java.util.function.BooleanSupplier;

final class FeedbackMenus {
 static final class Page extends ToolPage {
  final MainActivity app;final ArrayList<Runnable> updates=new ArrayList<>();boolean updating;int tab;
  final FeedbackPager pager;
  Page(MainActivity a,int initial){super(a,"声音与振动",a.menuTools::back);app=a;tab=initial;
   action("more","声音与振动操作",()->ReferenceDialogs.choose(app,"声音与振动",new String[]{"重置"},i->reset(this)));
   removeView(scroll);pager=new FeedbackPager(a,initial,this::tabsChanged);tabsChanged(initial);addView(pager,new LinearLayout.LayoutParams(-1,0,1));
   for(int i=0;i<2;i++){ToolPage content=new ToolPage(a,"",()->{});content.removeView(content.scroll);build(this,content,i==0);pager.addView(content.scroll);}
   update();
  }
  void tabsChanged(int selected){tab=selected;if(getChildCount()>1&&getChildAt(1)!=pager)removeViewAt(1);tabs(new String[]{"声音","振动"},selected,i->{if(i!=tab)pager.select(i);});android.widget.HorizontalScrollView sc=(android.widget.HorizontalScrollView)getChildAt(1);LinearLayout strip=(LinearLayout)sc.getChildAt(0);for(int i=0;i<2;i++){TextView label=(TextView)((LinearLayout)strip.getChildAt(i)).getChildAt(0);final String icon=i==0?"sound":"vibration";final int color=i==selected?ToolPage.ACCENT:ToolPage.MUTED;android.graphics.drawable.Drawable glyph=new android.graphics.drawable.Drawable(){final android.graphics.Paint paint=new android.graphics.Paint(3);public void draw(android.graphics.Canvas c){android.graphics.Rect b=getBounds();UiIcons.draw(c,paint,icon,b.centerX(),b.centerY(),b.width(),color);}public void setAlpha(int a){}public void setColorFilter(android.graphics.ColorFilter f){}public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}};glyph.setBounds(0,0,dp(22),dp(22));label.setCompoundDrawables(glyph,null,null,null);label.setCompoundDrawablePadding(dp(6));}}
  void update(){updating=true;try{for(Runnable r:updates)r.run();}finally{updating=false;}}
 }
 static void show(MainActivity app,int tab){FeedbackSounds.prepare(app);app.menuTools.show(new Page(app,tab));}
 static boolean enabled(Page page,String key,boolean fallback){return page.app.prefs.getBoolean(key,fallback);}
 static void build(Page page,ToolPage p,boolean sound){
  MainActivity app=page.app;String master=sound?"sound":"vibration";BooleanSupplier masterOn=()->enabled(page,master,!sound);
  toggle(page,p,sound?"音效":"振动",sound?"当您按某个键时，计算器将播放声音效果":"",master,!sound,()->true);
  LinearLayout slider=new LinearLayout(app);slider.setOrientation(1);slider.setPadding(p.dp(16),p.dp(10),p.dp(22),p.dp(14));TextView caption=ToolPage.text(app,sound?"音量":"按键时振动强度",18);slider.addView(caption);TextView percent=ToolPage.text(app,"",18);if(sound)slider.addView(percent);
  SeekBar seek=new SeekBar(app);seek.setMax(100);seek.setContentDescription(sound?"音量":"按键时振动强度");seek.setProgressTintList(ColorStateList.valueOf(ToolPage.ACCENT));seek.setThumbTintList(ColorStateList.valueOf(ToolPage.ACCENT));ReferenceControls.slider(seek,ToolPage.ACCENT,ToolPage.ACCENT,ToolPage.ACCENT);
  String valueKey=sound?"soundVolume":"vibrationIntensity";
  page.updates.add(()->{boolean on=masterOn.getAsBoolean();caption.setAlpha(on?1:.3f);percent.setAlpha(on?1:.3f);seek.setEnabled(on);seek.setProgress(app.prefs.getInt(valueKey,sound?100:20));percent.setText("设备音量的 "+seek.getProgress()+"%");});
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){if(sound)FeedbackPlayer.tone(app.prefs.getString("tone:keys","Tap"),s.getProgress());}public void onProgressChanged(SeekBar s,int value,boolean user){if(user){app.prefs.edit().putInt(valueKey,value).apply();percent.setText("设备音量的 "+value+"%");if(!sound)FeedbackPlayer.vibrate(app.view,app.prefs.getString("vibrationPattern:keys","Click"),value);}}});
  slider.addView(seek,new LinearLayout.LayoutParams(-1,p.dp(48)));p.body.addView(slider);
  section(p,"键盘");toggle(page,p,sound?"按键音":"按键振动",sound?"按下按键时播放声音":"",master+":keys",true,masterOn);
  BooleanSupplier keysOn=()->masterOn.getAsBoolean()&&enabled(page,master+":keys",true);
  choice(page,p,"按键","keys",sound?"Tap":"Click",sound,keysOn);
  if(sound){choice(page,p,"删除","delete","Click",true,keysOn);choice(page,p,"清除","clear","Click",true,keysOn);section(p,"等号键");toggle(page,p,"等号键音","按下等号键时播放声音","sound:equals",true,masterOn);choice(page,p,"等号","equals","Click",true,()->masterOn.getAsBoolean()&&enabled(page,"sound:equals",true));}
  section(p,"结果");toggle(page,p,sound?"结果音":"结果振动",sound?"计算成功时播放声音":"计算成功时振动",master+":result",sound,masterOn);
  choice(page,p,"结果","result",sound?"Success chirp":"Double",sound,()->masterOn.getAsBoolean()&&enabled(page,master+":result",sound));
  toggle(page,p,sound?"错误音":"错误振动",sound?"计算失败时播放声音":"计算失败时振动",master+":error",true,masterOn);
  choice(page,p,"错误","error",sound?"Error buzz":"Triple",sound,()->masterOn.getAsBoolean()&&enabled(page,master+":error",true));
  p.divider();TextView reset=ToolPage.text(app,"重置",18);reset.setTextColor(ToolPage.ACCENT);reset.setGravity(Gravity.RIGHT);reset.setPadding(p.dp(16),p.dp(14),p.dp(40),p.dp(14));reset.setOnClickListener(v->reset(page));p.body.addView(reset);
 }
 static void reset(Page page){MainActivity app=page.app;android.content.SharedPreferences.Editor e=app.prefs.edit();for(String k:app.prefs.getAll().keySet())if(k.startsWith("sound:")||k.startsWith("tone:")||k.startsWith("vibration:")||k.startsWith("vibrationPattern:"))e.remove(k);e.putBoolean("sound",false).putBoolean("vibration",true).putInt("soundVolume",100).putInt("vibrationIntensity",20).apply();app.applyPreferences();page.update();}
 static void section(ToolPage p,String name){p.divider();TextView t=ToolPage.text(p.getContext(),name,18);t.setTextColor(ToolPage.ACCENT);t.setBackgroundColor(0xff343339);t.setPadding(p.dp(16),0,0,0);p.body.addView(t,new LinearLayout.LayoutParams(-1,p.dp(42)));}
 static void toggle(Page page,ToolPage p,String title,String description,String key,boolean fallback,BooleanSupplier allowed){
  MainActivity app=page.app;LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(p.dp(16),p.dp(12),p.dp(16),p.dp(12));LinearLayout words=new LinearLayout(app);words.setOrientation(1);words.addView(ToolPage.text(app,title,18));if(!description.isEmpty())words.addView(ToolPage.text(app,description,16));row.addView(words,new LinearLayout.LayoutParams(0,-2,1));Switch control=new Switch(app);control.setContentDescription(title);control.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{0xff000000,0xff777681}));control.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{ToolPage.ACCENT,0xff50505a}));ReferenceControls.toggle(control);row.addView(control,new LinearLayout.LayoutParams(p.dp(58),p.dp(44)));
  page.updates.add(()->{control.setChecked(enabled(page,key,fallback));control.setEnabled(allowed.getAsBoolean());words.setAlpha(allowed.getAsBoolean()?1:.3f);});
  control.setOnCheckedChangeListener((v,on)->{if(page.updating)return;app.prefs.edit().putBoolean(key,on).apply();app.applyPreferences();page.update();});row.setOnClickListener(v->{if(allowed.getAsBoolean())control.toggle();});p.body.addView(row,new LinearLayout.LayoutParams(-1,-2));
 }
 static void choice(Page page,ToolPage p,String title,String kind,String fallback,boolean sound,BooleanSupplier allowed){
  MainActivity app=page.app;String key=(sound?"tone:":"vibrationPattern:")+kind;LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(p.dp(16),p.dp(12),p.dp(16),p.dp(12));LinearLayout words=new LinearLayout(app);words.setOrientation(1);words.addView(ToolPage.text(app,title,18));TextView summary=ToolPage.text(app,"",16);words.addView(summary);row.addView(words,new LinearLayout.LayoutParams(0,-2,1));TextView play=ToolPage.text(app,"▶",20);play.setTextColor(ToolPage.ACCENT);play.setGravity(Gravity.CENTER);
  Runnable preview=()->{String name=app.prefs.getString(key,fallback);if(sound)FeedbackPlayer.tone(name,app.prefs.getInt("soundVolume",100));else FeedbackPlayer.vibrate(app.view,name,app.prefs.getInt("vibrationIntensity",20));};
  play.setOnClickListener(v->{if(allowed.getAsBoolean())preview.run();});row.addView(play,new LinearLayout.LayoutParams(p.dp(38),p.dp(40)));
  page.updates.add(()->{String name=app.prefs.getString(key,fallback);summary.setText(name);words.setAlpha(allowed.getAsBoolean()?1:.3f);play.setContentDescription("播放 "+name);});
  row.setOnClickListener(v->{if(allowed.getAsBoolean())patterns(page,title,key,fallback,sound,preview);});p.body.addView(row);
 }
 static void patterns(Page page,String title,String key,String fallback,boolean sound,Runnable preview){
  MainActivity app=page.app;Dialog dialog=new Dialog(app);LinearLayout content=new LinearLayout(app);content.setOrientation(1);content.setBackgroundColor(ToolPage.BG);LinearLayout header=new LinearLayout(app);header.setGravity(Gravity.CENTER_VERTICAL);TextView heading=ToolPage.text(app,title,18);heading.setPadding(page.dp(16),0,0,0);header.addView(heading,new LinearLayout.LayoutParams(0,page.dp(64),1));header.addView(ToolPage.icon(app,"cross","关闭",dialog::dismiss),new LinearLayout.LayoutParams(page.dp(48),page.dp(64)));content.addView(header);
  ScrollView scroll=new ScrollView(app);LinearLayout body=new LinearLayout(app);body.setOrientation(1);body.setPadding(page.dp(14),0,page.dp(14),page.dp(8));scroll.addView(body);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  String[] groups=sound?SoundPalette.GROUPS:new String[]{"静音","单次","重复","变化","系统"};
  String[][] options=sound?SoundPalette.OPTIONS:new String[][]{{"None"},{"Click","Soft","Strong","Long"},{"Double","Triple","Stutter","Knock"},{"Ramp up","Ramp down","Heartbeat","Thump"},{"Tick","Tap","Heavy","Double tap"}};
  ArrayList<TextView> buttons=new ArrayList<>();Runnable update=()->{for(TextView button:buttons){boolean selected=button.getText().toString().equals(app.prefs.getString(key,fallback));button.setSelected(selected);GradientDrawable background=new GradientDrawable();background.setColor(ToolPage.BG);background.setCornerRadius(page.dp(13));background.setStroke(page.dp(2),selected?ToolPage.ACCENT:0xff3b4248);button.setBackground(background);}};
  for(int g=0;g<groups.length;g++){TextView group=ToolPage.text(app,groups[g],16);group.setTextColor(ToolPage.ACCENT);group.setPadding(page.dp(12),page.dp(16),0,page.dp(8));body.addView(group);
   int columns=5;for(int start=0;start<options[g].length;start+=columns){LinearLayout row=new LinearLayout(app);row.setGravity(Gravity.TOP);body.addView(row);
    for(int column=0;column<columns;column++){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,page.dp(44),1);lp.rightMargin=page.dp(column==columns-1?0:6);lp.bottomMargin=page.dp(4);if(start+column>=options[g].length){row.addView(new View(app),lp);continue;}
     String name=options[g][start+column];TextView button=ToolPage.text(app,name,14);button.setGravity(Gravity.CENTER);button.setMaxLines(2);buttons.add(button);row.addView(button,lp);button.setOnClickListener(v->{app.prefs.edit().putString(key,name).apply();page.update();update.run();preview.run();});
    }
   }
  }
  update.run();dialog.setContentView(content);dialog.show();Window window=dialog.getWindow();window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));window.setGravity(Gravity.BOTTOM);window.setLayout(-1,Math.min(app.getResources().getDisplayMetrics().heightPixels-page.dp(60),page.dp(sound?714:540)));window.setDimAmount(.35f);
 }
}
