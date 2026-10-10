package com.az.scicalc;
import android.app.*;import android.content.*;import android.graphics.*;import android.net.Uri;import android.provider.MediaStore;import android.view.*;import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognizer;
import java.util.*;

/** Bundled on-device OCR with crop, rotation, and editable recognition output. */
final class MathOcr {
 final MainActivity app;static final int CAMERA=4201,PICK=4202;private TextRecognizer recognizer;
 MathOcr(MainActivity app){this.app=app;}
 void open(){ReferenceDialogs.choose(app,"识别数学表达式",new String[]{"拍照","从图片选择"},i->{try{if(i==0){Uri uri=Uri.parse("content://"+app.getPackageName()+".images/camera.jpg");Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("拍照",uri));app.startActivityForResult(intent,CAMERA);}else app.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),PICK);}catch(Exception e){app.toast("无法打开图片来源："+e.getMessage());}});}
 void result(int request,int code,Intent data){if(code!=Activity.RESULT_OK)return;if(request==CAMERA)image(Uri.parse("content://"+app.getPackageName()+".images/camera.jpg"));else if(request==PICK&&data!=null)image(data.getData());}
 void image(Uri uri){if(uri==null)return;app.worker.execute(()->{try{ImageDecoder.Source source=ImageDecoder.createSource(app.getContentResolver(),uri);Bitmap bitmap=ImageDecoder.decodeBitmap(source,(decoder,info,s)->{decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);int size=Math.max(info.getSize().getWidth(),info.getSize().getHeight());if(size>2400)decoder.setTargetSampleSize((size+2399)/2400);});app.main.post(()->crop(bitmap));}catch(Throwable e){app.main.post(()->app.toast("读取图片失败："+e.getMessage()));}});}
 private void crop(Bitmap bitmap){ToolPage p=new ToolPage(app,"裁剪与旋转",app.menuTools::back);p.note("拖动框选公式区域，旋转后再识别。识别结果可以修改。");CropView crop=new CropView(app,bitmap);p.body.addView(crop,new LinearLayout.LayoutParams(-1,p.dp(340)));p.button(p.body,"旋转 90°",crop::rotate);p.button(p.body,"重置选区",()->{crop.selection=null;crop.invalidate();});TextView status=p.note("");p.button(p.body,"识别选区",()->{status.setText("识别中…");if(recognizer==null)recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);recognizer.process(InputImage.fromBitmap(crop.selected(),0)).addOnSuccessListener(text->{status.setText("");review(text);}).addOnFailureListener(e->status.setText("识别失败："+e.getMessage()));});app.menuTools.show(p);}
 static String normalize(String text){String s=text.replace('−','-').replace('—','-').replace('×','*').replace('·','*').replace('÷','/').replace('＝','=').replace('（','(').replace('）',')').replace("π","Pi").replace("∞","Infinity").replace("√","Sqrt").replace("²","^2").replace("³","^3").replace("⁴","^4").replace("⁵","^5").replace("⁶","^6").replace("⁷","^7").replace("⁸","^8").replace("⁹","^9");s=s.replaceAll("(?<=\\d)\\s*[xX]\\s*(?=\\d)","*");s=s.replaceAll("(?i)\\bsin\\b","Sin").replaceAll("(?i)\\bcos\\b","Cos").replaceAll("(?i)\\btan\\b","Tan").replaceAll("(?i)\\bln\\b","Log");return s.trim();}
 private void review(Text recognized){ToolPage p=new ToolPage(app,"识别结果",app.menuTools::back);String source=normalize(recognized.getText());EditText input=p.input(p.body,"修改表达式",source,true);p.note("检查分数、根号、上下标和换行；多行结果请选取需要的一条公式。");p.button(p.body,"插入公式",()->{String s=input.getText().toString().trim();if(s.isEmpty()){input.setError("未识别到公式，请重新拍照或填写");return;}app.checkpoint();app.formula.insertExpression(s);app.changed();app.menuTools.home();});app.menuTools.show(p);}
 void close(){if(recognizer!=null)recognizer.close();}
 static final class CropView extends View {
  Bitmap bitmap;RectF selection,display=new RectF();final Paint paint=new Paint(3);float startX,startY;
  CropView(Context c,Bitmap bitmap){super(c);this.bitmap=bitmap;}
  protected void onDraw(Canvas c){c.drawColor(0xff12171b);float scale=Math.min(getWidth()/(float)bitmap.getWidth(),getHeight()/(float)bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;display.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);c.drawBitmap(bitmap,null,display,paint);if(selection!=null){paint.setColor(0xffb18eff);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);c.drawRect(selection,paint);paint.setStyle(Paint.Style.FILL);}}
  public boolean onTouchEvent(MotionEvent e){float x=Math.max(display.left,Math.min(display.right,e.getX())),y=Math.max(display.top,Math.min(display.bottom,e.getY()));if(e.getAction()==0){startX=x;startY=y;getParent().requestDisallowInterceptTouchEvent(true);}selection=new RectF(Math.min(startX,x),Math.min(startY,y),Math.max(startX,x),Math.max(startY,y));invalidate();if(e.getAction()==1)getParent().requestDisallowInterceptTouchEvent(false);return true;}
  void rotate(){Matrix m=new Matrix();m.postRotate(90);bitmap=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);selection=null;invalidate();}
  Bitmap selected(){if(selection==null||selection.width()<10||selection.height()<10)return bitmap;float scale=bitmap.getWidth()/display.width();int x=Math.max(0,(int)((selection.left-display.left)*scale)),y=Math.max(0,(int)((selection.top-display.top)*scale));int w=Math.min(bitmap.getWidth()-x,Math.max(1,(int)(selection.width()*scale))),h=Math.min(bitmap.getHeight()-y,Math.max(1,(int)(selection.height()*scale)));return Bitmap.createBitmap(bitmap,x,y,w,h);}
 }
}
