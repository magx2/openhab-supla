package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;

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
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;
import pl.grzeslowski.jsupla.protocol.api.types.FromServerProto;
import pl.grzeslowski.openhab.supla.internal.handler.SuplaDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.server.ChannelUtil;
import pl.grzeslowski.openhab.supla.internal.server.cache.InMemoryStateCache;
import pl.grzeslowski.openhab.supla.internal.server.cache.StateCache;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandleCommand;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.HandlerCommandTrait;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerDevice;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerSubDeviceHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;

@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public class SubDeviceHandler extends SuplaDeviceHandler implements ServerDevice {
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
                updateStatus(OFFLINE, BRIDGE_UNINITIALIZED, text("supla.offline.no-bridge"));
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
                        text(
                                "supla.server.bridge-type-wrong",
                                "[" + GatewayDeviceHandler.class.getSimpleName() + "]",
                                simpleName));
                return;
            }
            this.bridgeHandler = localBridgeHandler;
            localBridgeHandler.deviceConnected();
        } // bridge
        {
            var config = getConfigAs(ServerSubDeviceHandlerConfiguration.class);
            subDeviceId = config.getId();
            if (subDeviceId < 1) {
                updateStatus(OFFLINE, CONFIGURATION_ERROR, text("supla.offline.no-id"));
                return;
            }
        } // config
        guid = requireNonNull(bridgeHandler).getGuid() + "." + subDeviceId;
        logger = LoggerFactory.getLogger("%s.%s".formatted(this.getClass().getName(), guid));
        updateStatus(ThingStatus.UNKNOWN, CONFIGURATION_PENDING, text("supla.server.waiting-for-gateway"));
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
                .map(ServerSuplaDeviceHandler::getGuid);
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
            updateStatus(OFFLINE, CONFIGURATION_ERROR, text("supla.server.no-channels"));
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

    public void consumeDeviceChannelValueTrait(DeviceChannelValue trait) {
        channelUtil.updateStatus(trait);
    }

    public void consumeSetCaption(SetCaption value) {
        channelUtil.setCaption(value);
    }

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
}
