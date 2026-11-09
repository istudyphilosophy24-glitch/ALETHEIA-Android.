package org.folio.crema;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Library entry point. No network permission, services, or background scanning. */
public final class MainActivity extends Activity {
    private BookStore db;
    private LinearLayout entries;
    private EditText filter;
    private ArrayList<BookStore.Book> cached;
    private static final int PICK=101;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);db=new BookStore(this);buildUi();
        Intent intent=getIntent();if(Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData()!=null)importUri(intent.getData());}
    @Override protected void onResume(){super.onResume();refresh();}
    private void buildUi(){
        LinearLayout root=Ui.column(this);root.setPadding(Ui.dp(this,13),Ui.dp(this,10),Ui.dp(this,13),Ui.dp(this,7));
        LinearLayout title=Ui.row(this);TextView brand=Ui.text(this,"FOLIO",28,true);brand.setTypeface(android.graphics.Typeface.SERIF,android.graphics.Typeface.BOLD);
        title.addView(brand,new LinearLayout.LayoutParams(0,Ui.dp(this,60),1));
        TextView add=Ui.action(this,"+ Import");add.setOnClickListener(v->startActivityForResult(new Intent(this,PickerActivity.class),PICK));
        title.addView(add);root.addView(title);TextView caption=Ui.text(this,"A quiet library. Entirely offline.",14,false);caption.setTextColor(Ui.GRAY);root.addView(caption);
        filter=new EditText(this);filter.setTextSize(16);filter.setSingleLine(true);filter.setHint("Search your library");
        filter.setInputType(android.text.InputType.TYPE_CLASS_TEXT);filter.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int co,int af){}public void onTextChanged(CharSequence s,int st,int before,int count){populate();}
            public void afterTextChanged(android.text.Editable e){}
        });root.addView(filter,new LinearLayout.LayoutParams(-1,Ui.dp(this,54)));
        Ui.separator(this,root);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);entries=Ui.column(this);scroll.addView(entries);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView foot=Ui.text(this,"EPUB · TXT  |  Android 4.4  |  No Wi-Fi required",12,false);foot.setTextColor(Ui.GRAY);foot.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,7));root.addView(foot);setContentView(root);
    }
    private void refresh(){if(db==null||entries==null)return;cached=db.books();populate();}
    private void populate(){if(entries==null||cached==null)return;entries.removeAllViews();String query=filter.getText().toString().trim().toLowerCase(Locale.US);
        int visible=0;for(BookStore.Book b:cached){if(!query.isEmpty()&&!(b.title+" "+b.author).toLowerCase(Locale.US).contains(query))continue;
            visible++;LinearLayout row=Ui.row(this);row.setPadding(0,Ui.dp(this,14),0,Ui.dp(this,14));
            TextView mark=Ui.text(this,"▤",28,false);mark.setGravity(android.view.Gravity.CENTER);row.addView(mark,new LinearLayout.LayoutParams(Ui.dp(this,45),Ui.dp(this,70)));
            LinearLayout detail=Ui.column(this);TextView title=Ui.text(this,b.title,18,true);title.setMaxLines(2);detail.addView(title);
            TextView sub=Ui.text(this,b.author+"  ·  "+b.format.toUpperCase(Locale.US),14,false);sub.setTextColor(Ui.GRAY);detail.addView(sub);
            if(b.opened>0){TextView progress=Ui.text(this,"Resume reading",12,false);detail.addView(progress);}
            row.addView(detail,new LinearLayout.LayoutParams(0,-2,1));row.setOnClickListener(v->open(b));row.setOnLongClickListener(v->{options(b);return true;});
            entries.addView(row);Ui.separator(this,entries);
        }
        if(visible==0){TextView empty=Ui.text(this,cached.size()==0?"Your shelf is empty. Tap + Import to add an EPUB or TXT book.":"No matching books.",17,false);
            empty.setGravity(android.view.Gravity.CENTER);empty.setPadding(Ui.dp(this,12),Ui.dp(this,45),Ui.dp(this,12),Ui.dp(this,45));entries.addView(empty);}
    }
    private void open(BookStore.Book b){Intent i=new Intent(this,ReaderActivity.class);i.putExtra("bookId",b.id);startActivity(i);}
    private void options(BookStore.Book b){new AlertDialog.Builder(this).setTitle(b.title).setItems(new String[]{"Open", "Remove imported copy"},(dialog,which)->{
        if(which==0)open(b);else new AlertDialog.Builder(this).setTitle("Remove from FOLIO?")
            .setMessage("This removes FOLIO's private imported copy, not your original book file.")
            .setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{db.deleteBook(b);refresh();}).show();}).show();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==PICK && result==RESULT_OK && data!=null){String path=data.getStringExtra("path");if(path!=null)importFile(new File(path));}}
    private void importUri(Uri uri){
        if(uri==null)return;importSource(()->getContentResolver().openInputStream(uri),uri.getLastPathSegment());
    }
    private void importFile(File file){if(!file.isFile()){toast("Cannot read file");return;}importSource(()->new FileInputStream(file),file.getName());}
    private interface Source {InputStream open() throws Exception;}
    private void importSource(Source source,String name){
        final String base=name==null?"book.epub":name;final String ext=base.toLowerCase(Locale.US).endsWith(".txt")?".txt":".epub";
        final ProgressDialog progress=ProgressDialog.show(this,"Importing book","Reading file and indexing chapters…",true,false);
        new Thread(()->{
            File tmp=null;try{
                File dir=new File(getFilesDir(),"books");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create library directory");
                tmp=File.createTempFile("folio_",".tmp",dir);MessageDigest digest=MessageDigest.getInstance("SHA-256");long total=0;
                try(InputStream in=source.open();OutputStream out=new FileOutputStream(tmp)){
                    if(in==null)throw new IOException("File is inaccessible");byte[] buf=new byte[16384];int n;
                    while((n=in.read(buf))!=-1){total+=n;if(total>150L*1024*1024)throw new IOException("File exceeds 150 MB import limit");digest.update(buf,0,n);out.write(buf,0,n);}
                }
                if(total==0)throw new IOException("Empty file");String id=hex(digest.digest());File dest=new File(dir,id+ext);
                if(!dest.exists()){
                    if(!tmp.renameTo(dest)){copy(tmp,dest);tmp.delete();}
                }
                EpubBook.Meta m;
                try{m=EpubBook.inspect(dest);}catch(Exception failure){if(!dest.equals(tmp))dest.delete();throw failure;}
                BookStore.Book book=new BookStore.Book();book.id=id;book.title=m.title;book.author=m.author;book.path=dest.getAbsolutePath();book.format=ext.substring(1);
                db.upsert(book);
                runOnUiThread(()->{progress.dismiss();refresh();toast("Imported: "+book.title);});
            }catch(Exception e){String msg=e.getMessage()==null?"Unknown format or damaged file":e.getMessage();runOnUiThread(()->{progress.dismiss();new AlertDialog.Builder(this).setTitle("Import failed").setMessage(msg).setPositiveButton("OK",null).show();});}
            finally {if(tmp!=null&&tmp.exists())tmp.delete();}
        },"FOLIO-import").start();
    }
    private static void copy(File from,File to)throws Exception{try(InputStream i=new FileInputStream(from);OutputStream o=new FileOutputStream(to)){
        byte[] b=new byte[8192];int n;while((n=i.read(b))!=-1)o.write(b,0,n);}}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte c:b)s.append(String.format(Locale.US,"%02x",c&255));return s.toString();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}
