package org.softcatala.engcat;

public class Language {

  public enum EnumLang {
    ENG, CAT
  };

  EnumLang enumlang = null;
  
  Language (EnumLang l) {
    enumlang = l;
  }
  
  public EnumLang translated() {
    if (enumlang == EnumLang.CAT) {
      return EnumLang.ENG;
    } else {
      return EnumLang.CAT;
    }
  }
  
  public String tag() {
    if (enumlang == EnumLang.CAT) {
      return "cat";
    } else {
      return "eng";
    }
  }
  
  public String translatedTag() {
    if (enumlang == EnumLang.CAT) {
      return "eng";
    } else {
      return "cat";
    }
  }

}
