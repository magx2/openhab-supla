package pl.grzeslowski.openhab.supla.actions;

import static java.lang.System.arraycopy;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pl.grzeslowski.jsupla.protocol.api.DeviceFlag.SUPLA_DEVICE_FLAG_AUTOMATIC_FIRMWARE_UPDATE_SUPPORTED;
import static pl.grzeslowski.jsupla.protocol.api.DeviceFlag.SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_ENTER_CFG_MODE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_RESET_COUNTERS;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_START_FIRMWARE_UPDATE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_START_SECURITY_UPDATE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import pl.grzeslowski.jsupla.protocol.api.DeviceConfigField;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.DeviceCalCfgRequest;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SetDeviceConfig;
import pl.grzeslowski.openhab.supla.internal.server.ChannelUtil;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfig;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigResult;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigUtil;
import pl.grzeslowski.openhab.supla.internal.server.handler.ServerSuplaDeviceHandler;

@ThingActionsScope(name = BINDING_ID)
@NonNullByDefault
@Slf4j
public class SuplaServerDeviceActions implements ThingActions {
    private static final int NO_DATA_TYPE = 0;
    private static final byte[] EMPTY_DATA = new byte[0];
    private static final byte SUPER_USER_AUTHORIZED = 1;
    private static final int NOT_BOUND_TO_CHANNEL = -1;
    private static final int SENDER_ID = 1;

