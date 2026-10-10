package com.az.scicalc;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public final class MainActivity extends Activity implements CalculatorView.Listener {
    Formula formula;
    CalculatorView view;
    SharedPreferences prefs;
    final MathEngine engine=new MathEngine();
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    final Handler main=new Handler(Looper.getMainLooper());
    final ArrayList<JSONObject> history=new ArrayList<>();
    private final ArrayDeque<String> undo=new ArrayDeque<>();
    final LinkedHashMap<String,String> variables=new LinkedHashMap<>();
    String exact="",ans="0",previous="0";
    boolean degrees=true,decimal=false;
    int angleMode=0;
    KeyFeatures keyFeatures;
    Workbench workbench;
    MathOcr ocr;
    MenuTools menuTools;
    private int generation=0;
    private GraphView graph;
    private FrameLayout surface;
    private ToolPage toolOverlay;
    private ToolPage fullPage;
    private CalculatorPanel panel;
    private MotionEvent edgeDown;private boolean edgeOpening;
    private boolean dismissingPopupTouch;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);getWindow().setNavigationBarContrastEnforced(false);
        prefs=getSharedPreferences("calculator",MODE_PRIVATE);if(prefs.getInt("historyLimit",1000)<1000)prefs.edit().putInt("historyLimit",1000).apply();cleanRemovedToolData();formula=new Formula();formula.restore(prefs.getString("formula","[]"));ans=prefs.getString("ans","0");previous=prefs.getString("previous","0");degrees=prefs.getBoolean("degrees",true);decimal=prefs.getBoolean("decimal",false);exact=prefs.getString("exact","");
        try{JSONArray a=new JSONArray(prefs.getString("history","[]"));for(int i=0;i<a.length();i++)history.add(a.getJSONObject(i));JSONObject v=new JSONObject(prefs.getString("variables","{}"));Iterator<String> it=v.keys();while(it.hasNext()){String k=it.next();variables.put(k,v.getString(k));}}catch(Exception ignored){}
        view=new CalculatorView(this,formula,this);view.angle=degrees?"DEG":"RAD";view.approximate=decimal;view.result=prefs.getString("displayResult",exact);view.basic=prefs.getBoolean("basic",false);surface=new FrameLayout(this);surface.addView(view,new FrameLayout.LayoutParams(-1,-1));setContentView(surface);
        FeedbackSounds.prepare(this);
        angleMode=prefs.getInt("angleMode",degrees?0:1);menuTools=new MenuTools(this);workbench=new Workbench(this);keyFeatures=new KeyFeatures(this);ocr=new MathOcr(this);view.restoreTape(prefs.getString("tape","[]"));view.inputActive=!formula.empty()||view.tape.isEmpty()||exact.isEmpty();applyPreferences();
        worker.execute(()->{try{engine.initialize();}catch(Throwable e){main.post(()->error("数学引擎初始化失败："+e.getClass().getSimpleName()));}});
    }
    private void cleanRemovedToolData(){
        if(prefs.getBoolean("removedToolDataV1",false))return;
        SharedPreferences.Editor edit=prefs.edit();
        for(String key:prefs.getAll().keySet())if(key.startsWith("rate_")||key.startsWith("unit:")||key.startsWith("unitHistory")||key.startsWith("unitFavorites")||key.startsWith("currency")||key.startsWith("formulaInput:")||key.equals("formulaFavorites")||key.startsWith("chem")||key.equals("atomFormula")||key.equals("massFormula"))edit.remove(key);
        edit.putBoolean("removedToolDataV1",true).apply();
    }
    @Override protected void onResume(){super.onResume();if(view!=null&&!view.busy&&view.result.isEmpty()){main.removeCallbacks(previewInput);main.postDelayed(previewInput,300);}}
    @Override protected void onPause(){super.onPause();main.removeCallbacks(previewInput);view.cancelTouches();if(panel!=null)panel.cancelTouch();if(edgeDown!=null){edgeDown.recycle();edgeDown=null;}edgeOpening=false;FeedbackPlayer.release();save();if(graph!=null)graph.saveState();}
    @Override protected void onDestroy(){super.onDestroy();if(ocr!=null)ocr.close();worker.shutdownNow();}
    @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);if(ocr!=null)ocr.result(request,code,data);}
    int historyLimit(){return Math.max(1000,prefs.getInt("historyLimit",1000));}
    void save(){save(true);}
    private void save(boolean includeHistory){
        SharedPreferences.Editor edit=prefs.edit().putString("tape",view.saveTape()).putInt("angleMode",angleMode).putString("formula",formula.save()).putString("ans",ans).putString("previous",previous).putString("exact",exact).putString("displayResult",view.result).putBoolean("degrees",degrees).putBoolean("decimal",decimal).putBoolean("basic",view.basic).putString("variables",new JSONObject(variables).toString());
        if(includeHistory){JSONArray h=new JSONArray();for(JSONObject j:history)h.put(j);edit.putString("history",h.toString());}
        edit.apply();
    }
    private final Runnable previewInput=()->{
        int id=generation;
        if(isDestroyed()||!prefs.getBoolean("autoPreview",true)||view.gridActive||view.workflowStep>=0||!view.prompt.isEmpty()||view.baseMode||!formula.complete()||formula.empty())return;
        String source;
        try{
            source=formula.calculationSource(angleMode,prefs.getBoolean("implicitPriority",true));
            if(!prefs.getBoolean("previewIntegral",false)&&source.contains("Integrate("))return;
            source=vars(source);
        }catch(Exception ignored){return;}
        final String expression=source;
        boolean approx=decimal||!prefs.getBoolean("casEnabled",true);
        worker.execute(()->{try{String result=engine.evaluate(expression,approx,prefs.getInt("digits",11));main.post(()->{if(id==generation&&!isDestroyed()){view.result=approx?formatted(result):ResultFormat.exactDisplay(result,prefs.getString("numberFormat","normal"),prefs.getInt("digits",11),prefs.getBoolean("scientificE",false));view.invalidate();}});}catch(Throwable ignored){}});
    };
    void changed(){
        view.editing();++generation;view.busy=false;view.message="";view.result="";exact="";view.invalidate();
        // Editing cannot alter committed history or variables. Avoid encoding
        // the entire history for every finger release.
        save(false);
        main.removeCallbacks(previewInput);
        if(view.baseMode)workbench.modes.previewBase();
        else main.postDelayed(previewInput,300);
    }
    void checkpoint(){undo.addLast(formula.save());while(undo.size()>60)undo.removeFirst();}
    private void closeFullPage(){if(fullPage==null)return;ToolPage exiting=fullPage;fullPage=null;exiting.closing=true;exiting.animate().cancel();exiting.animate().translationX(surface.getWidth()).setDuration(300).setInterpolator(new android.view.animation.DecelerateInterpolator()).withEndAction(()->{if(fullPage!=exiting&&exiting.closing)surface.removeView(exiting);}).start();view.animate().translationX(0).setDuration(300).start();}
    void calculator(){closeFullPage();if(toolOverlay!=null){ToolPage exiting=toolOverlay;toolOverlay=null;exiting.closing=true;exiting.animate().cancel();exiting.animate().alpha(0).setDuration(150).withEndAction(()->{if(exiting.closing&&toolOverlay!=exiting)surface.removeView(exiting);}).start();}if(surface.getParent()==null)setContentView(surface);applyPreferences();}
    void showTool(ToolPage page){showTool(page,false,true);}
    void showTool(ToolPage page,boolean backwards){showTool(page,backwards,true);}
    void replaceTool(ToolPage page){showTool(page,false,false);}
    private void showTool(ToolPage page,boolean backwards,boolean animate){
        view.cancelTouches();
        if(toolOverlay!=null){toolOverlay.animate().cancel();surface.removeView(toolOverlay);toolOverlay=null;}
        if(!page.embedded){
            if(fullPage!=null&&!backwards&&page.title.getText().toString().equals(fullPage.title.getText().toString()))animate=false;
            ToolPage outgoing=fullPage;fullPage=page;updateSystemBars();page.animate().cancel();page.closing=false;page.setAlpha(1);
            if(page.getParent()!=null)((ViewGroup)page.getParent()).removeView(page);if(surface.getParent()==null)setContentView(surface);
            int width=surface.getWidth();int index=backwards&&outgoing!=null&&outgoing.getParent()==surface?surface.indexOfChild(outgoing):surface.getChildCount();surface.addView(page,index,new FrameLayout.LayoutParams(-1,-1));
            page.setTranslationX(animate?(backwards?-width*.25f:width):0);if(animate)page.animate().translationX(0).setDuration(300).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
            if(outgoing!=null&&outgoing!=page){outgoing.closing=true;outgoing.animate().cancel();if(animate)outgoing.animate().translationX(backwards?width:-width*.25f).setDuration(300).withEndAction(()->{if(fullPage!=outgoing&&outgoing.closing)surface.removeView(outgoing);}).start();else surface.removeView(outgoing);}
            view.animate().translationX(-width*.25f).setDuration(animate?300:0).start();return;
        }
        closeFullPage();
        if(page.getParent()!=null)((ViewGroup)page.getParent()).removeView(page);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(page.overlayWidth==0?-1:Math.round(view.getWidth()*page.overlayWidth/1080f),Math.round(view.getHeight()*(page.overlayBottom-page.overlayTop)/2400f));
        lp.topMargin=Math.round(view.getHeight()*page.overlayTop/2400f);lp.gravity=Gravity.TOP|(page.overlayLeft<0?Gravity.CENTER_HORIZONTAL:Gravity.LEFT);if(page.overlayLeft>=0)lp.leftMargin=Math.round(view.getWidth()*page.overlayLeft/1080f);
        page.closing=false;page.animate().cancel();page.setAlpha(0);toolOverlay=page;surface.addView(page,lp);if(surface.getParent()==null)setContentView(surface);page.animate().alpha(1).setDuration(150).start();view.invalidate();
    }
    void applyPreferences(){view.setTheme(prefs.getInt("skin",0));view.setFeedback(prefs.getBoolean("sound",false),prefs.getBoolean("vibration",true));view.angle=new String[]{"DEG","RAD","GRA"}[angleMode];view.displayScale=prefs.getFloat("displayScale",1);view.displayMono=prefs.getBoolean("displayMono",true);view.blinkCursor=prefs.getBoolean("blinkCursor",true);view.ansStyle=prefs.getInt("ansStyle",2);view.fractionMultiply=prefs.getBoolean("fractionMultiply",true);view.descendingPowers=prefs.getBoolean("descendingPowers",false);view.showCalcInfo=prefs.getBoolean("showCalcInfo",true);view.relativePercent=prefs.getBoolean("relativePercent",true);view.implicitPriority=prefs.getBoolean("implicitPriority",true);view.divisionSlash=prefs.getBoolean("divisionSlash",false);view.multiplicationStar=prefs.getBoolean("multiplicationStar",false);view.keyLabelStyle=prefs.getInt("keyLabelStyle",0);view.keyFontScale=new float[]{.85f,1,1.15f}[prefs.getInt("keyFontSize",1)];view.groupThousands=prefs.getBoolean("groupThousands",true);view.groupDecimals=prefs.getBoolean("groupDecimals",true);view.decimalComma=prefs.getBoolean("decimalComma",false);view.formatLabel=prefs.getString("numberFormat","normal").equals("normal")?"NORM":prefs.getString("numberFormat","normal").toUpperCase(Locale.ROOT);view.digits=prefs.getInt("digits",11);view.approximate=decimal;view.invalidate();if(prefs.getBoolean("keepScreenOn",false))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);int ui=View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;if(!prefs.getBoolean("showStatusBar",true))ui|=View.SYSTEM_UI_FLAG_FULLSCREEN;getWindow().getDecorView().setSystemUiVisibility(ui);updateSystemBars();setRequestedOrientation(new int[]{android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}[prefs.getInt("orientation",0)]);}
    private void updateSystemBars(){View decor=getWindow().getDecorView();int flags=decor.getSystemUiVisibility(),light=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;if(fullPage==null&&graph==null&&prefs.getInt("skin",0)==8)flags|=light;else flags&=~light;decor.setSystemUiVisibility(flags);}
    String formatted(String value){return ResultFormat.scientific(ResultFormat.format(value,prefs.getString("numberFormat","normal"),prefs.getInt("digits",11)),prefs.getBoolean("scientificE",false));}
    void error(String s){view.resultFeedback(false);view.message=s;view.busy=false;view.invalidate();}
    String vars(String s){s=workbench.expandFunctions(s);LinkedHashMap<String,String> values=new LinkedHashMap<>();values.put("Ans",ans);for(Map.Entry<String,String> v:variables.entrySet())values.put(v.getKey().equals("e")?"scvE":v.getKey(),v.getValue());return MathFunctions.percent(MathFunctions.comparison(ScopedValues.apply(s,values)),prefs.getBoolean("relativePercent",true));}

    @Override public void press(String action){
        if(!action.equals("shift")&&!action.equals("alpha")){view.shift=false;view.alpha=false;}
        if(workbench!=null&&workbench.handle(action))return;
        if(keyFeatures!=null&&keyFeatures.handle(action))return;
        if(menuTools!=null&&menuTools.handle(action))return;
        if(action.equals("shift")){view.shift=!view.shift;view.alpha=false;view.invalidate();return;}
        if(action.equals("alpha")){view.alpha=!view.alpha;view.shift=false;view.invalidate();return;}
        view.shift=false;view.alpha=false;
        if(action.equals("left")||action.equals("right")||action.equals("up")||action.equals("down")){if(action.equals("left"))formula.left();if(action.equals("right"))formula.right();if(action.equals("up"))formula.vertical(false);if(action.equals("down"))formula.vertical(true);view.invalidate();return;}
        if(action.startsWith("text:")||action.startsWith("tpl:")||action.startsWith("func:")){checkpoint();if(action.startsWith("text:")){String input=action.substring(5);if(input.equals("(")&&prefs.getBoolean("closeParen",false)){formula.text("()");formula.left();changed();return;}if(formula.empty()&&(input.equals("=")||!view.inputActive&&(input.equals("+")||input.equals("−")||input.equals("×")||input.equals("÷"))))formula.answer(ans);if(input.equals("."))formula.decimalPoint();else if(input.equals(",")&&formula.nextArgument()){}else if(!input.equals(")")||!formula.closeFunction())formula.text(input);}else if(action.startsWith("tpl:"))formula.template(action.substring(4),"");else formula.template("func",action.substring(5));changed();if(action.startsWith("func:")&&action.substring(5).matches("(Arc)?(Sin|Cos|Tan|Cot|Sec|Csc)")&&!prefs.getBoolean("trigAngleChosen",false))DisplayMenus.angle(this);return;}
        switch(action){
            case "evaluate":calculate();break;
            case "delete":checkpoint();formula.delete();changed();break;
            case "clear":checkpoint();if(formula.empty()&&view.inputActive&&!view.gridActive&&view.workflowStep<0&&!view.baseMode)view.clearScreen();formula.clear();changed();break;
            case "undo":if(!undo.isEmpty()){formula.restore(undo.removeLast());changed();}break;
            case "square":checkpoint();formula.power("2");changed();break;
            case "cube":checkpoint();formula.power("3");changed();break;
            case "reciprocal":checkpoint();formula.power("−1");changed();break;
            case "log10":checkpoint();formula.template("func","Log10");changed();break;
            case "scientific":checkpoint();formula.text("×10");formula.template("pow","");changed();break;
            case "negate":checkpoint();formula.text("−");changed();break;
            case "decimal":decimal=!decimal;view.approximate=decimal;if(!exact.isEmpty())refreshResult();else changed();save();break;
            case "angle":degrees=!degrees;view.angle=degrees?"DEG":"RAD";changed();break;
            case "history":history();break;
            case "previous":checkpoint();formula.text(previous);changed();break;
            case "store":store();break;
            case "recall":recall();break;
            case "memoryAdd":case "memorySub":memory(action.equals("memoryAdd"));break;
            case "memoryClear":variables.remove("m");save();toast("内存已清除");break;
            case "memoryRecall":checkpoint();formula.text(variables.getOrDefault("m","0"));changed();break;
            case "clearVars":variables.clear();save();toast("变量已清除");break;
            case "copy":copyResult();break;
            case "paste":{ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(clipboard.hasPrimaryClip()){checkpoint();formula.text(clipboard.getPrimaryClip().getItemAt(0).coerceToText(this).toString());changed();}}break;
            case "capture":capture();break;
            case "modulo":tool("余数",new String[]{"被除数","除数"},new String[]{"7","3"},a->"Mod("+a[0]+","+a[1]+")");break;
            case "remainder":tool("整除与余数",new String[]{"被除数","除数"},new String[]{"7","3"},a->"{Quotient("+a[0]+","+a[1]+"),Mod("+a[0]+","+a[1]+")}");break;
            case "gcd":case "lcm":tool(action.equals("gcd")?"最大公约数":"最小公倍数",new String[]{"整数 a","整数 b"},new String[]{"12","18"},a->(action.equals("gcd")?"GCD":"LCM")+"("+a[0]+","+a[1]+")");break;
            case "randomInteger":tool("随机整数",new String[]{"下界","上界"},new String[]{"1","6"},a->"RandomInteger({"+a[0]+","+a[1]+"})");break;
            case "random":checkpoint();formula.text(String.format(Locale.US,"%.8f",Math.random()));changed();break;
            case "menu":menu();break;
            case "mode":mode();break;
            case "settings":settings();break;
            case "about":about();break;
            case "second":functions();break;
            case "hyp":choose("双曲函数",new String[]{"Sinh","Cosh","Tanh","ArcSinh","ArcCosh","ArcTanh"},i->{checkpoint();formula.template("func",new String[]{"Sinh","Cosh","Tanh","ArcSinh","ArcCosh","ArcTanh"}[i]);changed();});break;
            case "engineering":if(!exact.isEmpty())task(()->{double d=engine.value(exact);if(d==0)return "0";int e=(int)(Math.floor(Math.log10(Math.abs(d))/3)*3);return String.format(Locale.US,"%.10g × 10^%d",d/Math.pow(10,e),e);},v->{view.result=v;view.invalidate();});break;
            case "reset":checkpoint();keyFeatures.cancelEntry();formula.clear();view.clearScreen();changed();break;
            case "solve":equations();break;
            case "matrix":matrices();break;
            case "vector":vectors();break;
            case "stats":statistics();break;
            case "distribution":distributions();break;
            case "complex":complex();break;
            case "functions":functions();break;
            case "constants":constants();break;
            case "limit":tool("极限",new String[]{"表达式（变量 x）","趋近值"},new String[]{"Sin(x)/x","0"},a->"Limit("+a[0]+",x->("+a[1]+"))");break;
            case "factor":applyCurrent("Factor");break;
            case "mixed":tool("带分数",new String[]{"分数"},new String[]{"7/3"},a->"{Quotient(Numerator("+a[0]+"),Denominator("+a[0]+")),Mod(Numerator("+a[0]+"),Denominator("+a[0]+"))/Denominator("+a[0]+")}");break;
            case "cuberoot":checkpoint();formula.template("root","3");changed();break;
            case "nthroot":tool("n 次根",new String[]{"被开方数","根指数"},new String[]{"32","5"},a->"Surd("+a[0]+","+a[1]+")");break;
            case "tenpower":checkpoint();formula.text("10");formula.template("pow","");changed();break;
            case "logbase":tool("指定底数的对数",new String[]{"底数","真数"},new String[]{"2","8"},a->"Log("+a[0]+","+a[1]+")");break;
            case "polar":polar();break;
            case "rectangular":tool("极坐标 → 直角坐标",new String[]{"模 r","角度 θ（"+(degrees?"度":"弧度")+"）"},new String[]{"5","30"},a->"("+a[0]+")*(Cos("+a[1]+(degrees?"*Pi/180":"")+")+I*Sin("+a[1]+(degrees?"*Pi/180":"")+"))");break;
            case "dms":tool("角度 → 度分秒",new String[]{"角度（十进制度）"},new String[]{"30.5"},a->"{IntegerPart("+a[0]+"),IntegerPart(FractionalPart("+a[0]+")*60),FractionalPart(FractionalPart("+a[0]+")*60)*60}");break;
            case "permutation":tool("排列 nPr",new String[]{"n","r"},new String[]{"5","2"},a->"("+a[0]+")!/("+a[0]+"-"+a[1]+")!");break;
            case "combination":tool("组合 nCr",new String[]{"n","r"},new String[]{"5","2"},a->"Binomial("+a[0]+","+a[1]+")");break;
            case "product":tool("连乘",new String[]{"表达式（变量 x）","下界","上界"},new String[]{"x","1","5"},a->"Product("+a[0]+",{x,"+a[1]+","+a[2]+"})");break;
            default:error("此操作未识别："+action);
        }
        view.invalidate();
    }
    private void calculate(){if(formula.empty())return;if(!formula.complete()){error("请填完整公式模板");return;}try{String source=vars(formula.calculationSource(angleMode,prefs.getBoolean("implicitPriority",true)));runResult(source,decimal||!prefs.getBoolean("casEnabled",true),true);}catch(Exception e){error(e.getMessage());}}
    void runResult(String source,boolean approximate,boolean addHistory){runResult(source,approximate,addHistory,false);}
    void runFactored(String source){runResult(source,false,true,true);}
    private void runResult(String source,boolean approximate,boolean addHistory,boolean factorOutput){
        String saved=formula.save(),format=prefs.getString("numberFormat","normal");int digits=prefs.getInt("digits",11);boolean angle=degrees;
        task(()->{String value=engine.evaluate(source,false);if(factorOutput){if(!engine.evaluate("IntegerQ("+value+")",false).equals("True"))throw new IllegalArgumentException("质因数分解需要整数");value=MathFunctions.factorDisplay(engine.evaluate("FactorInteger("+value+")",false));}String display=approximate?ResultFormat.scientific(ResultFormat.format(engine.evaluate(value,true,digits),format,digits),prefs.getBoolean("scientificE",false)):ResultFormat.exactDisplay(value,format,digits,prefs.getBoolean("scientificE",false));String kind="";if(value.startsWith("{")&&!value.contains("->")){if(engine.evaluate("MatrixQ("+value+",NumericQ)",false).equals("True"))kind="MatAns";else if(engine.evaluate("VectorQ("+value+",NumericQ)",false).equals("True"))kind="VctAns";}return new JSONObject().put("result",value).put("displayResult",display).put("arrayKind",kind).toString();},packed->{
            try{JSONObject values=new JSONObject(packed);exact=values.getString("result");previous=ans;ans=exact;view.result=values.getString("displayResult");
                if(!values.optString("arrayKind").isEmpty())variables.put(values.getString("arrayKind"),exact);
                if(addHistory){JSONObject j=new JSONObject();j.put("source",formula.source(angleMode)).put("engineSource",source).put("tree",saved).put("result",exact).put("displayResult",view.result).put("degrees",angle).put("angleMode",angleMode).put("decimal",approximate).put("mode","基本算术").put("time",System.currentTimeMillis());history.add(0,j);while(history.size()>historyLimit())history.remove(history.size()-1);view.addTape(saved,exact,view.result);formula.clear();}
                view.invalidate();save();
            }catch(JSONException e){error("保存计算结果失败");}
        });
    }
    void refreshResult(){if(exact.isEmpty()){save();return;}String value=exact;boolean approximate=decimal||!prefs.getBoolean("casEnabled",true);int digits=prefs.getInt("digits",11);String format=prefs.getString("numberFormat","normal");task(()->approximate?ResultFormat.scientific(ResultFormat.format(engine.evaluate(value,true,digits),format,digits),prefs.getBoolean("scientificE",false)):ResultFormat.exactDisplay(value,format,digits,prefs.getBoolean("scientificE",false)),v->{view.result=v;view.updateLastDisplay(v);applyPreferences();view.invalidate();save();});}
    interface Work {String run() throws Exception;}
    void task(Work work,java.util.function.Consumer<String> result){main.removeCallbacks(previewInput);int id=++generation;view.busy=true;view.message="";view.invalidate();worker.execute(()->{try{String value=work.run();main.post(()->{if(id!=generation||isDestroyed())return;view.busy=false;result.accept(value);view.invalidate();});}catch(Throwable e){main.post(()->{if(id!=generation||isDestroyed())return;error("计算失败："+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));});}});}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    void computeCurrent(java.util.function.Consumer<String> done){if(formula.empty()){done.accept(ans);return;}if(!formula.complete()){error("请填完整公式模板");return;}String source=vars(formula.source(angleMode));task(()->engine.evaluate(source,false),done);}
    void setAngle(int mode){angleMode=mode;degrees=mode==0;prefs.edit().putInt("angleMode",mode).apply();applyPreferences();if(!formula.empty())changed();else save();}
    void transformCurrent(String name){if(formula.empty()){error("请先输入表达式");return;}runResult(name+"("+formula.source(angleMode)+")",decimal,true);}
    void graphCurrent(){if(!formula.empty()&&formula.complete())prefs.edit().putString("graphF",workbench.expandFunctions(formula.source(1))).apply();openGraph();}
    void graphExpression(String source){prefs.edit().putString("graphF",source).putString("graphKind","函数").apply();openGraph();}
    void dmsResult(){String value=exact;task(()->{double n=engine.value(value);if(!Double.isFinite(n)||Math.abs(n)>Integer.MAX_VALUE)throw new IllegalArgumentException("角度超出范围");return ResultForms.dms(n);},v->{view.result=v;view.updateLastDisplay(v);save();});}
    void mixedResult(){String value=exact;if(!view.tape.isEmpty()&&view.tape.get(view.tape.size()-1).display.matches("-?[0-9]+ [0-9]+/[0-9]+")){view.result=exact;view.updateLastDisplay(exact);save();return;}task(()->ResultFormat.mixed(engine.evaluate("Numerator("+value+")",false),engine.evaluate("Denominator("+value+")",false)),v->{view.result=v;view.updateLastDisplay(v);save();});}

    void resultFormat(int index){ResultFormats.open(this,index);}
    Formula copyFormula(){Formula f=new Formula();f.restore(formula.empty()&&!view.tape.isEmpty()?view.tape.get(view.tape.size()-1).tree:formula.save());return f;}
    void copyMenu(){ExpressionContext.open(this,copyFormula(),true);}
    android.net.Uri formulaImage(Formula f) throws java.io.IOException{Formula.Renderer renderer=new Formula.Renderer();renderer.showCursor=false;renderer.color=Color.BLACK;float size=66;Formula.Box box=renderer.measure(f.root,size);if(box.w>3900){size*=3900/box.w;box=renderer.measure(f.root,size);}android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(Math.max(100,(int)box.w+48),Math.max(100,(int)(box.a+box.d)+48),android.graphics.Bitmap.Config.ARGB_8888);android.graphics.Canvas canvas=new android.graphics.Canvas(image);canvas.drawColor(Color.WHITE);renderer.draw(canvas,f.root,24,24+box.a,size);try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(getCacheDir(),"clipboard.jpg"))){image.compress(android.graphics.Bitmap.CompressFormat.JPEG,100,out);}image.recycle();android.net.Uri uri=android.net.Uri.parse("content://"+getPackageName()+".images/clipboard.jpg");return uri;}
    void copyImage(){try{android.net.Uri uri=formulaImage(copyFormula());((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newUri(getContentResolver(),"公式图片",uri));toast("公式图片已复制");}catch(Exception e){toast("复制图片失败："+e.getMessage());}}
    @Override public void copyResult(){if(view.result.isEmpty())return;((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("计算结果",view.result));toast("结果已复制");}
    private void choose(String title,String[] labels,java.util.function.IntConsumer action){ReferenceDialogs.choose(this,title,labels,action);}
    private void history(){String[] a=new String[history.size()];for(int i=0;i<a.length;i++){JSONObject j=history.get(i);a[i]=j.optString("source")+"\n= "+j.optString("result");}if(a.length==0){toast("还没有计算记录");return;}choose("历史 · 点击重新编辑",a,i->{JSONObject j=history.get(i);checkpoint();formula.restore(j.optString("tree"));changed();exact=j.optString("result");view.result=exact;view.invalidate();});}
    private void store(){if(exact.isEmpty()){toast("先计算一个结果，再存入变量");return;}choose("将结果存入变量",new String[]{"a","b","c","x","y","z","m"},i->{String k=new String[]{"a","b","c","x","y","z","m"}[i];variables.put(k,exact);save();toast(k+" = "+exact);});}
    private void recall(){if(variables.isEmpty()){toast("尚未存储变量：SHIFT → RCL 存入");return;}ArrayList<String> names=new ArrayList<>(variables.keySet());String[] labels=new String[names.size()];for(int i=0;i<labels.length;i++)labels[i]=names.get(i)+" = "+variables.get(names.get(i));choose("变量",labels,i->{checkpoint();formula.text(names.get(i));changed();});}
    private void memory(boolean add){computeCurrent(r->task(()->engine.evaluate("("+variables.getOrDefault("m","0")+")"+(add?"+":"-")+"("+r+")",false),v->{variables.put("m",v);save();toast("m = "+v);}));}
    private void menu(){showPanel(true);}
    private void mode(){showPanel(false);}
    private void closePanel(){closePanel(null);}
    private void closePanel(Runnable next){CalculatorPanel closing=panel;if(closing==null){if(next!=null)next.run();return;}closing.dismiss(()->{surface.removeView(closing);if(panel==closing)panel=null;if(next!=null&&!isDestroyed())next.run();});}
    private void showPanel(boolean drawer){if(panel!=null)return;view.cancelTouches();panel=new CalculatorPanel(this,drawer,new CalculatorPanel.Listener(){public void position(float progress){view.drawerProgress=progress;view.postInvalidateOnAnimation();}public void close(){closePanel();}public void selected(String action){if(action.equals("toggleSound")){boolean sound=!prefs.getBoolean("sound",false);prefs.edit().putBoolean("sound",sound).apply();panel.sound=sound;panel.invalidate();applyPreferences();if(!sound)FeedbackPlayer.release();}else closePanel(()->panelAction(action));}});panel.basic=view.basic;panel.sound=prefs.getBoolean("sound",false);surface.addView(panel,new FrameLayout.LayoutParams(-1,-1));}
    private void panelAction(String action){
        if(workbench.handle(action))return;
        if(keyFeatures.handle(action))return;
        if(menuTools.handle(action))return;
        if(action.startsWith("help:")){ReferenceDialogs.builder(this).setTitle("操作帮助").setMessage("选择对应的计算模式，然后填写工具参数。主键直接输入；SHIFT 使用黄色副标，ALPHA 使用紫色副标。方向键进入或退出公式模板。").setPositiveButton("完成",null).show();return;}
        if(action.startsWith("poly:")){workbench.modes.polynomial(Integer.parseInt(action.substring(5)),null);return;}
        if(action.startsWith("inequality:")){String[] a=action.split(":");workbench.modes.polynomial(Integer.parseInt(a[1]),new String[]{">","<",">=","<="}[Integer.parseInt(a[2])]);return;}
        if(action.startsWith("system:")){workbench.modes.system(Integer.parseInt(action.substring(7)));return;}
        switch(action){case "normal":if(view.gridActive)workbench.finishGrid();view.mode="计算";view.invalidate();break;case "graph":openGraph();break;case "bases":bases();break;case "table":table();break;case "calculus":calculus();break;case "programming":programming();break;case "variables":recall();break;case "skin":choose("计算器皮肤",new String[]{"CalcES 参考布局"},i->view.invalidate());break;case "sound":toast("按键振动已启用");break;case "fullKeyboard":view.basic=false;view.invalidate();save();break;case "basicKeyboard":view.basic=true;view.invalidate();save();break;default:press(action);}
    }
    private void polynomial(int degree,String inequality){String[] labels=new String[degree+1],defaults=new String[degree+1];for(int i=0;i<=degree;i++){labels[i]="系数 "+(char)('a'+i)+" · x^"+(degree-i);defaults[i]=i==0?"1":i==degree?"-1":"0";}tool("解"+degree+"次"+(inequality==null?"多项式":"不等式"),labels,defaults,a->{StringBuilder s=new StringBuilder();for(int i=0;i<a.length;i++){if(i>0)s.append('+');s.append('(').append(a[i]).append(")*x^").append(degree-i);}return (inequality==null?"Solve(":"Reduce(")+s+(inequality==null?"==":inequality)+"0,x"+(inequality==null?"":",Reals")+")";});}
    private void capture(){try{android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(view.getWidth(),view.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);view.draw(new android.graphics.Canvas(image));ContentValues values=new ContentValues();values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME,"SciCalc-"+System.currentTimeMillis()+".png");values.put(android.provider.MediaStore.Images.Media.MIME_TYPE,"image/png");values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH,"Pictures/SciCalc");android.net.Uri uri=getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new java.io.IOException("无法创建图片");try(java.io.OutputStream out=getContentResolver().openOutputStream(uri)){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}image.recycle();toast("计算截图已保存到相册");}catch(Exception e){toast("保存失败："+e.getMessage());}}
    private void settings(){choose("设置",new String[]{"角度："+(degrees?"DEG 度":"RAD 弧度"),"结果："+(decimal?"小数":"精确"),"输入任意表达式","清除历史","开源组件"},i->{if(i==0)press("angle");if(i==1)press("decimal");if(i==2)linearInput();if(i==3){history.clear();save();toast("历史已清除");}if(i==4)about();});}
    private void about(){menuTools.handle("features");}
    private LinearLayout form(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);int p=(int)(20*getResources().getDisplayMetrics().density);l.setPadding(p,8,p,8);return l;}
    private EditText field(LinearLayout l,String label,String value){TextView t=new TextView(this);t.setText(label);t.setTextSize(13);t.setTextColor(0xffb4a4d2);l.addView(t);EditText e=new EditText(this);e.setText(value);e.setTextSize(17);e.setSingleLine(false);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);e.setSelectAllOnFocus(true);l.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;}
    private void tool(String title,String[] labels,String[] defaults,Function<String[],String> expression){LinearLayout l=form();EditText[] fields=new EditText[labels.length];for(int i=0;i<fields.length;i++)fields[i]=field(l,labels[i],defaults[i]);ScrollView scroll=new ScrollView(this);scroll.addView(l);AlertDialog d=ReferenceDialogs.builder(this).setTitle(title).setView(scroll).setNegativeButton("返回",null).setPositiveButton("计算",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{String[] a=new String[fields.length];for(int i=0;i<a.length;i++){a[i]=fields[i].getText().toString().trim();if(a[i].isEmpty()){fields[i].setError("请填写");return;}}try{String input=expression.apply(a);d.dismiss();task(()->ToolInput.evaluate(engine,input),r->outcome(title,input,r,labels,a,expression));}catch(Exception ex){toast(ex.getMessage());}}));d.show();}
    private void outcome(String title,String expression,String result,String[] labels,String[] values,Function<String[],String> factory){
        checkpoint();formula.load(result);exact=result;previous=ans;ans=result;view.result=result;view.invalidate();
        try{String[] markers=new String[values.length];for(int i=0;i<markers.length;i++)markers[i]=ToolInput.marker(i);String template="";try{template=factory.apply(markers);}catch(Exception ignored){}
            JSONObject j=new JSONObject();j.put("source",expression).put("tree",formula.save()).put("result",result).put("displayResult",result).put("time",System.currentTimeMillis()).put("mode",title).put("degrees",degrees).put("decimal",false).put("toolTitle",title).put("toolLabels",new JSONArray(Arrays.asList(labels))).put("toolValues",new JSONArray(Arrays.asList(values))).put("toolTemplate",template);history.add(0,j);while(history.size()>historyLimit())history.remove(history.size()-1);
        }catch(Exception ignored){}save();
        ReferenceDialogs.builder(this).setTitle(title).setMessage(ToolInput.readable(title,labels,values)+"\n\n结果：\n"+result).setPositiveButton("完成",null).setNeutralButton("复制",(d,w)->copyResult()).setNegativeButton("使用结果",(d,w)->{checkpoint();formula.load(result);changed();}).show();
    }
    void reopenTool(JSONObject record){
        if(workbench.modes.reopen(record))return;
        JSONArray names=record.optJSONArray("toolLabels"),inputs=record.optJSONArray("toolValues");if(names==null||inputs==null||names.length()!=inputs.length())return;
        String title=record.optString("toolTitle"),template=record.optString("toolTemplate");String[] labels=new String[names.length()],values=new String[names.length()];for(int i=0;i<labels.length;i++){labels[i]=names.optString(i);values[i]=inputs.optString(i);}
        if(title.equals("进制转换"))tool(title,labels,values,a->{int from=Integer.parseInt(a[1]),to=Integer.parseInt(a[2]);if(from<2||from>36||to<2||to>36)throw new IllegalArgumentException("进制范围为 2~36");return "\""+new java.math.BigInteger(a[0],from).toString(to).toUpperCase(Locale.ROOT)+"\"";});
        else if(!template.isEmpty())tool(title,labels,values,a->ToolInput.apply(template,a));
    }
    private void linearInput(){LinearLayout l=form();EditText e=field(l,"表达式，例如 Sin(Pi/6) 或 Solve(x^2=2,x)",formula.source(degrees));ReferenceDialogs.builder(this).setTitle("表达式输入").setView(l).setNegativeButton("取消",null).setPositiveButton("使用",(d,w)->{checkpoint();formula.load(e.getText().toString());changed();}).show();}
    private void applyCurrent(String fn){if(formula.empty()){tool(fn,new String[]{"表达式"},new String[]{"x^2-1"},a->fn+"("+a[0]+")");return;}task(()->engine.evaluate(fn+"("+formula.source(degrees)+")",false),r->{exact=r;view.result=r;view.invalidate();});}
    private void functions(){choose("函数",new String[]{"Abs","Floor","Ceiling","Round","GCD","LCM","Mod","Min","Max","Factorial","Log10","Exp","Sign"},i->{checkpoint();formula.template("func",new String[]{"Abs","Floor","Ceiling","Round","GCD","LCM","Mod","Min","Max","Factorial","Log10","Exp","Sign"}[i]);changed();});}
    private void calculus(){choose("微积分与代数",new String[]{"符号求导","不定积分","定积分","极限","化简","展开","因式分解","求和","连乘"},i->{String[] f={"D","Integrate","Integrate","Limit","FullSimplify","Expand","Factor","Sum","Product"};if(i==0||i==1)tool(i==0?"求导":"不定积分",new String[]{"表达式","变量"},new String[]{"x^2","x"},a->f[i]+"("+a[0]+","+a[1]+")");else if(i==2||i==7||i==8)tool(i==2?"定积分":i==7?"求和":"连乘",new String[]{"表达式","变量","下界","上界"},new String[]{"x^2","x","0",i==2?"1":"10"},a->f[i]+"("+a[0]+",{"+a[1]+","+a[2]+","+a[3]+"})");else if(i==3)press("limit");else tool(f[i],new String[]{"表达式"},new String[]{i==5?"(x+1)^3":"x^2-1"},a->f[i]+"("+a[0]+")");});}
    private void equations(){choose("方程与不等式",new String[]{"多项式 / 方程求解","方程组","不等式","数值求根"},i->{if(i==0)tool("方程求解",new String[]{"方程（等号使用 ==）","未知数"},new String[]{"x^2-5*x+6==0","x"},a->"Solve("+a[0]+","+a[1]+")");if(i==1)tool("方程组",new String[]{"方程列表","未知数列表"},new String[]{"{x+y==3,x-y==1}","{x,y}"},a->"Solve("+a[0]+","+a[1]+")");if(i==2)tool("不等式",new String[]{"不等式","未知数"},new String[]{"x^2-4<0","x"},a->"Reduce("+a[0]+","+a[1]+",Reals)");if(i==3)tool("数值求根",new String[]{"方程","未知数","初始值"},new String[]{"Cos(x)==x","x","0.5"},a->"FindRoot("+a[0]+",{"+a[1]+","+a[2]+"})");});}
    private void matrices(){String[] names={"加法","乘法","行列式","逆矩阵","转置","秩","行化简","特征值","特征向量","线性方程组"};choose("矩阵",names,i->{if(i==0||i==1)tool("矩阵"+names[i],new String[]{"矩阵 A","矩阵 B"},new String[]{"{{1,2},{3,4}}","{{2,0},{0,2}}"},a->i==0?"("+a[0]+")+("+a[1]+")":"Dot("+a[0]+","+a[1]+")");else if(i==9)tool("线性方程组 Ax=b",new String[]{"系数矩阵 A","向量 b"},new String[]{"{{1,1},{1,-1}}","{3,1}"},a->"LinearSolve("+a[0]+","+a[1]+")");else{String fn=new String[]{"","","Det","Inverse","Transpose","MatrixRank","RowReduce","Eigenvalues","Eigenvectors"}[i];tool(names[i],new String[]{"矩阵（每行用花括号）"},new String[]{"{{1,2},{3,4}}"},a->fn+"("+a[0]+")");}});}
    private void vectors(){String[] names={"加法","减法","点积","叉积","模长","单位向量","夹角"};choose("向量",names,i->{if(i==4||i==5)tool(names[i],new String[]{"向量"},new String[]{"{3,4,0}"},a->(i==4?"Norm":"Normalize")+"("+a[0]+")");else tool(names[i],new String[]{"向量 a","向量 b"},new String[]{"{1,0,0}","{0,1,0}"},a->{if(i==0||i==1)return "("+a[0]+")"+(i==0?"+":"-")+"("+a[1]+")";if(i==2||i==3)return (i==2?"Dot":"Cross")+"("+a[0]+","+a[1]+")";return "ArcCos(Dot("+a[0]+","+a[1]+")/(Norm("+a[0]+")*Norm("+a[1]+")))"+(degrees?"*180/Pi":"");});});}
    private void statistics(){String[] n={"均值","中位数","样本方差","样本标准差","最小值","最大值","总和","线性回归"};choose("统计与回归",n,i->{if(i==7)tool("线性回归",new String[]{"数据点 {x,y}"},new String[]{"{{1,2},{2,4},{3,6}}"},a->"Fit("+a[0]+",{1,x},x)");else tool(n[i],new String[]{"数据列表"},new String[]{"{1,2,3,4,5}"},a->new String[]{"Mean","Median","Variance","StandardDeviation","Min","Max","Total"}[i]+"("+a[0]+")");});}
    private void distributions(){choose("概率分布",new String[]{"正态分布密度 PDF","正态分布累积 CDF","正态分布分位数","二项分布概率","泊松分布概率","排列 nPr","组合 nCr"},i->{if(i<=2)tool("正态分布",new String[]{"均值 μ","标准差 σ (>0)",i==2?"概率 p (0~1)":"x"},new String[]{"0","1",i==2?"0.975":"1"},a->new String[]{"PDF","CDF","InverseCDF"}[i]+"(NormalDistribution("+a[0]+","+a[1]+"),"+a[2]+")");if(i==3)tool("二项分布",new String[]{"试验次数 n","成功概率 p","成功次数 k"},new String[]{"10","1/2","5"},a->"PDF(BinomialDistribution("+a[0]+","+a[1]+"),"+a[2]+")");if(i==4)tool("泊松分布",new String[]{"均值 λ","次数 k"},new String[]{"3","2"},a->"PDF(PoissonDistribution("+a[0]+"),"+a[1]+")");if(i==5)press("permutation");if(i==6)press("combination");});}
    private void complex(){choose("复数",new String[]{"复数运算","实部","虚部","模","辐角","共轭","直角坐标 → 极坐标","极坐标 → 直角坐标"},i->{if(i==6){polar();return;}if(i==7){press("rectangular");return;}String fn=new String[]{"","Re","Im","Abs","Arg","Conjugate"}[i];tool("复数 · "+(fn.isEmpty()?"运算":fn),new String[]{"复数表达式（虚数单位 I）"},new String[]{"3+4*I"},a->fn.isEmpty()?a[0]:fn+"("+a[0]+")"+(i==4&&degrees?"*180/Pi":""));});}
    private void polar(){tool("直角坐标 → 极坐标",new String[]{"实部 x","虚部 y"},new String[]{"3","4"},a->"{Sqrt(("+a[0]+")^2+("+a[1]+")^2),ArcTan("+a[0]+","+a[1]+")"+(degrees?"*180/Pi":"")+"}");}
    private void bases(){tool("进制转换",new String[]{"整数","输入进制 (2~36)","输出进制 (2~36)"},new String[]{"255","10","16"},a->{int from=Integer.parseInt(a[1]),to=Integer.parseInt(a[2]);if(from<2||from>36||to<2||to>36)throw new IllegalArgumentException("进制范围为 2~36");String v=new java.math.BigInteger(a[0],from).toString(to).toUpperCase(Locale.ROOT);return "\""+v+"\"";});}
    private void table(){tool("函数数值表",new String[]{"f(x)","起点","终点","步长"},new String[]{"x^2","0","5","1"},a->{double lo=Double.parseDouble(a[1]),hi=Double.parseDouble(a[2]),step=Double.parseDouble(a[3]);if(step==0||!Double.isFinite(step)||(hi-lo)/step<0||(hi-lo)/step>199)throw new IllegalArgumentException("步长须指向终点，最多 200 行");return "Table({x,"+a[0]+"},{x,"+a[1]+","+a[2]+","+a[3]+"})";});}
    private void constants(){String[] labels={"π · 圆周率","e · 自然常数","c · 光速 (m/s)","h · 普朗克常数 (J·s)","kB · 玻尔兹曼常数 (J/K)","NA · 阿伏伽德罗常数 (1/mol)","g₀ · 标准重力 (m/s²)"};String[] values={"π","E","299792458","6.62607015*10^(-34)","1.380649*10^(-23)","6.02214076*10^(23)","9.80665"};choose("常数 · 点击插入",labels,i->{checkpoint();formula.text(values[i]);changed();});}
    private void programming(){tool("数学脚本",new String[]{"Symja 表达式 / 脚本（分号分隔，末句返回结果）"},new String[]{"Table(n^2,{n,1,10})"},a->a[0]);}
    private void openGraph(){graph=new GraphView(this,()->{graph=null;setContentView(surface);updateSystemBars();});setContentView(graph);updateSystemBars();}
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        int action=e.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN)dismissingPopupTouch=false;
        if(dismissingPopupTouch){if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)dismissingPopupTouch=false;return true;}
        if(action==MotionEvent.ACTION_DOWN&&toolOverlay!=null&&toolOverlay.dismissOnOutside){
            android.graphics.Rect bounds=new android.graphics.Rect();toolOverlay.getGlobalVisibleRect(bounds);
            if(!bounds.contains((int)e.getRawX(),(int)e.getRawY())){
                // Swallow the gesture so dismissal cannot also activate the
                // underlying keyboard or move the formula's cursor.
                dismissingPopupTouch=true;view.cancelTouches();menuTools.home();return true;
            }
        }
        if(action==MotionEvent.ACTION_DOWN&&panel==null&&toolOverlay==null&&graph==null&&surface!=null&&surface.getParent()!=null&&e.getX()<20*getResources().getDisplayMetrics().density&&e.getY()>78*view.getHeight()/2400f){if(edgeDown!=null)edgeDown.recycle();edgeDown=MotionEvent.obtain(e);edgeOpening=false;}
        if(edgeDown!=null){
            if(!edgeOpening&&action==MotionEvent.ACTION_MOVE){float dx=e.getX()-edgeDown.getX(),dy=e.getY()-edgeDown.getY();if(dx>ViewConfiguration.get(this).getScaledTouchSlop()&&dx>Math.abs(dy)){
                MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();showPanel(true);panel.prepareDrag();panel.dispatchTouchEvent(edgeDown);edgeOpening=true;
            }}
            if(edgeOpening&&panel!=null)panel.dispatchTouchEvent(e);
            boolean consumed=edgeOpening;if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL||action==MotionEvent.ACTION_POINTER_DOWN&&!edgeOpening){edgeDown.recycle();edgeDown=null;edgeOpening=false;}if(consumed)return true;
        }
        return super.dispatchTouchEvent(e);
    }
    @Override public void onBackPressed(){if(panel!=null)closePanel();else if(menuTools!=null&&menuTools.isOpen())menuTools.back();else if(view.gridActive)press("gridDone");else if(graph!=null){graph.saveState();graph=null;setContentView(surface);updateSystemBars();}else {if(prefs.getBoolean("clearOnExit",false)){formula.clear();view.tape.clear();view.result="";exact="";save();}super.onBackPressed();}}
}
