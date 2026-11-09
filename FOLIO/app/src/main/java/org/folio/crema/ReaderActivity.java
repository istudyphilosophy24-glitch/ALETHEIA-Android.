package org.folio.crema;

import android.app.*;
import android.content.*;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Single foreground activity, paginated static text, no timers or wake locks. */
public final class ReaderActivity extends Activity {
    private BookStore db;
    private BookStore.Book record;
    private EpubBook epub;
    private TextView page,footer;
    private LinearLayout root;
    private CharSequence chapterText="";
    private final ArrayList<Integer> starts=new ArrayList<>();
    private int chapter=0,pageIndex=0,fontSp=21,marginDp=20;
    private float lineSpacing=1.3f;
    private boolean footerVisible=true,serif=true,loading=false;
    private android.content.SharedPreferences prefs;
    private boolean started=false;
    private int layoutWidth,layoutHeight;
    private final ArrayDeque<int[]> jumpHistory=new ArrayDeque<>();

    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        db=new BookStore(this);String id=getIntent().getStringExtra("bookId");record=db.byId(id==null?"":id);
        if(record==null){finish();return;}
        prefs=getSharedPreferences("reader",MODE_PRIVATE);fontSp=prefs.getInt("font",21);marginDp=prefs.getInt("margin",20);
        lineSpacing=prefs.getFloat("spacing",1.3f);footerVisible=prefs.getBoolean("footer",true);serif=prefs.getBoolean("serif",true);
        try{epub=new EpubBook(new File(record.path));}catch(Exception ex){error("Cannot open this book",ex.getMessage());return;}
        root=Ui.column(this);page=new TextView(this);page.setTextColor(Color.BLACK);page.setBackgroundColor(Color.WHITE);
        page.setGravity(Gravity.TOP|Gravity.LEFT);page.setIncludeFontPadding(false);page.setTextIsSelectable(false);
        page.setOnTouchListener((v,e)->{if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            if(followLink(e))return true;
            float x=e.getX();int width=page.getWidth();
            if(x<width*.32f)previous();else if(x>width*.68f)next();else menu();return true;
        });
        root.addView(page,new LinearLayout.LayoutParams(-1,0,1));
        footer=Ui.text(this,"",11,false);footer.setTextColor(Color.DKGRAY);footer.setGravity(Gravity.CENTER);
        footer.setOnClickListener(v->menu());root.addView(footer,new LinearLayout.LayoutParams(-1,Ui.dp(this,27)));
        setContentView(root);applyStyle();
        page.post(()->{
            layoutWidth=page.getWidth()-page.getPaddingLeft()-page.getPaddingRight();layoutHeight=page.getHeight()-page.getPaddingTop()-page.getPaddingBottom();
            if(layoutWidth<=20 || layoutHeight<=20){error("Display not ready","Reading viewport could not be measured");return;}
            chapter=Math.max(0,Math.min(record.chapter,epub.chapters.size()-1));loadChapter(chapter,record.offset);
        });
    }
    private void applyStyle(){
        page.setTypeface(Typeface.create(serif?"serif":"sans-serif",Typeface.NORMAL));
        page.setTextSize(TypedValue.COMPLEX_UNIT_SP,fontSp);page.setLineSpacing(0,lineSpacing);
        int m=Ui.dp(this,marginDp);page.setPadding(m,Ui.dp(this,marginDp),m,Ui.dp(this,marginDp));
        footer.setVisibility(footerVisible?View.VISIBLE:View.GONE);
    }
    private void error(String title,String msg){new AlertDialog.Builder(this).setTitle(title).setMessage(msg==null?"Unsupported or damaged document":msg)
        .setCancelable(false).setPositiveButton("Close",(d,w)->finish()).show();}
    private void loadChapter(int newChapter,int requestedChar){
        if(epub==null || newChapter<0||newChapter>=epub.chapters.size())return;
        chapter=newChapter;
        try {
            String markup=epub.chapter(chapter);
            if(record.format.equals("txt"))chapterText=markup.replace("\r\n","\n");
            else chapterText=renderHtml(markup);
            starts.clear();starts.add(0);pageIndex=0;
            layoutWidth=page.getWidth()-page.getPaddingLeft()-page.getPaddingRight();
            layoutHeight=page.getHeight()-page.getPaddingTop()-page.getPaddingBottom();
            int goal=Math.max(0,Math.min(requestedChar,chapterText.length()));int count=0;
            while(startAt(pageIndex)<goal && count++<10000){
                int end=endFor(startAt(pageIndex));if(end<=startAt(pageIndex)||end>=chapterText.length())break;
                if(end>=goal)break;starts.add(end);pageIndex++;
            }
            showPage();started=true;
        }catch(Exception e){error("Chapter failed to render",e.getMessage());}
    }
    private CharSequence renderHtml(String source) {
        // Android Html.fromHtml never executes JavaScript. Strip styling and active HTML anyway.
        String clean=source.replaceAll("(?is)<!DOCTYPE[^>]*>","")
            .replaceAll("(?is)<script\\b[^>]*>.*?</script>","")
            .replaceAll("(?is)<style\\b[^>]*>.*?</style>","")
            .replaceAll("(?is)<iframe\\b[^>]*>.*?</iframe>","")
            .replaceAll("(?is)<svg\\b[^>]*>.*?</svg>","");
        Matcher m=Pattern.compile("(?is)<body\\b[^>]*>(.*?)</body>").matcher(clean);if(m.find())clean=m.group(1);
        final String local=clean;final int which=chapter;
        Html.ImageGetter imageGetter=new Html.ImageGetter(){int images=0;
            @Override public Drawable getDrawable(String source){
                if(++images>12)return emptyImage();
                try{
                    String path=epub.assetPath(which,source);
                    if(path.isEmpty())return emptyImage();byte[] raw=epub.asset(path,2*1024*1024);
                    BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(raw,0,raw.length,opt);
                    if(opt.outWidth<1||opt.outHeight<1)return emptyImage();int sample=1;
                    while(opt.outWidth/sample>1024||opt.outHeight/sample>1024)sample*=2;
                    opt.inSampleSize=sample;opt.inJustDecodeBounds=false;
                    Bitmap bmp=BitmapFactory.decodeByteArray(raw,0,raw.length,opt);if(bmp==null)return emptyImage();
                    BitmapDrawable d=new BitmapDrawable(getResources(),bmp);int maxWidth=Math.max(100,layoutWidth);
                    float scale=Math.min(1f,Math.min((float)maxWidth/bmp.getWidth(),(float)Ui.dp(ReaderActivity.this,450)/bmp.getHeight()));
                    d.setBounds(0,0,Math.max(1,(int)(bmp.getWidth()*scale)),Math.max(1,(int)(bmp.getHeight()*scale)));return d;
                }catch(Exception ignored){return emptyImage();}
            }
        };
        return Html.fromHtml(local,imageGetter,null);
    }
    private Drawable emptyImage(){GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setBounds(0,0,2,2);return g;}
    private int startAt(int n){return starts.get(Math.max(0,Math.min(n,starts.size()-1)));}
    private int endFor(int start){
        int len=chapterText.length();if(start>=len)return len;
        int available=Math.max(40,layoutHeight), width=Math.max(80,layoutWidth);
        // Bounded measure: no whole-chapter StaticLayout for giant books.
        int end=Math.min(len,start+10000);
        CharSequence segment=chapterText.subSequence(start,end);
        StaticLayout l=new StaticLayout(segment,page.getPaint(),width,Layout.Alignment.ALIGN_NORMAL,lineSpacing,0,false);
        int line=l.getLineForVertical(Math.max(0,available-2));
        if(l.getHeight()<=available && end<len){
            // Usually impossible at 21sp; still handle very narrow or empty text safely.
            int limit=Math.min(len,start+50000);
            if(limit>end){segment=chapterText.subSequence(start,limit);l=new StaticLayout(segment,page.getPaint(),width,Layout.Alignment.ALIGN_NORMAL,lineSpacing,0,false);end=limit;line=l.getLineForVertical(Math.max(0,available-2));}
        }
        if(l.getHeight()<=available)return end;
        // Avoid the first clipped line; use the last line that fully fits.
        while(line>0 && l.getLineBottom(line)>available)line--;
        int result=start+l.getLineEnd(Math.max(0,line));
        if(result<=start)result=Math.min(len,start+1);
        return Math.min(len,result);
    }
    private void showPage(){
        if(page==null || chapterText==null)return;
        int begin=startAt(pageIndex);int end=endFor(begin);
        page.setText(chapterText.subSequence(begin,Math.max(begin,end)),TextView.BufferType.SPANNABLE);
        String caption=epub.chapters.get(chapter).title+"  ·  "+(pageIndex+1)+"  ·  "+(chapter+1)+"/"+epub.chapters.size();
        footer.setText(caption);persist(begin);
    }
    private void persist(int offset){if(record!=null)db.position(record.id,chapter,Math.max(0,offset));}
    private void next(){if(loading||epub==null)return;int last=startAt(pageIndex);int end=endFor(last);
        if(end<chapterText.length()){
            if(pageIndex+1>=starts.size())starts.add(end);
            pageIndex++;showPage();
        }else if(chapter+1<epub.chapters.size()){loadChapter(chapter+1,0);}
    }
    private void previous(){if(loading||epub==null)return;
        if(pageIndex>0){pageIndex--;showPage();return;}
        if(chapter>0){int prior=chapter-1;loadChapter(prior,Integer.MAX_VALUE);
            int count=0;while(endFor(startAt(pageIndex))<chapterText.length()&&count++<15000){int end=endFor(startAt(pageIndex));if(end<=startAt(pageIndex))break;starts.add(end);pageIndex++;}
            showPage();}
    }
    private void reflow(){int pos=startAt(pageIndex);applyStyle();page.post(()->loadChapter(chapter,pos));}
    private boolean followLink(MotionEvent ev){
        CharSequence display=page.getText();if(!(display instanceof Spanned))return false;
        Layout layout=page.getLayout();if(layout==null)return false;
        int x=(int)ev.getX()-page.getTotalPaddingLeft();int y=(int)ev.getY()-page.getTotalPaddingTop();
        if(x<0||y<0||y>=layout.getHeight())return false;
        int line=layout.getLineForVertical(y);int localOffset=layout.getOffsetForHorizontal(line,x);
        URLSpan[] links=((Spanned)display).getSpans(localOffset,Math.min(display.length(),localOffset+1),URLSpan.class);
        if(links.length==0)return false;
        String href=links[0].getURL();if(href==null||href.startsWith("http")||href.startsWith("mailto:"))return true;
        int target=epub.chapterFor(epub.chapters.get(chapter).path,chapter,href);
        if(target>=0){jumpHistory.push(new int[]{chapter,startAt(pageIndex)});loadChapter(target,0);}else Toast.makeText(this,"Reference not found in this book",Toast.LENGTH_SHORT).show();
        return true;
    }
    private void menu(){
        new AlertDialog.Builder(this).setTitle("FOLIO · "+record.title)
            .setItems(new String[]{"Continue reading","Table of contents","Add bookmark","My bookmarks","Typography & layout","Return to library"},
                (d,w)->{switch(w){case 1:toc();break;case 2:mark();break;case 3:marks();break;case 4:typography();break;case 5:finish();break;default:break;}})
            .show();
    }
    private void toc(){String[] labels=new String[epub.chapters.size()];for(int i=0;i<labels.length;i++)labels[i]=epub.chapters.get(i).title;
        new AlertDialog.Builder(this).setTitle("Table of contents").setItems(labels,(d,i)->loadChapter(i,0)).setNegativeButton("Close",null).show();}
    private void mark(){int pos=startAt(pageIndex);db.addMark(record.id,chapter,pos,epub.chapters.get(chapter).title+" · page "+(pageIndex+1));
        Toast.makeText(this,"Bookmark saved",Toast.LENGTH_SHORT).show();}
    private void marks(){ArrayList<BookStore.Mark> list=db.marks(record.id);if(list.isEmpty()){new AlertDialog.Builder(this).setMessage("No bookmarks yet.").setPositiveButton("OK",null).show();return;}
        String[] titles=new String[list.size()];for(int i=0;i<list.size();i++)titles[i]=list.get(i).label;
        new AlertDialog.Builder(this).setTitle("Bookmarks").setItems(titles,(d,i)->loadChapter(list.get(i).chapter,list.get(i).offset)).setNegativeButton("Close",null).show();}
    private void typography(){
        new AlertDialog.Builder(this).setTitle("Typography & layout").setItems(new String[]{
            "Smaller font ("+fontSp+" sp)","Larger font ("+fontSp+" sp)","Line spacing ("+lineSpacing+"×)",
            "Typeface: "+(serif?"Serif":"Sans"),"Margins ("+marginDp+" dp)","Show footer: "+(footerVisible?"Yes":"No")},(d,which)->{
            if(which==0)fontSp=Math.max(13,fontSp-1);if(which==1)fontSp=Math.min(35,fontSp+1);
            if(which==2)lineSpacing=lineSpacing>=1.59f?1.15f:(lineSpacing<1.25f?1.3f:1.6f);
            if(which==3)serif=!serif;if(which==4)marginDp=marginDp>=30?10:marginDp+5;
            if(which==5)footerVisible=!footerVisible;
            prefs.edit().putInt("font",fontSp).putInt("margin",marginDp).putFloat("spacing",lineSpacing)
                .putBoolean("serif",serif).putBoolean("footer",footerVisible).apply();reflow();
        }).setNegativeButton("Done",null).show();
    }
    @Override public boolean onKeyDown(int code,KeyEvent e){
        if(code==KeyEvent.KEYCODE_VOLUME_DOWN||code==KeyEvent.KEYCODE_DPAD_RIGHT||code==KeyEvent.KEYCODE_PAGE_DOWN){next();return true;}
        if(code==KeyEvent.KEYCODE_VOLUME_UP||code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_PAGE_UP){previous();return true;}
        if(code==KeyEvent.KEYCODE_MENU){menu();return true;}
        return super.onKeyDown(code,e);
    }
    @Override public void onBackPressed(){if(!jumpHistory.isEmpty()){int[] p=jumpHistory.pop();loadChapter(p[0],p[1]);return;}super.onBackPressed();}
    @Override protected void onPause(){if(started && chapterText!=null && record!=null)persist(startAt(pageIndex));super.onPause();}
    @Override protected void onDestroy(){try{if(epub!=null)epub.close();}catch(IOException ignored){}if(db!=null)db.close();super.onDestroy();}
}