    @Getter
    @Nullable
    private ServerSuplaDeviceHandler thingHandler;

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (!(handler instanceof ServerSuplaDeviceHandler suplaHandler)) {
            var handlerClass = Optional.of(handler)
                    .map(ThingHandler::getClass)
                    .map(Class::getSimpleName)
                    .orElse("<null>");
            log.warn(
                    "Handler {} has wrong class, actualClass={}, expectedClass={}",
                    handlerClass,
                    ServerSuplaDeviceHandler.class.getSimpleName());
            return;
        }
        this.thingHandler = suplaHandler;
    }

    @RuleAction(
            label = "@text/action.set-device-config.label",
            description = "@text/action.set-device-config.description")
    public void setDeviceConfig(
            @ActionInput(
                            name = "deviceConfigs",
                            label = "@text/action.input.device-configs.label",
                            description = "@text/action.input.device-configs.description")
                    String... deviceConfigs)
            throws InterruptedException, TimeoutException {
        setDeviceConfig(Arrays.asList(deviceConfigs));
    }

    @RuleAction(
            label = "@text/action.set-device-config.label",
            description = "@text/action.set-device-config.description")
    public void setDeviceConfig(
            @ActionInput(
                            name = "configs",
                            label = "@text/action.input.device-configs.label",
                            description = "@text/action.input.device-configs.description")
                    Collection<String> configs)
            throws InterruptedException, TimeoutException {
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("You need to pass configs!");
        }
        var localHandler = thingHandler;
        if (localHandler == null) {
            log.warn("Thing handler is null!");
            return;
        }

        var deviceConfig = configs.stream()
                .map(DeviceConfigUtil::parseDeviceConfig)
                .collect(Collectors.toCollection(TreeSet::new));

        var encodedConfig = deviceConfig.stream().map(DeviceConfig::encode).toList();

        var size = encodedConfig.stream().mapToInt(e -> e.length).sum();

        var config = new byte[size];
        var offset = 0;
        for (var encoded : encodedConfig) {
            arraycopy(encoded, 0, config, offset, encoded.length);
            offset += encoded.length;
        }

        var fields = BigInteger.valueOf(deviceConfig.stream()
                .map(DeviceConfig::field)
                .map(DeviceConfigField::getValue)
                .reduce(0L, (field, mask) -> field | mask));

        var message = new SetDeviceConfig(
                (short) 1, // end of data
                new short[8],
                requireNonNullElse(localHandler.getAvailableFields(), fields),
                fields,
                size,
                config);

        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }
        writer.write(message).await(30, SECONDS);
        var deviceConfigResult = localHandler.listenForSetDeviceConfigResult(30, SECONDS);
        var result = DeviceConfigResult.findConfigResult(deviceConfigResult.result());
        if (!result.isSuccess()) {
            throw new RuntimeException("Setting device config did not succeed! configs=%s. %s"
                    .formatted(
                            configs.stream().map(Object::toString).collect(Collectors.joining(", ")),
                            "SetDeviceConfig=" + message));
        }
        localHandler.consumeSetDeviceConfig(fields.longValue(), config);
    }

    @RuleAction(
            label = "@text/action.reset-electric-meter-counters.label",
            description = "@text/action.reset-electric-meter-counters.channel-uid.description")
    public synchronized void resetElectricMeterCounters(
            @ActionInput(
                            name = "channelUID",
                            label = "@text/action.input.channel-uid.label",
                            description = "@text/action.input.channel-uid.description")
                    String channelUID)
            throws InterruptedException, TimeoutException {
        var localHandler = thingHandler;
        if (localHandler == null) {
            log.warn("Thing handler is null!");
            return;
        }
        var parsedChannelUID = parseChannelUID(channelUID);
        var thingUID = localHandler.getThing().getUID();
        if (!thingUID.equals(parsedChannelUID.getThingUID())) {
            throw new IllegalArgumentException(
                    "Channel UID %s does not belong to thing %s".formatted(parsedChannelUID, thingUID));
        }
        int channelNumber = ChannelUtil.findSuplaChannelNumber(parsedChannelUID)
                .map(Short::intValue)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find channel number from " + parsedChannelUID));
        resetElectricMeterCounters(channelNumber);
    }

    @RuleAction(
            label = "@text/action.reset-electric-meter-counters.label",
            description = "@text/action.reset-electric-meter-counters.channel-number.description")
    public synchronized void resetElectricMeterCounters(
            @ActionInput(
                            name = "channelNumber",
                            label = "@text/action.input.channel-number.label",
                            description = "@text/action.input.channel-number.description")
                    int channelNumber)
            throws InterruptedException, TimeoutException {
        var localHandler = thingHandler;
        if (localHandler == null) {
            log.warn("Thing handler is null!");
            return;
        }
        if (!localHandler.hasRegisteredElectricityMeterChannel(channelNumber)) {
            throw new IllegalArgumentException(
                    "Cannot find electricity meter device channel for channel number " + channelNumber);
        }

        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }

        var message = new DeviceCalCfgRequest(
                SENDER_ID,
                channelNumber,
                SUPLA_CALCFG_CMD_RESET_COUNTERS,
                SUPER_USER_AUTHORIZED,
                NO_DATA_TYPE,
                EMPTY_DATA.length,
                EMPTY_DATA);
        localHandler.clearDeviceCalCfgResult();
        writer.write(message).await(30, SECONDS);

        var result = localHandler.listenForDeviceCalCfgResult(30, SECONDS);
        if (result.channelNumber() != channelNumber) {
            throw new RuntimeException("Reset counters returned a different channel number! request=%s, result=%s"
                    .formatted(message, result));
        }
        if (result.command() != SUPLA_CALCFG_CMD_RESET_COUNTERS) {
            throw new RuntimeException(
                    "Reset counters returned a different command! request=%s, result=%s".formatted(message, result));
        }
        if (result.result() != SUPLA_CALCFG_RESULT_DONE) {
            throw new RuntimeException(
                    "Reset counters did not succeed! request=%s, result=%s".formatted(message, result));
        }
    }

    @RuleAction(
            label = "@text/action.enter-config-mode.label",
            description = "@text/action.enter-config-mode.description")
    public synchronized void enterConfigMode() throws InterruptedException, TimeoutException {
        var localHandler = thingHandler;
        if (localHandler == null) {
            log.warn("Thing handler is null!");
            return;
        }

        var suplaDevice = localHandler.getSuplaDevice();
        if (suplaDevice == null) {
            throw new IllegalStateException("There is no registered device!");
        }
        if (!suplaDevice.flags().contains(SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE)) {
            throw new IllegalArgumentException("Device does not support entering config mode");
        }

        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }

        var message = new DeviceCalCfgRequest(
                SENDER_ID,
                NOT_BOUND_TO_CHANNEL,
                SUPLA_CALCFG_CMD_ENTER_CFG_MODE,
                SUPER_USER_AUTHORIZED,
                NO_DATA_TYPE,
                EMPTY_DATA.length,
                EMPTY_DATA);
        localHandler.clearDeviceCalCfgResult();
        writer.write(message).await(30, SECONDS);

        var result = localHandler.listenForDeviceCalCfgResult(30, SECONDS);
        if (result.channelNumber() != NOT_BOUND_TO_CHANNEL) {
            throw new RuntimeException("Enter config mode returned a different channel number! request=%s, result=%s"
                    .formatted(message, result));
        }
        if (result.command() != SUPLA_CALCFG_CMD_ENTER_CFG_MODE) {
            throw new RuntimeException(
                    "Enter config mode returned a different command! request=%s, result=%s".formatted(message, result));
        }
        if (result.result() != SUPLA_CALCFG_RESULT_DONE) {
            throw new RuntimeException(
                    "Enter config mode did not succeed! request=%s, result=%s".formatted(message, result));
        }
    }

    @RuleAction(
            label = "@text/action.check-firmware-update.label",
            description = "@text/action.check-firmware-update.description")
    public synchronized String checkFirmwareUpdate() throws InterruptedException, TimeoutException {
        var localHandler = requireOtaReadyHandler();
        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }

        var message = new DeviceCalCfgRequest(
                SENDER_ID,
                NOT_BOUND_TO_CHANNEL,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE,
                SUPER_USER_AUTHORIZED,
                NO_DATA_TYPE,
                EMPTY_DATA.length,
                EMPTY_DATA);

        localHandler.clearDeviceCalCfgResult();
        localHandler.markOtaCheckPending();
        writer.write(message).await(30, SECONDS);

        var result = localHandler.listenForDeviceCalCfgResult(30, SECONDS);
        if (result.channelNumber() != NOT_BOUND_TO_CHANNEL) {
            localHandler.markOtaCheckError();
            throw new RuntimeException(
                    "Check firmware update returned a different channel number! request=%s, result=%s"
                            .formatted(message, result));
        }
        if (result.command() != SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE) {
            localHandler.markOtaCheckError();
            throw new RuntimeException("Check firmware update returned a different command! request=%s, result=%s"
                    .formatted(message, result));
        }
        if (result.result() != SUPLA_CALCFG_RESULT_DONE) {
            localHandler.markOtaCheckError();
            throw new RuntimeException(
                    "Check firmware update did not succeed! request=%s, result=%s".formatted(message, result));
        }

        return localHandler.listenForOtaCheckResult(30, SECONDS).name();
    }

    @RuleAction(
            label = "@text/action.start-firmware-update.label",
            description = "@text/action.start-firmware-update.description")
    public synchronized String startFirmwareUpdate() throws InterruptedException, TimeoutException {
        return sendWholeDeviceCalCfgCommand(SUPLA_CALCFG_CMD_START_FIRMWARE_UPDATE, "Start firmware update");
    }

    @RuleAction(
            label = "@text/action.start-security-update.label",
            description = "@text/action.start-security-update.description")
    public synchronized String startSecurityUpdate() throws InterruptedException, TimeoutException {
        return sendWholeDeviceCalCfgCommand(SUPLA_CALCFG_CMD_START_SECURITY_UPDATE, "Start security update");
    }

    private String sendWholeDeviceCalCfgCommand(int command, String actionName)
            throws InterruptedException, TimeoutException {
        var localHandler = requireOtaReadyHandler();
        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }

        var message = new DeviceCalCfgRequest(
                SENDER_ID,
                NOT_BOUND_TO_CHANNEL,
                command,
                SUPER_USER_AUTHORIZED,
                NO_DATA_TYPE,
                EMPTY_DATA.length,
                EMPTY_DATA);
        localHandler.clearDeviceCalCfgResult();
        writer.write(message).await(30, SECONDS);

        var result = localHandler.listenForDeviceCalCfgResult(30, SECONDS);
        if (result.channelNumber() != NOT_BOUND_TO_CHANNEL) {
            throw new RuntimeException("%s returned a different channel number! request=%s, result=%s"
                    .formatted(actionName, message, result));
        }
        if (result.command() != command) {
            throw new RuntimeException(
                    "%s returned a different command! request=%s, result=%s".formatted(actionName, message, result));
        }
        if (result.result() != SUPLA_CALCFG_RESULT_DONE) {
            throw new RuntimeException(
                    "%s did not succeed! request=%s, result=%s".formatted(actionName, message, result));
        }
        localHandler.markOtaUpdateTriggered();
        return "ACCEPTED";
    }

    private ServerSuplaDeviceHandler requireOtaReadyHandler() {
        var localHandler = thingHandler;
        if (localHandler == null) {
            log.warn("Thing handler is null!");
            throw new IllegalStateException("Thing handler is null");
        }
        if (localHandler.getThing().getStatus() == ThingStatus.OFFLINE
                || localHandler.getWriter().get() == null) {
            throw new IllegalStateException("Device is offline");
        }

        var suplaDevice = localHandler.getSuplaDevice();
        if (suplaDevice == null) {
            throw new IllegalStateException("There is no registered device!");
        }
        if (!suplaDevice.flags().contains(SUPLA_DEVICE_FLAG_AUTOMATIC_FIRMWARE_UPDATE_SUPPORTED)
                || !localHandler.supportsAutomaticFirmwareUpdates()) {
            throw new IllegalArgumentException("Device does not support automatic firmware updates");
        }
        return localHandler;
    }

    private static ChannelUID parseChannelUID(String channelUID) {
        try {
            return new ChannelUID(channelUID);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse channel UID from " + channelUID, e);
        }
    }

    public static void setDeviceConfig(@Nullable ThingActions actions, String... deviceConfigs)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).setDeviceConfig(deviceConfigs);
    }

    public static void setDeviceConfig(@Nullable ThingActions actions, Collection<String> configs)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).setDeviceConfig(configs);
    }

    public static void resetElectricMeterCounters(@Nullable ThingActions actions, String channelUID)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).resetElectricMeterCounters(channelUID);
    }

    public static void resetElectricMeterCounters(@Nullable ThingActions actions, int channelNumber)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).resetElectricMeterCounters(channelNumber);
    }

    public static void enterConfigMode(@Nullable ThingActions actions) throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).enterConfigMode();
    }

    public static String checkFirmwareUpdate(@Nullable ThingActions actions)
            throws InterruptedException, TimeoutException {
        return ((SuplaServerDeviceActions) requireNonNull(actions)).checkFirmwareUpdate();
    }

    public static String startFirmwareUpdate(@Nullable ThingActions actions)
            throws InterruptedException, TimeoutException {
        return ((SuplaServerDeviceActions) requireNonNull(actions)).startFirmwareUpdate();
    }

    public static String startSecurityUpdate(@Nullable ThingActions actions)
            throws InterruptedException, TimeoutException {
        return ((SuplaServerDeviceActions) requireNonNull(actions)).startSecurityUpdate();
    }
}
