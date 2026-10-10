package com.az.touchprobe;

import android.app.Instrumentation;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.graphics.Bitmap;
import java.io.File;
import java.io.FileOutputStream;
import org.json.*;

/** Inject actual simultaneous pointers through Android's test automation API. */
public final class Probe extends Instrumentation {
    private Bundle arguments;
    @Override public void onCreate(Bundle args) { super.onCreate(args); arguments=args; start(); }
    @Override public void onStart() {
        Bundle output=new Bundle();
        try {
            JSONArray steps=new JSONArray(new String(android.util.Base64.decode(arguments.getString("events"),0),java.nio.charset.StandardCharsets.UTF_8));
            long downTime=SystemClock.uptimeMillis();
            JSONArray trace=new JSONArray();
            for(int i=0;i<steps.length();i++) {
                JSONObject step=steps.getJSONObject(i);
                SystemClock.sleep(step.optLong("wait",0));
                if(step.has("shot")) {
                    File file=new File(getContext().getExternalFilesDir(null),step.getString("shot")+".png");
                    Bitmap bitmap=getUiAutomation().takeScreenshot();
                    try(FileOutputStream stream=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}
                    bitmap.recycle(); continue;
                }
                if(!step.has("action"))continue;
                int action=step.getInt("action");
                JSONArray pointers=step.getJSONArray("pointers");
                MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[pointers.length()];
                MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[pointers.length()];
                for(int j=0;j<pointers.length();j++) {
                    JSONArray pointer=pointers.getJSONArray(j);
                    properties[j]=new MotionEvent.PointerProperties();properties[j].id=pointer.getInt(0);properties[j].toolType=MotionEvent.TOOL_TYPE_FINGER;
                    coords[j]=new MotionEvent.PointerCoords();coords[j].x=(float)pointer.getDouble(1);coords[j].y=(float)pointer.getDouble(2);coords[j].pressure=1;coords[j].size=.1f;
                }
                long now=SystemClock.uptimeMillis();if(action==0)downTime=now;
                trace.put(new JSONObject().put("action",action).put("eventTime",now).put("wallTime",System.currentTimeMillis()));
                MotionEvent event=MotionEvent.obtain(downTime,now,action,pointers.length(),properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
                if(!getUiAutomation().injectInputEvent(event,false))throw new IllegalStateException("Rejected event "+i);
                event.recycle();
            }
            output.putString("eventsTime",trace.toString());output.putString("result","OK"); finish(-1,output);
        } catch(Exception error) {output.putString("error",error.toString());finish(0,output);}
    }
}
