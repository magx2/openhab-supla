package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DIMMER;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_RESULT_NOT_SUPPORTED;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_FIRMWARE_CHECK_RESULT_ERROR;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_AVAILABLE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_NOT_AVAILABLE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.OTA_CHANGELOG_URL_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.OTA_LAST_CHECK_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.OTA_STATUS_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.OTA_VERSION_AVAILABLE_PROPERTY;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openhab.core.thing.Thing;
import pl.grzeslowski.jsupla.protocol.api.BitFunction;
import pl.grzeslowski.jsupla.protocol.api.ChannelFlag;
import pl.grzeslowski.jsupla.protocol.api.encoders.FirmwareCheckResultEncoder;
import pl.grzeslowski.jsupla.protocol.api.structs.FirmwareCheckResult;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.DeviceCalCfgResult;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

class ServerSuplaDeviceHandlerTest {
    private final Thing thing = Mockito.mock(Thing.class);
    private final Map<String, String> properties = new HashMap<>();
    private TestServerSuplaDeviceHandler handler;

    @BeforeEach
    void setUp() {
        when(thing.getProperties()).thenReturn(properties);
        when(thing.setProperty(anyString(), nullable(String.class))).thenAnswer(invocation -> {
            var key = invocation.getArgument(0, String.class);
            var value = invocation.getArgument(1, String.class);
            if (value == null) {
                return properties.remove(key);
            }
            return properties.put(key, value);
        });
        handler = new TestServerSuplaDeviceHandler(thing);
    }

    @Test
    void shouldBuildChannelProperties() {
        var firstChannel = new DeviceChannel(
                0,
                false,
                SUPLA_CHANNELTYPE_DIMMER,
                Set.of(ChannelFlag.SUPLA_CHANNEL_FLAG_CHANNELSTATE, ChannelFlag.SUPLA_CHANNEL_FLAG_WEEKLY_SCHEDULE),
                SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER,
                Set.of(),
                new byte[8],
                null,
                null,
                null,
                0L,
                Set.of(BitFunction.SUPLA_BIT_FUNC_LIGHTSWITCH),
                0);
        var secondChannel = new DeviceChannel(
                1,
                false,
                SUPLA_CHANNELTYPE_DIMMER,
                Set.of(),
                null,
                Set.of(),
                new byte[8],
                null,
                null,
                null,
                0L,
                Set.of(),
                0);

        var properties = ServerSuplaDeviceHandler.buildChannelProperties(List.of(firstChannel, secondChannel));

        assertThat(properties)
                .containsEntry("CHANNEL_FUNCTION_0", "SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER")
                .containsEntry("CHANNEL_FLAGS_1", "[]")
                .containsEntry("CHANNEL_FUNCTIONS_1", "[]")
                .doesNotContainKey("CHANNEL_FUNCTION_1");
        assertThat(properties.get("CHANNEL_FLAGS_0"))
                .startsWith("[")
                .endsWith("]")
                .contains("SUPLA_CHANNEL_FLAG_CHANNELSTATE", "SUPLA_CHANNEL_FLAG_WEEKLY_SCHEDULE");
        assertThat(properties.get("CHANNEL_FUNCTIONS_0"))
                .startsWith("[")
                .endsWith("]")
                .contains("SUPLA_BIT_FUNC_LIGHTSWITCH");
    }

    @Test
    void shouldTreatImmediateFirmwareCheckAcceptanceAsSeparateResult() throws Exception {
        handler.markOtaCheckPending();
        var immediateResult = new DeviceCalCfgResult(
                0, -1, SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE, SUPLA_CALCFG_RESULT_DONE, 0L, new byte[0]);

        handler.consumeDeviceCalCfgResult(immediateResult);

        assertThat(handler.listenForDeviceCalCfgResult(1, SECONDS)).isEqualTo(immediateResult);
        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("CHECKING");
        assertThat(handler.isOtaCheckPending()).isTrue();
    }

    @Test
    void shouldPersistAvailableFirmwareCheckResult() {
        handler.markOtaCheckPending();

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                0,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE,
                SUPLA_CALCFG_RESULT_DONE,
                FirmwareCheckResult.SIZE,
                encodeFirmwareCheckResult(
                        SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_AVAILABLE, "1.2.3", "https://example.test/changelog")));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("AVAILABLE");
        assertThat(properties.get(OTA_VERSION_AVAILABLE_PROPERTY)).isEqualTo("1.2.3");
        assertThat(properties.get(OTA_CHANGELOG_URL_PROPERTY)).isEqualTo("https://example.test/changelog");
        assertThat(properties.get(OTA_LAST_CHECK_PROPERTY)).isNotBlank();
        assertThat(handler.isOtaCheckPending()).isFalse();
    }

    @Test
    void shouldPersistNotAvailableFirmwareCheckResult() {
        handler.markOtaCheckPending();

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                0,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE,
                SUPLA_CALCFG_RESULT_DONE,
                FirmwareCheckResult.SIZE,
                encodeFirmwareCheckResult(SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_NOT_AVAILABLE, "", "")));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("NOT_AVAILABLE");
        assertThat(properties)
                .doesNotContainKey(OTA_VERSION_AVAILABLE_PROPERTY)
                .doesNotContainKey(OTA_CHANGELOG_URL_PROPERTY);
        assertThat(handler.isOtaCheckPending()).isFalse();
    }

    @Test
    void shouldPersistErrorFirmwareCheckResult() {
        handler.markOtaCheckPending();

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                0,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE,
                SUPLA_CALCFG_RESULT_DONE,
                FirmwareCheckResult.SIZE,
                encodeFirmwareCheckResult(SUPLA_FIRMWARE_CHECK_RESULT_ERROR, "", "")));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("ERROR");
        assertThat(handler.isOtaCheckPending()).isFalse();
    }

    @Test
    void shouldPersistErrorWhenFirmwareCheckCommandIsRejected() {
        handler.markOtaCheckPending();

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                0, -1, SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE, SUPLA_CALCFG_RESULT_NOT_SUPPORTED, 0L, new byte[0]));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("ERROR");
        assertThat(handler.isOtaCheckPending()).isFalse();
    }

    private static byte[] encodeFirmwareCheckResult(int result, String softVer, String changelogUrl) {
        var payload = new FirmwareCheckResult(
                (short) result,
                java.util.Arrays.copyOf(softVer.getBytes(StandardCharsets.UTF_8), 21),
                java.util.Arrays.copyOf(changelogUrl.getBytes(StandardCharsets.UTF_8), 101));
        var data = new byte[FirmwareCheckResult.SIZE];
        FirmwareCheckResultEncoder.INSTANCE.encode(payload, data, 0);
        return data;
    }
}
