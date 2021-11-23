package common;

import io.lettuce.core.api.async.RedisAsyncCommands;

import java.util.concurrent.atomic.AtomicInteger;

import static util.ByteArrayUtils.strToBytes;

public class ThrottleableClient {

    private static AtomicInteger nextClientIndex = new AtomicInteger(0);

    private final int maxPendingCommands;
    private final Thread worker;
    private final LettuceClient client;

    private AtomicInteger pending = new AtomicInteger(0);

    public ThrottleableClient(String host, int port, int maxPendingCommands) {
        this.maxPendingCommands = maxPendingCommands;

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

    private void run() {
        RedisAsyncCommands<byte[], byte[]> commands = client.commands();
        byte[] key = strToBytes("foo");
        byte[] value = strToBytes("bar");

        while (true) {
            while (pending.get() < maxPendingCommands) {
                commands.set(key, value).whenComplete((result, t) -> pending.decrementAndGet());
                pending.incrementAndGet();
            }

            Thread.yield();
        }
    }
}
