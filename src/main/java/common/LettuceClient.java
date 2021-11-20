package common;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class LettuceClient {

    private static final Logger logger = LogManager.getLogger(LettuceClient.class);

    private static final Duration COMMAND_TIMEOUT = Duration.ofMillis(1000);
    private static final Duration SOCKET_TIMEOUT = Duration.ofMillis(1000);

    private final StatefulRedisConnection<byte[], byte[]> connection;

    public LettuceClient(String host, Integer port) {
        logger.info(String.format("Creating relay Redis client (host: %s, port: %d)", host, port));

        RedisURI uri = RedisURI.create(host, port);
        RedisClient client = RedisClient.create(uri);
        client.setOptions(prepareClientOptions());

        connection = client.connect(new ByteArrayCodec());
        logger.info("Connected!");
    }

    private ClientOptions prepareClientOptions() {
        TimeoutOptions timeoutOptions = TimeoutOptions.builder().fixedTimeout(COMMAND_TIMEOUT).build();
        SocketOptions socketOptions = SocketOptions.builder().connectTimeout(SOCKET_TIMEOUT).build();

        return ClientOptions.builder()
                .autoReconnect(true)
                .cancelCommandsOnReconnectFailure(true)
                .requestQueueSize(Integer.MAX_VALUE)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .timeoutOptions(timeoutOptions)
                .socketOptions(socketOptions)
                .build();
    }

    public RedisAsyncCommands<byte[], byte[]> commands() {
        return connection.async();
    }

    public RedisCommands<byte[], byte[]> syncCommands() {
        return connection.sync();
    }

    /**
     * Redis commands invoked inside `callback` will be pipelined to the server, i.e., no waiting time between commands.
     *
     * Beware this method will BLOCK the calling thread until all futures are completed.
     */
    public void batch(Consumer<List<CompletableFuture<?>>> callback) throws ExecutionException, InterruptedException {
        batchAsync(callback).get();
    }

    /**
     * Redis commands invoked inside `callback` will be pipelined to the server, i.e., no waiting time between commands.
     *
     * This method will *not* block the calling thread.
     */
    public CompletableFuture<Void> batchAsync(Consumer<List<CompletableFuture<?>>> callback) {
        connection.setAutoFlushCommands(false);
        List<CompletableFuture<?>> futures = new ArrayList<>();
        callback.accept(futures);
        connection.flushCommands();
        connection.setAutoFlushCommands(true);

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
