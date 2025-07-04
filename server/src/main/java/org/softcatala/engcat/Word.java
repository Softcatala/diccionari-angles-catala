package org.softcatala.engcat;

import java.util.Comparator;
import java.util.HashSet;

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
  HashSet<String> forms = new HashSet<>();
  private int ocurrences = 0;

  public Word() {
  }

  public Word(Word w, String area, String globalRemark) {
    this.text = w.text;
    this.grammarClass = w.grammarClass;
    this.grammarAux = w.grammarAux;
    if (globalRemark == null) {
      this.remark = w.remark;
    } else {
      if (w.remark.isEmpty() && !globalRemark.isEmpty()) {
        this.remark = globalRemark;
      } else {
        this.remark = w.remark;
      }
    }
    this.tags = w.tags;
    this.feminine = w.feminine;
    this.plural = w.plural;
    this.before = w.before;
    this.after = w.after;
    this.area = area;
    this.forms = w.forms;
    this.ocurrences = 0;

    // Forma base
    this.forms.add(w.text);

    // Formes flexionades
    if (!w.feminine.isEmpty()) {
      this.forms.add(w.feminine);
    }
    if (!w.plural.isEmpty()) {
      this.forms.add(w.plural);
    }

    // Verbs en infinitiu sense "to"
    if (this.grammarClass.equals("v")) {
      String trimmedVerb = w.text.replaceAll("^to ","");
      this.forms.add(trimmedVerb);
    }
  }

  public Word(Word w) {
    this(w, "", "");
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
    if (o == null) {
      return false;
    }
    if (this.toString().equals(o.toString())) {
      return true;
    }
    return false;
  }

  public int getOcurrences() {
    return ocurrences;
  }

  public void addOcurrence() {
    ocurrences++;
  }

  public boolean isSameLema(Word oWord2) {
    return this.text.equals(oWord2.text)
        && this.grammarClass.equals(oWord2.grammarClass)
        && this.grammarAux.equals(oWord2.grammarAux)
        //&& this.tags.equals(oWord2.tags) // Si comprovem les etiquetes, les variants dialectals, col·loquials i similars queden en un lema a banda (per exemple, "half")
        && this.feminine.equals(oWord2.feminine)
        && this.plural.equals(oWord2.plural);
        //&& this.remark.equals(oWord2.remark); // Si comprovem el "remark", hi ha separacions de lemes no desitjades (per exemple, "de")
  }

  public boolean isSameSubLema(Word oWord2) {
    return this.equals(oWord2);
  }

}

class SortWordByOcurrences implements Comparator<Word> {
  public int compare(Word a, Word b) {
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

