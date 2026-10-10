package com.az.scicalc;

import android.content.SharedPreferences;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TabPreferencesTest {
    static final class Memory implements SharedPreferences {
        final Map<String,Object> data=new HashMap<>();
        public Map<String,?> getAll(){return new HashMap<>(data);}public boolean contains(String k){return data.containsKey(k);}
        public String getString(String k,String d){return (String)data.getOrDefault(k,d);}public int getInt(String k,int d){return (Integer)data.getOrDefault(k,d);}public long getLong(String k,long d){return (Long)data.getOrDefault(k,d);}public float getFloat(String k,float d){return (Float)data.getOrDefault(k,d);}public boolean getBoolean(String k,boolean d){return (Boolean)data.getOrDefault(k,d);}
        @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){return (Set<String>)data.getOrDefault(k,d);}
        public Editor edit(){return new Editor(){final Map<String,Object> pending=new HashMap<>();final Set<String> removed=new HashSet<>();boolean clear;
            private Editor put(String k,Object v){pending.put(k,v);return this;}public Editor putString(String k,String v){return put(k,v);}public Editor putInt(String k,int v){return put(k,v);}public Editor putLong(String k,long v){return put(k,v);}public Editor putFloat(String k,float v){return put(k,v);}public Editor putBoolean(String k,boolean v){return put(k,v);}public Editor putStringSet(String k,Set<String>v){return put(k,v);}public Editor remove(String k){removed.add(k);return this;}public Editor clear(){clear=true;return this;}public boolean commit(){if(clear)data.clear();for(String k:removed)data.remove(k);data.putAll(pending);return true;}public void apply(){commit();}};}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
    }
    @Test public void legacyDataMovesToFirstPageWithoutLeakingIntoBlankPage(){Memory root=new Memory();root.edit().putString("grid:MatA","[[1]]").putInt("digits",7).putInt("skin",8).apply();TabPreferences first=new TabPreferences(root,"first"),second=new TabPreferences(root,"second");first.importLegacy();assertEquals("[[1]]",first.getString("grid:MatA",""));assertEquals("",second.getString("grid:MatA",""));assertEquals(11,second.getInt("digits",11));assertEquals(8,second.getInt("skin",0));assertEquals("[[1]]",root.getString("grid:MatA",""));}
    @Test public void duplicatedDataCanBeEditedIndependently(){Memory root=new Memory();TabPreferences a=new TabPreferences(root,"a"),b=new TabPreferences(root,"b");a.edit().putString("grid:MatA","[[1]]").putString("stat:one","[[3,1]]").putString("coefficients:2","[[1,2,3]]").apply();b.copyFrom(a);b.edit().putString("grid:MatA","[[9]]").remove("stat:one").apply();assertEquals("[[1]]",a.getString("grid:MatA",""));assertEquals("[[9]]",b.getString("grid:MatA",""));assertTrue(a.contains("stat:one"));assertFalse(b.contains("stat:one"));assertEquals("[[1,2,3]]",b.getString("coefficients:2",""));}
    @Test public void newPageInheritsFormatAndKeepsLaterChangesLocal(){Memory root=new Memory();TabPreferences a=new TabPreferences(root,"a"),b=new TabPreferences(root,"b");a.edit().putString("numberFormat","scientific").putInt("digits",8).putBoolean("scientificE",true).putString("grid:MatB","[[4]]").apply();b.inheritFormat(a);b.edit().putInt("digits",3).apply();assertEquals("scientific",b.getString("numberFormat",""));assertTrue(b.getBoolean("scientificE",false));assertEquals(8,a.getInt("digits",0));assertEquals(3,b.getInt("digits",0));assertFalse(b.contains("grid:MatB"));}
    @Test public void clearingPageDataDoesNotClearOtherPagesOrSharedSettings(){Memory root=new Memory();TabPreferences a=new TabPreferences(root,"a"),b=new TabPreferences(root,"b");a.edit().putString("tableF","x").putInt("skin",4).apply();b.edit().putString("tableF","x^2").apply();a.edit().clear().apply();assertFalse(a.contains("tableF"));assertEquals("x^2",b.getString("tableF",""));assertEquals(4,a.getInt("skin",0));assertFalse(a.getAll().containsKey("tab:b:data:tableF"));}
}
