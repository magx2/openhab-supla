package pl.grzeslowski.openhab.supla.actions;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgCommand.SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgCommand.SUPLA_CALCFG_CMD_START_FIRMWARE_UPDATE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgCommand.SUPLA_CALCFG_CMD_START_SECURITY_UPDATE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.jsupla.protocol.api.DeviceFlag.SUPLA_DEVICE_FLAG_AUTOMATIC_FIRMWARE_UPDATE_SUPPORTED;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ACTION_SCOPE_FIRMWARE_UPDATE;

import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import pl.grzeslowski.jsupla.protocol.api.CalCfgCommand;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.DeviceCalCfgResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.DeviceCalCfgRequest;
import pl.grzeslowski.openhab.supla.internal.server.handler.ServerSuplaDeviceHandler;

@ThingActionsScope(name = ACTION_SCOPE_FIRMWARE_UPDATE)
@NonNullByDefault
public class SuplaServerFirmwareUpdateActions extends SuplaServerActionsSupport {
    @RuleAction(
            label = "@text/action.check-firmware-update.label",
            description = "@text/action.check-firmware-update.description")
    public synchronized String checkFirmwareUpdate() {
        return runAction("checkFirmwareUpdate", this::checkFirmwareUpdateOrThrow);
    }

    private String checkFirmwareUpdateOrThrow() throws InterruptedException, TimeoutException {
        var localHandler = requireOtaReadyHandler();
        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }

        var message = new DeviceCalCfgRequest(
                nextSenderId(localHandler),
                NOT_BOUND_TO_CHANNEL,
                SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue(),
                SUPER_USER_AUTHORIZED,
                NO_DATA_TYPE,
                EMPTY_DATA.length,
                EMPTY_DATA);

        localHandler.clearDeviceCalCfgResult();
        localHandler.markOtaCheckPending(message.senderId());
        var timeout = localHandler.getConfiguration().getCheckFirmwareUpdateActionTimeout();
        var timeoutMillis = timeout.toMillis();
        var checkFirmwareUpdateStart = System.nanoTime();
        try {
            var future = writer.write(message);
            future.await(timeoutMillis, MILLISECONDS);
            if (!future.isSuccess()) {
                throw new RuntimeException("Check firmware update dispatch failed! request=%s, cause=%s"
                        .formatted(message, future.cause()));
            }
        } catch (InterruptedException | RuntimeException e) {
            localHandler.markOtaCheckError();
            throw e;
        }

        DeviceCalCfgResult result;
        try {
            result = localHandler.listenForDeviceCalCfgResult(timeoutMillis, MILLISECONDS);
        } catch (InterruptedException | TimeoutException | RuntimeException e) {
            localHandler.markOtaCheckError();
            throw e;
        }
        if (result.receiverId() != message.senderId()) {
            localHandler.markOtaCheckError();
            throw new RuntimeException("Check firmware update returned a different receiver id! request=%s, result=%s"
                    .formatted(message, result));
        }
        if (result.channelNumber() != NOT_BOUND_TO_CHANNEL) {
            localHandler.markOtaCheckError();
            throw new RuntimeException(
                    "Check firmware update returned a different channel number! request=%s, result=%s"
                            .formatted(message, result));
        }
        if (result.command() != SUPLA_CALCFG_CMD_CHECK_FIRMWARE_UPDATE.getValue()) {
            localHandler.markOtaCheckError();
            throw new RuntimeException("Check firmware update returned a different command! request=%s, result=%s"
                    .formatted(message, result));
        }
        if (result.result() != SUPLA_CALCFG_RESULT_DONE.getValue()) {
            localHandler.markOtaCheckError();
            throw new RuntimeException(
                    "Check firmware update did not succeed! request=%s, result=%s".formatted(message, result));
        }

