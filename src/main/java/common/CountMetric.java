package common;

import java.util.concurrent.atomic.AtomicInteger;

public class CountMetric extends Metric {

    private final AtomicInteger value;

    public CountMetric(String name) {
        super(name);
        value = new AtomicInteger();
    }

    public void increment() {
        value.incrementAndGet();
    }

    public void increment(int delta) {
        value.addAndGet(delta);
    }

    @Override
    public void reset() {
        value.set(0);
    }

    @Override
    public String get() {
        return String.valueOf(value.get());
    }
}
