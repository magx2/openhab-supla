package pl.grzeslowski.openhab.supla.internal.server.device_config;

import static java.util.Objects.requireNonNullElse;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;
import static pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfig.DisableUserInterfaceConfig.UserInterface.PARTIAL;
import static pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigField.*;
import static pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigUtil.*;

import jakarta.annotation.Nullable;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import pl.grzeslowski.jsupla.protocol.api.encoders.*;
import pl.grzeslowski.jsupla.protocol.api.structs.*;

@NonNullByDefault
public sealed interface DeviceConfig extends Comparable<DeviceConfig> {
    DeviceConfigField field();

    byte[] encode();

    @Override
    default int compareTo(DeviceConfig o) {
        return Comparator.comparing(DeviceConfig::field).compare(this, o);
    }

    public record PowerStatusLedConfig(boolean disabled) implements DeviceConfig {
        @Override
        public DeviceConfigField field() {
            return POWER_STATUS_LED;
        }

        @Override
        public byte[] encode() {
            return PowerStatusLedEncoder.INSTANCE.encode(new PowerStatusLed(encodeBoolean(disabled)));
        }

        public static PowerStatusLedConfig parse(List<String> parameters) {
            return new PowerStatusLedConfig(parseBoolean(parameters.get(0)));
        }
    }

    public record HomeScreenOffDelayTypeConfig(@NonNull DelayType delayType) implements DeviceConfig {
        @Override
        public DeviceConfigField field() {
            return HOME_SCREEN_OFF_DELAY_TYPE;
        }

        @Override
        public byte[] encode() {
            return HomeScreenOffDelayTypeEncoder.INSTANCE.encode(new HomeScreenOffDelayType((short) delayType.value));
        }

        public static HomeScreenOffDelayTypeConfig parse(List<String> parameters) {
            return new HomeScreenOffDelayTypeConfig(parseEnum(parameters.get(0), DelayType.class));
        }

        @RequiredArgsConstructor
        public enum DelayType {
            ALWAYS_ENABLED(SUPLA_DEVCFG_HOME_SCREEN_OFF_DELAY_TYPE_ALWAYS_ENABLED),
            ENABLED_WHEN_DARK(SUPLA_DEVCFG_HOME_SCREEN_OFF_DELAY_TYPE_ENABLED_WHEN_DARK);
            private final int value;
        }
    }

    public record HomeScreenContentConfig(@NonNull List<HomeScreenContent> contents) implements DeviceConfig {
        @Override
        public DeviceConfigField field() {
            return HOME_SCREEN_CONTENT;
        }

        @Override
        public byte[] encode() {
            var fields = contents.stream().reduce(0L, (field, content) -> field | content.value, Long::sum);
            return DeviceConfigHomeScreenContentEncoder.INSTANCE.encode(
                    new DeviceConfigHomeScreenContent(new BigInteger("fields"), new BigInteger("fields")));
        }

        public static HomeScreenContentConfig parse(List<String> parameters) {
            return new HomeScreenContentConfig(parameters.stream()
                    .map(p -> parseEnum(p, HomeScreenContent.class))
                    .toList());
        }

        @RequiredArgsConstructor
        public enum HomeScreenContent {
            NONE(SUPLA_DEVCFG_HOME_SCREEN_CONTENT_NONE),
            TEMPERATURE(SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TEMPERATURE),
            TEMPERATURE_AND_HUMIDITY(SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TEMPERATURE_AND_HUMIDITY),
            TIME(SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TIME),
            TIME_DATE(SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TIME_DATE),
            TEMPERATURE_TIME(SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TEMPERATURE_TIME),
            MAIN_AND_AUX_TEMPERATURE(SUPLA_DEVCFG_HOME_SCREEN_CONTENT_MAIN_AND_AUX_TEMPERATURE),
            MODE_OR_TEMPERATURE(SUPLA_DEVCFG_HOME_SCREEN_CONTENT_MODE_OR_TEMPERATURE);

            private final long value;
        }
    }

    public record HomeScreenOffDelayConfig(
            boolean enabled, @NonNull Duration duration) implements DeviceConfig {
        @Override
        public DeviceConfigField field() {
            return HOME_SCREEN_OFF_DELAY;
        }

        @Override
        public byte[] encode() {
            return DeviceConfigHomeScreenOffDelayEncoder.INSTANCE.encode(
                    new DeviceConfigHomeScreenOffDelay(enabled ? (short) duration.toSeconds() : 0));
        }

        public static HomeScreenOffDelayConfig parse(List<String> parameters) {
            return new HomeScreenOffDelayConfig(parseBoolean(parameters.get(0)), parseDuration(parameters.get(1)));
        }
    }

