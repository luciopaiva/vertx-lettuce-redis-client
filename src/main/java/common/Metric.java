package common;

import java.util.concurrent.atomic.AtomicInteger;

public class Metric {

    private final String name;
    private final AtomicInteger value;

    public Metric(String name) {
        this.name = name;
        value = new AtomicInteger();
    }

    public void increment() {
        value.incrementAndGet();
    }

    public void increment(int delta) {
        value.addAndGet(delta);
    }

    public void reset() {
        value.set(0);
    }

    public int get() {
        return value.get();
    }

    public String getAndReset() {
        String result = String.format("%s=%d", name, get());
        reset();
        return result;
    }
}
