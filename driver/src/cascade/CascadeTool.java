package cascade;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import common.DriverUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import common.ToolAbstract;

public class CascadeTool extends ToolAbstract {;
  protected final File workingDirectory;
  protected final File driverFile;
  protected final String toolName;
  protected final String logFileName;
  protected final List<String> args;

  public CascadeTool(Document doc, Element etlElem, Element cascadeElem) {
    super(etlElem.getAttribute("hadoop_classpath"), "cascade", cascadeElem.getAttribute("id"));
    this.workingDirectory = DriverUtils.createDir(etlElem.getAttribute("workdir") + "/" + tool + "/" + id, true);
    this.toolName = tool + "_" + id;
    this.logFileName = workingDirectory + "/log";
    args = new LinkedList<String>();
    args.add("hadoop");
    args.add(cascadeElem.getAttribute("class"));
    NodeList argList = cascadeElem.getElementsByTagName("arg");
    for (int i = 0; i < argList.getLength(); i++) {
      Element argElem = (Element) argList.item(i);
      args.add("\"../../import/" + argElem.getTextContent() + "/*/data\"");
    }
    this.driverFile = buildDriverFile();
  }

  final protected void buildArgs(List<String> args) {}

  final protected List<String> buildArgs() {
    return args;
  }

  @Override
  final protected File getWorkingDirectory() {
    return workingDirectory;
  }

  @Override
  final public String getToolName() {
    return toolName;
  }

  @Override
  final public String getLogFileName() {
    return logFileName;
  }

  @Override
  final protected File getDriverFile() {
    return driverFile;
  }

  private File buildDriverFile() {
    try {
      File driverFile = new File(workingDirectory + "/driver.sh");
      FileWriter fw = new FileWriter(driverFile);
      fw.write("export HADOOP_CLASSPATH=" + classpath + "\n");
      fw.write(DriverUtils.join(buildArgs(), " ") + "\n");
      fw.write("exit $?\n");
      fw.close();
      driverFile.setExecutable(true);
      return driverFile;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
