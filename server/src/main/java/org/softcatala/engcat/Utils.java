package org.softcatala.engcat;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {

  private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("\\p{Punct}");
  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");
  private static final Pattern SPACES_PATTERN = Pattern.compile("\\s\\s+");

  public static final List<String> ENGLISH_VARIANTS = Arrays.asList("US", "UK", "AU", "IN");
  public static final List<String> CATALAN_VARIANTS = Arrays.asList("valencià", "balear", "rossellonès", "central",
      "nord-occidental", "mallorquí", "menorquí", "eivissenc", "ortografia_2017");
  public static List<String> tagsForAbbreviations = Arrays.asList("sigla", "abreviació", "acronym", "abbreviation", "initialism", "símbol", "symbol");

  static String removeDiacritics(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
  }

  static String replacePunctuation(String input, String replacement) {
    return PUNCTUATION_PATTERN.matcher(input).replaceAll(replacement);
  }

  static String normalizeWhitespaces(String input) {
    return SPACES_PATTERN.matcher(input).replaceAll(" ").trim();
  }

  public static boolean isLowerCaseLetter(String str) {
    if (str != null) { // && str.length() == 1
      char ch = str.charAt(0);
      return Character.isLetter(ch) && Character.isLowerCase(ch);
    }
    return false;
  }

  public static String capitalizeFirstLetter(String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  /**
   * Genera una llista de cadenes de cerca a partir de les formes d'una paraula.

   * @param  w   paraula
   * @return     llista de cadenes de cerca
   */
static List<String> wordFormsToSearch(Word w) {
    HashSet<String> wordFormsResults = new HashSet<>();
    List<String> wordFormsOriginal = new ArrayList<>();
    wordFormsOriginal.addAll(w.getAllForms());
    if (wordFormsOriginal.isEmpty()) {
      wordFormsOriginal.add(w.text);
    }
    // Generem variants de totes les formes
    for (String form : wordFormsOriginal) {
      // Forma sense diacrítics
      String formNoDiacritics = removeDiacritics(form);
      // Forma sense puntuació
      String formNoPunctuation = replacePunctuation(formNoDiacritics, "");
      wordFormsResults.add(formNoPunctuation);
      // Forma amb espais en comptes de puntuació
      String formPunctuationToSpace = normalizeWhitespaces(replacePunctuation(formNoDiacritics, " "));
      wordFormsResults.add(formPunctuationToSpace);
      // Forma sense espais: tinder box -> tinderbox
      String formNoSpaces = formPunctuationToSpace.replaceAll(" ", "");
      wordFormsResults.add(formNoSpaces);
    }
    return new ArrayList<>(wordFormsResults);
  }

  static List<String> wordFormsToSearch(String s) {
    Word w = new Word();
    w.text = s.toLowerCase();
    return wordFormsToSearch(w);
  }

  static boolean haveAnyCommonForm(List<String> list1, List<String> list2) {
    for (String w1: list1) {
      for (String w2: list2) {
        if (w1.equals(w2)) {
          return true;
        }
      }
    }
    return false;
  }

  static boolean haveAnyCommonForm(String s1, List<String> list2) {
    List<String> list1 = Arrays.asList(s1.split((" ")));
    for (String w1: list1) {
      for (String w2: list2) {
        if (w1.equals(w2)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Calculates the string distance between source and target strings using
   * the Damerau-Levenshtein algorithm. The distance is case-sensitive.
   *
   * @param source The source String.
   * @param target The target String.
   * @return The distance between source and target strings.
   * @throws IllegalArgumentException If either source or target is null.
   */
  public static int calculateDistance(CharSequence source, CharSequence target) {
    if (source == null || target == null) {
      throw new IllegalArgumentException("Parameter must not be null");
    }
    int sourceLength = source.length();
    int targetLength = target.length();
    if (sourceLength == 0) return targetLength;
    if (targetLength == 0) return sourceLength;
    int[][] dist = new int[sourceLength + 1][targetLength + 1];
    for (int i = 0; i < sourceLength + 1; i++) {
      dist[i][0] = i;
    }
    for (int j = 0; j < targetLength + 1; j++) {
      dist[0][j] = j;
    }
    for (int i = 1; i < sourceLength + 1; i++) {
      for (int j = 1; j < targetLength + 1; j++) {
        int cost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
        dist[i][j] = Math.min(Math.min(dist[i - 1][j] + 1, dist[i][j - 1] + 1), dist[i - 1][j - 1] + cost);
        if (i > 1 &&
            j > 1 &&
            source.charAt(i - 1) == target.charAt(j - 2) &&
            source.charAt(i - 2) == target.charAt(j - 1)) {
          dist[i][j] = Math.min(dist[i][j], dist[i - 2][j - 2] + cost);
        }
      }
    }
    return dist[sourceLength][targetLength];
  }

  private static List<String> stopWordListForAcronyms = Arrays.asList("sobre", "de", "d", "a", "l", "les", "els", "i", "el", "la",
      "of", "the");

  public static boolean isAcronym(String acronym, String expression) {
    String maj = expression.replaceAll("[^A-Z]", "");
    String base;
    if (!maj.isEmpty()) {
      base = maj;
    } else {
      StringBuilder b = new StringBuilder();
      for (String mot : expression.split("[\\s'-]+")) {
        if (stopWordListForAcronyms.contains(mot.toLowerCase())) {
          continue;
        }
        if (!mot.isEmpty()) {
          char c = mot.charAt(0);
          if (Character.isLetter(c)) b.append(Character.toUpperCase(c));
        }
      }
      base = b.toString();
    }
    return base.equals(acronym.toUpperCase());
  }


  /**
   * Comprova si una paraula és una "stop word".

   * @param  word           paraula
   * @param  stopWordList   llista de "stop words"
   * @return                booleà amb el resultat de la comprovació
   */
  static boolean isStopWord(String word, HashSet<String> stopWordList) {
    if (word.length()<3 || word.endsWith(".")) {
      return true;
    }
    for (String stopWord : stopWordList) {
      if (word.equalsIgnoreCase(stopWord)) {
        return true;
      }
    }
    return false;
  }

  public static List<String> getAllAbbreviations(List<Word> words) {
    List<String> results = new ArrayList<>();
    for (Word w: words) {
      if (haveAnyCommonForm(w.tags, tagsForAbbreviations)) {
        results.add(w.text);
      }
      for (AlternativeForm af : w.alternativeForms) {
        if (haveAnyCommonForm(af.tags, tagsForAbbreviations)) {
          results.add(af.text);
        }
      }
    }
    return results;
  }

}
