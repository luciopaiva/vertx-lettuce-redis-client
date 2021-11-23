package examples;

import common.CountMetric;
import common.LettuceClient;
import common.MetricReporter;
import common.SupplierMetric;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static util.ByteArrayUtils.strToBytes;

public class ThroughputAsyncSingleThreadExample {

    private static final Logger logger = LogManager.getLogger(ThroughputAsyncSingleThreadExample.class);

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String ...args) {
        MetricReporter reporter = new MetricReporter(1000);
        CountMetric iterations = new CountMetric("iterations");
        CountMetric timeouts = new CountMetric("timeouts");
        CountMetric successes = new CountMetric("successes");
        reporter.addMetric(iterations);
        reporter.addMetric(timeouts);
        reporter.addMetric(successes);

        AtomicInteger totalIterations = new AtomicInteger(0);
        AtomicInteger totalSuccesses = new AtomicInteger(0);
        AtomicInteger totalTimeouts = new AtomicInteger(0);

        SupplierMetric pending = new SupplierMetric("total-pending",
                () -> String.valueOf(totalIterations.get() - totalSuccesses.get() - totalTimeouts.get()));
        reporter.addMetric(pending);

        logger.info("Starting...");
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6379;
        LettuceClient client = new LettuceClient("localhost", port);
        RedisAsyncCommands<byte[], byte[]> commands = client.commands();
        long timeToStop = System.currentTimeMillis() + 1_000;

        byte[] key = strToBytes("foo");
        byte[] value = strToBytes("bar");

        while (true) {
            long now = System.currentTimeMillis();

            if (now < timeToStop) {
                commands.set(key, value).whenComplete((result, t) -> {
                    if (t == null) {
                        successes.increment();
                        totalSuccesses.accumulateAndGet(1, Integer::sum);
                    } else {
                        if (t instanceof RedisCommandTimeoutException) {
                            timeouts.increment();
                            totalTimeouts.accumulateAndGet(1, Integer::sum);
                        } else {
                            logger.warn(t.getClass().getName());
                        }
                    }
                });

                iterations.increment();
                totalIterations.accumulateAndGet(1, Integer::sum);
            } else {
                LockSupport.parkNanos(1_000_000);
            }
        }
    }
}
