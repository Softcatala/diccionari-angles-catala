package org.softcatala.engcat;

import java.util.*;
import java.text.Collator;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.RuleBasedCollator;

public class IndexOfWords {
  
  public Map<String, List<String>> map = new HashMap<>();
  private Set<String> usedforms = new HashSet<>();

  private static final int keyLength = 1;

  public void addWord(Word originalWord) {
    String word = getCanonical(originalWord.text);
    if (originalWord.grammarClass.equals("v") && word.startsWith("to ")) {
      word = word.substring(3);
    }
    String lowerCaseWord = word.toLowerCase();
    String firstLetter = lowerCaseWord;
    if (firstLetter.length() >= keyLength) {
      firstLetter = Utils.removeDiacritics(lowerCaseWord.substring(0, keyLength));
    }
    if (Utils.isLowerCaseLetter(firstLetter)) {
      if (usedforms.add(lowerCaseWord)) {
        List<String> list = map.computeIfAbsent(firstLetter, k -> new ArrayList<>());
        list.add(word);
        List<String> searchableForms = Utils.wordFormsToSearch(originalWord);
        for (String searchableForm : searchableForms) {
          usedforms.add(searchableForm.toLowerCase());
        }
      }
    }
  }
  
  public void sortWords(Locale locale) throws ParseException {
    RuleBasedCollator collator = (RuleBasedCollator) Collator.getInstance(locale);
    final String rules = collator.getRules();
    // Per defecte l'ordenació és alfabètica contínua (no té en compte els espais)
    // Canviem la prioritat dels espais perquè sigui ordenació alfabètica discontínua
    RuleBasedCollator fixedCollator = new RuleBasedCollator(rules.replaceAll("<'\u005f'", "<' '<'\u005f'"));
    for (String key: map.keySet()) {
      map.get(key).sort(fixedCollator);
    }
  }
  
  private String getCanonical(String s) {
    //s=s.toLowerCase();
//    if (s.startsWith("to ")) {
//      s= s.substring(3);
//    }
    return s;
  }

}
