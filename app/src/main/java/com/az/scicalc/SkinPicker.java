package com.az.scicalc;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** A native horizontal pager; swipes move cached pictures, never live calculators. */
final class SkinPicker {
 private final MainActivity app;
 private final ToolPage page;
 private final Pager pager;
 private final HorizontalScrollView tabs;
 private final ArrayList<TextView> labels=new ArrayList<>();
 private final ArrayList<View> markers=new ArrayList<>();
 private final ArrayList<ImageView> pictures=new ArrayList<>();
 private final android.util.LruCache<Integer,Bitmap> cache=new android.util.LruCache<>(4);
 private final TextView apply;
 private final int pictureWidth,pictureHeight;
 private int selected,generation;
 private boolean released;

 static ToolPage create(MainActivity app,int selected,Runnable back,Runnable font,Runnable settings){
  return new SkinPicker(app,selected,back,font,settings).page;
 }
 private SkinPicker(MainActivity app,int initial,Runnable back,Runnable font,Runnable settings){
  this.app=app;selected=Math.max(0,Math.min(CalculatorView.THEMES.length-1,initial));
  page=new ToolPage(app,"计算器皮肤",back){
   @Override protected void onAttachedToWindow(){super.onAttachedToWindow();released=false;post(()->loadNearby(selected));}
   @Override protected void onDetachedFromWindow(){super.onDetachedFromWindow();released=true;generation++;cache.evictAll();}
  };
  page.title.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP,20);
  page.action("font","字体",font);page.action("more","皮肤设置",settings);
  // The gallery owns horizontal drags; there is no interactive preview or outer vertical scroll.
  page.removeView(page.scroll);
  tabs=new HorizontalScrollView(app);tabs.setHorizontalScrollBarEnabled(false);
  LinearLayout strip=new LinearLayout(app);
  for(int i=0;i<CalculatorView.THEMES.length;i++){
   final int index=i;LinearLayout cell=new LinearLayout(app);cell.setOrientation(1);
   TextView label=ToolPage.text(app,CalculatorView.THEMES[i],19);label.setSingleLine(true);label.setGravity(Gravity.CENTER);label.setPadding(page.dp(14),0,page.dp(14),0);
   cell.addView(label,new LinearLayout.LayoutParams(-2,page.dp(44)));
   View marker=new View(app);cell.addView(marker,new LinearLayout.LayoutParams(-1,page.dp(2)));
   labels.add(label);markers.add(marker);cell.setOnClickListener(v->selectSkin(index));strip.addView(cell);
  }
  tabs.addView(strip);page.addView(tabs,new LinearLayout.LayoutParams(-1,page.dp(46)));
  int width=app.getResources().getDisplayMetrics().widthPixels;
  int height=app.getResources().getDisplayMetrics().heightPixels;
  pictureWidth=Math.max(page.dp(60),Math.min(width-page.dp(95),Math.max(page.dp(120),height-page.dp(256))/2));pictureHeight=pictureWidth*2;
  pager=new Pager(app,Math.max(pictureWidth+page.dp(28),width-page.dp(42)));
  pager.setPadding((width-pager.pageWidth)/2,0,(width-pager.pageWidth)/2,0);pager.setClipToPadding(false);
  LinearLayout pages=new LinearLayout(app);
  for(int i=0;i<CalculatorView.THEMES.length;i++){
   FrameLayout slot=new FrameLayout(app);ImageView picture=new ImageView(app);picture.setScaleType(ImageView.ScaleType.FIT_CENTER);
   picture.setContentDescription("皮肤预览："+CalculatorView.THEMES[i]);
   picture.setOutlineProvider(new ViewOutlineProvider(){public void getOutline(View v,Outline out){out.setRoundRect(0,0,v.getWidth(),v.getHeight(),page.dp(14));}});picture.setClipToOutline(true);
   FrameLayout.LayoutParams image=new FrameLayout.LayoutParams(pictureWidth,-1,Gravity.CENTER);image.topMargin=image.bottomMargin=page.dp(24);slot.addView(picture,image);
   pictures.add(picture);pages.addView(slot,new LinearLayout.LayoutParams(pager.pageWidth,-1));
  }
  pager.addView(pages,new HorizontalScrollView.LayoutParams(-2,-1));page.addView(pager,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout footer=new LinearLayout(app);footer.setGravity(Gravity.CENTER_VERTICAL);footer.setPadding(page.dp(12),page.dp(8),page.dp(12),page.dp(8));
  footer.addView(ToolPage.icon(app,"back","上一皮肤",()->pager.select((selected+CalculatorView.THEMES.length-1)%CalculatorView.THEMES.length),.4f),new LinearLayout.LayoutParams(page.dp(42),-1));
  apply=ToolPage.text(app,"",18);apply.setGravity(Gravity.CENTER);apply.setTextColor(Color.BLACK);apply.setBackground(ToolPage.rounded(app,ToolPage.ACCENT,22));
  footer.addView(apply,new LinearLayout.LayoutParams(0,page.dp(42),1));apply.setOnClickListener(v->{app.prefs.edit().putInt("skin",selected).apply();app.applyPreferences();updateSelected(selected);});
  TextView random=ToolPage.text(app,"⚄ 随机",18);random.setGravity(Gravity.CENTER);random.setTextColor(ToolPage.ACCENT);random.setBackground(ToolPage.rounded(app,ToolPage.BG,22));
  LinearLayout.LayoutParams randomLp=new LinearLayout.LayoutParams(page.dp(84),page.dp(42));randomLp.leftMargin=page.dp(8);footer.addView(random,randomLp);
  random.setOnClickListener(v->{int next=new Random().nextInt(CalculatorView.THEMES.length-1);if(next>=selected)next++;pager.select(next);});
  footer.addView(ToolPage.icon(app,"right","下一皮肤",()->pager.select((selected+1)%CalculatorView.THEMES.length),.4f),new LinearLayout.LayoutParams(page.dp(42),-1));
  page.addView(footer,new LinearLayout.LayoutParams(-1,page.dp(64)));
  updateSelected(selected);pager.post(()->{pager.scrollTo(selected*pager.pageWidth,0);loadNearby(selected);});
 }
 private void selectSkin(int index){pager.select(index);}
 private void updateSelected(int index){
  selected=index;page.title.setText("计算器皮肤\n"+(index+1)+" / "+CalculatorView.THEMES.length);
  apply.setText(app.prefs.getInt("skin",0)==index?"✓    已选定":"应用皮肤");
  for(int i=0;i<labels.size();i++){labels.get(i).setTextColor(i==index?ToolPage.ACCENT:ToolPage.MUTED);markers.get(i).setBackgroundColor(i==index?ToolPage.ACCENT:Color.TRANSPARENT);}
  View tab=(View)labels.get(index).getParent();tabs.post(()->tabs.smoothScrollTo(Math.max(0,tab.getLeft()-(tabs.getWidth()-tab.getWidth())/2),0));
  loadNearby(index);
 }
 private void loadNearby(int index){
  int token=++generation;
  for(int i=0;i<pictures.size();i++)if(Math.abs(i-index)>1)pictures.get(i).setImageDrawable(null);
  // Retain the four-page union of adjacent pages and their neighbours for reversal.
  // Populate the centre first, then neighbours on separate frames. Never rebuild the page.
  render(index,token);if(index>0)pager.postOnAnimation(()->render(index-1,token));if(index+1<pictures.size())pager.postOnAnimation(()->render(index+1,token));
 }
 private void render(int index,int token){
  if(released||token!=generation)return;
  Bitmap bitmap=cache.get(index);
  if(bitmap==null){
   Formula sample=new Formula();sample.insertExpression("Sqrt(2)/2+Sqrt(2+Sqrt(2+Sqrt(2+Sqrt(2))))");
   CalculatorView calculator=new CalculatorView(app,sample,new CalculatorView.Listener(){public void press(String action){}public void copyResult(){}});
   calculator.skinPreview=true;calculator.blinkCursor=false;calculator.setTheme(index);calculator.result="2.697476235";calculator.setFeedback(false,false);
   calculator.layout(0,0,pictureWidth,pictureHeight);bitmap=Bitmap.createBitmap(pictureWidth,pictureHeight,Bitmap.Config.ARGB_8888);calculator.draw(new Canvas(bitmap));cache.put(index,bitmap);
  }
  pictures.get(index).setImageBitmap(bitmap);
 }
 private final class Pager extends HorizontalScrollView {
  final int pageWidth;private int downPage;private float downX,downY;private boolean dragging,didFling,touching;
  private final Runnable settled=new Runnable(){int previous=-1;public void run(){if(released||touching)return;int x=getScrollX();if(x!=previous){previous=x;postDelayed(this,32);return;}previous=-1;int index=Math.max(0,Math.min(pictures.size()-1,Math.round(x/(float)pageWidth)));if(index!=selected)updateSelected(index);}};
  Pager(android.content.Context context,int width){super(context);pageWidth=width;setHorizontalScrollBarEnabled(false);setOverScrollMode(OVER_SCROLL_NEVER);setSmoothScrollingEnabled(true);}
  void select(int index){updateSelected(index);smoothScrollTo(index*pageWidth,0);}
  @Override public void fling(int velocity){didFling=true;int index=Math.round(getScrollX()/(float)pageWidth);if(Math.abs(velocity)>ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity()*3)index=downPage+(velocity>0?1:-1);select(Math.max(0,Math.min(pictures.size()-1,index)));}
  @Override public boolean onInterceptTouchEvent(MotionEvent event){
   if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downPage=Math.round(getScrollX()/(float)pageWidth);downX=event.getX();downY=event.getY();dragging=false;didFling=false;touching=true;removeCallbacks(settled);}
   if(event.getActionMasked()==MotionEvent.ACTION_MOVE&&Math.abs(event.getX()-downX)>ViewConfiguration.get(getContext()).getScaledTouchSlop()&&Math.abs(event.getX()-downX)>Math.abs(event.getY()-downY)){dragging=true;getParent().requestDisallowInterceptTouchEvent(true);}
   return super.onInterceptTouchEvent(event);
  }
  @Override public boolean onTouchEvent(MotionEvent event){boolean handled=super.onTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){touching=false;int nearest=Math.max(0,Math.min(pictures.size()-1,Math.round(getScrollX()/(float)pageWidth)));if(!didFling||event.getActionMasked()==MotionEvent.ACTION_CANCEL)select(nearest);getParent().requestDisallowInterceptTouchEvent(false);}return handled;}
  @Override protected void onScrollChanged(int x,int y,int oldX,int oldY){super.onScrollChanged(x,y,oldX,oldY);removeCallbacks(settled);postDelayed(settled,80);}
  @Override protected void onDetachedFromWindow(){removeCallbacks(settled);super.onDetachedFromWindow();}
 }
}
