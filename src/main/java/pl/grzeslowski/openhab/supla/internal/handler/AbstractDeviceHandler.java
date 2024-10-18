package pl.grzeslowski.openhab.supla.internal.handler;

import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import pl.grzeslowski.openhab.supla.internal.server.handler.HandleCommand;

@NonNullByDefault
public abstract class AbstractDeviceHandler extends BaseThingHandler implements HandleCommand {

    public AbstractDeviceHandler(final Thing thing) {
        super(thing);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void initialize() {
        try {
            internalInitialize();
        } catch (Exception e) {
            getLogger().error("Error occurred while initializing Supla device!", e);
            updateStatus(
                    OFFLINE,
                    CONFIGURATION_ERROR,
                    "Error occurred while initializing Supla device! " + e.getLocalizedMessage());
        }
    }

    protected abstract void internalInitialize() throws Exception;

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        getLogger().debug("handleCommand({}, {})", channelUID, command);
        try {
            if (command instanceof RefreshType) {
                handleRefreshCommand(channelUID);
            } else if (command instanceof OnOffType onOffValue) {
                handleOnOffCommand(channelUID, onOffValue);
            } else if (command instanceof UpDownType upDownValue) {
                handleUpDownCommand(channelUID, upDownValue);
            } else if (command instanceof HSBType hsBValue) {
                handleHsbCommand(channelUID, hsBValue);
            } else if (command instanceof OpenClosedType openClosedValue) {
                handleOpenClosedCommand(channelUID, openClosedValue);
            } else if (command instanceof PercentType percentValue) {
                handlePercentCommand(channelUID, percentValue);
            } else if (command instanceof DecimalType decimalValue) {
                handleDecimalCommand(channelUID, decimalValue);
            } else if (command instanceof StopMoveType stopMoveValue) {
                handleStopMoveTypeCommand(channelUID, stopMoveValue);
            } else if (command instanceof StringType stringValue) {
                handleStringCommand(channelUID, stringValue);
            } else {
                getLogger()
                        .warn(
                                "Does not know how to handle command `{}` ({}) on channel `{}`!",
                                command,
                                command.getClass().getSimpleName(),
                                channelUID);
            }
        } catch (Exception ex) {
            getLogger()
                    .error(
                            "Error occurred while handling command `{}` ({}) on channel `{}`!",
                            command,
                            command.getClass().getSimpleName(),
                            channelUID,
                            ex);
        }
    }

    protected abstract Logger getLogger();
}
