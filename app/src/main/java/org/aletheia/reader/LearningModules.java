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
  private static final String[][] ARGUMENTS={
    {"Practical activity first encounters equipment as significant.",
     "Equipment takes its role from a network of involvements.",
     "A meaningful network cannot be reduced to isolated neutral objects.",
     "The analysis of equipment discloses the phenomenon of worldhood."},
    {"Dasein already understands itself amid a meaningful world.",
     "Detached observation is one possible modification of practical involvement.",
     "The subject-object separation is not the original starting point.",
     "Knowing is therefore a founded way of Being-in-the-world."},
    {"Care unites projection, thrownness, and falling.",
     "These structures indicate how Dasein exists in terms of possibilities.",
     "Future, having-been, and present belong together ecstatically.",
     "Temporality gives the ontological meaning of care."}
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
  private static void arena(MainActivity m){
    m.title("The Dialectical Arena");
    m.note("Reconstruct an argument in the philosophically intended order. Tap four premises from first to last.");
    m.button("Encounter "+(m.arena+1)+" / 3 · switch",()->{m.arena=(m.arena+1)%3;m.sequence.clear();m.open("Arena");});
    int[][] permutations={{2,0,3,1},{3,1,0,2},{1,3,0,2}};
    for(int display=0;display<4;display++){
      int index=permutations[m.arena][display];
      boolean selected=m.sequence.contains(index);
      m.card((selected?"✓ ":"◇ ")+ARGUMENTS[m.arena][index],
        selected?"Chosen position "+(m.sequence.indexOf(index)+1):"Tap to append this claim",
        ()->{if(m.sequence.contains(index))m.sequence.remove((Integer)index);else m.sequence.add(index);m.open("Arena");});
    }
    m.note("Propositions selected: "+m.sequence.size()+" / 4.");
    m.button("Evaluate argument structure",()->{
      boolean accepted=m.sequence.size()==4;
      for(int i=0;i<m.sequence.size();i++)if(m.sequence.get(i)!=i)accepted=false;
      if(accepted)m.pref().edit().putBoolean("arena_"+m.arena,true).apply();
      new AlertDialog.Builder(m)
        .setTitle(accepted?"Valid intended reconstruction":"Argument requires revision")
        .setMessage(accepted?"The stated argument follows its intended order. Next, identify its assumptions and possible objections.":
          "Start with the grounding claim. Trace the intermediate conceptual distinctions before arriving at the conclusion.")
        .setPositiveButton("Continue",null).show();
    });
    m.button("Start this challenge again",()->{m.sequence.clear();m.open("Arena");});
    int completed=0;for(int i=0;i<3;i++)if(m.pref().getBoolean("arena_"+i,false))completed++;
    m.note("Boss encounters completed: "+completed+" of 3.");
  }
  private static void notes(MainActivity m){
    m.title("Scholar's Workspace");
    m.note("Your private interpretive and research notes are saved on the device.");
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
