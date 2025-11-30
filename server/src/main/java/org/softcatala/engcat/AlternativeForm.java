package org.softcatala.engcat;

public class AlternativeForm {
  String text = "";
  String tags = "";
  //TODO: també pot haver-hi femení, p. ex. ós óssa

  public AlternativeForm(String text, String tags) {
    this.text = text;
    this.tags = tags;
  }
  
  public String toString() {
    return text+" ["+tags+"]";
  }
}
