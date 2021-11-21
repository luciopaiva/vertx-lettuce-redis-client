package common;

public abstract class Metric {

    private final String name;

    public Metric(String name) {
        this.name = name;
    }

    public abstract String get();
    public abstract void reset();

    public String getAndReset() {
        String result = String.format("%s=%s", name, get());
        reset();
        return result;
    }
}
