package com.az.scicalc;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.util.*;

/** All access runs on the history executor. Payloads keep the original export format. */
final class HistoryStore extends SQLiteOpenHelper {
    private final IdentityHashMap<JSONObject,Long> ids=new IdentityHashMap<>();
    HistoryStore(Context context){super(context,"history.db",null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT, data TEXT NOT NULL)");
        db.execSQL("CREATE TABLE metadata (name TEXT PRIMARY KEY, value TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("未知历史数据库版本");}
    List<JSONObject> load(String legacy) throws JSONException {
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            boolean migrated;
            try(Cursor c=db.rawQuery("SELECT value FROM metadata WHERE name='legacyMigrated'",null)){migrated=c.moveToFirst();}
            if(!migrated){
                JSONArray old=new JSONArray(legacy);
                for(int i=old.length()-1;i>=0;i--)insert(db,old.getJSONObject(i));
                db.execSQL("INSERT INTO metadata VALUES ('legacyMigrated','1')");
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        ids.clear();List<JSONObject> records=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT id,data FROM records ORDER BY id DESC",null)){
            while(c.moveToNext()){JSONObject record=new JSONObject(c.getString(1));ids.put(record,c.getLong(0));records.add(record);}
        }
        return records;
    }
    private long insert(SQLiteDatabase db,JSONObject record){ContentValues data=new ContentValues();data.put("data",record.toString());return db.insertOrThrow("records",null,data);}
    void add(JSONObject record,int limit){
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{ids.put(record,insert(db,record));db.execSQL("DELETE FROM records WHERE id NOT IN (SELECT id FROM records ORDER BY id DESC LIMIT ?)",new Object[]{limit});db.setTransactionSuccessful();}
        finally{db.endTransaction();}
        // Release identities of pruned records as well as their payloads.
        long oldest=Long.MIN_VALUE;try(Cursor c=db.rawQuery("SELECT id FROM records ORDER BY id DESC LIMIT 1 OFFSET ?",new String[]{Integer.toString(limit-1)})){if(c.moveToFirst())oldest=c.getLong(0);}
        final long cutoff=oldest;ids.values().removeIf(id->id<cutoff);
    }
    void remove(JSONObject record){Long id=ids.get(record);if(id==null)throw new IllegalStateException("未找到历史记录");getWritableDatabase().delete("records","id=?",new String[]{id.toString()});ids.remove(record);}
    void clear(){getWritableDatabase().delete("records",null,null);ids.clear();}
}
