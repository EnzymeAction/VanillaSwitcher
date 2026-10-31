package net.vanillaswitcher.util;

public final class BrightnessTransition {
    public static final double VANILLA = 1.0;

    private double from = VANILLA;
    private double to = VANILLA;
    private long startNanos = 0L;
    private long durationNanos = 0L;

    public double valueAt(long nowNanos) {
        if (durationNanos <= 0L) {
            return to;
        }
        long elapsed = nowNanos - startNanos;
        if (elapsed >= durationNanos) {
            return to;
        }
        if (elapsed <= 0L) {
            return from;
        }
        double progress = (double) elapsed / (double) durationNanos;
        return from + (to - from) * progress;
    }

    public void startTowards(double target, double unitsPerSecond, long nowNanos) {
        double current = valueAt(nowNanos);
        double distance = Math.abs(target - current);

        if (distance < 1.0e-9 || !(unitsPerSecond > 0.0)) {
            snapTo(target);
            return;
        }

        this.from = current;
        this.to = target;
        this.startNanos = nowNanos;
        this.durationNanos = Math.max(1L, Math.round(distance / unitsPerSecond * 1.0e9));
    }

    public void fadeTo(double target, double strength, double seconds, long nowNanos) {
        double current = valueAt(nowNanos);
        double fullSwing = Math.max(Math.max(strength, target), current) - VANILLA;
        double safeSeconds = Math.max(0.05, seconds);
        startTowards(target, fullSwing / safeSeconds, nowNanos);
    }

    public void snapTo(double value) {
        this.from = value;
        this.to = value;
        this.durationNanos = 0L;
    }

    public double target() {
        return to;
    }

    public boolean isRunning(long nowNanos) {
        return durationNanos > 0L && (nowNanos - startNanos) < durationNanos;
    }
}
