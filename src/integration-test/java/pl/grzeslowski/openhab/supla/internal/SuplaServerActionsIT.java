package pl.grzeslowski.openhab.supla.internal;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatus.UNKNOWN;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_PENDING;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgCommand.*;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.jsupla.protocol.api.DeviceFlag.SUPLA_DEVICE_FLAG_AUTOMATIC_FIRMWARE_UPDATE_SUPPORTED;
import static pl.grzeslowski.jsupla.protocol.api.FirmwareCheckResultCode.SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_AVAILABLE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_DEVICE_TYPE_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_TYPE_ID;
import static pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension.deviceInitialize;
import static pl.grzeslowski.openhab.supla.internal.extension.supla.SuplaExtension.serverInitialize;

import io.github.glytching.junit.extension.random.RandomBeansExtension;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import pl.grzeslowski.jsupla.protocol.api.encoders.FirmwareCheckResultEncoder;
import pl.grzeslowski.jsupla.protocol.api.structs.FirmwareCheckResult;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.DeviceCalCfgResult;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelC;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceE;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.DeviceCalCfgRequest;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.openhab.supla.actions.SuplaServerConfigModeActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerElectricityMeterActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerFirmwareUpdateActions;
import pl.grzeslowski.openhab.supla.internal.device.Device;
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
class SuplaServerActionsIT {
    @Test
    @DisplayName("should send check firmware update action to ota-capable device and persist async result")
    void checkFirmwareUpdate(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Email String email,
            @AuthKey String authKey)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        serverInitialize(serverCtx, port);
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid);

        try (var device = new OtaAwareDevice(guid, email, authKey)) {
            device.initialize("localhost", port);
            device.register();
            var registerResult = device.readRegisterDeviceResultA();
            assertThat(registerResult).isEqualTo(new SuplaRegisterDeviceResultA(3, (short) 100, (short) 18, (short) 1));
            awaitWaitingForRegistration(deviceCtx);

            device.sendPing();
            assertThat(device.readPing().now()).isNotNull();
            awaitOnline(deviceCtx);

            var actions = createFirmwareUpdateActions(deviceCtx.handler());
            var action = CompletableFuture.supplyAsync(() -> {
                try {
                    return actions.checkFirmwareUpdate();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            var request = readDeviceCalCfgRequest(device::readDeviceCalCfgRequest);
            assertThat(request.channelNumber()).isEqualTo(-1);
            assertThat(request.command()).isEqualTo(SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue());
            assertThat(request.superUserAuthorized()).isEqualTo((byte) 1);

            consumeDeviceCalCfgResult(
                    deviceCtx.handler(),
                    new DeviceCalCfgResult(
                            request.senderId(),
                            request.channelNumber(),
                            request.command(),
                            SUPLA_CALCFG_RESULT_DONE.getValue(),
                            0L,
                            new byte[0]));
            assertThat(action.isDone()).isFalse();

            consumeDeviceCalCfgResult(
                    deviceCtx.handler(),
                    new DeviceCalCfgResult(
                            request.senderId(),
                            request.channelNumber(),
                            request.command(),
                            SUPLA_CALCFG_RESULT_DONE.getValue(),
                            FirmwareCheckResult.SIZE,
                            encodeFirmwareCheckResult(
                                    SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_AVAILABLE.getValue(),
                                    "2.3.4",
                                    "https://example.test/changelog")));
            assertThat(action.get(30, SECONDS)).isEqualTo("AVAILABLE");

            await().untilAsserted(() -> {
                var properties = deviceCtx.handler().getThing().getProperties();
                assertThat(properties.get("otaStatus")).isEqualTo("AVAILABLE");
                assertThat(properties.get("otaVersionAvailable")).isEqualTo("2.3.4");
                assertThat(properties.get("otaChangelogUrl")).isEqualTo("https://example.test/changelog");
                assertThat(properties.get("otaLastCheck")).isNotBlank();
            });
        }
    }

    @Test
    @DisplayName("should send start firmware update action to ota-capable device")
    void startFirmwareUpdate(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Email String email,
            @AuthKey String authKey)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        serverInitialize(serverCtx, port);
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid);

        try (var device = new OtaAwareDevice(guid, email, authKey)) {
            initializeAndAwaitOnline(deviceCtx, device, port);

            var actions = createFirmwareUpdateActions(deviceCtx.handler());
            var action = CompletableFuture.supplyAsync(() -> {
                try {
                    return actions.startFirmwareUpdate();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            var request = readDeviceCalCfgRequest(device::readDeviceCalCfgRequest);
            assertThat(request.channelNumber()).isEqualTo(-1);
            assertThat(request.command()).isEqualTo(SUPLA_CALCFG_CMD_START_FIRMWARE_UPDATE.getValue());
            consumeDeviceCalCfgResult(deviceCtx.handler(), request);
            assertThat(action.get(30, SECONDS)).isEqualTo("ACCEPTED");

            await().untilAsserted(() -> assertThat(
                            deviceCtx.handler().getThing().getProperties().get("otaStatus"))
                    .isEqualTo("UPDATE_TRIGGERED"));
        }
    }

    @Test
    @DisplayName("should send start security update action to ota-capable device")
    void startSecurityUpdate(
            @CreateHandler(thingTypeId = SUPLA_SERVER_DEVICE_TYPE_ID) ThingCtx deviceCtx,
            @CreateHandler(thingTypeId = SUPLA_SERVER_TYPE_ID) BridgeCtx serverCtx,
            @Port int port,
            @Email String email,
            @AuthKey String authKey)
            throws Exception {
        var guid = deviceCtx.thing().getUID().getId();
        serverInitialize(serverCtx, port);
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid);

        try (var device = new OtaAwareDevice(guid, email, authKey)) {
            initializeAndAwaitOnline(deviceCtx, device, port);

            var actions = createFirmwareUpdateActions(deviceCtx.handler());
            var action = CompletableFuture.supplyAsync(() -> {
                try {
                    return actions.startSecurityUpdate();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            var request = readDeviceCalCfgRequest(device::readDeviceCalCfgRequest);
            assertThat(request.channelNumber()).isEqualTo(-1);
            assertThat(request.command()).isEqualTo(SUPLA_CALCFG_CMD_START_SECURITY_UPDATE.getValue());
            consumeDeviceCalCfgResult(deviceCtx.handler(), request);
            assertThat(action.get(30, SECONDS)).isEqualTo("ACCEPTED");

            await().untilAsserted(() -> assertThat(
                            deviceCtx.handler().getThing().getProperties().get("otaStatus"))
                    .isEqualTo("UPDATE_TRIGGERED"));
        }
    }

    @Test
    @DisplayName("should reject firmware update action when device does not support ota")
    void startFirmwareUpdateUnsupported(
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
            initializeAndAwaitOnline(deviceCtx, device, port);

            var actions = createFirmwareUpdateActions(deviceCtx.handler());
            assertThatThrownBy(actions::startFirmwareUpdate)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device does not support automatic firmware updates");
        }
    }

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

            var actions = createElectricityMeterActions(deviceCtx.handler());
            var action = CompletableFuture.runAsync(() -> {
                try {
                    actions.resetElectricMeterCounters(
                            "supla:server-device:%s:0#totalForwardActiveEnergy".formatted(guid));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            var request = readDeviceCalCfgRequest(device::readDeviceCalCfgRequest);
            assertThat(request.channelNumber()).isEqualTo(0);
            assertThat(request.command()).isEqualTo(SUPLA_CALCFG_CMD_RESET_COUNTERS.getValue());
            assertThat(request.superUserAuthorized()).isEqualTo((byte) 1);
            consumeDeviceCalCfgResult(deviceCtx.handler(), request);
            action.get(30, SECONDS);
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

            var actions = createConfigModeActions(deviceCtx.handler());
            var action = CompletableFuture.runAsync(() -> {
                try {
                    actions.enterConfigMode();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            var request = readDeviceCalCfgRequest(device::readDeviceCalCfgRequest);
            assertThat(request.channelNumber()).isEqualTo(-1);
            assertThat(request.command()).isEqualTo(SUPLA_CALCFG_CMD_ENTER_CFG_MODE.getValue());
            assertThat(request.superUserAuthorized()).isEqualTo((byte) 1);
            consumeDeviceCalCfgResult(deviceCtx.handler(), request);
            action.get(30, SECONDS);
        }
    }

    private static SuplaServerFirmwareUpdateActions createFirmwareUpdateActions(ThingHandler handler) {
        var actions = new SuplaServerFirmwareUpdateActions();
        actions.setThingHandler(handler);
        return actions;
    }

    private static SuplaServerElectricityMeterActions createElectricityMeterActions(ThingHandler handler) {
        var actions = new SuplaServerElectricityMeterActions();
        actions.setThingHandler(handler);
        return actions;
    }

    private static SuplaServerConfigModeActions createConfigModeActions(ThingHandler handler) {
        var actions = new SuplaServerConfigModeActions();
        actions.setThingHandler(handler);
        return actions;
    }

    private static DeviceCalCfgRequest readDeviceCalCfgRequest(Supplier<DeviceCalCfgRequest> requestSupplier)
            throws Exception {
        return CompletableFuture.supplyAsync(requestSupplier).get(30, SECONDS);
    }

    private static void consumeDeviceCalCfgResult(ThingHandler handler, DeviceCalCfgRequest request) {
        ((ServerSuplaDeviceHandler) handler)
                .consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                        request.senderId(),
                        request.channelNumber(),
                        request.command(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        request.dataSize(),
                        request.data()));
    }

    private static void consumeDeviceCalCfgResult(ThingHandler handler, DeviceCalCfgResult result) {
        ((ServerSuplaDeviceHandler) handler).consumeDeviceCalCfgResult(result);
    }

    private static byte[] encodeFirmwareCheckResult(int result, String softVer, String changelogUrl) {
        var payload = new FirmwareCheckResult(
                (short) result,
                Arrays.copyOf(softVer.getBytes(UTF_8), 21),
                Arrays.copyOf(changelogUrl.getBytes(UTF_8), 101));
        var data = new byte[FirmwareCheckResult.SIZE];
        FirmwareCheckResultEncoder.INSTANCE.encode(payload, data, 0);
        return data;
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

    private static void initializeAndAwaitOnline(ThingCtx deviceCtx, Device device, int port) throws Exception {
        device.initialize("localhost", port);
        device.register();
        assertThat(device.readRegisterDeviceResultA()).isNotNull();
        awaitWaitingForRegistration(deviceCtx);
        device.sendPing();
        assertThat(device.readPing().now()).isNotNull();
        awaitOnline(deviceCtx);
    }

    private static final class ActionAwareMew01 extends ZamelMew01 {
        private ActionAwareMew01(String guid, String email, String authKey) {
            super(guid, email, authKey);
        }

        @SneakyThrows
        private DeviceCalCfgRequest readDeviceCalCfgRequest() {
            var read = read();
            assertThat(read).isInstanceOf(DeviceCalCfgRequest.class);
            return (DeviceCalCfgRequest) read;
        }
    }

    private static final class ActionAwareThw01 extends ZamelThw01 {
        private ActionAwareThw01(String guid, String email, String authKey) {
            super(guid, email, authKey);
        }

        @SneakyThrows
        private DeviceCalCfgRequest readDeviceCalCfgRequest() {
            var read = read();
            assertThat(read).isInstanceOf(DeviceCalCfgRequest.class);
            return (DeviceCalCfgRequest) read;
        }
    }

    private static final class OtaAwareDevice extends Device {
        private final byte[] email;
        private final byte[] authKey;

        private OtaAwareDevice(String guid, String email, String authKey) {
            super((short) 18, guid);
            this.email = Arrays.copyOf(email.getBytes(UTF_8), SUPLA_EMAIL_MAXSIZE);
            this.authKey = pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes(authKey);
        }

        @Override
        public void register() throws IOException {
            var proto = new SuplaRegisterDeviceE(
                    email,
                    authKey,
                    pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.hexToBytes(guid),
                    Arrays.copyOf("OTA TEST DEVICE".getBytes(UTF_8), 201),
                    Arrays.copyOf("2.3.4".getBytes(UTF_8), 21),
                    Arrays.copyOf("192.168.1.10".getBytes(UTF_8), 65),
                    SUPLA_DEVICE_FLAG_AUTOMATIC_FIRMWARE_UPDATE_SUPPORTED.getValue(),
                    (short) 4,
                    (short) 6000,
                    (short) 0,
                    new SuplaDeviceChannelC[0]);
            send(proto);
        }

        @SneakyThrows
        private DeviceCalCfgRequest readDeviceCalCfgRequest() {
            var read = read();
            assertThat(read).isInstanceOf(DeviceCalCfgRequest.class);
            return (DeviceCalCfgRequest) read;
        }

        @Override
        protected void updateChannel(short channelNumber, byte[] value) {
            throw new UnsupportedOperationException("There are no channels that OH can update in ota test device!");
        }
    }
}
