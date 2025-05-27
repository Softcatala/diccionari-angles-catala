package org.softcatala.engcat;

import java.util.ArrayList;
import java.util.List;

public class Entry {
  String src = "";
  String type = "";
  String area = "";
  String remark = "";
  String def_eng = "";
  String def_cat = "";
  boolean done = false;
  //0=English, 1=Catalan
  @SuppressWarnings("unchecked")
  List<Word>[] words = (ArrayList<Word>[]) new ArrayList[2];
  
  List<ExampleInSrcDict> examples = new ArrayList<ExampleInSrcDict>();
  
  Entry() {
    words[0] = new ArrayList<Word>();
    words[1] = new ArrayList<Word>();
  }
}
