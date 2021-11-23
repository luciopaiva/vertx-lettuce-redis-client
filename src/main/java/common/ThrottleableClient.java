package common;

import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.concurrent.atomic.AtomicInteger;

import static util.ByteArrayUtils.strToBytes;

public class ThrottleableClient {

    private static final AtomicInteger nextClientIndex = new AtomicInteger(0);

    private final int requestsPerSecond;
    private final Thread worker;
    private final LettuceClient client;
    private final AtomicInteger pending = new AtomicInteger(0);

    private int lastExecutionTimeInMillis = 0;

    public ThrottleableClient(String host, int port, int requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;

        worker = new Thread(this::run, "throtteable-client-" + nextClientIndex.incrementAndGet());
        client = new LettuceClient(host, port);
    }

    public void start() throws InterruptedException {
        worker.start();
        worker.join();
    }

    public AtomicInteger getPending() {
        return pending;
    }

    public int getLoad() {
        return (int) (lastExecutionTimeInMillis / 10d);
    }

    private void run() {
        RedisAsyncCommands<byte[], byte[]> commands = client.commands();
        byte[] key = strToBytes("foo");
        byte[] value = strToBytes("bar");

        while (true) {
            long startTime = System.currentTimeMillis();
            long nextRoundStartTime = startTime + 1000;

            for (int i = 0; i < requestsPerSecond; i++) {
                commands.set(key, value).whenComplete((result, t) -> pending.decrementAndGet());
                pending.incrementAndGet();
            }

            lastExecutionTimeInMillis = (int) (System.currentTimeMillis() - startTime);
            while (System.currentTimeMillis() < nextRoundStartTime) {
                Thread.yield();
            }
        }
    }
}
