package org.aletheia.reader;

import android.content.Context;
import android.net.Uri;
import android.text.Html;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Offline local files only. No included copyrighted translation content. */
public final class BookStore {
  private final Context context;
  public BookStore(Context context){this.context=context.getApplicationContext();}
  private String fileName(int translation){return translation==0?"macquarrie_robinson.txt":"stambaugh.txt";}
  public boolean isAvailable(int translation){return context.getFileStreamPath(fileName(translation)).exists();}
  public void importDocument(Uri uri,int translation) throws IOException {
    String name="";
    try(android.database.Cursor c=context.getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){
      if(c!=null&&c.moveToFirst())name=c.getString(0);
    }catch(Exception ignored){}
    byte[] bytes=readBounded(context.getContentResolver().openInputStream(uri),14*1024*1024);
    String text;
    if(name.toLowerCase(Locale.ROOT).endsWith(".epub") || (bytes.length>3&&bytes[0]=='P'&&bytes[1]=='K')){
      text=extractEpub(bytes);
    }else{
      text=new String(bytes,StandardCharsets.UTF_8);
    }
    text=text.replace("\r\n","\n").replace('\r','\n').replace("\u0000","");
    if(text.trim().length()<100)throw new IOException("The document contains too little extractable text.");
    try(FileOutputStream out=context.openFileOutput(fileName(translation),Context.MODE_PRIVATE)){
      out.write(text.getBytes(StandardCharsets.UTF_8));
    }
  }
  private static byte[] readBounded(InputStream in,int max) throws IOException {
    if(in==null)throw new IOException("File could not be opened.");
    try(InputStream input=in; ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] buf=new byte[8192];int n;
      while((n=input.read(buf))!=-1){
        if(out.size()+n>max)throw new IOException("File exceeds the import limit.");
        out.write(buf,0,n);
      }
      return out.toByteArray();
    }
  }
  private static String extractEpub(byte[] data) throws IOException {
    Map<String,String> docs=new TreeMap<>();
    try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(data))){
      ZipEntry entry;int scanned=0;
      while((entry=zip.getNextEntry())!=null){
        if(++scanned>2500)throw new IOException("Too many EPUB archive entries.");
        String name=entry.getName().toLowerCase(Locale.ROOT);
        if(!entry.isDirectory()&&(name.endsWith(".xhtml")||name.endsWith(".html")||name.endsWith(".htm"))){
          byte[] b=readZipEntry(zip,900_000);
          String html=new String(b,StandardCharsets.UTF_8);
          String cleaned=Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY).toString();
          docs.put(name,cleaned);
        }
        zip.closeEntry();
        if(docs.size()>280)throw new IOException("Too many EPUB chapters.");
      }
    }
    if(docs.isEmpty())throw new IOException("No readable EPUB chapters found. DRM-protected books cannot be imported.");
    // Filename order is a provisional fallback. Imported editions do NOT automatically
    // align paragraphs or chapter numbering between translations.
    StringBuilder b=new StringBuilder();
    for(String chapter:docs.values()){b.append(chapter.trim()).append("\n\n");}
    return b.toString();
  }
  private static byte[] readZipEntry(InputStream in,int max) throws IOException{
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
    while((n=in.read(buf))!=-1){if(out.size()+n>max)throw new IOException("EPUB chapter too large.");out.write(buf,0,n);}
    return out.toByteArray();
  }
  public List<String> paragraphs(int translation)throws IOException {
    File file=context.getFileStreamPath(fileName(translation));
    if(!file.exists())return Collections.emptyList();
    String text=new String(readBounded(new FileInputStream(file),17*1024*1024),StandardCharsets.UTF_8);
    String[] blocks=text.trim().split("\\n\\s*\\n+");
    ArrayList<String> out=new ArrayList<>();
    for(String block:blocks){
      String normalized=block.replaceAll("[ \\t]+"," ").trim();
      if(!normalized.isEmpty())out.add(normalized);
      if(out.size()>25000)break;
    }
    return out;
  }
}
