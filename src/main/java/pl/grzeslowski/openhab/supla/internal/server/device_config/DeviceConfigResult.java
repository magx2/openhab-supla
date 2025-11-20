package pl.grzeslowski.openhab.supla.internal.server.device_config;

import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceConfigResult {
    FALSE(false),
    TRUE(true),
    DATA_ERROR(false),
    TYPE_NOT_SUPPORTED(false),
    FUNCTION_NOT_SUPPORTED(false),
    LOCAL_CONFIG_DISABLED(false),
    NOT_ALLOWED(false),
    DEVICE_NOT_FOUND(false),
    UNKNOWN(false);

    final boolean success;

    public static DeviceConfigResult findConfigResult(int value) {
        return switch (value) {
            case SUPLA_CONFIG_RESULT_FALSE -> FALSE;
            case SUPLA_CONFIG_RESULT_TRUE -> TRUE;
            case SUPLA_CONFIG_RESULT_DATA_ERROR -> DATA_ERROR;
            case SUPLA_CONFIG_RESULT_TYPE_NOT_SUPPORTED -> TYPE_NOT_SUPPORTED;
            case SUPLA_CONFIG_RESULT_FUNCTION_NOT_SUPPORTED -> FUNCTION_NOT_SUPPORTED;
            case SUPLA_CONFIG_RESULT_LOCAL_CONFIG_DISABLED -> LOCAL_CONFIG_DISABLED;
            case SUPLA_CONFIG_RESULT_NOT_ALLOWED -> NOT_ALLOWED;
            case SUPLA_CONFIG_RESULT_DEVICE_NOT_FOUND -> DEVICE_NOT_FOUND;
            default -> UNKNOWN;
        };
    }
}
