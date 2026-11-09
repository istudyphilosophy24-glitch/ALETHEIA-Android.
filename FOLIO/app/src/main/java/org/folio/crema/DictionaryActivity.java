package org.folio.crema;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

/** Native low-refresh, fully offline PHILOSOPHIA index UI. */
public final class DictionaryActivity extends Activity {
    private FolioDictionary dict;
    private EditText query;
    private TextView info,results;
    private CheckBox philosophy;
    private LinearLayout root;
    private volatile boolean cancelled;
    private String initial="";
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dict=new FolioDictionary(this);initial=getIntent().getStringExtra("query");if(initial==null)initial="";
        root=Ui.column(this);root.setPadding(Ui.dp(this,15),Ui.dp(this,10),Ui.dp(this,15),Ui.dp(this,8));
        TextView head=Ui.text(this,"PHILOSOPHIA · OFFLINE DICTIONARY",20,true);head.setTypeface(android.graphics.Typeface.SERIF);
        root.addView(head,new LinearLayout.LayoutParams(-1,Ui.dp(this,55)));
        TextView about=Ui.text(this,"1,793,921 definition records · 1,356,788 language–headword pairs",12,false);
        about.setTextColor(Color.DKGRAY);root.addView(about);
        LinearLayout searchRow=Ui.row(this);query=new EditText(this);query.setSingleLine(true);query.setTextSize(19);query.setHint("Word or philosophical term");query.setText(initial);
        searchRow.addView(query,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
        TextView go=Ui.action(this,"Find");go.setOnClickListener(v->lookup());searchRow.addView(go);root.addView(searchRow);
        philosophy=new CheckBox(this);philosophy.setText("Prioritize philosophical and technical vocabulary");
        philosophy.setChecked(false);root.addView(philosophy);
        info=Ui.text(this,"Opening the offline lexicon...",13,false);root.addView(info);
        ScrollView scroll=new ScrollView(this);results=Ui.text(this,"",18,false);
        results.setGravity(Gravity.TOP|Gravity.LEFT);results.setPadding(Ui.dp(this,3),Ui.dp(this,9),Ui.dp(this,3),Ui.dp(this,9));
        scroll.addView(results);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView note=Ui.text(this,"Wiktionary-derived multilingual entries · definitions may have several senses · source attribution in About",11,false);
        note.setTextColor(Color.DKGRAY);root.addView(note);
        setContentView(root);
        Thread load=new Thread(()->{
            try{dict.initialize(p->runOnUiThread(()->{if(!cancelled)info.setText("Preparing dictionary: "+p+"%");}));
                runOnUiThread(()->{if(cancelled)return;info.setText("Ready · entirely offline");if(!initial.isEmpty())lookup();});
            }catch(Exception ex){runOnUiThread(()->{if(!cancelled){info.setText("Dictionary unavailable");
                results.setText("Cannot prepare the embedded dictionary:\n\n"+ex.getMessage()+"\n\nPlease free internal storage and try again.");}});}
        },"folio-dictionary-prepare");load.start();
    }
    private void lookup(){if(!dict.ready()){Toast.makeText(this,"Dictionary is preparing",Toast.LENGTH_SHORT).show();return;}
        String word=query.getText().toString().trim();if(word.length()==0)return;
        InputMethodManager input=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if(input!=null)input.hideSoftInputFromWindow(query.getWindowToken(),0);
        info.setText("Searching ...");
        final boolean prioritizePhilosophy=philosophy.isChecked();
        new Thread(()->{
            final ArrayList<FolioDictionary.Entry> matches=dict.lookup(word,prioritizePhilosophy);
            runOnUiThread(()->{if(cancelled)return;
                StringBuilder text=new StringBuilder();int total=0;
                for(FolioDictionary.Entry entry:matches){
                    total++;text.append(entry.word).append("  ·  ").append(entry.lang).append(" · ").append(entry.pos).append("\n")
                        .append(entry.definition).append("\n\n");
                }
                info.setText(matches.size()+" matching definitions");results.setText(total==0?"No definition found for: "+word:text.toString());
            });
        },"folio-lookup").start();
    }
    @Override protected void onDestroy(){cancelled=true;dict.close();super.onDestroy();}
}
