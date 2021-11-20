package common;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class VertxRunner {

    private static final Logger logger = LogManager.getLogger(VertxRunner.class);

    private final Vertx vertx;
    private final Supplier<CompletableFuture<?>> callback;
    private final int periodInMillis;

    public VertxRunner(Supplier<CompletableFuture<?>> callback, int periodInMillis) {
        this.callback = callback;
        this.periodInMillis = periodInMillis;
        vertx = Vertx.vertx();
        resetTimer();
    }

    private void resetTimer() {
        vertx.getOrCreateContext().runOnContext(h -> {
            logger.info("Resetting Vert.x timer...");
            vertx.setTimer(periodInMillis, id -> callback.get().whenComplete((v, ex) -> resetTimer()));
        });
    }

    public static void run(Supplier<CompletableFuture<?>> callback, int periodInMillis) {
        new VertxRunner(callback, periodInMillis);
    }
}
