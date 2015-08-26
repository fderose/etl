package cascade;

import cascading.flow.Flow;
import cascading.flow.FlowConnector;
import cascading.flow.FlowDef;
import cascading.flow.FlowProps;
import cascading.operation.Identity;
import cascading.operation.aggregator.Sum;
import cascading.pipe.*;
import cascading.pipe.assembly.AggregateBy;
import cascading.pipe.assembly.SumBy;
import cascading.property.AppProps;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import common.Utils;
import common.Utils.FlowType;
import java.util.Properties;

public class Main {
  private static final Utils.FlowType flowType = FlowType.hadoop;

  public static void main(String[] args) {
    // Set up the input and output paths
    String headersInPath = args[0];
    String activitiesInPath = args[1];
    String paymentsInPath = args[2];
    String headersOutPath = "data/headers";
    String activitiesOutPath = "data/activities";
    String paymentsOutPath = "data/payments";

    // Perform some preliminary configuration.
    Properties properties = new Properties();
    AppProps.setApplicationJarClass(properties, Main.class);
    properties.setProperty(FlowProps.PRESERVE_TEMPORARY_FILES, "true");

    // Define the fields in each stream of records
    Fields headerFields = new Fields("Headers.ProducerPublicId", "Headers.StatementDate", "PreviousBalance");
    Fields activitiesFields = new Fields("Activities.ProducerPublicId", "Activities.StatementDate", "Activities.PolicyNumber", "AccountNumber",
      "ItemEventDate", "Event", "InvoiceItemType", "ChargeId", "EventDate", "BasisAmount", "ChargeName", "TransactionId", "Activities.TransactionAmount");
    Fields paymentsFields = new Fields("Payments.ProducerPublicId", "Payments.StatementDate", "Payments.PolicyNumber", "Context", "Payments.TransactionAmount", "TransactionDate",
      "TransactionNumber");

    // Define the input taps.
    Tap headersInTap = Utils.createTap(flowType, headerFields, false, headersInPath, "");
    Tap activitiesInTap = Utils.createTap(flowType, activitiesFields, false, activitiesInPath, "");
    Tap paymentsInTap= Utils.createTap(flowType, paymentsFields, false, paymentsInPath, "");

    // Define the output taps.
    Tap headersOutTap = Utils.createTap(flowType, false, headersOutPath, "");
    Tap activitiesOutTap = Utils.createTap(flowType, false, activitiesOutPath, "");
    Tap paymentsOutTap = Utils.createTap(flowType, false, paymentsOutPath, "");

    // Group Header records by Producer and StatementDate and sum PreviousBalance
    Pipe headersPipe = new Pipe("headersPipe");
    Fields headersGroupFields = new Fields("Headers.ProducerPublicId", "Headers.StatementDate");
    headersPipe = new GroupBy(headersPipe, headersGroupFields, headersGroupFields);
    headersPipe = new Every(headersPipe, new Fields("PreviousBalance"), new Sum(new Fields("PreviousBalance")), Fields.ALL);

    // Group Activity records by Producer and StatementDate and sum TransactionAmount and BasisAmount
    Pipe activitiesPipeUngrouped = new Pipe("activitiesPipeUngrouped");
    Pipe activitiesPipeGrouped = new Pipe("activitiesPipeGrouped", activitiesPipeUngrouped);
    Fields activitiesGroupFields = new Fields("Activities.ProducerPublicId", "Activities.StatementDate");
    SumBy transactionAmountSum = new SumBy(new Fields("Activities.TransactionAmount"), new Fields("TotalActivityAmount"), double.class);
    SumBy basisAmountSum = new SumBy(new Fields("BasisAmount"), new Fields("TotalBasisAmount"), double.class);
    activitiesPipeGrouped = new AggregateBy(activitiesPipeGrouped, activitiesGroupFields, transactionAmountSum, basisAmountSum);

    // Group Payments records by Producer and StatementDate and sum TransactionAmount
    Pipe paymentsPipeUngrouped = new Pipe("paymentsPipeUngrouped");
    Pipe paymentsPipeGrouped = new Pipe("paymentsPipeGrouped", paymentsPipeUngrouped);
    Fields paymentsGroupFields = new Fields("Payments.ProducerPublicId", "Payments.StatementDate");
    SumBy paymentAmountSum = new SumBy(new Fields("Payments.TransactionAmount"), new Fields("TotalPaymentAmount"), double.class);
    paymentsPipeGrouped = new AggregateBy(paymentsPipeGrouped, paymentsGroupFields, paymentAmountSum);

    // Join the summed header records with the summed Activity and Payment records
    Pipe outputPipe = new HashJoin(headersPipe, new Fields("Headers.ProducerPublicId"), activitiesPipeGrouped, new Fields("Activities.ProducerPublicId"));
    outputPipe = new HashJoin(outputPipe, new Fields("Headers.ProducerPublicId"), paymentsPipeGrouped, new Fields("Payments.ProducerPublicId"));

    // Rename fields
    outputPipe = new Each(outputPipe, new Fields("Headers.ProducerPublicId", "Headers.StatementDate", "PreviousBalance", "TotalActivityAmount", "TotalBasisAmount", "TotalPaymentAmount"), new Identity());

    // Hook up input and output taps to the sources and tails of the pipes.
    FlowDef flowDef = FlowDef.flowDef()
      .setName("bc")
      .addSource(headersPipe, headersInTap)
      .addSource(activitiesPipeUngrouped, activitiesInTap)
      .addSource(paymentsPipeUngrouped, paymentsInTap)
      .addTailSink(outputPipe, headersOutTap)
      .addTailSink(activitiesPipeUngrouped, activitiesOutTap)
      .addTailSink(paymentsPipeUngrouped, paymentsOutTap)
      ;

    // Allocate a flow connector to validate the flow and connect the pipes, write a DOT file, and run the flow
    FlowConnector flowConnector = Utils.createFlowConnector(flowType, properties);
    Flow wcFlow = flowConnector.connect(flowDef);
    wcFlow.writeDOT("dot/bcas.dot");
    wcFlow.complete();
  }
}
