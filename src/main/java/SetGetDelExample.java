import io.lettuce.core.api.async.RedisAsyncCommands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

import static util.ByteArrayUtils.bytesToStr;
import static util.ByteArrayUtils.strToBytes;

public class SetGetDelExample {

    private static final Logger logger = LogManager.getLogger(SetGetDelExample.class);

    private static LettuceClient client;

    private static CompletableFuture<?> task() {
        logger.info("Running...");

        byte[] key = strToBytes("foo");
        byte[] value = strToBytes("bar");

        RedisAsyncCommands<byte[], byte[]> commands = client.commands();
        return commands.set(key, value)
                .thenCompose(a -> commands.get(key))
                .thenAccept(v -> logger.info(String.format("received value '%s'", bytesToStr(v))))
                .thenCompose(a -> commands.del(key))
                .thenAccept(n -> logger.info(String.format("keys deleted: %d", n)))
                .toCompletableFuture();
    }

    public static void main(String ...args) {
        logger.info("Starting...");
        client = new LettuceClient("localhost", 6379);
        VertxRunner.run(SetGetDelExample::task, 1000);
    }
}
