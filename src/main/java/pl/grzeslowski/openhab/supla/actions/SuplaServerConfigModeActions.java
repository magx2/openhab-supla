package pl.grzeslowski.openhab.supla.actions;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgCommand.SUPLA_CALCFG_CMD_ENTER_CFG_MODE;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.jsupla.protocol.api.DeviceFlag.SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ACTION_SCOPE_CONFIG_MODE;
import static pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerDevice.SENDER_ID;

import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.DeviceCalCfgRequest;

@Component(scope = ServiceScope.PROTOTYPE, service = SuplaServerConfigModeActions.class)
@ThingActionsScope(name = ACTION_SCOPE_CONFIG_MODE)
@NonNullByDefault
public class SuplaServerConfigModeActions extends SuplaServerActionsSupport {
    @RuleAction(
            label = "@text/action.enter-config-mode.label",
            description = "@text/action.enter-config-mode.description")
    public synchronized String enterConfigMode() {
        return runAction("enterConfigMode", this::enterConfigModeOrThrow);
    }

    private String enterConfigModeOrThrow() throws InterruptedException, TimeoutException {
        var localHandler = getThingHandlerOrWarn();
        if (localHandler == null) {
            throw new IllegalStateException("Thing handler is null");
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
                SUPLA_CALCFG_CMD_ENTER_CFG_MODE.getValue(),
                SUPER_USER_AUTHORIZED,
                NO_DATA_TYPE,
                EMPTY_DATA.length,
                EMPTY_DATA);
        localHandler.clearDeviceCalCfgResult();
        var timeout = localHandler.getConfiguration().getEnterConfigModeActionTimeout();
        writer.write(message).await(timeout.toMillis(), MILLISECONDS);

        var result = localHandler.listenForDeviceCalCfgResult(timeout.toMillis(), MILLISECONDS);
        if (result.channelNumber() != NOT_BOUND_TO_CHANNEL) {
            throw new RuntimeException("Enter config mode returned a different channel number! request=%s, result=%s"
                    .formatted(message, result));
        }
        if (result.command() != SUPLA_CALCFG_CMD_ENTER_CFG_MODE.getValue()) {
            throw new RuntimeException(
                    "Enter config mode returned a different command! request=%s, result=%s".formatted(message, result));
        }
        if (result.result() != SUPLA_CALCFG_RESULT_DONE.getValue()) {
            throw new RuntimeException(
                    "Enter config mode did not succeed! request=%s, result=%s".formatted(message, result));
        }
        return text("action.enter-config-mode.result.success");
    }

    public static String enterConfigMode(@Nullable ThingActions actions) {
        if (actions instanceof SuplaServerConfigModeActions serverActions) {
            return serverActions.enterConfigMode();
        }
        return unavailableActionService("enterConfigMode", actions, SuplaServerConfigModeActions.class);
    }
}
