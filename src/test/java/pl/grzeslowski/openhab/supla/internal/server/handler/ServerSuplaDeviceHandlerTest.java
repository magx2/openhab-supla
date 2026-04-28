package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgCommand.SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_NOT_SUPPORTED;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_ELECTRICITY_METER;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DIMMER;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_ELECTRICITY_METER;
import static pl.grzeslowski.jsupla.protocol.api.DeviceFlag.SUPLA_DEVICE_FLAG_AUTOMATIC_FIRMWARE_UPDATE_SUPPORTED;
import static pl.grzeslowski.jsupla.protocol.api.DeviceFlag.SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE;
import static pl.grzeslowski.jsupla.protocol.api.FirmwareCheckResultCode.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.MANUFACTURER_ID_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.OTA_CHANGELOG_URL_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.OTA_LAST_CHECK_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.OTA_STATUS_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.OTA_VERSION_AVAILABLE_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.PRODUCT_ID_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.PRODUCT_LATEST_DESCRIPTION_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.PRODUCT_LATEST_RELEASE_AT_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.PRODUCT_LATEST_VERSION_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.PRODUCT_MANUFACTURER_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.PRODUCT_NAME_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.PRODUCT_UPDATES_COUNT_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.PRODUCT_URL_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.SOFTWARE_UPDATE_AVAILABLE_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.SOFTWARE_UPDATE_LAST_CHECK_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.SOFTWARE_UPDATE_STATUS_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.SOFTWARE_UPDATE_URL_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.SOFTWARE_UPDATE_VERSION_PROPERTY;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
import pl.grzeslowski.jsupla.protocol.api.SuplaProducts;
import pl.grzeslowski.jsupla.protocol.api.encoders.FirmwareCheckResultEncoder;
import pl.grzeslowski.jsupla.protocol.api.structs.FirmwareCheckResult;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.DeviceCalCfgResult;
import pl.grzeslowski.openhab.supla.actions.SuplaServerConfigModeActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerDeviceConfigActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerElectricityMeterActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerFirmwareUpdateActions;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterEmailDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.updates.SuplaUpdatesClient;

class ServerSuplaDeviceHandlerTest {
    private static final int FIRMWARE_CHECK_MESSAGE_ID = 37;

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
    void shouldBuildProductNamePropertyFromManufacturerAndProductIds() {
        var productName = ServerSuplaDeviceHandler.buildProductNameProperty(4, 6000);

        assertThat(productName).isEqualTo("ZAMEL THW-01");
    }

    @Test
    void shouldBuildProductInfoPropertiesFromManufacturerAndProductIds() {
        var productInfo = SuplaProducts.findByIds(4, 6000).orElseThrow();

        var properties = ServerSuplaDeviceHandler.buildProductInfoProperties(4, 6000);

        assertThat(properties)
                .containsEntry(PRODUCT_MANUFACTURER_PROPERTY, productInfo.manufacturer())
                .containsEntry(PRODUCT_NAME_PROPERTY, productInfo.name())
                .containsEntry(PRODUCT_UPDATES_COUNT_PROPERTY, Integer.toString(productInfo.updatesCount()))
                .containsEntry(PRODUCT_LATEST_RELEASE_AT_PROPERTY, productInfo.latestReleaseAt())
                .containsEntry(PRODUCT_LATEST_VERSION_PROPERTY, productInfo.latestVersion())
                .containsEntry(PRODUCT_LATEST_DESCRIPTION_PROPERTY, productInfo.latestDescription())
                .containsEntry(PRODUCT_URL_PROPERTY, productInfo.productUrl());
    }

