package pl.grzeslowski.openhab.supla.internal.server.device_config;

import static java.util.Arrays.stream;
import static java.util.Map.entry;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;
import static pl.grzeslowski.openhab.supla.internal.Documentation.SET_DEVICE_CONFIG;
import static pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigField.*;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.javatuples.Pair;
import pl.grzeslowski.jsupla.protocol.api.decoders.*;

@UtilityClass
@NonNullByDefault
public class DeviceConfigUtil {
    public static final String PREFIX = "DEVICE_CONFIG_";
    public static final String SPLIT_CHAR = ":";

    public static Map<String, String> buildDeviceConfig(long fields, byte[] config) {
        var configMap = hasFields(fields).stream()
                .reduce(
                        Pair.with(0, new HashMap<String, String>()),
                        (pair, field) -> {
                            var result = decode(field, config, pair.getValue0());
                            pair.getValue1().putAll(result.configMap);
                            return new Pair<>(pair.getValue0() + result.usedBytes, pair.getValue1());
                        },
                        (a, b) -> Pair.with(a.getValue0() + b.getValue0(), mergeMaps(a.getValue1(), b.getValue1())))
                .getValue1();
        return configMap.entrySet().stream()
                .map(entry -> entry(PREFIX + entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static DeviceConfig parseDeviceConfig(String string) {
        var split = string.split(SPLIT_CHAR, 2);
        if (split.length != 2) {
            throw new IllegalArgumentException(
                    "Cannot parse `%s`! It should have at least one `%s`".formatted(string, SPLIT_CHAR));
        }

        var className = split[0];
        var parameters = split[1];

        Function<List<String>, ? extends DeviceConfig> parse =
                switch (className) {
                    case "PowerStatusLedConfig" -> DeviceConfig.PowerStatusLedConfig::parse;
                    case "HomeScreenOffDelayTypeConfig" -> DeviceConfig.HomeScreenOffDelayTypeConfig::parse;
                    case "HomeScreenContentConfig" -> DeviceConfig.HomeScreenContentConfig::parse;
                    case "HomeScreenOffDelayConfig" -> DeviceConfig.HomeScreenOffDelayConfig::parse;
                    case "AutomaticTimeSyncConfig" -> DeviceConfig.AutomaticTimeSyncConfig::parse;
                    case "DisableUserInterfaceConfig" -> DeviceConfig.DisableUserInterfaceConfig::parse;
                    case "ButtonVolumeConfig" -> DeviceConfig.ButtonVolumeConfig::parse;
                    case "ScreenBrightnessConfig" -> DeviceConfig.ScreenBrightnessConfig::parse;
                    case "StatusLedConfig" -> DeviceConfig.StatusLedConfig::parse;
                    default -> throw new IllegalArgumentException(
                            "There is not device config with name %s. Visit %s to get more info how to set device config."
                                    .formatted(className, SET_DEVICE_CONFIG));
                };
        return parse.apply(parseParams(parameters));
    }

    private static List<String> parseParams(String parameters) {
        return stream(parameters.split(",")).map(String::trim).toList();
    }

    public static <T extends Enum<T>> T parseEnum(String parameter, Class<T> enumClass) {
        try {
            return valueOf(enumClass, parameter);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(
                    "Cannot parse enum %s from string %s! ".formatted(enumClass.getSimpleName(), parameter)
                            + "Visit %s to get more info how to set device config.".formatted(SET_DEVICE_CONFIG),
                    ex);
        }
    }

    public static boolean parseBoolean(String parameter) {
        if (!parameter.equals(String.valueOf(true)) && !parameter.equals(String.valueOf(false))) {
            throw new IllegalArgumentException("Cannot parse boolean from `%s`! ".formatted(parameter)
                    + "Visit %s to get more info how to set device config.".formatted(SET_DEVICE_CONFIG));
        }
        return Boolean.parseBoolean(parameter);
    }

    public static Duration parseDuration(String parameter) {
        try {
            return Duration.parse(parameter);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Cannot parse duration from `%s`! ".formatted(parameter)
                            + "Visit %s to get more info how to set device config. ".formatted(SET_DEVICE_CONFIG)
                            + ex.getLocalizedMessage(),
                    ex);
        }
    }

    public static int parseInt(String parameter) {
        try {
            return Integer.parseInt(parameter);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Cannot parse int from `%s`! ".formatted(parameter)
                            + "Visit %s to get more info how to set device config. ".formatted(SET_DEVICE_CONFIG)
                            + ex.getLocalizedMessage(),
                    ex);
        }
    }

    private static HashMap<String, String> mergeMaps(HashMap<String, String> value1, HashMap<String, String> value2) {
        var map = new HashMap<String, String>();
        map.putAll(value1);
        map.putAll(value2);
        return map;
    }

    private DecodeResult decode(DeviceConfigField field, byte[] config, int offset) {
        return switch (field) {
            case STATUS_LED -> {
                var decode = DeviceConfigStatusLedDecoder.INSTANCE.decode(config, offset);
                var value =
                        switch (decode.statusLedType()) {
                            case SUPLA_DEVCFG_STATUS_LED_ON_WHEN_CONNECTED -> "ON_WHEN_CONNECTED";
                            case SUPLA_DEVCFG_STATUS_LED_OFF_WHEN_CONNECTED -> "OFF_WHEN_CONNECTED";
                            case SUPLA_DEVCFG_STATUS_LED_ALWAYS_OFF -> "ALWAYS_OFF";
                            default -> "UNKNOWN";
                        };
                yield new DecodeResult(Map.of(STATUS_LED.name(), value), decode.protoSize());
            }
            case SCREEN_BRIGHTNESS -> {
                var values = new HashMap<String, String>();
                var decode = DeviceConfigScreenBrightnessDecoder.INSTANCE.decode(config, offset);
                values.put(SCREEN_BRIGHTNESS.name(), decode.screenBrightness() + "%");
                values.put(SCREEN_BRIGHTNESS.name() + "_AUTOMATIC", Boolean.toString(decode.automatic() == 0));
                values.put(
                        SCREEN_BRIGHTNESS.name() + "_ADJUSTMENT_FOR_AUTOMATIC",
                        String.valueOf(decode.adjustmentForAutomatic()));
                yield new DecodeResult(values, decode.protoSize());
            }
            case BUTTON_VOLUME -> {
                var values = new HashMap<String, String>();
                var decode = DeviceConfigButtonVolumeDecoder.INSTANCE.decode(config, offset);
                values.put(BUTTON_VOLUME.name(), decode.volume() + "%");
                yield new DecodeResult(values, decode.protoSize());
            }
            case DISABLE_USER_INTERFACE -> {
                var values = new HashMap<String, String>();
                var decode = DeviceConfigDisableUserInterfaceDecoder.INSTANCE.decode(config, offset);
                values.put(
                        DISABLE_USER_INTERFACE.name(),
                        switch (decode.disableUserInterface()) {
                            case 0 -> Boolean.toString(false);
                            case 1 -> Boolean.toString(true);
                            case 2 -> "partial";
                            default -> "UNKNOWN";
                        });
                values.put(
                        DISABLE_USER_INTERFACE.name() + "_MIN_ALLOWED_TEMPERATURE_SET_POINT_FROM_LOCAL_UI",
                        String.valueOf(decode.minAllowedTemperatureSetpointFromLocalUI()));
                values.put(
                        DISABLE_USER_INTERFACE.name() + "_MAX_ALLOWED_TEMPERATURE_SET_POINT_FROM_LOCAL_UI",
                        String.valueOf(decode.maxAllowedTemperatureSetpointFromLocalUI()));

                yield new DecodeResult(values, decode.protoSize());
            }
            case AUTOMATIC_TIME_SYNC -> {
                var values = new HashMap<String, String>();
                var decode = DeviceConfigAutomaticTimeSyncDecoder.INSTANCE.decode(config, offset);
                values.put(AUTOMATIC_TIME_SYNC.name(), decode.automaticTimeSync() == 0 ? "disabled" : "enabled");
                yield new DecodeResult(values, decode.protoSize());
            }
            case HOME_SCREEN_OFF_DELAY -> {
                var values = new HashMap<String, String>();
                var decode = DeviceConfigHomeScreenOffDelayDecoder.INSTANCE.decode(config, offset);
                values.put(
                        HOME_SCREEN_OFF_DELAY.name(),
                        decode.homeScreenOffDelayS() == 0
                                ? "disabled"
                                : Duration.ofSeconds(decode.homeScreenOffDelayS())
                                        .toString());
                yield new DecodeResult(values, decode.protoSize());
            }
            case HOME_SCREEN_CONTENT -> {
                var values = new HashMap<String, String>();
                var decode = DeviceConfigHomeScreenContentDecoder.INSTANCE.decode(config, offset);
                var homeScreenContent = decode.homeScreenContent().longValue();
                values.put(
                        HOME_SCREEN_CONTENT.name() + "_NONE",
                        Boolean.toString((homeScreenContent & SUPLA_DEVCFG_HOME_SCREEN_CONTENT_NONE) != 0));
                values.put(
                        HOME_SCREEN_CONTENT.name() + "_TEMPERATURE",
                        Boolean.toString((homeScreenContent & SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TEMPERATURE) != 0));
                values.put(
                        HOME_SCREEN_CONTENT.name() + "_TEMPERATURE_AND_HUMIDITY",
                        Boolean.toString(
                                (homeScreenContent & SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TEMPERATURE_AND_HUMIDITY) != 0));
                values.put(
                        HOME_SCREEN_CONTENT.name() + "_TIME",
                        Boolean.toString((homeScreenContent & SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TIME) != 0));
                values.put(
                        HOME_SCREEN_CONTENT.name() + "_TIME_DATE",
                        Boolean.toString((homeScreenContent & SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TIME_DATE) != 0));
                values.put(
                        HOME_SCREEN_CONTENT.name() + "_TEMPERATURE_TIME",
                        Boolean.toString((homeScreenContent & SUPLA_DEVCFG_HOME_SCREEN_CONTENT_TEMPERATURE_TIME) != 0));
                values.put(
                        HOME_SCREEN_CONTENT.name() + "_MAIN_AND_AUX_TEMPERATURE",
                        Boolean.toString(
                                (homeScreenContent & SUPLA_DEVCFG_HOME_SCREEN_CONTENT_MAIN_AND_AUX_TEMPERATURE) != 0));
                values.put(
                        HOME_SCREEN_CONTENT.name() + "_MODE_OR_TEMPERATURE",
                        Boolean.toString(
                                (homeScreenContent & SUPLA_DEVCFG_HOME_SCREEN_CONTENT_MODE_OR_TEMPERATURE) != 0));
                yield new DecodeResult(values, decode.protoSize());
            }
            case HOME_SCREEN_OFF_DELAY_TYPE -> {
                var values = new HashMap<String, String>();
                var decode = HomeScreenOffDelayTypeDecoder.INSTANCE.decode(config, offset);
                values.put(
                        HOME_SCREEN_OFF_DELAY_TYPE.name(),
                        switch (decode.homeScreenOffDelayType()) {
                            case SUPLA_DEVCFG_HOME_SCREEN_OFF_DELAY_TYPE_ALWAYS_ENABLED -> "ALWAYS_ENABLED";
                            case SUPLA_DEVCFG_HOME_SCREEN_OFF_DELAY_TYPE_ENABLED_WHEN_DARK -> "ENABLED_WHEN_DARK";
                            default -> "UNKNOWN";
                        });
                yield new DecodeResult(values, decode.protoSize());
            }
            case POWER_STATUS_LED -> {
                var values = new HashMap<String, String>();
                var decode = PowerStatusLedDecoder.INSTANCE.decode(config, offset);
                values.put(POWER_STATUS_LED.name(), decode.disabled() == 0 ? "disabled" : "enabled");
                yield new DecodeResult(values, decode.protoSize());
            }
        };
    }

    private static record DecodeResult(Map<String, String> configMap, int usedBytes) {}
}
