package org.philosophia.ereader;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Storage, import and indexed search restricted to Android 4.4-era APIs. */
final class Dict {
    static final class Entry {
        long id;
        String word,language,pos,definition,tradition,source;
        int priority;
        Entry(Cursor c) {
            id=c.getLong(0); word=c.getString(1); language=c.getString(2);
            pos=c.getString(3); definition=c.getString(4);
            tradition=c.getString(5); source=c.getString(6);
            priority=c.getInt(7);
        }
    }
    private static final String COLS="id,word,lang,pos,definition,tradition,source,priority";
    private final Context context;
    private SQLiteDatabase db;
    boolean starter=true;
    String path="";
    String warning="";

    Dict(Context c) { context=c.getApplicationContext(); open(); }
    private static String normal(String q) {
        String s=Normalizer.normalize(q.toLowerCase(Locale.US),Normalizer.Form.NFKD);
        StringBuilder b=new StringBuilder();
        for(int i=0;i<s.length();i++) {
            int t=Character.getType(s.charAt(i));
            if(t!=Character.NON_SPACING_MARK && t!=Character.COMBINING_SPACING_MARK
               && t!=Character.ENCLOSING_MARK) b.append(s.charAt(i));
        }
        return b.toString().trim();
    }

    synchronized void close() { if(db!=null) {db.close();db=null;} }
    private File seed() throws Exception {
        File target=new File(context.getFilesDir(),"seed.sqlite");
        if(target.isFile()&&target.length()>4096)return target;
        InputStream in=context.getAssets().open("demo.sqlite");
        FileOutputStream out=new FileOutputStream(target);
        try { byte[] buf=new byte[32768]; int n;
              while((n=in.read(buf))!=-1)out.write(buf,0,n); }
        finally {try{in.close();}finally{out.close();}}
        return target;
    }
    synchronized void open() {
        close(); warning="";
        File internal=new File(context.getFilesDir(),"installed.sqlite");
        File root=Environment.getExternalStorageDirectory();
        File[] files={internal,new File(root,"PHILOSOPHIA/philosophia.sqlite"),
                      new File(root,"PHILOSOPHIA/lexicon.db"),
                      new File("/storage/extSdCard/PHILOSOPHIA/philosophia.sqlite"),
                      new File("/storage/sdcard1/PHILOSOPHIA/philosophia.sqlite")};
        for(File f:files) {
            if(!f.isFile())continue;
            try {
                SQLiteDatabase check=SQLiteDatabase.openDatabase(f.getAbsolutePath(),
                  null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                boolean ok;
                try {ok=looksValid(check);}catch(Exception e){check.close();throw e;}
                if(!ok) {check.close();warning="Invalid schema: "+f.getPath();continue;}
                db=check;starter=false;path=f.getAbsolutePath();return;
            }catch(Exception e){warning="Cannot read "+f.getPath()+": "+e.getMessage();}
        }
        try {
            File f=seed();
            db=SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,
                 SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            if(!looksValid(db))throw new Exception("Bundled glossary schema mismatch");
            starter=true;path=f.getAbsolutePath();
        }catch(Exception e){warning="Bundled glossary unavailable: "+e.getMessage();close();}
    }
    private static boolean looksValid(SQLiteDatabase check) {
        Cursor c=check.rawQuery("SELECT value FROM metadata WHERE key='schema_version'",null);
        try {if(!c.moveToFirst()||!"1".equals(c.getString(0)))return false;}
        finally{c.close();}
        c=check.rawQuery("SELECT "+COLS+" FROM entries LIMIT 1",null);
        try {return c.moveToFirst();}finally{c.close();}
    }
    synchronized String meta(String name) {
        if(db==null)return "0";
        Cursor c=db.rawQuery("SELECT value FROM metadata WHERE key=?",new String[]{name});
        try{return c.moveToFirst()?c.getString(0):"0";}finally{c.close();}
    }
    private void add(List<Entry> list,String sql,String...args) {
        Cursor c=db.rawQuery(sql,args);
        try {while(c.moveToNext())list.add(new Entry(c));}
        finally {c.close();}
    }
    synchronized List<Entry> search(String query, boolean philosophyOnly) {
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
    /** Stage a ZIP or raw SQLite file without touching the active dictionary. */
    File stage(Uri uri) throws Exception {
        File out=new File(context.getFilesDir(),"staged.sqlite");
        if(out.exists()&&!out.delete())throw new Exception("Cannot remove prior temporary pack");
        if(context.getFilesDir().getUsableSpace()<25L*1024*1024)
            throw new Exception("Low internal storage. Use SD card placement instead.");
        InputStream supplied=context.getContentResolver().openInputStream(uri);
        if(supplied==null)throw new Exception("File picker returned no readable data");
        try {
            BufferedInputStream buffered=new BufferedInputStream(supplied,65536);
            buffered.mark(4);int x=buffered.read(),y=buffered.read();buffered.reset();
            if(x=='P'&&y=='K') {
                ZipInputStream zip=new ZipInputStream(buffered);
                try {
                    boolean found=false;ZipEntry entry;
                    while((entry=zip.getNextEntry())!=null) {
                        String name=entry.getName().toLowerCase(Locale.US);
                        if(!entry.isDirectory()&&(name.endsWith(".sqlite")||name.endsWith(".db"))) {
                            copy(zip,out); found=true;break;
                        }
                        zip.closeEntry();
                    }
                    if(!found)throw new Exception("ZIP archive has no .sqlite dictionary file");
                } finally {zip.close();}
            } else copy(buffered,out);
            validate(out);
            return out;
        }catch(Exception e) {out.delete();throw e;}
        finally {supplied.close();}
    }
    private static void copy(InputStream in,File file) throws Exception {
        FileOutputStream dst=new FileOutputStream(file);
        try {
            byte[] buffer=new byte[65536];int n;long total=0;
            while((n=in.read(buffer))!=-1) {
                total+=n;
                if(total>3500L*1024*1024)throw new Exception("Dictionary too large to import safely");
                dst.write(buffer,0,n);
            }
            dst.flush();
        }finally{dst.close();}
    }
    private static void validate(File f) throws Exception {
        if(f.length()<4096)throw new Exception("Not a valid SQLite pack");
        SQLiteDatabase d=SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,
            SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        try {
            if(!looksValid(d))throw new Exception("Wrong dictionary schema");
            Cursor c=d.rawQuery("PRAGMA quick_check(1)",null);
            try {
                if(!c.moveToFirst()||!"ok".equalsIgnoreCase(c.getString(0)))
                    throw new Exception("SQLite integrity validation failed");
            }finally{c.close();}
        }finally{d.close();}
    }
    synchronized void activate(File staged) throws Exception {
        File active=new File(context.getFilesDir(),"installed.sqlite");
        File backup=new File(context.getFilesDir(),"backup.sqlite");
        close();
        if(backup.exists()&&!backup.delete())throw new Exception("Cannot clear backup");
        boolean old=active.exists();
        if(old&&!active.renameTo(backup))throw new Exception("Cannot back up working dictionary");
        if(!staged.renameTo(active)) {
            if(old)backup.renameTo(active);
            open();throw new Exception("Cannot activate dictionary on device");
        }
        open();
        if(db==null||starter||!path.equals(active.getAbsolutePath())) {
            close();active.delete();if(old)backup.renameTo(active);open();
            throw new Exception("Database activation failed; restored previous pack");
        }
        if(old)backup.delete();
    }
}
