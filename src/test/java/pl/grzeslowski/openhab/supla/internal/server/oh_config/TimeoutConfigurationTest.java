package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TimeoutConfigurationTest {
    @Test
    void shouldKeepTimeoutValues() {
        var timeout = new TimeoutConfiguration(Duration.ofSeconds(5), Duration.ofSeconds(3), Duration.ofSeconds(7));

        assertThat(timeout.timeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(timeout.min()).isEqualTo(Duration.ofSeconds(3));
        assertThat(timeout.max()).isEqualTo(Duration.ofSeconds(7));
    }

    @Test
    void shouldConvertIntSecondsToDuration() {
        var timeout = new TimeoutConfiguration(6, 4, 9);

        assertThat(timeout.timeout()).isEqualTo(Duration.ofSeconds(6));
        assertThat(timeout.min()).isEqualTo(Duration.ofSeconds(4));
        assertThat(timeout.max()).isEqualTo(Duration.ofSeconds(9));
    }

    @Test
    void shouldConvertStringSecondsToDuration() {
        var timeout = new TimeoutConfiguration("6", "4", "9");

        assertThat(timeout.timeout()).isEqualTo(Duration.ofSeconds(6));
        assertThat(timeout.min()).isEqualTo(Duration.ofSeconds(4));
        assertThat(timeout.max()).isEqualTo(Duration.ofSeconds(9));
    }

    @Test
    void shouldConvertStringSecondsWithFractionToDuration() {
        var timeout = new TimeoutConfiguration("1.5", "1.2", "1.8");

        assertThat(timeout.timeout()).isEqualTo(Duration.ofSeconds(1).plusMillis(500));
        assertThat(timeout.min()).isEqualTo(Duration.ofSeconds(1).plusMillis(200));
        assertThat(timeout.max()).isEqualTo(Duration.ofSeconds(1).plusMillis(800));
    }

    @Test
    void shouldRejectNonPositiveTimeout() {
        assertThatThrownBy(() -> new TimeoutConfiguration(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout has to be grater than 0. Was PT0S");
    }

    @Test
    void shouldRejectNonPositiveMin() {
        assertThatThrownBy(() -> new TimeoutConfiguration(Duration.ofSeconds(1), Duration.ZERO, Duration.ofSeconds(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("min has to be grater than 0. Was PT0S");
    }

    @Test
    void shouldRejectNonPositiveMax() {
        assertThatThrownBy(() -> new TimeoutConfiguration(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("max has to be grater than 0. Was PT0S");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", "0.00"})
    void shouldRejectZeroTimeoutFromString(String value) {
        assertThatThrownBy(() -> new TimeoutConfiguration(value, "1", "2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout has to be grater than 0. Was PT0S");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", "0.00"})
    void shouldRejectZeroMinFromString(String value) {
        assertThatThrownBy(() -> new TimeoutConfiguration("1", value, "2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("min has to be grater than 0. Was PT0S");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", "0.00"})
    void shouldRejectZeroMaxFromString(String value) {
        assertThatThrownBy(() -> new TimeoutConfiguration("1", "1", value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("max has to be grater than 0. Was PT0S");
    }

    @Test
    void shouldRejectMinGreaterThanTimeout() {
        assertThatThrownBy(() ->
                        new TimeoutConfiguration(Duration.ofSeconds(5), Duration.ofSeconds(6), Duration.ofSeconds(7)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("min (PT6S) has to be smaller than timeout (PT5S)!");
    }

    @Test
    void shouldRejectTimeoutGreaterThanMax() {
        assertThatThrownBy(() ->
                        new TimeoutConfiguration(Duration.ofSeconds(8), Duration.ofSeconds(6), Duration.ofSeconds(7)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("timeout (PT8S) has to be smaller than max (PT7S)!");
    }

    @Test
    void shouldParseDurationFromNullAsEmpty() {
        assertThat(TimeoutConfiguration.tryParseDuration(null)).isEmpty();
    }

    @Test
    void shouldParseDurationFromDoubleSeconds() {
        assertThat(TimeoutConfiguration.tryParseDuration("2.25"))
                .contains(Duration.ofSeconds(2).plusMillis(250));
    }

    @ParameterizedTest
    @CsvSource({"1.5,1,500", "2.25,2,250", "10.75,10,750"})
    void shouldParseDurationFromDoubleSecondsParameterized(String value, long seconds, long millis) {
        assertThat(TimeoutConfiguration.tryParseDuration(value))
                .contains(Duration.ofSeconds(seconds).plusMillis(millis));
    }

    @Test
    void shouldParseDurationFromIso() {
        assertThat(TimeoutConfiguration.tryParseDuration("PT3S")).contains(Duration.ofSeconds(3));
    }

    @Test
    void shouldReturnEmptyWhenCannotParseDuration() {
        assertThat(TimeoutConfiguration.tryParseDuration("not-a-duration")).isEmpty();
    }
}
