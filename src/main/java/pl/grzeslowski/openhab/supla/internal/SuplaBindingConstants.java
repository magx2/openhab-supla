package pl.grzeslowski.openhab.supla.internal;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import pl.grzeslowski.jsupla.server.NettyConfig;

/** The {@link SuplaBindingConstants} class defines common constants, which are used across the whole binding. */
@NonNullByDefault
public class SuplaBindingConstants {

    public static final String BINDING_ID = "supla";
    public static final int DEVICE_REGISTER_MAX_DELAY = (int) MINUTES.toSeconds(1);

    // List of all Thing Type UIDs
    public static final ThingTypeUID SUPLA_SERVER_DEVICE_TYPE = new ThingTypeUID(BINDING_ID, "server-device");
    public static final ThingTypeUID SUPLA_SERVER_TYPE = new ThingTypeUID(BINDING_ID, "server-bridge");
    public static final ThingTypeUID SUPLA_CLOUD_DEVICE_TYPE = new ThingTypeUID(BINDING_ID, "cloud-device");
    public static final ThingTypeUID SUPLA_CLOUD_SERVER_TYPE = new ThingTypeUID(BINDING_ID, "cloud-bridge");
    public static final ThingTypeUID SUPLA_GATEWAY_DEVICE_TYPE = new ThingTypeUID(BINDING_ID, "gateway-device");
    public static final ThingTypeUID SUPLA_SUB_DEVICE_TYPE = new ThingTypeUID(BINDING_ID, "sub-device-device");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(
            SUPLA_SERVER_DEVICE_TYPE,
            SUPLA_SERVER_TYPE,
            SUPLA_CLOUD_DEVICE_TYPE,
            SUPLA_CLOUD_SERVER_TYPE,
            SUPLA_GATEWAY_DEVICE_TYPE,
            SUPLA_SUB_DEVICE_TYPE);

    // supla device and cloud-device
    public static final String SUPLA_DEVICE_GUID = "guid";
    public static final String SUPLA_DEVICE_CLOUD_ID = "cloud-id";
    public static final String THREAD_POOL_NAME = "supla-cloud-thread-pool";

    // SuplaServer constants
    public static final int DEFAULT_PORT = NettyConfig.SUPLA_HTTPS_PORT;
    public static final String CONFIG_SERVER_ACCESS_ID = "serverAccessId";
    public static final String CONFIG_SERVER_ACCESS_ID_PASSWORD = "serverAccessIdPassword";
    public static final String CONFIG_EMAIL = "email";
    public static final String CONFIG_PORT = "port";
    public static final String CONNECTED_DEVICES_CHANNEL_ID = "server-devices";

    // Thing Bridge constants
    public static final String GATEWAY_CONNECTED_DEVICES_CHANNEL_ID = "gateway-connected-devices";

    // sub device
    public static final String SUPLA_SUB_DEVICE_ID = "id";

    public static class ServerDevicesProperties {
        public static final String SOFT_VERSION_PROPERTY = "softVersion";
        public static final String MANUFACTURER_ID_PROPERTY = "manufacturerId";
        public static final String PRODUCT_ID_PROPERTY = "productId";
        public static final String PRODUCT_CODE_PROPERTY = "productCode";
        public static final String CONFIG_AUTH_PROPERTY = "authKey";
        public static final String SERVER_NAME_PROPERTY = "serverName";
        public static final String SERIAL_NUMBER_PROPERTY = "serialNumber";
    }

    // CloudBridgeHandler
    public static class CloudBridgeHandlerConstants {
        public static final String ADDRESS_CHANNEL_ID = "address";
        public static final String API_VERSION_CHANNEL_ID = "api-version";
        public static final String CLOUD_VERSION_CHANNEL_ID = "cloud-version";
        public static final String API_CALLS_IDH_CHANNEL_ID = "api-calls";
        public static final String REMAINING_API_CALLS_CHANNEL_ID = "remaining-api-calls";
        public static final String API_CALLS_ID_PERCENTAGE_CHANNEL_ID = "api-calls-percentage";
        public static final String REMAINING_API_CALLS_ID_PERCENTAGE_CHANNEL_ID = "remaining-api-calls-percentage";
        public static final String RATE_LIMIT_MAX_CHANNEL_ID = "rate-limit-max";
        public static final String RATE_LIMIT_RESET_DATE_TIME_CHANNEL_ID = "rate-limit-reset-date-time";
        public static final String REQ_PER_S_CHANNEL_ID = "req-per-s";
        public static final String REQ_PER_M_CHANNEL_ID = "req-per-m";
        public static final String REQ_PER_H_CHANNEL_ID = "req-per-h";
    }

    public static class Channels {
        public static final String LIGHT_CHANNEL_ID = "light-channel";
        public static final String SWITCH_CHANNEL_ID = "switch-channel";
        public static final String SWITCH_CHANNEL_RO_ID = "switch-channel-ro";
        public static final String FLAG_CHANNEL_ID = "flag-channel";
        public static final String DECIMAL_CHANNEL_ID = "decimal-channel";
        public static final String ENERGY_CHANNEL_ID = "energy-channel";
        public static final String POWER_CHANNEL_ID = "power-channel";
        public static final String VOLTAGE_CHANNEL_ID = "voltage-channel";
        public static final String CURRENT_CHANNEL_ID = "current-channel";
        public static final String FREQUENCY_CHANNEL_ID = "frequency-channel";
        public static final String RGB_CHANNEL_ID = "rgb-channel";
        public static final String ROLLER_SHUTTER_CHANNEL_ID = "roller-shutter-channel";
        public static final String TEMPERATURE_CHANNEL_ID = "temperature-channel";
        public static final String HUMIDITY_CHANNEL_ID = "humidity-channel";
        public static final String DIMMER_CHANNEL_ID = "dimmer-channel";
        public static final String TOGGLE_GAT_CHANNEL_ID = "toggle-gate-channel";
        public static final String STRING_CHANNEL_ID = "string-channel";
        public static final String UNKNOWN_CHANNEL_ID = "unknown-channel";
        // hvac
        public static final String HVAC_WORKING_CHANNEL_ID = "hvac-working";
        public static final String HVAC_MODE_CHANNEL_ID = "hvac-mode";
        public static final String HVAC_TEMPERATURE_HEAT_CHANNEL_ID = "hvac-temperature-heat";
        public static final String HVAC_TEMPERATURE_COOL_CHANNEL_ID = "hvac-temperature-cool";
    }

    public static class ChannelIds {
        public static class Hvac {
            public static final String HVAC_ON = "on";
            public static final String HVAC_MODE = "mode";
            public static final String HVAC_SET_POINT_TEMPERATURE_HEAT = "setPointTemperatureHeat";
            public static final String HVAC_SET_POINT_TEMPERATURE_COOL = "setPointTemperatureCool";
        }
    }
}
