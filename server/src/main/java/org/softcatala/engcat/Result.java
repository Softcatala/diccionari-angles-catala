package org.softcatala.engcat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Result {
  List<Lemma> lemmas = new ArrayList<>();

  public void sortLemmas() {
    Collections.sort(lemmas, new SortLemmas());
  }
}