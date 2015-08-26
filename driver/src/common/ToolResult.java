package common;

public class ToolResult {
  private final int exitCode;
  private final ToolAbstract tool;

  public ToolResult(int exitCode, ToolAbstract tool) {
    this.exitCode = exitCode;
    this.tool = tool;
  }

  public int getExitCode() {
    return exitCode;
  }

  public ToolAbstract getTool() {
    return tool;
  }
}