    @Test
    void shouldNotBuildProductNamePropertyWhenAnyIdIsMissing() {
        assertThat(ServerSuplaDeviceHandler.buildProductNameProperty(null, 6000))
                .isNull();
        assertThat(ServerSuplaDeviceHandler.buildProductNameProperty(4, null)).isNull();
        assertThat(ServerSuplaDeviceHandler.buildProductNameProperty(999, 999)).isNull();
        assertThat(ServerSuplaDeviceHandler.buildProductInfoProperties(null, 6000))
                .isEmpty();
        assertThat(ServerSuplaDeviceHandler.buildProductInfoProperties(4, null)).isEmpty();
        assertThat(ServerSuplaDeviceHandler.buildProductInfoProperties(999, 999))
                .isEmpty();
    }

    @Test
    void shouldBuildSoftwareUpdateRequestFromRegisteredDevice() {
        var registerEntity = new RegisterEmailDeviceTrait(
                "guid", "device", "1.2.3", 4, 6000, Set.of(), List.of(), "test@example.org", new byte[0], "server");

        var request = ServerSuplaDeviceHandler.buildSoftwareUpdateRequest(registerEntity);

        assertThat(request).isNotNull();
        assertThat(request.manufacturerId()).isEqualTo(4);
        assertThat(request.productId()).isEqualTo(6000);
        assertThat(request.productName()).isEqualTo("ZAMEL THW-01");
        assertThat(request.version()).isEqualTo("1.2.3");
    }

    @Test
    void shouldNotBuildSoftwareUpdateRequestWhenIdsAreMissing() {
        var registerEntity = new RegisterEmailDeviceTrait(
                "guid", "device", "1.2.3", null, 6000, Set.of(), List.of(), "test@example.org", new byte[0], "server");

        assertThat(ServerSuplaDeviceHandler.buildSoftwareUpdateRequest(registerEntity))
                .isNull();
    }

    @Test
    void shouldPersistAvailableSoftwareUpdateResult() {
        var checkedAt = Instant.parse("2026-04-28T10:15:30Z");

        handler.updateSoftwareUpdateState(
                new SuplaUpdatesClient.Result(
                        SuplaUpdatesClient.Status.UPDATE_AVAILABLE, "2.0.0", "https://updates.example/device"),
                checkedAt);

        assertThat(properties.get(SOFTWARE_UPDATE_STATUS_PROPERTY)).isEqualTo("UPDATE_AVAILABLE");
        assertThat(properties.get(SOFTWARE_UPDATE_AVAILABLE_PROPERTY)).isEqualTo("true");
        assertThat(properties.get(SOFTWARE_UPDATE_VERSION_PROPERTY)).isEqualTo("2.0.0");
        assertThat(properties.get(SOFTWARE_UPDATE_URL_PROPERTY)).isEqualTo("https://updates.example/device");
        assertThat(properties.get(SOFTWARE_UPDATE_LAST_CHECK_PROPERTY)).isEqualTo(checkedAt.toString());
    }

    @Test
    void shouldClearSoftwareUpdateVersionWhenUpdateIsNotAvailable() {
        properties.put(SOFTWARE_UPDATE_VERSION_PROPERTY, "2.0.0");
        properties.put(SOFTWARE_UPDATE_URL_PROPERTY, "https://updates.example/device");
        var checkedAt = Instant.parse("2026-04-28T10:15:30Z");

        handler.updateSoftwareUpdateState(
                new SuplaUpdatesClient.Result(
                        SuplaUpdatesClient.Status.UPDATE_NOT_AVAILABLE, "2.0.0", "https://updates.example/device"),
                checkedAt);

        assertThat(properties.get(SOFTWARE_UPDATE_STATUS_PROPERTY)).isEqualTo("UPDATE_NOT_AVAILABLE");
        assertThat(properties.get(SOFTWARE_UPDATE_AVAILABLE_PROPERTY)).isEqualTo("false");
        assertThat(properties)
                .doesNotContainKey(SOFTWARE_UPDATE_VERSION_PROPERTY)
                .doesNotContainKey(SOFTWARE_UPDATE_URL_PROPERTY);
        assertThat(properties.get(SOFTWARE_UPDATE_LAST_CHECK_PROPERTY)).isEqualTo(checkedAt.toString());
    }

