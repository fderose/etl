package sqoop;

import java.util.List;
import java.util.LinkedList;

import common.DriverUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ImportTool extends SqoopToolAbstract {
  private final String query;
  private final String targetDir = "data";
  private final String splitBy;

  public ImportTool(Document doc, Element etlElem, Element importElem, Element dbElem) {
    super(etlElem.getAttribute("sqoop_user_classpath"), etlElem.getAttribute("workdir"), "import", importElem.getAttribute("id"), DriverUtils.getDb(doc, dbElem.getTextContent()), importElem.getAttribute("fieldDelimiter"));
    this.query = importElem.getAttribute("query");
    this.splitBy = importElem.getAttribute("splitBy");
  }

  @Override
  final protected List<String> buildArgs() {
    List<String> args = new LinkedList<String>();
    super.buildArgs(args);
    args.add("--query");
    args.add("\"" + query + "\"");
    args.add("--target-dir");
    args.add(targetDir);
    args.add("--split-by");
    args.add(splitBy);
    return args;
  }
}