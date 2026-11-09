package org.folio.crema;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.util.LruCache;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** True two-column E Ink bookshelf with cover thumbnails and compact-list fallback. */
public final class MainActivity extends Activity {
    private static final int PICK=101;
    private BookStore db;
    private GridView shelf;
    private ShelfAdapter adapter;
    private EditText search;
    private TextView shelfToggle, summary;
    private ArrayList<BookStore.Book> allBooks=new ArrayList<>(),shown=new ArrayList<>();
    private boolean gridMode=true;
    private final LruCache<String,Bitmap> bitmapCache=new LruCache<String,Bitmap>(4096) {
        @Override protected int sizeOf(String key,Bitmap bitmap){return Math.max(1,bitmap.getByteCount()/1024);}
    };
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        db=new BookStore(this);gridMode=getPreferences(MODE_PRIVATE).getBoolean("shelf_grid",true);makeScreen();
        Intent intent=getIntent();if(Intent.ACTION_VIEW.equals(intent.getAction())&&intent.getData()!=null)importUri(intent.getData());
    }
    @Override protected void onResume(){super.onResume();reload();}
    private void makeScreen(){
        LinearLayout root=Ui.column(this);root.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,5));
        LinearLayout toolbar=Ui.row(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=Ui.text(this,"FOLIO",27,true);brand.setTypeface(Typeface.SERIF,Typeface.BOLD);
        toolbar.addView(brand,new LinearLayout.LayoutParams(0,Ui.dp(this,55),1));
        TextView add=Ui.action(this,"+ Import");add.setOnClickListener(v->startActivityForResult(new Intent(this,PickerActivity.class),PICK));
        toolbar.addView(add);
        TextView resume=Ui.action(this,"Continue");resume.setOnClickListener(v->{
            for(BookStore.Book b:allBooks){if(b.opened>0){open(b);return;}}
            if(!allBooks.isEmpty())open(allBooks.get(0));else toast("Import a book first");
        });toolbar.addView(resume);root.addView(toolbar);
        LinearLayout sub=Ui.row(this);sub.setGravity(Gravity.CENTER_VERTICAL);
        summary=Ui.text(this,"YOUR BOOKSHELF",12,false);summary.setTextColor(Ui.GRAY);
        sub.addView(summary,new LinearLayout.LayoutParams(0,Ui.dp(this,35),1));
        shelfToggle=Ui.action(this,gridMode?"≡ List":"▦ Shelf");shelfToggle.setTextSize(14);
        shelfToggle.setOnClickListener(v->{gridMode=!gridMode;getPreferences(MODE_PRIVATE).edit().putBoolean("shelf_grid",gridMode).apply();
            shelfToggle.setText(gridMode?"≡ List":"▦ Shelf");shelf.setNumColumns(gridMode?2:1);adapter.notifyDataSetChanged();});
        sub.addView(shelfToggle);root.addView(sub);
        search=new EditText(this);search.setTextSize(16);search.setSingleLine(true);search.setHint("Find a book or author");
        search.setPadding(Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8),Ui.dp(this,8));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){filter();}public void afterTextChanged(Editable e){} });
        root.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(this,46)));
        Ui.separator(this,root);
        shelf=new GridView(this);shelf.setBackgroundColor(Color.WHITE);shelf.setNumColumns(gridMode?2:1);
        shelf.setVerticalSpacing(Ui.dp(this,13));shelf.setHorizontalSpacing(Ui.dp(this,10));
        shelf.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);shelf.setPadding(Ui.dp(this,2),Ui.dp(this,13),Ui.dp(this,2),Ui.dp(this,13));
        shelf.setClipToPadding(false);shelf.setSelector(android.R.color.transparent);
        adapter=new ShelfAdapter();shelf.setAdapter(adapter);
        shelf.setOnItemClickListener((parent,v,pos,id)->open(shown.get(pos)));
        shelf.setOnItemLongClickListener((parent,v,pos,id)->{options(shown.get(pos));return true;});
        root.addView(shelf,new LinearLayout.LayoutParams(-1,0,1));
        TextView footer=Ui.text(this,"EPUB · TXT    |    OFFLINE · E INK",11,false);footer.setGravity(Gravity.CENTER);
        footer.setTextColor(Ui.GRAY);root.addView(footer,new LinearLayout.LayoutParams(-1,Ui.dp(this,28)));
        setContentView(root);
    }
    private void reload(){if(db==null||shelf==null)return;allBooks=db.books();filter();}
    private void filter(){if(shelf==null||search==null||adapter==null)return;
        String query=search.getText().toString().trim().toLowerCase(Locale.US);shown.clear();
        for(BookStore.Book b:allBooks)if(query.isEmpty()||(b.title+" "+b.author).toLowerCase(Locale.US).contains(query))shown.add(b);
        summary.setText(query.isEmpty()?(allBooks.size()+" BOOK"+(allBooks.size()==1?"":"S")+" ON YOUR SHELF"):(shown.size()+" MATCHING BOOKS"));
        adapter.notifyDataSetChanged();shelf.invalidateViews();
    }
    private class ShelfAdapter extends BaseAdapter {
        @Override public int getCount(){return shown.size();}
        @Override public Object getItem(int position){return shown.get(position);}
        @Override public long getItemId(int position){return shown.get(position).id.hashCode();}
        @Override public View getView(int position,View convert,android.view.ViewGroup parent){
            BookStore.Book b=shown.get(position);
            LinearLayout tile=Ui.column(MainActivity.this);tile.setGravity(Gravity.CENTER_HORIZONTAL);
            tile.setPadding(Ui.dp(MainActivity.this,2),Ui.dp(MainActivity.this,1),Ui.dp(MainActivity.this,2),Ui.dp(MainActivity.this,4));
            final int coverHeight=gridMode?Ui.dp(MainActivity.this,178):Ui.dp(MainActivity.this,130);
            final int coverWidth=gridMode?Ui.dp(MainActivity.this,124):Ui.dp(MainActivity.this,95);
            LinearLayout.LayoutParams coverParams=new LinearLayout.LayoutParams(coverWidth,coverHeight);
            FrameLayout cover=new FrameLayout(MainActivity.this);
            GradientDrawable border=new GradientDrawable();border.setColor(Color.rgb(237,237,237));border.setStroke(Ui.dp(MainActivity.this,1),Color.BLACK);
            cover.setBackground(border);
            Bitmap bitmap=bitmapCache.get(b.id);
            if(bitmap==null){bitmap=CoverCache.decode(MainActivity.this,b.id);if(bitmap!=null)bitmapCache.put(b.id,bitmap);}
            if(bitmap!=null){ImageView img=new ImageView(MainActivity.this);img.setImageBitmap(bitmap);img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                img.setPadding(Ui.dp(MainActivity.this,2),Ui.dp(MainActivity.this,2),Ui.dp(MainActivity.this,2),Ui.dp(MainActivity.this,2));
                cover.addView(img,new FrameLayout.LayoutParams(-1,-1));}
            else{
                TextView bookFace=new TextView(MainActivity.this);bookFace.setText(b.title.toUpperCase(Locale.US)+"\n\n"+b.author);
                bookFace.setTextColor(Color.rgb(15,15,15));bookFace.setTextSize(gridMode?13:11);bookFace.setTypeface(Typeface.SERIF,Typeface.BOLD);
                bookFace.setGravity(Gravity.CENTER);bookFace.setMaxLines(9);
                bookFace.setPadding(Ui.dp(MainActivity.this,8),Ui.dp(MainActivity.this,8),Ui.dp(MainActivity.this,8),Ui.dp(MainActivity.this,8));
                cover.addView(bookFace,new FrameLayout.LayoutParams(-1,-1));
                View strip=new View(MainActivity.this);strip.setBackgroundColor(Color.rgb(88,88,88));
                FrameLayout.LayoutParams stripParams=new FrameLayout.LayoutParams(Ui.dp(MainActivity.this,3),-1,Gravity.LEFT);
                cover.addView(strip,stripParams);
            }
            if(gridMode){
                tile.addView(cover,coverParams);
                TextView title=Ui.text(MainActivity.this,b.title,14,true);title.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);
                title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,Ui.dp(MainActivity.this,40));tp.topMargin=Ui.dp(MainActivity.this,7);tile.addView(title,tp);
                TextView author=Ui.text(MainActivity.this,b.author,11,false);author.setTextColor(Ui.GRAY);
                author.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);author.setSingleLine(true);author.setEllipsize(TextUtils.TruncateAt.END);
                tile.addView(author,new LinearLayout.LayoutParams(-1,Ui.dp(MainActivity.this,22)));
                View shelfLine=new View(MainActivity.this);shelfLine.setBackgroundColor(Color.rgb(85,85,85));
                LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,Ui.dp(MainActivity.this,2));sl.topMargin=Ui.dp(MainActivity.this,4);tile.addView(shelfLine,sl);
            }else{
                LinearLayout row=Ui.row(MainActivity.this);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(cover,coverParams);
                LinearLayout details=Ui.column(MainActivity.this);details.setPadding(Ui.dp(MainActivity.this,14),0,0,0);
                TextView title=Ui.text(MainActivity.this,b.title,17,true);title.setMaxLines(3);details.addView(title);
                TextView author=Ui.text(MainActivity.this,b.author,13,false);author.setTextColor(Ui.GRAY);details.addView(author);
                TextView format=Ui.text(MainActivity.this,b.format.toUpperCase(Locale.US)+(b.opened>0?" · CONTINUE":""),11,false);format.setTextColor(Ui.GRAY);details.addView(format);
                row.addView(details,new LinearLayout.LayoutParams(0,-2,1));tile.addView(row,new LinearLayout.LayoutParams(-1,-2));
                Ui.separator(MainActivity.this,tile);
            }
            return tile;
        }
    }
    private void open(BookStore.Book b){Intent i=new Intent(this,ReaderActivity.class);i.putExtra("bookId",b.id);startActivity(i);}
    private void options(BookStore.Book b){new AlertDialog.Builder(this).setTitle(b.title).setItems(new String[]{"Read book","Remove imported copy"},(d,which)->{
        if(which==0)open(b);else new AlertDialog.Builder(this).setTitle("Remove from FOLIO?")
        .setMessage("The original file outside this app will not be changed. This removes the private imported copy and its bookmarks.")
        .setNegativeButton("Cancel",null).setPositiveButton("Remove",(dialog,w)->{db.deleteBook(b);CoverCache.remove(this,b.id);bitmapCache.remove(b.id);reload();}).show();}).show();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==PICK&&result==RESULT_OK&&data!=null){String path=data.getStringExtra("path");if(path!=null)importFile(new File(path));}}
    private void importUri(Uri uri){if(uri!=null)importSource(()->getContentResolver().openInputStream(uri),uri.getLastPathSegment());}
    private void importFile(File file){if(!file.isFile()){toast("Cannot read file");return;}importSource(()->new FileInputStream(file),file.getName());}
    private interface Source {InputStream open() throws Exception;}
    private void importSource(Source source,String name){
        final String base=name==null?"book.epub":name;final String ext=base.toLowerCase(Locale.US).endsWith(".txt")?".txt":".epub";
        final ProgressDialog progress=ProgressDialog.show(this,"Adding to your shelf","Importing book and cover…",true,false);
        new Thread(()->{File tmp=null;try{
            File dir=new File(getFilesDir(),"books");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create library directory");
            tmp=File.createTempFile("folio_",".tmp",dir);MessageDigest digest=MessageDigest.getInstance("SHA-256");long total=0;
            try(InputStream in=source.open();OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new IOException("File is inaccessible");byte[] buf=new byte[16384];int n;
                while((n=in.read(buf))!=-1){total+=n;if(total>150L*1024*1024)throw new IOException("File exceeds 150 MB import limit");digest.update(buf,0,n);out.write(buf,0,n);}
            }
            if(total==0)throw new IOException("Empty file");String id=hex(digest.digest());File dest=new File(dir,id+ext);
            if(!dest.exists()){if(!tmp.renameTo(dest)){copy(tmp,dest);tmp.delete();}}
            EpubBook.Meta meta;
            try{meta=EpubBook.inspect(dest);}catch(Exception fail){dest.delete();throw fail;}
            BookStore.Book book=new BookStore.Book();book.id=id;book.title=meta.title;book.author=meta.author;book.path=dest.getAbsolutePath();book.format=ext.substring(1);
            if(ext.equals(".epub"))CoverCache.create(this,dest,id);
            db.upsert(book);runOnUiThread(()->{progress.dismiss();reload();toast("Added to shelf: "+book.title);});
        }catch(Exception e){String msg=e.getMessage()==null?"Damaged or unsupported file":e.getMessage();runOnUiThread(()->{progress.dismiss();new AlertDialog.Builder(this).setTitle("Import failed").setMessage(msg).setPositiveButton("OK",null).show();});}
        finally{if(tmp!=null&&tmp.exists())tmp.delete();}
        },"FOLIO-import").start();
    }
    private static void copy(File from,File to)throws Exception{try(InputStream in=new FileInputStream(from);OutputStream out=new FileOutputStream(to)){
        byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte c:b)s.append(String.format(Locale.US,"%02x",c&255));return s.toString();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){bitmapCache.evictAll();if(db!=null)db.close();super.onDestroy();}
}
