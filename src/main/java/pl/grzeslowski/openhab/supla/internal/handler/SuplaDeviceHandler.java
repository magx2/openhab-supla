package pl.grzeslowski.openhab.supla.internal.handler;

import static java.util.Optional.ofNullable;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static pl.grzeslowski.openhab.supla.internal.GuidLogger.attachGuid;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.*;

import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import pl.grzeslowski.openhab.supla.internal.Localization;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandleCommand;

@NonNullByDefault
public abstract class SuplaDeviceHandler extends BaseThingHandler implements HandleCommand {
    private static final Set<String> READ_ONLY_SEMANTIC_CHANNEL_TYPES =
            Set.of(PUMP_SWITCH_VALUE_CHANNEL_ID, HEAT_OR_COLD_SOURCE_SWITCH_VALUE_CHANNEL_ID);

    public SuplaDeviceHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public final void initialize() {
        attachGuid(findGuid(), () -> {
            try {
                internalInitialize();
            } catch (InitializationException e) {
                getLogger().debug("InitializationException", e);
                updateStatus(e.getStatus(), e.getStatusDetail(), e.getLocalizedMessage());
            } catch (Exception e) {
                getLogger().error("Error occurred while initializing Supla device!", e);
                updateStatus(
                        OFFLINE,
                        CONFIGURATION_ERROR,
                        Localization.text("supla.offline.initialization-error", e.getLocalizedMessage()));
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
                if (!(command instanceof RefreshType) && isReadOnlySemanticChannel(channelUID)) {
                    getLogger().warn("Ignoring command `{}` on read-only channel `{}`", command, channelUID);
                    return;
                }
                switch (command) {
                    case RefreshType refreshType -> handleRefreshCommand(channelUID);
                    case OnOffType onOffValue -> handleOnOffCommand(channelUID, onOffValue);
                    case UpDownType upDownValue -> handleUpDownCommand(channelUID, upDownValue);
                    case HSBType hsBValue -> handleHsbCommand(channelUID, hsBValue);
                    case OpenClosedType openClosedValue -> handleOpenClosedCommand(channelUID, openClosedValue);
                    case PercentType percentValue -> handlePercentCommand(channelUID, percentValue);
                    case StopMoveType stopMoveValue -> handleStopMoveTypeCommand(channelUID, stopMoveValue);
                    case StringType stringValue -> handleStringCommand(channelUID, stringValue);
                    case QuantityType<?> quantityType -> handleQuantityType(channelUID, quantityType);
                    default ->
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
        });
    }

    private boolean isReadOnlySemanticChannel(ChannelUID channelUID) {
        return ofNullable(getThing().getChannel(channelUID))
                .map(Channel::getChannelTypeUID)
                .map(typeUID -> READ_ONLY_SEMANTIC_CHANNEL_TYPES.contains(typeUID.getId()))
                .orElse(false);
    }

    protected abstract Logger getLogger();

    @Override
    public void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }
}
