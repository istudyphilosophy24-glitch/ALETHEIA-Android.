package org.aletheia.reader;

import android.app.AlertDialog;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import java.util.List;
import java.util.Map;

final class LearningModules {
  private static final String[] CHALLENGES={
    "The Question of Being", "The Cartesian Confrontation", "The Hammer & Worldhood",
    "The They versus Selfhood", "The Hermeneutic Circle", "Anxiety versus Fear",
    "The Structure of Care", "The Problem of Reality", "Truth as Disclosedness",
    "Being-toward-Death", "Conscience & Resoluteness", "Ecstatic Temporality"
  };
  private static final String[][] ARGUMENTS={
    {"An inquiry must clarify what it seeks.", "Ordinary use of 'is' does not clarify Being.", "Entities can be examined without settling their Being.", "The question of the meaning of Being must be raised explicitly."},
    {"Dasein always already exists in a meaningful world.", "Theoretical observation is a transformation of involvement.", "Isolated subject and extended object are derivative abstractions.", "A Cartesian substance ontology cannot ground worldhood."},
    {"A tool is encountered in ongoing practical concern.", "Its role depends on other equipment and purposes.", "Failure reveals a background network of references.", "The analysis shows why worldhood is not a heap of objects."},
    {"Everyday intelligibility is socially mediated.", "Public interpretations enable ordinary conduct.", "Anonymity can conceal the ownership of one's possibilities.", "Authenticity modifies rather than abolishes Being-with."},
    {"Interpretation articulates prior understanding.", "Every interpretation works from fore-structures.", "This circularity is unavoidable, not simply a logical vice.", "The task is to enter the circle appropriately and critically."},
    {"Fear takes its bearing from a determinate threatening entity.", "Anxiety lacks such an innerworldly object of threat.", "Worldly significance can lose its familiar grip.", "Anxiety can disclose Being-in-the-world as a whole."},
    {"Existence projects itself toward possibilities.", "Projection is always already factically thrown.", "Absorption in entities makes the present practically significant.", "Care unifies these moments as Dasein's Being."},
    {"Skepticism first isolates a subject from an external world.", "Dasein is primordially Being-in-the-world.", "The allegedly necessary proof presupposes a derivative separation.", "Heidegger challenges the framing of the reality problem."},
    {"A proposition can discover an entity as it is.", "Discovery presupposes the disclosedness of a world.", "Disclosedness cannot be reduced to propositional correctness.", "Truth requires both the existential condition and particular true assertions."},
    {"Death cannot be experienced as my completed actuality.", "Dasein's ending is not the last piece of an assembled object.", "Anticipation discloses an unsurpassable existential possibility.", "Being-toward-death concerns finitude rather than predicting demise."},
    {"Everyday self-interpretation disperses responsibility into the They.", "The call of conscience interrupts this absorption.", "Resoluteness appropriates factical possibilities without a fixed moral recipe.", "Authenticity is an existential modification of everyday existence."},
    {"Care exhibits projected possibility, facticity and involvement.", "These moments belong together in existential self-understanding.", "Future, having-been and present temporalize ecstatically.", "Temporality is interpreted as the ontological meaning of care."}
  };
  private static final String[] ARGUMENT_INSIGHTS={
    "Differentiate the ontic familiarity with entities from the ontological question.",
    "The disagreement concerns the ontological starting point, not whether material things exist.",
    "Breakdown reveals involvement; it is not instantly equivalent to pure presence-at-hand.",
    "Authenticity is not solitary withdrawal from others.",
    "A fore-structure is a condition to refine critically, not a license for arbitrary readings.",
    "Existential anxiety is not a substitute for clinical accounts of anxiety disorders.",
    "Care (Sorge) names a formal structure, not psychological worrying.",
    "Heidegger contests a premise of the skeptical proof, rather than offering an alternative proof.",
    "Correctness remains indispensable even if disclosedness is ontologically more basic.",
    "Anticipation is not a calculation of when one will die.",
    "Conscience here is analyzed existentially, not as a catalogue of moral commands.",
    "The ecstases are not a chronology of three separate clock moments."
  };
  private static final String[] EXPERIMENTS={"The equipment workshop","Anxiety and significance","Ecstatic temporality"};
  private static final String[] EXPERIMENT_NOTES={
    "The workshop shows how equipment belongs to a context of practical involvement. Use the slider to introduce disruption. A broken hammer is not automatically equivalent to an object present-at-hand.",
    "The visual field becomes less familiar as significance withdraws. This is a teaching analogy for the existential analysis of anxiety, not a simulation of a clinical disorder.",
    "The three temporal ecstases form a unity rather than a simple sequence of dated events. Use the slider to explore how the diagram represents their interrelatedness."
  };
  static void render(MainActivity m,String screen){
    switch(screen){
      case "Glossary":glossary(m);break;
      case "Atlas":atlas(m);break;
      case "Lab":lab(m);break;
      case "Arena":arena(m);break;
      case "Notes":notes(m);break;
      case "Tutor":tutor(m);break;
      default:m.home();
    }
  }
  private static void glossary(MainActivity m){
    m.title("The Philosophical Lexicon");
    m.note("Search the German term or its English philosophical explanation.");
    EditText input=new EditText(m);input.setSingleLine(true);input.setHint("e.g. Zuhandenheit or Care");
    m.content.addView(input);
    LinearLayout result=new LinearLayout(m);result.setOrientation(1);m.content.addView(result);
    Runnable populate=()->{
      result.removeAllViews();
      String q=input.getText().toString().toLowerCase(java.util.Locale.ROOT);int count=0;
      for(Map.Entry<String,String> concept:Catalog.LEXICON.entrySet()){
        String term=concept.getKey(),meaning=concept.getValue();
        if(!term.toLowerCase(java.util.Locale.ROOT).contains(q)&&!meaning.toLowerCase(java.util.Locale.ROOT).contains(q))continue;
        count++;
        LinearLayout box=new LinearLayout(m);box.setOrientation(1);
        box.setBackground(m.bg(Color.WHITE));box.setPadding(m.dp(13),m.dp(12),m.dp(13),m.dp(12));
        box.addView(m.text(term,18,true));box.addView(m.text(meaning,15,false));
        box.setOnClickListener(v->new AlertDialog.Builder(m).setTitle(term).setMessage(meaning)
          .setPositiveButton("Close",null).show());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,m.dp(6),0,m.dp(6));result.addView(box,p);
      }
      if(count==0)result.addView(m.text("No entry matches your search.",15,false));
    };
    input.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int start,int count,int after){}
      public void onTextChanged(CharSequence s,int start,int before,int count){populate.run();}
      public void afterTextChanged(Editable e){}
    });populate.run();
  }
  private static void atlas(MainActivity m){
    m.title("The Ontological Atlas");
    m.note("A graphical path through the guiding question, the analytic of Dasein, care and temporality.");
    m.content.addView(new VisualView(m,0),new LinearLayout.LayoutParams(-1,m.dp(405)));
    m.card("Conceptual dependencies","A simplified pathway, not a proof of deductive entailment. Worldhood, Being-with, and understanding interrelate as existential structures.",null);
    m.button("Commentary at current §"+m.section,m::commentary);
    m.button("Change the philosophical section",m::pickSection);
  }
  private static void lab(MainActivity m){
    m.title("Phenomenological Laboratory");
    for(int i=0;i<EXPERIMENTS.length;i++){
      int index=i;m.button((m.lab==i?"● ":"○ ")+EXPERIMENTS[i],()->{m.lab=index;m.open("Lab");});
    }
    m.content.addView(new VisualView(m,m.lab+1),new LinearLayout.LayoutParams(-1,m.dp(275)));
    m.card("Conceptual experiment",EXPERIMENT_NOTES[m.lab],null);
    VisualView panel=new VisualView(m,m.lab+1);
    // Keep an independent interactive diagram for the slider below.
    m.content.addView(panel,new LinearLayout.LayoutParams(-1,m.dp(170)));
    SeekBar control=new SeekBar(m);control.setMax(100);control.setProgress(30);m.content.addView(control);
    control.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
      public void onProgressChanged(SeekBar s,int value,boolean user){panel.intensity=value;panel.invalidate();}
      public void onStartTrackingTouch(SeekBar s){}
      public void onStopTrackingTouch(SeekBar s){}
    });
    m.button("Write my phenomenological observation",()->m.edit("lab_"+m.lab,"Observation · "+EXPERIMENTS[m.lab]));
    m.button("Review saved observation",()->m.readNote("lab_"+m.lab));
  }
  private static int[] arenaOrder(MainActivity m){
    int seed=m.pref().getInt("arena_seed_"+m.arena,431+m.arena*919);
    java.util.ArrayList<Integer> order=new java.util.ArrayList<>();
    for(int i=0;i<4;i++)order.add(i);
    java.util.Collections.shuffle(order,new java.util.Random(seed));
    int[] result=new int[4];
    for(int i=0;i<4;i++)result[i]=order.get(i);
    return result;
  }
  private static void arena(MainActivity m){
    m.title("The Dialectical Arena");
    m.note("Twelve different philosophical encounters. Build the argument by selecting its four claims in an order that supports the conclusion.");
    m.button("ENCOUNTER "+(m.arena+1)+"/"+CHALLENGES.length+" · "+CHALLENGES[m.arena],()->{
      String[] names=new String[CHALLENGES.length];
      for(int i=0;i<names.length;i++)names[i]=(m.pref().getBoolean("arena_"+i,false)?"✓ ":"◇ ")+(i+1)+". "+CHALLENGES[i];
      new AlertDialog.Builder(m).setTitle("Choose encounter").setItems(names,(d,index)->{
        m.arena=index;m.sequence.clear();m.open("Arena");
      }).setNegativeButton("Cancel",null).show();
    });
    VisualView visual=new VisualView(m,4);
    visual.intensity=m.sequence.size()*25;
    m.content.addView(visual,new LinearLayout.LayoutParams(-1,m.dp(220)));
    int[] order=arenaOrder(m);
    for(int display=0;display<4;display++){
      final int index=order[display];
      boolean selected=m.sequence.contains(index);
      m.card((selected?"✓ ":"◇ ")+ARGUMENTS[m.arena][index],
        selected?"Chosen position "+(m.sequence.indexOf(index)+1):"Tap to place this proposition",
        ()->{if(m.sequence.contains(index))m.sequence.remove((Integer)index);else m.sequence.add(index);m.open("Arena");});
    }
    m.note("Reconstructed claims: "+m.sequence.size()+" / 4");
    m.button("TEST MY RECONSTRUCTION",()->{
      boolean accepted=m.sequence.size()==4;
      for(int i=0;i<m.sequence.size();i++)if(m.sequence.get(i)!=i)accepted=false;
      if(accepted)m.pref().edit().putBoolean("arena_"+m.arena,true).apply();
      new AlertDialog.Builder(m).setTitle(accepted?"Encounter resolved":"Revise your reasoning")
        .setMessage((accepted?"The intended reasoning has been reconstructed. ":"The inference sequence needs work. ")+
          ARGUMENT_INSIGHTS[m.arena]+
          (accepted?"\n\nNow write a counterargument in your Scholar's Workspace.":
            "\n\nIdentify which premise grounds the next, then try again."))
        .setPositiveButton("Continue",null).show();
    });
    m.button("Write a philosophical objection",()->m.edit("arena_"+m.arena,"Objection · "+CHALLENGES[m.arena]));
    m.button("RESTART with a new proposition order",()->{
      m.pref().edit().putInt("arena_seed_"+m.arena,new java.util.Random().nextInt()).apply();
      m.sequence.clear();m.open("Arena");
    });
    int completed=0;for(int i=0;i<ARGUMENTS.length;i++)if(m.pref().getBoolean("arena_"+i,false))completed++;
    m.note("Resolved encounters: "+completed+"/"+CHALLENGES.length+". Your own objections are kept privately on your device.");
  }
  private static void notes(MainActivity m){
    m.title("Scholar's Workspace");
    m.note("Your private interpretive and research notes are saved on the device.");
    m.button("EXPORT MY NOTEBOOK + READING PROGRESS",m::exportNotebook);
    m.button("RESTORE A PREVIOUS RESEARCH BACKUP",m::importNotebookBackup);
    m.button("Write a thesis, objection, or research question",()->m.edit("general","Your research notebook"));
    String general=m.pref().getString("note_general","");
    if(!general.isEmpty())m.card("General research notebook",general,()->m.edit("general","Research notes"));
    m.button("Write interpretation of current §"+m.section,()->m.edit("s_"+m.section,"Section commentary"));
    for(int i=1;i<=83;i++){
      String saved=m.pref().getString("note_s_"+i,"");
      if(!saved.isEmpty()){
        int index=i;m.card("§"+i+" · "+Catalog.sectionTitle(i),saved,
          ()->m.edit("s_"+index,"Your interpretation of §"+index));
      }
    }
    for(int i=0;i<3;i++){
      String saved=m.pref().getString("note_lab_"+i,"");
      if(!saved.isEmpty()){
        int index=i;m.card(EXPERIMENTS[i],saved,()->m.edit("lab_"+index,"Phenomenological observation"));
      }
    }
    m.note("Reader-specific paragraph annotations can be accessed by tapping imported passages.");
  }
  private static void tutor(MainActivity m){
    m.title("Hermeneutic Tutor");
    m.note("Offline, text-grounded study prompts. This prototype does not yet offer a generative AI conversation.");
    m.button("Choose text section",m::pickSection);
    String[] entry=Catalog.COMMENTARY.get(m.section);
    if(entry==null){
      m.card("§"+m.section+" · "+Catalog.sectionTitle(m.section),
        "A detailed study commentary has not yet been written for this section. Select an annotated section to work with.",null);
      return;
    }
    m.card("§"+m.section+" · "+entry[0],entry[1],null);
    m.card("Scholarly challenge",entry[2],null);
    m.button("Draft an answer or counterargument",()->m.edit("tutor_"+m.section,"Your analysis of §"+m.section));
    m.button("Review drafted response",()->m.readNote("tutor_"+m.section));
    m.note("Distinguish Heidegger's claims, later interpretations, and your own inferences in every answer.");
  }
  private LearningModules(){}
}
