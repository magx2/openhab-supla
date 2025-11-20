package pl.grzeslowski.openhab.supla.internal.handler;

import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static pl.grzeslowski.openhab.supla.internal.GuidLogger.attachGuid;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandleCommand;

@NonNullByDefault
public abstract class AbstractDeviceHandler extends BaseThingHandler implements HandleCommand {

    public AbstractDeviceHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public final void initialize() {
        attachGuid(findGuid(), () -> {
            try {
                internalInitialize();
            } catch (InitializationException e) {
                updateStatus(e.getStatus(), e.getStatusDetail(), e.getMessage());
            } catch (Exception e) {
                getLogger().error("Error occurred while initializing Supla device!", e);
                updateStatus(
                        OFFLINE,
                        CONFIGURATION_ERROR,
                        "Error occurred while initializing Supla device! " + e.getLocalizedMessage());
            }
        });
    }

    @Nullable
    protected abstract String findGuid();

    protected abstract void internalInitialize() throws Exception;

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        attachGuid(findGuid(), () -> {
            getLogger().debug("handleCommand({}, {})", channelUID, command);
            try {
                switch (command) {
                    case RefreshType refreshType -> handleRefreshCommand(channelUID);
                    case OnOffType onOffValue -> handleOnOffCommand(channelUID, onOffValue);
                    case UpDownType upDownValue -> handleUpDownCommand(channelUID, upDownValue);
                    case HSBType hsBValue -> handleHsbCommand(channelUID, hsBValue);
                    case OpenClosedType openClosedValue -> handleOpenClosedCommand(channelUID, openClosedValue);
                    case PercentType percentValue -> handlePercentCommand(channelUID, percentValue);
                    case DecimalType decimalValue -> handleDecimalCommand(channelUID, decimalValue);
                    case StopMoveType stopMoveValue -> handleStopMoveTypeCommand(channelUID, stopMoveValue);
                    case StringType stringValue -> handleStringCommand(channelUID, stringValue);
                    case QuantityType<?> quantityType -> handleQuantityType(channelUID, quantityType);
                    default -> getLogger()
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
        });
    }

    protected abstract Logger getLogger();
}
