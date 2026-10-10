package com.az.scicalc;

import android.content.SharedPreferences;
import java.util.*;

/** Named data and number format belong to a calculation page; app appearance stays shared. */
final class TabPreferences implements SharedPreferences {
    final SharedPreferences root;final String prefix;
    TabPreferences(SharedPreferences root,String id){this.root=root;prefix="tab:"+id+":data:";}
    static boolean scoped(String key){return key.startsWith("grid:")||key.startsWith("stat:")||key.startsWith("coefficients:")||Arrays.asList("statRegression","baseRadix","baseBits","tableF","tableG","tableLo","tableHi","tableStep","tableResults","numberFormat","digits","scientificE").contains(key);}
    private String key(String key){return scoped(key)?prefix+key:key;}
    void importLegacy(){Editor e=edit();for(Map.Entry<String,?> value:root.getAll().entrySet())if(scoped(value.getKey()))put(e,value.getKey(),value.getValue());e.apply();}
    void copyFrom(TabPreferences source){Editor e=edit();for(Map.Entry<String,?> value:source.root.getAll().entrySet())if(value.getKey().startsWith(source.prefix))put(e,value.getKey().substring(source.prefix.length()),value.getValue());e.apply();}
    void inheritFormat(TabPreferences source){Editor e=edit();for(String k:new String[]{"numberFormat","digits","scientificE"})if(source.contains(k))put(e,k,source.getAll().get(k));e.apply();}
    @SuppressWarnings("unchecked") private static void put(Editor e,String key,Object value){if(value instanceof String)e.putString(key,(String)value);else if(value instanceof Boolean)e.putBoolean(key,(Boolean)value);else if(value instanceof Integer)e.putInt(key,(Integer)value);else if(value instanceof Long)e.putLong(key,(Long)value);else if(value instanceof Float)e.putFloat(key,(Float)value);else if(value instanceof Set)e.putStringSet(key,(Set<String>)value);}
    public Map<String,?> getAll(){Map<String,Object> values=new HashMap<>();for(Map.Entry<String,?> e:root.getAll().entrySet()){String k=e.getKey();if(k.startsWith(prefix))values.put(k.substring(prefix.length()),e.getValue());else if(!k.startsWith("tab:")&&!scoped(k))values.put(k,e.getValue());}return values;}
    public String getString(String k,String d){return root.getString(key(k),d);}public Set<String> getStringSet(String k,Set<String>d){return root.getStringSet(key(k),d);}
    public int getInt(String k,int d){return root.getInt(key(k),d);}public long getLong(String k,long d){return root.getLong(key(k),d);}public float getFloat(String k,float d){return root.getFloat(key(k),d);}public boolean getBoolean(String k,boolean d){return root.getBoolean(key(k),d);}public boolean contains(String k){return root.contains(key(k));}
    public Editor edit(){return new Editor(){final Editor delegate=root.edit();public Editor putString(String k,String v){delegate.putString(key(k),v);return this;}public Editor putStringSet(String k,Set<String>v){delegate.putStringSet(key(k),v);return this;}public Editor putInt(String k,int v){delegate.putInt(key(k),v);return this;}public Editor putLong(String k,long v){delegate.putLong(key(k),v);return this;}public Editor putFloat(String k,float v){delegate.putFloat(key(k),v);return this;}public Editor putBoolean(String k,boolean v){delegate.putBoolean(key(k),v);return this;}public Editor remove(String k){delegate.remove(key(k));return this;}public Editor clear(){for(String k:root.getAll().keySet())if(k.startsWith(prefix))delegate.remove(k);return this;}public boolean commit(){return delegate.commit();}public void apply(){delegate.apply();}};}
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){root.registerOnSharedPreferenceChangeListener(l);}public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){root.unregisterOnSharedPreferenceChangeListener(l);}
}
