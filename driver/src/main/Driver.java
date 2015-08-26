package main;

import java.util.*;
import java.util.concurrent.*;

import common.ToolResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

import common.ToolAbstract;
import static common.DriverUtils.*;
import sqoop.ImportTool;
import cascade.CascadeTool;
import sqoop.ExportTool;

public class Driver {
  private final Document doc;
  private final Element etlElem;

  public Driver(Document doc) {
    validate(doc);
    this.doc = doc;
    this.etlElem = doc.getDocumentElement();
  }

  private void validate(Document doc) {
    NodeList importList = doc.getDocumentElement().getElementsByTagName("import");
    Set<String> dbIdCombos = new HashSet<String>();
    for (int i = 0; i < importList.getLength(); i++) {
      Element elem = (Element) importList.item(i);
      String dbIdCombo = elem.getAttribute("db") + ":" + elem.getAttribute("id");
      if (dbIdCombos.contains(dbIdCombo)) {
        throw new RuntimeException("duplicate db:id combination found: " + dbIdCombo);
      }
      dbIdCombos.add(dbIdCombo);
    }
  }

  private void complete(CompletionService<ToolResult> completionService, int size) {
    for (int i = 0; i < size; ++i) {
      try {
        Future<ToolResult> future = completionService.take();
        ToolResult f = future.get();
        if (f != null) {
          int exitValue = f.getExitCode();
          ToolAbstract tool = f.getTool();
          String prefix = "Tool " + tool.getToolName();
          if (exitValue == 0) {
            System.out.println(prefix + " completed successfully.");
          } else {
            System.out.println(prefix + " returned non-zero exit code; see log " + tool.getLogFileName() + " for details.");
          }
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void runImports() {
    NodeList importList = etlElem.getElementsByTagName("import");
    ExecutorService executorService = Executors.newFixedThreadPool(Integer.parseInt(etlElem.getAttribute("numSqoopThreads")));
    CompletionService<ToolResult> completionService = new ExecutorCompletionService<ToolResult>(executorService);
    int nImports = 0;
    for (int i = 0; i < importList.getLength(); i++) {
      Element importElem = (Element) importList.item(i);
      NodeList dbList = importElem.getElementsByTagName("db");
      for (int j = 0; j < dbList.getLength(); j++) {
        Element dbElem = (Element) dbList.item(j);
        ImportTool importTool = new ImportTool(doc, etlElem, importElem, dbElem);
        System.out.println("Starting tool " + importTool.getToolName() + ".");
        completionService.submit(importTool);
        nImports++;
      }
    }
    complete(completionService, nImports);
    executorService.shutdown();
    System.out.println("Imports completed.");
  }

  public String runCascade() {
    NodeList cascadeList = etlElem.getElementsByTagName("cascade");
    ExecutorService executorService = Executors.newFixedThreadPool(1);
    CompletionService<ToolResult> completionService = new ExecutorCompletionService<ToolResult>(executorService);
    // There should be only one cascade element, so get the 0th item in the list.
    Element cascadeElem = (Element) cascadeList.item(0);
    CascadeTool cascadeTool = new CascadeTool(doc, etlElem, cascadeElem);
    System.out.println("Starting tool " + cascadeTool.getToolName() + ".");
    completionService.submit(cascadeTool);
    complete(completionService, 1);
    executorService.shutdown();
    System.out.println("Cascade completed.");
    return cascadeElem.getAttribute("id");
  }

  public void runExports() {
    NodeList exportList = etlElem.getElementsByTagName("export");
    ExecutorService executorService = Executors.newFixedThreadPool(Integer.parseInt(etlElem.getAttribute("numSqoopThreads")));
    CompletionService<ToolResult> completionService = new ExecutorCompletionService<ToolResult>(executorService);
    for (int i = 0; i < exportList.getLength(); i++) {
      Element exportElem = (Element) exportList.item(i);
      ExportTool exportTool = new ExportTool(doc, etlElem, exportElem, ((Element)etlElem.getElementsByTagName("cascade").item(0)).getAttribute("id"));
      System.out.println("Starting tool " + exportTool.getToolName() + ".");
      completionService.submit(exportTool);
    }
    complete(completionService, exportList.getLength());
    executorService.shutdown();
    System.out.println("Exports completed.");
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: driver etl.xml");
      return;
    }
    Document doc = readConfigFile(args[0], new DefaultHandler());
    Driver driver = new Driver(doc);
    driver.runImports();
    driver.runCascade();
    driver.runExports();
  }
}
