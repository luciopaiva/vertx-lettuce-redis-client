package examples;

import common.MetricReporter;
import common.SupplierMetric;
import common.ThrottleableClient;

public class ThroughputAsyncSingleThreadExample3 {

    public static void main(String ...args) throws InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6379;

        ThrottleableClient client = new ThrottleableClient("localhost", port, 50000);

        MetricReporter reporter = new MetricReporter(1000);
        SupplierMetric pending = new SupplierMetric("pending", () -> String.valueOf(client.getPending()));
        SupplierMetric load = new SupplierMetric("load", () -> String.valueOf(client.getLoad()));
        reporter.addMetric(pending);
        reporter.addMetric(load);

        client.start();
    }
}
