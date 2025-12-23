package pl.grzeslowski.openhab.supla.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatus.UNKNOWN;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.*;

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
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import pl.grzeslowski.jsupla.protocol.api.structs.SuplaTimeval;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaPingServerResult;
import pl.grzeslowski.openhab.supla.internal.device.ZamelRow01;
import pl.grzeslowski.openhab.supla.internal.extension.random.LocationPassword;
import pl.grzeslowski.openhab.supla.internal.extension.random.Port;
import pl.grzeslowski.openhab.supla.internal.extension.random.RandomExtension;
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
        var configuration = new Configuration(Map.of(
                "guid", guid,
                "serverAccessId", serverAccessId,
                "serverAccessIdPassword", serverAccessIdPassword));
        deviceCtx.thing().setConfiguration(configuration);
        deviceCtx.thing().setBridgeUID(serverCtx.thing().getUID());
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
