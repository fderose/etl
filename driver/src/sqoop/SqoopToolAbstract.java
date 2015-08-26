package sqoop;

import java.io.*;
import java.util.List;
import common.DBSpec;
import common.DriverUtils;
import common.ToolAbstract;

public abstract class SqoopToolAbstract extends ToolAbstract {
  protected final DBSpec dbSpec;
  protected final String fieldDelimiter;

  protected final File workingDirectory;
  protected final String toolName;
  protected final String logFileName;

  public SqoopToolAbstract(String classpath, String workDir, String tool, String id, DBSpec dbSpec, String fieldDelimiter) {
    super(classpath, tool, id);
    this.workingDirectory = DriverUtils.createDir(workDir + "/" + tool + "/" + id + "/" + dbSpec.getId(), true);
    this.dbSpec = dbSpec;
    this.fieldDelimiter = fieldDelimiter;

    this.toolName = tool + "_" + id + "_" + dbSpec.getId();
    this.logFileName = workingDirectory + "/log";
  }

  final protected void buildArgs(List<String> args) {
    args.add("sqoop");
    args.add(tool);
    args.add("--connect");
    args.add(dbSpec.getUrl());
    args.add("--fields-terminated-by");
    args.add("\"" + fieldDelimiter + "\"");
  }

  protected abstract List<String> buildArgs();

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
    try {
      File driverFile = new File(workingDirectory + "/driver.sh");
      FileWriter fw = new FileWriter(driverFile);
      fw.write("export SQOOP_USER_CLASSPATH=" + classpath + "\n");
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
