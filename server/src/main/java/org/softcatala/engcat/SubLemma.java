package org.softcatala.engcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SubLemma {
  Word originalWord;
  List<TranslationsSet> translationsSets = new ArrayList<>();
    
  // Nou grup de lemes amb un sol lema inicial
  public SubLemma(TranslationsSet translationsSet, Word oWord) {
    translationsSets.add(translationsSet);
    originalWord = oWord;
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
    // alphabetical order, but empty area first
    String a1 = o1.originalWord.area;
    String a2 = o2.originalWord.area;
    String a1b = o1.originalWord.after + o1.originalWord.before;
    String a2b = o2.originalWord.after + o2.originalWord.before;
    if (a1.isEmpty() && a2.isEmpty()) {
      if (a1b.isEmpty() && a2b.isEmpty()) {
        return 0;
      } else if (a1b.isEmpty()) {
        return -100;
      } else if (a2b.isEmpty()) {
        return 100;
      }
    } else if (a1.isEmpty()) {
      return -100;
    } else if (a2.isEmpty()) {
      return 100;
    }
    return a1.compareTo(a2);
  }

}
