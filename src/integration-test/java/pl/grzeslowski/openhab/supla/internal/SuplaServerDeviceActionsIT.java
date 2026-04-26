package pl.grzeslowski.openhab.supla.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatus.UNKNOWN;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_PENDING;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_ENTER_CFG_MODE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_RESET_COUNTERS;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_DEVICE_TYPE_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_TYPE_ID;
import static pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension.deviceInitialize;
import static pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension.serverInitialize;

import io.github.glytching.junit.extension.random.RandomBeansExtension;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.DeviceCalCfgResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.DeviceCalCfgRequest;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.openhab.supla.actions.SuplaServerDeviceActions;
import pl.grzeslowski.openhab.supla.internal.device.ZamelMew01;
import pl.grzeslowski.openhab.supla.internal.device.ZamelThw01;
import pl.grzeslowski.openhab.supla.internal.extension.random.AuthKey;
import pl.grzeslowski.openhab.supla.internal.extension.random.Email;
import pl.grzeslowski.openhab.supla.internal.extension.random.Port;
import pl.grzeslowski.openhab.supla.internal.extension.random.RandomExtension;
import pl.grzeslowski.openhab.supla.internal.extension.supla.CreateHandler;
import pl.grzeslowski.openhab.supla.internal.extension.supla.Ctx.BridgeCtx;
import pl.grzeslowski.openhab.supla.internal.extension.supla.Ctx.ThingCtx;
import pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension;
import pl.grzeslowski.openhab.supla.internal.server.handler.ServerSuplaDeviceHandler;

@ExtendWith({MockitoExtension.class, RandomExtension.class, RandomBeansExtension.class, SuplaExtension.class})
class SuplaServerDeviceActionsIT {
    @Test
    @DisplayName("should send reset electricity meter counters action to device")
    void resetElectricMeterCounters(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Email String email,
            @AuthKey String authKey)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        serverInitialize(serverCtx, port);
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid);

        try (var device = new ActionAwareMew01(guid, email, authKey)) {
            device.initialize("localhost", port);
            device.register();
            var registerResult = device.readRegisterDeviceResultA();
            assertThat(registerResult).isEqualTo(new SuplaRegisterDeviceResultA(3, (short) 100, (short) 10, (short) 1));
            awaitWaitingForRegistration(deviceCtx);

            device.sendPing();
            assertThat(device.readPing().now()).isNotNull();
            awaitOnline(deviceCtx);

            device.meterValueUpdatedCall100();
            await().untilAsserted(() ->
                    assertThat(deviceCtx.openHabDevice().getChannelStates()).hasSize(2));

            var actions = createActions(deviceCtx.handler());
            var action = CompletableFuture.runAsync(() -> {
                try {
                    actions.resetElectricMeterCounters(
                            "supla:server-device:%s:0#totalForwardActiveEnergy".formatted(guid));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            var request = device.readDeviceCalCfgRequest();
            assertThat(request.channelNumber()).isEqualTo(0);
            assertThat(request.command()).isEqualTo(SUPLA_CALCFG_CMD_RESET_COUNTERS);
            assertThat(request.superUserAuthorized()).isEqualTo((byte) 1);
            consumeDeviceCalCfgResult(deviceCtx.handler(), request, SUPLA_CALCFG_RESULT_DONE);
            action.get(30, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("should send enter config mode action to device")
    void enterConfigMode(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Email String email,
            @AuthKey String authKey)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        serverInitialize(serverCtx, port);
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid);

        try (var device = new ActionAwareThw01(guid, email, authKey)) {
            device.initialize("localhost", port);
            device.register();
            var registerResult = device.readRegisterDeviceResultA();
            assertThat(registerResult).isEqualTo(new SuplaRegisterDeviceResultA(3, (short) 100, (short) 18, (short) 1));
            awaitWaitingForRegistration(deviceCtx);

            device.sendPing();
            assertThat(device.readPing().now()).isNotNull();
            awaitOnline(deviceCtx);

            var actions = createActions(deviceCtx.handler());
            var action = CompletableFuture.runAsync(() -> {
                try {
                    actions.enterConfigMode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            var request = device.readDeviceCalCfgRequest();
            assertThat(request.channelNumber()).isEqualTo(-1);
            assertThat(request.command()).isEqualTo(SUPLA_CALCFG_CMD_ENTER_CFG_MODE);
            assertThat(request.superUserAuthorized()).isEqualTo((byte) 1);
            consumeDeviceCalCfgResult(deviceCtx.handler(), request, SUPLA_CALCFG_RESULT_DONE);
            action.get(30, TimeUnit.SECONDS);
        }
    }

    private static SuplaServerDeviceActions createActions(ThingHandler handler) {
        var actions = new SuplaServerDeviceActions();
        actions.setThingHandler(handler);
        return actions;
    }

    private static void consumeDeviceCalCfgResult(ThingHandler handler, DeviceCalCfgRequest request, int result) {
        ((ServerSuplaDeviceHandler) handler)
                .consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                        request.senderId(),
                        request.channelNumber(),
                        request.command(),
                        result,
                        request.dataSize(),
                        request.data()));
    }

    private static void awaitWaitingForRegistration(ThingCtx deviceCtx) {
        await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                .isEqualTo(new ThingStatusInfo(
                        UNKNOWN, CONFIGURATION_PENDING, "@text/supla.offline.waiting-for-registration")));
    }

    private static void awaitOnline(ThingCtx deviceCtx) {
        await().untilAsserted(() -> assertThat(deviceCtx.openHabDevice().findThingStatus())
                .isEqualTo(ThingStatusInfoBuilder.create(ONLINE, org.openhab.core.thing.ThingStatusDetail.NONE)
                        .build()));
    }

    private static final class ActionAwareMew01 extends ZamelMew01 {
        private ActionAwareMew01(String guid, String email, String authKey) {
            super(guid, email, authKey);
        }

        private DeviceCalCfgRequest readDeviceCalCfgRequest() throws IOException {
            var read = read();
            assertThat(read).isInstanceOf(DeviceCalCfgRequest.class);
            return (DeviceCalCfgRequest) read;
        }
    }

    private static final class ActionAwareThw01 extends ZamelThw01 {
        private ActionAwareThw01(String guid, String email, String authKey) {
            super(guid, email, authKey);
        }

        private DeviceCalCfgRequest readDeviceCalCfgRequest() throws IOException {
            var read = read();
            assertThat(read).isInstanceOf(DeviceCalCfgRequest.class);
            return (DeviceCalCfgRequest) read;
        }
    }
}
