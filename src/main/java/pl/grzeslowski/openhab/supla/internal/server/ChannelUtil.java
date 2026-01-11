package pl.grzeslowski.openhab.supla.internal.server;

import static java.lang.Short.parseShort;
import static java.lang.String.valueOf;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.joining;
import static org.openhab.core.thing.ChannelUID.CHANNEL_GROUP_SEPARATOR;
import static org.openhab.core.types.UnDefType.UNDEF;
import static pl.grzeslowski.jsupla.protocol.api.ChannelStateField.*;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.ChannelStateField;
import pl.grzeslowski.jsupla.protocol.api.LastConnectionResetCause;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ChannelClassSwitch;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ChannelValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ElectricityMeterValue;
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
    private final Map<Integer, DeviceChannel> deviceChannels = new java.util.concurrent.ConcurrentHashMap<>();
    private final List<ScheduledFuture<?>> schedules = new ArrayList<>();

    public void buildChannels(List<DeviceChannel> deviceChannels) {
        {
            var adjustLabel = deviceChannels.size() > 1;
            var digits = deviceChannels.isEmpty() ? 1 : ((int) Math.log10(deviceChannels.size()) + 1);
            var idx = new AtomicInteger(1);
            var channels = deviceChannels.stream()
                    .flatMap(deviceChannel -> createChannel(deviceChannel, adjustLabel, idx.getAndIncrement(), digits))
                    .toList();
            if (invoker.getLogger().isDebugEnabled()) {
                var rawChannels =
                        deviceChannels.stream().map(DeviceChannel::toString).collect(joining("\n - ", " - ", ""));
                var string = channels.stream()
                        .map(channel -> channel.getUID() + " -> " + channel.getChannelTypeUID())
                        .collect(joining("\n - ", " - ", ""));
                invoker.getLogger().debug("""
                        Registering channels:
                         > Raw:
                        {}

                         > OpenHABs:
                        {}""", rawChannels, string);
            }
            updateChannels(channels);
        }
        deviceChannels.stream()
                .flatMap(this::channelForUpdate)
                .forEach(pair -> invoker.updateState(pair.uid(), pair.state()));
    }

    private Stream<Channel> createChannel(DeviceChannel deviceChannel, boolean adjustLabel, int idx, int digits) {
        deviceChannels.put(deviceChannel.number(), deviceChannel);
        var channelCallback = new ChannelCallback(invoker.getThing().getUID(), deviceChannel);
        var channelValueSwitch = new ChannelClassSwitch<>(channelCallback);
        var clazz = ChannelTypeDecoder.INSTANCE.findClass(deviceChannel.type());
        var channels = channelValueSwitch.doSwitch(clazz);
        if (adjustLabel) {
            record ChannelAndLabel(
                    ChannelBuilder builder, @Nullable String label) {}
            return channels.map(channel -> new ChannelAndLabel(ChannelBuilder.create(channel), channel.getLabel()))
                    .map(pair -> pair.builder().withLabel(("#%0" + digits + "d ").formatted(idx) + pair.label()))
                    .map(ChannelBuilder::build);
        }
        return channels;
    }

    private Stream<ChannelValueToState.ChannelState> channelForUpdate(DeviceChannel deviceChannel) {
        Class<? extends ChannelValue> clazz = ChannelTypeDecoder.INSTANCE.findClass(deviceChannel.type());
        if (clazz.isAssignableFrom(ElectricityMeterValue.class)) {
            return Stream.empty();
        }
        return findState(deviceChannel);
    }

    public Stream<ChannelValueToState.ChannelState> findState(DeviceChannel deviceChannel) {
        return findState(deviceChannel, deviceChannel.value());
    }

    public Stream<ChannelValueToState.ChannelState> findState(
            DeviceChannel deviceChannel, @jakarta.annotation.Nullable byte[] value) {
        val valueSwitch = new ChannelValueToState(invoker.getThing().getUID(), deviceChannel);
        ChannelValue channelValue;
        if (value != null) {
            channelValue = ChannelTypeDecoder.INSTANCE.decode(deviceChannel.type(), value);
        } else if (deviceChannel.action() != null) {
            channelValue = deviceChannel.action();
        } else if (deviceChannel.hvacValue() != null) {
            channelValue = deviceChannel.hvacValue();
        } else {
            throw new IllegalArgumentException("value and hvacValue cannot be null!");
        }
        return valueSwitch.switchOn(channelValue);
    }

    public void updateChannels(List<Channel> channels) {
        var sortedChannels = channels.stream()
                .sorted((o1, o2) -> comparing(ChannelUtil::channelKeyExtractor).compare(o1, o2))
                .toList();

        var thingBuilder = invoker.editThing();
        thingBuilder.withChannels(sortedChannels);
        invoker.updateThing(thingBuilder.build());
    }

    private static Integer channelKeyExtractor(Channel id) {
        try {
            var stringId = id.getUID().getId();
            if (stringId.contains(CHANNEL_GROUP_SEPARATOR)) {
                stringId = stringId.split(CHANNEL_GROUP_SEPARATOR)[0];
            }
            return Integer.parseInt(stringId);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public void updateStatus(DeviceChannelValue trait) {
        if (trait.offline()) {
            invoker.getLogger().debug("Channel Value is offline, ignoring it. trait={}", trait);
            return;
        }
        updateStatus(trait.channelNumber(), trait.value(), trait.validityTimeSec());
    }

    public void updateStatus(int channelNumber, byte[] channelValue) {
        updateStatus(channelNumber, channelValue, null);
    }

    private void updateStatus(int channelNumber, byte[] channelValue, @Nullable Long validityTimeSec) {
        invoker.getLogger()
                .debug("Updating status for channelNumber={}, value={}", channelNumber, Arrays.toString(channelValue));
        var deviceChannel = deviceChannels.get(channelNumber);
        if (deviceChannel == null) {
            if (invoker.getLogger().isWarnEnabled()) {
                var collect = deviceChannels.isEmpty()
                        ? "<none>"
                        : deviceChannels.values().stream()
                                .map(Objects::toString)
                                .collect(joining("\n - ", "\n - ", ""));
                invoker.getLogger()
                        .warn(
                                "There is no device channel for channel number {}. Cannot update status.\nExisting device channels: {}",
                                channelNumber,
                                collect);
            }
            return;
        }
        findState(deviceChannel, channelValue).forEach(pair -> {
            var channelUID = pair.uid();
            var state = pair.state();
            invoker.getLogger()
                    .debug(
                            "Updating state for channel {}, channelNumber {}, state={}, value={}",
                            channelUID,
                            channelNumber,
                            state,
                            Arrays.toString(channelValue));
            invoker.updateState(channelUID, state);
            var validityTime =
                    (validityTimeSec != null && validityTimeSec > 0) ? Duration.ofSeconds(validityTimeSec) : null;
            invoker.saveState(channelUID, state, validityTime);
            if (validityTime != null) {
                var now = now();
                invoker.getLogger()
                        .debug(
                                "Setting thread to refresh channel {} in {} ({})",
                                channelUID,
                                validityTime,
                                now.plus(validityTime));
                synchronized (schedules) {
                    var schedule = ThreadPoolManager.getScheduledPool(BINDING_ID)
                            .schedule(
                                    () -> {
                                        invoker.getLogger()
                                                .debug(
                                                        "Refreshing channel {}, because validity time expired!",
                                                        channelUID);
                                        invoker.handleRefreshCommand(channelUID);
                                        ThreadPoolManager.getScheduledPool(BINDING_ID)
                                                .schedule(this::cleanDoneSchedules, 100, MILLISECONDS);
                                    },
                                    validityTime.toMillis(),
                                    MILLISECONDS);
                    schedules.add(schedule);
                }
            }
        });
    }

    private void cleanDoneSchedules() {
        synchronized (schedules) {
            // clean done schedules
            invoker.getLogger().debug("Cleaning done schedules");
            for (var iterator = schedules.iterator(); iterator.hasNext(); ) {
                var future = iterator.next();
                if (future.isDone() || future.isCancelled()) {
                    invoker.getLogger().debug("Removing schedule {} because it's already done");
                    iterator.remove();
                }
            }
        }
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
        var fields = ChannelStateField.findByMask(value.fields());
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

        var lastConnectionResetCause = LastConnectionResetCause.findByValue(value.lastConnectionResetCause())
                .map(Enum::name)
                .orElse("UNKNOWN(%s)".formatted(value.lastConnectionResetCause()));
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

    private void setField(Set<ChannelStateField> fields, ChannelStateField mask, String key, Object value) {
        if (!fields.contains(mask)) {
            return;
        }
        invoker.setProperty(key, valueOf(value));
    }

    public void dispose() {
        invoker.getLogger().debug("Disposing channel util");
        synchronized (schedules) {
            schedules.forEach(this::disposeSchedule);
            schedules.clear();
        }
    }

    private void disposeSchedule(ScheduledFuture<?> future) {
        if (future.isDone()) {
            invoker.getLogger().debug("Schedule {} was already done/cancelled", future);
            return;
        }
        invoker.getLogger().debug("Disposing schedule {}", future);
        try {
            future.cancel(true);
        } catch (Exception e) {
            invoker.getLogger().warn("Got exception when cancelling schedule {}", future);
        }
    }
}
