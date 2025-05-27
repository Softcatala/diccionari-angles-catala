package org.softcatala.engcat;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dictionary {

  private List<Entry> entries = new ArrayList<>();
  
  public IndexOfWords indexCat = new IndexOfWords();
  public IndexOfWords indexEng = new IndexOfWords();

  private List<HashSet<String>> stopWords = new ArrayList<>();

  public Dictionary(EngCatConfiguration conf) throws SAXException, IOException, ParserConfigurationException {
    long startTime = System.currentTimeMillis();
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    EntryHandler handler = new EntryHandler();
    handler.setConfiguration(conf);
    File[] files = conf.srcFolder.listFiles(File::isFile);
    for (File file : files) {
      if (file.getName().endsWith(".xml")) {
        saxParser.parse(file, handler);
        entries.addAll(handler.getEntries());
      }
    }
    // Carreguem les stop words per a cada llengua
    stopWords.add(new HashSet<String>(Files.readAllLines(Paths.get(conf.engStopWordFilePath), Charset.forName("UTF-8"))));
    EngCatServer.log("INFO", "Loaded " + stopWords.get(0).size() + " stop words for English from file: " + conf.engStopWordFilePath);
    stopWords.add(new HashSet<String>(Files.readAllLines(Paths.get(conf.catStopWordFilePath), Charset.forName("UTF-8"))));
    EngCatServer.log("INFO", "Loaded " + stopWords.get(1).size() + " stop words for Catalan from file: " + conf.catStopWordFilePath);

    long endTime = System.currentTimeMillis();
    createIndex();
    EngCatServer.log("INFO", "Loaded " + entries.size() + " entries from files in: " + conf.srcFolderPath + " in "
        + (endTime - startTime) + " ms");
  }

  /**
  * Genera una resposta a partir d'un terme de cerca.

  * @param  searchWord   terme que es vol cercar
  * @return              resposta amb els resultats de la cerca
  */
  Response getResponse(String searchWord) throws IOException {
    long startTime = System.currentTimeMillis();
    Response response = new Response();
    searchWord = normalizeWhitespaces(searchWord).replace("’", "'");
    response.searchedWord = searchWord;

    // Cerca sense diacrítics i sense puntuació
    String searchWordNoDiacritics = removeDiacritics(searchWord.toLowerCase());
    // dos modes: esborrem la puntuació o la reemplacem per espais
    String searchWordNoPunctuation = replacePunctuation(searchWordNoDiacritics, "");
    String searchWordPunctuationToSpace = normalizeWhitespaces(replacePunctuation(searchWordNoDiacritics, " "));
    String escapedSearchWord;
    if (searchWordPunctuationToSpace.equals(searchWordNoPunctuation)) {
      escapedSearchWord = "(" + Pattern.quote(searchWordNoPunctuation) + ")";
    } else {
      escapedSearchWord = "(" + Pattern.quote(searchWordNoPunctuation) + "|" + Pattern.quote(searchWordPunctuationToSpace) + ")";
    }

    // Si la cerca és una stop word, la limitem a coincidències exactes per a la llengua en qüestió
    List<Pattern> patternToSearch = new ArrayList<>();
    for (int l = 0; l < 2; l++) {
      if (isStopWord(searchWord, stopWords.get(l))) {
        patternToSearch.add(Pattern.compile("^(to )?"+escapedSearchWord+"$", Pattern.CASE_INSENSITIVE));
      } else {
        patternToSearch.add(Pattern.compile("(.*)\\b"+escapedSearchWord+"\\b(.*)", Pattern.CASE_INSENSITIVE));
      }
    }

    //TODO: cercar amb formes flexionades, sufixos, prefixos... però mostrar-ho amb prioritat més baixa
    for (Entry e : entries) {
      // l=0 anglès, l=1 català
      for (int l = 0; l < 2; l++) {
        for (Word w : e.words[l]) {
          List<String> wordForms = wordFormsToSearch(w);
          for (String wordForm : wordForms) {
            Matcher m = patternToSearch.get(l).matcher(wordForm);
            if (m.matches()) {
              int groupCount = m.groupCount();
              if (groupCount>2) {
                // no mostrar "col·laborador" si es busca "col" o "laborador"
                if (m.group(1) != null && m.group(groupCount) != null && (m.group(1).endsWith("·") || m.group(groupCount).startsWith("·"))) {
                  continue;
                }
              }
              addToResponse(response, l, w, e.words[1 - l], e);
              break;
            }
          }

        }
      }
    }
    long endTime = System.currentTimeMillis();
    EngCatServer.log("INFO", "Searched: " + searchWord + " in " + (endTime - startTime) + " ms");
    String canonical = getCanonicalForm(response, searchWord);
    if (!canonical.isEmpty()) {
      response.canonicalLemma=canonical;
    }
    return response;
  }

  private List<String> wordFormsToSearch(Word w) {
    List<String> wordForms = new ArrayList<>();
    // lemma
    String wordNoDiacritics = removeDiacritics(w.text.toLowerCase());
    // dos modes: esborrem la puntuació o la reemplacem per espais
    String wordNoPunctuation = replacePunctuation(wordNoDiacritics, "");
    wordForms.add(wordNoPunctuation);
    String wordPunctuationToSpace = normalizeWhitespaces(replacePunctuation(wordNoDiacritics, " "));
    if (!wordForms.contains(wordPunctuationToSpace)) {
      wordForms.add(wordPunctuationToSpace);
    }
    // femenine
    if (!w.feminine.isEmpty()) {
      wordNoDiacritics = removeDiacritics(w.feminine.toLowerCase());
      // dos modes: esborrem la puntuació o la reemplacem per espais
      wordNoPunctuation = replacePunctuation(wordNoDiacritics, "");
      wordForms.add(wordNoPunctuation);
      wordPunctuationToSpace = normalizeWhitespaces(replacePunctuation(wordNoDiacritics, " "));
      if (!wordForms.contains(wordPunctuationToSpace)) {
        wordForms.add(wordPunctuationToSpace);
      }
    }
    return wordForms;
  }

  private void addToResponse(Response r, int l, Word originalWord, List<Word> translatedWords, Entry entry) {
    Word originalWordWithArea = new Word(originalWord, entry.area, entry.remark);
    boolean translationsMerged = false;
    for (Lemma lemma : r.results[l].lemmas) {
      if (originalWord.isSameLema(lemma.originalWord)) {
        for (SubLemma subLemma : lemma.subLemmaList) {
          if (originalWordWithArea.isSameSubLema(subLemma.originalWord)) {
            for (TranslationsSet translationsSet : subLemma.translationsSets) {
              if (translationsSet.intersects(translatedWords) && translationsSet.sharesDefinition(entry, l)) {
                translationsSet.addTraslatedWords(translatedWords);
                translationsSet.addExamples(entry, lemma.originalWord.text, l);
                translationsMerged = true;
                subLemma.sortByOccurrences();
                break;
              }
            }
            if (!translationsMerged) {
              TranslationsSet translationsSet = new TranslationsSet(translatedWords);
              translationsSet.addDefinition(entry, l);
              translationsSet.addExamples(entry, lemma.originalWord.text, l);
              subLemma.addTranslationsSet(translationsSet);
              translationsMerged = true;
              subLemma.sortByOccurrences();
              break;
            }
          }
        }
        // Crea un nou grup de lemes separat. Abans s'ha de mirar si es podia afegir a un grup existent
        if (!translationsMerged) {
          TranslationsSet translationsSet = new TranslationsSet(translatedWords);
          translationsSet.addDefinition(entry, l);
          translationsSet.addExamples(entry, lemma.originalWord.text, l);
          lemma.add(new SubLemma(translationsSet, originalWordWithArea));
          lemma.sortOriginalWordList();
          translationsMerged = true;
          break;
        }
      }
    }
    if (!translationsMerged) {
      TranslationsSet translationsSet = new TranslationsSet(translatedWords);
      Lemma lemma = new Lemma(originalWord);
      translationsSet.addDefinition(entry, l);
      translationsSet.addExamples(entry, lemma.originalWord.text, l);
      lemma.add(new SubLemma(translationsSet, originalWordWithArea));
      r.results[l].lemmas.add(lemma);
      r.results[l].sortLemmas();
    }
  }
  
  private String getCanonicalForm(Response response, String searchWord) {
    if (searchWord.isEmpty()) {
      return "";
    }
    String canonical = "";
    String lcSearchWord = searchWord.toLowerCase();
    for (Result result : response.results) {
      if (!result.lemmas.isEmpty()) {
        for (Lemma lemma : result.lemmas) {
          if (lemma.originalWord.text.toLowerCase().equals(lcSearchWord)) {
            canonical = lemma.originalWord.text; 
          }
          if (lemma.originalWord.text.toLowerCase().equals("to "+lcSearchWord)) {
            // en verbs l'adreça canònica no tindrà el "to" inicial
            canonical = lcSearchWord; //lemma.originalWord.text; 
          }
        }
        if (!canonical.isEmpty()) {
          break;  
        }
      }
      if (!canonical.isEmpty()) {
        break;  
      }
    }
    return canonical;
  }
  
  /**
  * Crea l'índex alfabètic amb totes les paraules de la llista d'entrades.
  */
  public void createIndex() {
    for (Entry e : entries) {
      if (!e.type.equals("sentence")) {
        for (int l = 0; l < 2; l++) {
          for (Word w : e.words[l]) {
            if (l==0) {
              indexEng.addWord(w);
            } else {
              indexCat.addWord(w);  
            }
          }
        }  
      }
    }
    indexEng.sortWords();
    indexCat.sortWords();
  }

  /**
  * Obtén l'índex per a la llengua i la lletra especificades.

  * @param  s   cadena amb el format "<codi_llengua>-<lletra>"" (p. ex., "eng-a")
  * @return     índex de paraules
  */
  public Index getIndex(String s) {
    String[] parts = s.split("-");
    Index index = null;
    if (parts.length == 2) {
      String lang = parts[0]; // eng / cat
      String letter = parts[1].substring(0,1).toLowerCase();
      
      if (lang.equals("eng")) {
        index = new Index(indexEng.map.get(letter), letter);
      }
      if (lang.equals("cat")) {
        index = new Index(indexCat.map.get(letter), letter);
      }
    }
    return index;
  }

  boolean isStopWord(String word, HashSet<String> stopWordList) {
    if (word.length()<3 || word.endsWith(".")) {
      return true;
    }
    for (String stopWord : stopWordList) {
      if (word.equalsIgnoreCase(stopWord)) {
        return true;
      }
    }
    return false;
  }

  private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("\\p{Punct}");
  private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}");
  private static final Pattern SPACES_PATTERN = Pattern.compile("\\s\\s+");

  String removeDiacritics(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
  }

  String replacePunctuation(String input, String replacement) {
    return PUNCTUATION_PATTERN.matcher(input).replaceAll(replacement);
  }

  String normalizeWhitespaces(String input) {
    return SPACES_PATTERN.matcher(input).replaceAll(" ").trim();
  }

}


    /*for (Word translatedWord : translatedWords) {
      boolean translatedWordFound = false;
      for (Word tw : lemma.translatedWords) {
        if (tw.equals(translatedWord)) {
          tw.addOcurrence();
          translatedWordFound = true;
          lemma.sortByOccurrences();
          break;
        }
      }
      if (!translatedWordFound) {
        lemma.translatedWords.add(translatedWord);
      }
    }*/
