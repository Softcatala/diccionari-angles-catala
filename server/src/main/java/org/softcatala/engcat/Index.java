package org.softcatala.engcat;

import java.util.List;

public class Index {

  String startWith;
  int size;
  public List<String> words;

  public Index(List<String> w, String s) {
    words = w; 
    startWith = s;
    size = w.size();
  }

}
