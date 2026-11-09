package org.folio.crema;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.util.*;

/** In-app folder browser works without a working Android DocumentsUI provider. */
public final class PickerActivity extends Activity {
    private LinearLayout list;
    private TextView pathView;
    private File current;
    private boolean pickFont;
    private final ArrayList<File> history=new ArrayList<>();
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);pickFont="font".equals(getIntent().getStringExtra("mode"));show(new File("/storage"));}
    private void show(File dir) {
        if(dir==null || !dir.isDirectory() || !dir.canRead()){
            File fallback=Environment.getExternalStorageDirectory();if(!fallback.isDirectory()||!fallback.canRead())fallback=new File("/");
            dir=fallback;
        }
        current=dir;LinearLayout root=Ui.column(this);root.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),0);
        LinearLayout bar=Ui.row(this);TextView back=Ui.action(this,"‹ Back");back.setOnClickListener(v->onBackPressed());bar.addView(back);
        TextView home=Ui.action(this,"Storage");home.setOnClickListener(v->show(Environment.getExternalStorageDirectory()));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1);hp.leftMargin=Ui.dp(this,8);bar.addView(home,hp);
        root.addView(bar);pathView=Ui.text(this,dir.getAbsolutePath(),14,false);pathView.setPadding(4,Ui.dp(this,14),4,Ui.dp(this,14));root.addView(pathView);Ui.separator(this,root);
        ScrollView scroll=new ScrollView(this);list=Ui.column(this);scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        File[] files=dir.listFiles();if(files==null)files=new File[0];Arrays.sort(files,new Comparator<File>(){@Override public int compare(File a,File b){
            if(a.isDirectory()!=b.isDirectory())return a.isDirectory()?-1:1;return a.getName().compareToIgnoreCase(b.getName());}});
        final File selectedDir=dir;
        if(selectedDir.getParentFile()!=null)row("↖ ..",()->show(selectedDir.getParentFile()));
        boolean any=false;
        for(File f:files){
            if(!f.canRead() || f.getName().startsWith("."))continue;
            if(f.isDirectory()){row("▣  "+f.getName(),()->show(f));any=true;}
            else if(pickFont ? (f.getName().toLowerCase(Locale.US).endsWith(".ttf")||f.getName().toLowerCase(Locale.US).endsWith(".otf")) : (f.getName().toLowerCase(Locale.US).endsWith(".epub")||f.getName().toLowerCase(Locale.US).endsWith(".txt"))){
                row("▤  "+f.getName(),()->{Intent data=new Intent();data.putExtra("path",f.getAbsolutePath());setResult(RESULT_OK,data);finish();});any=true;
            }
        }
        if(!any)row(pickFont?"No TTF/OTF font files here":"No EPUB/TXT books here",()->{});
    }
    private void row(String label,Runnable click){
        TextView t=Ui.text(this,label,17,false);t.setPadding(Ui.dp(this,6),Ui.dp(this,15),Ui.dp(this,6),Ui.dp(this,15));
        t.setOnClickListener(v->click.run());list.addView(t);Ui.separator(this,list);
    }
    @Override public void onBackPressed(){File p=current==null?null:current.getParentFile();if(p==null)super.onBackPressed();else show(p);}
}
