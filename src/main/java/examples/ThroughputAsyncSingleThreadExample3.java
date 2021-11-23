package examples;

import common.MetricReporter;
import common.SupplierMetric;
import common.ThrottleableClient;

public class ThroughputAsyncSingleThreadExample3 {

    public static void main(String ...args) throws InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6379;

        ThrottleableClient client = new ThrottleableClient("localhost", port, 100000);

        MetricReporter reporter = new MetricReporter(1000);
        SupplierMetric pending = new SupplierMetric("pending", () -> String.valueOf(client.getPending()));
        reporter.addMetric(pending);

        client.start();
    }
}
