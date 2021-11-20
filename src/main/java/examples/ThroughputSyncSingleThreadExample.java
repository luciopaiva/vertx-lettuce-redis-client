package examples;

import common.LettuceClient;
import io.lettuce.core.api.sync.RedisCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static util.ByteArrayUtils.strToBytes;

public class ThroughputSyncSingleThreadExample {

    private static final Logger logger = LogManager.getLogger(ThroughputSyncSingleThreadExample.class);

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String ...args) {
        logger.info("Starting...");
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6379;
        LettuceClient client = new LettuceClient("localhost", port);
        RedisCommands<byte[], byte[]> commands = client.syncCommands();

        byte[] key = strToBytes("foo");
        byte[] value = strToBytes("bar");

        int iterationsPerSecond = 0;
        long nextTimeReportMetrics = System.currentTimeMillis() + 1000;

        while (true) {
            commands.set(key, value);

            iterationsPerSecond++;
            if (nextTimeReportMetrics < System.currentTimeMillis()) {
                logger.info("Iterations per second: " + iterationsPerSecond);
                iterationsPerSecond = 0;
                nextTimeReportMetrics = System.currentTimeMillis() + 1000;
            }
        }
    }
}
