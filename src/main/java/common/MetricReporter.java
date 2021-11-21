package common;

import examples.ThroughputAsyncSingleThreadExample;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class MetricReporter {

    private static final Logger logger = LogManager.getLogger(MetricReporter.class);

    private final List<Metric> metrics;
    private final int periodInMillis;

    public MetricReporter(int periodInMillis) {
        this.periodInMillis = periodInMillis;

        metrics = new ArrayList<>();
        Thread thread = new Thread(this::loop, "metrics");
        thread.start();
    }

    public void addMetric(Metric metric) {
        metrics.add(metric);
    }

    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    public void loop() {
        while (true) {
            try {
                Thread.sleep(periodInMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            StringJoiner joiner = new StringJoiner(" ");
            for (Metric metric : metrics) {
                joiner.add(metric.getAndReset());
            }
            logger.info(joiner.toString());
        }
    }
}
