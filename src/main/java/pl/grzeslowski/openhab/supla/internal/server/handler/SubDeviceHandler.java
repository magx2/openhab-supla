package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.*;

import io.netty.channel.ChannelFuture;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaPingServer;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.jsupla.protocol.api.types.FromServerProto;
import pl.grzeslowski.jsupla.server.SuplaWriter;
import pl.grzeslowski.openhab.supla.internal.handler.AbstractDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.server.ChannelUtil;
import pl.grzeslowski.openhab.supla.internal.server.cache.InMemoryStateCache;
import pl.grzeslowski.openhab.supla.internal.server.cache.StateCache;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandleCommand;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandlerCommandTrait;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.SuplaDevice;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerSubDeviceHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;

@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public class SubDeviceHandler extends AbstractDeviceHandler implements SuplaDevice {
    @Getter
    private final Map<Integer, Integer> channelTypes = synchronizedMap(new HashMap<>());

    private final ChannelUtil channelUtil = new ChannelUtil(this);

    @Delegate(types = HandleCommand.class)
    private final HandlerCommandTrait handlerCommandTrait = new HandlerCommandTrait(this);

    @Getter
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Getter
    @ToString.Include
    private int subDeviceId;

    @Nullable
    @ToString.Include
    private String guid;

    @Nullable
    @Getter
    private GatewayDeviceHandler bridgeHandler;

    @Getter
    private final AtomicInteger senderId = new AtomicInteger(1);

    @Getter
    private final Map<Integer, ChannelAndPreviousState> senderIdToChannelUID = synchronizedMap(new HashMap<>());

    @Getter
    private List<DeviceChannel> channels = List.of();

    @Delegate(types = StateCache.class)
    private final StateCache stateCache = new InMemoryStateCache(logger);

    public SubDeviceHandler(Thing thing) {
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
            if (!(rawBridgeHandler instanceof GatewayDeviceHandler localBridgeHandler)) {
                String simpleName;
                if (rawBridgeHandler != null) {
                    simpleName = rawBridgeHandler.getClass().getSimpleName();
                } else {
                    simpleName = "<null>";
                }
                updateStatus(
                        OFFLINE,
                        CONFIGURATION_ERROR,
                        "Bridge has wrong type! Should be " + GatewayDeviceHandler.class.getSimpleName() + ", but was "
                                + simpleName);
                return;
            }
            this.bridgeHandler = localBridgeHandler;
            localBridgeHandler.deviceConnected();
        } // bridge
        {
            var config = getConfigAs(ServerSubDeviceHandlerConfiguration.class);
            subDeviceId = config.id();
            if (subDeviceId < 1) {
                updateStatus(OFFLINE, CONFIGURATION_ERROR, "There is no ID for this thing.");
                return;
            }
        } // config
        guid = requireNonNull(bridgeHandler).getGuid() + "." + subDeviceId;
        logger = LoggerFactory.getLogger("%s.%s".formatted(this.getClass().getName(), guid));
        updateStatus(ThingStatus.UNKNOWN, CONFIGURATION_PENDING, "Waiting for gateway");
    }

    @Override
    protected @Nullable String findGuid() {
        if (guid != null) {
            return guid;
        }
        var bridgeGuid = Optional.ofNullable(getBridge())
                .map(Bridge::getHandler)
                .filter(GatewayDeviceHandler.class::isInstance)
                .map(GatewayDeviceHandler.class::cast)
                .map(ServerAbstractDeviceHandler::getGuid);
        if (bridgeGuid.isEmpty()) {
            return null;
        }
        if (subDeviceId < 1) {
            return null;
        }
        return bridgeGuid.get() + "." + subDeviceId;
    }

    public void setChannels(List<DeviceChannel> channels) {
        this.channels = channels;
        channelUtil.buildChannels(channels);
        if (channels.isEmpty()) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "No channels.");
        } else {
            updateStatus(ONLINE);
        }
    }

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
    public void consumeSuplaPingServer(SuplaPingServer ping, SuplaWriter SuplaWriter) {
        logger.warn("Not supporting `consumeSuplaPingServer()`");
    }

    @Override
    public void consumeSuplaSetActivityTimeout(SuplaWriter SuplaWriter) {
        logger.warn("Not supporting `consumeSuplaSetActivityTimeout()`");
    }

    @Override
    public void consumeDeviceChannelValueTrait(DeviceChannelValue trait) {
        channelUtil.updateStatus(trait);
    }

    @Override
    public void consumeSuplaDeviceChannelExtendedValue(int channelNumber, int type, byte[] value) {}

    @Override
    public void consumeLocalTimeRequest(SuplaWriter SuplaWriter) {
        logger.warn("Not supporting `consumeLocalTimeRequest()`");
    }

    @Override
    public void consumeSetCaption(SetCaption value) {
        channelUtil.setCaption(value);
    }

    @Override
    public void consumeChannelState(ChannelState value) {
        throw new UnsupportedOperationException("ServerSubDeviceHandler.consumeChannelState(value)");
    }

    @Override
    public void consumeSubDeviceDetails(SubdeviceDetails value) {
        logger.warn("Not supporting `consumeSubDeviceDetails({})`", value);
    }

    @Override
    public void consumeSuplaChannelNewValueResult(SuplaChannelNewValueResult value) {
        channelUtil.consumeSuplaChannelNewValueResult(value);
    }

    @Override
    public void updateStatus(ThingStatus thingStatus, ThingStatusDetail thingStatusDetail, String message) {
        super.updateStatus(thingStatus, thingStatusDetail, message);
    }

    @Override
    public void updateStatus(ThingStatus thingStatus) {
        super.updateStatus(thingStatus);
    }

    @Override
    public ChannelFuture write(FromServerProto proto) {
        var local = requireNonNull(bridgeHandler, "There is not bridge!");
        var writer = requireNonNull(local.getWriter().get(), "There is not writer!");
        logger.debug("Writing proto {}", proto);
        return writer.write(proto);
    }

    @Nullable
    @Override
    public String setProperty(String name, @Nullable String value) {
        return thing.setProperty(name, value);
    }

    @Override
    public void consumeSetDeviceConfigResult(SetDeviceConfigResult value) {
        throw new UnsupportedOperationException("ServerSubDeviceHandler.consumeSetDeviceConfigResult(value)");
    }

    @Override
    public void consumeSetDeviceConfig(SetDeviceConfig value) {
        throw new UnsupportedOperationException("ServerSubDeviceHandler.consumeSetDeviceConfig(value)");
    }

    @Override
    public void consumeSetChannelConfigResult(SetChannelConfigResult value) {
        throw new UnsupportedOperationException("ServerSubDeviceHandler.consumeSetChannelConfigResult(value)");
    }
}
