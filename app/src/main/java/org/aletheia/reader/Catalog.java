package org.aletheia.reader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Catalog {
  public static final String TITLE = "Being and Time";
  public static final Map<String,String> LEXICON = new LinkedHashMap<>();
  public static final Map<Integer,String[]> COMMENTARY = new LinkedHashMap<>();
  /** Independently written navigation themes; these are not verbatim section titles. */
  public static final String[] SECTION_THEMES = {
    "Why the question of Being must be reopened","The formal structure of the question",
    "Ontological priority of the question","Ontic priority of the question",
    "Dasein's analytic and the ontological horizon","Destruction of the history of ontology",
    "Method: phenomenology","Outline of the treatise","The analytic of Dasein",
    "Demarcation from anthropology and psychology","The difficulty of a natural concept of world",
    "Being-in-the-world as a fundamental structure","Knowing the world as a derivative mode",
    "The idea of worldhood","Entities encountered in the environment",
    "Equipmental breakdown and worldly character","Signs and reference",
    "Involvement, significance and worldhood","Descartes and the world as extension",
    "Grounds of Cartesian world-ontology","Hermeneutic critique of Cartesian worldhood",
    "Spatiality of the ready-to-hand","Spatiality of Being-in-the-world","Space and Dasein's spatiality",
    "Who is Dasein?","Others and everyday Being-with","The They and everyday selfhood",
    "The task of analyzing Being-in","Attunement and Being-there","Fear as an attunement",
    "Understanding as projection","Understanding and interpretation","Assertion as derivative interpretation",
    "Discourse and language","Idle talk","Curiosity","Ambiguity","Falling and thrownness",
    "The structural whole of Dasein","Anxiety as disclosure","Care as Dasein's Being",
    "Pre-ontological testimony to care","Worldhood and Reality","Disclosedness and truth",
    "Result of the preparatory analytic","The difficulty of grasping Dasein as a whole",
    "Experiencing the death of others","Outstanding possibilities, ending and totality",
    "Existential analysis and other accounts of death","The existential structure of death",
    "Everyday Being-toward-death","Everyday Being-toward-the-end","Authentic Being-toward-death",
    "Attesting an authentic possibility","Foundations of conscience","Conscience as a call",
    "Conscience as the call of care","Understanding guilt and the appeal",
    "Existential versus everyday readings of conscience","Resoluteness and authentic possibility",
    "Methodological transition to temporality","Anticipatory resoluteness","The hermeneutic situation",
    "Care and selfhood","Temporality as the meaning of care",
    "Repeating the analytic more primordially","Sketch of the temporal interpretation",
    "The temporality of disclosedness","Being-in-the-world and temporality",
    "The temporality of spatiality","The temporal character of everydayness","The problem of history",
    "Ordinary history and Dasein's happening","Basic structure of historicality",
    "Historicality and world history","The origin of historiography","Dilthey and Yorck",
    "Limits of the temporal analysis","Temporality and reckoning with time","World-time and within-time-ness",
    "The ordinary conception of time","Temporality, Dasein and Hegel's conception of time",
    "The unfinished question of fundamental ontology"
  };
  public static final String[] DIVISIONS = {
    "Introduction: Exposition of the Question of the Meaning of Being",
    "Part One, Division I: Preparatory Fundamental Analysis of Dasein",
    "Part One, Division II: Dasein and Temporality"
  };
  public static final String[] TOPICS = {
    "The Question of Being", "Dasein", "Being-in-the-world",
    "Worldhood", "Readiness-to-hand", "Being-with", "The They",
    "Understanding", "Disposedness", "Anxiety", "Care",
    "Death", "Conscience", "Resoluteness", "Temporality",
    "Historicity", "Everyday Time"
  };
  static {
    add("Erschlossenheit","Disclosedness: Dasein's openness in which self, others and world can be encountered as meaningful.");
    add("Jemeinigkeit","Minenness: Dasein is in each case its own; this does not amount to a private Cartesian subject.");
    add("Das Worumwillen","For-the-sake-of-which: the existential possibility orienting assignments and practical involvement.");
    add("Das Um-zu","In-order-to: the equipmental referential structure of action directed toward a task.");
    add("Zeugzusammenhang","Equipmental totality: interrelated roles within which particular tools acquire practical significance.");
    add("Umsicht","Circumspection: the practically oriented understanding that guides absorbed dealings with equipment.");
    add("Auffälligkeit","Conspicuousness: the unobtrusive equipment becoming noticeable when unusable.");
    add("Aufdringlichkeit","Obtrusiveness: equipment's absence revealing its importance in a practical task.");
    add("Aufsässigkeit","Obstinacy: equipment obstructing a task and drawing attention to a background of involvements.");
    add("Mitdasein","Being-there-with: others are disclosed within the shared world of Dasein.");
    add("Fürsorge","Solicitude: modes of Being-with others, including forms of leaping-in and leaping-ahead.");
    add("Gerede","Idle talk: public circulation of interpreted meanings without necessarily firsthand understanding.");
    add("Neugier","Curiosity: a tendency toward constant novel seeing that may evade sustained involvement.");
    add("Zweideutigkeit","Ambiguity: shared public interpretations can conceal lack of genuine understanding.");
    add("Schuld","Guilt: existential nullity and responsibility, distinct from merely juridical or moral transgression.");
    add("Vorlaufen","Anticipation: an authentic relation to finite possibility, not the forecasting of a dated demise.");
    add("Wiederholung","Retrieval or repetition: an appropriative relation to inherited historical possibilities.");
    add("Weltzeit","World-time: datable, public and significant time in practical concern, not mere abstract clock sequence.");
    add("Sein", "Being. The guiding question of the work concerns the meaning of Being, not an additional entity.");
    add("Seiendes", "An entity or being: that which is. Heidegger distinguishes the Being of entities from entities themselves.");
    add("Dasein", "The entity for whom its own Being is at issue. Neither a Cartesian subject nor simply a biological human organism.");
    add("In-der-Welt-sein", "Being-in-the-world. A unitary structure of Dasein, not a subject first existing inside a spatial container.");
    add("Weltlichkeit", "Worldhood: the structure of meaningful worldly significance that makes encounters with entities possible.");
    add("Zuhandenheit", "Readiness-to-hand: the way equipment is disclosed within absorbed practical involvement.");
    add("Vorhandenheit", "Presence-at-hand: an ontological mode in which entities are encountered as determinate things.");
    add("Zeug", "Equipment, intelligible within relations of involvement and use rather than as isolated things.");
    add("Bewandtnis", "Involvement: the relations through which equipment has a role within a practical totality.");
    add("Bedeutsamkeit", "Significance: the relational structure of meaningful reference constituting worldhood.");
    add("Mitsein", "Being-with: Dasein is constitutively related to others, even when alone.");
    add("Das Man", "The they: anonymous public norms through which everyday Dasein interprets its possibilities.");
    add("Befindlichkeit", "Disposedness or state-of-mind: Dasein finds itself already attuned to a world.");
    add("Verstehen", "Understanding: Dasein's projection into possibilities, not primarily theoretical comprehension.");
    add("Auslegung", "Interpretation: articulated development of the possibilities disclosed in understanding.");
    add("Rede", "Discourse: the articulation of intelligibility, equiprimordial with attunement and understanding.");
    add("Sorge", "Care: the structural unity of Dasein's Being, comprising existence, facticity, and falling.");
    add("Geworfenheit", "Thrownness: Dasein always already finds itself in an unchosen factical situation.");
    add("Entwurf", "Projection: Dasein exists in terms of possibilities that it can take up.");
    add("Verfallen", "Falling: Dasein's absorption in its everyday public world.");
    add("Angst", "Anxiety: a fundamental attunement that discloses Being-in-the-world as such.");
    add("Eigentlichkeit", "Authenticity: owning one's possibilities, not an ethical command to be eccentric.");
    add("Uneigentlichkeit", "Inauthenticity: a mode of existence, not simply a moral defect.");
    add("Sein-zum-Tode", "Being-toward-death: existential relation to Dasein's ownmost, nonrelational possibility.");
    add("Entschlossenheit", "Resoluteness: the authentic disclosure of a factical situation through conscience.");
    add("Zeitlichkeit", "Temporality: the ecstatic unity of future, having-been, and present as the meaning of care.");
    add("Geschichtlichkeit", "Historicity: Dasein's temporal constitution as a being stretched between birth and death.");
    add("Ontologische Differenz", "Ontological difference: the distinction between Being and entities.");

    note(1,"Why ask again?","Heidegger argues that Being is apparently familiar yet not conceptually clarified. Universal applicability, indefinability and supposed self-evidence are precisely reasons to investigate, not to stop.","Objection: Does ordinary competence already involve an implicit understanding sufficient to begin? Distinguish a tacit understanding from explicit ontology.");
    note(2,"Form of the question","A philosophical question has what is asked about, what is interrogated, and what is sought. In this case, entities are interrogated to disclose the meaning of their Being.","Test: Why can no particular entity itself be the answer to the question of Being?");
    note(4,"Ontic priority of Dasein","Dasein is distinctive because its Being matters to it, and it already lives with an understanding of Being, however unthematic.","Challenge: Explain why Dasein's priority need not entail subjective idealism.");
    note(7,"The phenomenological method","Phenomenology concerns letting that which shows itself be seen from itself. Interpretation must reveal structures that everyday appearances can conceal.","Compare phenomenon, seeming, appearance and manifestation without conflating them.");
    note(9,"The analytic of Dasein","Dasein's essence lies in its existence; its possibilities are not properties of a fixed object understood merely by categories.","Question: Why should the analytic use existentials instead of categories?");
    note(12,"Being-in-the-world","The compound names a unitary phenomenon. Dasein is not first an inner consciousness that must bridge toward external objects.","Try to formulate the strongest Cartesian alternative and locate Heidegger's point of disagreement.");
    note(13,"Knowing the world","Theoretical cognition is interpreted as a founded way of Being-in-the-world, rather than the primary relation between a subject and an object.","Does founded mean dispensable? Consider whether cognition reveals features that absorbed activity does not.");
    note(14,"Worldhood","Heidegger seeks the ontological structure of world, rather than an inventory of all present-at-hand objects.","What happens to the concept of world when meaningful involvements are omitted?");
    note(15,"The workshop","A hammer belongs to an equipmental totality and is normally encountered in circumspective activity, not first as a detachable bearer of properties.","The hammer's usefulness is not a merely subjective value added to a neutrally present thing.");
    note(16,"Breakdown","Conspicuous, obtrusive, and obstinate equipment can reveal the normally recessive relations of involvement.","Avoid the simplification that every broken tool is thereby fully present-at-hand.");
    note(17,"Signs and reference","Signs make a network of references conspicuous and help to clarify the referential structure of worldhood.","Can a sign be explained by a private mental representation alone?");
    note(18,"Significance","The with-which and for-the-sake-of-which interconnect through a totality of involvements.","Trace the hammer, nails, shelter, and practices back to Dasein's projects.");
    note(25,"Who Dasein is","The question of the 'who' of everyday Dasein is not answered merely by pointing to a self-identical subject.","Investigate social constitution without collapsing individual Dasein into collective anonymity.");
    note(27,"The they","Publicness enables shared intelligibility while tending toward averageness and leveling.","Why isn't the they simply a group of identifiable other people?");
    note(29,"Attunement","Moods disclose thrownness; they are not just psychological colors layered on neutral experiences.","Compare fear as an orientation toward an entity with anxiety's distinct disclosive character.");
    note(31,"Understanding","Dasein exists as possibility and projects itself in terms of what it can be.","Projection is not merely a conscious plan or optional act of imagination.");
    note(32,"Interpretation","Interpretation elaborates an understanding already at work, structured by fore-having, fore-sight, and fore-conception.","Is the hermeneutic circle vicious? Give a reasoned answer.");
    note(40,"Anxiety","In anxiety, ordinary involvements lose their familiar grip and Being-in-the-world itself comes to the fore.","Do not equate existential anxiety with a clinical anxiety disorder.");
    note(41,"Care","Care integrates ahead-of-itself, already-being-in, and being-alongside; these are not three independently assembled components.","Reconstruct why care unifies the preceding existential structures.");
    note(45,"Dasein as a whole","An account of Dasein's Being must face its possible wholeness, including Being-toward-death.","Why is completeness here different from finishing an inventory?");
    note(50,"Death","Death is analyzed existentially as Dasein's ownmost possibility, not just as a biological event.","Compare death as an occurrence to Being-toward-death as a structure.");
    note(54,"Conscience","The call of conscience has a distinctive formal character; it summons Dasein from absorption in the they.","Avoid reducing the call to an external moral lawgiver.");
    note(60,"Resoluteness","Resoluteness is not a list of correct choices but an authentic way a situation is disclosed.","Can a resolute act still be mistaken about its facts?");
    note(65,"Temporality and care","The ecstatic unity of future, having-been, and present gives the ontological meaning of care.","Do not substitute three objectively successive instants for ecstatic temporality.");
    note(74,"Historicity","Dasein's historical existence involves heritage, possibilities, and temporal repetition.","How is repetition different from mechanically reenacting the past?");
    note(83,"Unfinished question","The published work ends before the projected further divisions. Its analysis of Dasein prepares, but does not complete, the question of Being.","State what the published argument establishes and what it leaves open.");
  }
  private static void add(String key,String meaning){ LEXICON.put(key,meaning); }
  private static void note(int section,String title,String analysis,String question){
    COMMENTARY.put(section,new String[]{title,analysis,question});
  }
  public static String sectionTitle(int section){
    String[] t={
      "The necessity of restating the question of Being","Formal structure of the question",
      "Ontological priority","Ontic priority of Dasein","Existential analytic",
      "Destruction of the history of ontology","The phenomenological method",
      "The plan of the treatise","Theme of the analytic of Dasein",
      "Delimitation from anthropology and psychology","The everyday Dasein",
      "Being-in-the-world as fundamental structure","Knowing as founded mode",
      "The idea of worldhood","The Being of equipment","How equipmental world announces itself",
      "Reference and signs","Involvement and significance"
    };
    if(section>=1 && section<=t.length)return t[section-1];
    switch(section){
      case 25:return "Who is everyday Dasein?";
      case 27:return "Everyday Being-one's-Self and the they";
      case 29:return "Being-there as disposedness";
      case 31:return "Being-there as understanding";
      case 32:return "Understanding and interpretation";
      case 40:return "The fundamental attunement of anxiety";
      case 41:return "Dasein's Being as care";
      case 45:return "Dasein's possible Being-a-whole";
      case 50:return "Existential structure of death";
      case 54:return "Conscience as attestation";
      case 60:return "The existential structure of resoluteness";
      case 65:return "Temporality as the ontological meaning of care";
      case 74:return "The basic constitution of historicity";
      case 83:return "The existential-temporal analytic and the fundamental question";
      default:return section>=1&&section<=83?SECTION_THEMES[section-1]:"Unknown section";
    }
  }
  public static int division(int section){return section<=8?0:section<=44?1:2;}
  private Catalog(){}
}
