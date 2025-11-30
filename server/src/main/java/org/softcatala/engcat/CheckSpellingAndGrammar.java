package org.softcatala.engcat;

import org.languagetool.JLanguageTool;
import org.languagetool.Languages;
import org.languagetool.language.Catalan;
import org.languagetool.rules.RuleMatch;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.Arrays;
import java.util.List;

public class CheckSpellingAndGrammar {

  static Catalan catalan = (Catalan) Languages.getLanguageForShortCode("ca");

  static JLanguageTool lt = new JLanguageTool(catalan);
  static String outputFile = "/logs/possibles_errors.txt";
  static List<String> disabledRules = Arrays.asList("UPPERCASE_SENTENCE_START", "COMMA_LOCUTION", "EXIGEIX_POSSESSIUS_V",
      "EXIGEIX_VERBS_CENTRAL", "VERBS_PRONOMINALS", "FRASE_INFINITIU", "NOMS_OPERACIONS");

  CheckSpellingAndGrammar(String logFileGrammarChecking) {
    outputFile = logFileGrammarChecking;
    lt.disableRules(disabledRules);
  }

  public void check(List<Entry> entries) throws Exception {

    for (Entry e : entries) {
      if (!e.done) {
        continue;
      }
      if (e.src.contains("termcat") || e.area.contains("llengües")) {
        continue;
      }
      for (ExampleInSrcDict example : e.examples) {
         checkText(example.sentCat);
      }
      /*for (Word word : e.words[1]) {
        if (word.tags.contains("scientific_name")) {
          continue;
        }

        checkText(word.text);
      }*/
    }

  }

  private void checkText(String text) throws IOException {
    List<RuleMatch> matches = lt.check(text);
    for (RuleMatch match : matches) {
      writeOutput(match.getSentence().getText() + " ; " + match.getRule().getFullId() + " ; " + match.getSuggestedReplacements().toString());
    }
  }

  private void writeOutput(String sentence) {
    try (BufferedWriter escriptor = new BufferedWriter(new FileWriter(outputFile, true))) {
      escriptor.write(sentence);
      escriptor.newLine();
      System.out.println(sentence);
    } catch (IOException e) {
      System.err.println("S'ha produït un error en escriure el fitxer: " + e.getMessage());
    }
  }


}