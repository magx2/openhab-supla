package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgCommand.SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_NOT_SUPPORTED;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DIMMER;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_HUMIDITYANDTEMPSENSOR;
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

import io.netty.handler.timeout.ReadTimeoutException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import pl.grzeslowski.jsupla.protocol.api.BitFunction;
import pl.grzeslowski.jsupla.protocol.api.ChannelFlag;
import pl.grzeslowski.jsupla.protocol.api.DeviceFlag;
import pl.grzeslowski.jsupla.protocol.api.SuplaProducts;
import pl.grzeslowski.jsupla.protocol.api.encoders.FirmwareCheckResultEncoder;
import pl.grzeslowski.jsupla.protocol.api.structs.FirmwareCheckResult;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.DeviceCalCfgResult;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueC;
import pl.grzeslowski.jsupla.server.SuplaWriteFuture;
import pl.grzeslowski.jsupla.server.SuplaWriter;
import pl.grzeslowski.openhab.supla.actions.SuplaServerConfigModeActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerDeviceConfigActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerElectricityMeterActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerFirmwareUpdateActions;
import pl.grzeslowski.openhab.supla.internal.server.netty.OpenHabMessageHandler;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.AuthData;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.DeviceConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterEmailDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.SuplaDevice;
import pl.grzeslowski.openhab.supla.internal.updates.SuplaUpdatesClient;

