package pl.grzeslowski.openhab.supla.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatus.UNKNOWN;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static org.openhab.core.types.UnDefType.UNDEF;
import static pl.grzeslowski.jsupla.protocol.api.HvacMode.SUPLA_HVAC_MODE_OFF;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_DEVICE_TYPE_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_TYPE_ID;
import static pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension.deviceInitialize;
import static pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension.serverInitialize;
import static tech.units.indriya.unit.Units.CELSIUS;
import static tech.units.indriya.unit.Units.PERCENT;

import io.github.glytching.junit.extension.random.RandomBeansExtension;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.util.ColorUtil;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.openhab.supla.internal.device.*;
import pl.grzeslowski.openhab.supla.internal.extension.random.*;
import pl.grzeslowski.openhab.supla.internal.extension.supla.CreateHandler;
import pl.grzeslowski.openhab.supla.internal.extension.supla.Ctx.BridgeCtx;
import pl.grzeslowski.openhab.supla.internal.extension.supla.Ctx.ThingCtx;
import pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

@Slf4j
@ExtendWith({MockitoExtension.class, RandomExtension.class, RandomBeansExtension.class, SuplaExtension.class})
public class MainIT {
    @Test
    @DisplayName("should run tests for Zamel ROW-01")
    void zamelRow01(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @LocationId int serverAccessId,
            @LocationPassword String serverAccessIdPassword)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        log.info(
                "Testing Zamel ROW-01 with GUID={}, "
                        + "using socket on port={}, "
                        + "locationId={}, "
                        + "locationPassword={}",
                guid,
                port,
                serverAccessId,
                serverAccessIdPassword);
        serverInitialize(serverCtx, port);
        deviceInitialize(deviceCtx, serverCtx, serverAccessId, serverAccessIdPassword, guid);
        // DEVICE
        try (var device = new ZamelRow01(guid, serverAccessId, serverAccessIdPassword)) {
            device.initialize("localhost", port);
            // register
            device.register();
            var registerResult = device.readRegisterDeviceResultA();
            assertThat(registerResult).isEqualTo(new SuplaRegisterDeviceResultA(3, (short) 100, (short) 5, (short) 1));
            assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(new ThingStatusInfo(
                            UNKNOWN, CONFIGURATION_PENDING, "@text/supla.offline.waiting-for-registration"));
            // ping
            device.sendPing();
            var ping = device.readPing().now();
            assertThat(ping).isNotNull();
            await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(ThingStatusInfoBuilder.create(ONLINE, NONE).build()));

