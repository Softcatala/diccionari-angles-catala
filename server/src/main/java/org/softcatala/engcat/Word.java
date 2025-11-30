package org.softcatala.engcat;

import java.util.*;

public class Word {
  String text = "";
  String grammarClass = "";
  String grammarAux = "";
  String remark = "";
  String tags = "";
  String feminine = "";
  String plural = "";
  String before = "";
  String after = "";
  String area = "";
  boolean primary = false; // és el lema principal
  HashSet<AlternativeForm> alternativeForms = new LinkedHashSet<>();
  private int ocurrences = 0;

  public Word() {
  }

  public Word(Word w, String area) {
    this.text = w.text;
    this.grammarClass = w.grammarClass;
    this.grammarAux = w.grammarAux;
    this.remark = w.remark;
    this.tags = w.tags;
    this.feminine = w.feminine;
    this.plural = w.plural;
    this.before = w.before;
    this.after = w.after;
    this.area = area;
    this.alternativeForms = w.alternativeForms;
    this.ocurrences = 0;
/*
    */
  }

  public Word(Word w) {
    this(w, "");
  }

  public List<String> getAllForms(){
    HashSet<String> forms = new HashSet<>();
    // Forma base
    forms.add(this.text);
    // Formes flexionades
    if (!this.feminine.isEmpty()) {
      forms.add(this.feminine);
    }
    if (!this.plural.isEmpty()) {
      forms.add(this.plural);
    }
    // Verbs en infinitiu sense "to"
    if (this.grammarClass.equals("v")) {
      String trimmedVerb = this.text.replaceAll("^to ","");
      forms.add(trimmedVerb);
    }
    for (AlternativeForm af : this.alternativeForms) {
      forms.add(af.text);
    }
    return new ArrayList<>(forms);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    String grammarTag = grammarClass;
    if (!grammarAux.isEmpty()) {
      grammarTag = grammarTag + " " + grammarAux;
    }
    sb.append(grammarTag + " ");
    if (!area.isEmpty()) {
      sb.append("[" + area + "] ");
    }
    if (!tags.isEmpty()) {
      sb.append("[" + tags + "] ");
    }
    if (!before.isEmpty()) {
      sb.append("(" + before + ") ");
    }
    sb.append(text);
    if (!after.isEmpty()) {
      sb.append(" (" + after + ") ");
    }
    if (!feminine.isEmpty()) {
      sb.append(" [fem. " + feminine + "]");
    }
    if (!plural.isEmpty()) {
      sb.append(" [pl. " + plural + "]");
    }
    if (!remark.isEmpty()) {
      sb.append(" [" + remark + "]");
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (!(o instanceof Word)) {
      return false;
    }
    return this.toString().equals(o.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.toString());
  }

  public int getOcurrences() {
    return ocurrences;
  }

  public void addOcurrence() {
    ocurrences++;
  }

  public boolean isSameLema(Word oWord2) {
    return this.text.equals(oWord2.text)
        && this.alternativeForms.toString().equals(oWord2.alternativeForms.toString())
        && this.grammarClass.equals(oWord2.grammarClass)
        && this.grammarAux.equals(oWord2.grammarAux)
        && this.tags.equals(oWord2.tags)
        && this.feminine.equals(oWord2.feminine)
        && this.plural.equals(oWord2.plural);
        //&& this.remark.equals(oWord2.remark); // Si comprovem el "remark", hi ha separacions de lemes no desitjades (per exemple, "de")
  }

  public boolean isSameSubLema(Word oWord2, Entry entry) {
    return this.text.equals(oWord2.text)
        && this.alternativeForms.toString().equals(oWord2.alternativeForms.toString())
        && this.grammarClass.equals(oWord2.grammarClass)
        && this.grammarAux.equals(oWord2.grammarAux)
        && this.tags.equals(oWord2.tags)
        && this.before.equals(oWord2.before)
        && this.after.equals(oWord2.after)
        && this.feminine.equals(oWord2.feminine)
        && this.plural.equals(oWord2.plural)
        && this.area.equals(oWord2.area)
        && entry.remark.equals(oWord2.remark);
  }
  
  public boolean hasSameAttributesExceptDialect(Word oWord2) {
    String tags1 = removeDialectFromTags(this.tags);
    String tags2 = removeDialectFromTags(oWord2.tags);

    String feminine1 = Utils.removeDiacritics(this.feminine);
    String feminine2 = Utils.removeDiacritics(oWord2.feminine);
    return this.grammarClass.equals(oWord2.grammarClass)
        && this.grammarAux.equals(oWord2.grammarAux)
        && tags1.equals(tags2)
        && this.before.equals(oWord2.before)
        && this.after.equals(oWord2.after)
        && feminine1.equals(feminine2)
        && this.plural.equals(oWord2.plural)
        && this.area.equals(oWord2.area);
        //&& entry.remark.equals(oWord2.remark);
  }

  private String removeDialectFromTags(String s) {
    for(String variant: Utils.ENGLISH_VARIANTS) {
      s = s.replace(variant, "").strip();
    }
    for(String variant: Utils.CATALAN_VARIANTS) {
      s = s.replace(variant, "").strip();
    }
    for(String variant: Utils.tagsForAbbreviations) {
      s = s.replace(variant, "").strip();
    }
    return s;
  }
}

class SortWordByOcurrences implements Comparator<Word> {

  final static List<String> tagsToMoveToTheEnd = Arrays.asList("symbol", "símbol", "old", "rare", "obsolete", "obsolet",
      "dialectal", "impròpiament", "castellanisme", "abbreviation", "abreviació", "sigla", "initialism", "acronym",
      "colloquial", "col·loquial", "antic");

  public int compare(Word a, Word b) {

    // els símbols van al final
    if (Utils.haveAnyCommonForm(a.tags, tagsToMoveToTheEnd)) {
      return 1000;
    }
    if (Utils.haveAnyCommonForm(b.tags, tagsToMoveToTheEnd)) {
      return -1000;
    }
    // Les traduccions que apareixen en més entrades XML tenen prioritat
    int x = b.getOcurrences() - a.getOcurrences();
    if (x != 0) {
      return 100 * x;
    }
    // No reordenem alfabèticament. Mantenim l'ordre que hi ha en l'XML
    //return a.text.compareTo(b.text);
    return 0;

  }
}

