package org.softcatala.engcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SubLemma {
  Word originalWord;
  List<TranslationsSet> translationsSets = new ArrayList<>();
    
  // Nou grup de lemes amb un sol lema inicial
  public SubLemma(TranslationsSet translationsSet, Word oWord, Entry entry) {
    translationsSets.add(translationsSet);
    originalWord = oWord;
    originalWord.remark = entry.remark;
  }
  
  public void addTranslationsSet(TranslationsSet translationsSet) {
    translationsSets.add(translationsSet);
  }
  
  public void sortByOccurrences() {
    Collections.sort(translationsSets, new SortLemmaByOcurrences());
  }
}

class SortSubLemmas implements Comparator<SubLemma> {

  public int compare(SubLemma o1, SubLemma o2) {
    // Alphabetical order, but empty area and remark first
    String a1 = o1.originalWord.area;
    String a2 = o2.originalWord.area;
    String a1b = o1.originalWord.after + o1.originalWord.before;
    String a2b = o2.originalWord.after + o2.originalWord.before;
    String r1 = o1.originalWord.remark;
    String r2 = o2.originalWord.remark;

    int areaDiff = 0;
    if (a1.isEmpty() && !a2.isEmpty()) {
      areaDiff = -100;
    } else if (!a1.isEmpty() && a2.isEmpty()) {
      areaDiff = 100;
    } else {
      areaDiff = a1.compareTo(a2) * 100;
    }

    int remarkDiff = 0;
    if (r1.isEmpty() && !r2.isEmpty()) {
      remarkDiff = -90;
    } else if (!r1.isEmpty() && r2.isEmpty()) {
      remarkDiff = 90;
    }

    int afterbeforeDiff = 0;
    if (a1b.isEmpty() && !a2b.isEmpty()) {
      afterbeforeDiff = -80;
    } else if (!a1b.isEmpty() && a2b.isEmpty()) {
      afterbeforeDiff = 80;
    }

    return areaDiff + remarkDiff + afterbeforeDiff;
  }

}
