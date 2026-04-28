package pl.grzeslowski.openhab.supla.internal.server;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgCommand.*;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_NOT_SUPPORTED;
import static pl.grzeslowski.jsupla.protocol.api.DeviceFlag.SUPLA_DEVICE_FLAG_AUTOMATIC_FIRMWARE_UPDATE_SUPPORTED;
import static pl.grzeslowski.jsupla.protocol.api.DeviceFlag.SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ACTION_SCOPE_CONFIG_MODE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ACTION_SCOPE_DEVICE_CONFIG;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ACTION_SCOPE_ELECTRICITY_METER;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ACTION_SCOPE_FIRMWARE_UPDATE;

import io.netty.channel.ChannelFuture;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingActionsScope;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.DeviceCalCfgResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.DeviceCalCfgRequest;
import pl.grzeslowski.jsupla.server.SuplaWriter;
import pl.grzeslowski.openhab.supla.actions.SuplaServerConfigModeActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerDeviceConfigActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerElectricityMeterActions;
import pl.grzeslowski.openhab.supla.actions.SuplaServerFirmwareUpdateActions;
import pl.grzeslowski.openhab.supla.internal.server.handler.ServerSuplaDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerDeviceHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.traits.SuplaDevice;

@ExtendWith(MockitoExtension.class)
class SuplaServerActionsTest {
    private static final int ACTION_SENDER_ID = 37;

    @Mock
    private ServerSuplaDeviceHandler handler;

    @Mock
    private SuplaWriter writer;

    @Mock
    private Thing thing;

    private SuplaServerElectricityMeterActions electricityMeterActions;
    private SuplaServerConfigModeActions configModeActions;
    private SuplaServerFirmwareUpdateActions firmwareUpdateActions;

    private ServerDeviceHandlerConfiguration configuration;
    private ChannelFuture successfulFuture;

    @BeforeEach
    void setUp() {
        var channel = new EmbeddedChannel();
        successfulFuture = new DefaultChannelPromise(channel).setSuccess(null);
        configuration = new ServerDeviceHandlerConfiguration();
        electricityMeterActions = new SuplaServerElectricityMeterActions();
        configModeActions = new SuplaServerConfigModeActions();
        firmwareUpdateActions = new SuplaServerFirmwareUpdateActions();

        electricityMeterActions.setThingHandler(handler);
        configModeActions.setThingHandler(handler);
        firmwareUpdateActions.setThingHandler(handler);
        org.mockito.Mockito.lenient().when(handler.getWriter()).thenReturn(new AtomicReference<>(writer));
        org.mockito.Mockito.lenient().when(handler.getSenderId()).thenReturn(new AtomicInteger(ACTION_SENDER_ID));
        org.mockito.Mockito.lenient().when(handler.getThing()).thenReturn(thing);
        org.mockito.Mockito.lenient().when(handler.getConfiguration()).thenReturn(configuration);
        org.mockito.Mockito.lenient().when(thing.getUID()).thenReturn(new ThingUID("supla:test:1"));
        org.mockito.Mockito.lenient().when(thing.getStatus()).thenReturn(ONLINE);
        org.mockito.Mockito.lenient()
                .when(handler.getSuplaDevice())
                .thenReturn(new SuplaDevice(
                        SuplaDevice.Type.EMAIL,
                        "guid",
                        "device",
                        "soft",
                        null,
                        null,
                        Set.of(
                                SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE,
                                SUPLA_DEVICE_FLAG_AUTOMATIC_FIRMWARE_UPDATE_SUPPORTED),
                        List.of()));
        org.mockito.Mockito.lenient()
                .when(handler.hasRegisteredElectricityMeterChannel(7))
                .thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(handler.supportsAutomaticFirmwareUpdates())
                .thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(writer.write(argThat(proto -> proto instanceof DeviceCalCfgRequest)))
                .thenReturn(successfulFuture);
    }

