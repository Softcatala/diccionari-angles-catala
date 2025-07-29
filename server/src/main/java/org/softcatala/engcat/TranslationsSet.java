package org.softcatala.engcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TranslationsSet {

  List<Word> translatedWords = new ArrayList<Word>();

  String definition = "";

  List<Example> examples = new ArrayList<Example>();

  public TranslationsSet(List<Word> tWords) {
    for (Word tw : tWords) {
      translatedWords.add(new Word(tw, ""));
    }
    this.sortByOccurrences();

  }

  public void addExamples(Entry entry, String lemma, int l) {
    //TODO: comprovar el femení i altres formes flexionades?
    if (lemma.toLowerCase().startsWith("to ")) {
      lemma = lemma.substring(3);
    }
    for (ExampleInSrcDict exampleInSrcDict : entry.examples) {
      String root;
      if (lemma.length() >= 3) {
        root = lemma.substring(0, 3);
      } else {
        root = lemma;
      }
      String firstSent = "";
      String secondSent = "";
      if (l == 1) {
        firstSent = exampleInSrcDict.sentCat;
        secondSent = exampleInSrcDict.sentEng;
      } else {
        firstSent = exampleInSrcDict.sentEng;
        secondSent = exampleInSrcDict.sentCat;
      }
      if ((entry.words[0].size() == 1 && entry.words[1].size() == 1) 
          || firstSent.toLowerCase().contains(root.toLowerCase())) {
        examples.add(new Example(firstSent, secondSent));
      }
    }
  }

  public void addDefinition(Entry entry, int l) {
    // Afegim les definicions en la llengua de les traduccions
    if (definition.equals("")) {
      if (l == 1) {
        definition = entry.def_eng;
      }
      else {
        definition = entry.def_cat;
      }
    }
  }

  public void sortByOccurrences() {
    Collections.sort(translatedWords, new SortWordByOcurrences());
  }

  public boolean intersects(List<Word> translatedWords2) {
    int count = 0;
    for (Word tw1 : translatedWords) {
      for (Word tw2 : translatedWords2) {
        if (tw1.equals(tw2)) {
          count++;
        }
      }
    }
    //if (count > 5) {
      // amb tantes coincidències donem per suposat que són similars
      //return true;
    //}
    if (count == translatedWords.size() || count == translatedWords2.size()) {
      return true;
    }
    return false;
  }

  public boolean sharesDefinition(Entry e, int l) {
    // Comprovem si la definició existent i la definició corresponent de l'entrada coincideixen
    String def = l == 1 ? e.def_eng : e.def_cat;
    if (definition.equals(def)) {
      return true;
    }
    return false;
  }

  public void addTraslatedWords(List<Word> translatedWords2) {
    for (Word tw2 : translatedWords2) {
      boolean found = false;
      for (Word tw1 : translatedWords) {
        if (tw1.equals(tw2)) {
          tw1.addOcurrence();
          found = true;
          break;
        }
      }
      if (!found) {
        translatedWords.add(new Word(tw2, ""));
      }
    }
    this.sortByOccurrences();
  }
  
  public int getOcurrences() {
    int occurrences = 0;
    for (Word tw : this.translatedWords) {
      occurrences += tw.getOcurrences();
    }
    return occurrences;
  }
}

class SortLemmaByOcurrences implements Comparator<TranslationsSet> {
  public int compare(TranslationsSet a, TranslationsSet b) {
    int x = b.getOcurrences() - a.getOcurrences();
    return x;
  }
}