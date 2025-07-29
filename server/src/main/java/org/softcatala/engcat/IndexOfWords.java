package org.softcatala.engcat;

import java.util.List;
import java.text.Collator;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class IndexOfWords {
  
  public Map<String, List<String>> map = new HashMap<>();
  
  private static final Pattern DIACRIT_MARKS = Pattern.compile("[\\p{InCombiningDiacriticalMarks}]");
  private static final int keyLength = 1;
  
  public void addWord(Word originalWord) {
    String word = getCanonical(originalWord.text);
    if (originalWord.grammarClass.equals("v") && word.startsWith("to ")) {
      word = word.substring(3);
    }
    String firstLetter = word.toLowerCase();
    if (firstLetter.length()>=keyLength) {
      firstLetter = removeDiacritics(word.toLowerCase().substring(0, keyLength));
    }
    if (isLowerCaseLetter(firstLetter)) {
      List<String> list = map.computeIfAbsent(firstLetter, k -> new ArrayList<>());
      if (!list.contains(word)) {
        list.add(word);
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
  
  public static String removeDiacritics(String str) {
    String s = Normalizer.normalize(str, Normalizer.Form.NFD);
    return DIACRIT_MARKS.matcher(s).replaceAll("");
  }
  
  public static boolean isLowerCaseLetter(String str) {
    if (str != null) { // && str.length() == 1
        char ch = str.charAt(0);
        return Character.isLetter(ch) && Character.isLowerCase(ch);
    }
    return false;
}
  
}