    @Test
    void shouldExposeOnlySupportedRuleActionsOnServiceClasses() {
        assertThat(ruleActionSignatures(SuplaServerDeviceConfigActions.class))
                .containsExactlyInAnyOrder(
                        "setDeviceConfig(java.lang.String[])", "setDeviceConfig(java.util.Collection)");
        assertThat(ruleActionSignatures(SuplaServerElectricityMeterActions.class))
                .containsExactlyInAnyOrder(
                        "resetElectricMeterCounters(java.lang.String)", "resetElectricMeterCounters(int)");
        assertThat(ruleActionSignatures(SuplaServerConfigModeActions.class)).containsExactly("enterConfigMode()");
        assertThat(ruleActionSignatures(SuplaServerFirmwareUpdateActions.class))
                .containsExactlyInAnyOrder("checkFirmwareUpdate()", "startFirmwareUpdate()", "startSecurityUpdate()");
        assertThat(ruleActionReturnTypes(
                        SuplaServerDeviceConfigActions.class,
                        SuplaServerElectricityMeterActions.class,
                        SuplaServerConfigModeActions.class,
                        SuplaServerFirmwareUpdateActions.class))
                .containsOnly(String.class);
    }

    @Test
    void shouldUseSeparateActionScopesForSplitServices() {
        assertThat(actionScopes(
                        SuplaServerDeviceConfigActions.class,
                        SuplaServerElectricityMeterActions.class,
                        SuplaServerConfigModeActions.class,
                        SuplaServerFirmwareUpdateActions.class))
                .containsExactlyInAnyOrder(
                        ACTION_SCOPE_DEVICE_CONFIG,
                        ACTION_SCOPE_ELECTRICITY_METER,
                        ACTION_SCOPE_CONFIG_MODE,
                        ACTION_SCOPE_FIRMWARE_UPDATE)
                .doesNotHaveDuplicates();
    }

