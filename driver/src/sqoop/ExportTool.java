package sqoop;

import java.util.List;
import java.util.LinkedList;

import common.DriverUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ExportTool extends SqoopToolAbstract {
  private final String exportDir;
  private final String table;

  public ExportTool(Document doc, Element etlElem, Element importElem, String cascadeId) {
    super(etlElem.getAttribute("sqoop_user_classpath"), etlElem.getAttribute("workdir"), "export", importElem.getAttribute("id"), DriverUtils.getDb(doc, importElem.getAttribute("db")), importElem.getAttribute("fieldDelimiter"));
    this.table = importElem.getAttribute("table");
    this.exportDir = "../../../cascade/" + cascadeId + "/data/" + importElem.getAttribute("id");
  }

  @Override
  final protected List<String> buildArgs() {
    List<String> args = new LinkedList<String>();
    super.buildArgs(args);
    args.add("--export-dir");
    args.add(exportDir);
    args.add("--table");
    args.add(table);
    args.add("--username");
    args.add(dbSpec.getUser().toUpperCase());
    args.add("--password");
    args.add(dbSpec.getPassword());
    return args;
  }
}