package common;

import java.util.Properties;
import cascading.flow.FlowConnector;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.flow.local.LocalFlowConnector;
import cascading.tap.Tap;
import cascading.tap.hadoop.Lfs;
import cascading.tap.local.FileTap;
import cascading.tuple.Fields;

public class Utils {

  public static enum FlowType {hadoop, local}

  public static Tap createTap(FlowType flowType, String outPath, String index) {
    return createTap(flowType, Fields.ALL, true, outPath, index);
  }

  public static Tap createTap(FlowType flowType, boolean hasHeaders, String outPath, String index) {
    return createTap(flowType, Fields.ALL, hasHeaders, outPath, index);
  }

  public static Tap createTap(FlowType flowType, Fields fields, boolean hasHeaders, String outPath, String index) {
    switch (flowType) {
      case hadoop:
        return new Lfs(new cascading.scheme.hadoop.TextDelimited(fields, hasHeaders, "\t"), outPath + index);
      case local:
        return new FileTap(new cascading.scheme.local.TextDelimited(fields, hasHeaders, "\t"), outPath + index);
      default:
        throw new RuntimeException("unknown flow type");
    }
  }

  public static FlowConnector createFlowConnector(FlowType flowType, Properties properties) {
    switch (flowType) {
      case hadoop:
        return new HadoopFlowConnector(properties);
      case local:
        return new LocalFlowConnector(properties);
      default:
        throw new RuntimeException("unknown flow type");
    }
  }


}