    @Test
    void shouldSendResetCountersRequest() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0,
                        7,
                        SUPLA_CALCFG_CMD_RESET_COUNTERS.getValue(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        0L,
                        new byte[0]));

        assertThat(electricityMeterActions.resetElectricMeterCounters("supla:test:1:7#power"))
                .isEqualTo(text("action.reset-electric-meter-counters.result.success", 7));

        var inOrder = inOrder(handler, writer);
        inOrder.verify(handler).clearDeviceCalCfgResult();
        inOrder.verify(writer)
                .write(argThat(proto -> proto instanceof DeviceCalCfgRequest request
                        && request.senderId() == ACTION_SENDER_ID
                        && request.channelNumber() == 7
                        && request.command() == SUPLA_CALCFG_CMD_RESET_COUNTERS.getValue()
                        && request.superUserAuthorized() == 1
                        && request.dataType() == 0
                        && request.dataSize() == 0L
                        && request.data().length == 0));
    }

    @Test
    void shouldFailWhenDeviceRejectsResetCounters() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0,
                        7,
                        SUPLA_CALCFG_CMD_RESET_COUNTERS.getValue(),
                        SUPLA_CALCFG_RESULT_NOT_SUPPORTED.getValue(),
                        0L,
                        new byte[0]));

        assertThat(electricityMeterActions.resetElectricMeterCounters("supla:test:1:7#power"))
                .contains("Reset counters did not succeed");
    }

    @Test
    void shouldFailWhenChannelNumberCannotBeParsed() {
        assertThat(electricityMeterActions.resetElectricMeterCounters("supla:test:1:not-a-channel"))
                .contains("Cannot find channel number from");
    }

    @Test
    void shouldFailWhenChannelUidBelongsToDifferentThing() {
        assertThat(electricityMeterActions.resetElectricMeterCounters("supla:test:2:7#power"))
                .contains("does not belong to thing");
    }

    @Test
    void shouldSendResetCountersRequestForChannelNumber() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0,
                        7,
                        SUPLA_CALCFG_CMD_RESET_COUNTERS.getValue(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        0L,
                        new byte[0]));

        assertThat(electricityMeterActions.resetElectricMeterCounters(7))
                .isEqualTo(text("action.reset-electric-meter-counters.result.success", 7));

        verify(writer)
                .write(argThat(proto -> proto instanceof DeviceCalCfgRequest request
                        && request.channelNumber() == 7
                        && request.command() == SUPLA_CALCFG_CMD_RESET_COUNTERS.getValue()));
    }

    @Test
    void shouldFailWhenChannelNumberIsNotRegistered() {
        assertThat(electricityMeterActions.resetElectricMeterCounters(11))
                .contains("Cannot find electricity meter device channel for channel number 11");
    }

    @Test
    void shouldFailWhenRegisteredChannelIsNotElectricityMeter() {
        when(handler.hasRegisteredElectricityMeterChannel(9)).thenReturn(false);

        assertThat(electricityMeterActions.resetElectricMeterCounters(9))
                .contains("Cannot find electricity meter device channel for channel number 9");
    }

    @Test
    void shouldSendEnterConfigModeRequest() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0,
                        -1,
                        SUPLA_CALCFG_CMD_ENTER_CFG_MODE.getValue(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        0L,
                        new byte[0]));

        assertThat(configModeActions.enterConfigMode()).isEqualTo(text("action.enter-config-mode.result.success"));

        var inOrder = inOrder(handler, writer);
        inOrder.verify(handler).clearDeviceCalCfgResult();
        inOrder.verify(writer)
                .write(argThat(proto -> proto instanceof DeviceCalCfgRequest request
                        && request.senderId() == ACTION_SENDER_ID
                        && request.channelNumber() == -1
                        && request.command() == SUPLA_CALCFG_CMD_ENTER_CFG_MODE.getValue()
                        && request.superUserAuthorized() == 1
                        && request.dataType() == 0
                        && request.dataSize() == 0L
                        && request.data().length == 0));
    }

    @Test
    void shouldFailWhenDeviceDoesNotSupportEnteringConfigMode() {
        when(handler.getSuplaDevice())
                .thenReturn(new SuplaDevice(
                        SuplaDevice.Type.EMAIL, "guid", "device", "soft", null, null, Set.of(), List.of()));

        assertThat(configModeActions.enterConfigMode()).contains("Device does not support entering config mode");
    }

    @Test
    void shouldFailWhenDeviceRejectsEnterConfigMode() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0,
                        -1,
                        SUPLA_CALCFG_CMD_ENTER_CFG_MODE.getValue(),
                        SUPLA_CALCFG_RESULT_NOT_SUPPORTED.getValue(),
                        0L,
                        new byte[0]));

        assertThat(configModeActions.enterConfigMode()).contains("Enter config mode did not succeed");
    }

    @Test
    void shouldFailWhenFirmwareUpdateActionCalledForOfflineDevice() {
        when(thing.getStatus()).thenReturn(OFFLINE);

        assertThat(firmwareUpdateActions.checkFirmwareUpdate())
                .isEqualTo(text("action.result.failure", "Device is offline"));
    }

    @Test
    void shouldFailWhenDeviceDoesNotSupportFirmwareUpdates() {
        when(handler.getSuplaDevice())
                .thenReturn(new SuplaDevice(
                        SuplaDevice.Type.EMAIL,
                        "guid",
                        "device",
                        "soft",
                        null,
                        null,
                        Set.of(SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE),
                        List.of()));

        assertThat(firmwareUpdateActions.checkFirmwareUpdate())
                .isEqualTo(text("action.result.failure", "Device does not support automatic firmware updates"));
    }

    @Test
    void shouldSendCheckFirmwareUpdateRequest() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        ACTION_SENDER_ID,
                        -1,
                        SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        0L,
                        new byte[0]));
        when(handler.listenForOtaCheckResult(
                        org.mockito.ArgumentMatchers.longThat(timeout -> timeout > 0 && timeout <= 30_000),
                        org.mockito.ArgumentMatchers.eq(MILLISECONDS)))
                .thenReturn(ServerSuplaDeviceHandler.OtaStatus.AVAILABLE);

        assertThat(firmwareUpdateActions.checkFirmwareUpdate())
                .isEqualTo(text("action.check-firmware-update.result.success", "AVAILABLE"));

        var inOrder = inOrder(handler, writer);
        inOrder.verify(handler).clearDeviceCalCfgResult();
        inOrder.verify(handler).markOtaCheckPending(ACTION_SENDER_ID);
        inOrder.verify(writer)
                .write(argThat(proto -> proto instanceof DeviceCalCfgRequest request
                        && request.senderId() == ACTION_SENDER_ID
                        && request.channelNumber() == -1
                        && request.command() == SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue()));
        inOrder.verify(handler).listenForDeviceCalCfgResult(30_000, MILLISECONDS);
        inOrder.verify(handler)
                .listenForOtaCheckResult(
                        org.mockito.ArgumentMatchers.longThat(timeout -> timeout > 0 && timeout <= 30_000),
                        org.mockito.ArgumentMatchers.eq(MILLISECONDS));
    }

    @Test
    void shouldFailWhenCheckFirmwareUpdateAcceptanceBelongsToDifferentRequest() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        ACTION_SENDER_ID - 1,
                        -1,
                        SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        0L,
                        new byte[0]));

        assertThat(firmwareUpdateActions.checkFirmwareUpdate()).contains("different receiver id");
        verify(handler).markOtaCheckError();
    }

    @Test
    void shouldUseRemainingTimeoutForOtaCheckResult() throws Exception {
        configuration.setCheckFirmwareUpdateActionTimeout("PT0.1S");
        when(handler.listenForDeviceCalCfgResult(100, MILLISECONDS)).thenAnswer(invocation -> {
            Thread.sleep(40);
            return new DeviceCalCfgResult(
                    ACTION_SENDER_ID,
                    -1,
                    SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                    SUPLA_CALCFG_RESULT_DONE.getValue(),
                    0L,
                    new byte[0]);
        });
        when(handler.listenForOtaCheckResult(
                        org.mockito.ArgumentMatchers.longThat(timeout -> timeout < 100),
                        org.mockito.ArgumentMatchers.eq(MILLISECONDS)))
                .thenReturn(ServerSuplaDeviceHandler.OtaStatus.AVAILABLE);

        assertThat(firmwareUpdateActions.checkFirmwareUpdate())
                .isEqualTo(text("action.check-firmware-update.result.success", "AVAILABLE"));

        verify(handler)
                .listenForOtaCheckResult(
                        org.mockito.ArgumentMatchers.longThat(timeout -> timeout < 100),
                        org.mockito.ArgumentMatchers.eq(MILLISECONDS));
    }

    @Test
    void shouldFailImmediatelyWhenNoTimeoutBudgetRemainsForOtaCheck() throws Exception {
        configuration.setCheckFirmwareUpdateActionTimeout("PT0S");
        when(handler.listenForDeviceCalCfgResult(0, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        ACTION_SENDER_ID,
                        -1,
                        SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        0L,
                        new byte[0]));

        assertThat(firmwareUpdateActions.checkFirmwareUpdate()).contains("timeout budget exhausted");
        verify(handler, org.mockito.Mockito.never()).listenForOtaCheckResult(0, MILLISECONDS);
        verify(handler).markOtaCheckError();
    }

    @Test
    void shouldMarkOtaCheckErrorWhenCheckFirmwareUpdateDispatchFails() {
        when(writer.write(argThat(proto -> proto instanceof DeviceCalCfgRequest)))
                .thenThrow(new RuntimeException("dispatch failed"));

        assertThat(firmwareUpdateActions.checkFirmwareUpdate()).contains("dispatch failed");
        verify(handler).clearDeviceCalCfgResult();
        verify(handler).markOtaCheckPending(ACTION_SENDER_ID);
        verify(handler).markOtaCheckError();
    }

    @Test
    void shouldMarkOtaCheckErrorWhenDeviceCalCfgResultWaitTimesOut() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS)).thenThrow(new TimeoutException("timeout"));

        assertThat(firmwareUpdateActions.checkFirmwareUpdate()).contains("timeout");
        verify(handler).markOtaCheckError();
    }

    @Test
    void shouldMarkOtaCheckErrorWhenOtaCheckWaitTimesOut() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        ACTION_SENDER_ID,
                        -1,
                        SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        0L,
                        new byte[0]));
        when(handler.listenForOtaCheckResult(
                        org.mockito.ArgumentMatchers.longThat(timeout -> timeout > 0 && timeout <= 30_000),
                        org.mockito.ArgumentMatchers.eq(MILLISECONDS)))
                .thenThrow(new TimeoutException("timeout"));

        assertThat(firmwareUpdateActions.checkFirmwareUpdate()).contains("timeout");
        verify(handler).markOtaCheckError();
    }

    @Test
    void shouldFailWhenCheckFirmwareUpdateDispatchFutureIsFailed() {
        var channel = new EmbeddedChannel();
        var failedFuture = new DefaultChannelPromise(channel).setFailure(new IllegalStateException("write failed"));
        when(writer.write(argThat(proto -> proto instanceof DeviceCalCfgRequest)))
                .thenReturn(failedFuture);

        assertThat(firmwareUpdateActions.checkFirmwareUpdate()).contains("dispatch failed");
        verify(handler).markOtaCheckPending(ACTION_SENDER_ID);
        verify(handler).markOtaCheckError();
    }

    @Test
    void shouldSendStartFirmwareUpdateRequest() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0,
                        -1,
                        SUPLA_CALCFG_CMD_START_FIRMWARE_UPDATE.getValue(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        0L,
                        new byte[0]));

        assertThat(firmwareUpdateActions.startFirmwareUpdate())
                .isEqualTo(text("action.start-firmware-update.result.success"));

        var inOrder = inOrder(handler, writer);
        inOrder.verify(handler).clearDeviceCalCfgResult();
        inOrder.verify(writer)
                .write(argThat(proto -> proto instanceof DeviceCalCfgRequest request
                        && request.senderId() == ACTION_SENDER_ID
                        && request.channelNumber() == -1
                        && request.command() == SUPLA_CALCFG_CMD_START_FIRMWARE_UPDATE.getValue()));
        inOrder.verify(handler).markOtaUpdateTriggered();
    }

    @Test
    void shouldSendStartSecurityUpdateRequest() throws Exception {
        when(handler.listenForDeviceCalCfgResult(30_000, MILLISECONDS))
                .thenReturn(new DeviceCalCfgResult(
                        0,
                        -1,
                        SUPLA_CALCFG_CMD_START_SECURITY_UPDATE.getValue(),
                        SUPLA_CALCFG_RESULT_DONE.getValue(),
                        0L,
                        new byte[0]));

        assertThat(firmwareUpdateActions.startSecurityUpdate())
                .isEqualTo(text("action.start-security-update.result.success"));

        verify(writer)
                .write(argThat(proto -> proto instanceof DeviceCalCfgRequest request
                        && request.senderId() == ACTION_SENDER_ID
                        && request.channelNumber() == -1
                        && request.command() == SUPLA_CALCFG_CMD_START_SECURITY_UPDATE.getValue()));
        verify(handler).markOtaUpdateTriggered();
    }

    private static List<String> ruleActionSignatures(Class<?> actionService) {
        return ruleActionMethods(actionService).stream()
                .map(SuplaServerActionsTest::signature)
                .toList();
    }

    private static List<Class<?>> ruleActionReturnTypes(Class<?>... actionServices) {
        return Arrays.stream(actionServices)
                .flatMap(actionService -> ruleActionMethods(actionService).stream())
                .map(Method::getReturnType)
                .toList();
    }

    private static List<String> actionScopes(Class<?>... actionServices) {
        return Arrays.stream(actionServices)
                .map(actionService ->
                        actionService.getAnnotation(ThingActionsScope.class).name())
                .toList();
    }

    private static List<Method> ruleActionMethods(Class<?> actionService) {
        return Arrays.stream(actionService.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(RuleAction.class))
                .toList();
    }

    private static String signature(Method method) {
        var parameters = Arrays.stream(method.getParameterTypes())
                .map(Class::getCanonicalName)
                .collect(joining(","));
        return "%s(%s)".formatted(method.getName(), parameters);
    }
}
