package examples;

import common.CountMetric;
import common.MetricReporter;
import common.SupplierMetric;
import common.ThrottleableClient;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ThroughputAsyncMultiThreadExample {

    public static void main(String ...args) throws InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6379;

        MetricReporter reporter = new MetricReporter(1000);

        List<ThrottleableClient> clients = IntStream.rangeClosed(1, 2)
                .mapToObj(id -> {
                    CountMetric failures = new CountMetric("failures-" + id);
                    reporter.addMetric(failures);
                    ThrottleableClient client =
                            new ThrottleableClient("localhost", port, 40000, failures);

                    SupplierMetric pending = new SupplierMetric("pending-" + id,
                            () -> String.valueOf(client.getPending()));
                    SupplierMetric load = new SupplierMetric("load-" + id,
                            () -> String.valueOf(client.getLoad()));
                    reporter.addMetric(pending);
                    reporter.addMetric(load);

                    client.start();

                    return client;
                })
                .collect(Collectors.toList());

        for (ThrottleableClient client : clients) {
            client.join();
        }
    }
}
