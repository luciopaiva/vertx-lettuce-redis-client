import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    private static RedisAsyncCommands<String, String> commands;
    private static Vertx vertx;

    private static void run() {
        logger.info("running");

        commands.set("foo", "bar")
                .thenCompose(a -> commands.get("foo"))
                .thenAccept(v -> logger.info(String.format("received value '%s'", v)))
                .thenCompose(a -> commands.del("foo"))
                .thenAccept(n -> logger.info(String.format("keys deleted: %d", n)))
                .thenRun(Main::resetTimer);
    }

    private static void resetTimer() {
        logger.info("will reset timer");
        vertx.getOrCreateContext().runOnContext(h -> {
            logger.info("resetting timer");
            vertx.setTimer(1000, id -> run());
        });
    }

    private static void setupRedisClient() {
        RedisURI uri = RedisURI.create("localhost", 6379);
        RedisClient client = RedisClient.create(uri);
        StatefulRedisConnection<String, String> connection = client.connect();
        commands = connection.async();
        logger.info("connected!");
    }

    public static void main(String ...args) {
        logger.info("starting...");
        setupRedisClient();

        vertx = Vertx.vertx();
        resetTimer();
    }
}
