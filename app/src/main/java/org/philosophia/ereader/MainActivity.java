package org.philosophia.ereader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Native no-animation UI designed for old Crema e-ink readers. */
public final class MainActivity extends Activity {
    private Dict dict;
    private SharedPreferences prefs;
    private final Handler handler=new Handler();
    private Runnable pending;
    private EditText queryBox;
    private ListView resultsList;
    private TextView status;
    private ArrayAdapter<String> adapter;
    private final List<Dict.Entry> displayed=new ArrayList<Dict.Entry>();
    private String query="";
    private int font=19;
    private boolean focused=false;
    private boolean detail=false;
    private Dict.Entry current;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        prefs=getSharedPreferences("paper",MODE_PRIVATE);
        font=prefs.getInt("font",19);focused=prefs.getBoolean("philosophy",false);
        dict=new Dict(this);
        showLoading();
        startDictionary();

    }
    @Override protected void onDestroy() {
        if(pending!=null)handler.removeCallbacks(pending);
        if(dict!=null)dict.close();
        super.onDestroy();
    }
    @Override public void onBackPressed() {
        if(detail)showSearch();else super.onBackPressed();
    }
    private TextView loadingLine;
    private void showLoading() {
        LinearLayout page=newPage();
        headline(page,"A complete dictionary, built into the application");
        page.addView(text("Preparing 1,793,921 definitions for offline reading.",19,true));
        page.addView(text("The first launch automatically unpacks the dictionary from the APK into the app's private storage. No downloads, files, or Internet access are needed. This one-time process requires about 425 MB free space.",16,false));
        loadingLine=text("Checking built-in database...",16,true);
        page.addView(loadingLine);
    }
    private void startDictionary() {
        new Thread(new Runnable(){
            @Override public void run(){
                Exception failure=null;
                try {
                    dict.prepareEmbedded(new Dict.Progress(){
                        @Override public void update(final int percent){
                            if(percent!=100 && percent%5!=0)return;
                            runOnUiThread(new Runnable(){
                                @Override public void run(){
                                    if(!isFinishing() && loadingLine!=null)
                                        loadingLine.setText(percent==100 ?
                                            "Dictionary ready." : "Preparing offline database: "+percent+"%");
                                }
                            });
                        }
                    });
                } catch(Exception e){failure=e;}
                final Exception problem=failure;
                runOnUiThread(new Runnable(){
                    @Override public void run(){
                        if(isFinishing())return;
                        if(problem==null){loadingLine=null;showSearch();}
                        else showSetupError(problem.getMessage());
                    }
                });
            }
        },"embedded-philosophia-setup").start();
    }
    private void showSetupError(String reason) {
        LinearLayout page=newPage();
        headline(page,"Unable to prepare offline database");
        page.addView(text(reason==null?"The bundled data could not be opened.":reason,17,false));
        page.addView(button("RETRY",new Runnable(){
            @Override public void run(){showLoading();startDictionary();}
        }));
        page.addView(text("Please make sure the APK is complete and sufficient device storage is free. No external dictionary file is required.",14,false));
    }
    private int dp(int value) {
        return (int)(value*getResources().getDisplayMetrics().density+0.5f);
    }
    private TextView text(String value,int size,boolean strong) {
        TextView t=new TextView(this);
        t.setText(value);t.setTextColor(Color.BLACK);
        t.setTextSize(size);
        t.setTypeface(Typeface.create(strong?"sans-serif":"serif",
                          strong?Typeface.BOLD:Typeface.NORMAL));
        t.setPadding(dp(5),dp(7),dp(5),dp(7));
        return t;
    }
    private LinearLayout column() {
        LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);
        v.setBackgroundColor(Color.WHITE);return v;
    }
    private LinearLayout row() {
        LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);
        v.setBackgroundColor(Color.WHITE);return v;
    }
    private Button button(String caption,final Runnable action) {
        Button b=new Button(this);b.setText(caption);b.setTextColor(Color.BLACK);
        b.setTextSize(12);b.setAllCaps(false);b.setBackgroundColor(Color.WHITE);
        b.setPadding(dp(3),dp(6),dp(3),dp(6));
        b.setMinHeight(dp(46));b.setMinimumHeight(dp(46));
        b.setOnClickListener(new View.OnClickListener(){
            @Override public void onClick(View v){action.run();}
        });return b;
    }
    private void distribute(LinearLayout line,Button b,int weight) {
        line.addView(b,new LinearLayout.LayoutParams(0,dp(48),weight));
    }
    private void rule(LinearLayout to) {
        View r=new View(this);r.setBackgroundColor(0xff666666);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));
        p.setMargins(0,dp(5),0,dp(5));to.addView(r,p);
    }
    private LinearLayout newPage() {
        LinearLayout r=column();r.setPadding(dp(11),dp(10),dp(11),dp(9));
        setContentView(r);return r;
    }
    private void headline(LinearLayout root,String subtitle) {
        root.addView(text("PHILOSOPHIA",24,true));
        root.addView(text(subtitle,12,false));
        rule(root);
    }
    private void showSearch() {
        detail=false;
        LinearLayout page=newPage();
        headline(page,"The offline philosophical lexicon");
        queryBox=new EditText(this);
        queryBox.setSingleLine(true);queryBox.setTextColor(Color.BLACK);
        queryBox.setHintTextColor(0xff555555);queryBox.setHint("Search a term…");
        queryBox.setBackgroundColor(Color.WHITE);queryBox.setTextSize(20);
        queryBox.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        page.addView(queryBox,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout buttons=row();
        distribute(buttons,button(focused?"◆ PHILOSOPHY":"◇ ALL WORDS",new Runnable(){
            @Override public void run(){
                focused=!focused;prefs.edit().putBoolean("philosophy",focused).apply();
                showSearch();
            }
        }),3);

        page.addView(buttons);rule(page);
        status=text("",12,false);page.addView(status);
        resultsList=new ListView(this);
        resultsList.setBackgroundColor(Color.WHITE);
        resultsList.setCacheColorHint(Color.WHITE);
        resultsList.setScrollingCacheEnabled(false);
        resultsList.setSmoothScrollbarEnabled(false);
        adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,new ArrayList<String>());
        resultsList.setAdapter(adapter);
        resultsList.setOnItemClickListener((parent,v,position,id)->{
            if(position<displayed.size())showEntry(displayed.get(position));
        });
        page.addView(resultsList,new LinearLayout.LayoutParams(-1,0,1));
        rule(page);
        LinearLayout tools=row();
        distribute(tools,button("★ SAVED",new Runnable(){
            @Override public void run(){showSaved();}
        }),2);
        distribute(tools,button("A−",new Runnable(){
            @Override public void run(){font=Math.max(14,font-2);prefs.edit().putInt("font",font).apply();showSearch();}
        }),1);
        distribute(tools,button("A+",new Runnable(){
            @Override public void run(){font=Math.min(32,font+2);prefs.edit().putInt("font",font).apply();showSearch();}
        }),1);
        distribute(tools,button("ABOUT",new Runnable(){
            @Override public void run(){about();}
        }),2);
        page.addView(tools);
        queryBox.setText(query);queryBox.setSelection(query.length());
        queryBox.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                query=s.toString();if(pending!=null)handler.removeCallbacks(pending);
                pending=new Runnable(){@Override public void run(){update();}};
                handler.postDelayed(pending,160);
            }
            @Override public void afterTextChanged(Editable e){}
        });
        update();
    }
    private void update() {
        if(detail||adapter==null)return;
        displayed.clear();adapter.clear();
        try {
            displayed.addAll(dict.search(query,focused));
            for(Dict.Entry e:displayed){
                String description=e.definition.replace('\n',' ');
                if(description.length()>96)description=description.substring(0,96)+"…";
                adapter.add(e.word+"  ["+e.language+"]\n"+description);
            }
            if(query.trim().length()==0)
                status.setText("BUNDLED OFFLINE LEXICON"+" · "+
                   dict.meta("sense_count")+" definitions · "+
                   dict.meta("headword_count")+" distinct headwords");
            else status.setText(displayed.size()+" definitions shown");
            if(displayed.size()==0&&dict.warning.length()>0)status.setText(dict.warning);
        }catch(Exception ex){status.setText("Dictionary error: "+ex.getMessage());}
        adapter.notifyDataSetChanged();
    }
    private String savedKey(Dict.Entry e) {return e.language+"|"+e.word;}
    private boolean isSaved(Dict.Entry e) {
        return prefs.getStringSet("saved",new HashSet<String>()).contains(savedKey(e));
    }
    private void toggleSaved(Dict.Entry e) {
        Set<String> s=new HashSet<String>(prefs.getStringSet("saved",new HashSet<String>()));
        if(!s.add(savedKey(e)))s.remove(savedKey(e));
        prefs.edit().putStringSet("saved",s).apply();
    }
    private void showSaved() {
        Set<String> saved=prefs.getStringSet("saved",new HashSet<String>());
        LinearLayout page=newPage();headline(page,"Bookmarked concepts");
        page.addView(button("‹ BACK TO SEARCH",new Runnable(){
            @Override public void run(){showSearch();}
        }));
        ScrollView sc=new ScrollView(this);LinearLayout items=column();
        for(String key:saved) {
            final String word=key.contains("|")?key.substring(key.indexOf('|')+1):key;
            items.addView(button("★  "+word,new Runnable(){
                @Override public void run(){
                    query=word;showSearch();
                }
            }));
        }
        sc.addView(items);page.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        detail=true;
    }
    private void showEntry(final Dict.Entry entry) {
        current=entry;detail=true;
        LinearLayout root=newPage();
        root.addView(button("‹ BACK TO RESULTS",new Runnable(){
            @Override public void run(){showSearch();}
        }));
        headline(root,entry.language+" · "+entry.pos);
        ScrollView scroll=new ScrollView(this);
        LinearLayout content=column();
        TextView title=text(entry.word,font+10,true);content.addView(title);
        rule(content);
        TextView definition=text(entry.definition,font,false);
        definition.setLineSpacing(dp(4),1.12f);
        definition.setTextIsSelectable(true);content.addView(definition);
        if(entry.tradition!=null&&!entry.tradition.isEmpty())
            content.addView(text("TRADITION: "+entry.tradition,14,false));
        content.addView(text("SOURCE: "+entry.source,12,false));
        scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        rule(root);
        LinearLayout controls=row();
        distribute(controls,button(isSaved(entry)?"★ SAVED":"☆ BOOKMARK",new Runnable(){
            @Override public void run(){toggleSaved(entry);showEntry(entry);}
        }),2);
        distribute(controls,button("COPY",new Runnable(){
            @Override public void run(){
                ClipboardManager m=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                m.setPrimaryClip(ClipData.newPlainText("PHILOSOPHIA",entry.word+"\n"+entry.definition));
            }
        }),1);
        distribute(controls,button("A−",new Runnable(){
            @Override public void run(){font=Math.max(14,font-2);prefs.edit().putInt("font",font).apply();showEntry(entry);}
        }),1);
        distribute(controls,button("A+",new Runnable(){
            @Override public void run(){font=Math.min(32,font+2);prefs.edit().putInt("font",font).apply();showEntry(entry);}
        }),1);
        root.addView(controls);
    }
    private void about() {
        detail=true;
        LinearLayout root=newPage();headline(root,"Database and compatibility");
        root.addView(button("‹ BACK",new Runnable(){@Override public void run(){showSearch();}}));
        ScrollView scroll=new ScrollView(this);LinearLayout content=column();
        content.addView(text("Running Android 4.4 compatible code (API 19 minimum).",15,false));
        content.addView(text("Loaded: COMPLETE BUNDLED DICTIONARY",15,true));
        content.addView(text("Definitions: "+dict.meta("sense_count")+
            "\nUnique language-headwords: "+dict.meta("headword_count"),16,false));
        content.addView(text("File: "+dict.path,12,false));
        content.addView(text("The full dictionary is packaged inside this APK. It is automatically prepared during first launch and requires no separate download, account, Internet connection, or file manager.",14,false));
        if(dict.warning.length()>0)content.addView(text("WARNING: "+dict.warning,13,false));
        content.addView(text("All searches work without Internet, trackers, sign-in, or Google services.",13,false));
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void message(String title,String message){
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton("OK",null).show();
    }
}
