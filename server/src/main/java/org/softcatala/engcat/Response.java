package org.softcatala.engcat;

import java.util.List;

public class Response {
  String searchedWord;
  String canonicalLemma;
  List<String> alternatives;

  Result[] results = new Result[2];

  Response() {
    results[0] = new Result();
    results[1] = new Result();
  }
  
  public boolean isEmpty() {
    return results[0].lemmas.size()==0 && results[1].lemmas.size()==0;
  }

}
