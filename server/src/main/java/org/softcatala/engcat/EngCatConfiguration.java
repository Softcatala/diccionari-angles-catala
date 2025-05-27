package org.softcatala.engcat;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class EngCatConfiguration {
  private static String DEFAULT_PORT = "8000";
  private static String DEFAULT_SERVERTIME = "Europe/Madrid";
  private static String DEFAULT_MAX_THREADS = "10";
  private static String DEFAULT_URLPATH = "/eng-cat-dict-api/";
  
  Integer serverPort;
  File srcFolder = null;
  String srcFolderPath;
  String engStopWordFilePath;
  String catStopWordFilePath;
  String logFilePath;
  String serverTimezone;
  String logging;
  boolean production;
  String urlPath;
  int maxCheckThreads;
  List<String> blockedUserAgents;

  public  String toString() {
    return String.format("serverPort=%d, srcFile=%s, engStopWordFile=%s, catStopWordFile=%s, serverTimezone=%s, logging=%s, production=%s, urlPath=%s",
            serverPort, srcFolderPath, engStopWordFilePath, catStopWordFilePath, serverTimezone, logging, production, urlPath
    );
  }

  public EngCatConfiguration(String[] args) {
    File file = new File (args[1]);
    try {
      Properties props = new Properties();
      try (FileInputStream fis = new FileInputStream(file)) {
        InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
        props.load(isr);
        serverPort = Integer.parseInt(getOptionalProperty(props,"serverPort", DEFAULT_PORT));
        serverTimezone = getOptionalProperty(props,"serverTimezone", DEFAULT_SERVERTIME);
        urlPath = getOptionalProperty(props,"urlPath", DEFAULT_URLPATH);
        logging = getOptionalProperty(props, "logging", "on");
        production = getOptionalProperty(props, "production", "yes").equalsIgnoreCase("yes");
        srcFolderPath = getOptionalProperty(props, "srcFolder", null);
        engStopWordFilePath = getOptionalProperty(props, "engStopWordFile", null);
        catStopWordFilePath = getOptionalProperty(props, "catStopWordFile", null);
        String blockedUserAgentsStr = getOptionalProperty(props, "blockedUserAgents", "");
        if (!blockedUserAgentsStr.isEmpty()) {
          blockedUserAgents = Arrays.asList(blockedUserAgentsStr.split(","));
        } else {
          blockedUserAgents = null;
        }
        logFilePath = getOptionalProperty(props, "logFile", "/logs/eng-cat-server.log");
        maxCheckThreads = Integer.parseInt(getOptionalProperty(props, "maxCheckThreads", DEFAULT_MAX_THREADS));
        if (srcFolderPath != null) {
          srcFolder = new File(srcFolderPath);
          if (!srcFolder.exists() || !srcFolder.isDirectory()) {
            EngCatServer.log("ERROR", "Source folder can not be found: " + srcFolderPath);
            throw new RuntimeException();
          }
        }
        if (engStopWordFilePath != null) {
          File engStopWordFile = new File(engStopWordFilePath);
          if (!engStopWordFile.exists() || !engStopWordFile.isFile()) {
            EngCatServer.log("ERROR", "English stop word file can not be found: " + engStopWordFilePath);
            throw new RuntimeException();
          }
        }
        if (catStopWordFilePath != null) {
          File catStopWordFile = new File(catStopWordFilePath);
          if (!catStopWordFile.exists() || !catStopWordFile.isFile()) {
            EngCatServer.log("ERROR", "Catalan stop word file can not be found: " + catStopWordFilePath);
            throw new RuntimeException();
          }
        }
      } 
    } catch (IOException e) {
      throw new RuntimeException("Could not load properties from '" + file + "'", e);
    } 
  }
  
  protected String getOptionalProperty(Properties props, String propertyName, String defaultValue) {
    String propertyValue = (String)props.get(propertyName);
    if (propertyValue == null) {
      return defaultValue;
    }
    return propertyValue;
  }
}
