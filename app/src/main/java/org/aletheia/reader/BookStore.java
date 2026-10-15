package org.aletheia.reader;

import android.content.Context;
import android.net.Uri;
import android.text.Html;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

/** Offline local files only. No included copyrighted translation content. */
public final class BookStore {
  private final Context context;
  public BookStore(Context context){this.context=context.getApplicationContext();PDFBoxResourceLoader.init(this.context);}
  private String fileName(int translation){return translation==0?"macquarrie_robinson.txt":"stambaugh.txt";}
  public boolean isAvailable(int translation){return context.getFileStreamPath(fileName(translation)).exists();}
  public void importDocument(Uri uri,int translation) throws IOException {
    String name="";
    try(android.database.Cursor c=context.getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null)){
      if(c!=null&&c.moveToFirst())name=c.getString(0);
    }catch(Exception ignored){}
    byte[] bytes=readBounded(context.getContentResolver().openInputStream(uri),65*1024*1024);
    String text;
    if(name.toLowerCase(Locale.ROOT).endsWith(".pdf") || isPdf(bytes)){
      text=extractPdf(bytes);
    }else if(name.toLowerCase(Locale.ROOT).endsWith(".epub") || (bytes.length>3&&bytes[0]=='P'&&bytes[1]=='K')){
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
  private static boolean isPdf(byte[] data) {
    return data.length>5&&data[0]=='%'&&data[1]=='P'&&data[2]=='D'&&data[3]=='F';
  }
  private static String extractPdf(byte[] data) throws IOException {
    try(PDDocument document=PDDocument.load(data)){
      if(document.isEncrypted())throw new IOException("This PDF is encrypted. Import a legally usable unlocked copy.");
      if(document.getNumberOfPages()>1800)throw new IOException("PDF is too long for this version (1800-page limit).");
      PDFTextStripper stripper=new PDFTextStripper();
      stripper.setSortByPosition(true);
      String result=stripper.getText(document);
      if(result==null||result.trim().length()<150)
        throw new IOException("This PDF appears scanned or image-only. It needs a searchable text layer (OCR) before import.");
      return result.replaceAll("(?m)(?<!\\n)\\n(?!\\n)"," ").replaceAll("(?m)[ \\t]{2,}"," ").replaceAll("(?m)\\n{2,}","\\n\\n");
    }catch(IOException e){throw e;}catch(Exception e){throw new IOException("Could not extract PDF text: "+e.getMessage(),e);}
  }
  private static String extractEpub(byte[] data) throws IOException {
    // Respect EPUB package spine instead of filename sorting (which may scramble chapters).
    Map<String,String> docs=new LinkedHashMap<>();
    Map<String,String> packages=new HashMap<>();
    String container=null;
    try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(data))){
      ZipEntry entry;int scanned=0;
      while((entry=zip.getNextEntry())!=null){
        if(++scanned>2500)throw new IOException("Too many EPUB archive entries.");
        String name=entry.getName().replace('\\','/').toLowerCase(Locale.ROOT);
        if(!entry.isDirectory()&&name.equals("meta-inf/container.xml")){
          container=new String(readZipEntry(zip,128_000),StandardCharsets.UTF_8);
        }else if(!entry.isDirectory()&&name.endsWith(".opf")){
          packages.put(name,new String(readZipEntry(zip,650_000),StandardCharsets.UTF_8));
        }else if(!entry.isDirectory()&&(name.endsWith(".xhtml")||name.endsWith(".html")||name.endsWith(".htm"))){
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
    List<String> order=new ArrayList<>();
    try{
      String packagePath=null;
      if(container!=null){
        XmlPullParser x=Xml.newPullParser();
        x.setInput(new StringReader(container));
        while(x.next()!=XmlPullParser.END_DOCUMENT){
          if(x.getEventType()==XmlPullParser.START_TAG && x.getName().equalsIgnoreCase("rootfile")){
            packagePath=x.getAttributeValue(null,"full-path");
            if(packagePath!=null)break;
          }
        }
      }
      if(packagePath==null&&packages.size()==1)packagePath=packages.keySet().iterator().next();
      if(packagePath!=null){
        String normalized=packagePath.toLowerCase(Locale.ROOT);
        String opf=packages.get(normalized);
        if(opf!=null){
          Map<String,String> items=new HashMap<>();
          List<String> refs=new ArrayList<>();
          XmlPullParser x=Xml.newPullParser();
          x.setInput(new StringReader(opf));
          while(x.next()!=XmlPullParser.END_DOCUMENT){
            if(x.getEventType()!=XmlPullParser.START_TAG)continue;
            String tag=x.getName();
            if(tag.equalsIgnoreCase("item")){
              String id=x.getAttributeValue(null,"id");
              String href=x.getAttributeValue(null,"href");
              if(id!=null&&href!=null)items.put(id,href);
            }else if(tag.equalsIgnoreCase("itemref")){
              String ref=x.getAttributeValue(null,"idref");
              if(ref!=null)refs.add(ref);
            }
          }
          String directory=normalized.contains("/")?normalized.substring(0,normalized.lastIndexOf('/')+1):"";
          for(String ref:refs){
            String href=items.get(ref);
            if(href==null)continue;
            href=href.split("#",2)[0].replace("%20"," ");
            String file=new File(directory+href).toPath().normalize().toString().replace('\\','/').toLowerCase(Locale.ROOT);
            if(docs.containsKey(file)&&!order.contains(file))order.add(file);
          }
        }
      }
    }catch(Exception ignored){
      // Some EPUBs contain malformed metadata; preserve the ZIP entry order as a fallback.
    }
    for(String file:docs.keySet())if(!order.contains(file))order.add(file);
    StringBuilder b=new StringBuilder();
    for(String file:order)b.append(docs.get(file).trim()).append("\n\n");
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
    String text=new String(readBounded(new FileInputStream(file),80*1024*1024),StandardCharsets.UTF_8);
    String[] blocks=text.trim().split("\\n\\s*\\n+");
    // A scanned/text-extracted PDF can contain very long lines and little paragraph spacing.
    if(blocks.length<8 && text.length()>10000){
      blocks=text.trim().split("(?<=[.!?])\\s+(?=[A-Z][a-z])");
    }
    ArrayList<String> out=new ArrayList<>();
    for(String block:blocks){
      String normalized=block.replaceAll("[ \\t]+"," ").trim();
      if(!normalized.isEmpty())out.add(normalized);
      if(out.size()>25000)break;
    }
    return out;
  }
}
