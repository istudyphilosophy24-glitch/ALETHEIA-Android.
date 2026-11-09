package org.folio.crema;

import android.content.Context;
import android.graphics.Typeface;
import java.io.*;
import java.util.*;

/** System fonts plus user-selected TTF/OTF files, without shipping proprietary font binaries. */
final class FontCatalog {
    static final class Entry {
        final String key,label;
        Entry(String key,String label){this.key=key;this.label=label;}
        @Override public String toString(){return label;}
    }
    private static final Entry[] BUILTINS = new Entry[]{
        new Entry("sys:serif","Book Serif"),
        new Entry("sys:sans-serif","Sans Serif"),
        new Entry("sys:sans-serif-light","Light Sans"),
        new Entry("sys:sans-serif-condensed","Condensed Sans"),
        new Entry("sys:monospace","Monospace"),
        new Entry("sys:cursive","Cursive"),
        new Entry("sys:casual","Casual")
    };
    private FontCatalog(){}
    private static Context appContext;
    private static final Map<String,Typeface> fontCache=new LinkedHashMap<String,Typeface>();
    static void initialize(Context c){appContext=c.getApplicationContext();}
    private static void addBundled(Context c,ArrayList<Entry> list){
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(c.getAssets().open("font_catalog.tsv"),"UTF-8"))){
            String row;
            while((row=reader.readLine())!=null){
                String[] parts=row.split("\t");
                if(parts.length>=2 && parts[0].matches("[a-z0-9]+")){
                    list.add(new Entry("asset:"+parts[0],"Book Font · "+parts[1]+" ("+parts[0]+")"));
                }
            }
        }catch(Exception e){android.util.Log.w("FOLIO","Font catalog unavailable",e);}
    }

    static ArrayList<Entry> list(Context c){
        ArrayList<Entry> items=new ArrayList<Entry>(Arrays.asList(BUILTINS));
        addBundled(c,items);
        File[] files=folder(c).listFiles();
        if(files!=null){Arrays.sort(files,new Comparator<File>(){public int compare(File a,File b){return a.getName().compareToIgnoreCase(b.getName());}});
            for(File f:files){if(f.isFile()&&isFont(f.getName()))items.add(new Entry("file:"+f.getAbsolutePath(),"Custom · "+name(f.getName())));}
        }
        return items;
    }
    static Typeface load(String key,int weight){
        try {
            if(key!=null&&key.startsWith("asset:")){
                synchronized(fontCache){
                    Typeface face=fontCache.get(key);
                    if(face==null && appContext!=null){
                        face=Typeface.createFromAsset(appContext.getAssets(),"fonts/"+key.substring(6)+".ttf");
                        if(fontCache.size()>=6)fontCache.clear();
                        fontCache.put(key,face);
                    }
                    return face==null?Typeface.create("serif",weight):Typeface.create(face,weight);
                }
            }
            if(key!=null&&key.startsWith("file:"))return Typeface.create(Typeface.createFromFile(key.substring(5)),weight);
            String family=(key!=null&&key.startsWith("sys:"))?key.substring(4):"serif";
            return Typeface.create(family,weight);
        }catch(Exception ignored){return Typeface.create("serif",weight);}
    }
    static File install(Context c,File src) throws IOException {
        if(!src.isFile()||!isFont(src.getName()))throw new IOException("Select a .ttf or .otf font file");
        if(src.length()<64||src.length()>6L*1024*1024)throw new IOException("Font must be 64 bytes–6 MB");
        try(InputStream input=new FileInputStream(src)){
            byte[] header=new byte[4];if(input.read(header)!=4)throw new IOException("Font file is truncated");
            boolean sfnt=(header[0]==0&&header[1]==1&&header[2]==0&&header[3]==0);
            boolean otto=(header[0]=='O'&&header[1]=='T'&&header[2]=='T'&&header[3]=='O');
            boolean truetype=(header[0]=='t'&&header[1]=='r'&&header[2]=='u'&&header[3]=='e');
            if(!sfnt&&!otto&&!truetype)throw new IOException("Unsupported font file header");
        }
        File dir=folder(c);if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create fonts folder");
        String basename=src.getName().replaceAll("[^A-Za-z0-9._-]","_");
        File dest=new File(dir,basename);
        try(InputStream input=new FileInputStream(src);OutputStream output=new FileOutputStream(dest)){
            byte[] buf=new byte[8192];int n;while((n=input.read(buf))!=-1)output.write(buf,0,n);
        }
        return dest;
    }
    private static File folder(Context c){return new File(c.getFilesDir(),"fonts");}
    private static boolean isFont(String name){String s=name.toLowerCase(Locale.US);return s.endsWith(".ttf")||s.endsWith(".otf");}
    private static String name(String n){return n.substring(0,n.lastIndexOf('.')).replace('_',' ').replace('-',' ');}
}
