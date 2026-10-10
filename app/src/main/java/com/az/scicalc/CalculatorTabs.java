package com.az.scicalc;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Each page owns its editor, keypad state and workflow controllers. Only one is attached. */
final class CalculatorTabs {
    static final class Page {
        final String id;String name;final TabPreferences prefs;
        Formula formula;CalculatorView view;Workbench workbench;KeyFeatures keys;
        java.util.concurrent.Future<?> calculation,preview;
        LinkedHashMap<String,String> variables=new LinkedHashMap<>();ArrayDeque<String> undo=new ArrayDeque<>();
        String exact="",ans="0",previous="0";int angle,generation;boolean decimal;int oldPosition;
        Page(String id,String name,SharedPreferences root){this.id=id;this.name=name;prefs=new TabPreferences(root,id);}
    }
    final MainActivity app;final SharedPreferences root;final FrameLayout surface;
    final ArrayList<Page> pages=new ArrayList<>();final ArrayDeque<Page> closed=new ArrayDeque<>();
    Page selected,context;private int nextName=1;
    private final LinearLayout bar,labels;private final HorizontalScrollView scroll;private final TextView plus;
    private boolean refreshing;private String stripLayout="";
    CalculatorTabs(MainActivity app,SharedPreferences root,FrameLayout surface){
        this.app=app;this.root=root;this.surface=surface;
        bar=new LinearLayout(app);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setContentDescription("计算标签栏");
        scroll=new HorizontalScrollView(app);scroll.setHorizontalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);scroll.setFillViewport(false);
        labels=new LinearLayout(app);labels.setGravity(Gravity.CENTER_VERTICAL);scroll.addView(labels,new FrameLayout.LayoutParams(-2,-1));bar.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));
        plus=label("＋");plus.setPadding(0,0,0,0);plus.setEllipsize(null);plus.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,18);plus.setContentDescription("新建计算标签（长按恢复关闭的标签）");plus.setOnClickListener(v->create(false));plus.setOnLongClickListener(v->{restoreClosed();return true;});bar.addView(plus,new LinearLayout.LayoutParams(dp(30),-1));
        surface.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->position());
        surface.setOnApplyWindowInsetsListener((v,insets)->{v.post(this::position);return insets;});
    }
    private int dp(float n){return Math.round(n*app.getResources().getDisplayMetrics().density);}
    private TextView label(String text){TextView v=new TextView(app);v.setText(text);v.setTextSize(11);v.setGravity(Gravity.CENTER);v.setSingleLine(true);v.setEllipsize(TextUtils.TruncateAt.END);v.setPadding(dp(8),0,dp(8),0);return v;}
    private String stateKey(Page p){return "tab:"+p.id+":state";}
    private JSONObject readState(Page p)throws JSONException{JSONObject state=new JSONObject(root.getString(stateKey(p),"{}"));if(root.contains(stateKey(p)+":undo"))state.put("undo",new JSONArray(root.getString(stateKey(p)+":undo","[]")));return state;}
    void restore(){
        CalculatorView original=app.view;
        JSONObject book;try{book=new JSONObject(root.getString("tabsBook","{}"));}catch(Exception e){book=new JSONObject();}
        nextName=Math.max(1,book.optInt("nextName",1));JSONArray entries=book.optJSONArray("pages");
        if(entries!=null)for(int i=0;i<Math.min(32,entries.length());i++)try{JSONObject entry=entries.getJSONObject(i);Page p=new Page(entry.getString("id"),entry.optString("name","计算 "+nextName),root);init(p,readState(p));pages.add(p);}catch(Exception ignored){}
        if(pages.isEmpty()){
            Page p=new Page(UUID.randomUUID().toString(),"计算 "+nextName++,root);p.prefs.importLegacy();p.formula=app.formula;p.view=app.view;p.workbench=app.workbench;p.keys=app.keyFeatures;p.variables=app.variables;p.undo=app.undo;context=p;capture(p);pages.add(p);
        }
        selected=pages.get(0);for(Page p:pages)if(p.id.equals(book.optString("selected")))selected=p;
        JSONArray recent=book.optJSONArray("closed");if(recent!=null)for(int i=0;i<Math.min(10,recent.length());i++)try{JSONObject entry=recent.getJSONObject(i);Page p=new Page(entry.getString("id"),entry.getString("name"),root);p.oldPosition=entry.optInt("position");init(p,readState(p));closed.addLast(p);}catch(Exception ignored){}
        bind(selected);if(original!=app.view)surface.removeView(original);if(app.view.getParent()==null)surface.addView(app.view,0,new FrameLayout.LayoutParams(-1,-1));surface.addView(bar);position();refresh();persistAll();
    }
    private void init(Page p,JSONObject state){
        p.formula=new Formula();p.formula.restore(state.optString("formula","[]"));p.formula.restoreCursor(state.optJSONObject("cursor"));
        p.view=new CalculatorView(app,p.formula,app);p.view.restorePage(state.optJSONObject("view"));
        p.exact=state.optString("exact");p.ans=state.optString("ans","0");p.previous=state.optString("previous","0");p.angle=Math.max(0,Math.min(2,state.optInt("angle")));p.decimal=state.optBoolean("decimal");
        JSONObject vars=state.optJSONObject("variables");if(vars!=null){Iterator<String> it=vars.keys();while(it.hasNext()){String k=it.next();p.variables.put(k,vars.optString(k));}}JSONArray undo=state.optJSONArray("undo");if(undo!=null)for(int i=Math.max(0,undo.length()-60);i<undo.length();i++)p.undo.addLast(undo.optString(i));
        Page old=context;bind(p);p.workbench=new Workbench(app);p.workbench.restorePage(state.optJSONObject("workbench"));p.keys=new KeyFeatures(app);p.keys.restorePage(state.optJSONObject("keys"));app.workbench=p.workbench;app.keyFeatures=p.keys;if(old!=null)bind(old);
    }
    private void capture(Page p){p.exact=app.exact;p.ans=app.ans;p.previous=app.previous;p.angle=app.angleMode;p.decimal=app.decimal;p.generation=app.generation;}
    private void bind(Page p){context=p;app.prefs=p.prefs;app.formula=p.formula;app.view=p.view;app.workbench=p.workbench;app.keyFeatures=p.keys;app.variables=p.variables;app.undo=p.undo;app.exact=p.exact;app.ans=p.ans;app.previous=p.previous;app.angleMode=p.angle;app.degrees=p.angle==0;app.decimal=p.decimal;app.generation=p.generation;}
    private JSONObject state(Page p,boolean full){
        try{JSONObject data=new JSONObject();data.put("formula",p.formula.save()).put("cursor",p.formula.saveCursor()).put("view",p.view.savePage()).put("exact",p.exact).put("ans",p.ans).put("previous",p.previous).put("angle",p.angle).put("decimal",p.decimal).put("variables",new JSONObject(p.variables)).put("workbench",p.workbench.savePage()).put("keys",p.keys.savePage());if(full)data.put("undo",new JSONArray(p.undo));return data;}catch(Exception e){return new JSONObject();}
    }
    void persist(boolean full){if(context==null)return;capture(context);SharedPreferences.Editor e=root.edit().putString(stateKey(context),state(context,false).toString());if(full)e.putString(stateKey(context)+":undo",new JSONArray(context.undo).toString());e.apply();}
    void persistAll(){if(context!=null)capture(context);SharedPreferences.Editor e=root.edit();for(Page p:pages)e.putString(stateKey(p),state(p,false).toString()).putString(stateKey(p)+":undo",new JSONArray(p.undo).toString());for(Page p:closed)e.putString(stateKey(p),state(p,false).toString()).putString(stateKey(p)+":undo",new JSONArray(p.undo).toString());e.apply();header();}
    private void header(){try{JSONArray entries=new JSONArray(),recent=new JSONArray();for(Page p:pages)entries.put(new JSONObject().put("id",p.id).put("name",p.name));for(Page p:closed)recent.put(new JSONObject().put("id",p.id).put("name",p.name).put("position",p.oldPosition));root.edit().putString("tabsBook",new JSONObject().put("pages",entries).put("closed",recent).put("selected",selected.id).put("nextName",nextName).toString()).apply();}catch(Exception ignored){}}
    boolean foreground(){return context==selected;}
    Page owner(){return context;}
    boolean valid(Page p,int generation){return !app.isDestroyed()&&(pages.contains(p)||closed.contains(p))&&generation==(context==p?app.generation:p.generation);}
    void inPage(Page p,Runnable action){Page old=context;capture(old);bind(p);try{action.run();}finally{capture(p);bind(old);refresh();}}
    void select(Page target){
        if(target==selected)return;app.prepareTabSwitch();persist(true);CalculatorView old=selected.view;old.cancelTouches();selected=target;bind(target);surface.removeView(old);surface.addView(app.view,0,new FrameLayout.LayoutParams(-1,-1));position();app.applyPreferences();header();refresh();app.schedulePreview();
    }
    boolean create(boolean duplicate){
        if(pages.size()>=32){app.toast("最多同时打开 32 个计算标签");return false;}
        capture(context);Page source=selected;Page p=new Page(UUID.randomUUID().toString(),duplicate?source.name+" 副本":"计算 "+nextName++,root);
        JSONObject data=new JSONObject();if(duplicate){data=state(source,true);p.prefs.copyFrom(source.prefs);}else {p.prefs.inheritFormat(source.prefs);try{data.put("angle",source.angle).put("decimal",source.decimal);}catch(Exception ignored){}}
        init(p,data);pages.add(p);select(p);persist(true);header();return true;
    }
    void close(Page p){
        if(pages.size()==1){app.toast("保留至少一个计算标签");return;}
        int index=pages.indexOf(p);if(p==selected)select(pages.get(index==0?1:index-1));p.oldPosition=index;pages.remove(p);closed.addFirst(p);while(closed.size()>10){Page expired=closed.removeLast();expired.generation++;SharedPreferences.Editor e=root.edit();for(String k:root.getAll().keySet())if(k.startsWith("tab:"+expired.id+":"))e.remove(k);e.apply();}header();refresh();app.toast("已关闭，可长按 ＋ 恢复");
    }
    void restoreClosed(){if(closed.isEmpty()){app.toast("没有最近关闭的标签");return;}if(pages.size()>=32){app.toast("请先关闭一个标签");return;}Page p=closed.removeFirst();pages.add(Math.min(p.oldPosition,pages.size()),p);select(p);header();}
    private void rename(Page p){EditText input=new EditText(app);input.setSingleLine(true);input.setText(p.name);input.setSelectAllOnFocus(true);input.setTextColor(ToolPage.TEXT);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(24)});ReferenceDialogs.builder(app).setTitle("重命名标签").setView(input).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{String text=input.getText().toString().trim();if(!text.isEmpty()){p.name=text;header();refresh();}}).show();}
    private void actions(Page p){ReferenceDialogs.choose(app,p.name,new String[]{"重命名","复制标签","关闭标签","恢复最近关闭标签"},i->{if(i==0)rename(p);else if(i==1){select(p);create(true);}else if(i==2)close(p);else restoreClosed();});}
    void refresh(){
        if(selected==null||refreshing)return;int[] colors=selected.view.tabColors();StringBuilder signature=new StringBuilder(selected.id+Arrays.toString(colors));for(Page p:pages)signature.append('|').append(p.id).append(':').append(p.name);String next=signature.toString();if(next.equals(stripLayout))return;stripLayout=next;refreshing=true;labels.removeAllViews();bar.setBackgroundColor(Color.TRANSPARENT);plus.setTextColor(colors[1]);plus.setBackgroundColor(Color.TRANSPARENT);
        for(Page p:pages){TextView v=label(p.name);v.setContentDescription("计算标签："+p.name);v.setSelected(p==selected);v.setTypeface(p==selected?UiFonts.MONO_BOLD:UiFonts.REGULAR);v.setTextColor(colors[1]);if(p==selected)v.setBackground(ToolPage.rounded(app,colors[2],3));v.setMinWidth(dp(64));v.setMaxWidth(dp(112));v.setOnClickListener(w->select(p));v.setOnLongClickListener(w->{actions(p);return true;});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-1);lp.setMargins(dp(2),dp(1),dp(2),dp(1));labels.addView(v,lp);if(p==selected)v.post(()->{int left=v.getLeft(),right=v.getRight();if(left<scroll.getScrollX())scroll.smoothScrollTo(left,0);else if(right>scroll.getScrollX()+scroll.getWidth())scroll.smoothScrollTo(right-scroll.getWidth(),0);});}
        refreshing=false;
    }
    private void position(){if(surface.getWidth()==0||surface.getHeight()==0)return;WindowInsets insets=surface.getRootWindowInsets();int top=insets==null?Math.round(surface.getHeight()*78f/2400):insets.getInsets(WindowInsets.Type.statusBars()).top;FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.round(surface.getWidth()*1044f/1080),Math.max(dp(24),label("计算").getLineHeight()+dp(4)));lp.leftMargin=Math.round(surface.getWidth()*18f/1080);lp.topMargin=top;bar.setLayoutParams(lp);for(Page p:pages)p.view.setTabHeaderPixels(top+lp.height,surface.getWidth(),surface.getHeight());}
}
