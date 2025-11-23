package pl.grzeslowski.openhab.supla.internal.server;

import static java.lang.Short.parseShort;
import static java.lang.String.valueOf;
import static java.util.Comparator.comparing;
import static org.openhab.core.thing.ChannelUID.CHANNEL_GROUP_SEPARATOR;
import static org.openhab.core.types.UnDefType.UNDEF;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.*;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.javatuples.Pair;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult;
import pl.grzeslowski.jsupla.protocol.api.structs.dsc.ChannelState;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerDevice;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;

@NonNullByDefault
@RequiredArgsConstructor
public class ChannelUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelUtil.class);
    private final ServerDevice invoker;

    public void buildChannels(List<DeviceChannel> deviceChannels) {
        {
            var adjustLabel = deviceChannels.size() > 1;
            var digits = deviceChannels.isEmpty() ? 1 : ((int) Math.log10(deviceChannels.size()) + 1);
            var idx = new AtomicInteger(1);
            var channels = deviceChannels.stream()
                    .flatMap(deviceChannel -> createChannel(deviceChannel, adjustLabel, idx.getAndIncrement(), digits))
                    .toList();
            if (invoker.getLogger().isDebugEnabled()) {
                var rawChannels = deviceChannels.stream()
                        .map(DeviceChannel::toString)
                        .collect(Collectors.joining("\n - ", " - ", ""));
                var string = channels.stream()
                        .map(channel -> channel.getUID() + " -> " + channel.getChannelTypeUID())
                        .collect(Collectors.joining("\n - ", " - ", ""));
                invoker.getLogger()
                        .debug(
                                """
                                        Registering channels:
                                         > Raw:
                                        {}

                                         > OpenHABs:
                                        {}""",
                                rawChannels,
                                string);
            }
            updateChannels(channels);
        }
        deviceChannels.stream()
                .flatMap(this::channelForUpdate)
                .forEach(pair -> invoker.updateState(pair.uid(), pair.state()));
    }

    private Stream<Channel> createChannel(DeviceChannel deviceChannel, boolean adjustLabel, int idx, int digits) {
        var channelCallback = new ChannelCallback(invoker.getThing().getUID(), deviceChannel.number());
        var channelValueSwitch = new ChannelClassSwitch<>(channelCallback);
        var clazz = ChannelTypeDecoder.INSTANCE.findClass(deviceChannel.type());
        var channels = channelValueSwitch.doSwitch(clazz);
        invoker.getChannelTypes().put(deviceChannel.number(), deviceChannel.type());
        if (adjustLabel) {
            return channels.map(channel -> new Pair<>(ChannelBuilder.create(channel), channel.getLabel()))
                    .map(pair -> pair.getValue0().withLabel(("#%0" + digits + "d ").formatted(idx) + pair.getValue1()))
                    .map(ChannelBuilder::build);
        }
        return channels;
    }

    private Stream<ChannelValueToState.ChannelState> channelForUpdate(DeviceChannel deviceChannel) {
        Class<? extends ChannelValue> clazz = ChannelTypeDecoder.INSTANCE.findClass(deviceChannel.type());
        if (clazz.isAssignableFrom(ElectricityMeterValue.class)) {
            return Stream.empty();
        }
        return findState(
                deviceChannel.type(), deviceChannel.number(), deviceChannel.value(), deviceChannel.hvacValue());
    }

    public Stream<ChannelValueToState.ChannelState> findState(
            int type,
            int channelNumber,
            @Nullable @jakarta.annotation.Nullable byte[] value,
            @jakarta.annotation.Nullable @Nullable HvacValue hvacValue) {
        val valueSwitch = new ChannelValueSwitch<>(
                new ChannelValueToState(invoker.getThing().getUID(), channelNumber));
        ChannelValue channelValue;
        if (value != null) {
            channelValue = ChannelTypeDecoder.INSTANCE.decode(type, value);
        } else if (hvacValue != null) {
            channelValue = hvacValue;
        } else {
            throw new IllegalArgumentException("value and hvacValue cannot be null!");
        }
        return valueSwitch.doSwitch(channelValue);
    }

    public void updateChannels(List<Channel> channels) {
        new ArrayList<>(channels).sort((o1, o2) -> comparing((Channel id) -> {
                    try {
                        var stringId = id.getUID().getId();
                        if (stringId.contains(CHANNEL_GROUP_SEPARATOR)) {
                            stringId = stringId.split(CHANNEL_GROUP_SEPARATOR)[0];
                        }
                        return Integer.parseInt(stringId);
                    } catch (NumberFormatException e) {
                        return Integer.MAX_VALUE;
                    }
                })
                .compare(o1, o2));

        var thingBuilder = invoker.editThing();
        thingBuilder.withChannels(channels);
        invoker.updateThing(thingBuilder.build());
    }

    public void updateStatus(DeviceChannelValue trait) {
        if (trait.offline()) {
            invoker.getLogger().debug("Channel Value is offline, ignoring it. trait={}", trait);
            return;
        }
        var type = invoker.getChannelTypes().get(trait.channelNumber());
        updateStatus(trait.channelNumber(), type, trait.value());
    }

    public void updateStatus(int channelNumber, int type, byte[] channelValue) {
        invoker.getLogger().debug("Updating status for channelNumber={}, type={}", channelNumber, type);
        findState(type, channelNumber, channelValue, null).forEach(pair -> {
            var channelUID = pair.uid();
            var state = pair.state();
            invoker.getLogger()
                    .debug(
                            "Updating state for channel {}, channelNumber {}, type {}, state={}",
                            channelUID,
                            channelNumber,
                            type,
                            state);
            invoker.updateState(channelUID, state);
            invoker.saveState(channelUID, state);
        });
    }

    public void setCaption(SetCaption value) {
        var id = findId(value.id(), value.channelNumber()).orElse(null);
        if (id == null) {
            invoker.getLogger().debug("Cannot set caption, because ID is null. value={}", value);
            return;
        }
        var channelUID = new ChannelUID(invoker.getThing().getUID(), valueOf(id));
        var channel = invoker.getThing().getChannel(channelUID);
        var channels = new ArrayList<Channel>(1);
        if (channel == null) {
            // look for group channels
            channels.addAll(invoker.getThing().getChannels().stream()
                    .filter(c -> {
                        var uid = c.getUID();
                        if (!uid.isInGroup()) return false;
                        var guid = uid.getGroupId();
                        if (guid == null) return false;
                        return guid.equals(valueOf(id));
                    })
                    .toList());
            if (channels.isEmpty()) {
                invoker.getLogger()
                        .warn(
                                "There is no channel with ID {} that I can set value to. value={}, caption={}",
                                id,
                                value,
                                parseString(value.caption()));
                return;
            }
        } else {
            channels.add(channel);
        }
        var channelsWithCaption = channels.stream()
                .map(c -> ChannelBuilder.create(c)
                        .withLabel(parseString(value.caption()) + " > " + c.getLabel())
                        .build())
                .toList();
        var updatedChannelsIds =
                channelsWithCaption.stream().map(Channel::getUID).collect(Collectors.toSet());
        var newChannels = new ArrayList<>(invoker.getThing().getChannels().stream()
                .filter(c -> !updatedChannelsIds.contains(c.getUID()))
                .toList());
        newChannels.addAll(channelsWithCaption);
        updateChannels(newChannels);
    }

    public static Optional<Integer> findId(@Nullable Integer id, @Nullable Short channelNumber) {
        return Optional.ofNullable(id)
                .or(() -> Optional.ofNullable(channelNumber).map(Integer::valueOf));
    }

    public void consumeSuplaChannelNewValueResult(SuplaChannelNewValueResult value) {
        var channelAndPreviousState = invoker.getSenderIdToChannelUID().remove(value.senderId());
        if (value.success() != 0) {
            // operation was successful; can terminate
            return;
        }
        if (channelAndPreviousState == null) {
            invoker.getLogger()
                    .info(
                            "Some previous new value result failed. " + "Refreshing channel nr {}. value={}",
                            value.channelNumber(),
                            value);
            invoker.getThing().getChannels().stream()
                    .map(Channel::getUID)
                    .filter(uid -> correctChannelNumber(uid, value.channelNumber()))
                    .forEach(invoker::handleRefreshCommand);
            return;
        }

        var channelUID = channelAndPreviousState.channelUID();
        var previousState = channelAndPreviousState.previousState();
        if (previousState == null) {
            previousState = UNDEF;
        }
        invoker.updateState(channelUID, previousState);
        invoker.getLogger()
                .info(
                        "Some previous new value result failed. "
                                + "Refreshing channel ID {}. value={}, previousState={}",
                        channelUID,
                        value,
                        previousState);
        invoker.handleRefreshCommand(channelUID);
    }

    private boolean correctChannelNumber(ChannelUID channel, short channelNumber) {
        return findSuplaChannelNumber(channel)
                .filter(number -> number == channelNumber)
                .isPresent();
    }

    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static Optional<Short> findSuplaChannelNumber(ChannelUID channelUID) {
        return Optional.ofNullable(channelUID.getGroupId())
                .or(() -> Optional.of(channelUID.getId()))
                .map(groupId -> {
                    try {
                        return parseShort(groupId);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                });
    }

    public void consumeChannelState(ChannelState value) {
        var fields = value.fields();
        setField(fields, SUPLA_CHANNELSTATE_FIELD_IPV4, "IPV4", parseIpv4(value.iPv4()));
        setField(fields, SUPLA_CHANNELSTATE_FIELD_MAC, "MAC", parseMac(value.mAC()));
        setField(fields, SUPLA_CHANNELSTATE_FIELD_BATTERYLEVEL, "Battery Level", value.batteryLevel() + "%");
        setField(fields, SUPLA_CHANNELSTATE_FIELD_BATTERYPOWERED, "Battery Powered", value.batteryPowered() != 1);
        setField(fields, SUPLA_CHANNELSTATE_FIELD_WIFIRSSI, "WI-FI RSSI", value.wiFiRSSI());
        setField(
                fields,
                SUPLA_CHANNELSTATE_FIELD_WIFISIGNALSTRENGTH,
                "WI-FI Signal Strength",
                value.wiFiSignalStrength() + "%");
        setField(
                fields,
                SUPLA_CHANNELSTATE_FIELD_BRIDGENODESIGNALSTRENGTH,
                "Bridge Node Signal Strength",
                value.bridgeNodeSignalStrength() + "%");
        setField(fields, SUPLA_CHANNELSTATE_FIELD_UPTIME, "Up Time", Duration.ofSeconds(value.uptime()));
        setField(
                fields,
                SUPLA_CHANNELSTATE_FIELD_CONNECTIONUPTIME,
                "Connection Up Time",
                Duration.ofSeconds(value.connectionUptime()));
        setField(fields, SUPLA_CHANNELSTATE_FIELD_BATTERYHEALTH, "Battery Health", value.batteryHealth());
        setField(fields, SUPLA_CHANNELSTATE_FIELD_BRIDGENODEONLINE, "Bridge Node Online", value.bridgeNodeOnline());

        var lastConnectionResetCause =
                switch (value.lastConnectionResetCause()) {
                    case 0 -> "UNKNOWN";
                    case 1 -> "ACTIVITY_TIMEOUT";
                    case 2 -> "WIFI_CONNECTION_LOST";
                    case 3 -> "SERVER_CONNECTION_LOST";
                    default -> "UNKNOWN(%s)".formatted(value.lastConnectionResetCause());
                };
        setField(
                fields,
                SUPLA_CHANNELSTATE_FIELD_LASTCONNECTIONRESETCAUSE,
                "Last Connection Reset Cause",
                lastConnectionResetCause);

        invoker.setProperty(
                "Light Source Lifespan",
                Duration.ofHours(value.lightSourceLifespan()).toString());
        if (value.lightSourceLifespanLeft() != null) {
            var string =
                    value.lightSourceLifespanLeft() == -32767 ? "100%" : (value.lightSourceLifespanLeft() * 0.01) + "%";
            invoker.setProperty("Light Source Lifespan", string);
        }
        if (value.lightSourceOperatingTime() != null) {
            invoker.setProperty(
                    "Light Source Operating Time",
                    Duration.ofSeconds(value.lightSourceOperatingTime()).toString());
        }
        if (value.operatingTime() != null) {
            invoker.setProperty(
                    "Operating Time", Duration.ofSeconds(value.operatingTime()).toString());
        }
    }

    private void setField(int fields, int mask, String key, Object value) {
        if ((fields & mask) == 0) {
            return;
        }
        invoker.setProperty(key, valueOf(value));
    }
}
