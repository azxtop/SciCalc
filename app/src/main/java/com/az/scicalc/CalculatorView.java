package com.az.scicalc;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.OverScroller;
import java.util.*;

/** Geometry measured from the 1080 x 2400 reference viewport. */
public final class CalculatorView extends View {
 // Reference geometry after its top placeholder has finished measuring.
 // The reclaimed top space remains inside our display, as requested.
 private static final float KEYBOARD_SCALE_Y=.92375f,KEYBOARD_OFFSET_Y=179.7338f,TOOLBAR_OFFSET_Y=121f;
 private static final float DISPLAY_BOTTOM=772,DISPLAY_CLIP_BOTTOM=767;
 public interface Listener {void press(String action);void copyResult();}
 public final Formula editor;
 public String result="",message="",mode="计算",angle="DEG";
 public boolean skinPreview=false;
 public float drawerProgress=0;public float resultMenuTop=400;public float contextMenuTop=240;private float maxScroll=0;
 public boolean shift=false,alpha=false,approximate=false,busy=false,basic=false;
 public String busyLabel="计算中…";
 public boolean second=false,displayMono=true,gridActive=false;
 public boolean blinkCursor=true,divisionSlash=false,multiplicationStar=false,groupThousands=true,groupDecimals=true,decimalComma=false,fractionMultiply=true,descendingPowers=false,showCalcInfo=true,relativePercent=true,implicitPriority=true;
 public boolean baseMode=false;public int baseRadix=10,baseBits=32,workflowStep=-1;public String workflowCaption="",coefficientTitle="";
 public int ansStyle=2,keyLabelStyle=0;public float keyFontScale=1;
 public String memoryState="",prompt="",formatLabel="NORM",gridTitle="",search="";
 public float displayScale=1;public int digits=11,gridRow=0,gridCol=0;
 private float headerOffset=0;
 void setTabHeaderPixels(float bottom,int width,int height){setTabHeader(bottom/outputScaleFor(width,height));}
 static float outputScaleFor(int width,int height){return width>height?Math.min(width/2160f,height/1080f):height/2400f;}
 private float outputRight(float inset){return outputWidth-inset;}
 private float outputClipBottom(){return outputBottom-5;}
 private float touchContentTop(){return contentTop()*outputScale/sy;}
 private float touchOutputBottom(){return outputBottom*outputScale/sy;}
 private float touchClipBottom(){return outputClipBottom()*outputScale/sy;}
 void setTabHeader(float bottom){float next=Math.max(0,bottom-60);if(next!=headerOffset){headerOffset=next;invalidate();}}
 public String[][] grid=new String[0][];public String[] gridHeaders=new String[0];
 static final class TapeLine {
  String tree,exact,display;final Formula formula=new Formula();String layoutKey="";Picture picture;RenderNode node;float height,inputHeight;
  TapeLine(String t,String e,String d){tree=t;exact=e;display=d;formula.restore(t);}
 }
 final ArrayList<TapeLine> tape=new ArrayList<>();
 private final ArrayList<Key> secondKeys=new ArrayList<>(),baseKeys=new ArrayList<>();
 private final HashMap<RectF,String> screenActions=new LinkedHashMap<>();
 private float scroll=0,touchY,startY,startX;private boolean dragged=false,panelGesture=false,verticalDrag=false;
 private final android.util.SparseArray<KeyTouch> keyTouches=new android.util.SparseArray<>();
 private int gesturePointer=-1;private String gestureTarget="";private boolean contextOpened;
 private final Runnable panelHold=this::openPanelContext;
 private void openPanelContext(){if(gesturePointer<0||dragged||!panelGesture)return;String action=gestureTarget.startsWith("tapeedit:")?"inputcontext:"+gestureTarget.substring(9):gestureTarget.startsWith("resultmenu")?gestureTarget.replace("resultmenu","outputcontext"):"inputcontext";if(!gestureTarget.isEmpty()&&!gestureTarget.startsWith("tapeedit:")&&!gestureTarget.startsWith("resultmenu"))return;contextOpened=true;listener.press(action);}
 boolean inputActive=true;
 private final OverScroller scroller;private VelocityTracker velocity;private final int touchSlop,minVelocity,maxVelocity;
 private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());
 private final android.os.Handler blinkTimer=new android.os.Handler(android.os.Looper.getMainLooper());
 private final Runnable cursorBlink=new Runnable(){public void run(){if(blinkCursor&&inputActive)postInvalidateOnAnimation();long phase=System.currentTimeMillis()%1000;blinkTimer.postDelayed(this,phase<600?600-phase:1000-phase);}};
 private final android.util.LruCache<String,Formula.Row> outputRows=new android.util.LruCache<>(40);
 void addTape(String tree,String exact,String display){resultFeedback(true);tape.add(new TapeLine(tree,exact,display));while(tape.size()>30)tape.remove(0);stopScroll();scroll=0;inputActive=false;}
 void clearScreen(){stopScroll();tape.clear();scroll=0;inputActive=true;invalidate();}
 void updateLastDisplay(String value){if(!inputActive&&editor.empty()&&!tape.isEmpty()){TapeLine last=tape.get(tape.size()-1);last.display=value;last.layoutKey="";}invalidate();}
 void editing(){stopScroll();scroll=0;inputActive=true;}
 private void stopScroll(){scroller.forceFinished(true);}
 void freezeScroll(){stopScroll();}
 String saveTape(){org.json.JSONArray a=new org.json.JSONArray();try{for(TapeLine l:tape)a.put(new org.json.JSONObject().put("tree",l.tree).put("exact",l.exact).put("display",l.display));}catch(Exception ignored){}return a.toString();}
 void restoreTape(String source){try{org.json.JSONArray a=new org.json.JSONArray(source);for(int i=0;i<a.length();i++){org.json.JSONObject j=a.getJSONObject(i);tape.add(new TapeLine(j.getString("tree"),j.getString("exact"),j.getString("display")));}}catch(Exception ignored){}}
 org.json.JSONObject savePage(){
  org.json.JSONObject j=new org.json.JSONObject();try{j.put("tape",saveTape()).put("result",result).put("message",message).put("mode",mode).put("inputActive",inputActive).put("scroll",scroll).put("basic",basic).put("second",second).put("shift",shift).put("alpha",alpha).put("gridActive",gridActive).put("grid",new org.json.JSONArray(Arrays.asList(grid))).put("headers",new org.json.JSONArray(Arrays.asList(gridHeaders))).put("gridTitle",gridTitle).put("gridRow",gridRow).put("gridCol",gridCol).put("baseMode",baseMode).put("baseRadix",baseRadix).put("baseBits",baseBits).put("workflowStep",workflowStep).put("workflowCaption",workflowCaption).put("coefficientTitle",coefficientTitle).put("memoryState",memoryState).put("prompt",prompt).put("busy",busy);}catch(Exception ignored){}return j;
 }
 void restorePage(org.json.JSONObject j){
  if(j==null)return;try{restoreTape(j.optString("tape","[]"));result=j.optString("result");message=j.optString("message");mode=j.optString("mode","计算");inputActive=j.optBoolean("inputActive",true);scroll=Math.max(0,(float)j.optDouble("scroll"));basic=j.optBoolean("basic");second=j.optBoolean("second");shift=j.optBoolean("shift");alpha=j.optBoolean("alpha");gridActive=j.optBoolean("gridActive");org.json.JSONArray rows=j.optJSONArray("grid");if(rows!=null){grid=new String[rows.length()][];for(int r=0;r<rows.length();r++){org.json.JSONArray row=rows.getJSONArray(r);grid[r]=new String[row.length()];for(int col=0;col<row.length();col++)grid[r][col]=row.optString(col);}}org.json.JSONArray headers=j.optJSONArray("headers");if(headers!=null){gridHeaders=new String[headers.length()];for(int i=0;i<headers.length();i++)gridHeaders[i]=headers.optString(i);}gridTitle=j.optString("gridTitle");gridRow=Math.max(0,Math.min(Math.max(0,grid.length-1),j.optInt("gridRow")));gridCol=Math.max(0,Math.min(Math.max(0,gridHeaders.length-1),j.optInt("gridCol")));baseMode=j.optBoolean("baseMode");baseRadix=j.optInt("baseRadix",10);baseBits=j.optInt("baseBits",32);workflowStep=j.optInt("workflowStep",-1);workflowCaption=j.optString("workflowCaption");coefficientTitle=j.optString("coefficientTitle");memoryState=j.optString("memoryState");prompt=j.optString("prompt");if(j.optBoolean("busy")){busy=false;message="上次计算未完成，按 = 重新计算";}}catch(Exception ignored){}
 }
 int[] tabColors(){return new int[]{ink(bg),theme==8?Color.BLACK:0xffdbdcd8,ink(0xff504d53)};}
 private float contentTop(){return skinPreview?132:landscape?Math.min(outputBottom-120,132+headerOffset):Math.max(254,132+headerOffset);}
 private final Paint p=new Paint(3);private final List<Key> keys=new ArrayList<>(),basicKeys=new ArrayList<>();
 private final Formula.Renderer renderer=new Formula.Renderer();private final Picture cursorLayoutProbe=new Picture();private final Listener listener;
 private final RenderNode keyboardNode=new RenderNode("calculator-keyboard");private String keyboardLayout="";private boolean recordingKeyboard;
 private boolean landscape;private float outputScale=1,outputWidth=1080,outputBottom=772;private float sx=1,sy=1,formulaX=24;private long downTime;private Bitmap texture;
 private int theme=0;private boolean sound=false,vibration=true;
 public static final String[] THEMES={"Calc350es","Retro","深海","森林","暖棕","石墨","暮蓝","莓酒","米白","黑铜"};
 private static final int[][] PALETTES={{0xff16191c,0xffd4e4e3,0xff504d53,0xff272b31,0xffe78e51,0xfffdc12b,0xff7066ad},{0xff39382f,0xffc6d2a9,0xff686354,0xff353630,0xffe59c64,0xffe6c77a,0xff8a7c6b},{0xff102331,0xffd1e6ee,0xff3c6078,0xff20394c,0xffefab63,0xffffd45c,0xff616fc0},{0xff162820,0xffd7e7c9,0xff49634f,0xff233d32,0xffe8a16a,0xffdcc662,0xff688f83},{0xff35251e,0xffeedccb,0xff796254,0xff47362d,0xffe79465,0xffecc576,0xffad8175},{0xff1b1b20,0xffdedee4,0xff555560,0xff2d2d36,0xffdda0ae,0xffe0cf87,0xff8884ad},{0xff101a2c,0xffdbe5ef,0xff33465e,0xff1e2d42,0xffe6a66c,0xffdac274,0xff728eb5},{0xff261b25,0xfff0e0e8,0xff624451,0xff392937,0xffcb8871,0xffe3bd78,0xffac86a2},{0xffe6e3db,0xfff6f4ee,0xffd2cec5,0xfff0ede5,0xffca704c,0xffd8b765,0xffb4abb9},{0xff151516,0xffe5e0d6,0xff3c3936,0xff232221,0xffbe8054,0xffcbb17b,0xff8f8b84}};
 public void setTheme(int n){int next=Math.max(0,Math.min(PALETTES.length-1,n));if(theme!=next){theme=next;texture=null;invalidate();}}
 public void setFeedback(boolean s,boolean v){sound=s;vibration=v;}
 private int ink(int color){if(theme>=6){if(color==yellow)return theme==8?0xff886427:PALETTES[theme][5];if(color==purple)return theme==8?0xff71556d:PALETTES[theme][6];if(color==0xff282a2c)return PALETTES[theme][3];if(theme==8&&(color==0xfff6f9f6||color==0xffe2e2e0||color==0xffdbdcd8))return 0xff292b2d;}int[] original=PALETTES[0];for(int i=0;i<original.length;i++)if(color==original[i])return PALETTES[theme][i];return color;}
 private void feedback(String action){FeedbackPlayer.play(this,action,sound,vibration);}
 void resultFeedback(boolean success){FeedbackPlayer.play(this,success?"result":"error",sound,vibration);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();blinkTimer.post(cursorBlink);}
 @Override protected void onDetachedFromWindow(){cancelTouches();blinkTimer.removeCallbacksAndMessages(null);stopScroll();releaseVelocity();FeedbackPlayer.release();super.onDetachedFromWindow();}
 private final int bg=0xff16191c,panel=0xffd4e4e3,yellow=0xffffd600,purple=0xffb488ff,orange=0xffe78e51;
 private static final Typeface MONO=UiFonts.MONO,BOLD=UiFonts.MONO_BOLD,SANS=UiFonts.REGULAR;
 private static final class Key {
  final RectF r,hitRect,hitModelRect;final String label,action,secondary,alt,tertiary,alphaAction;final int fill;final boolean toolbar;
  final float yScale,yOffset;
  Path outline,shadow,highlight;Shader shine;
  android.graphics.drawable.RippleDrawable ripple;
  long rippleUntil;
  Key(float x,float y,float w,float h,String l,String a,String s,String b,String t,String aa,int f,boolean bar){
   r=new RectF(x,y,x+w,y+h);label=l;action=a;secondary=s;alt=b;tertiary=t;alphaAction=aa;fill=f;toolbar=bar;
   hitRect=hitBounds(r,bar,a,bar?1:KEYBOARD_SCALE_Y,bar?TOOLBAR_OFFSET_Y:KEYBOARD_OFFSET_Y);
   // Native rows round their weighted heights separately. A single affine
   // mapping accumulates two-pixel errors in the lower rows.
   if(bar){yScale=1;yOffset=TOOLBAR_OFFSET_Y;}
   else if(a.equals("dpad")){yScale=251f/h;yOffset=890-y*yScale;}
   else if(referenceRow(y)){yScale=(hitRect.height()-6)/h;yOffset=hitRect.top+3-y*yScale;}
   else {yScale=KEYBOARD_SCALE_Y;yOffset=KEYBOARD_OFFSET_Y;}
   hitModelRect=new RectF(hitRect.left,modelY(hitRect.top),hitRect.right,modelY(hitRect.bottom));
  }
  float modelY(float y){return (y-yOffset)/yScale;}
 }
 private static RectF hitBounds(RectF drawing,boolean toolbar,String action,float scale,float offset){
  if(action.equals("dpad"))return new RectF(360,880,720,1150);
  if(toolbar)return new RectF(drawing.left,786,drawing.right+3,870);
  // Reference View bounds include background insets beyond the visible key.
  // Keep those rectangular targets independent of the rounded drawing path.
  float top=(float)Math.floor(offset+drawing.top*scale)-3,bottom=(float)Math.ceil(offset+drawing.bottom*scale)+3;
  switch(Math.round(drawing.top)){
   case 762:top=880;bottom=989;break;
   case 935:top=1040;bottom=1150;break;
   case 1109:top=1201;bottom=1310;break;
   case 1283:top=1361;bottom=1471;break;
   case 1457:top=1523;bottom=1634;break;
   case 1635:top=1688;bottom=1814;break;
   case 1830:top=1868;bottom=1995;break;
   case 2026:top=2049;bottom=2176;break;
   case 2222:top=2230;bottom=2358;break;
  }
  return new RectF(drawing.left-8,top,drawing.right+8,bottom);
 }
 private static boolean referenceRow(float y){switch(Math.round(y)){case 762:case 935:case 1109:case 1283:case 1457:case 1635:case 1830:case 2026:case 2222:return true;default:return false;}}
 public CalculatorView(Context c,Formula e,Listener l){super(c);editor=e;listener=l;scroller=new OverScroller(c);ViewConfiguration config=ViewConfiguration.get(c);touchSlop=config.getScaledTouchSlop();minVelocity=config.getScaledMinimumFlingVelocity();maxVelocity=config.getScaledMaximumFlingVelocity();setFocusable(true);setContentDescription("科学计算器，完整键盘与结构化公式");layoutKeys();}
 @Override protected void onSizeChanged(int w,int h,int ow,int oh){cancelTouches();sx=w/1080f;sy=h/2400f;landscape=!skinPreview&&w>h;outputScale=landscape?outputScaleFor(w,h):sy;outputWidth=landscape?w/outputScale:1080;outputBottom=landscape?h*.38f/outputScale:772;keyboardLayout="";}
 private void key(float x,float y,float w,float h,String label,String action,String sec,String alt,String third,String aa,int color,boolean bar){keys.add(new Key(x,y,w,h,label,action,sec,alt,third,aa,color,bar));}
 private void simple(float x,float y,float w,float h,String label,String action,int color,boolean bar){key(x,y,w,h,label,action,"","","","",color,bar);}
 private void layoutKeys(){
  float[] x={10,146,253,360,467,947},width={123,94,94,94,94,117};
  String[] icon={"menu","sigma","gear","sign","camera","DEG"},actions={"menu","history","settings","font","ocr","angle"};
  for(int i=0;i<x.length;i++)simple(x[i],666,width[i],81,icon[i],actions[i],0xff282a2c,true);
  simple(13,762,154,112,"SHIFT","shift",0xfffdc12b,false);simple(193,762,154,112,"ALPHA","alpha",0xff7066ad,false);
  simple(733,762,154,112,"MODE","mode",0xff504d53,false);simple(913,762,154,112,"2nd","second",0xff504d53,false);
  simple(789,666,146,81,"MORE","more",0xff282a2c,true);
  key(13,935,154,112,"CALC","calc","SOLVE","solve","=","text:=",0xff504d53,false);
  key(193,935,154,112,"integral","integralMenu","d/dx","derivativeMenu",";","text:;",0xff504d53,false);
  key(733,935,154,112,"reciprocal","reciprocal","x!","factorial","","",0xff504d53,false);
  key(913,935,154,112,"logbase","logbase","∑","tpl:sum","Π","product",0xff504d53,false);
  simple(369,769,342,271,"","dpad",0xff504d53,false);
  String[][] label={{"fraction","sqrt","square","power","Log","Ln"},{"(−)","° ′ ″","hyp","Sin","Cos","Tan"},{"RCL","ENG","(",")","S⇌D","M+"}};
  String[][] action={{"tpl:frac","tpl:sqrt","square","tpl:pow","log10","func:Log"},{"text:−","dms","hyp","func:Sin","func:Cos","func:Tan"},{"recall","engineering","text:(","text:)","decimal","memoryAdd"}};
  String[][] sec={{"fraction","³√x","x³","ˣ√y","10ˣ","eˣ"},{"∠","FACT","|x|","Sin⁻¹","Cos⁻¹","Tan⁻¹"},{"STO","i","%",";","polar","M−"}};
  String[][] alt={{"mixed","cuberoot","cube","nthroot","tenpower","expPower"},{"polarSymbol","factor","func:Abs","func:ArcSin","func:ArcCos","func:ArcTan"},{"store","symbol:I","percent","text:,","mixedFormat","memorySub"}};
  sec[2][3]=",";
  String[][] third={{"÷R","mod","▰","","","t"},{"a","b","c","d","e","f"},{"CLRv","Cot","Cot⁻¹","x","y","m"}};
  String[][] aa={{"remainder","modulo","recurring","","","var:t"},{"var:a","var:b","var:c","var:d","var:e","var:f"},{"clearVars","func:Cot","func:ArcCot","var:x","var:y","var:m"}};
  for(int row=0;row<3;row++)for(int col=0;col<6;col++)key(13+180*col,1109+174*row,154,112,label[row][col],action[row][col],sec[row][col],alt[row][col],third[row][col],aa[row][col],0xff504d53,false);
  String[][] digits={{"7","8","9","delete","AC"},{"4","5","6","×","÷"},{"1","2","3","+","−"},{"0",".","Exp","Ans","="}};
  String[][] a2={{"text:7","text:8","text:9","delete","clear"},{"text:4","text:5","text:6","text:×","text:÷"},{"text:1","text:2","text:3","text:+","text:−"},{"text:0","text:.","scientific","answer","evaluate"}};
  String[][] second={{"CONST","","Limit","","CLR ALL"},{"MATRIX","VECTOR","FUNC","nPr","nCr"},{"STAT","CMPLX","DISTR","Pol","Rec"},{"COPY","Ran#","π","","History"}};
  String[][] shifted={{"constants","","limit","delete","reset"},{"matrixShortcut","vectorShortcut","functions","permutation","combination"},{"statsShortcut","complex","distribution","polar","rectangular"},{"copy","random","symbol:Pi","answer","history"}};
  String[][] thirds={{"","","∞","",""},{"matrixGrid","","HELP","GCD","LCM"},{"","","","Ceil","Floor"},{"PASTE","RanInt","e","PreAns",""}};
  String[][] a3={{"","","symbol:Infinity","",""},{"matrixGrid","","help","gcd","lcm"},{"","","","func:Ceiling","func:Floor"},{"paste","randomInteger","symbol:E","previous",""}};
  int[] numericY={1635,1830,2026,2222};for(int row=0;row<4;row++)for(int col=0;col<5;col++)key(13+216*col,numericY[row],190,row==3?133:131,digits[row][col],a2[row][col],second[row][col],shifted[row][col],thirds[row][col],a3[row][col],row==0&&col>2?orange:0xff272b31,false);
  for(Key k:keys)if(k.toolbar)basicKeys.add(k);
  layoutSecond();layoutBase();
  String[][] bl={{"reciprocal","◀","▶","delete","AC"},{"fraction","power","sqrt","FACT","MC"},{"(",")","M+","M−","MR"}};
  String[][] ba={{"reciprocal","left","right","delete","clear"},{"tpl:frac","tpl:pow","tpl:sqrt","factor","memoryClear"},{"text:(","text:)","memoryAdd","memorySub","memoryRecall"}};
  for(int row=0;row<3;row++)for(int col=0;col<5;col++)basicKeys.add(new Key(13+216*col,821+180*row,194,114,bl[row][col],ba[row][col],"","","","",row==0&&col>=3?orange:0xff504d53,false));
  String[][] bn={{"7","8","9","÷"},{"4","5","6","×"},{"1","2","3","−"},{"%","0",".","+"},{"S⇌D","Exp","Ans","="}};
  for(int row=0;row<5;row++)for(int col=0;col<4;col++){String a=bn[row][col];basicKeys.add(new Key(13+270*col,1362+212*row,246,146,a,a.equals("=")?"evaluate":a.equals("Ans")?"answer":a.equals("S⇌D")?"decimal":a.equals("Exp")?"scientific":a.equals("%")?"text:/100":"text:"+a,"","","","",0xff272b31,false));}
 }
 private void secondKey(int row,int col,String label,String action,String sec,String alt,String third,String aa){secondKeys.add(new Key(13+180*col,935+174*row,154,112,label,action,sec,alt,third,aa,0xff504d53,false));}
 private void layoutSecond(){
  for(Key k:keys)if(k.toolbar||k.action.equals("shift")||k.action.equals("alpha"))secondKeys.add(k);
  secondKeys.add(new Key(373,762,154,112,"◀","left","","","","",0xff504d53,false));secondKeys.add(new Key(553,762,154,112,"▶","right","","","","",0xff504d53,false));secondKeys.add(new Key(913,762,154,112,"1st","second","","","","",0xff504d53,false));
  String[] top={"FACTOR","EXPAND","SIMPLY","GRAPH","∫","d/dx"},ta={"factorAlgebra","expand","simplify","graphCurrent","template:indefinite","template:derivative"};for(int c=0;c<6;c++)secondKey(0,c,top[c],ta[c],"","","","");
  String[][] row={{"r","u","x","{","}",","},{"=","<",">","True","→",":="},{"matrixGrid","•","Det","Rank","inverseMatrix","EiValues"}};
  String[][] act={{"var:r","var:u","var:x","text:{","text:}","text:,"},{"token:==","token:<","token:>","symbol:True","token:->","token::="},{"matrixGrid","binary:dot","call:Det:1","call:MatrixRank:1","call:Inverse:1","call:Eigenvalues:1"}};
  String[][] s={{"s","v","y","(",")",""},{"≠","≤","≥","False","∈",""},{"v2","Cross","Transpose","Trace","RowReduce","EiVectors"}};
  String[][] sa={{"var:s","var:v","var:y","text:(","text:)",""},{"token:!=","token:<=","token:>=","symbol:False","call:Element:2",""},{"vectorGrid:2","call:Cross:2","call:Transpose:1","call:Tr:1","call:RowReduce:1","call:Eigenvectors:1"}};
  String[][] t={{"t","w","z","","",""},{"and","or","xor","¬","∉",""},{"v3","","","","",""}};
  String[][] aa={{"var:t","var:w","var:z","","",""},{"token:&&","token:||","binary:xor","call:Not:1","call:NotElement:2",""},{"vectorGrid:3","","","","",""}};
  for(int r=0;r<3;r++)for(int c=0;c<6;c++)secondKey(r+1,c,row[r][c],act[r][c],s[r][c],sa[r][c],t[r][c],aa[r][c]);
  secondKeys.add(new Key(13,1635,622,131,"搜索函数","searchFunctions","","","","",0xff272b31,false));secondKeys.add(new Key(661,1635,190,131,"delete","delete","","","","",orange,false));secondKeys.add(new Key(877,1635,190,131,"CLR","clear","","","","",orange,false));
  String[][] cats={{"arith","algebra","solver","calculus"},{"list","linear algebra","combinatorics","stat dist"},{"number","boolean","special","others"}};String[][] shifts={{"constant","domain","",""},{"","clustering","","random"},{"","predicate","",""}};for(int r=0;r<3;r++)for(int c=0;c<4;c++)secondKeys.add(new Key(13+270*c,1830+196*r,246,131,cats[r][c],"category:"+cats[r][c],shifts[r][c],shifts[r][c].equals("constant")?"constants":shifts[r][c].isEmpty()?"":"category:"+shifts[r][c],"","",0xff272b31,false));
 }
 private void layoutBase(){
  for(Key k:keys)if(k.toolbar||k.action.equals("shift")||k.action.equals("dpad")||k.action.equals("mode"))baseKeys.add(k);
  baseKeys.add(new Key(193,762,154,112,"img","copy","","","","",0xff7066ad,false));baseKeys.add(new Key(913,762,154,112,"","none","","","","",0xff504d53,false));
  String[] labels={"AND","OR","XOR","NOT"},actions={" and "," or "," xor ","not("},secondary={"NAND","NOR","XNOR",""},shifted={" nand "," nor "," xnor ",""};int[] xs={13,193,733,913};
  for(int c=0;c<4;c++)baseKeys.add(new Key(xs[c],935,154,112,labels[c],"baseText:"+actions[c],secondary[c],"baseText:"+shifted[c],"","",0xff504d53,false));
  String[][] rows={{"power","sqrt","DEC","HEX","BIN","OCT"},{"A","B","C","D","E","F"},{"RoL","RoR","(",")","<<",">>"}};
  String[][] act={{"**","Sqrt(","10","16","2","8"},{"A","B","C","D","E","F"},{"RoL(","RoR(","(",")","<<",">>"}};
  String[][] sec={{"x²","³√x","","","",""},{"x³","ˣ√y","|x|","","",""},{"","","",",","",">>>"}};
  String[][] alt={{"**2","Cbrt(","","","",""},{"**3","Root(","Abs(","","",""},{"","","",",","",">>>"}};
  for(int r=0;r<3;r++)for(int c=0;c<6;c++)baseKeys.add(new Key(13+180*c,1109+174*r,154,112,rows[r][c],r==0&&c>=2?"baseRadix:"+act[r][c]:"baseText:"+act[r][c],sec[r][c],alt[r][c].isEmpty()?"":"baseText:"+alt[r][c],"","",0xff504d53,false));
  for(Key k:keys)if(k.r.top>=1635){String label=k.label,secondaryLabel="",shiftAction="";if(k.label.equals("Exp")){label="RandInt";}if(k.label.equals("4")){secondaryLabel="64bit";shiftAction="baseBits:64";}if(k.label.equals("1")||k.label.equals("2")||k.label.equals("3")){int bits=k.label.equals("1")?8:k.label.equals("2")?16:32;secondaryLabel=bits+"bit";shiftAction="baseBits:"+bits;}if(k.label.equals("6")){secondaryLabel="mod";shiftAction="baseText: mod ";}if(k.label.equals("×")){secondaryLabel="GCD";shiftAction="baseText:GCD(";}if(k.label.equals("÷")){secondaryLabel="LCM";shiftAction="baseText:LCM(";}if(k.label.equals("+")){secondaryLabel="Ceil";shiftAction="baseText:Ceil(";}if(k.label.equals("−")){secondaryLabel="Floor";shiftAction="baseText:Floor(";}String action=k.label.equals("Exp")?"baseText:RandInt(":k.action;baseKeys.add(new Key(k.r.left,k.r.top,k.r.width(),k.r.height(),label,action,secondaryLabel,shiftAction,"","",k.fill,false));}
 }
 private List<Key> activeKeys(){return baseMode?baseKeys:basic?basicKeys:second?secondKeys:keys;}
 private boolean visible(Key k){if(skinPreview&&k.action.equals("more"))return false;return !k.action.equals("more")||gridActive&&mode.equals("SOLVE")||!gridActive&&((!editor.empty()&&editor.complete())||!inputActive&&!tape.isEmpty());}
 private void text(Canvas c,String s,float x,float y,float z,int color,Paint.Align align,Typeface face){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(ink(color));p.setTextAlign(align);p.setTextSize(z);p.setTypeface(face);c.drawText(s,x,y,p);}
 private void fill(Canvas c,RectF r,float radius,int color){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(ink(color));c.drawRoundRect(r,radius,radius,p);}
 private Path keyOutline(RectF r,float top,float bottom){Path shape=new Path();shape.addRoundRect(r,new float[]{top,top,top,top,bottom,bottom,bottom,bottom},Path.Direction.CW);return shape;}
 private void drawKey(Canvas c,Key k){
  // CalcES keys have tight upper corners and wider lower corners.
  float top=k.toolbar?k.r.height()/2:k.action.equals("dpad")?87:20;
  float bottom=k.toolbar?top:k.action.equals("dpad")?87:36;
  if(k.outline==null){k.outline=keyOutline(k.r,top,bottom);k.shadow=keyOutline(new RectF(k.r.left-3,k.r.top-2,k.r.right+3,k.r.bottom+4),top+3,bottom+3);k.highlight=keyOutline(new RectF(k.r.left+1,k.r.top+2,k.r.right-1,k.r.bottom-1),top-1,bottom-1);int highlight=k.fill==0xff272b31?0xff484a5a:0xff8e8794;k.shine=new LinearGradient(0,k.r.top+3,0,k.r.top+25,new int[]{highlight,highlight&0x00ffffff},null,Shader.TileMode.CLAMP);}
  p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xff0f0c0d);
  c.drawPath(k.shadow,p);
  Path outline=k.outline;p.setColor(ink(k.fill));c.drawPath(outline,p);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(0xff0f0c0d);c.drawPath(outline,p);
  if(k.toolbar){p.setStrokeWidth(3);p.setColor(0xff515355);c.drawPath(keyOutline(new RectF(k.r.left+1,k.r.top+1,k.r.right-1,k.r.bottom-1),top,bottom),p);}
  else {
   p.setColor(Color.WHITE);p.setStrokeWidth(2);
   p.setShader(k.shine);c.drawPath(k.highlight,p);
  }
  p.setShader(null);p.setStyle(Paint.Style.FILL);
  if(!recordingKeyboard&&k.ripple!=null){c.save();c.clipPath(k.outline);k.ripple.draw(c);c.restore();}
 }
 private void line(Canvas c,float x,float y,float xx,float yy,int color,float width){p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,xx,yy,p);p.setStyle(Paint.Style.FILL);}
 private void background(Canvas c){
  if(texture==null){texture=Bitmap.createBitmap(12,12,Bitmap.Config.ARGB_8888);Canvas tile=new Canvas(texture);tile.drawColor(ink(bg));Paint dot=new Paint(3);dot.setColor(theme==8?0xffd9d5cc:0xff050707);tile.drawOval(1,1,4,3,dot);tile.drawOval(7,7,10,9,dot);dot.setColor(theme==8?0xffeeeae2:0xff25292b);tile.drawOval(1,3,4,4,dot);tile.drawOval(7,9,10,10,dot);}
  p.setShader(new BitmapShader(texture,Shader.TileMode.REPEAT,Shader.TileMode.REPEAT));p.setStyle(Paint.Style.FILL);c.drawRect(0,0,1080,2400,p);p.setShader(null);
 }
 @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.save();canvas.scale(sx,sy);Canvas c=canvas;background(c);canvas.restore();canvas.save();canvas.scale(landscape?outputScale:sx,outputScale);
  float header=skinPreview?0:headerOffset;c.save();c.translate(0,header);
  float statusBaseline=skinPreview?106:100,statusBottom=skinPreview?128:120;
  text(c,"S",24,statusBaseline,32,shift?yellow:0xff282c2e,Paint.Align.LEFT,BOLD);text(c,"A",69,statusBaseline,32,alpha?purple:0xff282c2e,Paint.Align.LEFT,BOLD);
  screenActions.clear();text(c,"STO",112,statusBaseline,32,memoryState.equals("STO")?yellow:0xff282c2e,Paint.Align.LEFT,BOLD);text(c,"RCL",183,statusBaseline,32,memoryState.equals("RCL")?yellow:0xff282c2e,Paint.Align.LEFT,BOLD);text(c,formatLabel+"-"+digits+" "+(mode.equals("计算")?"MATH":mode)+" "+(approximate?"DECI":"FRAC"),262,statusBaseline,32,0xffdbdcd8,Paint.Align.LEFT,BOLD);
  screenActions.put(new RectF(12,60,62,statusBottom),"shift");screenActions.put(new RectF(62,60,109,statusBottom),"alpha");screenActions.put(new RectF(110,60,180,statusBottom),"store");screenActions.put(new RectF(180,60,257,statusBottom),"recall");screenActions.put(new RectF(260,60,452,statusBottom),"formatStatus");screenActions.put(new RectF(453,60,551,statusBottom),"modeStatus");screenActions.put(new RectF(552,60,665,statusBottom),"decimal");
  c.restore();for(RectF r:screenActions.keySet())r.offset(0,header);
  fill(c,new RectF(15,(skinPreview?130:122)+header,outputRight(15),outputBottom),25,panel);drawFormula(c);
  if(busy){RectF cancel=new RectF(outputRight(218),outputBottom-102,outputRight(30),outputBottom-8);fill(c,cancel,12,0xffc1d6d1);text(c,"取消",cancel.centerX(),cancel.centerY()+15,38,Color.BLACK,Paint.Align.CENTER,SANS);screenActions.put(cancel,"cancelCalculation");}
  if(landscape){
   for(RectF hit:screenActions.keySet()){hit.left*=outputScale/sx;hit.right*=outputScale/sx;hit.top*=outputScale/sy;hit.bottom*=outputScale/sy;}
   contextMenuTop*=outputScale/sy;
  }
  canvas.restore();canvas.save();canvas.scale(sx,sy);
  if(c.isHardwareAccelerated()){
   String layout=landscape+":"+theme+":"+basic+":"+second+":"+baseMode+":"+baseRadix+":"+gridActive+":"+mode+":"+angle+":"+keyLabelStyle+":"+keyFontScale+":"+divisionSlash+":"+multiplicationStar+":"+skinPreview+":"+(!editor.empty()&&editor.complete()||!inputActive&&!tape.isEmpty());
   if(!layout.equals(keyboardLayout)||!keyboardNode.hasDisplayList()){keyboardNode.setPosition(0,0,1080,2400);Canvas cached=keyboardNode.beginRecording();recordingKeyboard=true;for(Key k:activeKeys())if(visible(k))drawWholeKey(cached,k);recordingKeyboard=false;keyboardNode.endRecording();keyboardLayout=layout;}
   c.drawRenderNode(keyboardNode);long now=android.os.SystemClock.uptimeMillis();for(Key k:activeKeys())if(visible(k)){if(k.ripple!=null&&(isKeyDown(k)||now<k.rippleUntil))drawWholeKey(c,k);else if(k.action.equals("menu"))drawWholeKey(c,k);}
  }else for(Key k:activeKeys())if(visible(k))drawWholeKey(c,k);
  canvas.restore();
 }
 private float keyXScale(Key k){return landscape?.5f:1;}
 private boolean basicLandscape(){return basic&&!baseMode&&!second;}
 private boolean numericLandscape(Key k){return !k.toolbar&&k.r.top>=(basicLandscape()?1362:1635);}
 private float keyXOffset(Key k){return landscape&&numericLandscape(k)?540:landscape&&k.toolbar?k.r.left*.5f:0;}
 private float keyYScale(Key k){return !landscape?1:k.toolbar?1.5f:basicLandscape()?1140f/(numericLandscape(k)?926:445):numericLandscape(k)?1140f/670:1140f/754;}
 private float keyYOffset(Key k){return !landscape?0:k.toolbar?930-786*1.5f:1180-(basicLandscape()?(numericLandscape(k)?1434:935):(numericLandscape(k)?1688:880))*keyYScale(k);}
 private float keyX(Key k,float x){return (x-keyXOffset(k))/keyXScale(k);}
 private float keyY(Key k,float y){return (y-keyYOffset(k))/keyYScale(k);}
 private void drawWholeKey(Canvas c,Key k){c.save();c.translate(keyXOffset(k),keyYOffset(k));c.scale(keyXScale(k),keyYScale(k));c.translate(0,k.yOffset);c.scale(1,k.yScale);drawKey(c,k);if(k.action.equals("dpad"))drawPad(c,k.r);else if(k.toolbar)drawToolbar(c,k);else drawLabel(c,k);drawSecondary(c,k);c.restore();}
 private void drawFormula(Canvas c){
  if(gridActive||workflowStep>=0||baseMode){Set<RectF> headerActions=new HashSet<>(screenActions.keySet());float dy=contentTop()-132;c.save();c.clipRect(18,contentTop(),outputRight(18),outputClipBottom());c.translate(0,dy);if(gridActive)drawGrid(c);else drawWorkflow(c);c.restore();for(RectF r:screenActions.keySet())if(!headerActions.contains(r))r.offset(0,dy);return;}
  renderer.spots.clear();renderer.editor=editor;renderer.color=Color.BLACK;renderer.accent=0xff8550ff;configure(renderer);renderer.showCursor=!skinPreview&&(!blinkCursor||System.currentTimeMillis()%1000<600);
  renderer.typeface=displayMono?MONO:SANS;float z=60*displayScale;Formula.Box b=renderer.measure(editor.root,z);
  float baseline=contentTop()+12+b.a,available=outputWidth-60;formulaX=24;
  if(b.w>available){
   // Collect cursor geometry off screen. Drawing this pass on the real canvas
   // overlays the unscrolled expression with the scrolled expression below.
   float caret=b.w;
   if(editor.cursor!=editor.root||editor.index!=editor.root.nodes.size()){
    Canvas probe=cursorLayoutProbe.beginRecording((int)Math.ceil(outputWidth),2400);
    try{renderer.draw(probe,editor.root,0,baseline,z);}finally{cursorLayoutProbe.endRecording();}
    for(Formula.Spot spot:renderer.spots)if(spot.row==editor.cursor&&spot.index==editor.index)caret=spot.x;
   }
   formulaX=24-Math.max(0,Math.min(b.w-available,caret-available*.75f));renderer.spots.clear();}
  c.save();c.clipRect(18,contentTop(),outputRight(18),outputClipBottom());
  float total=0;String layoutKey=outputWidth+":"+z+":"+displayMono+":"+groupThousands+":"+groupDecimals+":"+decimalComma+":"+ansStyle+":"+fractionMultiply+":"+descendingPowers+":"+showCalcInfo+":"+relativePercent+":"+implicitPriority;
  for(TapeLine l:tape){if(!layoutKey.equals(l.layoutKey))layoutTape(l,layoutKey,z);total+=l.height;}
  float currentHeight=!inputActive&&editor.empty()?0:Math.max(150,b.a+b.d+130+(prompt.isEmpty()?0:86));
  if(landscape&&currentHeight>0){Formula.Box answer=renderer.measure(displayResult(result),z);currentHeight=b.a+b.d+(result.isEmpty()?12:answer.a+answer.d+28+(calcInfo(editor.source(1)).isEmpty()?0:32))+(prompt.isEmpty()?0:86);}
  maxScroll=Math.max(0,total+currentHeight-(outputClipBottom()-contentTop()-(landscape?12:33)));scroll=Math.min(scroll,maxScroll);float at=contentTop()+12-maxScroll+scroll;
  for(int i=0;i<tape.size();i++){
   TapeLine l=tape.get(i);float h=l.height;
   if(at+h>contentTop()&&at<outputClipBottom()){c.save();c.translate(0,at);if(c.isHardwareAccelerated()){ensureTapeNode(l);c.drawRenderNode(l.node);}else c.drawPicture(l.picture);c.restore();
    screenActions.put(new RectF(90,at,outputRight(20),at+l.inputHeight+16),"tapeedit:"+i);
    screenActions.put(new RectF(18,at+l.inputHeight+16,outputRight(20),at+h),"resultmenu:"+i);
    if(i<tape.size()-1||inputActive)line(c,18,at+h-2,outputRight(18),at+h-2,0xff98aaa8,1);
   }at+=h;
  }
  baseline=at+b.a+(prompt.isEmpty()?0:86);contextMenuTop=baseline+b.d+8;if(!prompt.isEmpty())text(c,prompt,28,at+60,60,Color.BLACK,Paint.Align.LEFT,SANS);
  if(editor.empty()){if(renderer.showCursor&&inputActive)line(c,26,at,26,at+77,renderer.accent,5);}else {renderer.spots.clear();renderer.draw(c,editor.root,formulaX,baseline,z);if(!skinPreview)renderer.drawCursor(c,z);}
  if(!result.isEmpty()){
   Formula.Row row=displayResult(result);float rz=z;Formula.Box rb=renderer.measure(row,rz);float resultWidth=outputWidth-140;if(rb.w>resultWidth)rz*=Math.max(.35f,resultWidth/rb.w);rb=renderer.measure(row,rz);Formula.Renderer out=new Formula.Renderer();configure(out);out.color=Color.BLACK;out.showCursor=false;
   if(!editor.empty()){float resultTop=skinPreview?700:baseline+b.d+28;c.save();c.clipRect(104,resultTop-8,outputRight(22),outputClipBottom());out.draw(c,row,outputRight(30)-rb.w,resultTop+rb.a,rz);c.restore();String info=calcInfo(editor.source(1));if(!info.isEmpty())text(c,info,outputRight(30),resultTop+rb.a+rb.d+32,24,0xff64716f,Paint.Align.RIGHT,SANS);screenActions.put(new RectF(18,resultTop-8,outputRight(20),resultTop+Math.max(64,rb.a+rb.d)+8),"resultmenu");if(!skinPreview)for(int i=0;i<3;i++){p.setColor(0xff728381);c.drawCircle(57,resultTop+21+i*15,5.5f,p);}}
  }
  if(busy||!message.isEmpty())text(c,busy?busyLabel:message,28,outputBottom-25,26,busy?0xff688b82:0xffad5041,Paint.Align.LEFT,SANS);c.restore();
 }
 private void layoutTape(TapeLine l,String key,float z){
  Formula.Renderer r=new Formula.Renderer();configure(r);r.showCursor=false;r.color=Color.BLACK;
  Formula.Box in=r.measure(l.formula.root,z);Formula.Row row=displayResult(l.display);Formula.Box out=r.measure(row,z);
  String info=calcInfo(l.formula.source(1));l.inputHeight=in.a+in.d;l.height=l.inputHeight+out.a+out.d+42+(info.isEmpty()?0:30);
  l.picture=new Picture();Canvas c=l.picture.beginRecording((int)Math.ceil(outputWidth),(int)Math.ceil(l.height));r.draw(c,l.formula.root,26,in.a,z);
  float size=z;if(out.w>outputWidth-140){size*=(outputWidth-140)/out.w;out=r.measure(row,size);}r.spots.clear();r.draw(c,row,outputRight(36)-out.w,l.height-16-out.d-(info.isEmpty()?0:30),size);
  if(!info.isEmpty())text(c,info,outputRight(36),l.height-8,24,0xff64716f,Paint.Align.RIGHT,SANS);
  p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(0xff728381);for(int d=0;d<3;d++)c.drawCircle(57,l.height-65+d*15,5.5f,p);
  l.picture.endRecording();if(l.node!=null)l.node.discardDisplayList();ensureTapeNode(l);l.layoutKey=key;
 }
 // Android can discard nested display lists when the window is hidden. Keep the
 // parsed formula and Picture, but restore the hardware recording before drawing.
 private void ensureTapeNode(TapeLine line){
  if(line.node==null)line.node=new RenderNode("calculation");
  if(!line.node.hasDisplayList()){
   line.node.setPosition(0,0,(int)Math.ceil(outputWidth),(int)Math.ceil(line.height));
   Canvas cached=line.node.beginRecording();
   try{cached.drawPicture(line.picture);}finally{line.node.endRecording();}
  }
 }
 @Override public void computeScroll(){if(scroller.computeScrollOffset()){scroll=scroller.getCurrY()/outputScale;postInvalidateOnAnimation();}}
 private void flowButton(Canvas c,String label,RectF bounds,String action){p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setColor(0xff64716f);p.setStrokeWidth(2);c.drawRoundRect(bounds,8,8,p);p.setStyle(Paint.Style.FILL);text(c,label,bounds.centerX(),bounds.centerY()+14,40,Color.BLACK,Paint.Align.CENTER,SANS);screenActions.put(bounds,action);}
 private void drawWorkflow(Canvas c){
  c.save();c.clipRect(18,132,outputRight(18),outputClipBottom());renderer.editor=editor;configure(renderer);renderer.color=Color.BLACK;renderer.showCursor=!skinPreview&&(!blinkCursor||System.currentTimeMillis()%1000<600);renderer.spots.clear();float z=60*displayScale,left=24,top=baseMode?200:230;
  if(baseMode){String label=(baseRadix==10?"DECIMAL":baseRadix==16?"HEXADECIMAL":baseRadix==2?"BINARY":"OCTAL")+" "+baseBits+" bits signed";text(c,label,outputRight(30),182,42,Color.BLACK,Paint.Align.RIGHT,MONO);screenActions.put(new RectF(380,132,outputRight(20),200),"baseSettings");}
  else{text(c,"第 "+(workflowStep+1)+"/5 步",36,187,40,Color.BLACK,Paint.Align.LEFT,SANS);flowButton(c,"[?]",new RectF(485,142,579,210),"workflowHelp");flowButton(c,"上一步",new RectF(613,142,783,210),"workflowPrevious");flowButton(c,"下一步 [=]",new RectF(817,142,outputRight(28),210),"workflowNext");line(c,18,222,outputRight(18),222,0xff8e9693,2);text(c,workflowCaption,26,290,60,Color.BLACK,Paint.Align.LEFT,MONO);p.setTextSize(60);p.setTypeface(MONO);left=26+p.measureText(workflowCaption)+22;}
  Formula.Box b=renderer.measure(editor.root,z);float baseline=top+b.a;if(editor.empty()){if(renderer.showCursor)line(c,left,top,left,top+77,renderer.accent,5);}else{renderer.draw(c,editor.root,left,baseline,z);if(!skinPreview)renderer.drawCursor(c,z);}
  if(baseMode&&!result.isEmpty()){text(c,result,outputRight(36),baseline+b.d+z+20,60,Color.BLACK,Paint.Align.RIGHT,SANS);}
  if(busy||!message.isEmpty())text(c,busy?busyLabel:message,28,outputBottom-22-(contentTop()-132),24,0xffad5041,Paint.Align.LEFT,SANS);c.restore();
 }
 private Formula.Row displayResult(String value){String key=descendingPowers+":"+value;Formula.Row row=outputRows.get(key);if(row==null){row=Formula.result(descendingPowers?ResultFormat.orderPolynomial(value):value);outputRows.put(key,row);}return row;}
 private String calcInfo(String source){if(!showCalcInfo)return "";if(source.contains("%"))return relativePercent?"百分比：相对左侧数值":"百分比：除以 100";if(implicitPriority&&source.matches(".*[/÷][0-9.]+\\*.*"))return "隐式乘法优先";return "";}
 private void configure(Formula.Renderer r){r.typeface=displayMono?MONO:SANS;r.groupThousands=groupThousands;r.groupDecimals=groupDecimals;r.decimalComma=decimalComma;r.ansStyle=ansStyle;r.fractionMultiply=fractionMultiply;r.closeParen=getContext().getSharedPreferences("calculator",0).getBoolean("closeParen",false);}
 private void drawGrid(Canvas c){
  c.save();c.clipRect(18,132,outputRight(18),outputClipBottom());int cols=gridHeaders.length;if(cols==0){c.restore();return;}
  boolean coefficients=mode.equals("SOLVE");float left=18,right=outputRight(18),width=(right-left)/cols,headerTop=coefficients?220:132,headerBottom=headerTop+82;if(coefficients){text(c,coefficientTitle,26,190,40,Color.BLACK,Paint.Align.LEFT,SANS);flowButton(c,"[?]",new RectF(714,142,829,210),"workflowHelp");flowButton(c,"求解 [=]",new RectF(852,142,outputRight(28),210),"workflowNext");line(c,left,220,right,220,0xff8e9693,2);}
  for(int col=0;col<cols;col++){String label=gridHeaders[col];if(label.equals("Freq"))label="频率";if(label.equals("X"))label="x";if(label.equals("Y"))label="y";text(c,label,left+(col+.5f)*width,headerTop+52,46,Color.BLACK,Paint.Align.CENTER,SANS);if(col>0)line(c,left+col*width,headerTop,left+col*width,coefficients?headerBottom+grid.length*66:outputClipBottom(),0xff8e9693,2);}
  line(c,left,headerBottom,right,headerBottom,0xff8e9693,2);
  int visibleRows=Math.max(1,(int)((outputClipBottom()-(contentTop()-132)-headerBottom-2)/66));int start=Math.max(0,gridRow-visibleRows+1),last=grid.length-1;
  if(mode.equals("STAT")){last=gridRow;for(int r=0;r<grid.length;r++)if(!grid[r][0].isEmpty())last=Math.max(last,r);}
  for(int r=start;r<=Math.min(last,start+visibleRows-1);r++){float y=headerBottom+2+(r-start)*66;
   for(int col=0;col<cols;col++){RectF cell=new RectF(left+col*width,y,left+(col+1)*width,y+64);boolean selected=r==gridRow&&col==gridCol;
    if(selected){fill(c,cell,0,0xffcbd2e9);p.setStyle(Paint.Style.STROKE);p.setColor(purple);p.setStrokeWidth(5);c.drawRect(cell.left+2.5f,cell.top+2.5f,cell.right-2.5f,cell.bottom-2.5f,p);p.setStyle(Paint.Style.FILL);}
    String value=grid[r][col];if(mode.equals("STAT")&&grid[r][0].isEmpty()&&col==cols-1&&value.equals("1"))value="";
    if(selected)value=editor.empty()?"":editor.source(1);text(c,value,cell.left+10,y+48,Math.min(46,46*14f/Math.max(14,value.length())),Color.BLACK,Paint.Align.LEFT,SANS);screenActions.put(cell,"gridcell:"+r+":"+col);
   }line(c,left,y+64,right,y+64,0xff8e9693,2);
  }
  if(coefficients){float y=headerBottom+grid.length*66+65;if(result.isEmpty())text(c,editor.source(1),28,y,60,Color.BLACK,Paint.Align.LEFT,SANS);else{Formula.Renderer out=new Formula.Renderer();configure(out);out.color=Color.BLACK;out.showCursor=false;Formula.Row answer=displayResult(result);Formula.Box box=out.measure(answer,50);out.draw(c,answer,28,y+box.a,50);}}
  if(busy||!message.isEmpty())text(c,busy?busyLabel:message,28,outputBottom-22-(contentTop()-132),24,0xffad5041,Paint.Align.LEFT,SANS);c.restore();
 }
 private void drawLabel(Canvas c,Key k){
  float x=k.r.centerX(),y=k.r.centerY();int color=theme==8?0xff292b2d:k.fill==orange||k.fill==0xfffdc12b?Color.BLACK:k.fill==0xff272b31?0xffe2e2e0:0xfff6f9f6;
  if(basic||keyLabelStyle==1){
   p.setColor(color);p.setStyle(Paint.Style.FILL);
   switch(k.label){
    case "fraction":c.drawRoundRect(x-22,y-47,x+22,y-10,3,3,p);line(c,x-29,y,x+29,y,color,3);p.setColor(color);c.drawRoundRect(x-22,y+11,x+22,y+48,3,3,p);return;
    case "power":c.drawRoundRect(x-54,y-8,x-7,y+39,3,3,p);c.drawRoundRect(x+17,y-38,x+64,y+5,3,3,p);return;
    case "reciprocal":c.drawRoundRect(x-55,y-10,x+1,y+40,3,3,p);text(c,"−1",x+42,y-1,45,color,Paint.Align.CENTER,SANS);return;
    case "sqrt":Path q=new Path();q.moveTo(x-57,y+5);q.lineTo(x-46,y+2);q.lineTo(x-33,y+40);q.lineTo(x-16,y-35);q.lineTo(x+58,y-35);q.lineTo(x+58,y-24);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);c.drawPath(q,p);p.setStyle(Paint.Style.FILL);c.drawRoundRect(x-8,y-22,x+46,y+32,3,3,p);return;
    case "◀":case "▶":c.save();if(k.label.equals("▶"))c.rotate(180,x,y);Path arrow=new Path();arrow.moveTo(x-34,y);arrow.lineTo(x+31,y-26);arrow.quadTo(x+10,y,x+31,y+26);arrow.close();c.drawPath(arrow,p);c.restore();return;
   }
  }
  if(k.label.equals("matrixGrid")){line(c,x-37,y-27,x-37,y+27,color,3);line(c,x+37,y-27,x+37,y+27,color,3);for(int a=0;a<2;a++)for(int b=0;b<2;b++){p.setColor(color);c.drawRect(x-26+a*30,y-23+b*28,x-8+a*30,y-5+b*28,p);}return;}
  if(k.label.equals("inverseMatrix")){text(c,"[ ]",x-12,y+23,58,color,Paint.Align.CENTER,SANS);text(c,"−1",x+45,y-10,35,color,Paint.Align.CENTER,SANS);return;}
  switch(k.label){
   case "img":UiIcons.draw(c,p,"history",x,y,42,color);return;
   case "fraction":text(c,"x",x,y-17,46,color,Paint.Align.CENTER,SANS);line(c,x-12,y,x+12,y,color,2);text(c,"y",x,y+43,46,color,Paint.Align.CENTER,SANS);return;
   case "sqrt":radical(c,x-48,y+30,color);return;
   case "square":case "power":case "reciprocal":text(c,"x",x-11,y+31,62,color,Paint.Align.CENTER,SANS);text(c,k.label.equals("square")?"2":k.label.equals("power")?"y":"−1",x+24,y-9,44,color,Paint.Align.CENTER,SANS);return;
   case "integral":text(c,"∫",x-43,y+32,71,color,Paint.Align.CENTER,SANS);text(c,"dx",x+12,y+27,56,color,Paint.Align.CENTER,SANS);return;
   case "logbase":text(c,"Log",x-24,y+9,52,color,Paint.Align.CENTER,SANS);text(c,"x",x+20,y+34,34,color,Paint.Align.CENTER,SANS);text(c,"y",x+49,y+12,51,color,Paint.Align.CENTER,SANS);return;
   case "delete":Path path=new Path();path.moveTo(x-42,y);path.lineTo(x-22,y-28);path.lineTo(x+32,y-28);path.quadTo(x+39,y-28,x+39,y-19);path.lineTo(x+39,y+28);path.lineTo(x-22,y+28);path.close();p.setColor(Color.BLACK);c.drawPath(path,p);line(c,x-9,y-13,x+15,y+13,orange,5);line(c,x+15,y-13,x-9,y+13,orange,5);return;
  }
  float z=k.r.width()>180&&(!basic||k.r.top>1300)||basic&&k.label.equals("AC")?84:k.label.equals("SHIFT")||k.label.equals("ALPHA")?45:k.label.equals("MODE")?56:k.label.equals("2nd")?59:60;
  Typeface face=k.r.top>1600?SANS:k.label.equals("2nd")?UiFonts.ITALIC:k.label.equals("Sin")||k.label.equals("Cos")||k.label.equals("Tan")||k.label.equals("hyp")?MONO:SANS;
  p.setTypeface(face);p.setTextSize(z);float width=p.measureText(k.label);if(width>k.r.width()-14)z*=(k.r.width()-14)/width;
  z*=keyFontScale;String label=k.label.equals("÷")&&divisionSlash?"/":k.label.equals("×")&&multiplicationStar?"*":k.label;text(c,label,x,y+z*.34f,z,color,Paint.Align.CENTER,face);
 }
 private void radical(Canvas c,float x,float y,int color){Path path=new Path();path.moveTo(x,y-15);path.lineTo(x+10,y-17);path.lineTo(x+20,y+12);path.lineTo(x+36,y-61);path.lineTo(x+85,y-61);path.lineTo(x+85,y-50);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);text(c,"x",x+62,y,60,color,Paint.Align.CENTER,SANS);}
 private void drawSecondary(Canvas c,Key k){
  if(k.secondary.isEmpty()&&k.tertiary.isEmpty())return;float y=k.r.top-18,x=k.r.left+1,z=38;String sec=k.secondary,third=k.tertiary;
  float tz=third.length()>4?31:third.length()>2?34:38;p.setTypeface(BOLD);p.setTextSize(tz);float tertiaryWidth=third.equals("matrixGrid")?36:p.measureText(third);p.setTextSize(z);float secondaryWidth=p.measureText(sec);if(!third.isEmpty()&&!sec.isEmpty()&&!sec.equals("polar")&&!sec.equals("fraction"))z=Math.min(z,Math.max(24,38*(k.r.width()-tertiaryWidth-7)/secondaryWidth));
  if(sec.equals("fraction")){text(c,"x",x+20,y-7,28,yellow,Paint.Align.CENTER,BOLD);text(c,"y",x+33,y-24,21,yellow,Paint.Align.CENTER,BOLD);line(c,x+29,y-12,x+46,y-12,yellow,1.5f);text(c,"z",x+35,y+5,21,yellow,Paint.Align.CENTER,BOLD);}
  else if(sec.equals("polar")){text(c,"x",x+16,y-22,19,yellow,Paint.Align.CENTER,BOLD);line(c,x+7,y-16,x+26,y-16,yellow,1.5f);text(c,"y",x+16,y+2,19,yellow,Paint.Align.CENTER,BOLD);text(c,"→x",x+29,y-3,25,yellow,Paint.Align.LEFT,BOLD);text(c,"y",x+74,y-22,19,yellow,Paint.Align.CENTER,BOLD);line(c,x+65,y-16,x+84,y-16,yellow,1.5f);text(c,"z",x+74,y+2,19,yellow,Paint.Align.CENTER,BOLD);}
  else if(sec.equals("∑"))MathGlyphs.draw(c,p,false,x+17,y-36,37,36,ink(yellow));
  else if(sec.equals("³√x")||sec.equals("ˣ√y")){text(c,sec.equals("³√x")?"3":"x",x+9,y-16,23,yellow,Paint.Align.LEFT,BOLD);Path root=new Path();root.moveTo(x+21,y-12);root.lineTo(x+27,y-13);root.lineTo(x+34,y+6);root.lineTo(x+44,y-30);root.lineTo(x+71,y-30);root.lineTo(x+71,y-24);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.6f);p.setColor(ink(yellow));c.drawPath(root,p);p.setStyle(Paint.Style.FILL);text(c,sec.equals("³√x")?"x":"y",x+58,y+3,33,yellow,Paint.Align.CENTER,BOLD);}
  else if(!sec.isEmpty()){p.setTypeface(BOLD);p.setTextSize(z);float offset=third.isEmpty()?Math.max(0,(k.r.width()-p.measureText(sec))/2):0;text(c,sec,x+offset,y,z,yellow,Paint.Align.LEFT,BOLD);}
  if(third.equals("matrixGrid")){line(c,k.r.right-36,y-27,k.r.right-36,y+1,purple,3);line(c,k.r.right-3,y-27,k.r.right-3,y+1,purple,3);for(int a=0;a<2;a++)for(int b=0;b<2;b++){p.setColor(ink(purple));c.drawRect(k.r.right-29+a*14,y-26+b*16,k.r.right-21+a*14,y-18+b*16,p);}}
  else if(third.equals("▰")){line(c,k.r.right-64,y-32,k.r.right-31,y-32,purple,3);p.setColor(ink(purple));c.drawRoundRect(k.r.right-57,y-22,k.r.right-35,y+4,3,3,p);}
  else if(third.equals("Π"))MathGlyphs.draw(c,p,true,k.r.right-51,y-36,39,36,ink(purple));
  else if(!third.isEmpty())text(c,third,k.r.right-2,y,tz,purple,Paint.Align.RIGHT,BOLD);
 }
 private void drawPad(Canvas c,RectF r){float x=r.centerX(),y=r.centerY();line(c,r.left+32,r.top+25,r.right-32,r.bottom-25,0xff100d0e,5);line(c,r.right-32,r.top+25,r.left+32,r.bottom-25,0xff100d0e,5);p.setColor(0xff100d0e);c.drawCircle(x,y,31,p);for(int i=0;i<4;i++){c.save();c.rotate(i*90,x,y);Path path=new Path();path.moveTo(x,y-120);path.lineTo(x-21,y-72);path.quadTo(x,y-87,x+21,y-72);path.close();p.setColor(theme==8?0xff525055:0xffbdbfbd);c.drawPath(path,p);c.restore();}}
 private void drawToolbar(Canvas c,Key k){if(recordingKeyboard&&k.action.equals("menu"))return;float x=k.r.centerX(),y=k.r.centerY();if(k.label.equals("MORE")&&gridActive&&mode.equals("SOLVE")){text(c,"GRAPH",x,y+17,43,0xfff6f9f6,Paint.Align.CENTER,BOLD);return;}if(k.label.equals("MORE")){p.setColor(purple);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);c.drawRoundRect(k.r,41,41,p);p.setStyle(Paint.Style.FILL);text(c,k.label,x,y+17,46,purple,Paint.Align.CENTER,BOLD);return;}if(k.label.equals("DEG")){text(c,angle,x,y+15,43,0xfff6f9f6,Paint.Align.CENTER,BOLD);return;}c.save();if(k.action.equals("menu"))c.rotate(180*drawerProgress,x,y);UiIcons.draw(c,p,k.label,x,y,58,ink(0xfff6f9f6));c.restore();}
 private void keyPressed(Key k,boolean pressed,float x,float y){
  if(k.ripple==null){k.ripple=new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x50000000),null,new android.graphics.drawable.ColorDrawable(Color.WHITE));k.ripple.setCallback(this);k.ripple.setBounds((int)k.r.left,(int)k.r.top,(int)k.r.right,(int)k.r.bottom);}
  k.ripple.setHotspot(x,y);k.ripple.setState(pressed?new int[]{android.R.attr.state_enabled,android.R.attr.state_pressed}:new int[]{android.R.attr.state_enabled});
  k.rippleUntil=android.os.SystemClock.uptimeMillis()+600;
 }
 @Override protected boolean verifyDrawable(android.graphics.drawable.Drawable d){return d instanceof android.graphics.drawable.RippleDrawable||super.verifyDrawable(d);}
 @Override public void invalidateDrawable(android.graphics.drawable.Drawable d){postInvalidateOnAnimation();}
 private void releaseVelocity(){if(velocity!=null){velocity.recycle();velocity=null;}}
 private final class KeyTouch {
  final Key key;int pointer;float x,y;boolean held,cancelled;String repeat;long nextRepeat;
  final Runnable hold;
  KeyTouch(Key k,int id,float px,float py){key=k;pointer=id;x=px;y=py;repeat=k.action.equals("dpad")?padAction(k,px,py):k.action;
   hold=this::heldAction;
  }
  private void heldAction(){if(cancelled||!isKeyDown(key))return;held=true;
   if(isRepeating(key)){listener.press(repeat);nextRepeat+=200;timer.postAtTime(hold,Math.max(nextRepeat,android.os.SystemClock.uptimeMillis()));}
   else if(!key.alt.isEmpty()){feedback(key.action);listener.press(key.alt);}
  }
 }
 private boolean isRepeating(Key k){return k.action.equals("delete")||k.action.equals("dpad")||k.action.equals("left")||k.action.equals("right");}
 private boolean isKeyDown(Key k){for(int i=0;i<keyTouches.size();i++)if(keyTouches.valueAt(i).key==k&&!keyTouches.valueAt(i).cancelled)return true;return false;}
 void cancelTouches(){
  for(int i=0;i<keyTouches.size();i++){KeyTouch t=keyTouches.valueAt(i);t.cancelled=true;timer.removeCallbacks(t.hold);keyPressed(t.key,false,t.x,t.y);}
  timer.removeCallbacks(panelHold);keyTouches.clear();gesturePointer=-1;panelGesture=false;dragged=false;releaseVelocity();postInvalidateOnAnimation();
 }
 private void beginKey(int id,float x,float y,long eventTime){
  for(Key k:activeKeys())if(visible(k)&&k.hitRect.contains(keyX(k,x),keyY(k,y))){
   x=keyX(k,x);y=keyY(k,y);
   // Android splits different key views by pointer. The first pointer owns
   // a key; lifting it while another remains on that same key cancels it.
   for(int i=0;i<keyTouches.size();i++){KeyTouch t=keyTouches.valueAt(i);if(t.key==k){keyTouches.put(id,t);return;}}
   float modelY=k.modelY(y);KeyTouch t=new KeyTouch(k,id,x,modelY);keyTouches.put(id,t);feedback(k.action);keyPressed(k,true,x,modelY);
   if(isRepeating(k)||!k.alt.isEmpty()){t.nextRepeat=eventTime+(isRepeating(k)?400:300);timer.postAtTime(t.hold,t.nextRepeat);}
   postInvalidateOnAnimation();return;
  }
 }
 private void finishKey(int id,float x,float y){
  KeyTouch t=keyTouches.get(id);if(t==null)return;x=keyX(t.key,x);y=t.key.modelY(keyY(t.key,y));keyTouches.remove(id);
  for(int i=0;i<keyTouches.size();i++)if(keyTouches.valueAt(i)==t){if(t.pointer==id){t.cancelled=true;timer.removeCallbacks(t.hold);keyPressed(t.key,false,x,y);postInvalidateOnAnimation();}return;}
  timer.removeCallbacks(t.hold);keyPressed(t.key,false,x,y);postInvalidateOnAnimation();
  // Like Android View, a press remains armed until a MOVE leaves the target
  // plus touch slop. UP does not impose a second, tighter drawing-bound check.
  if(t.cancelled||t.held&&!isRepeating(t.key))return;
  Key k=t.key;if(k.action.equals("dpad"))listener.press(padAction(k,x,y));else if(shift&&!k.alt.isEmpty())listener.press(k.alt);else if(alpha&&!k.alphaAction.isEmpty())listener.press(k.alphaAction);else listener.press(k.action);performClick();
 }
 private RectF cancelTouchBounds(){return new RectF((landscape?outputRight(218)*outputScale/sx:862),(outputBottom-102)*outputScale/sy,(landscape?outputRight(30)*outputScale/sx:1050),(outputBottom-8)*outputScale/sy);}
 @Override public boolean onTouchEvent(MotionEvent e){
  int action=e.getActionMasked(),index=e.getActionIndex(),id=e.getPointerId(index);float x=e.getX(index)/sx,y=e.getY(index)/sy;
  if(action==MotionEvent.ACTION_CANCEL){cancelTouches();return true;}
  if(action==MotionEvent.ACTION_DOWN){
   boolean wasMoving=!scroller.isFinished();cancelTouches();stopScroll();downTime=e.getEventTime();contextOpened=false;gestureTarget="";startX=x;startY=touchY=y;verticalDrag=false;
   beginKey(id,x,y,e.getEventTime());if(keyTouches.get(id)!=null)return true;
   gesturePointer=id;panelGesture=y>=touchContentTop()&&y<touchOutputBottom()&&!gridActive&&workflowStep<0&&!baseMode;dragged=wasMoving&&panelGesture;verticalDrag=dragged;
   if(panelGesture&&!dragged){for(Map.Entry<RectF,String> hit:screenActions.entrySet())if(hit.getKey().contains(x,y)){gestureTarget=hit.getValue();if(gestureTarget.startsWith("tapeedit:")||gestureTarget.startsWith("resultmenu"))contextMenuTop=hit.getKey().bottom+8;break;}timer.postAtTime(panelHold,downTime+ViewConfiguration.getLongPressTimeout());}
   velocity=VelocityTracker.obtain();velocity.addMovement(e);return true;
  }
  if(action==MotionEvent.ACTION_POINTER_DOWN){
   // A second pointer must not cancel keys already held by another finger.
   if(gesturePointer>=0){timer.removeCallbacks(panelHold);panelGesture=false;dragged=true;releaseVelocity();}
   beginKey(id,x,y,e.getEventTime());return true;
  }
  if(velocity!=null)velocity.addMovement(e);
  if(action==MotionEvent.ACTION_MOVE){
   for(int i=0;i<keyTouches.size();i++){
    KeyTouch t=keyTouches.valueAt(i);if(t.pointer!=keyTouches.keyAt(i)||t.cancelled)continue;
    int pointerIndex=e.findPointerIndex(t.pointer);if(pointerIndex<0)continue;
    t.x=keyX(t.key,e.getX(pointerIndex)/sx);t.y=t.key.modelY(keyY(t.key,e.getY(pointerIndex)/sy));RectF r=t.key.hitModelRect;float ySlop=touchSlop/(sy*t.key.yScale*keyYScale(t.key));
    if(t.x<r.left-touchSlop/(sx*keyXScale(t.key))||t.x>=r.right+touchSlop/(sx*keyXScale(t.key))||t.y<r.top-ySlop||t.y>=r.bottom+ySlop){t.cancelled=true;timer.removeCallbacks(t.hold);keyPressed(t.key,false,t.x,t.y);postInvalidateOnAnimation();}
    else if(t.key.action.equals("dpad"))t.repeat=padAction(t.key,t.x,t.y);
   }
   int pointerIndex=e.findPointerIndex(gesturePointer);if(pointerIndex<0)return true;x=e.getX(pointerIndex)/sx;y=e.getY(pointerIndex)/sy;
   if(panelGesture){if(!dragged&&Math.max(Math.abs(y-startY)*sy,Math.abs(x-startX)*sx)>touchSlop){timer.removeCallbacks(panelHold);dragged=true;verticalDrag=Math.abs(y-startY)*sy>=Math.abs(x-startX)*sx;touchY=startY+(y>startY?touchSlop/sy:-touchSlop/sy);}
    if(dragged&&verticalDrag){scroll=Math.max(0,Math.min(maxScroll,scroll+(y-touchY)*sy/outputScale));touchY=y;postInvalidateOnAnimation();}
   }return true;
  }
  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
   if(keyTouches.get(id)!=null){finishKey(id,x,y);return true;}
   if(id!=gesturePointer)return true;timer.removeCallbacks(panelHold);gesturePointer=-1;if(contextOpened){releaseVelocity();return true;}
   if(dragged){if(velocity!=null&&panelGesture&&verticalDrag){velocity.computeCurrentVelocity(1000,maxVelocity);float v=velocity.getYVelocity(id);if(Math.abs(v)>minVelocity){scroller.fling(0,Math.round(scroll*outputScale),0,Math.round(v),0,0,0,Math.round(maxScroll*outputScale));postInvalidateOnAnimation();}}releaseVelocity();return true;}
   releaseVelocity();
   if(busy&&cancelTouchBounds().contains(x,y)&&cancelTouchBounds().contains(startX,startY)){listener.press("cancelCalculation");performClick();return true;}
   for(Map.Entry<RectF,String> hit:screenActions.entrySet())if(hit.getKey().contains(x,y)&&hit.getKey().contains(startX,startY)){
    // Rows are laid out before clipping; their hidden portions are not targets.
    if((hit.getValue().startsWith("tapeedit:")||hit.getValue().startsWith("resultmenu")||hit.getValue().startsWith("gridcell:"))&&(y<touchContentTop()||y>touchClipBottom()||startY<touchContentTop()||startY>touchClipBottom()))continue;
    if(hit.getValue().startsWith("resultmenu"))resultMenuTop=Math.min(1050,hit.getKey().bottom+12);listener.press(hit.getValue());performClick();return true;
   }
   if(panelGesture){renderer.select(landscape?x*sx/outputScale:x,y*sy/outputScale-(gridActive||workflowStep>=0||baseMode?contentTop()-132:0));postInvalidateOnAnimation();}return true;
  }return true;
 }
 private String padAction(Key k,float x,float y){float dx=x-k.r.centerX(),dy=y-k.r.centerY();return Math.abs(dx)>Math.abs(dy)?(dx>0?"right":"left"):(dy>0?"down":"up");}
 @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(!focus)cancelTouches();}
 @Override public boolean performClick(){super.performClick();return true;}
}
