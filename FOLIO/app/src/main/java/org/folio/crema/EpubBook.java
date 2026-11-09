package org.folio.crema;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

/** EPUB 2 + reflowable EPUB 3 spine reader. No browser, script execution or network access. */
public final class EpubBook implements Closeable {
    public static final class Chapter {
        public final String path, title;
        Chapter(String p,String t){path=p;title=t;}
    }
    public static final class Meta {
        public String title="Untitled",author="Unknown author",coverPath="";
    }
    private final ZipFile zip;
    private final String root;
    public final ArrayList<Chapter> chapters=new ArrayList<>();
    private final HashMap<String,String> hrefToSpine=new HashMap<>();
    public final Meta metadata=new Meta();
    private final String txt;
    private final boolean textFile;
    public EpubBook(File f) throws Exception {
        textFile=f.getName().toLowerCase(Locale.US).endsWith(".txt");
        if(textFile) {
            zip=null;root="";String name=f.getName();metadata.title=name.substring(0,name.lastIndexOf('.'));txt=readLimited(new FileInputStream(f),12*1024*1024);
            chapters.add(new Chapter("text","Text"));return;
        }
        txt=null;
        zip=new ZipFile(f);
        if(zip.size()>5000)throw new IOException("EPUB has too many files");
        Document c=parse(read("META-INF/container.xml",256*1024));
        NodeList refs=c.getElementsByTagNameNS("*","rootfile");
        if(refs.getLength()==0)throw new IOException("Missing EPUB container rootfile");
        String opf=attr((Element)refs.item(0),"full-path");
        if(opf.isEmpty())throw new IOException("Missing EPUB package path");
        root=parent(opf);
        Document pkg=parse(read(opf,3*1024*1024));
        String title=firstElementText(pkg,"title"),author=firstElementText(pkg,"creator");
        if(!title.isEmpty())metadata.title=title;
        if(!author.isEmpty())metadata.author=author;
        HashMap<String,String> idPath=new HashMap<>();
        HashMap<String,String> idProperties=new HashMap<>();
        HashMap<String,String> idTypes=new HashMap<>();
        HashMap<String,String> titleMap=new HashMap<>();
        NodeList items=pkg.getElementsByTagNameNS("*","item");
        for(int i=0;i<items.getLength();i++){
            Element e=(Element)items.item(i);String id=attr(e,"id");String href=attr(e,"href");
            if(!id.isEmpty()&&!href.isEmpty()){
                String full=resolve(root,href);
                idPath.put(id,full);idProperties.put(id,attr(e,"properties"));idTypes.put(id,attr(e,"media-type"));
            }
        }
        // Prefer EPUB3 cover-image, then EPUB2 metadata cover ID, then obvious cover filename.
        for(Map.Entry<String,String> item:idProperties.entrySet()){
            if(Arrays.asList(item.getValue().split("\\s+")).contains("cover-image")){
                metadata.coverPath=idPath.get(item.getKey());break;
            }
        }
        if(metadata.coverPath.isEmpty()){
            NodeList allMeta=pkg.getElementsByTagNameNS("*","meta");
            for(int i=0;i<allMeta.getLength();i++){
                Element e=(Element)allMeta.item(i);
                if("cover".equalsIgnoreCase(attr(e,"name"))){
                    String candidate=idPath.get(attr(e,"content"));
                    if(candidate!=null && isImageType(idTypes.get(attr(e,"content")))){metadata.coverPath=candidate;break;}
                }
            }
        }
        if(metadata.coverPath.isEmpty()){
            for(Map.Entry<String,String> item:idPath.entrySet()){
                String v=item.getValue().toLowerCase(Locale.US);
                if(isImageType(idTypes.get(item.getKey())) && (v.contains("cover.")||v.contains("cover_")||v.contains("cover-"))){
                    metadata.coverPath=item.getValue();break;
                }
            }
        }
        // NCX (EPUB 2) and navigation document (EPUB 3) labels are best-effort.
        try {
            for(int i=0;i<items.getLength();i++){
                Element e=(Element)items.item(i);String props=attr(e,"properties");
                String mt=attr(e,"media-type");String path=idPath.get(attr(e,"id"));
                if(path!=null && (props.contains("nav")||mt.equals("application/x-dtbncx+xml"))){
                    Document nav=parse(read(path,2*1024*1024));
                    NodeList links=nav.getElementsByTagNameNS("*","navPoint");
                    for(int j=0;j<links.getLength();j++){
                        Element point=(Element)links.item(j);NodeList cs=point.getElementsByTagNameNS("*","content");
                        if(cs.getLength()>0){String dest=resolve(parent(path),attr((Element)cs.item(0),"src"));
                            String label=firstElementText(point,"text");if(!label.isEmpty())titleMap.put(stripFragment(dest),label);}
                    }
                    NodeList anchors=nav.getElementsByTagNameNS("*","a");
                    for(int j=0;j<anchors.getLength();j++){Element a=(Element)anchors.item(j);
                        String label=a.getTextContent();if(label!=null&&label.trim().length()>0)titleMap.put(stripFragment(resolve(parent(path),attr(a,"href"))),label.trim());}
                }
            }
        }catch(Exception ignored){}
        NodeList refsInSpine=pkg.getElementsByTagNameNS("*","itemref");
        for(int i=0;i<refsInSpine.getLength();i++){
            Element e=(Element)refsInSpine.item(i);if("no".equals(attr(e,"linear")))continue;
            String path=idPath.get(attr(e,"idref"));if(path==null)continue;
            String name=titleMap.get(path);if(name==null||name.length()>140)name="Section "+(chapters.size()+1);
            hrefToSpine.put(path,String.valueOf(chapters.size()));chapters.add(new Chapter(path,name));
        }
        if(chapters.isEmpty())throw new IOException("No readable EPUB spine items");
    }
    private static boolean isImageType(String media) {
        return media!=null && (media.equals("image/jpeg")||media.equals("image/png")||media.equals("image/webp"));
    }
    /** Returns a bounded embedded EPUB cover; absent for TXT/coverless EPUB. */
    public byte[] coverBytes() throws IOException {
        if(textFile||metadata.coverPath.isEmpty())return null;
        return readBytes(metadata.coverPath,3*1024*1024);
    }
    public String chapter(int index) throws IOException {
        if(textFile)return txt;
        if(index<0||index>=chapters.size())throw new IOException("Invalid chapter");
        return read(chapters.get(index).path,12*1024*1024);
    }
    /** Detect image-only spine entries, including common SVG-wrapped EPUB covers. */
    public String fullPageImagePath(int index,String markup){
        if(textFile||markup==null||index<0||index>=chapters.size())return null;
        String file=chapters.get(index).path;
        // Remove script/styles, XML prolog and SVG tags when measuring visible text.
        String body=markup.replaceAll("(?is)<script\\b[^>]*>.*?</script>","")
            .replaceAll("(?is)<style\\b[^>]*>.*?</style>","");
        Matcher bm=Pattern.compile("(?is)<body\\b[^>]*>(.*?)</body>").matcher(body);
        if(bm.find())body=bm.group(1);
        Pattern images=Pattern.compile("(?is)<(?:img|image)\\b[^>]*?\\b(?:src|(?:xlink:)?href)\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
        Matcher m=images.matcher(body);String ref=null;int count=0;
        while(m.find()){count++;if(count==1)ref=m.group(1);if(count>1)return null;}
        // Some EPUB2 covers use <image xlink:href=...> in a standalone SVG.
        if(ref==null){
            if(file.toLowerCase(Locale.US).contains("cover")&&!metadata.coverPath.isEmpty())return metadata.coverPath;
            return null;
        }
        String text=body.replaceAll("(?is)<(?:img|image)\\b[^>]*>"," ")
            .replaceAll("(?is)<[^>]+>"," ")
            .replace("&nbsp;"," ").replace("&#160;"," ").replace("&amp;","&").trim();
        boolean namedCover=file.toLowerCase(Locale.US).matches(".*(?:cover|titlepage|frontispiece).*?");
        if(text.length()>(namedCover?120:24))return null;
        String resolved=resolve(parent(file),ref);
        if(resolved.isEmpty() || !zipHasImage(resolved))return null;
        return resolved;
    }
    private boolean zipHasImage(String path){
        String p=stripFragment(path).toLowerCase(Locale.US);
        if(!(p.endsWith(".jpg")||p.endsWith(".jpeg")||p.endsWith(".png")||p.endsWith(".webp")||p.endsWith(".gif")))return false;
        return zip.getEntry(stripFragment(path))!=null;
    }
    public byte[] asset(String path,int limit) throws IOException {
        if(textFile)return new byte[0];return readBytes(path,limit);
    }
    public String assetPath(int chapter,String href) {
        if(textFile)return "";return resolve(parent(chapters.get(chapter).path),href);
    }
    public int chapterFor(String fromPath,int chapter,String href){
        String path=resolve(parent(fromPath),href);String i=hrefToSpine.get(stripFragment(path));return i==null?-1:Integer.parseInt(i);
    }
    public static Meta inspect(File file) throws Exception {try(EpubBook b=new EpubBook(file)){return b.metadata;}}
    private String read(String path,int limit) throws IOException {return readLimited(new ByteArrayInputStream(readBytes(path,limit)),limit);}
    private byte[] readBytes(String path,int limit) throws IOException {
        String normalized=stripFragment(normalize(path));ZipEntry e=zip.getEntry(normalized);
        if(e==null)throw new IOException("Missing EPUB resource: "+normalized);
        if(e.getSize()>limit)throw new IOException("EPUB entry too large");
        try(InputStream in=zip.getInputStream(e);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n,total=0;
            while((n=in.read(buf))!=-1){total+=n;if(total>limit)throw new IOException("EPUB resource too large");out.write(buf,0,n);}
            return out.toByteArray();
        }
    }
    private static String readLimited(InputStream in,int limit)throws IOException{
        try(InputStream source=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[8192];int n,total=0;
            while((n=source.read(buf))!=-1){total+=n;if(total>limit)throw new IOException("Text exceeds size limit");out.write(buf,0,n);}
            byte[] bytes=out.toByteArray();String data=new String(bytes,"UTF-8");
            if(data.startsWith("\uFEFF"))data=data.substring(1);
            return data;
        }
    }
    private static Document parse(String xml) throws Exception {
        if(xml.matches("(?is).*<!\\s*(?:DOCTYPE|ENTITY)\\b.*"))throw new IOException("DTD and entities not permitted");
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);
        try{f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);}catch(Exception ignored){}
        try{f.setFeature("http://xml.org/sax/features/external-general-entities",false);}catch(Exception ignored){}
        try{f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);}catch(Exception ignored){}
        try{f.setXIncludeAware(false);}catch(Exception ignored){}f.setExpandEntityReferences(false);
        return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
    private static String firstElementText(Node n,String name) {
        NodeList list= n instanceof Document ? ((Document)n).getElementsByTagNameNS("*",name) : ((Element)n).getElementsByTagNameNS("*",name);return list.getLength()==0?"":list.item(0).getTextContent().trim();
    }
    private static String attr(Element e,String a){String s=e.getAttribute(a);return s==null?"":s;}
    private static String stripFragment(String s){int p=s.indexOf('#');return p<0?s:s.substring(0,p);}
    private static String parent(String p){int at=p.lastIndexOf('/');return at<0?"":p.substring(0,at+1);}
    public static String resolve(String base,String path){
        if(path==null)path="";try{path=java.net.URLDecoder.decode(path,"UTF-8");}catch(Exception ignored){}
        if(path.startsWith("http:")||path.startsWith("https:")||path.startsWith("file:"))return "";
        return normalize(base+path);
    }
    private static String normalize(String p){
        String fragment="";int at=p.indexOf('#');if(at>=0){fragment=p.substring(at);p=p.substring(0,at);}
        Deque<String> parts=new ArrayDeque<>();for(String el:p.replace('\\','/').split("/")){
            if(el.isEmpty()||el.equals("."))continue;
            if(el.equals("..")){if(!parts.isEmpty())parts.removeLast();else return "";}else parts.addLast(el);
        }
        StringBuilder result=new StringBuilder();for(String el:parts){if(result.length()>0)result.append('/');result.append(el);}
        return result.toString()+fragment;
    }
    @Override public void close()throws IOException{if(zip!=null)zip.close();}
}
