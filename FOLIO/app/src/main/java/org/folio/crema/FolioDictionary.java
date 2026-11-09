package org.folio.crema;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** PHILOSOPHIA GRAND v2 SQLite, embedded in APK as one compressed asset. */
final class FolioDictionary {
    static final long EXPECTED_BYTES=393191424L;
    static final long REQUIRED_SENSES=1793921L;
    static final long EXPECTED_HEADS=1356788L;
    interface Progress {void update(int percentage);}
    static final class Entry {
        final String word,lang,pos,definition,source;
        Entry(Cursor c){word=c.getString(0);lang=c.getString(1);pos=c.getString(2);definition=c.getString(3);source=c.getString(4);}
    }
    private final Context ctx;
    private SQLiteDatabase db;
    FolioDictionary(Context context){ctx=context.getApplicationContext();}
    synchronized boolean ready(){return db!=null&&db.isOpen();}
    synchronized void close(){if(db!=null){db.close();db=null;}}
    private static String normalized(String text){
        String s=Normalizer.normalize(text.toLowerCase(Locale.US),Normalizer.Form.NFKD);
        StringBuilder r=new StringBuilder();
        for(int i=0;i<s.length();i++){
            int type=Character.getType(s.charAt(i));
            if(type!=Character.NON_SPACING_MARK&&type!=Character.COMBINING_SPACING_MARK&&type!=Character.ENCLOSING_MARK)r.append(s.charAt(i));
        }
        return r.toString().trim();
    }
    private static String meta(SQLiteDatabase database,String key){
        Cursor cur=database.rawQuery("SELECT value FROM metadata WHERE key=?",new String[]{key});
        try{return cur.moveToFirst()?cur.getString(0):"";}finally{cur.close();}
    }
    private static SQLiteDatabase valid(File file) throws Exception {
        SQLiteDatabase check=SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,
            SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        try {
            if(!"1".equals(meta(check,"schema_version"))||
                Long.parseLong(meta(check,"sense_count"))!=REQUIRED_SENSES||
                Long.parseLong(meta(check,"headword_count"))!=EXPECTED_HEADS)throw new IOException("Unexpected dictionary data");
            Cursor c=check.rawQuery("PRAGMA quick_check(1)",null);
            try{if(!c.moveToFirst()||!"ok".equals(c.getString(0)))throw new IOException("SQLite quick_check failed");}
            finally{c.close();}
            return check;
        }catch(Exception e){check.close();throw e;}
    }
    /** Called off UI thread. Unpack only when dictionary is used, and only once. */
    void initialize(Progress callback) throws Exception {
        File target=new File(ctx.getFilesDir(),"philosophia.sqlite");
        File partial=new File(ctx.getFilesDir(),"philosophia.sqlite.partial");
        if(target.exists()&&target.length()==EXPECTED_BYTES){
            try{SQLiteDatabase good=valid(target);synchronized(this){close();db=good;}callback.update(100);return;}
            catch(Exception ignored){}
        }
        if(ctx.getFilesDir().getUsableSpace()<EXPECTED_BYTES+32L*1024*1024)
            throw new IOException("Not enough storage. Free approximately 425 MB of internal storage in addition to the installed APK.");
        if(partial.exists()&&!partial.delete())throw new IOException("Cannot clear temporary database");
        long written=0;int shown=-5;
        try(InputStream input=new GZIPInputStream(new BufferedInputStream(ctx.getAssets().open("philosophia.lexpack"),65536),65536);
            FileOutputStream output=new FileOutputStream(partial)){
            byte[] buf=new byte[65536];int amount;
            while((amount=input.read(buf))!=-1){
                written+=amount;
                if(written>EXPECTED_BYTES)throw new IOException("Dictionary data exceeds known size");
                output.write(buf,0,amount);
                int percent=(int)((written*95L)/EXPECTED_BYTES);
                if(percent>=shown+5){shown=percent;callback.update(percent);}
            }
            output.getFD().sync();
        }catch(Exception ex){partial.delete();throw ex;}
        if(written!=EXPECTED_BYTES){partial.delete();throw new IOException("Incomplete dictionary: "+written+" bytes");}
        SQLiteDatabase check=valid(partial);check.close();
        if(target.exists()&&!target.delete())throw new IOException("Cannot replace damaged dictionary");
        if(!partial.renameTo(target))throw new IOException("Cannot activate database");
        synchronized(this){close();db=valid(target);}
        callback.update(100);
    }
    synchronized ArrayList<Entry> lookup(String query,boolean onlyPhilosophy){
        ArrayList<Entry> out=new ArrayList<Entry>();if(db==null||!db.isOpen())return out;
        String norm=normalized(query);if(norm.isEmpty())return out;
        String cols="word,lang,pos,definition,source";
        String extra=onlyPhilosophy?" AND priority>=25":"";
        Cursor c=db.rawQuery("SELECT "+cols+" FROM entries WHERE lookup=?"+extra+" ORDER BY priority DESC LIMIT 30",new String[]{norm});
        try{while(c.moveToNext())out.add(new Entry(c));}finally{c.close();}
        if(out.size()<12){
            c=db.rawQuery("SELECT "+cols+" FROM entries WHERE lookup>? AND lookup<?"+extra+
                " ORDER BY lookup,priority DESC LIMIT ?",new String[]{norm,norm+"\uffff",String.valueOf(28-out.size())});
            try{while(c.moveToNext())out.add(new Entry(c));}finally{c.close();}
        }
        return out;
    }
}
