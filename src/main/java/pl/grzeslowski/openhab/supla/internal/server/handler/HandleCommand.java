package pl.grzeslowski.openhab.supla.internal.server.handler;

import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;

public interface HandleCommand {
    void handleRefreshCommand(final ChannelUID channelUID);

    void handleOnOffCommand(final ChannelUID channelUID, final OnOffType command);

    void handleUpDownCommand(final ChannelUID channelUID, final UpDownType command);

    void handleHsbCommand(final ChannelUID channelUID, final HSBType command);

    void handleOpenClosedCommand(final ChannelUID channelUID, final OpenClosedType command);

    void handlePercentCommand(final ChannelUID channelUID, final PercentType command);

    void handleDecimalCommand(final ChannelUID channelUID, final DecimalType command);

    void handleStopMoveTypeCommand(final ChannelUID channelUID, final StopMoveType command);

    void handleStringCommand(final ChannelUID channelUID, final StringType command);
}
