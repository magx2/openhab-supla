package pl.grzeslowski.openhab.supla.internal.server.handler.device_config;

import static java.util.Objects.requireNonNullElse;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;
import static pl.grzeslowski.openhab.supla.internal.server.handler.device_config.DeviceConfig.DisableUserInterfaceConfig.UserInterface.PARTIAL;
import static pl.grzeslowski.openhab.supla.internal.server.handler.device_config.DeviceConfigField.*;
import static pl.grzeslowski.openhab.supla.internal.server.handler.device_config.DeviceConfigUtil.*;

import jakarta.annotation.Nullable;
import java.math.BigInteger;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.eclipse.jdt.annotation.NonNullByDefault;
import pl.grzeslowski.jsupla.protocol.api.encoders.*;
import pl.grzeslowski.jsupla.protocol.api.structs.*;

@NonNullByDefault
public sealed interface DeviceConfig extends Comparable<DeviceConfig> {
    DeviceConfigField getField();

    byte[] encode();

    @Override
    default int compareTo(DeviceConfig o) {
        return Comparator.comparing(DeviceConfig::getField).compare(this, o);
    }

    @Value
    public class PowerStatusLedConfig implements DeviceConfig {
        DeviceConfigField field = POWER_STATUS_LED;
        boolean disabled;

        @Override
        public byte[] encode() {
            return PowerStatusLedEncoder.INSTANCE.encode(new PowerStatusLed(encodeBoolean(disabled)));
        }

        public static PowerStatusLedConfig parse(List<String> parameters) {
            return new PowerStatusLedConfig(parseBoolean(parameters.get(0)));
        }
    }

    @Value
    public class HomeScreenOffDelayTypeConfig implements DeviceConfig {
        DeviceConfigField field = HOME_SCREEN_OFF_DELAY_TYPE;

        @NonNull
        DelayType delayType;

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

    @Value
    public class HomeScreenContentConfig implements DeviceConfig {
        DeviceConfigField field = HOME_SCREEN_CONTENT;

        @NonNull
        List<HomeScreenContent> contents;

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

    @Value
    public class HomeScreenOffDelayConfig implements DeviceConfig {
        DeviceConfigField field = HOME_SCREEN_OFF_DELAY;
        boolean enabled;

        @NonNull
        Duration duration;

        @Override
        public byte[] encode() {
            return DeviceConfigHomeScreenOffDelayEncoder.INSTANCE.encode(
                    new DeviceConfigHomeScreenOffDelay(enabled ? (short) duration.toSeconds() : 0));
        }

        public static HomeScreenOffDelayConfig parse(List<String> parameters) {
            return new HomeScreenOffDelayConfig(parseBoolean(parameters.get(0)), parseDuration(parameters.get(1)));
        }
    }

    @Value
    public class AutomaticTimeSyncConfig implements DeviceConfig {
        DeviceConfigField field = AUTOMATIC_TIME_SYNC;
        boolean enabled;

        @Override
        public byte[] encode() {
            return DeviceConfigAutomaticTimeSyncEncoder.INSTANCE.encode(
                    new DeviceConfigAutomaticTimeSync(encodeBoolean(enabled)));
        }

        public static AutomaticTimeSyncConfig parse(List<String> parameters) {
            return new AutomaticTimeSyncConfig(parseBoolean(parameters.get(0)));
        }
    }

    @Value
    public class DisableUserInterfaceConfig implements DeviceConfig {
        DeviceConfigField field = DISABLE_USER_INTERFACE;

        @NonNull
        UserInterface userInterface;

        @Nullable
        Integer minTemp;

        @Nullable
        Integer maxTemp;

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

    @Value
    public class ButtonVolumeConfig implements DeviceConfig {
        DeviceConfigField field = BUTTON_VOLUME;

        @Min(0)
        @Max(100)
        int volume;

        @Override
        public byte[] encode() {
            return DeviceConfigButtonVolumeEncoder.INSTANCE.encode(
                    new DeviceConfigButtonVolume((short) clampPercentage(volume)));
        }

        public static ButtonVolumeConfig parse(List<String> parameters) {
            return new ButtonVolumeConfig(parseInt(parameters.get(0)));
        }
    }

    @Value
    public class ScreenBrightnessConfig implements DeviceConfig {
        DeviceConfigField field = SCREEN_BRIGHTNESS;

        @Min(0)
        @Max(100)
        int screenBrightness;

        boolean automatic;
        int adjustmentForAutomatic;

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

    @Value
    public class StatusLedConfig implements DeviceConfig {
        DeviceConfigField field = STATUS_LED;

        @NonNull
        StatusLed statusLed;

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
