package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import java.time.Duration;

public record TimeoutConfiguration(Duration timeout, Duration min, Duration max) {
    public TimeoutConfiguration(int timeout, int min, int max) {
        this(Duration.ofSeconds(timeout), Duration.ofSeconds(min), Duration.ofSeconds(max));
    }
}
