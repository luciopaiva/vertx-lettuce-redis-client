import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static util.ByteArrayUtils.intToBytes;
import static util.ByteArrayUtils.strToBytes;

public class BatchExample {

    private static final Logger logger = LogManager.getLogger(BatchExample.class);

    private static LettuceClient client;

    private static CompletableFuture<?> task() {
        logger.info("Running...");

        return client.batchAsync(BatchExample::setKeys);
    }

    private static void setKeys(List<CompletableFuture<?>> futures) {
        for (int i = 1; i < 100; i++) {
            CompletableFuture<?> future =
                    client.commands().setex(makeKey(i), 3, intToBytes(i)).toCompletableFuture();
            futures.add(future);
        }
    }

    private static byte[] makeKey(int id) {
        String idStr = String.valueOf(id);
        String key = "foo-".concat(idStr);
        return strToBytes(key);
    }

    public static void main(String ...args) {
        logger.info("Starting...");
        client = new LettuceClient("localhost", 6379);
        VertxRunner.run(BatchExample::task, 1000);
    }
}
