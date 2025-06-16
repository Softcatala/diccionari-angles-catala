package org.softcatala.engcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Lemma {
  Word originalWord;
  List<SubLemma> subLemmaList = new ArrayList<>();

  Lemma (Word oWord) {
    this.originalWord = oWord;
    // Llevem les etiquetes i el remark perquè el lema sigui genèric
    this.originalWord.tags = "";
    this.originalWord.remark = "";
  }

  public void add(SubLemma subLemma) {
    subLemmaList.add(subLemma);
  }

  public void sortOriginalWordList() {
    Collections.sort(subLemmaList, new SortSubLemmas());
  }

}

class SortLemmas implements Comparator<Lemma> {
  public int compare(Lemma a, Lemma b) {
    int lengthDiff = a.originalWord.text.replaceAll("to ","").length()
        - b.originalWord.text.replaceAll("to ","").length();
    if (lengthDiff != 0) {
      return lengthDiff;
    }
    int weightDiff = weightOfGrammarClass(a.originalWord.grammarClass) - weightOfGrammarClass(b.originalWord.grammarClass);
    if (weightDiff != 0) {
      return weightDiff;
    }
    return 0;
  }

  private int weightOfGrammarClass(String grammarClass) {
    switch (grammarClass) {
      case "np":
        return -13;
      case "prep":
        return -12;
      case "pron":
        return -12;
      case "num":
        return -12;
      case "n":
      case "m":
      case "f":
        return -10;
      case "mp":
      case "fp":
      case "n_p":
        return -9;
      case "adj":
        return -8;
      case "v":
        return -7;
      case "adv":
        return -6;
      case "expr":
        return -5;
    }
    return 0;
  }
}

