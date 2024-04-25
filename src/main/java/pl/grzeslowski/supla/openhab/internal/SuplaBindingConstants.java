/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * <p>See the NOTICE file(s) distributed with this work for additional information.
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package pl.grzeslowski.supla.openhab.internal;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link SuplaBindingConstants} class defines common constants, which are used across the whole binding.
 *
 * @author Grzeslowski - Initial contribution
 */
@NonNullByDefault
public class SuplaBindingConstants {

    public static final String BINDING_ID = "supla";
    public static final int DEVICE_REGISTER_MAX_DELAY = (int) MINUTES.toSeconds(1);

    // List of all Thing Type UIDs
    public static final ThingTypeUID SUPLA_SERVER_DEVICE_TYPE = new ThingTypeUID(BINDING_ID, "server-device");
    public static final ThingTypeUID SUPLA_SERVER_TYPE = new ThingTypeUID(BINDING_ID, "server-bridge");
    public static final ThingTypeUID SUPLA_CLOUD_DEVICE_TYPE = new ThingTypeUID(BINDING_ID, "cloud-device");
    public static final ThingTypeUID SUPLA_CLOUD_SERVER_TYPE = new ThingTypeUID(BINDING_ID, "cloud-bridge");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS =
            Set.of(SUPLA_SERVER_DEVICE_TYPE, SUPLA_CLOUD_DEVICE_TYPE, SUPLA_SERVER_TYPE, SUPLA_CLOUD_SERVER_TYPE);

    // supla device and cloud-device
    public static final String SUPLA_DEVICE_GUID = "guid";
    public static final String SUPLA_DEVICE_CLOUD_ID = "cloud-id";
    public static final String THREAD_POOL_NAME = "supla-cloud-thread-pool";

    // SuplaServer constants
    public static final int DEFAULT_PORT = 2016;
    public static final String CONFIG_SERVER_ACCESS_ID = "serverAccessId";
    public static final String CONFIG_SERVER_ACCESS_ID_PASSWORD = "serverAccessIdPassword";
    public static final String CONFIG_EMAIL = "email";
    public static final String CONFIG_PORT = "port";
    public static final String CONNECTED_DEVICES_CHANNEL_ID = "server-devices";

    public static class ServerDevicesProperties {
        public static final String SOFT_VERSION_PROPERTY = "softVersion";
        public static final String CONFIG_AUTH_PROPERTY = "authKey";
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
    }
}
