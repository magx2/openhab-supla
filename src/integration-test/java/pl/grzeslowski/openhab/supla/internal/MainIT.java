package pl.grzeslowski.openhab.supla.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatus.UNKNOWN;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_DEVICE_TYPE_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_TYPE_ID;
import static tech.units.indriya.unit.Units.CELSIUS;

import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import java.util.Map;
import javax.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import pl.grzeslowski.jsupla.protocol.api.structs.SuplaTimeval;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaPingServerResult;
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
            assertThat(device.readPing()).isEqualTo(new SuplaPingServerResult(new SuplaTimeval(0, 0)));
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
        }
        // device is closed
        log.info("Waiting for the device to disconnect");
        await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                .isEqualTo(
                        new ThingStatusInfo(OFFLINE, COMMUNICATION_ERROR, "@text/supla.offline.channel-disconnected")));
    }

    private static void serverInitialize(BridgeCtx serverCtx, int port) {
        // configure server handler
        var configuration = new Configuration(
                Map.of("port", port, "ssl", false, "serverAccessId", 123, "serverAccessIdPassword", "none"));
        serverCtx.thing().setConfiguration(configuration);
        serverCtx.handler().initialize();
    }

    private static void deviceInitialize(
            ThingCtx deviceCtx, BridgeCtx serverCtx, int serverAccessId, String serverAccessIdPassword, String guid) {
        // configure device handler
        var configuration = Map.<String, Object>of(
                "guid", guid,
                "serverAccessId", serverAccessId,
                "serverAccessIdPassword", serverAccessIdPassword);
        deviceInitialize(deviceCtx, serverCtx, configuration, guid);
    }

    private static void deviceInitialize(
            ThingCtx deviceCtx, BridgeCtx serverCtx, String email, String authKey, String guid) {
        // configure device handler
        var configuration = Map.<String, Object>of(
                "guid", guid,
                "email", email,
                "authKey", authKey);
        deviceInitialize(deviceCtx, serverCtx, configuration, guid);
    }

    private static void deviceInitialize(
            ThingCtx deviceCtx, BridgeCtx serverCtx, Map<String, Object> configuration, String guid) {
        deviceCtx.thing().setConfiguration(new Configuration(configuration));
        deviceCtx.thing().setBridgeUID(serverCtx.thing().getUID());
        deviceCtx.openHabDevice().setGuid(guid);
        deviceCtx.openHabDevice().setBridge(serverCtx.thing());
        deviceCtx.openHabDevice().setThing(deviceCtx.thing());
        log.info("Initializing server device handler");
        deviceCtx.handler().initialize();
        serverCtx.handler().childHandlerInitialized(deviceCtx.handler(), deviceCtx.thing());
        assertThat(deviceCtx.openHabDevice().findThingStatus())
                .isEqualTo(new ThingStatusInfo(
                        UNKNOWN, HANDLER_CONFIGURATION_PENDING, "@text/supla.server.waiting-for-connection"));
    }
}
