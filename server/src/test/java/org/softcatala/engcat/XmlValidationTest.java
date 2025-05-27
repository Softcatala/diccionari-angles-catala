package org.softcatala.engcat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XmlValidationTest {

    private static final List<String> xmlFiles = new ArrayList<>();

    @BeforeAll
    public static void setup() throws IOException {
        // Define the directory where the XML files are located
        String directoryPath = "../diccionari";
        
        // Collect all XML files in the directory
        Files.walk(Paths.get(directoryPath))
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".xml"))
            .forEach(path -> xmlFiles.add(path.toString()));
    }

    @Test
    public void testAllXmlFiles() {
        List<String> invalidFiles = new ArrayList<>();

        for (String filePath : xmlFiles) {
            try {
                validateXmlFile(filePath);
            } catch (Exception e) {
                invalidFiles.add(filePath);
            }
        }

        // Assert that there are no invalid files
        assertTrue(invalidFiles.isEmpty(), "The following XML files are invalid: " + invalidFiles);
    }

    private void validateXmlFile(String filePath) throws SAXException, IOException, ParserConfigurationException {
        File xmlFile = new File(filePath);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        DefaultHandler handler = new DefaultHandler();
        try (FileInputStream fis = new FileInputStream(xmlFile)) {
            saxParser.parse(new InputSource(fis), handler);
        } catch (Exception e) {
            throw new SAXException("Error parsing file: " + filePath, e);
        }
    }
}