            var channel = deviceCtx.openHabDevice().findChannel();
            { // device updates it's state with OH
                var previousState = device.isState();
                device.toggleSwitch();
                var currentState = device.isState();
                assertThat(currentState).isNotEqualTo(previousState);
                log.info("Waiting for OH to propagate state change");
                await().untilAsserted(() -> {
                    var channelState = deviceCtx.openHabDevice().findChannelState(channel);
                    assertThat(channelState).isEqualTo(OnOffType.from(currentState));
                });
            }
            {
                // OH updates it's state with the device
                var previousState = device.isState();
                deviceCtx.handler().handleCommand(channel, OnOffType.from(!previousState));
                device.updateChannel();
                assertThat(device.isState()).isNotEqualTo(previousState);
            }
        }
        // device is closed
        log.info("Waiting for the device to disconnect");
        await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                .isEqualTo(
                        new ThingStatusInfo(OFFLINE, COMMUNICATION_ERROR, "@text/supla.offline.channel-disconnected")));
    }

    @Test
    @DisplayName("should run tests for Zamel GKW-02")
    void zamelGkw02(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Email String email,
            @AuthKey String authKey)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        log.info(
                "Testing Zamel GKW-02 with GUID={}, " + "using socket on port={}, " + "email={}, " + "authKey={}",
                guid,
                port,
                email,
                authKey);
        serverInitialize(serverCtx, port);
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid);
        // DEVICE
        try (var device = new ZamelGkw02(guid, email, authKey)) {
            device.initialize("localhost", port);
            // register
            device.register();
            var registerResult = device.readRegisterDeviceResultA();
            assertThat(registerResult).isEqualTo(new SuplaRegisterDeviceResultA(3, (short) 100, (short) 23, (short) 1));
            assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(new ThingStatusInfo(
                            UNKNOWN, CONFIGURATION_PENDING, "@text/supla.offline.waiting-for-registration"));
            // ping
            device.sendPing();
            var ping = device.readPing().now();
            assertThat(ping).isNotNull();
            log.info("Waiting for handler to be online");
            await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(ThingStatusInfoBuilder.create(ONLINE, NONE).build()));

            var channels = deviceCtx.openHabDevice().getChannelStates();
            assertThat(channels).hasSize(20);

            // Check temperature state
            log.info("Waiting for temperature to be {} °C", device.getTemperature());
            await().untilAsserted(() -> {
                var temperatureState = deviceCtx.openHabDevice().findChannelState(1);
                assertThat(temperatureState).isEqualTo(new QuantityType<>(device.getTemperature(), CELSIUS));
            });

            { // device updates temperature with OH
                var oldTemperature = device.getTemperature();
                device.temperatureUpdated();
                var currentTemperature = device.getTemperature();
                assertThat(currentTemperature).isNotEqualTo(oldTemperature);
                log.info("Waiting for OH to propagate state change");
                await().untilAsserted(() -> {
                    var temperatureState = deviceCtx.openHabDevice().findChannelState(1);
                    assertThat(temperatureState).isEqualTo(new QuantityType<>(device.getTemperature(), CELSIUS));
                });
            }
            { // shortPress
                for (int channelNumber = 0; channelNumber < 3; channelNumber++) {
                    device.shortPress(channelNumber);
                    var trigger = deviceCtx.openHabDevice().popTrigger();
                    assertThat(trigger.channelUID().getId()).isEqualTo(String.valueOf(channelNumber + 2));
                    assertThat(trigger.event()).isEqualTo("SHORT_PRESS_x1");
                }
            }
            { // hold
                for (int channelNumber = 0; channelNumber < 3; channelNumber++) {
                    device.hold(channelNumber);
                    var trigger = deviceCtx.openHabDevice().popTrigger();
                    assertThat(trigger.channelUID().getId()).isEqualTo(String.valueOf(channelNumber + 2));
                    assertThat(trigger.event()).isEqualTo("HOLD");
                }
            }
            { // OH updates HVAC / setPointTemperatureHeat
                var channel = new ChannelUID("supla:server-device:%s:0#setPointTemperatureHeat".formatted(guid));
                var newTemperature = new BigDecimal("100.00");
                deviceCtx.handler().handleCommand(channel, new QuantityType<>(newTemperature, CELSIUS));
                device.updateChannel();
                assertThat(device.getHvac().getSetPointTemperatureHeat()).isEqualTo(newTemperature);
            }
            { // OH updates HVAC / mode
                var channel = new ChannelUID("supla:server-device:%s:0#mode".formatted(guid));
                var newMode = "OFF";
                deviceCtx.handler().handleCommand(channel, StringType.valueOf(newMode));
                device.updateChannel();
                assertThat(device.getHvac().getMode()).isEqualTo(SUPLA_HVAC_MODE_OFF);
            }
        }
        // device is closed
        log.info("Waiting for the device to disconnect");
        await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                .isEqualTo(
                        new ThingStatusInfo(OFFLINE, COMMUNICATION_ERROR, "@text/supla.offline.channel-disconnected")));
    }

    @Test
    @DisplayName("should run tests for Zamel DIW-01")
    void zamelDiw01(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Email String email,
            @AuthKey String authKey)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        log.info(
                "Testing Zamel DIW-01 with GUID={}, using socket on port={}, email={}, authKey={}",
                guid,
                port,
                email,
                authKey);
        serverInitialize(serverCtx, port);
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid);
        // DEVICE
        try (var device = new ZamelDiw01(guid, email, authKey)) {
            device.initialize("localhost", port);
            // register
            device.register();
            var registerResult = device.readRegisterDeviceResultA();
            assertThat(registerResult).isEqualTo(new SuplaRegisterDeviceResultA(3, (short) 100, (short) 12, (short) 1));
            assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(new ThingStatusInfo(
                            UNKNOWN, CONFIGURATION_PENDING, "@text/supla.offline.waiting-for-registration"));
            // ping
            device.sendPing();
            var ping = device.readPing().now();
            assertThat(ping).isNotNull();
            await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(ThingStatusInfoBuilder.create(ONLINE, NONE).build()));

            var channel = deviceCtx.openHabDevice().findChannel();
            { // device updates it's state with OH
                var previousState = device.getValue();
                device.dim();
                var currentState = device.getValue();
                assertThat(currentState).isNotEqualTo(previousState);
                log.info("Waiting for OH to propagate state change");
                await().untilAsserted(() -> {
                    var channelState = deviceCtx.openHabDevice().findChannelState(channel);
                    assertThat(channelState).isEqualTo(new QuantityType<>(currentState.value(), PERCENT));
                });
            }
            { // OH updates it's state with the device
                var previousState = device.getValue();
                deviceCtx
                        .handler()
                        .handleCommand(
                                channel,
                                new PercentType(RandomExtension.INSTANCE
                                        .randomPercentage(previousState)
                                        .value()));
                device.updateChannel();
                assertThat(device.getValue()).isNotEqualTo(previousState);
            }
        }
        // device is closed
        log.info("Waiting for the device to disconnect");
        await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                .isEqualTo(
                        new ThingStatusInfo(OFFLINE, COMMUNICATION_ERROR, "@text/supla.offline.channel-disconnected")));
    }

    @Test
    @DisplayName("should run tests for Zamel mSLW-01")
    void zamelMslw01(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Email String email,
            @AuthKey String authKey)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        log.info(
                "Testing Zamel mSLW-01 with GUID={}, using socket on port={}, email={}, authKey={}",
                guid,
                port,
                email,
                authKey);
        serverInitialize(serverCtx, port);
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid);
        // DEVICE
        try (var device = new ZamelMslw01(guid, email, authKey)) {
            device.initialize("localhost", port);
            // register
            device.register();
            var registerResult = device.readRegisterDeviceResultA();
            assertThat(registerResult).isEqualTo(new SuplaRegisterDeviceResultA(3, (short) 100, (short) 18, (short) 1));
            assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(new ThingStatusInfo(
                            UNKNOWN, CONFIGURATION_PENDING, "@text/supla.offline.waiting-for-registration"));
            // ping
            device.sendPing();
            var ping = device.readPing().now();
            assertThat(ping).isNotNull();
            await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(ThingStatusInfoBuilder.create(ONLINE, NONE).build()));

            var channels = deviceCtx.openHabDevice().getChannelStates();
            assertThat(channels).hasSize(2);

            { // device updates RGB
                var olgRgb = device.getRgbwValue();
                device.rgbUpdated();
                var currentRgb = device.getRgbwValue();
                assertThat(currentRgb).isNotEqualTo(olgRgb);
                log.info("Waiting for OH to propagate state change");
                await().untilAsserted(() -> {
                    var rgbwValue = device.getRgbwValue();
                    {
                        var rgbState = deviceCtx.openHabDevice().findChannelState("0", "rgbw_color");
                        var hsb = ColorUtil.rgbToHsb(new int[] {rgbwValue.red(), rgbwValue.green(), rgbwValue.blue()});
                        var expected = new HSBType(
                                hsb.getHue(), hsb.getSaturation(), new PercentType(rgbwValue.colorBrightness()));
                        assertThat(rgbState).isEqualTo(expected);
                    }
                    {
                        var brightnessState = deviceCtx.openHabDevice().findChannelState("0", "rgbw_brightness");
                        var expected = new PercentType(rgbwValue.brightness());
                        assertThat(brightnessState).isEqualTo(expected);
                    }
                });
            }
            { // OH updates it's state with the device
                { // OH Updates RGB color
                    var previousState = device.getRgbwValue();
                    var hsbType = RandomExtension.INSTANCE.randomHsb();
                    deviceCtx
                            .handler()
                            .handleCommand(
                                    new ChannelUID("supla:server-device:%s:0#rgbw_color".formatted(guid)), hsbType);
                    device.updateChannel();

                    var rgb = ColorUtil.hsbToRgb(hsbType);
                    assertThat(device.getRgbwValue().red()).isEqualTo(rgb[0]);
                    assertThat(device.getRgbwValue().green()).isEqualTo(rgb[1]);
                    assertThat(device.getRgbwValue().blue()).isEqualTo(rgb[2]);
                    assertThat(device.getRgbwValue().colorBrightness())
                            .isEqualTo(hsbType.getBrightness().intValue());

                    assertThat(device.getRgbwValue().brightness()).isEqualTo(previousState.brightness());
                }
                { // OH Updates brightness
                    var previousState = device.getRgbwValue();
                    var percentType = new PercentType(
                            RandomExtension.INSTANCE.randomPercentage().value());
                    deviceCtx
                            .handler()
                            .handleCommand(
                                    new ChannelUID("supla:server-device:%s:0#rgbw_brightness".formatted(guid)),
                                    percentType);
                    device.updateChannel();

                    assertThat(device.getRgbwValue().red()).isEqualTo(previousState.red());
                    assertThat(device.getRgbwValue().green()).isEqualTo(previousState.green());
                    assertThat(device.getRgbwValue().blue()).isEqualTo(previousState.blue());

                    assertThat(device.getRgbwValue().brightness()).isEqualTo(percentType.intValue());
                }
            }
            { // shortPress
                device.shortPress();
                var trigger = deviceCtx.openHabDevice().popTrigger();
                assertThat(trigger.channelUID().getId()).isEqualTo(String.valueOf(1));
                assertThat(trigger.event()).isEqualTo("SHORT_PRESS_x1");
            }
            { // hold
                device.hold();
                var trigger = deviceCtx.openHabDevice().popTrigger();
                assertThat(trigger.channelUID().getId()).isEqualTo(String.valueOf(1));
                assertThat(trigger.event()).isEqualTo("HOLD");
            }
        }
        // device is closed
        log.info("Waiting for the device to disconnect");
        await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                .isEqualTo(
                        new ThingStatusInfo(OFFLINE, COMMUNICATION_ERROR, "@text/supla.offline.channel-disconnected")));
    }

    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    @Test
    @DisplayName("should run tests for Zamel THW-01")
    void zamelThw01(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Email String email,
            @AuthKey String authKey)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        log.info(
                "Testing Zamel THW-01 with GUID={}, using socket on port={}, email={}, authKey={}",
                guid,
                port,
                email,
                authKey);
        serverInitialize(serverCtx, port);
        var timeout = new TimeoutConfiguration(3, 1, 4);
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid, timeout);
        // DEVICE
        try (var device = new ZamelThw01(guid, email, authKey)) {
            device.initialize("localhost", port);
            // register
            device.register();
            var registerResult = device.readRegisterDeviceResultA();
            assertThat(registerResult).isEqualTo(new SuplaRegisterDeviceResultA(3, (short) 100, (short) 18, (short) 1));
            assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(new ThingStatusInfo(
                            UNKNOWN, CONFIGURATION_PENDING, "@text/supla.offline.waiting-for-registration"));
            // ping
            device.sendPing();
            var ping = device.readPing().now();
            assertThat(ping).isNotNull();
            log.info("Waiting for handler to be online");
            await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(ThingStatusInfoBuilder.create(ONLINE, NONE).build()));

            var channels = deviceCtx.openHabDevice().getChannelStates();
            assertThat(channels).hasSize(6);

            { // Check temperature state
                log.info("Waiting for temperature to be {} °C", device.getTemperature());
                await().untilAsserted(() -> {
                    var temperatureState = deviceCtx.openHabDevice().findChannelState("0", "temperature");
                    assertThat(temperatureState).isEqualTo(new QuantityType<>(device.getTemperature(), CELSIUS));
                });
            }
            { // Check humidity state
                log.info("Waiting for humidity to be {}%", device.getHumidity());
                await().untilAsserted(() -> {
                    var humidityState = deviceCtx.openHabDevice().findChannelState("0", "humidity");
                    assertThat(humidityState).isEqualTo(new QuantityType<>(device.getHumidity(), PERCENT));
                });
            }

            { // device updates temperature & humidity with OH
                device.temperatureAndHumidityUpdated();
                log.info("Waiting for OH to propagate state change");
                await().untilAsserted(() -> {
                    var temperatureState = deviceCtx.openHabDevice().findChannelState("0", "temperature");
                    assertThat(temperatureState).isEqualTo(new QuantityType<>(device.getTemperature(), CELSIUS));
                });
                await().untilAsserted(() -> {
                    var humidityState = deviceCtx.openHabDevice().findChannelState("0", "humidity");
                    assertThat(humidityState).isEqualTo(new QuantityType<>(device.getHumidity(), PERCENT));
                });
            }

            { // device updates temperature & humidity with OH
                // but value validity time expires
                // which means channels should be UNDEF
                device.temperatureAndHumidityUpdated();
                log.info("Waiting for temperature channel to invalidate");
                await().timeout(device.getValidityTime().multipliedBy(2))
                        .pollInterval(timeout.min())
                        .untilAsserted(() -> {
                            device.ping();
                            var temperatureState = deviceCtx.openHabDevice().findChannelState("0", "temperature");
                            assertThat(temperatureState).isEqualTo(UNDEF);
                        });
                log.info("Waiting for humidity channel to invalidate");
                await().timeout(device.getValidityTime().multipliedBy(2))
                        .pollInterval(timeout.min())
                        .untilAsserted(() -> {
                            device.ping();
                            var humidityState = deviceCtx.openHabDevice().findChannelState("0", "humidity");
                            assertThat(humidityState).isEqualTo(UNDEF);
                        });
            }
        }
        // device is closed
        log.info("Waiting for the device to disconnect");
        await()
                // because this is a sleep device we need to wait longer for it to go offline
                .timeout(timeout.max().multipliedBy(2))
                .untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                        .isEqualTo(new ThingStatusInfo(
                                OFFLINE, COMMUNICATION_ERROR, "@text/supla.offline.channel-disconnected")));
    }
}
