package com.az.scicalc;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.provider.OpenableColumns;import java.io.*;
/** Only explicitly granted files in the calculator's image cache are exposed. */
public final class ImageProvider extends ContentProvider {
 public boolean onCreate(){return true;}public String getType(Uri uri){return "image/jpeg";}
 private File file(Uri uri) throws FileNotFoundException{String name=uri.getLastPathSegment();if(!"camera.jpg".equals(name)&&!"clipboard.jpg".equals(name))throw new FileNotFoundException();return new File(getContext().getCacheDir(),name);}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.parseMode(mode));}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File f=file(uri);MatrixCursor c=new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE});c.addRow(new Object[]{f.getName(),f.length()});return c;}catch(Exception e){return null;}}
 public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}public int delete(Uri uri,String s,String[] args){return 0;}public int update(Uri uri,ContentValues v,String s,String[] args){return 0;}
}
