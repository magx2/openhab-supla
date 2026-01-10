package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import java.time.Duration;

public record TimeoutConfiguration(Duration timeout, Duration min, Duration max) {
    public TimeoutConfiguration {
        if (min.compareTo(timeout) > 0) {
            throw new IllegalArgumentException("min (%s) has to be smaller than timeout (%s)!".formatted(min, timeout));
        }
        if (timeout.compareTo(max) > 0) {
            throw new IllegalArgumentException("timeout (%s) has to be smaller than max (%s)!".formatted(timeout, max));
        }
    }

    public TimeoutConfiguration(int timeout, int min, int max) {
        this(Duration.ofSeconds(timeout), Duration.ofSeconds(min), Duration.ofSeconds(max));
    }
}