        var checkFirmwareUpdateElapsed = System.nanoTime() - checkFirmwareUpdateStart;
        var remainingTimeoutMillis =
                Math.max(0L, timeoutMillis - MILLISECONDS.convert(checkFirmwareUpdateElapsed, NANOSECONDS));
        if (remainingTimeoutMillis <= 0) {
            localHandler.markOtaCheckError();
            throw new TimeoutException("Check firmware update timeout budget exhausted before OTA result wait");
        }
        try {
            var otaStatus = localHandler
                    .listenForOtaCheckResult(remainingTimeoutMillis, MILLISECONDS)
                    .name();
            return text("action.check-firmware-update.result.success", otaStatus);
        } catch (InterruptedException | TimeoutException | RuntimeException e) {
            localHandler.markOtaCheckError();
            throw e;
        }
    }

    @RuleAction(
            label = "@text/action.start-firmware-update.label",
            description = "@text/action.start-firmware-update.description")
    public synchronized String startFirmwareUpdate() {
        return sendWholeDeviceCalCfgCommand(SUPLA_CALCFG_CMD_START_FIRMWARE_UPDATE, "Start firmware update");
    }

    @RuleAction(
            label = "@text/action.start-security-update.label",
            description = "@text/action.start-security-update.description")
    public synchronized String startSecurityUpdate() {
        return sendWholeDeviceCalCfgCommand(SUPLA_CALCFG_CMD_START_SECURITY_UPDATE, "Start security update");
    }

    public static String checkFirmwareUpdate(@Nullable ThingActions actions) {
        if (actions instanceof SuplaServerFirmwareUpdateActions serverActions) {
            return serverActions.checkFirmwareUpdate();
        }
        return unavailableActionService("checkFirmwareUpdate", actions, SuplaServerFirmwareUpdateActions.class);
    }

    public static String startFirmwareUpdate(@Nullable ThingActions actions) {
        if (actions instanceof SuplaServerFirmwareUpdateActions serverActions) {
            return serverActions.startFirmwareUpdate();
        }
        return unavailableActionService("startFirmwareUpdate", actions, SuplaServerFirmwareUpdateActions.class);
    }

    public static String startSecurityUpdate(@Nullable ThingActions actions) {
        if (actions instanceof SuplaServerFirmwareUpdateActions serverActions) {
            return serverActions.startSecurityUpdate();
        }
        return unavailableActionService("startSecurityUpdate", actions, SuplaServerFirmwareUpdateActions.class);
    }

    private String sendWholeDeviceCalCfgCommand(CalCfgCommand command, String actionName) {
        return runAction(actionName, () -> sendWholeDeviceCalCfgCommandOrThrow(command, actionName));
    }

    private String sendWholeDeviceCalCfgCommandOrThrow(CalCfgCommand command, String actionName)
            throws InterruptedException, TimeoutException {
        var localHandler = requireOtaReadyHandler();
        var timeout =
                switch (command) {
                    case SUPLA_CALCFG_CMD_START_FIRMWARE_UPDATE ->
                        localHandler.getConfiguration().getStartFirmwareUpdateActionTimeout();
                    case SUPLA_CALCFG_CMD_START_SECURITY_UPDATE ->
                        localHandler.getConfiguration().getStartSecurityUpdateActionTimeout();
                    default -> localHandler.getConfiguration().getActionTimeout();
                };
        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }

        var message = new DeviceCalCfgRequest(
                nextSenderId(localHandler),
                NOT_BOUND_TO_CHANNEL,
                command.getValue(),
                SUPER_USER_AUTHORIZED,
                NO_DATA_TYPE,
                EMPTY_DATA.length,
                EMPTY_DATA);
        localHandler.clearDeviceCalCfgResult();
        writer.write(message).await(timeout.toMillis(), MILLISECONDS);

        var result = localHandler.listenForDeviceCalCfgResult(timeout.toMillis(), MILLISECONDS);
        if (result.channelNumber() != NOT_BOUND_TO_CHANNEL) {
            throw new RuntimeException("%s returned a different channel number! request=%s, result=%s"
                    .formatted(actionName, message, result));
        }
        if (result.command() != command.getValue()) {
            throw new RuntimeException(
                    "%s returned a different command! request=%s, result=%s".formatted(actionName, message, result));
        }
        if (result.result() != SUPLA_CALCFG_RESULT_DONE.getValue()) {
            throw new RuntimeException(
                    "%s did not succeed! request=%s, result=%s".formatted(actionName, message, result));
        }
        localHandler.markOtaUpdateTriggered();
        return switch (command) {
            case SUPLA_CALCFG_CMD_START_FIRMWARE_UPDATE -> text("action.start-firmware-update.result.success");
            case SUPLA_CALCFG_CMD_START_SECURITY_UPDATE -> text("action.start-security-update.result.success");
            default -> text("action.result.success");
        };
    }

    private ServerSuplaDeviceHandler requireOtaReadyHandler() {
        var localHandler = getThingHandlerOrWarn();
        if (localHandler == null) {
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
}
