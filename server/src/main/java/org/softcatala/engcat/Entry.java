package org.softcatala.engcat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.softcatala.engcat.EngCatServer.log;
import static org.softcatala.engcat.Utils.*;

public class Entry {
  String src = "";
  String type = "";
  String area = "";
  String remark = "";
  String def_eng = "";
  String def_cat = "";
  boolean done = false;
  String scientific_name = "";
  // 0=English, 1=Catalan
  @SuppressWarnings("unchecked")
  List<Word>[] words = (ArrayList<Word>[]) new ArrayList[2];

  List<ExampleInSrcDict> examples = new ArrayList<ExampleInSrcDict>();

  Entry() {
    words[0] = new ArrayList<Word>();
    words[1] = new ArrayList<Word>();
  }

  public void addNewWord(int l, Word w) {
    // Comprova si la nova paraula és una variant d'una paraula ja existent.
    // Decideix quina de les dues formes és la principal i quina és l'alternativa.

    if (w.tags.equals("symbol") || w.tags.equals("símbol")) {
      // s'afegeix com a forma alternativa a totes les formes anteriors
      // s'hauria de garantir que és l'últim
      if (words[l].size() == 0) {
        words[l].add(w);
        try {
          log("WARNING", "Símbol solt, sense paraula associada: " + w.text);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      for (int i = 0; i < words[l].size(); i++) {
        words[l].get(i).alternativeForms.add(new AlternativeForm(w.text, w.tags));
      }
      return;
    }

    for (int i = 0; i < words[l].size(); i++) {
      if (w.primary || words[l].get(i).primary
          || (words[l].get(i).hasSameAttributesExceptDialect(w) && isSimilar(words[l].get(i), w))) {
        if (changePriority(words[l].get(i), w)) {
          AlternativeForm af = new AlternativeForm(words[l].get(i).text, words[l].get(i).tags);
          words[l].get(i).alternativeForms.add(af);
          words[l].get(i).text = w.text;
          words[l].get(i).tags = w.tags;
        } else {
          words[l].get(i).alternativeForms.add(new AlternativeForm(w.text, w.tags));
        }
        return;
      }
    }
    // No hi ha coincidència.
    words[l].add(w);
  }


  private static boolean changePriority(Word oldWord, Word newWord) {
    if (newWord.primary) {
      return true;
    }
    if (newWord.tags.contains("US")) {
      return true;
    }
    if (newWord.tags.contains("ortografia_2017")) {
      return false;
    }
    if (tagsForAbbreviations.contains(oldWord.tags)) {
      return true;
    }
    if (tagsForAbbreviations.contains(newWord.tags)) {
      return false;
    }
    return false;
  }

  private static boolean isSimilar(Word oldWord, Word newWord) {
    /*if (oldWord.text.equals(newWord.text)) {
      try {
        log("INFO", oldWord.text + " -> " + newWord.text);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }*/
    if (tagsForAbbreviations.contains(oldWord.tags) && Utils.isAcronym(oldWord.text, newWord.text)) {
      return true;
    }
    if (tagsForAbbreviations.contains(newWord.tags) && Utils.isAcronym(newWord.text, oldWord.text)) {
      return true;
    }
    if (haveAnyCommonForm(oldWord.tags, ENGLISH_VARIANTS) && haveAnyCommonForm(newWord.tags, ENGLISH_VARIANTS)) {
      String oldWordStr = oldWord.text;
      String newWordStr = newWord.text;
      int dist = Utils.calculateDistance(oldWordStr, newWordStr);
      if (dist < 2) {
        // Són variants US/UK, i la distància és petita
        return true;
      }
    }
    List<String> formsOld = Utils.wordFormsToSearch(oldWord);
    List<String> formsNew = Utils.wordFormsToSearch(newWord);
    if (haveAnyCommonForm(formsOld, formsNew)) {
      return true;
    }
    /*String oldWordStr = oldWord.text;
    String newWordStr = newWord.text;
    int dist = Utils.calculateDistance(oldWordStr, newWordStr);
    if (dist < 2) {
      // Són variants US/UK, i la distància és petita
      try {
        log("INFO", oldWordStr + " -> " + newWordStr);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }*/
    return false;
  }

}
