package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.slf4j.Logger;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.SuplaDevice;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;

@ExtendWith(MockitoExtension.class)
class ChannelUtilTest {
    @Mock
    private SuplaDevice suplaDevice;

    @Mock
    private Logger logger;

    @Mock
    private Thing thing;

    @Mock
    private ThingBuilder thingBuilder;

    private HashMap<Integer, Integer> channelTypes;

    @InjectMocks
    private ChannelUtil channelUtil;

    @BeforeEach
    void setUp() {
        channelTypes = new HashMap<>();

        when(suplaDevice.getLogger()).thenReturn(logger);
        when(suplaDevice.getThing()).thenReturn(thing);
        when(suplaDevice.getChannelTypes()).thenReturn(channelTypes);
        when(suplaDevice.editThing()).thenReturn(thingBuilder);
        when(thingBuilder.withChannels(any())).thenReturn(thingBuilder);
    }

    @Test
    void shouldSkipOfflineStatusUpdate() {
        var channelValue = new DeviceChannelValue(4, new byte[] {0x01}, true, null);

        channelUtil.updateStatus(channelValue);

        verify(suplaDevice, never()).updateState(any(), any());
        verify(suplaDevice, never()).saveState(any(), any());
    }

    @Test
    void shouldFindIdFromChannelNumberWhenIdMissing() {
        Optional<Integer> result = ChannelUtil.findId(null, (short) 12);

        assertThat(result).hasValue(12);
    }

    @Test
    void shouldPreferExplicitId() {
        Optional<Integer> result = ChannelUtil.findId(7, (short) 13);

        assertThat(result).hasValue(7);
    }

    @Test
    void shouldParseChannelNumberFromGroup() {
        var thingUid = new org.openhab.core.thing.ThingUID("supla:test:1");
        ChannelUID channelUID = new ChannelUID(new ChannelGroupUID(thingUid, "2"), "power");

        Optional<Short> number = ChannelUtil.findSuplaChannelNumber(channelUID);

        assertThat(number).hasValue((short) 2);
    }

    @Test
    void shouldParseChannelNumberFromId() {
        var thingUid = new org.openhab.core.thing.ThingUID("supla:test:1");
        ChannelUID channelUID = new ChannelUID(thingUid, "11");

        Optional<Short> number = ChannelUtil.findSuplaChannelNumber(channelUID);

        assertThat(number).hasValue((short) 11);
    }

    @Test
    void shouldIgnoreNonNumericIds() {
        var thingUid = new org.openhab.core.thing.ThingUID("supla:test:1");
        ChannelUID channelUID = new ChannelUID(thingUid, "abc");

        Optional<Short> number = ChannelUtil.findSuplaChannelNumber(channelUID);

        assertThat(number).isEmpty();
    }

    @Test
    void shouldDropStoredSenderEntryOnSuccess() {
        var map = new HashMap<Integer, SuplaDevice.ChannelAndPreviousState>();
        var channelUID = new ChannelUID("supla:test:1:1");
        map.put(5, new SuplaDevice.ChannelAndPreviousState(channelUID, null));
        when(suplaDevice.getSenderIdToChannelUID()).thenReturn(map);
        var newValueResult = org.mockito.Mockito.mock(
                pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult.class);
        when(newValueResult.senderId()).thenReturn(5);
        when(newValueResult.success()).thenReturn((byte) 1);

        channelUtil.consumeSuplaChannelNewValueResult(newValueResult);

        assertThat(map).isEmpty();
        verify(suplaDevice, never()).handleRefreshCommand(any(ChannelUID.class));
    }

    @Test
    void shouldRefreshWhenSenderMissing() {
        var map = new HashMap<Integer, SuplaDevice.ChannelAndPreviousState>();
        when(suplaDevice.getSenderIdToChannelUID()).thenReturn(map);
        var channelUID = new ChannelUID("supla:test:1:2");
        var channel = org.mockito.Mockito.mock(Channel.class);
        when(channel.getUID()).thenReturn(channelUID);
        when(thing.getChannels()).thenReturn(new ArrayList<>(java.util.List.of(channel)));
        var newValueResult = org.mockito.Mockito.mock(
                pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult.class);
        when(newValueResult.senderId()).thenReturn(8);
        when(newValueResult.channelNumber()).thenReturn((short) 2);
        when(newValueResult.success()).thenReturn((byte) 0);

        channelUtil.consumeSuplaChannelNewValueResult(newValueResult);

        verify(suplaDevice).handleRefreshCommand(channelUID);
    }
}
