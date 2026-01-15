package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;

import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    public TimeoutConfiguration(String timeout, String min, String max) {
        this(parse(timeout), parse(min), parse(max));
    }

    public static Optional<Duration> tryParseDuration(@Nullable String timeout) {
        if (timeout == null) return Optional.empty();
        try {
            return Optional.of(parse(timeout));
        } catch (Exception e) {
            log.debug("Cannot parse duration from {}", timeout, e);
            return Optional.empty();
        }
    }

    private static Duration parse(@NonNull String timeout) {
        try {
            var parsed = parseDouble(timeout);
            // cast to int (remove what after .) to get seconds
            var seconds = (int) parsed;
            // take the reminder and use it as millis
            var milliseconds = (int) ((parsed - seconds) * 1_000);
            return Duration.ofSeconds((int) parsed).plusMillis(milliseconds);
        } catch (NumberFormatException e) {
            log.debug("Cannot parse double from {}", timeout, e);
        }
        try {
            var parsed = parseLong(timeout);
            return Duration.ofSeconds(parsed);
        } catch (NumberFormatException e) {
            log.debug("Cannot parse long from {}", timeout, e);
        }
        try {
            return Duration.parse(timeout);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Cannot parse duration from " + timeout, e);
        }
    }
}
