package org.aletheia.reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
  static final int INK=Color.rgb(30,57,76), BLUE=Color.rgb(45,99,142);
  static final int MUTED=Color.rgb(92,112,125), PALE=Color.rgb(236,244,249);
  static final int PAPER=Color.rgb(250,249,247);
  private static final int IMPORT=1701;
  LinearLayout content;
  BookStore books;
  TextToSpeech tts;
  List<String> paras=Collections.emptyList();
  int edition=0,importEdition=0,page=0,section=1,lab=0,arena=0;
  String current="Home";
  final ArrayList<Integer> sequence=new ArrayList<>();
  android.content.SharedPreferences pref(){return getSharedPreferences("aletheia",MODE_PRIVATE);}
  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(14));return d;}
  TextView text(String value,int size,boolean strong){
    TextView t=new TextView(this);t.setText(value);t.setTextSize(size);
    t.setTextColor(strong?INK:MUTED);if(strong)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    t.setLineSpacing(dp(3),1.08f);return t;
  }
  void note(String value){TextView t=text(value,15,false);t.setPadding(0,dp(6),0,dp(6));content.addView(t);}
  void title(String s){TextView t=text(s,24,true);t.setPadding(0,dp(7),0,dp(8));content.addView(t);}
  void button(String label,Runnable click){
    TextView t=text(label,15,true);t.setTextColor(BLUE);t.setGravity(Gravity.CENTER);t.setPadding(dp(12),dp(12),dp(12),dp(12));
    t.setBackground(bg(PALE));t.setOnClickListener(v->click.run());
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(5));content.addView(t,p);
  }
  void card(String heading,String description,Runnable click){
    LinearLayout group=new LinearLayout(this);group.setOrientation(1);group.setPadding(dp(14),dp(13),dp(14),dp(13));group.setBackground(bg(Color.WHITE));
    group.addView(text(heading,17,true));TextView sub=text(description,15,false);sub.setPadding(0,dp(6),0,0);group.addView(sub);
    if(click!=null)group.setOnClickListener(v->click.run());
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(6));content.addView(group,p);
  }
  @Override public void onCreate(Bundle b){
    super.onCreate(b);getWindow().setStatusBarColor(INK);getWindow().setNavigationBarColor(PAPER);
    books=new BookStore(this);edition=pref().getInt("edition",0);section=pref().getInt("section",1);page=pref().getInt("page",0);
    load();makeTts(pref().getString("ttsEngine",null));open("Home");
  }
  void open(String name){
    current=name;
    LinearLayout outer=new LinearLayout(this);outer.setOrientation(1);outer.setBackgroundColor(PAPER);setContentView(outer);
    TextView mast=text("◈  ALETHEIA                                     §"+section,17,true);
    mast.setBackgroundColor(Color.WHITE);mast.setPadding(dp(19),dp(18),dp(12),dp(18));outer.addView(mast);
    ScrollView scroller=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(1);
    content.setPadding(dp(18),dp(18),dp(18),dp(20));scroller.addView(content);
    outer.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
    HorizontalScrollView tabs=new HorizontalScrollView(this);tabs.setHorizontalScrollBarEnabled(false);
    LinearLayout links=new LinearLayout(this);links.setBackgroundColor(Color.WHITE);
    for(String s:new String[]{"Home","Reader","Glossary","Atlas","Lab","Arena","Notes","Audio","Tutor"}){
      TextView tab=text(s,13,name.equals(s));tab.setPadding(dp(13),dp(15),dp(13),dp(15));
      tab.setOnClickListener(v->open(s));links.addView(tab);
    }tabs.addView(links);outer.addView(tabs);
    switch(name){
      case "Home":home();break;
      case "Reader":reader();break;
      case "Audio":audio();break;
      default:LearningModules.render(this,name);
    }
  }
  void home(){
    title("Being and Time");
    note("A native Android philosophical reading laboratory · offline-first");
    card("THE GREAT READER","Two English-translation import slots, passage notes, audio and comparison.",()->open("Reader"));
    card("PHILOSOPHICAL LEXICON","German concepts, distinctions and etymological study prompts.",()->open("Glossary"));
    card("ONTOLOGICAL ATLAS","Interactive diagrams of Dasein, worldhood, care and temporality.",()->open("Atlas"));
    card("PHENOMENOLOGICAL LABORATORY","Visual experiments in equipment, anxiety and ecstatic time.",()->open("Lab"));
    card("DIALECTICAL ARENA","Reconstruct philosophical arguments rather than guessing answers.",()->open("Arena"));
    card("SCHOLAR'S WORKSPACE","Your interpretive notebook and local learning records.",()->open("Notes"));
    note("Current focus: §"+section+" — "+Catalog.sectionTitle(section));
    button("Choose a section (§1–§83)",this::pickSection);
    note("The Macquarrie–Robinson and Stambaugh complete texts are not bundled. Import DRM-free, lawfully obtained editions.");
  }
  void load(){
    try{paras=books.paragraphs(edition);}catch(Exception e){paras=Collections.emptyList();}
    page=Math.min(page,Math.max(0,(paras.size()-1)/4));
  }
  String editionName(int n){return n==0?"Macquarrie–Robinson":"Stambaugh";}
  void selectEdition(int e){
    edition=e;page=0;pref().edit().putInt("edition",e).apply();load();open("Reader");
  }
  void reader(){
    title("The Great Reader");note("Complete offline text after importing each translation.");
    button("Edition A · Macquarrie–Robinson "+(edition==0?"●":""),()->selectEdition(0));
    button("Edition B · Stambaugh "+(edition==1?"●":""),()->selectEdition(1));
    note("Selected: "+editionName(edition)+(books.isAvailable(edition)?" · imported":" · not imported"));
    button("Import TXT or EPUB for this translation",()->{
      importEdition=edition;Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");
      startActivityForResult(intent,IMPORT);
    });
    if(paras.isEmpty()){
      card("No imported primary text","Select a DRM-free EPUB or UTF-8 TXT book file from your phone. The app stores it privately offline.",null);
    }else{
      int first=page*4,last=Math.min(paras.size(),first+4);
      note("Imported paragraph "+(first+1)+" to "+last+" of "+paras.size());
      for(int i=first;i<last;i++){
        int idx=i;String passage=paras.get(idx);
        card("PARAGRAPH "+(i+1),passage,()->passageActions(idx));
      }
      button("◀ Previous paragraphs",()->turn(-1));
      button("Next paragraphs ▶",()->turn(1));
      button("Compare this paragraph number across editions",()->compare(first));
      button("Narrate displayed paragraphs",()->speak(pageText()));
    }
    note("Section commentary is separately indexed using Heidegger's § numbers. Imported editions are NOT automatically paragraph-aligned.");
    button("Open commentary: §"+section,this::commentary);
    button("Choose a different section",this::pickSection);
  }
  String pageText(){
    StringBuilder b=new StringBuilder();
    for(int i=page*4;i<Math.min(paras.size(),page*4+4);i++)b.append(paras.get(i)).append("\n\n");
    return b.toString();
  }
  void turn(int direction){
    page=Math.max(0,Math.min(Math.max(0,(paras.size()-1)/4),page+direction));
    pref().edit().putInt("page",page).apply();open("Reader");
  }
  @Override protected void onActivityResult(int code,int result,Intent data){
    super.onActivityResult(code,result,data);
    if(code==IMPORT&&result==RESULT_OK&&data!=null&&data.getData()!=null){
      try{
        books.importDocument(data.getData(),importEdition);edition=importEdition;page=0;load();
        Toast.makeText(this,"Stored offline",Toast.LENGTH_LONG).show();
      }catch(Exception e){
        new AlertDialog.Builder(this).setTitle("Import failed").setMessage(e.getMessage()).setPositiveButton("Close",null).show();
      }
      open("Reader");
    }
  }
  void passageActions(int index){
    String[] choices={"Narrate paragraph","Save private annotation","Read annotation","Share with compatible external reader","Compare other translation"};
    new AlertDialog.Builder(this).setTitle("Paragraph "+(index+1)).setItems(choices,(d,i)->{
      if(i==0)speak(paras.get(index));
      if(i==1)edit("p_"+edition+"_"+index,"Annotation · paragraph "+(index+1));
      if(i==2)readNote("p_"+edition+"_"+index);
      if(i==3)share(paras.get(index));
      if(i==4)compare(index);
    }).show();
  }
  void compare(int index){
    if(!books.isAvailable(0)||!books.isAvailable(1)){
      new AlertDialog.Builder(this).setMessage("Import both translation files to compare them.").setPositiveButton("OK",null).show();
      return;
    }
    try{
      List<String> other=books.paragraphs(1-edition);
      String a=index<paras.size()?paras.get(index):"No such paragraph";
      String b=index<other.size()?other.get(index):"No matching index";
      new AlertDialog.Builder(this).setTitle("Translation comparison (provisional)")
        .setMessage(editionName(edition)+"\n\n"+a+"\n\n"+editionName(1-edition)+"\n\n"+b+
          "\n\nEqual indices do not prove passages correspond. Verified scholarly alignment is planned.")
        .setPositiveButton("Close",null).show();
    }catch(Exception e){Toast.makeText(this,"Unable to read comparison",Toast.LENGTH_SHORT).show();}
  }
  void pickSection(){
    String[] choices=new String[83];
    for(int i=1;i<=83;i++)choices[i-1]="§"+i+" · "+Catalog.sectionTitle(i);
    new AlertDialog.Builder(this).setTitle("Primary-work sections").setSingleChoiceItems(choices,section-1,(d,i)->{
      section=i+1;pref().edit().putInt("section",section).apply();d.dismiss();commentary();
    }).setNegativeButton("Cancel",null).show();
  }
  void commentary(){
    String[] n=Catalog.COMMENTARY.get(section);
    String result=n==null?"Detailed commentary has not yet been authored for this section; the app will not repeat a generic explanation.":
      n[1]+"\n\nCRITICAL QUESTION\n"+n[2];
    new AlertDialog.Builder(this).setTitle("§"+section+" · "+Catalog.sectionTitle(section))
      .setMessage(result).setNeutralButton("My interpretation",(d,w)->edit("s_"+section,"§"+section+" research note"))
      .setPositiveButton("Close",(d,w)->open(current)).show();
  }
  void edit(String id,String heading){
    EditText e=new EditText(this);e.setMinLines(5);e.setGravity(Gravity.TOP);e.setPadding(dp(14),dp(9),dp(14),dp(9));
    e.setText(pref().getString("note_"+id,""));
    new AlertDialog.Builder(this).setTitle(heading).setView(e)
      .setPositiveButton("Save",(d,w)->{pref().edit().putString("note_"+id,e.getText().toString()).apply();if(current.equals("Notes"))open("Notes");})
      .setNegativeButton("Cancel",null).show();
  }
  void readNote(String id){
    new AlertDialog.Builder(this).setTitle("Saved interpretation").setMessage(pref().getString("note_"+id,"No saved note."))
      .setPositiveButton("Close",null).show();
  }
  void makeTts(String engine){
    if(tts!=null){tts.stop();tts.shutdown();}
    TextToSpeech.OnInitListener listener=status->{
      if(status==TextToSpeech.SUCCESS&&tts!=null){
        tts.setLanguage(Locale.UK);tts.setSpeechRate(pref().getFloat("speed",.95f));
      }
    };
    tts=engine==null?new TextToSpeech(this,listener):new TextToSpeech(this,listener,engine);
  }
  void speak(String s){
    if(s==null||s.trim().isEmpty()){Toast.makeText(this,"Import a passage first.",Toast.LENGTH_SHORT).show();return;}
    if(tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"aletheia-passages");
  }
  void share(String s){
    Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,s);
    startActivity(Intent.createChooser(i,"Send text to a compatible reader"));
  }
  void audio(){
    title("Audio Study Studio");
    note("Select an Android TTS engine, including ElevenReader when its speech service is available to external applications.");
    button("Open Android text-to-speech settings",()->{
      try{startActivity(new Intent("com.android.settings.TTS_SETTINGS"));}
      catch(Exception e){Toast.makeText(this,"Open Android Settings → Text-to-speech",Toast.LENGTH_LONG).show();}
    });
    button("Choose installed speech engine",()->{
      if(tts==null)return;
      List<TextToSpeech.EngineInfo> engines=tts.getEngines();
      if(engines==null||engines.isEmpty())return;
      String[] options=new String[engines.size()];
      for(int i=0;i<engines.size();i++)options[i]=engines.get(i).label;
      new AlertDialog.Builder(this).setTitle("Speech engines").setItems(options,(d,i)->{
        String engine=engines.get(i).name;pref().edit().putString("ttsEngine",engine).apply();makeTts(engine);open("Audio");
      }).show();
    });
    note("Selected engine: "+pref().getString("ttsEngine","Android default"));
    note("Speaking speed");
    SeekBar speed=new SeekBar(this);speed.setMax(190);speed.setProgress((int)(pref().getFloat("speed",.95f)*100));content.addView(speed);
    speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar s,int p,boolean user){
        float rate=Math.max(.45f,p/100f);pref().edit().putFloat("speed",rate).apply();if(tts!=null)tts.setSpeechRate(rate);
      }
      public void onStartTrackingTouch(SeekBar s){}
      public void onStopTrackingTouch(SeekBar s){}
    });
    button("Test Android narration",()->speak("The question concerning the meaning of Being remains our guiding inquiry."));
    button("Read current page",()->speak(pageText()));
    button("Stop",()->{if(tts!=null)tts.stop();});
    button("Open passage in external app",()->share(pageText()));
    note("Android sharing does not guarantee ElevenReader accepts shared passages. Its ability to narrate in-app depends on how ElevenReader implements its Android speech integration.");
  }
  @Override public void onBackPressed(){if(current.equals("Home"))super.onBackPressed();else open("Home");}
  @Override public void onDestroy(){if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
