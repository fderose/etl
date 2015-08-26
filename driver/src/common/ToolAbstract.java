package common;

import java.io.*;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class ToolAbstract implements Callable<ToolResult> {
  protected final String classpath;
  protected final String id;
  protected final String tool;

  private Process process;
  private FileOutputStream fos;
  private Thread stdOutThread;
  private Thread stdErrThread;
  private int exitValue;

  public ToolAbstract(String classpath, String tool, String id) {
    this.classpath = classpath;
    this.id = id;
    this.tool = tool;
  }

  protected abstract File getWorkingDirectory();

  public abstract String getToolName();

  public abstract String getLogFileName();

  protected abstract File getDriverFile();

  protected abstract void buildArgs(List<String> args);

  protected abstract List<String> buildArgs();

  private static class LogRunnable implements Runnable {
    private final InputStream is;
    private final OutputStream os;

    LogRunnable(InputStream is, OutputStream os) {
      this.is = is;
      this.os = os;
    }

    @Override
    public void run() {
      String line;
      BufferedReader br = null;
      try {
        br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
        while ((line = br.readLine()) != null) {
          os.write((line + "\n").getBytes("UTF-8"));
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (br != null) {
            br.close();
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void startStdOutThread() {
    LogRunnable runnable = new LogRunnable(process.getInputStream(), fos);
    stdOutThread = new Thread(runnable, getToolName());
    stdOutThread.start();
  }

  private void startStdErrThread() {
    LogRunnable runnable = new LogRunnable(process.getErrorStream(), fos);
    stdErrThread = new Thread(runnable, getToolName());
    stdErrThread.start();
  }

  private FileOutputStream getLog() {
    File file = new File(getLogFileName());
    try {
      return new FileOutputStream(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private void startProcess() {
    try {
      ProcessBuilder pb = new ProcessBuilder(getDriverFile().getCanonicalPath());
      pb.directory(getWorkingDirectory());
      process = pb.start();
      fos = getLog();
      startStdOutThread();
      startStdErrThread();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void waitForProcess() {
    try {
      process.waitFor();
      stdOutThread.join();
      stdErrThread.join();
      fos.close();
      exitValue = process.exitValue();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ToolResult call() {
    startProcess();
    waitForProcess();
    return new ToolResult(exitValue, this);
  }
}
