package org.softcatala.engcat;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

import org.languagetool.tools.LtThreadPoolFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

public class EngCatServer {

  static EngCatConfiguration conf = null;
  static Dictionary dict = null;
  static File exportDir = null;
  private static ThreadPoolExecutor executorService;

  public static void main(String[] args) throws Exception {
    ArgumentParser parser = ArgumentParsers.newFor("prog").build().defaultHelp(true).description("Run a server from the bilingual dictionary data provided.");
    parser.addArgument("--config").metavar("configFile").required(true).type(Arguments.fileType().verifyIsFile()).help("configuration file");
    parser.addArgument("--export").metavar("exportFolder").type(Arguments.fileType().verifyIsDirectory()).help("folder to export the dictionary data");
    try {
        conf = new EngCatConfiguration(parser.parseArgs(args).get("config"));
        exportDir = parser.parseArgs(args).get("export");
        dict = new Dictionary(conf);
    }
    catch (ArgumentParserException e) {
        parser.handleError(e);
        System.exit(1);
    }

    if (exportDir != null) {
      // Si s'expecifica una carpeta d'exportació, només s'exporten les dades en format JSON
      // i s'acaba el programa
      dict.exportJSON(exportDir);
      System.exit(0);
    }
    
    if (conf.logFileGrammarChecking != null) {
      // Revisió gramatical i final del programa
      log("INFO", "Comença la revisió ortogràfica i gramatical");
      CheckSpellingAndGrammar checker = new CheckSpellingAndGrammar(conf.logFileGrammarChecking);
      checker.check(dict.getEntries());
      log("INFO", "Revisió ortogràfica i gramatical acabada");
      System.exit(0);
    }

    // Eliminem totes les estructures intermèdies innecessàries
    dict.cleanMemory();

    executorService = getExecutorService(conf);
    HttpServer server = HttpServer.create(new InetSocketAddress(conf.serverPort), 0);
    log("INFO", "Server enabled on port: " + conf.serverPort + "; path: " + conf.urlPath);
    server.setExecutor(executorService);
    server.createContext(conf.urlPath, new MyHandler());
    server.setExecutor(null);
    server.start();
    
  }

  static class MyHandler implements HttpHandler {

    public void handle(HttpExchange t) throws IOException {
      GsonBuilder builder = new GsonBuilder();
      builder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.PRIVATE);
      Gson gson = builder.create();

      String userAgent = t.getRequestHeaders().get("user-agent").toString();
      
      if (conf.blockedUserAgents != null) {
        for (String blockedUserAgent : conf.blockedUserAgents) {
          if (!blockedUserAgent.isEmpty() && userAgent.toLowerCase().contains(blockedUserAgent.toLowerCase())) {
            t.sendResponseHeaders(500, -1);
            return;
          }
        }  
      }

      String url = t.getRequestURI().toString().substring(conf.urlPath.length());
      String apiCall = "";
      try {
        apiCall = java.net.URLDecoder.decode(url, StandardCharsets.UTF_8.name());
      } catch (UnsupportedEncodingException e) {
        // not going to happen - value came from JDK's own StandardCharsets
      }
      apiCall = apiCall.trim();
      log("INFO", "API call: " + apiCall + " UserAgent: " + userAgent);
      String[] parts = apiCall.split("/");
      int code = 200;
      if (parts.length == 2) {
        String jsonResponse = null;
        if (parts[0].equalsIgnoreCase("search")) {
          Response response = dict.getResponse(parts[1]);
          jsonResponse = gson.toJson(response);
          if (response.results == null || response.isEmpty()) {
            code = 404;
          }
        } else if (parts[0].equalsIgnoreCase("index")) {
          Index index = dict.getIndex(parts[1]);
          jsonResponse = gson.toJson(index);
          if (index.words == null || index.words.size() == 0) {
            code = 404;
          }
        }
           /* else if
           * (parts[0].equalsIgnoreCase("autocomplete")) { Index index =
           * dict.getAutocomplete(parts[1]); jsonResponse = gson.toJson(index); if
           * (index.words == null || index.words.size() == 0) { code = 404; } }
           */
        if (jsonResponse != null) {
          t.getResponseHeaders().add("Content-type", "application/json");
          t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
          t.sendResponseHeaders(code, jsonResponse.getBytes().length);
          OutputStream os = t.getResponseBody();
          os.write(jsonResponse.getBytes());
          os.close();
          return;
        }
      }
      // Error 500
      t.sendResponseHeaders(500, -1);
    }
  }

  static void log(String commentType, String comment) throws IOException {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    Date date = new Date(System.currentTimeMillis());
    StringBuilder msg = new StringBuilder();
    msg.append(formatter.format(date));
    msg.append(" " + commentType + " ");
    msg.append(comment);
    System.out.println(msg);
    if (conf != null && conf.logging.equals("on") && !comment.isEmpty()) {
      try (BufferedWriter out = new BufferedWriter(new FileWriter(conf.logFilePath, true))) {
        out.write(msg + "\n");
      }
    }
  }
  
  private static ThreadPoolExecutor getExecutorService(EngCatConfiguration conf) throws IOException {
    int threadPoolSize = conf.maxCheckThreads;
    log("INFO", "Setting up thread pool with " + threadPoolSize + " threads");
    return LtThreadPoolFactory.createFixedThreadPoolExecutor(LtThreadPoolFactory.SERVER_POOL,
      threadPoolSize, threadPoolSize, 0,0L, false,
      (thread, throwable) -> {
        try {
          log("INFO", "Thread: " + thread.getName() + " failed with: " + throwable.getMessage());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }, false);
  }

  private static void printCounts() {
    for (String langcode : Arrays.asList("eng", "cat")) {
      int total = 0;
      for (char lletra = 'a'; lletra <= 'z'; lletra++) {
        int mida = dict.getIndex(String.valueOf(langcode+"-"+lletra)).size;
        System.out.println(langcode+"\t"+lletra+"\t"+ String.valueOf(mida));
        total += mida;
      }
      System.out.println(langcode+"\ttotal\t"+ String.valueOf(total));
    }
  }

}