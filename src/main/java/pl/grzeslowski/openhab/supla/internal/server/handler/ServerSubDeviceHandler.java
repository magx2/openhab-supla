package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.BRIDGE_UNINITIALIZED;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaPingServer;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SubdeviceDetails;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.server.api.Writer;
import pl.grzeslowski.openhab.supla.internal.handler.AbstractDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.server.ChannelUtil;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValueTrait;

@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public class ServerSubDeviceHandler extends AbstractDeviceHandler implements ChannelUtil.Invoker, HandleProto {
    @Getter
    private final Map<Integer, Integer> channelTypes = synchronizedMap(new HashMap<>());

    private final ChannelUtil channelUtil = new ChannelUtil(this);

    @Getter
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Getter
    @ToString.Include
    private int subDeviceId;

    @Nullable
    private ServerGatewayDeviceHandler bridgeHandler;

    @Getter
    private List<DeviceChannelTrait> channels = List.of();

    public ServerSubDeviceHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void internalInitialize() {
        {
            var bridge = getBridge();
            if (bridge == null) {
                updateStatus(
                        OFFLINE,
                        BRIDGE_UNINITIALIZED,
                        "There is no bridge for this thing. Remove it and add it again.");
                return;
            }
            var rawBridgeHandler = bridge.getHandler();
            if (!(rawBridgeHandler instanceof ServerGatewayDeviceHandler localBridgeHandler)) {
                String simpleName;
                if (rawBridgeHandler != null) {
                    simpleName = rawBridgeHandler.getClass().getSimpleName();
                } else {
                    simpleName = "<null>";
                }
                updateStatus(
                        OFFLINE,
                        CONFIGURATION_ERROR,
                        "Bridge has wrong type! Should be " + ServerGatewayDeviceHandler.class.getSimpleName()
                                + ", but was " + simpleName);
                return;
            }
            this.bridgeHandler = localBridgeHandler;
            localBridgeHandler.deviceConnected();
        } // bridge
        {
            var config = getConfigAs(ServerSubDeviceHandlerConfiguration.class);
            subDeviceId = config.getId();
            if (subDeviceId < 1) {
                updateStatus(OFFLINE, CONFIGURATION_ERROR, "There is no ID for this thing.");
                return;
            }
        } // config
        logger = LoggerFactory.getLogger("%s.%s.%s"
                .formatted(
                        this.getClass().getName(), requireNonNull(bridgeHandler).getGuid(), subDeviceId));
        updateStatus(ONLINE);
    }

    public void setChannels(List<DeviceChannelTrait> channels) {
        this.channels = channels;
        channelUtil.buildChannels(channels);
        if (channels.isEmpty()) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "No channels.");
        } else {
            updateStatus(ONLINE);
        }
    }

    @Override
    protected void handleRefreshCommand(ChannelUID channelUID) {
        // ignored
    }

    @Override
    protected void handleOnOffCommand(ChannelUID channelUID, OnOffType command) {}

    @Override
    protected void handleUpDownCommand(ChannelUID channelUID, UpDownType command) {}

    @Override
    protected void handleHsbCommand(ChannelUID channelUID, HSBType command) {}

    @Override
    protected void handleOpenClosedCommand(ChannelUID channelUID, OpenClosedType command) {}

    @Override
    protected void handlePercentCommand(ChannelUID channelUID, PercentType command) {}

    @Override
    protected void handleDecimalCommand(ChannelUID channelUID, DecimalType command) {}

    @Override
    protected void handleStopMoveTypeCommand(ChannelUID channelUID, StopMoveType command) {}

    @Override
    protected void handleStringCommand(ChannelUID channelUID, StringType command) {}

    @Override
    public void dispose() {
        var localBridgeHandler = bridgeHandler;
        bridgeHandler = null;
        if (localBridgeHandler != null) {
            localBridgeHandler.deviceDisconnected();
        }
        channels = List.of();
        logger = LoggerFactory.getLogger(this.getClass());
    }

    @Override
    public void updateState(ChannelUID uid, State state) {
        super.updateState(uid, state);
    }

    @Override
    public ThingBuilder editThing() {
        return super.editThing();
    }

    @Override
    public void updateThing(Thing thing) {
        super.updateThing(thing);
    }

    @Override
    public void consumeSuplaPingServer(SuplaPingServer ping, Writer writer) {
        throw new UnsupportedOperationException("I should not support this method!");
    }

    @Override
    public void consumeSuplaSetActivityTimeout(Writer writer) {
        throw new UnsupportedOperationException("I should not support this method!");
    }

    @Override
    public void consumeDeviceChannelValueTrait(DeviceChannelValueTrait trait) {
        channelUtil.updateStatus(trait);
    }

    @Override
    public void consumeSuplaDeviceChannelExtendedValue(int channelNumber, int type, byte[] value) {}

    @Override
    public void consumeLocalTimeRequest(Writer writer) {
        throw new UnsupportedOperationException("I should not support this method!");
    }

    @Override
    public void consumeSetCaption(SetCaption value) {
        channelUtil.setCaption(value);
    }

    @Override
    public void consumeChannelState(ChannelState value) {}

    @Override
    public void consumeSubDeviceDetails(SubdeviceDetails value) {
        throw new UnsupportedOperationException("I should not support this method!");
    }
}
