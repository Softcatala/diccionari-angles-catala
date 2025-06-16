package org.softcatala.engcat;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class EntryHandler extends DefaultHandler {

  private List<Entry> entries;
  private Entry currentEntry;
  private Word currentWord;
  private String currentText;
  private EngCatConfiguration conf;
  
  public void setConfiguration(EngCatConfiguration conf) {
    this.conf = conf;
  }

  @Override
  public void startDocument() throws SAXException {
    entries = new ArrayList<>();
  }

  @Override
  public void startElement(String uri, String lName, String qName, Attributes attrs) throws SAXException {
    switch (qName) {
    case "e":
      currentEntry = new Entry();
      if (attrs.getValue("src") != null) {
        currentEntry.src = attrs.getValue("src");
      }
      if (attrs.getValue("area") != null) {
        currentEntry.area = attrs.getValue("area");
      }
      if (attrs.getValue("type") != null) {
        currentEntry.type = attrs.getValue("type");
      }
      if (attrs.getValue("remark") != null) {
        currentEntry.remark = attrs.getValue("remark");
      }
      if (attrs.getValue("def_eng") != null) {
        currentEntry.def_eng = attrs.getValue("def_eng");
      }
      if (attrs.getValue("def_cat") != null) {
        currentEntry.def_cat = attrs.getValue("def_cat");
      }
      if (attrs.getValue("done") != null) {
        currentEntry.done = attrs.getValue("done").equals("yes");
      }
      break;
    case "cat":
    case "eng":
      currentWord = new Word();

      if (attrs.getValue("class") != null) {
        currentWord.grammarClass = attrs.getValue("class");
      }
      if (attrs.getValue("grammar") != null) {
        currentWord.grammarAux = attrs.getValue("grammar");
      }
      if (attrs.getValue("remark") != null) {
        currentWord.remark = attrs.getValue("remark");
      }
      if (attrs.getValue("tags") != null) {
        currentWord.tags = attrs.getValue("tags");
      }
      if (attrs.getValue("feminine") != null) {
        currentWord.feminine = attrs.getValue("feminine");
      }
      if (attrs.getValue("plural") != null) {
        currentWord.plural = attrs.getValue("plural");
      }
      if (attrs.getValue("before") != null) {
        currentWord.before = attrs.getValue("before");
      }
      if (attrs.getValue("after") != null) {
        currentWord.after = attrs.getValue("after");
      }
      break;
    case "example":
      currentEntry.examples.add(new ExampleInSrcDict(attrs.getValue("sent_eng"), attrs.getValue("sent_cat")));
      break;
    }
  }

  @Override
  public void characters(char[] buf, int offset, int len) {
    currentText = new String(buf, offset, len);
  }

  @Override
  public void endElement(String namespaceURI, String sName, String qName) throws SAXException {
    switch (qName) {
    case "e":
      // use only entries done="yes"
      if (currentEntry.done || !conf.production) {
        entries.add(currentEntry);
      }
      break;
    case "eng":
      currentWord.text = currentText;
      // Creem una paraula nova per a generar les formes
      currentEntry.words[0].add(new Word(currentWord));
      break;
    case "cat":
      currentWord.text = currentText;
      // Creem una paraula nova per a generar les formes
      currentEntry.words[1].add(new Word(currentWord));
      break;
    }
  }

  public List<Entry> getEntries() {
    return entries;
  }
}