    @Test
    void shouldClearProductInfoProperties() {
        properties.put("OTHER", "preserved");
        properties.put(MANUFACTURER_ID_PROPERTY, "4");
        properties.put(PRODUCT_ID_PROPERTY, "6000");
        properties.put(PRODUCT_MANUFACTURER_PROPERTY, "Zamel");
        properties.put(PRODUCT_NAME_PROPERTY, "THW-01");
        properties.put(PRODUCT_UPDATES_COUNT_PROPERTY, "1");
        properties.put(PRODUCT_LATEST_RELEASE_AT_PROPERTY, "2024-09-05");
        properties.put(PRODUCT_LATEST_VERSION_PROPERTY, "1.0.0");
        properties.put(PRODUCT_LATEST_DESCRIPTION_PROPERTY, "desc");
        properties.put(PRODUCT_URL_PROPERTY, "https://example.org");

        handler.clearProductInfoProperties();

        assertThat(properties)
                .containsEntry("OTHER", "preserved")
                .doesNotContainKeys(
                        MANUFACTURER_ID_PROPERTY,
                        PRODUCT_ID_PROPERTY,
                        PRODUCT_MANUFACTURER_PROPERTY,
                        PRODUCT_NAME_PROPERTY,
                        PRODUCT_UPDATES_COUNT_PROPERTY,
                        PRODUCT_LATEST_RELEASE_AT_PROPERTY,
                        PRODUCT_LATEST_VERSION_PROPERTY,
                        PRODUCT_LATEST_DESCRIPTION_PROPERTY,
                        PRODUCT_URL_PROPERTY);
    }

    @Test
    void shouldSelectActionServicesFromRegisteredDeviceCapabilities() {
        var electricityMeterChannel = new DeviceChannel(
                0,
                false,
                SUPLA_CHANNELTYPE_ELECTRICITY_METER,
                Set.of(),
                SUPLA_CHANNELFNC_ELECTRICITY_METER,
                Set.of(),
                new byte[8],
                null,
                null,
                null,
                0L,
                Set.of(),
                0);
        var registerEntity = new RegisterEmailDeviceTrait(
                "guid",
                "device",
                "soft",
                null,
                null,
                Set.of(SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE, SUPLA_DEVICE_FLAG_AUTOMATIC_FIRMWARE_UPDATE_SUPPORTED),
                List.of(electricityMeterChannel),
                "test@example.org",
                new byte[0],
                "server");

        assertThat(ServerSuplaDeviceHandler.actionServicesFor(registerEntity))
                .isEqualTo(Set.of(
                        SuplaServerDeviceConfigActions.class,
                        SuplaServerElectricityMeterActions.class,
                        SuplaServerConfigModeActions.class,
                        SuplaServerFirmwareUpdateActions.class));
    }

    @Test
    void shouldTreatImmediateFirmwareCheckAcceptanceAsSeparateResult() throws Exception {
        handler.markOtaCheckPending(FIRMWARE_CHECK_MESSAGE_ID);
        var immediateResult = new DeviceCalCfgResult(
                FIRMWARE_CHECK_MESSAGE_ID,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                SUPLA_CALCFG_RESULT_DONE.getValue(),
                0L,
                new byte[0]);

        handler.consumeDeviceCalCfgResult(immediateResult);

        assertThat(handler.listenForDeviceCalCfgResult(1, SECONDS)).isEqualTo(immediateResult);
        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("CHECKING");
        assertThat(handler.isOtaCheckPending()).isTrue();
    }

