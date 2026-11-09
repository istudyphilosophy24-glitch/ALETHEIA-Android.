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
    private static final int PICK_FONT=207;
    private static final long QUOTE_HOLD_MS=5000L;
    private final Handler touchHandler = new Handler(Looper.getMainLooper());
    private Runnable quoteHold;
    private boolean touchActive=false, holdTriggered=false, touchMoved=false;
    private int touchPointerId=-1;
    private float downX=0,downY=0;
    private long downAt=0;
    private int visiblePageStart=0;
    private boolean focusReading=true;
    private int chapter=0,pageIndex=0,fontSp=21,marginDp=20,indentDp=14,paragraphGapDp=7;
    private float lineSpacing=1.3f;
    private String fontKey="sys:serif";
    private boolean footerVisible=true,boldText=false,loading=false;
    private float typeScaleX=1.0f;
    private android.content.SharedPreferences prefs;
    private boolean started=false;
    private int layoutWidth,layoutHeight;
    private final ArrayDeque<int[]> jumpHistory=new ArrayDeque<>();

    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        db=new BookStore(this);String id=getIntent().getStringExtra("bookId");record=db.byId(id==null?"":id);
        if(record==null){finish();return;}
        prefs=getSharedPreferences("reader",MODE_PRIVATE);fontSp=prefs.getInt("font",21);marginDp=prefs.getInt("margin",20);
        lineSpacing=prefs.getFloat("spacing",1.3f);footerVisible=prefs.getBoolean("footer",true);
        indentDp=prefs.getInt("indent",14);paragraphGapDp=prefs.getInt("paragraph_gap",7);
        fontKey=prefs.getString("font_key",prefs.getBoolean("serif",true)?"sys:serif":"sys:sans-serif");
        boldText=prefs.getBoolean("font_bold",false);typeScaleX=prefs.getFloat("font_width",1.0f);
        try{epub=new EpubBook(new File(record.path));}catch(Exception ex){error("Cannot open this book",ex.getMessage());return;}
        root=Ui.column(this);page=new TextView(this);page.setTextColor(Color.BLACK);page.setBackgroundColor(Color.WHITE);
        page.setGravity(Gravity.TOP|Gravity.LEFT);page.setIncludeFontPadding(false);
        // Disable ordinary TextView selection, which otherwise hijacks page taps.
        page.setTextIsSelectable(false);
        page.setLongClickable(false);
        final int movementTolerance=ViewConfiguration.get(this).getScaledTouchSlop();
        page.setOnTouchListener(new View.OnTouchListener(){
            @Override public boolean onTouch(View view,MotionEvent event){
                int action=event.getActionMasked();
                if(action==MotionEvent.ACTION_DOWN){
                    cancelHold();touchActive=true;touchMoved=false;holdTriggered=false;
                    touchPointerId=event.getPointerId(0);
                    downX=event.getX();downY=event.getY();downAt=SystemClock.uptimeMillis();
                    final float holdX=downX,holdY=downY;
                    quoteHold=new Runnable(){@Override public void run(){
                        if(!touchActive||touchMoved||holdTriggered)return;
                        holdTriggered=true;
                        openQuotationPicker(holdX,holdY);
                    }};
                    touchHandler.postDelayed(quoteHold,QUOTE_HOLD_MS);
                    return true;
                }
                if(action==MotionEvent.ACTION_POINTER_DOWN){
                    touchMoved=true;cancelHold();return true;
                }
                if(action==MotionEvent.ACTION_MOVE){
                    int idx=event.findPointerIndex(touchPointerId);
                    if(idx<0||Math.abs(event.getX(idx)-downX)>movementTolerance
                        ||Math.abs(event.getY(idx)-downY)>movementTolerance){
                        touchMoved=true;cancelHold();
                    }
                    return true;
                }
                if(action==MotionEvent.ACTION_CANCEL){
                    cancelHold();touchActive=false;return true;
                }
                if(action==MotionEvent.ACTION_UP){
                    cancelHold();touchActive=false;
                    if(holdTriggered||touchMoved)return true;
                    long elapsed=SystemClock.uptimeMillis()-downAt;
                    if(elapsed>1100L)return true;
                    if(followLink(event))return true;
                    int width=page.getWidth();
                    if(event.getX()<width*.30f)previous();
                    else if(event.getX()>width*.70f)next();
                    else menu();
                    return true;
                }
                return true;
            }
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
    private void cancelHold(){
        if(quoteHold!=null){touchHandler.removeCallbacks(quoteHold);quoteHold=null;}
    }
    /** An isolated selection panel prevents selection gestures from stealing page taps. */
    private void openQuotationPicker(float x,float y){
        if(page==null||page.getText().length()==0)return;
        final int base=visiblePageStart;
        final CharSequence visible=page.getText();
        final int nearby=offsetAtTouch(x,y);
        final TextView selectionView=new TextView(this);
        selectionView.setTextColor(Color.BLACK);selectionView.setBackgroundColor(Color.WHITE);
        selectionView.setTextSize(TypedValue.COMPLEX_UNIT_SP,Math.min(25,Math.max(17,fontSp)));
        selectionView.setTypeface(FontCatalog.load(fontKey,boldText?Typeface.BOLD:Typeface.NORMAL));
        selectionView.setIncludeFontPadding(false);
        selectionView.setLineSpacing(Ui.dp(this,3),1.20f);
        selectionView.setText(visible,TextView.BufferType.SPANNABLE);
        selectionView.setTextIsSelectable(true);
        int pad=Ui.dp(this,13);selectionView.setPadding(pad,pad,pad,pad);
        final ScrollView scroll=new ScrollView(this);
        scroll.addView(selectionView,new FrameLayout.LayoutParams(-1,-2));
        final LinearLayout panel=Ui.column(this);
        TextView instruction=Ui.text(this,"Select words below and adjust the handles, then tap Underline selection. Or use Nearest sentence.",14,false);
        instruction.setPadding(pad,pad,pad,pad);instruction.setTextColor(Color.DKGRAY);
        panel.addView(instruction);
        panel.addView(scroll,new LinearLayout.LayoutParams(-1,Ui.dp(this,300)));
        final AlertDialog dialog=new AlertDialog.Builder(this)
            .setTitle("Quotation selection")
            .setView(panel)
            .setPositiveButton("Underline selection",null)
            .setNeutralButton("Nearest sentence",null)
            .setNegativeButton("Cancel",null)
            .create();
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                int selectedStart=selectionView.getSelectionStart(),selectedEnd=selectionView.getSelectionEnd();
                if(selectedStart<0||selectedEnd<=selectedStart){
                    Toast.makeText(this,"Select text in the panel first, or tap Nearest sentence",Toast.LENGTH_LONG).show();
                    return;
                }
                dialog.dismiss();annotate(base+selectedStart,base+selectedEnd,true);
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
                int[] range=sentenceNear(visible.toString(),nearby);
                if(range==null){
                    Toast.makeText(this,"No text to underline here",Toast.LENGTH_SHORT).show();return;
                }
                dialog.dismiss();annotate(base+range[0],base+range[1],true);
            });
        });
        dialog.show();
    }
    private int offsetAtTouch(float x,float y){
        Layout layout=page.getLayout();if(layout==null||layout.getHeight()==0)return 0;
        int xx=(int)x-page.getTotalPaddingLeft(),yy=(int)y-page.getTotalPaddingTop();
        int line=layout.getLineForVertical(Math.max(0,Math.min(layout.getHeight()-1,yy)));
        return Math.max(0,Math.min(page.getText().length(),layout.getOffsetForHorizontal(line,Math.max(0,xx))));
    }
    private int[] sentenceNear(String text,int anchor){
        if(text==null||text.trim().isEmpty())return null;
        Matcher m=Pattern.compile("[^.!?\\n]+(?:[.!?]+|$)").matcher(text);
        int[] nearest=null;int distance=Integer.MAX_VALUE;
        while(m.find()){
            int a=m.start(),b=m.end();
            while(a<b&&Character.isWhitespace(text.charAt(a)))a++;
            while(b>a&&Character.isWhitespace(text.charAt(b-1)))b--;
            if(a>=b)continue;
            int delta=anchor<a?a-anchor:anchor>=b?anchor-b+1:0;
            if(delta<distance){distance=delta;nearest=new int[]{a,b};if(delta==0)break;}
        }
        return nearest;
    }
    private void applyStyle(){
        page.setTypeface(FontCatalog.load(fontKey,boldText?Typeface.BOLD:Typeface.NORMAL));page.setTextScaleX(typeScaleX);
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
            CharSequence original=record.format.equals("txt")?markup.replace("\r\n","\n"):renderHtml(markup);
            chapterText=TextLayoutStyler.style(original,Ui.dp(this,indentDp),Ui.dp(this,paragraphGapDp),record.format.equals("txt"));
            refreshUnderlineSpans();
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
        visiblePageStart=begin;
        page.setText(chapterText.subSequence(begin,Math.max(begin,end)),TextView.BufferType.SPANNABLE);
        float fraction=chapterText.length()==0?0f:(float)begin/(float)chapterText.length();
        float complete=100f*(chapter+fraction)/Math.max(1,epub.chapters.size());
        String caption=epub.chapters.get(chapter).title+"  ·  "+(pageIndex+1)+"  ·  "+String.format(Locale.US,"%.1f%%",complete);
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
            .setItems(new String[]{"Continue reading","Table of contents","Add bookmark","My bookmarks","Underlined quotations","Underline help (5s hold)","Reading styles","Typography & layout","Focus & battery","Return to library"},
                (d,w)->{switch(w){case 1:toc();break;case 2:mark();break;case 3:marks();break;case 4:quotationList();break;case 5:showUnderlineHelp();break;case 6:readingStyles();break;case 7:typography();break;case 8:focusMenu();break;case 9:finish();break;default:break;}})
            .show();
    }
    private void toc(){String[] labels=new String[epub.chapters.size()];for(int i=0;i<labels.length;i++)labels[i]=epub.chapters.get(i).title;
        new AlertDialog.Builder(this).setTitle("Table of contents").setItems(labels,(d,i)->loadChapter(i,0)).setNegativeButton("Close",null).show();}
    private void mark(){int pos=startAt(pageIndex);db.addMark(record.id,chapter,pos,epub.chapters.get(chapter).title+" · page "+(pageIndex+1));
        Toast.makeText(this,"Bookmark saved",Toast.LENGTH_SHORT).show();}
    private void marks(){ArrayList<BookStore.Mark> list=db.marks(record.id);if(list.isEmpty()){new AlertDialog.Builder(this).setMessage("No bookmarks yet.").setPositiveButton("OK",null).show();return;}
        String[] titles=new String[list.size()];for(int i=0;i<list.size();i++)titles[i]=list.get(i).label;
        new AlertDialog.Builder(this).setTitle("Bookmarks").setItems(titles,(d,i)->loadChapter(list.get(i).chapter,list.get(i).offset)).setNegativeButton("Close",null).show();}
    private static final class FolioUnderlineSpan extends UnderlineSpan { }
    /** Persistent source-offset underlines, independent of visual page breaks. */
    private void annotate(int from,int to,boolean add){
        if(chapterText==null||from<0||to<=from||to>chapterText.length())return;
        if(add){String quote=chapterText.subSequence(from,to).toString().trim();
            if(quote.isEmpty())return;
            db.addUnderline(record.id,chapter,from,to,quote);
            Toast.makeText(this,"Quotation underlined",Toast.LENGTH_SHORT).show();
        }else{
            int count=db.removeUnderlinesTouching(record.id,chapter,from,to);
            Toast.makeText(this,count>0?"Underline removed":"No underline in selection",Toast.LENGTH_SHORT).show();
        }
        refreshUnderlineSpans();showPage();
    }
    private void refreshUnderlineSpans(){
        if(!(chapterText instanceof Spannable))return;
        Spannable sp=(Spannable)chapterText;
        FolioUnderlineSpan[] old=sp.getSpans(0,sp.length(),FolioUnderlineSpan.class);
        for(FolioUnderlineSpan u:old)sp.removeSpan(u);
        for(BookStore.Quote q:db.underlines(record.id,chapter)){
            int a=Math.max(0,Math.min(sp.length(),q.start));int b=Math.max(a,Math.min(sp.length(),q.end));
            if(b>a)sp.setSpan(new FolioUnderlineSpan(),a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private void showUnderlineHelp(){
        new AlertDialog.Builder(this).setTitle("Underline quotations")
            .setMessage("Hold your finger still on a passage for FIVE full seconds. A quotation picker opens. Select exact words or tap Nearest sentence.\n\nQuick taps on the left or right turn pages; the center opens the menu.")
            .setPositiveButton("Got it",null).show();
    }
    private void quotationList(){
        final ArrayList<BookStore.Quote> quotes=db.underlines(record.id,-1);
        if(quotes.isEmpty()){new AlertDialog.Builder(this).setTitle("Underlined quotations")
            .setMessage("To underline, hold a passage for five seconds. A separate quotation picker opens. Ordinary taps turn pages.")
            .setPositiveButton("OK",null).show();return;}
        String[] labels=new String[quotes.size()];
        for(int i=0;i<quotes.size();i++){
            BookStore.Quote q=quotes.get(i);String t=q.quote.replace('\n',' ').trim();
            labels[i]="§"+(q.chapter+1)+" · "+(t.length()>105?t.substring(0,102)+"…":t);
        }
        new AlertDialog.Builder(this).setTitle("My quotations · "+quotes.size())
            .setItems(labels,(d,index)->{final BookStore.Quote q=quotes.get(index);
                new AlertDialog.Builder(this).setTitle("Underlined passage").setMessage(q.quote)
                    .setPositiveButton("Go to",(di,w)->loadChapter(q.chapter,q.start))
                    .setNeutralButton("Copy",(di,w)->{
                        android.content.ClipboardManager clip=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                        if(clip!=null)clip.setPrimaryClip(ClipData.newPlainText("FOLIO quote",q.quote));
                    })
                    .setNegativeButton("Remove underline",(di,w)->{db.removeUnderline(q.id);if(chapter==q.chapter){refreshUnderlineSpans();showPage();}}).show();
            }).setNeutralButton("Export",(d,w)->{
                StringBuilder all=new StringBuilder(record.title+" — Quotations\n\n");
                for(BookStore.Quote q:quotes)all.append("Chapter ").append(q.chapter+1).append("\n").append(q.quote).append("\n\n");
                Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,all.toString());
                startActivity(Intent.createChooser(send,"Export quotations"));
            }).setNegativeButton("Close",null).show();
    }
    private void readingStyles(){
        final String[] names={"FOLIO Clarity · large, generous spacing","FOLIO Logos · philosophy and Greek","FOLIO Novel · traditional typesetting","Current custom settings"};
        new AlertDialog.Builder(this).setTitle("FOLIO Reading Styles")
            .setItems(names,(d,index)->{
                if(index==3){typography();return;}
                if(index==0){fontKey="sys:sans-serif";fontSp=25;lineSpacing=1.48f;indentDp=0;paragraphGapDp=12;marginDp=17;boldText=true;typeScaleX=1.05f;}
                else if(index==1){fontKey="sys:serif";fontSp=22;lineSpacing=1.38f;indentDp=16;paragraphGapDp=7;marginDp=21;boldText=false;typeScaleX=1.02f;}
                else {fontKey="sys:serif";fontSp=21;lineSpacing=1.25f;indentDp=22;paragraphGapDp=3;marginDp=19;boldText=false;typeScaleX=0.99f;}
                saveTypography();reflow();
                Toast.makeText(this,"Reading style applied",Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Cancel",null).show();
    }
    private void focusMenu(){
        new AlertDialog.Builder(this).setTitle("Focus & battery")
            .setMessage("FOLIO already uses full-screen mode, no network permission, and no wake locks or background services. Android 4.4 cannot reliably force-stop other apps without privileged access. Automatically killing apps can interfere with the launcher, system services and saved work.\n\nYou can manage running apps manually in Android Settings. Disable Wi-Fi in Quick Settings for additional savings.")
            .setPositiveButton("Keep reading",null)
            .setNeutralButton("Manage apps",(d,w)->{
                try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS));}
                catch(Exception e){Toast.makeText(this,"Open Android Settings manually",Toast.LENGTH_LONG).show();}
            }).show();
    }
    /** Full-size, native KitKat-friendly reading controls. Apply once to avoid constant E Ink redraws. */
    private void typography(){
        final ScrollView scroller=new ScrollView(this);
        final LinearLayout controls=Ui.column(this);
        controls.setPadding(Ui.dp(this,19),Ui.dp(this,10),Ui.dp(this,19),Ui.dp(this,12));
        scroller.addView(controls);
        TextView hint=Ui.text(this,"Book typography · settings apply when you tap Apply",13,false);
        hint.setTextColor(Ui.GRAY);controls.addView(hint);
        TextView faceTitle=Ui.text(this,"TYPEFACE",14,true);faceTitle.setPadding(0,Ui.dp(this,17),0,Ui.dp(this,5));controls.addView(faceTitle);
        final ArrayList<FontCatalog.Entry> fonts=FontCatalog.list(this);
        final Spinner fontSelect=new Spinner(this);
        ArrayAdapter<FontCatalog.Entry> spinnerAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,fonts);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontSelect.setAdapter(spinnerAdapter);int chosen=0;
        for(int i=0;i<fonts.size();i++)if(fonts.get(i).key.equals(fontKey)){chosen=i;break;}
        fontSelect.setSelection(chosen);controls.addView(fontSelect,new LinearLayout.LayoutParams(-1,Ui.dp(this,49)));
        final CheckBox heavier=new CheckBox(this);heavier.setText("Heavier text / bold");heavier.setChecked(boldText);
        controls.addView(heavier);
        final SeekBar size=slider(controls,"Font size",14,36,fontSp,"sp");
        final SeekBar lines=slider(controls,"Line spacing",100,190,Math.round(lineSpacing*100f),"%");
        final SeekBar faceWidth=slider(controls,"Character width",92,110,Math.round(typeScaleX*100f),"%");
        final SeekBar indents=slider(controls,"First-line paragraph indent",0,40,indentDp,"dp");
        final SeekBar paragraph=slider(controls,"Space after paragraphs",0,24,paragraphGapDp,"dp");
        final SeekBar margins=slider(controls,"Page margins",4,40,marginDp,"dp");
        final CheckBox foot=new CheckBox(this);foot.setText("Show chapter / page footer");foot.setChecked(footerVisible);
        controls.addView(foot);
        TextView note=Ui.text(this,"EPUB publisher styles may differ. Full justified alignment isn't available in Android 4.4's native text layout.",12,false);
        note.setTextColor(Ui.GRAY);note.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,10));controls.addView(note);
        new AlertDialog.Builder(this).setTitle("FOLIO · Type & spacing").setView(scroller)
            .setNegativeButton("Cancel",null)
            .setNeutralButton("Import TTF / OTF",(d,w)->{
                Intent i=new Intent(this,PickerActivity.class);i.putExtra("mode","font");startActivityForResult(i,PICK_FONT);
            })
            .setPositiveButton("Apply",(d,w)->{
                FontCatalog.Entry selected=(FontCatalog.Entry)fontSelect.getSelectedItem();
                if(selected!=null)fontKey=selected.key;
                boldText=heavier.isChecked();fontSp=14+size.getProgress();
                lineSpacing=(100+lines.getProgress())/100f;typeScaleX=(92+faceWidth.getProgress())/100f;
                indentDp=indents.getProgress();paragraphGapDp=paragraph.getProgress();
                marginDp=4+margins.getProgress();footerVisible=foot.isChecked();
                saveTypography();reflow();
            }).show();
    }
    private SeekBar slider(LinearLayout into,String label,int low,int high,int current,String unit){
        final TextView title=Ui.text(this,label+"  ·  "+current+unit,15,false);
        title.setPadding(0,Ui.dp(this,12),0,0);into.addView(title);
        SeekBar control=new SeekBar(this);control.setMax(high-low);control.setProgress(Math.min(high-low,Math.max(0,current-low)));
        control.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int progress,boolean fromUser){
                title.setText(label+"  ·  "+(low+progress)+unit);
            }
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){}
        });
        into.addView(control,new LinearLayout.LayoutParams(-1,Ui.dp(this,49)));
        return control;
    }
    private void saveTypography(){
        prefs.edit().putInt("font",fontSp).putInt("margin",marginDp).putFloat("spacing",lineSpacing)
            .putInt("indent",indentDp).putInt("paragraph_gap",paragraphGapDp)
            .putString("font_key",fontKey).putBoolean("font_bold",boldText).putFloat("font_width",typeScaleX)
            .putBoolean("footer",footerVisible).apply();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=PICK_FONT||result!=RESULT_OK||data==null)return;
        String path=data.getStringExtra("path");if(path==null)return;
        try{File file=FontCatalog.install(this,new File(path));
            fontKey="file:"+file.getAbsolutePath();saveTypography();reflow();
            Toast.makeText(this,"Typeface added: "+file.getName(),Toast.LENGTH_LONG).show();
        }catch(Exception failure){new AlertDialog.Builder(this).setTitle("Cannot add font")
            .setMessage(failure.getMessage()).setPositiveButton("OK",null).show();}
    }
    @Override public boolean onKeyDown(int code,KeyEvent e){
        if(code==KeyEvent.KEYCODE_VOLUME_DOWN||code==KeyEvent.KEYCODE_DPAD_RIGHT||code==KeyEvent.KEYCODE_PAGE_DOWN){next();return true;}
        if(code==KeyEvent.KEYCODE_VOLUME_UP||code==KeyEvent.KEYCODE_DPAD_LEFT||code==KeyEvent.KEYCODE_PAGE_UP){previous();return true;}
        if(code==KeyEvent.KEYCODE_MENU){menu();return true;}
        return super.onKeyDown(code,e);
    }
    @Override public void onBackPressed(){if(!jumpHistory.isEmpty()){int[] p=jumpHistory.pop();loadChapter(p[0],p[1]);return;}super.onBackPressed();}
    @Override protected void onPause(){cancelHold();touchActive=false;if(started && chapterText!=null && record!=null)persist(startAt(pageIndex));super.onPause();}
    @Override protected void onDestroy(){cancelHold();try{if(epub!=null)epub.close();}catch(IOException ignored){}if(db!=null)db.close();super.onDestroy();}
}
