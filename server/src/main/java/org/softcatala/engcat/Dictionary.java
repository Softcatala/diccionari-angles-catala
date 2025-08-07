package org.softcatala.engcat;

import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dictionary {

  // Llista amb totes les entrades carregades dels fitxers XML
  private List<Entry> entries = new ArrayList<>();
  // Índex de paraules i totes les entrades on apareixen
  private List<HashMap<String, List<Entry>>> indexWordEntries = Arrays.asList(new HashMap<String, List<Entry>>(), new HashMap<String, List<Entry>>());
  // Índex de cadenes de cerca i llistes de lemes coincidents
  private List<HashMap<String, List<Lemma>>> indexWordLemmas = Arrays.asList(new HashMap<String, List<Lemma>>(), new HashMap<String, List<Lemma>>());
  
  // Índex alfabètic de paraules
  public List<IndexOfWords> indexWords = Arrays.asList(new IndexOfWords(), new IndexOfWords());

  // Llista de "stop words" per a cada llengua
  private List<HashSet<String>> stopWords = new ArrayList<>();

  public Dictionary(EngCatConfiguration conf) throws SAXException, IOException, ParserConfigurationException, ParseException {
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
    
    // Generem els índexs de cerca
    startTime = System.currentTimeMillis();
    generateIndexWordEntries();
    generateIndexWordLemmas();
    endTime = System.currentTimeMillis();
    EngCatServer.log("INFO", "Created search indices in " + (endTime - startTime) + " ms");
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
        patternToSearch.add(Pattern.compile("^"+escapedSearchWord+"$", Pattern.CASE_INSENSITIVE));
      } else {
        patternToSearch.add(Pattern.compile("(.*)\\b"+escapedSearchWord+"\\b(.*)", Pattern.CASE_INSENSITIVE));
      }
    }

    // Cerca el patró a l'índex de lemes
    for (int l = 0; l < 2; l++) {
      for (String wordForm : indexWordLemmas.get(l).keySet()) {
        Matcher m = patternToSearch.get(l).matcher(wordForm);
        if (m.matches()) {
          int groupCount = m.groupCount();
          if (groupCount>2) {
            // Hack per al punt volat, per a no mostrar "col·laborador" si es busca "col" o "laborador"
            if (m.group(1) != null && m.group(groupCount) != null && (m.group(1).endsWith("·") || m.group(groupCount).startsWith("·"))) {
              continue;
            }
          }
          addToResponse(response, l, indexWordLemmas.get(l).get(wordForm));
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

  /**
  * Genera una llista de cadenes de cerca a partir de les formes d'una paraula.

  * @param  w   paraula
  * @return     llista de cadenes de cerca
  */
  private List<String> wordFormsToSearch(Word w) {
    List<String> wordForms = new ArrayList<>();
    // Generem variants de totes les formes
    for (String form : w.forms) {
      // Forma sense diacrítics
      String formNoDiacritics = removeDiacritics(form);
      // Forma sense puntuació
      String formNoPunctuation = replacePunctuation(formNoDiacritics, "");
      wordForms.add(formNoPunctuation);
      // Forma amb espais en comptes de puntuació
      String formPunctuationToSpace = normalizeWhitespaces(replacePunctuation(formNoDiacritics, " "));
      if (!wordForms.contains(formPunctuationToSpace)) {
        wordForms.add(formPunctuationToSpace);
      }
    }
    return wordForms;
  }

  /**
  * Afegeix els lemes d'una llista a una resposta de cerca per a una llengua.

  * @param  r        resposta de cerca
  * @param  l        índex de la llengua dels lemes (0 anglès, 1 català)
  * @param  lemmas   llista de lemes
  */
  private void addToResponse(Response r, int l, List<Lemma> lemmas) {
    for (Lemma lemma : lemmas) {
      Boolean lemmaAlreadyExists = false;
      for (Lemma resultLemma : r.results[l].lemmas) {
        if (lemma.originalWord.isSameLema(resultLemma.originalWord)) {
          lemmaAlreadyExists = true;
          break;
        }
      }
      if (!lemmaAlreadyExists) {
        r.results[l].lemmas.add(lemma);
      }
    }
    r.results[l].sortLemmas();
  }

  /**
  * Afegeix una paraula d'una entrada a una llista de lemes.

  * @param  lemmaList      resposta de cerca
  * @param  l              índex de la llengua de la paraula (0 anglès, 1 català)
  * @param  originalWord   paraula
  * @param  entry          entrada a la qual pertany la paraula
  */
  private void addToLemmaList(List<Lemma> lemmaList, int l, Word originalWord, Entry entry) {
    List<Word> translatedWords = entry.words[1 - l];
    if (lemmaList.size() > 0)
    {
      for (Lemma lemma : lemmaList) {
        if (originalWord.isSameLema(lemma.originalWord)) {
          // Ja hi ha un lema coincident, cal comprovar els sublemes
          for (SubLemma subLemma : lemma.subLemmaList) {
            if (originalWord.isSameSubLema(subLemma.originalWord, entry)) {
              for (TranslationsSet translationsSet : subLemma.translationsSets) {
                if (translationsSet.intersects(translatedWords) && translationsSet.sharesDefinition(entry, l)) {
                  // Afegeix traduccions addicionals a un conjunt de traduccions existent
                  translationsSet.addTraslatedWords(translatedWords);
                  translationsSet.addExamples(entry, lemma.originalWord.text, l);
                  subLemma.sortByOccurrences();
                  return;
                }
              }
              // Afegeix un conjunt de traduccions nou
              TranslationsSet translationsSet = new TranslationsSet(translatedWords);
              translationsSet.addDefinition(entry, l);
              translationsSet.addExamples(entry, lemma.originalWord.text, l);
              subLemma.addTranslationsSet(translationsSet);
              subLemma.sortByOccurrences();
              return;
            }
          }
          // Afegeix un sublema nou
          TranslationsSet translationsSet = new TranslationsSet(translatedWords);
          translationsSet.addDefinition(entry, l);
          translationsSet.addExamples(entry, lemma.originalWord.text, l);
          lemma.add(new SubLemma(translationsSet, originalWord, entry));
          lemma.sortOriginalWordList();
          return;
        }
      }
    }
    // Afegeix un lema nou
    TranslationsSet translationsSet = new TranslationsSet(translatedWords);
    Lemma lemma = new Lemma(new Word(originalWord)); // Fem una còpia per a no afectar la paraula original
    translationsSet.addDefinition(entry, l);
    translationsSet.addExamples(entry, lemma.originalWord.text, l);
    lemma.add(new SubLemma(translationsSet, originalWord, entry));
    lemmaList.add(lemma);
    Collections.sort(lemmaList, new SortLemmas());
  }

  /**
  * Genera l'índex d'entrades.
  */
  private void generateIndexWordEntries() {
    for (Entry entry : entries) {
      for (int l = 0; l < 2; l++) {
        for (Word word : entry.words[l]) {
          if (!indexWordEntries.get(l).containsKey(word.text)) {
            indexWordEntries.get(l).put(word.text, new ArrayList<>(Arrays.asList(entry)));
          }
          else {
            indexWordEntries.get(l).get(word.text).add(entry);
          }
        }
      }
    }
  }

  /**
  * Genera l'índex de lemes.
  */
  private void generateIndexWordLemmas() {
    for (int l = 0; l < 2; l++) {
      for (String w : indexWordEntries.get(l).keySet()) {
        for (Entry iEntry : indexWordEntries.get(l).get(w)) {
          Word baseWord = new Word();
          for (Word word : iEntry.words[l]) {
            if (word.text.equals(w)) {
              baseWord = new Word(word, iEntry.area);
            }
          }
          // Afegim totes les formes
          for (String wordForm : wordFormsToSearch(baseWord)) {
            indexWordLemmas.get(l).putIfAbsent(wordForm, new ArrayList<>());
            addToLemmaList(indexWordLemmas.get(l).get(wordForm), l, baseWord, iEntry);
          }
        }
      }
    }
  }

  /**
  * Exporta totes les paraules i lemes del diccionari en format JSON.

  * @param  outputFolder   carpeta d'exportació
  */
  void exportJSON(File outputFolder) throws IOException {
    EngCatServer.log("INFO", "Exporting dictionary words and lemmas in JSON format...");
    for (int l = 0; l < 2; l++) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      SortedMap<String, List<Lemma>> wordsLemmas = new TreeMap<String, List<Lemma>>();
      for (List<String> letter : indexWords.get(l).map.values()) {
        for (String word : letter) {
          wordsLemmas.put(word, indexWordLemmas.get(l).get(word));
        }
      }
      String outputFile = "";
      if (l==0) {
        outputFile = "eng-cat.json";
      } else {
        outputFile = "cat-eng.json";
      }

      File f = new File(outputFolder, outputFile);
      FileWriter fw = new FileWriter(f);
      fw.write(gson.toJson(wordsLemmas));
      fw.close();
    }
    EngCatServer.log("INFO", "Finished exporting dictionary.");
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
  public void createIndex() throws ParseException {
    for (Entry e : entries) {
      if (!e.type.equals("sentence")) {
        for (int l = 0; l < 2; l++) {
          for (Word w : e.words[l]) {
            indexWords.get(l).addWord(w);
          }
        }  
      }
    }
    indexWords.get(0).sortWords(new Locale("eng"));
    indexWords.get(1).sortWords(new Locale("cat"));
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
        index = new Index(indexWords.get(0).map.get(letter), letter);
      }
      if (lang.equals("cat")) {
        index = new Index(indexWords.get(1).map.get(letter), letter);
      }
    }
    return index;
  }

  /**
  * Comprova si una paraula és una "stop word".

  * @param  word           paraula
  * @param  stopWordList   llista de "stop words"
  * @return                booleà amb el resultat de la comprovació
  */
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

  public List<Entry> getEntries() {
    return entries;
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
