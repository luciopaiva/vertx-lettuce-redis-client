package common;

import java.util.function.Supplier;

public class SupplierMetric extends Metric {

    private final Supplier<String> supplier;

    public SupplierMetric(String name, Supplier<String> supplier) {
        super(name);
        this.supplier = supplier;
    }

    @Override
    public String get() {
        return supplier.get();
    }

    @Override
    public void reset() {
    }
}
