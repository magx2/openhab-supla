package pl.grzeslowski.openhab.supla.actions;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgCommand.SUPLA_CALCFG_CMD_RESET_COUNTERS;
import static pl.grzeslowski.jsupla.protocol.api.CalCfgResult.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ACTION_SCOPE_ELECTRICITY_METER;
import static pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerDevice.SENDER_ID;

import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.DeviceCalCfgRequest;
import pl.grzeslowski.openhab.supla.internal.server.ChannelUtil;

@Component(scope = ServiceScope.PROTOTYPE, service = SuplaServerElectricityMeterActions.class)
@ThingActionsScope(name = ACTION_SCOPE_ELECTRICITY_METER)
@NonNullByDefault
public class SuplaServerElectricityMeterActions extends SuplaServerActionsSupport {
    @RuleAction(
            label = "@text/action.reset-electric-meter-counters.label",
            description = "@text/action.reset-electric-meter-counters.channel-uid.description")
    public synchronized String resetElectricMeterCounters(
            @ActionInput(
                            name = "channelUID",
                            label = "@text/action.input.channel-uid.label",
                            description = "@text/action.input.channel-uid.description")
                    String channelUID) {
        return runAction("resetElectricMeterCounters", () -> resetElectricMeterCountersByChannelUid(channelUID));
    }

    private String resetElectricMeterCountersByChannelUid(String channelUID)
            throws InterruptedException, TimeoutException {
        var localHandler = getThingHandlerOrWarn();
        if (localHandler == null) {
            throw new IllegalStateException("Thing handler is null");
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
        return resetElectricMeterCountersByChannelNumber(channelNumber);
    }

    @RuleAction(
            label = "@text/action.reset-electric-meter-counters.label",
            description = "@text/action.reset-electric-meter-counters.channel-number.description")
    public synchronized String resetElectricMeterCounters(
            @ActionInput(
                            name = "channelNumber",
                            label = "@text/action.input.channel-number.label",
                            description = "@text/action.input.channel-number.description")
                    int channelNumber) {
        return runAction("resetElectricMeterCounters", () -> resetElectricMeterCountersByChannelNumber(channelNumber));
    }

    private String resetElectricMeterCountersByChannelNumber(int channelNumber)
            throws InterruptedException, TimeoutException {
        var localHandler = getThingHandlerOrWarn();
        if (localHandler == null) {
            throw new IllegalStateException("Thing handler is null");
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
                SUPLA_CALCFG_CMD_RESET_COUNTERS.getValue(),
                SUPER_USER_AUTHORIZED,
                NO_DATA_TYPE,
                EMPTY_DATA.length,
                EMPTY_DATA);
        localHandler.clearDeviceCalCfgResult();
        var timeout = localHandler.getConfiguration().getResetElectricMeterCountersActionTimeout();
        writer.write(message).await(timeout.toMillis(), MILLISECONDS);

        var result = localHandler.listenForDeviceCalCfgResult(timeout.toMillis(), MILLISECONDS);
        if (result.channelNumber() != channelNumber) {
            throw new RuntimeException("Reset counters returned a different channel number! request=%s, result=%s"
                    .formatted(message, result));
        }
        if (result.command() != SUPLA_CALCFG_CMD_RESET_COUNTERS.getValue()) {
            throw new RuntimeException(
                    "Reset counters returned a different command! request=%s, result=%s".formatted(message, result));
        }
        if (result.result() != SUPLA_CALCFG_RESULT_DONE.getValue()) {
            throw new RuntimeException(
                    "Reset counters did not succeed! request=%s, result=%s".formatted(message, result));
        }
        return text("action.reset-electric-meter-counters.result.success", channelNumber);
    }

    public static String resetElectricMeterCounters(@Nullable ThingActions actions, String channelUID) {
        if (actions instanceof SuplaServerElectricityMeterActions serverActions) {
            return serverActions.resetElectricMeterCounters(channelUID);
        }
        return unavailableActionService(
                "resetElectricMeterCounters", actions, SuplaServerElectricityMeterActions.class);
    }

    public static String resetElectricMeterCounters(@Nullable ThingActions actions, int channelNumber) {
        if (actions instanceof SuplaServerElectricityMeterActions serverActions) {
            return serverActions.resetElectricMeterCounters(channelNumber);
        }
        return unavailableActionService(
                "resetElectricMeterCounters", actions, SuplaServerElectricityMeterActions.class);
    }

    private static ChannelUID parseChannelUID(String channelUID) {
        try {
            return new ChannelUID(channelUID);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse channel UID from " + channelUID, e);
        }
    }
}