    @Test
    void shouldPersistAvailableFirmwareCheckResult() {
        handler.markOtaCheckPending(FIRMWARE_CHECK_MESSAGE_ID);

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                FIRMWARE_CHECK_MESSAGE_ID,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                SUPLA_CALCFG_RESULT_DONE.getValue(),
                FirmwareCheckResult.SIZE,
                encodeFirmwareCheckResult(
                        SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_AVAILABLE.getValue(),
                        "1.2.3",
                        "https://example.test/changelog")));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("AVAILABLE");
        assertThat(properties.get(OTA_VERSION_AVAILABLE_PROPERTY)).isEqualTo("1.2.3");
        assertThat(properties.get(OTA_CHANGELOG_URL_PROPERTY)).isEqualTo("https://example.test/changelog");
        assertThat(properties.get(OTA_LAST_CHECK_PROPERTY)).isNotBlank();
        assertThat(handler.isOtaCheckPending()).isFalse();
    }

    @Test
    void shouldPersistNotAvailableFirmwareCheckResult() {
        handler.markOtaCheckPending(FIRMWARE_CHECK_MESSAGE_ID);

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                FIRMWARE_CHECK_MESSAGE_ID,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                SUPLA_CALCFG_RESULT_DONE.getValue(),
                FirmwareCheckResult.SIZE,
                encodeFirmwareCheckResult(SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_NOT_AVAILABLE.getValue(), "", "")));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("NOT_AVAILABLE");
        assertThat(properties)
                .doesNotContainKey(OTA_VERSION_AVAILABLE_PROPERTY)
                .doesNotContainKey(OTA_CHANGELOG_URL_PROPERTY);
        assertThat(handler.isOtaCheckPending()).isFalse();
    }

    @Test
    void shouldPersistErrorFirmwareCheckResult() {
        handler.markOtaCheckPending(FIRMWARE_CHECK_MESSAGE_ID);

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                FIRMWARE_CHECK_MESSAGE_ID,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                SUPLA_CALCFG_RESULT_DONE.getValue(),
                FirmwareCheckResult.SIZE,
                encodeFirmwareCheckResult(SUPLA_FIRMWARE_CHECK_RESULT_ERROR.getValue(), "", "")));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("ERROR");
        assertThat(handler.isOtaCheckPending()).isFalse();
    }

    @Test
    void shouldPersistErrorWhenFirmwareCheckCommandIsRejected() {
        handler.markOtaCheckPending(FIRMWARE_CHECK_MESSAGE_ID);

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                FIRMWARE_CHECK_MESSAGE_ID,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                SUPLA_CALCFG_RESULT_NOT_SUPPORTED.getValue(),
                0L,
                new byte[0]));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("ERROR");
        assertThat(handler.isOtaCheckPending()).isFalse();
    }

    @Test
    void shouldIgnoreFirmwareCheckResultFromPreviousRequest() {
        handler.markOtaCheckPending(FIRMWARE_CHECK_MESSAGE_ID);

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                FIRMWARE_CHECK_MESSAGE_ID - 1,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                SUPLA_CALCFG_RESULT_DONE.getValue(),
                FirmwareCheckResult.SIZE,
                encodeFirmwareCheckResult(
                        SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_AVAILABLE.getValue(),
                        "9.9.9",
                        "https://example.test/stale")));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("CHECKING");
        assertThat(properties)
                .doesNotContainKey(OTA_VERSION_AVAILABLE_PROPERTY)
                .doesNotContainKey(OTA_CHANGELOG_URL_PROPERTY)
                .doesNotContainKey(OTA_LAST_CHECK_PROPERTY);
        assertThat(handler.isOtaCheckPending()).isTrue();

        handler.consumeDeviceCalCfgResult(new DeviceCalCfgResult(
                FIRMWARE_CHECK_MESSAGE_ID,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                SUPLA_CALCFG_RESULT_DONE.getValue(),
                FirmwareCheckResult.SIZE,
                encodeFirmwareCheckResult(SUPLA_FIRMWARE_CHECK_RESULT_UPDATE_NOT_AVAILABLE.getValue(), "", "")));

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("NOT_AVAILABLE");
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
