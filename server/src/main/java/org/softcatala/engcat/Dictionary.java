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

import static org.softcatala.engcat.Utils.*;

public class Dictionary {

  // Llista amb totes les entrades carregades dels fitxers XML
  private List<Entry> xmlEntries = new ArrayList<>();
  // Índex de paraules i totes les entrades on apareixen
  private List<HashMap<String, List<Entry>>> indexWordToEntries = Arrays.asList(new HashMap<String, List<Entry>>(), new HashMap<String, List<Entry>>());
  // Índex de cadenes de cerca i llistes de lemes coincidents
  private List<HashMap<String, List<Lemma>>> indexWordToLemmas = Arrays.asList(new HashMap<String, List<Lemma>>(), new HashMap<String, List<Lemma>>());
  
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
        xmlEntries.addAll(handler.getEntries());
      }
    }
    long endTime = System.currentTimeMillis();
    EngCatServer.log("INFO", "Loaded " + xmlEntries.size() + " entries from files in: " + conf.srcFolderPath + " in "
        + (endTime - startTime) + " ms");
    // Carreguem les stop words per a cada llengua
    stopWords.add(new HashSet<String>(Files.readAllLines(Paths.get(conf.engStopWordFilePath), Charset.forName("UTF-8"))));
    EngCatServer.log("INFO", "Loaded " + stopWords.get(0).size() + " stop words for English from file: " + conf.engStopWordFilePath);
    stopWords.add(new HashSet<String>(Files.readAllLines(Paths.get(conf.catStopWordFilePath), Charset.forName("UTF-8"))));
    EngCatServer.log("INFO", "Loaded " + stopWords.get(1).size() + " stop words for Catalan from file: " + conf.catStopWordFilePath);

    EngCatServer.log("INFO", "Creating indexes.");

    // Generem els índexs de cerca
    startTime = System.currentTimeMillis();
    createIndex();
    endTime = System.currentTimeMillis();
    EngCatServer.log("INFO", "Created general index in " + (endTime - startTime) + " ms");

    startTime = System.currentTimeMillis();
    generateWordToEntriesIndex();
    endTime = System.currentTimeMillis();
    EngCatServer.log("INFO", "Created word-to-entries index in " + (endTime - startTime) + " ms");

    startTime = System.currentTimeMillis();
    generateWordToLemmasIndex();
    endTime = System.currentTimeMillis();
    EngCatServer.log("INFO", "Created word-to-lemmas index in " + (endTime - startTime) + " ms");

  }

  /**
  * Genera una resposta a partir d'un terme de cerca.

  * @param  searchWord   terme que es vol cercar
  * @return              resposta amb els resultats de la cerca
  */
  Response getResponse(String searchWord) throws IOException {
    long startTime = System.currentTimeMillis();
    Response response = new Response();
    searchWord = Utils.normalizeWhitespaces(searchWord).replace("’", "'");
    response.searchedWord = searchWord;

    // Cerca sense diacrítics, sense puntuació, sense espais
    StringBuilder searchStrBuilder = new StringBuilder();
    searchStrBuilder.append("(");
    for (String s : wordFormsToSearch(searchWord.toLowerCase())) {
        if (searchStrBuilder.length()>1) {
          searchStrBuilder.append("|");
        }
      searchStrBuilder.append(Pattern.quote(s));
    }
    searchStrBuilder.append(")");
    String escapedSearchWord = searchStrBuilder.toString();
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

      for (String wordForm : indexWordToLemmas.get(l).keySet()) {
        Matcher m = patternToSearch.get(l).matcher(wordForm);
        if (m.matches()) {
          int groupCount = m.groupCount();
          if (groupCount>2) {
            // Hack per al punt volat, per a no mostrar "col·laborador" si es busca "col" o "laborador"
            if (m.group(1) != null && m.group(groupCount) != null && (m.group(1).endsWith("·") || m.group(groupCount).startsWith("·"))) {
              continue;
            }
          }
          addToResponse(response, l, indexWordToLemmas.get(l).get(wordForm));
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
  private void generateWordToEntriesIndex() {
    for (Entry entry : xmlEntries) {
      for (int l = 0; l < 2; l++) {
        for (Word word : entry.words[l]) {
          if (!indexWordToEntries.get(l).containsKey(word.text)) {
            indexWordToEntries.get(l).put(word.text, new ArrayList<>(Arrays.asList(entry)));
          }
          else {
            indexWordToEntries.get(l).get(word.text).add(entry);
          }
        }
      }
    }
  }

  /**
  * Genera l'índex de lemes.
  */
  private void generateWordToLemmasIndex() {
    for (int l = 0; l < 2; l++) {
      for (String w : indexWordToEntries.get(l).keySet()) {
        for (Entry iEntry : indexWordToEntries.get(l).get(w)) {
          Word baseWord = new Word();
          for (Word word : iEntry.words[l]) {
            if (word.text.equals(w)) {
              baseWord = new Word(word, iEntry.area);
            }
          }
          List<String> possibleAbbreviations = getAllAbbreviations(iEntry.words[l]);
          indexWordToLemmas.get(l).putIfAbsent(baseWord.text, new ArrayList<>());
          addToLemmaList(indexWordToLemmas.get(l).get(baseWord.text), l, baseWord, iEntry);
          // Afegim totes les formes, amb les mateixes llistes de lemes (sense crear objectes nous)
          for (String wordForm : wordFormsToSearch(baseWord)) {
            if (wordForm.equalsIgnoreCase(baseWord.text)) {
              continue;
            }
            if (indexWordToLemmas.get(l).containsKey(wordForm) ) {
              addToLemmaList(indexWordToLemmas.get(l).get(wordForm), l, baseWord, iEntry);
            } else {
              if (possibleAbbreviations.contains(wordForm)) {
                // Si és una abreviatura sí que creem un objecte diferent per si de cas.
                // Hi ha paraules sense relació que comparteixen abreviació
                // TODO: pot haver-hi problemes amb altres formes: plurals, femenins, etc.
                indexWordToLemmas.get(l).putIfAbsent(wordForm, new ArrayList<>());
                addToLemmaList(indexWordToLemmas.get(l).get(wordForm), l, baseWord, iEntry);
              }
              else {
                // En la resta de casos, aprofitem l'objecte que ja tenim, i no en construïm un de nou.
                indexWordToLemmas.get(l).put(wordForm, indexWordToLemmas.get(l).get(baseWord.text));
              }
            }
          }
          // cerca per nom científic; creem una llista de lemes diferents, per a evitar barreges de lemes
          if (!iEntry.scientific_name.isEmpty()) {
            indexWordToLemmas.get(l).putIfAbsent(iEntry.scientific_name.toLowerCase(), new ArrayList<>());
            addToLemmaList(indexWordToLemmas.get(l).get(iEntry.scientific_name.toLowerCase()), l, baseWord, iEntry);
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
          wordsLemmas.put(word, indexWordToLemmas.get(l).get(word));
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
    for (Entry e : xmlEntries) {
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


  public List<Entry> getEntries() {
    return xmlEntries;
  }

  public void cleanMemory() {
    xmlEntries = null;
    indexWordToEntries = null;
    // Imprescindibles per al servidor en producció: indexWordToLemmas, stopWords i indexWords

  }
}
