package pl.grzeslowski.openhab.supla.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatus.UNKNOWN;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_DEVICE_TYPE_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_TYPE_ID;
import static pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension.deviceInitialize;
import static pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension.serverInitialize;
import static tech.units.indriya.unit.Units.CELSIUS;

import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import java.math.BigDecimal;
import javax.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.openhab.supla.internal.device.ZamelDiw01;
import pl.grzeslowski.openhab.supla.internal.device.ZamelGkw02;
import pl.grzeslowski.openhab.supla.internal.device.ZamelRow01;
import pl.grzeslowski.openhab.supla.internal.extension.random.*;
import pl.grzeslowski.openhab.supla.internal.extension.supla.CreateHandler;
import pl.grzeslowski.openhab.supla.internal.extension.supla.Ctx.BridgeCtx;
import pl.grzeslowski.openhab.supla.internal.extension.supla.Ctx.ThingCtx;
import pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension;

@Slf4j
@ExtendWith({MockitoExtension.class, RandomExtension.class, RandomBeansExtension.class, SuplaExtension.class})
public class MainIT {
    @Test
    @DisplayName("should run tests for Zamel ROW-01")
    void zamelRow01(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Random @Min(1) int serverAccessId,
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
            assertThat(ping.tvSec()).isGreaterThan(0);
            assertThat(ping.tvUsec()).isEqualTo(0);
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
            assertThat(ping.tvSec()).isGreaterThan(0);
            assertThat(ping.tvUsec()).isEqualTo(0);
            await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                    .isEqualTo(ThingStatusInfoBuilder.create(ONLINE, NONE).build()));

            var channels = deviceCtx.openHabDevice().getChannelStates();
            assertThat(channels).hasSize(18);

            // Check temperature state
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
                var newTemperature = BigDecimal.valueOf(100.0);
                deviceCtx.handler().handleCommand(channel, new QuantityType<>(newTemperature, CELSIUS));
                device.updateChannel();
                assertThat(device.getHvac().getSetPointTemperatureHeat()).isEqualTo(newTemperature);
            }
            { // OH updates HVAC / mode
                var channel = new ChannelUID("supla:server-device:%s:0#mode".formatted(guid));
                var newMode = "OFF";
                deviceCtx.handler().handleCommand(channel, StringType.valueOf(newMode));
                device.updateChannel();
                assertThat(device.getHvac().getMode()).isEqualTo(HvacValue.Mode.valueOf(newMode));
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
            assertThat(ping.tvSec()).isGreaterThan(0);
            assertThat(ping.tvUsec()).isEqualTo(0);
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
                    assertThat(channelState).isEqualTo(new PercentType(currentState.value()));
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
}
