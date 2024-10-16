package pl.grzeslowski.openhab.supla.internal.server;

import static java.lang.String.valueOf;
import static java.util.Comparator.comparing;
import static org.openhab.core.thing.ChannelUID.CHANNEL_GROUP_SEPARATOR;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.HVACValueDecoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ChannelClassSwitch;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ChannelValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ChannelValueSwitch;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ElectricityMeterValue;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SetCaption;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValueTrait;

@NonNullByDefault
@RequiredArgsConstructor
public class ChannelUtil {
    private final Invoker invoker;

    public interface Invoker {
        Logger getLogger();

        Thing getThing();

        Map<Integer, Integer> getChannelTypes();

        void updateState(ChannelUID uid, State state);

        ThingBuilder editThing();

        void updateThing(Thing thing);
    }

    public void buildChannels(List<DeviceChannelTrait> deviceChannels) {
        {
            var adjustLabel = deviceChannels.size() > 1;
            var digits = deviceChannels.isEmpty() ? 1 : ((int) Math.log10(deviceChannels.size()) + 1);
            var idx = new AtomicInteger(1);
            var channels = deviceChannels.stream()
                    .flatMap(deviceChannel -> createChannel(deviceChannel, adjustLabel, idx.getAndIncrement(), digits))
                    .toList();
            if (invoker.getLogger().isDebugEnabled()) {
                var rawChannels = deviceChannels.stream()
                        .map(DeviceChannelTrait::toString)
                        .collect(Collectors.joining("\n"));
                var string = channels.stream()
                        .map(channel -> channel.getUID() + " -> " + channel.getChannelTypeUID())
                        .collect(Collectors.joining("\n"));
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
                .forEach(pair -> invoker.updateState(pair.getValue0(), pair.getValue1()));
    }

    private Stream<Channel> createChannel(DeviceChannelTrait deviceChannel, boolean adjustLabel, int idx, int digits) {
        var channelCallback = new ChannelCallback(invoker.getThing().getUID(), deviceChannel.getNumber());
        var channelValueSwitch = new ChannelClassSwitch<>(channelCallback);
        var clazz = ChannelTypeDecoder.INSTANCE.findClass(deviceChannel.getType());
        var channels = channelValueSwitch.doSwitch(clazz);
        invoker.getChannelTypes().put(deviceChannel.getNumber(), deviceChannel.getType());
        if (adjustLabel) {
            return channels.map(channel -> new Pair<>(ChannelBuilder.create(channel), channel.getLabel()))
                    .map(pair ->
                            pair.getValue0().withLabel(("#%0" + digits + "d ").formatted(idx) + pair.getValue1()))
                    .map(ChannelBuilder::build);
        }
        return channels;
    }

    private Stream<Pair<ChannelUID, State>> channelForUpdate(DeviceChannelTrait deviceChannel) {
        Class<? extends ChannelValue> clazz = ChannelTypeDecoder.INSTANCE.findClass(deviceChannel.getType());
        if (clazz.isAssignableFrom(ElectricityMeterValue.class)) {
            return Stream.empty();
        }
        return findState(
                deviceChannel.getType(),
                deviceChannel.getNumber(),
                deviceChannel.getValue(),
                deviceChannel.getHvacValue());
    }

    public Stream<Pair<ChannelUID, State>> findState(
            int type,
            int channelNumber,
            @Nullable @jakarta.annotation.Nullable byte[] value,
            @jakarta.annotation.Nullable @Nullable HVACValue hvacValue) {
        val valueSwitch = new ChannelValueSwitch<>(
                new ChannelValueToState(invoker.getThing().getUID(), channelNumber));
        ChannelValue channelValue;
        if (value != null) {
            channelValue = ChannelTypeDecoder.INSTANCE.decode(type, value);
        } else if (hvacValue != null) {
            channelValue = HVACValueDecoderImpl.INSTANCE.decode(hvacValue);
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

    public void updateStatus(DeviceChannelValueTrait trait) {
        if (trait.isOffline()) {
            invoker.getLogger().debug("Channel Value is offline, ignoring it. trait={}", trait);
            return;
        }
        var type = invoker.getChannelTypes().get(trait.getChannelNumber());
        updateStatus(trait.getChannelNumber(), type, trait.getValue());
    }

    public void updateStatus(int channelNumber, int type, byte[] channelValue) {
        invoker.getLogger().debug("Updating status for channelNumber={}, type={}", channelNumber, type);
        findState(type, channelNumber, channelValue, null).forEach(pair -> {
            var channelUID = pair.getValue0();
            var state = pair.getValue1();
            invoker.getLogger()
                    .debug(
                            "Updating state for channel {}, channelNumber {}, type {}, state={}",
                            channelUID,
                            channelNumber,
                            type,
                            state);
            invoker.updateState(channelUID, state);
        });
    }

    public void setCaption(SetCaption value) {
        var id = findId(value.id, value.channelNumber).orElse(null);
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
                                parseString(value.caption));
                return;
            }
        } else {
            channels.add(channel);
        }
        var channelsWithCaption = channels.stream()
                .map(c -> ChannelBuilder.create(c)
                        .withLabel(parseString(value.caption) + " > " + c.getLabel())
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

    public static Optional<Integer> findId(@Nullable Integer id, @Nullable  Short channelNumber) {
        return Optional.ofNullable(id)
                .or(()->
                        Optional.ofNullable(channelNumber)
                                .map(Integer::valueOf));
    }
}