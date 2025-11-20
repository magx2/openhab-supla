package pl.grzeslowski.openhab.supla.internal.server.device_config;

import static java.util.Arrays.stream;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceConfigField {
    STATUS_LED(SUPLA_DEVICE_CONFIG_FIELD_STATUS_LED),
    SCREEN_BRIGHTNESS(SUPLA_DEVICE_CONFIG_FIELD_SCREEN_BRIGHTNESS),
    BUTTON_VOLUME(SUPLA_DEVICE_CONFIG_FIELD_BUTTON_VOLUME),
    DISABLE_USER_INTERFACE(SUPLA_DEVICE_CONFIG_FIELD_DISABLE_USER_INTERFACE),
    AUTOMATIC_TIME_SYNC(SUPLA_DEVICE_CONFIG_FIELD_AUTOMATIC_TIME_SYNC),
    HOME_SCREEN_OFF_DELAY(SUPLA_DEVICE_CONFIG_FIELD_HOME_SCREEN_OFF_DELAY),
    HOME_SCREEN_CONTENT(SUPLA_DEVICE_CONFIG_FIELD_HOME_SCREEN_CONTENT),
    HOME_SCREEN_OFF_DELAY_TYPE(SUPLA_DEVICE_CONFIG_FIELD_HOME_SCREEN_OFF_DELAY_TYPE),
    POWER_STATUS_LED(SUPLA_DEVICE_CONFIG_FIELD_POWER_STATUS_LED);

    private final long mask;

    public boolean hasField(long field) {
        return (field & mask) != 0;
    }

    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static List<DeviceConfigField> hasFields(long field) {
        return stream(values()).filter(value -> value.hasField(field)).toList();
    }
}
