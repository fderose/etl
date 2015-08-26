package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

public class DriverUtils {


  public static File createDir(String dirName, boolean deleteDirFirst) {
    File dir = new File(dirName);
    String name;
    boolean b;

    try {
      name = dir.getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (deleteDirFirst && dir.exists()) {
      try {
        FileUtils.deleteDirectory(dir);
      } catch (IOException e) {
        throw new RuntimeException("Cannot delete directory " + name, e);
      }
    }

    b = dir.mkdirs();
    if (!b) {
      throw new RuntimeException("Cannot create directory " + name);
    }

    return dir;
  }

  public static DocumentBuilder newValidatingDocumentBuilder(final File file, EntityResolver entityResolver) {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setValidating(true);
    documentBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", XMLConstants.W3C_XML_SCHEMA_NS_URI);
    DocumentBuilder validatingDocumentBuilder;
    try {
      validatingDocumentBuilder = documentBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    validatingDocumentBuilder.setErrorHandler(new ErrorHandler() {
      @Override
      public void warning(SAXParseException e) throws SAXException {
        //SolrLogger.warn(String.format("Warning encountered while parsing config file %s: %s", file.getName(), e.getMessage()));
      }

      @Override
      public void error(SAXParseException e) throws SAXException {
        throw new SAXException(String.format("Error encountered while parsing config file %s: %s", file.getName(), e.getMessage()));
      }

      @Override
      public void fatalError(SAXParseException e) throws SAXException {
        throw new SAXException(String.format("Fatal error encountered while parsing config file %s: %s", file.getName(), e.getMessage()));
      }
    });
    validatingDocumentBuilder.setEntityResolver(entityResolver);
    return validatingDocumentBuilder;
  }

  public static synchronized Document parseAndValidateXml(File file, EntityResolver entityResolver) throws IOException, SAXException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      return newValidatingDocumentBuilder(file, entityResolver).parse(fis);
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
  }

  public static Document readConfigFile(String fileName, EntityResolver entityResolver) {
    File file = new File(fileName);
    if (!file.exists()) {
      throw new RuntimeException(String.format("Config file %s does not exist.", fileName));
    }
    if (file.isDirectory()) {
      throw new RuntimeException(String.format("Config file %s is a directory.", fileName));
    }
    if (!file.canRead()) {
      throw new RuntimeException(String.format("Config file %s is not readable.", fileName));
    }
    try {
      return parseAndValidateXml(file, entityResolver);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static NodeList getElements(String xPathString, Node doc) throws XPathExpressionException {
    XPathFactory factory = XPathFactory.newInstance();
    XPath xpath = factory.newXPath();
    XPathExpression expr = xpath.compile(xPathString);
    return (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
  }

  public static Element getElement(String xPathString, Node doc) throws XPathExpressionException {
    return (Element)getElements(xPathString, doc).item(0);
  }

  public static String join(List<String> strings, String delim) {
    boolean first = true;
    StringBuilder sb = new StringBuilder();
    for (String s : strings) {
      if (first) {
        first = false;
      } else {
        sb.append(delim);
      }
      sb.append(s);
    }
    return sb.toString();
  }

  public static DBSpec getDb(Document doc, String name) {
    try {
      return new DBSpec(getElement(String.format("//etl/db[@id='%s']", name), doc));
    } catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }
}
