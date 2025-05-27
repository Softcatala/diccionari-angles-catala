package org.softcatala.engcat;

import java.util.Collections;
import java.util.List;

public class Index {

  String startWith;
  int size;
  public List<String> words;

  public Index(List<String> w, String s) {
    Collections.sort(w, String.CASE_INSENSITIVE_ORDER);
    words = w; 
    startWith = s;
    size = w.size();
  }

}
