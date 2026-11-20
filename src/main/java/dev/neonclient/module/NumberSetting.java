package dev.neonclient.module;

public class NumberSetting {
    public final String name;
    public final double min, max, step;
    public double value;

    public NumberSetting(String name, double min, double max, double def, double step) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = def;
    }

    public void set(double v) {
        if (step > 0) v = Math.round(v / step) * step;
        this.value = Math.max(min, Math.min(max, v));
    }
}
