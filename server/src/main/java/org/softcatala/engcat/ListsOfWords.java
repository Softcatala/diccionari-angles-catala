package org.softcatala.engcat;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ListsOfWords {

  public List<String> commonLemmas;
  public List<String> onlyEnglishLemmas;
  public List<String> onlyCatalanLemmas;
  public List<String> englishLemmas;
  public List<String> catalanLemmas;

  public ListsOfWords(List<IndexOfWords> indexWords) {

    Set<String> EnglishSet =
        indexWords.get(0).map.values().stream()
            .flatMap(List::stream)
            .map(String::toLowerCase)
            .collect(Collectors.toCollection(TreeSet::new));

    Set<String> CatalanSet =
        indexWords.get(1).map.values().stream()
            .flatMap(List::stream)
            .map(String::toLowerCase)
            .collect(Collectors.toCollection(TreeSet::new));

    Set<String> CommonSet = new TreeSet<>(EnglishSet);
    CommonSet.retainAll(CatalanSet);

    Set<String> OnlyEnglishSet = new TreeSet<>(EnglishSet);
    OnlyEnglishSet.removeAll(CommonSet);

    Set<String> OnlyCatalanSet = new TreeSet<>(CatalanSet);
    OnlyCatalanSet.removeAll(CommonSet);

    englishLemmas = List.copyOf(EnglishSet);
    catalanLemmas = List.copyOf(CatalanSet);
    commonLemmas = List.copyOf(CommonSet);
    onlyEnglishLemmas = List.copyOf(OnlyEnglishSet);
    onlyCatalanLemmas = List.copyOf(OnlyCatalanSet);
  }
}