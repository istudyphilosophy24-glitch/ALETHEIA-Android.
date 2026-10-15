package org.aletheia.reader;

/** Eighty-three separately authored section orientations, not Heidegger's copyrighted text. */
public final class StudyGuide {
  public static final String[] ORIENTATIONS = new String[]{
    "Ordinary familiarity with the verb 'to be' does not amount to an explicit account of Being itself.",
    "Every inquiry distinguishes what it seeks, what it interrogates, and the meaning it seeks to uncover.",
    "Ontology cannot simply assume what it means for anything to count as an entity.",
    "Dasein deserves methodological priority because understanding Being already matters to its existence.",
    "The analytic investigates Dasein to approach the horizon of the question of Being.",
    "Inherited ontological concepts are critically retrieved, not rejected merely because they are historical.",
    "Phenomenology allows structures of disclosure to become manifest through hermeneutic interpretation.",
    "The published structure introduces a planned question wider than the surviving divisions establish.",
    "Dasein is characterized by its existing as possibility rather than by an inventory of properties.",
    "An existential analytic is distinct from empirical psychology, anthropology, or biology.",
    "The familiar idea of 'world' must not be equated with a container of objects.",
    "Being-in-the-world is an original unity; its constituent moments are not separate substances.",
    "Theoretical cognition is a founded possibility within an already intelligible world.",
    "Worldhood names an ontological structure of meaningful relations, not a largest object.",
    "Equipment is initially encountered within practical tasks and assignments, not as isolated substances.",
    "Disruption of equipment makes a background network of involvements explicit.",
    "Signs clarify the referential relationships that let things point beyond themselves.",
    "Worldly significance joins involvements to projects for the sake of which Dasein exists.",
    "The Cartesian understanding of extension makes an important but limited conception of world.",
    "A thing's measurable spatial properties do not exhaust the meaning of the world in which it matters.",
    "Heidegger's critique targets the foundational role given to detached representation.",
    "Nearness in practical concern cannot be understood solely as geometric proximity.",
    "Dasein's spatiality is constituted through directionality and de-severance in situated activity.",
    "Space is disclosed within involvement rather than explaining that involvement from the outset.",
    "The 'who' of everyday Dasein is not established by positing a self-contained subject.",
    "Being-with is constitutive of world; others are not first inferred as external minds.",
    "The anonymous 'they' structures ordinary intelligibility without being a specific group.",
    "Being-in includes an existential disclosedness deeper than locating one thing inside another.",
    "Attunement discloses thrownness and what matters before detached judgement.",
    "Fear has a determinate threatening entity and thus differs formally from anxiety.",
    "Understanding is projection of existential possibility rather than merely theoretical knowledge.",
    "Interpretation elaborates possibilities already understood through fore-structures.",
    "Assertion makes a predicative disclosure possible but derives from more primary interpretation.",
    "Discourse articulates intelligibility and belongs with understanding and attunement.",
    "Idle talk circulates interpretations that can be intelligible without original appropriation.",
    "Curiosity seeks constant novelty while sometimes failing to dwell with what is encountered.",
    "Ambiguity can make apparently shared understanding conceal an absence of clarification.",
    "Falling names absorption in everyday publicness, not an original moral fall.",
    "The foregoing existential analyses require a unified ontological formulation.",
    "Anxiety can loosen ordinary involvements and disclose Being-in-the-world as such.",
    "Care is the unity of ahead-of-itself, already-in-a-world, and Being-alongside.",
    "Pre-ontological self-interpretations offer indications of the structure of care.",
    "The question of reality is misconceived when proof of an external world is demanded from an isolated subject.",
    "Truth is grounded in disclosedness while the correctness of propositions retains its place.",
    "The preliminary analytic cannot yet claim to have grasped Dasein's possible wholeness.",
    "The possibility of being-a-whole cannot be modeled as completing a collection of parts.",
    "Another person's death is accessible in distinctive ways but cannot replace my own relation to dying.",
    "Outstanding possibilities and ending must be reinterpreted existentially, not as a missing quantity.",
    "Existential death is distinguished from biological perishing and factual demise.",
    "Being-toward-death concerns Dasein's ownmost, nonrelational, unsurpassable possibility.",
    "Everyday public talk can cover over death even while constantly discussing it.",
    "Everyday dealings often understand death as an event that will eventually happen.",
    "Anticipation discloses finite existence without forecasting the date or manner of death.",
    "An authentic potentiality-for-Being is sought through an attestation within Dasein.",
    "Conscience is analyzed in its structural possibility, rather than as a code of conduct.",
    "The call of conscience interrupts the anonymous interpretations of everyday existence.",
    "The call can be interpreted as emerging from Dasein's own uncanny Being-in-the-world.",
    "Being-guilty involves nullity and responsibility, not merely the commission of a moral offense.",
    "Existential conscience must be distinguished from ordinary accusatory moral psychology.",
    "Resoluteness owns the factical situation without prescribing one determinate moral action.",
    "The move to temporality requires reexamining what made the previous analysis possible.",
    "Anticipatory resoluteness articulates authenticity and finite wholeness together.",
    "Clarifying the hermeneutic situation tests the access gained by anticipatory resoluteness.",
    "Selfhood cannot be understood without the formal structure of care.",
    "Ecstatic temporality is articulated through future, having-been, and making-present.",
    "The prior existential results receive a renewed interpretation from temporality.",
    "The temporal interpretation proceeds by recovering how disclosedness takes place.",
    "Understanding, attunement, falling, and discourse have distinctive temporal meanings.",
    "Worldhood and worldly engagement have an essentially temporal articulation.",
    "Spatiality is reinterpreted through the temporality of concern.",
    "Everydayness is not simply measured by how many days have passed.",
    "Historicality becomes a problem because Dasein exists stretched between birth and death.",
    "Dasein's happening is not a sequence of interchangeable experiences.",
    "Historicality structures how possibilities can be inherited, retrieved, and projected.",
    "World-history concerns entities and events as involved in Dasein's historical world.",
    "Historiography is founded upon a more original possibility of historical disclosedness.",
    "Dilthey and Yorck provide important interlocutors concerning life and historicity.",
    "The temporal analytic encounters questions that remain open about interpretation and Being.",
    "Concern reckons with time in dated, public, and situationally significant ways.",
    "World-time is constituted as datable and significant before homogenized measurement.",
    "The ordinary succession of now-points is interpreted as a derived concept of time.",
    "Comparison with Hegel reveals a deep dispute about time, spirit, and ontological foundations.",
    "The surviving work concludes with the question of how temporality opens the meaning of Being."
  };
  public static String orientation(int section){
    return section>=1&&section<=ORIENTATIONS.length?ORIENTATIONS[section-1]:"Choose a section from the index.";
  }
  public static String criticalExercise(int section){
    String title=Catalog.sectionTitle(section);
    return "Reconstruct the leading distinction in "+title.toLowerCase(java.util.Locale.ROOT)+
      ". Identify the claim, its supporting phenomenon, an alternative interpretation, and an objection. "+
      "Cite a specific passage after importing your edition.";
  }
  public static String division(int n){
    return n<=8?"INTRODUCTION · THE QUESTION OF BEING":n<=44?"DIVISION I · THE ANALYTIC OF DASEIN":"DIVISION II · DASEIN AND TEMPORALITY";
  }
  private StudyGuide(){}
}
