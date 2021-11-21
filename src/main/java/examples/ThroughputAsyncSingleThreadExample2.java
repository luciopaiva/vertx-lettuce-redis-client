package examples;

import common.CountMetric;
import common.LettuceClient;
import common.MetricReporter;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static util.ByteArrayUtils.strToBytes;

public class ThroughputAsyncSingleThreadExample2 {

    private static final Logger logger = LogManager.getLogger(ThroughputAsyncSingleThreadExample2.class);

    public static void main(String ...args) {
        MetricReporter reporter = new MetricReporter(1000);
        CountMetric iterations = new CountMetric("iterations");
        CountMetric timeouts = new CountMetric("timeouts");
        CountMetric successes = new CountMetric("successes");
        reporter.addMetric(iterations);
        reporter.addMetric(timeouts);
        reporter.addMetric(successes);

        logger.info("Starting...");
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6379;
        LettuceClient client = new LettuceClient("localhost", port);
        RedisAsyncCommands<byte[], byte[]> commands = client.commands();

        byte[] key = strToBytes("foo");
        byte[] value = strToBytes("bar");

        for (int i = 0; i < 10; i++) {
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

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            iterations.increment();
        }
    }
}
