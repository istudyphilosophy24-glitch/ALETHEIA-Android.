package org.aletheia.reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
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
  VoiceBridge voice;
  boolean studyMode=true;
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
    group.addView(text(heading,17,true));TextView sub=text(description,15,false);sub.setPadding(0,dp(6),0,0);sub.setTextIsSelectable(click==null);group.addView(sub);
    if(click!=null)group.setOnClickListener(v->click.run());
    LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(6));content.addView(group,p);
  }
  @Override public void onCreate(Bundle b){
    super.onCreate(b);getWindow().setStatusBarColor(INK);getWindow().setNavigationBarColor(PAPER);
    books=new BookStore(this);edition=pref().getInt("edition",0);section=pref().getInt("section",1);page=pref().getInt("page",0);
    studyMode=pref().getBoolean("studyMode",true);
    load();voice=new VoiceBridge(this);voice.init(pref().getString("ttsEngine",null));open("Home");
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
      case "Reader":try{reader();}catch(Exception error){title("Reader recovery");note("The Reader encountered an error: "+error.getMessage());button("Return to offline study guide",()->{studyMode=true;pref().edit().putBoolean("studyMode",true).apply();open("Reader");});}break;
      case "Audio":audio();break;
      default:LearningModules.render(this,name);
    }
  }
  void home(){
    title("Being and Time");
    note("A native Android philosophical reading laboratory · offline-first");
    card("THE GREAT READER · OPEN NOW","Start with the complete 83-section offline study index. Import your own Macquarrie–Robinson or Stambaugh PDF / EPUB / TXT to reveal its primary text.",()->{studyMode=true;pref().edit().putBoolean("studyMode",true).apply();open("Reader");});
    card("PHILOSOPHICAL LEXICON","German concepts, distinctions and etymological study prompts.",()->open("Glossary"));
    card("ONTOLOGICAL ATLAS","Interactive diagrams of Dasein, worldhood, care and temporality.",()->open("Atlas"));
    card("PHENOMENOLOGICAL LABORATORY","Visual experiments in equipment, anxiety and ecstatic time.",()->open("Lab"));
    card("DIALECTICAL ARENA","Reconstruct philosophical arguments rather than guessing answers.",()->open("Arena"));
    card("SCHOLAR'S WORKSPACE","Your interpretive notebook and local learning records.",()->open("Notes"));
    note("Current focus: §"+section+" — "+Catalog.sectionTitle(section));
    int studied=0;for(int i=1;i<=83;i++)if(pref().getBoolean("done_section_"+i,false))studied++;
    card("YOUR PHILOSOPHICAL PROGRESS",studied+" of 83 sections studied · "+
      (int)(100f*studied/83)+"% · Offline material remains available without importing a book.",null);
    button("Choose a section (§1–§83)",this::pickSection);
    note("Import your own licensed editions privately from Google Drive or phone storage. Full texts are never uploaded to this public GitHub project.");
    button("OPEN READER · offline guide",()->{studyMode=true;open("Reader");});
    button("IMPORT MY MACQUARRIE–ROBINSON EDITION",()->importEditionFile(0));
    button("IMPORT MY STAMBAUGH EDITION",()->importEditionFile(1));
    button("TEST NARRATION AND ELEVENREADER",()->open("Audio"));
  }
  void load(){
    try{paras=books.paragraphs(edition);}catch(Exception e){paras=Collections.emptyList();}
    page=Math.min(page,Math.max(0,(paras.size()-1)/4));
  }
  String editionName(int n){return n==0?"Macquarrie–Robinson":"Stambaugh";}
  void selectEdition(int e){
    edition=e;page=0;studyMode=false;pref().edit().putInt("edition",e).putBoolean("studyMode",false).putInt("page",0).apply();load();open("Reader");
  }
  void reader(){
    title("The Great Reader");
    note("Always usable offline: 83 distinct section orientations. Your legally acquired translation files stay private on this device.");
    button((studyMode?"● ":"○ ")+"ORIGINAL STUDY GUIDE · available offline",()->{
      studyMode=true;pref().edit().putBoolean("studyMode",true).apply();open("Reader");
    });
    button((!studyMode&&edition==0?"● ":"○ ")+"Macquarrie–Robinson · "+(books.isAvailable(0)?"imported":"import required"),()->selectEdition(0));
    button((!studyMode&&edition==1?"● ":"○ ")+"Stambaugh · "+(books.isAvailable(1)?"imported":"import required"),()->selectEdition(1));
    if(studyMode){
      note(StudyGuide.division(section));
      title("§"+section+" · "+Catalog.sectionTitle(section));
      card("PHILOSOPHICAL ORIENTATION",StudyGuide.orientation(section),null);
      String[] curated=Catalog.COMMENTARY.get(section);
      if(curated!=null){
        card("CLOSE READING · "+curated[0],curated[1],null);
        card("INTERPRETIVE DIFFICULTY",curated[2],null);
      }else{
        card("TEXTUAL COMMENTARY NOT YET AUTHORED",
          "This section has an individual study orientation, but no verified paragraph-by-paragraph exegesis. Import a lawful translation and save your own notes while a detailed commentary is developed.",null);
      }
      card("ARGUMENT RECONSTRUCTION",StudyGuide.criticalExercise(section),null);
      button("◀ Previous section",()->{section=Math.max(1,section-1);saveSection();open("Reader");});
      button("Next section ▶",()->{section=Math.min(83,section+1);saveSection();open("Reader");});
      button("Choose section (§1–§83)",this::pickSection);
      button("Listen to the study commentary",()->voice.speak(studyAudio()));
      button("Open this study text in ElevenReader",()->voice.handoffToElevenReader(studyAudio()));
      button("Write my interpretation of §"+section,()->edit("s_"+section,"§"+section+" research note"));
      button("Read my saved interpretation",()->readNote("s_"+section));
      button(pref().getBoolean("done_section_"+section,false)?"✓ Section studied · tap to reset":"Mark this section studied",()->{
        boolean old=pref().getBoolean("done_section_"+section,false);
        pref().edit().putBoolean("done_section_"+section,!old).apply();open("Reader");
      });
      return;
    }
    note("PRIMARY EDITION · "+editionName(edition));
    button("Import from Google Drive / Downloads · PDF, EPUB or TXT",()->importEditionFile(edition));
    button("Locate §"+section+" in this translation (best effort)",()->seekSectionInBook(section));
    if(paras.isEmpty()){
      card("Primary text not installed yet",
        "Your "+editionName(edition)+" library is empty. Import your licensed searchable PDF, EPUB or TXT using Android's document picker. You can select a file stored in Google Drive when Drive is enabled as a file provider. The imported text remains private and offline.",null);
      button("IMPORT BOOK NOW · Drive or Files",()->importEditionFile(edition));
      button("Return to usable offline study reader",()->{
        studyMode=true;pref().edit().putBoolean("studyMode",true).apply();open("Reader");
      });
      return;
    }
    int first=Math.min(page*4,Math.max(0,paras.size()-1));
    int last=Math.min(paras.size(),first+4);
    note("Paragraph "+(first+1)+"–"+last+" / "+paras.size()+" · local file order");
    note("Section-heading navigation is approximate for imported editions; page and paragraph order always remain unchanged.");
    for(int i=first;i<last;i++)readerPassage(i);
    button("A−  Smaller primary text",()->{pref().edit().putInt("readerFont",Math.max(14,pref().getInt("readerFont",18)-1)).apply();open("Reader");});
    button("A+  Larger primary text",()->{pref().edit().putInt("readerFont",Math.min(28,pref().getInt("readerFont",18)+1)).apply();open("Reader");});
    button("◀ Previous page",()->turn(-1));
    button("Next page ▶",()->turn(1));
    button("▶ READ THIS PAGE ALOUD",()->voice.speak(pageText()));
    button("OPEN AUDIO ENGINE AND ELEVENREADER SETTINGS",()->open("Audio"));
    button("Send current page to ElevenReader",()->voice.handoffToElevenReader(pageText()));
    button("Search the imported book",this::searchImportedText);
    button("Bookmark / unbookmark displayed page",()->toggleBookmark(first));
    button("Browse bookmarked passages",this::showBookmarks);
    button("Compare passage indices (unverified)",()->compare(first));
    button("Open §"+section+" commentary",this::commentary);
    button("Select philosophical section",this::pickSection);
  }
  void saveSection(){pref().edit().putInt("section",section).apply();}
  void copyText(String value){
    ClipboardManager manager=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
    manager.setPrimaryClip(ClipData.newPlainText("ALETHEIA text",value));
    Toast.makeText(this,"Text copied. Paste into ElevenReader → Write text.",Toast.LENGTH_LONG).show();
  }
  void seekSectionInBook(int number){
    if(paras.isEmpty()){open("Reader");return;}
    java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("(?iu)^\\s*(?:§\\s*|section\\s+)?"+number+"(?:[.\\s:—-]|$)");
    for(int i=0;i<paras.size();i++){
      String candidate=paras.get(i).trim();
      if(candidate.length()<350&&pattern.matcher(candidate).find()){
        page=i/4;pref().edit().putInt("page",page).apply();open("Reader");
        Toast.makeText(this,"Possible §"+number+" found. Confirm against the printed edition.",Toast.LENGTH_LONG).show();return;
      }
    }
    new AlertDialog.Builder(this).setTitle("§"+number+" not detected")
      .setMessage("This edition may omit standard § markers or the imported PDF may have split headings. Use Search the imported book instead.").setPositiveButton("OK",null).show();
  }
  void readerPassage(int index){
    String passage=paras.get(index);
    LinearLayout panel=new LinearLayout(this);panel.setOrientation(1);
    panel.setPadding(dp(16),dp(16),dp(16),dp(14));
    panel.setBackground(bg(Color.WHITE));
    boolean marked=pref().getBoolean("bookmark_"+edition+"_"+index,false);
    panel.addView(text((marked?"★ ":"")+"¶ "+(index+1)+" · "+editionName(edition),13,true));
    TextView body=text(passage,pref().getInt("readerFont",18),true);
    body.setTypeface(Typeface.SERIF,Typeface.NORMAL);
    body.setTextColor(INK);body.setTextIsSelectable(true);body.setPadding(0,dp(12),0,dp(12));
    panel.addView(body);
    LinearLayout actions=new LinearLayout(this);actions.setOrientation(0);
    for(String label:new String[]{"Explain","Listen","Note","★"}){
      TextView b=text(label,13,true);b.setTextColor(BLUE);b.setPadding(dp(7),dp(11),dp(10),dp(11));
      b.setOnClickListener(v->{
        switch(label){case "Explain":paragraphAnalysis(index);break;case "Listen":voice.speak(passage);break;
          case "Note":edit("p_"+edition+"_"+index,"Your commentary · paragraph "+(index+1));break;
          default:toggleBookmark(index);}
      });
      actions.addView(b);
    }
    panel.addView(actions);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(13);
    content.addView(panel,lp);
  }
  String studyAudio(){
    StringBuilder b=new StringBuilder();
    b.append("Section ").append(section).append(". ").append(Catalog.sectionTitle(section)).append(". ");
    b.append(StudyGuide.orientation(section)).append(" ");
    String[] c=Catalog.COMMENTARY.get(section);
    if(c!=null)b.append(c[1]).append(" Critical question: ").append(c[2]);
    else b.append(StudyGuide.criticalExercise(section));
    return b.toString();
  }
  void importEditionFile(int chosen){
    importEdition=chosen;
    try{
      Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
      i.addCategory(Intent.CATEGORY_OPENABLE);
      i.setType("*/*");
      i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/plain","application/epub+zip","application/pdf","application/octet-stream"});
      startActivityForResult(i,IMPORT);
    }catch(Exception error){
      new AlertDialog.Builder(this).setTitle("Cannot open Android file picker")
       .setMessage(error.getMessage()).setPositiveButton("Close",null).show();
    }
  }
  void paragraphAnalysis(int index){
    if(index<0||index>=paras.size())return;
    String p=paras.get(index);
    StringBuilder found=new StringBuilder();
    String lower=p.toLowerCase(Locale.ROOT);
    for(Map.Entry<String,String> entry:Catalog.LEXICON.entrySet()){
      if(lower.contains(entry.getKey().toLowerCase(Locale.ROOT))){
        if(found.length()>0)found.append("\n\n");
        found.append(entry.getKey()).append(": ").append(entry.getValue());
        if(found.length()>2500)break;
      }
    }
    String msg="ORIGINAL PASSAGE · "+(index+1)+"\n\n"+p.substring(0,Math.min(p.length(),1300))+
      "\n\nCONCEPTUAL CROSS-REFERENCES\n"+
      (found.length()>0?found.toString():"No automatic German-term matches. Do not treat this as evidence that the passage lacks philosophical significance.")+
      "\n\nThis terminology detection is not a paragraph-specific scholarly commentary. Use your note to reconstruct the claim, its textual evidence, and possible objections.";
    new AlertDialog.Builder(this).setTitle("Paragraph study laboratory")
      .setMessage(msg).setPositiveButton("Write commentary",(d,w)->edit("p_"+edition+"_"+index,"Analysis · paragraph "+(index+1)))
      .setNeutralButton("Narrate",(d,w)->voice.speak(p))
      .setNegativeButton("Close",null).show();
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
    if(code!=IMPORT||result!=RESULT_OK||data==null||data.getData()==null)return;
    final android.net.Uri source=data.getData();
    final int target=importEdition;
    Toast.makeText(this,"Importing offline: please wait…",Toast.LENGTH_LONG).show();
    new Thread(()->{
      try{
        books.importDocument(source,target);
        runOnUiThread(()->{
          edition=target;page=0;studyMode=false;
          pref().edit().putInt("edition",edition).putInt("page",0).putBoolean("studyMode",false).apply();
          load();open("Reader");
          Toast.makeText(this,"Import complete. Your book is stored offline.",Toast.LENGTH_LONG).show();
        });
      }catch(Exception e){
        runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Import unsuccessful")
          .setMessage(e.getMessage()).setPositiveButton("Close",null).show());
      }
    },"AletheiaImport").start();
  }
  void toggleBookmark(int index){
    String key="bookmark_"+edition+"_"+index;
    boolean next=!pref().getBoolean(key,false);
    pref().edit().putBoolean(key,next).apply();
    Toast.makeText(this,next?"Paragraph bookmarked offline":"Bookmark removed",Toast.LENGTH_SHORT).show();
    if("Reader".equals(current))open("Reader");
  }
  void showBookmarks(){
    if(paras.isEmpty()){Toast.makeText(this,"Import an edition to bookmark its paragraphs.",Toast.LENGTH_LONG).show();return;}
    ArrayList<Integer> indices=new ArrayList<>();
    ArrayList<String> labels=new ArrayList<>();
    for(int i=0;i<paras.size();i++){
      if(pref().getBoolean("bookmark_"+edition+"_"+i,false)){
        indices.add(i);
        String excerpt=paras.get(i);
        labels.add("¶"+(i+1)+" · "+excerpt.substring(0,Math.min(excerpt.length(),85)));
      }
    }
    if(indices.isEmpty()){Toast.makeText(this,"No bookmarks in this translation yet.",Toast.LENGTH_LONG).show();return;}
    new AlertDialog.Builder(this).setTitle("Saved bookmarks · "+editionName(edition))
      .setItems(labels.toArray(new String[0]),(d,which)->{
        page=indices.get(which)/4;pref().edit().putInt("page",page).apply();open("Reader");
      }).setNegativeButton("Close",null).show();
  }
  void searchImportedText(){
    if(paras.isEmpty()){Toast.makeText(this,"Import a book to search primary text.",Toast.LENGTH_LONG).show();return;}
    EditText input=new EditText(this);
    input.setSingleLine(true);input.setHint("Find a word or phrase");
    LinearLayout wrap=new LinearLayout(this);wrap.setPadding(dp(18),0,dp(18),0);wrap.addView(input);
    new AlertDialog.Builder(this).setTitle("Search · "+editionName(edition))
      .setView(wrap).setPositiveButton("Find",(d,w)->{
        String needle=input.getText().toString().trim();
        if(needle.isEmpty())return;
        Toast.makeText(this,"Searching this edition…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
          ArrayList<Integer> positions=new ArrayList<>();
          ArrayList<String> excerpts=new ArrayList<>();
          String q=needle.toLowerCase(Locale.ROOT);
          for(int i=0;i<paras.size()&&positions.size()<50;i++){
            String paragraph=paras.get(i);
            int k=paragraph.toLowerCase(Locale.ROOT).indexOf(q);
            if(k>=0){
              positions.add(i);
              int left=Math.max(0,k-35);
              int right=Math.min(paragraph.length(),k+needle.length()+80);
              excerpts.add("¶"+(i+1)+" · …"+paragraph.substring(left,right).replace('\n',' ')+"…");
            }
          }
          runOnUiThread(()->{
            if(positions.isEmpty()){
              new AlertDialog.Builder(this).setTitle("No matches").setMessage("No instances of \""+needle+"\" were found in this edition.")
              .setPositiveButton("Close",null).show();return;
            }
            new AlertDialog.Builder(this).setTitle("Search results · "+positions.size()+" (max 50)")
              .setItems(excerpts.toArray(new String[0]),(dialog,index)->{
                page=positions.get(index)/4;pref().edit().putInt("page",page).apply();open("Reader");
              }).setNegativeButton("Close",null).show();
          });
        },"AletheiaSearch").start();
      }).setNegativeButton("Cancel",null).show();
  }
  void passageActions(int index){
    String[] choices={"Narrate paragraph","Analyze concepts in this paragraph","Save private annotation","Read annotation","Bookmark / unbookmark","Open in ElevenReader","Copy paragraph","Compare other translation"};
    new AlertDialog.Builder(this).setTitle("Paragraph "+(index+1)).setItems(choices,(d,i)->{
      if(i==0)speak(paras.get(index));
      if(i==1)paragraphAnalysis(index);
      if(i==2)edit("p_"+edition+"_"+index,"Annotation · paragraph "+(index+1));
      if(i==3)readNote("p_"+edition+"_"+index);
      if(i==4)toggleBookmark(index);
      if(i==5)voice.handoffToElevenReader(paras.get(index));
      if(i==6)copyText(paras.get(index));
      if(i==7)compare(index);
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
      section=i+1;saveSection();d.dismiss();open("Reader");
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
  void speak(String text){voice.speak(text);}
  void share(String text){voice.share(text);}
  void audio(){
    title("Audio Study Studio");
    card("ANDROID SPEECH ENGINE · LIVE STATUS",voice.status(),null);
    note("Selected engine: "+voice.engine());
    note("An external reader may not expose itself as a system Text-to-speech engine. Use the ElevenReader handoff separately when required.");
    button("Refresh speech connection",()->{voice.init(pref().getString("ttsEngine",null));open("Audio");});
    button("Choose a speech engine installed on Android",()->{
      List<TextToSpeech.EngineInfo> options=voice.engines();
      if(options==null||options.isEmpty()){
        new AlertDialog.Builder(this).setTitle("No speech engines detected")
          .setMessage("Open Android's Text-to-speech settings to install or select a compatible speech service.")
          .setPositiveButton("Open settings",(d,w)->voice.settings()).setNegativeButton("Cancel",null).show();
        return;
      }
      String[] labels=new String[options.size()];
      for(int i=0;i<options.size();i++)labels[i]=options.get(i).label;
      new AlertDialog.Builder(this).setTitle("Choose Android TTS service")
        .setItems(labels,(d,i)->{
          String engine=options.get(i).name;
          pref().edit().putString("ttsEngine",engine).apply();
          voice.init(engine);open("Audio");
        }).show();
    });
    button("Use system preferred TTS engine",()->{
      pref().edit().remove("ttsEngine").apply();voice.init(null);open("Audio");
    });
    button("Open Android Text-to-speech output settings",voice::settings);
    note("Speaking speed");
    SeekBar bar=new SeekBar(this);bar.setMax(140);
    bar.setProgress(Math.max(0,Math.min(140,(int)(pref().getFloat("speed",.95f)*100)-55)));
    content.addView(bar);
    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar s,int progress,boolean user){
        float rate=(55f+progress)/100f;
        if(user){pref().edit().putFloat("speed",rate).apply();voice.speed(rate);}
      }
      public void onStartTrackingTouch(SeekBar s){}
      public void onStopTrackingTouch(SeekBar s){}
    });
    button("Test selected narrator",()->voice.speak(
       "Being and Time. The question concerning the meaning of Being is the guiding inquiry."));
    button("Listen to current Reader content",()->voice.speak(studyMode?studyAudio():pageText()));
    button("Stop narration",voice::stop);
    card("ELEVENREADER · EXTERNAL HANDOFF",
      "Try sending text directly to ElevenReader. If it does not accept Android text sharing, the app opens ElevenReader and copies the passage for pasting.",null);
    button("Open current text in ElevenReader",()->voice.handoffToElevenReader(studyMode?studyAudio():pageText()));
    button("Copy current text for ElevenReader → Write text",()->copyText(studyMode?studyAudio():pageText()));
    button("Show Android sharing choices",()->voice.share(studyMode?studyAudio():pageText()));
  }
  @Override protected void onResume(){
    super.onResume();
    if(voice!=null&&voice.returnedFromSettings()){
      pref().edit().remove("ttsEngine").apply();
      voice.init(null);
      if("Audio".equals(current))open("Audio");
    }
  }
  @Override public void onBackPressed(){if(current.equals("Home"))super.onBackPressed();else open("Home");}
  @Override public void onDestroy(){if(voice!=null)voice.destroy();super.onDestroy();}
}