class ServerSuplaDeviceHandlerTest {
    private static final int FIRMWARE_CHECK_MESSAGE_ID = 37;
    private static final ZoneId TIME_ZONE = ZoneId.of("America/New_York");
    private static final TimeZoneProvider TIME_ZONE_PROVIDER = () -> TIME_ZONE;

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
        handler = new TestServerSuplaDeviceHandler(thing, TIME_ZONE_PROVIDER);
    }

    @ParameterizedTest
    @MethodSource("sleepDisconnects")
    void shouldTreatSleepingDeviceSocketDisconnectAsSleep(Throwable exception) throws Exception {
        prepareLifecycle(true);
        var connection = mock(OpenHabMessageHandler.class);
        setHandlerField("handler", connection);
        var writer = mock(SuplaWriter.class);
        handler.active(writer);

        handler.socketException(exception);

        assertThat(handler.isSleeping()).isTrue();
        assertThat(handler.getWriter().get()).isNull();
        verify(connection).clear();
        verify(handler, never()).updateStatus(eq(ThingStatus.OFFLINE), any(), any());
        verify(handler.getLogger(), never()).warn(anyString());
        verify(handler.getLogger(), never()).warn(anyString(), any(Object.class));
    }

    private static Stream<Throwable> sleepDisconnects() {
        return Stream.of(ReadTimeoutException.INSTANCE, new SocketException("Connection reset"));
    }

    @Test
    void shouldWakeAndSleepRepeatedlyWithoutLosingCachedState() throws Exception {
        prepareLifecycle(true);
        var channelUid = new ChannelUID("supla:server-device:guid:0#temperature");
        var state = new DecimalType(21);
        handler.saveState(channelUid, state, Duration.ofMinutes(1));
        try {
            for (int cycle = 0; cycle < 3; cycle++) {
                handler.active(mock(SuplaWriter.class));
                handler.register(registration(true), mock(OpenHabMessageHandler.class));
                handler.handle(new SuplaDeviceChannelValueC((short) 0, (short) 0, 60L, new byte[8]));
                assertThat(handler.isSleeping()).isFalse();

                handler.socketException(ReadTimeoutException.INSTANCE);
                handler.inactive();

                assertThat(handler.isSleeping()).isTrue();
                assertThat(handler.getWriter().get()).isNull();
                assertThat(handler.findState(channelUid)).contains(state);
            }
            verify(handler.getLogger(), never()).warn(anyString());
            verify(handler.getLogger(), never()).warn(anyString(), any(), any());
            verify(handler.getLogger(), never()).error(anyString(), any(Throwable.class));
        } finally {
            handler.dispose();
        }
    }

    @Test
    void shouldStillReportNormalDeviceSocketTimeout() throws Exception {
        prepareLifecycle(false);

        handler.socketException(ReadTimeoutException.INSTANCE);

        verify(handler).updateStatus(eq(ThingStatus.OFFLINE), eq(ThingStatusDetail.COMMUNICATION_ERROR), anyString());
        assertThat(handler.isSleeping()).isFalse();
    }

    @Test
    void shouldReplacePreviousConnectionQuietlyAndRestartPing() throws Exception {
        prepareLifecycle(true);
        var oldConnection = mock(OpenHabMessageHandler.class);
        var newConnection = mock(OpenHabMessageHandler.class);
        setHandlerField("handler", oldConnection);
        var writer = mock(SuplaWriter.class);
        handler.active(writer);
        try {
            handler.register(registration(true), newConnection);

            verify(oldConnection).clear();
            assertThat(handler.getWriter().get()).isSameAs(writer);
            assertThat(handler.isSleeping()).isFalse();
            verify(handler.getLogger(), never()).warn(anyString(), any(), any());
        } finally {
            handler.dispose();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReceiveDataAfterPingExpiredWithoutWarnings(boolean sleepEnabled) throws Exception {
        prepareLifecycle(sleepEnabled);
        handler.active(mock(SuplaWriter.class));
        var currentTime = new AtomicReference<>(Instant.parse("2026-10-18T12:00:00Z"));
        try (var clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenAnswer(__ -> currentTime.get());
            handler.register(registration(sleepEnabled), mock(OpenHabMessageHandler.class));
            var ping = devicePing();
            currentTime.set(currentTime.get().plusSeconds(61));
            checkPing();
            assertThat(ping.isRunning()).isFalse();

            handler.handle(new SuplaDeviceChannelValueC((short) 0, (short) 0, 60L, new byte[8]));

            verify(handler).updateStatus(ThingStatus.ONLINE);
            verify(handler.getLogger(), never()).error(anyString(), any(Throwable.class));
            assertThat(ping.isRunning()).isTrue();
        } finally {
            handler.dispose();
        }
    }

    @Test
    void shouldPreserveSleepValidityDuringActivityTimeoutNegotiation() throws Exception {
        prepareLifecycle(true);
        var writer = mock(SuplaWriter.class);
        when(writer.write(any())).thenReturn(mock(SuplaWriteFuture.class));
        handler.active(writer);
        var currentTime = new AtomicReference<>(Instant.parse("2026-10-18T12:00:00Z"));
        try (var clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenAnswer(__ -> currentTime.get());
            handler.register(registration(true), mock(OpenHabMessageHandler.class));
            handler.consumeSuplaSetActivityTimeout((short) 10, writer);
            handler.inactive();
            clearInvocations(handler);

            currentTime.set(currentTime.get().plusSeconds(30));
            checkPing();

            assertThat(devicePing().isRunning()).isTrue();
            assertThat(handler.isSleeping()).isTrue();
            verify(handler, never()).updateStatus(eq(ThingStatus.OFFLINE), any(), any());

            currentTime.set(currentTime.get().plusSeconds(31));
            checkPing();

            assertThat(devicePing().isRunning()).isFalse();
            verify(handler)
                    .updateStatus(eq(ThingStatus.OFFLINE), eq(ThingStatusDetail.COMMUNICATION_ERROR), anyString());
        } finally {
            handler.dispose();
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 5})
    void shouldReplaceChannelValidityWhenNewReadingArrives(long validitySeconds) throws Exception {
        prepareLifecycle(true);
        handler.active(mock(SuplaWriter.class));
        var currentTime = new AtomicReference<>(Instant.parse("2026-10-18T12:00:00Z"));
        try (var clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenAnswer(__ -> currentTime.get());
            handler.register(registration(true), mock(OpenHabMessageHandler.class));
            currentTime.set(currentTime.get().plusSeconds(30));

            handler.handle(new SuplaDeviceChannelValueC((short) 0, (short) 0, validitySeconds, new byte[8]));
            handler.inactive();
            clearInvocations(handler);
            currentTime.set(currentTime.get().plusSeconds(13));
            checkPing();

            assertThat(devicePing().isRunning()).isFalse();
            verify(handler)
                    .updateStatus(eq(ThingStatus.OFFLINE), eq(ThingStatusDetail.COMMUNICATION_ERROR), anyString());
        } finally {
            handler.dispose();
        }
    }

    private void prepareLifecycle(boolean sleepEnabled) throws Exception {
        handler = mock(
                TestServerSuplaDeviceHandler.class,
                withSettings().useConstructor(thing, TIME_ZONE_PROVIDER).defaultAnswer(CALLS_REAL_METHODS));
        when(thing.getProperties()).thenAnswer(__ -> Map.copyOf(properties));
        handler.logger = mock(Logger.class);
        setHandlerField("guid", "guid");
        doNothing().when(handler).updateStatus(any(ThingStatus.class));
        doNothing()
                .when(handler)
                .updateStatus(any(ThingStatus.class), any(ThingStatusDetail.class), nullable(String.class));
        setHandlerField("suplaDevice", SuplaDevice.of(registration(sleepEnabled)));
        setHandlerField(
                "deviceConfiguration",
                new DeviceConfiguration(
                        new TimeoutConfiguration(10, 8, 12),
                        new AuthData(null, new AuthData.EmailAuthData("test@example.org", ""))));
    }

    private RegisterEmailDeviceTrait registration(boolean sleepEnabled) {
        var channel = new DeviceChannel(
                0,
                false,
                SUPLA_CHANNELTYPE_HUMIDITYANDTEMPSENSOR,
                Set.of(),
                null,
                Set.of(),
                new byte[8],
                null,
                null,
                null,
                60L,
                Set.of(),
                0);
        return new RegisterEmailDeviceTrait(
                "guid",
                "THW-01",
                "1.0",
                null,
                null,
                sleepEnabled ? Set.of(DeviceFlag.SUPLA_DEVICE_FLAG_SLEEP_MODE_ENABLED) : Set.of(),
                List.of(channel),
                "test@example.org",
                new byte[0],
                "server");
    }

    private Ping devicePing() throws Exception {
        var field = ServerSuplaDeviceHandler.class.getDeclaredField("ping");
        field.setAccessible(true);
        return (Ping) field.get(handler);
    }

    private void checkPing() throws Exception {
        var check = Ping.class.getDeclaredMethod("checkIfDeviceIsUp");
        check.setAccessible(true);
        check.invoke(devicePing());
    }

    private void setHandlerField(String name, Object value) throws Exception {
        var field = ServerSuplaDeviceHandler.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(handler, value);
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

        var properties = ServerSuplaDeviceHandler.buildProductInfoProperties(4, 6000, TIME_ZONE_PROVIDER);

        assertThat(properties)
                .containsEntry(PRODUCT_MANUFACTURER_PROPERTY, productInfo.manufacturer())
                .containsEntry(PRODUCT_NAME_PROPERTY, productInfo.name())
                .containsEntry(PRODUCT_UPDATES_COUNT_PROPERTY, Integer.toString(productInfo.updatesCount()))
                .containsEntry(
                        PRODUCT_LATEST_RELEASE_AT_PROPERTY,
                        Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(productInfo.latestReleaseAt()))
                                .atZone(TIME_ZONE)
                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
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
        assertThat(ServerSuplaDeviceHandler.buildProductInfoProperties(null, 6000, TIME_ZONE_PROVIDER))
                .isEmpty();
        assertThat(ServerSuplaDeviceHandler.buildProductInfoProperties(4, null, TIME_ZONE_PROVIDER))
                .isEmpty();
        assertThat(ServerSuplaDeviceHandler.buildProductInfoProperties(999, 999, TIME_ZONE_PROVIDER))
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
        assertThat(properties.get(SOFTWARE_UPDATE_LAST_CHECK_PROPERTY))
                .isEqualTo(checkedAt.atZone(TIME_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
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
        assertThat(properties.get(SOFTWARE_UPDATE_LAST_CHECK_PROPERTY))
                .isEqualTo(checkedAt.atZone(TIME_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
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
    void shouldExposeStableActionServicesForServerDevices() {
        assertThat(handler.getServices())
                .containsExactlyInAnyOrder(
                        SuplaServerDeviceConfigActions.class,
                        SuplaServerElectricityMeterActions.class,
                        SuplaServerConfigModeActions.class,
                        SuplaServerFirmwareUpdateActions.class);
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
    void shouldDeferFirmwareCheckResultUntilMessageIdIsKnown() throws Exception {
        handler.markOtaCheckPending();
        var immediateResult = new DeviceCalCfgResult(
                FIRMWARE_CHECK_MESSAGE_ID,
                -1,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                SUPLA_CALCFG_RESULT_DONE.getValue(),
                0L,
                new byte[0]);

        handler.consumeDeviceCalCfgResult(immediateResult);
        handler.markOtaCheckMessageId(FIRMWARE_CHECK_MESSAGE_ID);

        assertThat(handler.listenForDeviceCalCfgResult(1, SECONDS)).isEqualTo(immediateResult);
        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("CHECKING");
        assertThat(handler.isOtaCheckPending()).isTrue();
    }

    @Test
    void shouldIgnoreDeferredFirmwareCheckResultFromPreviousRequest() {
        handler.markOtaCheckPending();

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
        handler.markOtaCheckMessageId(FIRMWARE_CHECK_MESSAGE_ID);

        assertThat(properties.get(OTA_STATUS_PROPERTY)).isEqualTo("CHECKING");
        assertThat(properties)
                .doesNotContainKey(OTA_VERSION_AVAILABLE_PROPERTY)
                .doesNotContainKey(OTA_CHANGELOG_URL_PROPERTY)
                .doesNotContainKey(OTA_LAST_CHECK_PROPERTY);
        assertThat(handler.isOtaCheckPending()).isTrue();
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