    public record AutomaticTimeSyncConfig(boolean enabled) implements DeviceConfig {
        @Override
        public DeviceConfigField field() {
            return AUTOMATIC_TIME_SYNC;
        }

        @Override
        public byte[] encode() {
            return DeviceConfigAutomaticTimeSyncEncoder.INSTANCE.encode(
                    new DeviceConfigAutomaticTimeSync(encodeBoolean(enabled)));
        }

        public static AutomaticTimeSyncConfig parse(List<String> parameters) {
            return new AutomaticTimeSyncConfig(parseBoolean(parameters.get(0)));
        }
    }

    public record DisableUserInterfaceConfig(
            @NonNull UserInterface userInterface,
            @Nullable Integer minTemp,
            @Nullable Integer maxTemp) implements DeviceConfig {
        @Override
        public DeviceConfigField field() {
            return DISABLE_USER_INTERFACE;
        }

        @Override
        public byte[] encode() {
            if (userInterface == PARTIAL && (minTemp == null || maxTemp == null)) {
                throw new IllegalArgumentException(
                        "When userInterface==%s then you need to pass minTemp and maxTemp".formatted(PARTIAL));
            }
            return DeviceConfigDisableUserInterfaceEncoder.INSTANCE.encode(new DeviceConfigDisableUserInterface(
                    (short) userInterface.value, requireNonNullElse(minTemp, 0), requireNonNullElse(maxTemp, 0)));
        }

        public static DisableUserInterfaceConfig parse(List<String> parameters) {
            var ui = parseEnum(parameters.get(0), UserInterface.class);
            if (parameters.size() == 1) {
                return new DisableUserInterfaceConfig(ui, null, null);
            }
            return new DisableUserInterfaceConfig(ui, parseInt(parameters.get(1)), parseInt(parameters.get(2)));
        }

        @RequiredArgsConstructor
        public enum UserInterface {
            DISABLED(1),
            ENABLED(0),
            PARTIAL(2);
            private final int value;
        }
    }

    public record ButtonVolumeConfig(@Min(0) @Max(100) int volume) implements DeviceConfig {
        @Override
        public DeviceConfigField field() {
            return BUTTON_VOLUME;
        }

        @Override
        public byte[] encode() {
            return DeviceConfigButtonVolumeEncoder.INSTANCE.encode(
                    new DeviceConfigButtonVolume((short) clampPercentage(volume)));
        }

        public static ButtonVolumeConfig parse(List<String> parameters) {
            return new ButtonVolumeConfig(parseInt(parameters.get(0)));
        }
    }

    public record ScreenBrightnessConfig(
            @Min(0) @Max(100) int screenBrightness, boolean automatic, int adjustmentForAutomatic)
            implements DeviceConfig {
        @Override
        public DeviceConfigField field() {
            return SCREEN_BRIGHTNESS;
        }

        @Override
        public byte[] encode() {
            return DeviceConfigScreenBrightnessEncoder.INSTANCE.encode(new DeviceConfigScreenBrightness(
                    (short) clampPercentage(screenBrightness), encodeBoolean(automatic), (byte)
                            adjustmentForAutomatic));
        }

        public static ScreenBrightnessConfig parse(List<String> parameters) {
            return new ScreenBrightnessConfig(
                    parseInt(parameters.get(0)), parseBoolean(parameters.get(1)), parseInt(parameters.get(2)));
        }
    }

    public record StatusLedConfig(@NonNull StatusLed statusLed) implements DeviceConfig {
        @Override
        public DeviceConfigField field() {
            return STATUS_LED;
        }

        public static StatusLedConfig parse(List<String> parameters) {
            return new StatusLedConfig(parseEnum(parameters.get(0), StatusLed.class));
        }

        @Override
        public byte[] encode() {
            return DeviceConfigStatusLedEncoder.INSTANCE.encode(new DeviceConfigStatusLed((short) statusLed.value));
        }

        @RequiredArgsConstructor
        public static enum StatusLed {
            ON_WHEN_CONNECTED(SUPLA_DEVCFG_STATUS_LED_ON_WHEN_CONNECTED),
            OFF_WHEN_CONNECTED(SUPLA_DEVCFG_STATUS_LED_OFF_WHEN_CONNECTED),
            ALWAYS_OFF(SUPLA_DEVCFG_STATUS_LED_ALWAYS_OFF);
            private final int value;
        }
    }

    private static int clampPercentage(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static short encodeBoolean(boolean value) {
        return (short) (value ? 1 : 0);
    }
}
