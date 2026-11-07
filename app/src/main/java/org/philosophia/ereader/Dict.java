package org.philosophia.ereader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Single-package, entirely offline lexicon.
 * The APK carries philosophia.sqlite.gz in its assets. On FIRST launch the
 * compressed asset is streamed to private app storage without network access,
 * a file picker, or an additional data download. Subsequent launches open
 * the already installed SQLite database directly. Uses only Android API 19.
 */
final class Dict {
    static final class Entry {
        long id;
        String word,language,pos,definition,tradition,source;
        int priority;
        Entry(Cursor c) {
            id=c.getLong(0);word=c.getString(1);language=c.getString(2);
            pos=c.getString(3);definition=c.getString(4);
            tradition=c.getString(5);source=c.getString(6);
            priority=c.getInt(7);
        }
    }
    interface Progress { void update(int percent); }
    static final String ASSET="philosophia.sqlite.gz";
    static final long EXPECTED_BYTES=393191424L;
    static final int REQUIRED_SENSES=1000000;
    private static final String COLS="id,word,lang,pos,definition,tradition,source,priority";
    private final Context context;
    private SQLiteDatabase db;
    String path="";
    String warning="";
    Dict(Context c) { context=c.getApplicationContext(); }

    private static String normal(String q) {
        String s=Normalizer.normalize(q.toLowerCase(Locale.US),Normalizer.Form.NFKD);
        StringBuilder b=new StringBuilder();
        for(int i=0;i<s.length();i++) {
            int t=Character.getType(s.charAt(i));
            if(t!=Character.NON_SPACING_MARK && t!=Character.COMBINING_SPACING_MARK
                && t!=Character.ENCLOSING_MARK)b.append(s.charAt(i));
        }
        return b.toString().trim();
    }
    synchronized void close() { if(db!=null){db.close();db=null;} }
    private static boolean looksValid(SQLiteDatabase check) {
        Cursor c=check.rawQuery("SELECT value FROM metadata WHERE key='schema_version'",null);
        try {if(!c.moveToFirst()||!"1".equals(c.getString(0)))return false;}
        finally {c.close();}
        c=check.rawQuery("SELECT "+COLS+" FROM entries LIMIT 1",null);
        try {return c.moveToFirst();}finally {c.close();}
    }
    private static int actualCount(SQLiteDatabase check) {
        Cursor c=check.rawQuery("SELECT value FROM metadata WHERE key='sense_count'",null);
        try {return c.moveToFirst()?Integer.parseInt(c.getString(0)):0;}
        finally {c.close();}
    }
    private static SQLiteDatabase readAndCheck(File f) throws Exception {
        SQLiteDatabase candidate=SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,
            SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        try {
            if(!looksValid(candidate)||actualCount(candidate)<REQUIRED_SENSES)
                throw new IOException("The embedded dictionary has an incompatible schema or too few definitions.");
            return candidate;
        }catch(Exception e){candidate.close();throw e;}
    }
    /** Called only on a background thread, before displaying the search page. */
    void prepareEmbedded(Progress progress) throws Exception {
        final File target=new File(context.getFilesDir(),"philosophia.sqlite");
        final File temp=new File(context.getFilesDir(),"philosophia.sqlite.tmp");
        // Reopen the prior installation on normal launches; no repeat unpacking.
        if(target.isFile()&&target.length()==EXPECTED_BYTES) {
            try {
                SQLiteDatabase ready=readAndCheck(target);
                synchronized(this){close();db=ready;path=target.getAbsolutePath();warning="";}
                progress.update(100);
                return;
            }catch(Exception e){warning="Reinstalling damaged bundled dictionary: "+e.getMessage();}
        }
        if(temp.exists()&&!temp.delete())throw new IOException("Cannot clear unfinished dictionary setup.");
        // Keep enough free internal storage for the fully expanded DB + filesystem overhead.
        long available=context.getFilesDir().getUsableSpace();
        if(available<EXPECTED_BYTES+32L*1024*1024)
            throw new IOException("Not enough free internal storage. PHILOSOPHIA needs approximately 425 MB free after the APK is installed. Available: "+
                    (available/(1024*1024))+" MB. Free space and reopen the app.");
        InputStream asset=null;
        GZIPInputStream unzip=null;
        FileOutputStream output=null;
        long written=0;
        int last=-1;
        try {
            asset=new BufferedInputStream(context.getAssets().open(ASSET),65536);
            unzip=new GZIPInputStream(asset,65536);
            output=new FileOutputStream(temp);
            byte[] buffer=new byte[65536];
            int n;
            while((n=unzip.read(buffer))!=-1) {
                output.write(buffer,0,n);
                written+=n;
                if(written>EXPECTED_BYTES)
                    throw new IOException("Bundled data exceeds the verified dictionary size.");
                int percent=(int)Math.min(95,(written*95)/EXPECTED_BYTES);
                // Keep screen refreshes infrequent on e-ink.
                if(percent>=last+5){last=percent;progress.update(percent);}
            }
            output.getFD().sync();
        }catch(Exception e){temp.delete();throw e;}
        finally {
            if(output!=null)try{output.close();}catch(Exception ignored){}
            if(unzip!=null)try{unzip.close();}catch(Exception ignored){}
            else if(asset!=null)try{asset.close();}catch(Exception ignored){}
        }
        if(written!=EXPECTED_BYTES){temp.delete();throw new IOException("Incomplete embedded dictionary ("+written+" bytes).");}
        progress.update(96);
        try {
            SQLiteDatabase verified=readAndCheck(temp);
            try {
                Cursor check=verified.rawQuery("PRAGMA quick_check(1)",null);
                try{if(!check.moveToFirst()||!"ok".equalsIgnoreCase(check.getString(0)))
                    throw new IOException("SQLite consistency check failed.");}
                finally{check.close();}
            }finally{verified.close();}
            if(target.exists()&&!target.delete())throw new IOException("Cannot replace the old database.");
            if(!temp.renameTo(target))throw new IOException("Cannot activate the bundled dictionary.");
            SQLiteDatabase ready=readAndCheck(target);
            synchronized(this){close();db=ready;path=target.getAbsolutePath();warning="";}
            progress.update(100);
        }catch(Exception e){temp.delete();throw e;}
    }
    synchronized String meta(String name) {
        if(db==null)return "0";
        Cursor c=db.rawQuery("SELECT value FROM metadata WHERE key=?",new String[]{name});
        try{return c.moveToFirst()?c.getString(0):"0";}finally{c.close();}
    }
    private void add(List<Entry> list,String sql,String...args) {
        Cursor c=db.rawQuery(sql,args);
        try{while(c.moveToNext())list.add(new Entry(c));}
        finally{c.close();}
    }
    synchronized List<Entry> search(String query,boolean philosophyOnly) {
        List<Entry> items=new ArrayList<Entry>();
        if(db==null)return items;
        String q=normal(query);
        if(q.length()==0) {
            add(items,"SELECT "+COLS+" FROM entries WHERE priority>=100 ORDER BY lookup LIMIT 60");
            return items;
        }
        String filter=philosophyOnly?" AND priority>=25":"";
        add(items,"SELECT "+COLS+" FROM entries WHERE lookup=?"+filter+" ORDER BY priority DESC LIMIT 15",q);
        add(items,"SELECT "+COLS+" FROM entries WHERE lookup>? AND lookup<?"+filter+
             " ORDER BY lookup ASC,priority DESC LIMIT 45",q,q+"\uffff");
        return items;
    }
}
