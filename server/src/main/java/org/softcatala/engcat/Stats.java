package org.softcatala.engcat;

import java.util.List;

public class Stats {
  public int numberEnglishLemmas;
  public int numberCatalanLemmas;
  public int numberCommonLemmas;
  public int numberOnlyEnglishLemmas;
  public int numberOnlyCatalanLemmas;

  public Stats (ListsOfWords lists) {
    numberEnglishLemmas = lists.englishLemmas.size();
    numberCatalanLemmas = lists.catalanLemmas.size();
    numberCommonLemmas = lists.commonLemmas.size();
    numberOnlyEnglishLemmas = lists.onlyEnglishLemmas.size();
    numberOnlyCatalanLemmas = lists.onlyCatalanLemmas.size();

  }
}
