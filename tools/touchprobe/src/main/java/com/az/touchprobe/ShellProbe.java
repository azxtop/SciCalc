package com.az.touchprobe;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.view.MotionEvent;
import java.lang.reflect.Method;
import org.json.*;

/** Shell-only multi-pointer injector. Runs with app_process; no APK install. */
public final class ShellProbe {
    public static void main(String[] args) throws Exception {
        Class<?> managerClass;
        try { managerClass=Class.forName("android.hardware.input.InputManagerGlobal"); }
        catch (ClassNotFoundException e) { managerClass=Class.forName("android.hardware.input.InputManager"); }
        Object manager=managerClass.getMethod("getInstance").invoke(null);
        Method inject=managerClass.getMethod("injectInputEvent",InputEvent.class,int.class);
        byte[] bytes=args[0].startsWith("@")?java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(args[0].substring(1))):android.util.Base64.decode(args[0],0);
        JSONArray steps=new JSONArray(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
        long downTime=SystemClock.uptimeMillis();
        JSONArray trace=new JSONArray();
        for(int i=0;i<steps.length();i++) {
            JSONObject step=steps.getJSONObject(i);
            SystemClock.sleep(step.optLong("wait",0));
            if(step.has("key")){
                long now=SystemClock.uptimeMillis();int code=step.getInt("key");
                boolean accepted=true;
                for(int action:new int[]{KeyEvent.ACTION_DOWN,KeyEvent.ACTION_UP}){
                    KeyEvent key=new KeyEvent(now,now,action,code,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,KeyEvent.FLAG_FROM_SYSTEM,InputDevice.SOURCE_KEYBOARD);
                    accepted&=(boolean)inject.invoke(manager,key,0);
                }
                trace.put(new JSONObject().put("key",code).put("eventTime",now).put("accepted",accepted));
                if(!accepted)throw new IllegalStateException("Rejected key "+i);
                continue;
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
            MotionEvent event=MotionEvent.obtain(downTime,now,action,pointers.length(),properties,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
            boolean accepted=(boolean)inject.invoke(manager,event,0);
            trace.put(new JSONObject().put("action",action).put("eventTime",now).put("accepted",accepted));
            event.recycle();if(!accepted)throw new IllegalStateException("Rejected event "+i);
        }
        System.out.println(trace);
    }
}
