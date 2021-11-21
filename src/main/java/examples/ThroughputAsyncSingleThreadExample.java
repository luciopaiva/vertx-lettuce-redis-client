package examples;

import common.LettuceClient;
import common.Metric;
import common.MetricReporter;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static util.ByteArrayUtils.strToBytes;

public class ThroughputAsyncSingleThreadExample {

    private static final Logger logger = LogManager.getLogger(ThroughputAsyncSingleThreadExample.class);

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String ...args) {
        MetricReporter reporter = new MetricReporter(1000);
        Metric iterations = new Metric("iterations");
        Metric timeouts = new Metric("timeouts");
        Metric successes = new Metric("successes");
        reporter.addMetric(iterations);
        reporter.addMetric(timeouts);
        reporter.addMetric(successes);

        logger.info("Starting...");
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6379;
        LettuceClient client = new LettuceClient("localhost", port);
        RedisAsyncCommands<byte[], byte[]> commands = client.commands();

        byte[] key = strToBytes("foo");
        byte[] value = strToBytes("bar");

        while (true) {
            commands.set(key, value).whenComplete((result, t) -> {
                if (t == null) {
                    successes.increment();
                } else {
                    if (t instanceof RedisCommandTimeoutException) {
                        timeouts.increment();
                    } else {
                        logger.warn(t.getClass().getName());
                    }
                }
            });

            iterations.increment();
        }
    }
}
