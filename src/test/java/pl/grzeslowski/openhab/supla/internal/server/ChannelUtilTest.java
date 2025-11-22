package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerDevice;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelValue;

@ExtendWith(MockitoExtension.class)
class ChannelUtilTest {
    @Mock
    private ServerDevice serverDevice;

    @Mock
    private Logger logger;

    @Mock
    private Thing thing;

    @Mock
    private ThingBuilder thingBuilder;

    @InjectMocks
    private ChannelUtil channelUtil;

    @BeforeEach
    void setUp() {
        var channelTypes = new HashMap<Integer, Integer>();

        lenient().when(serverDevice.getLogger()).thenReturn(logger);
        lenient().when(serverDevice.getThing()).thenReturn(thing);
        lenient().when(serverDevice.getChannelTypes()).thenReturn(channelTypes);
        lenient().when(serverDevice.editThing()).thenReturn(thingBuilder);
        lenient().when(thingBuilder.withChannels(any(List.class))).thenReturn(thingBuilder);
    }

    @Test
    void shouldSkipOfflineStatusUpdate() {
        var channelValue = new DeviceChannelValue(4, new byte[] {0x01}, true, null);

        channelUtil.updateStatus(channelValue);

        verify(serverDevice, never()).updateState(any(), any());
        verify(serverDevice, never()).saveState(any(), any());
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
        var map = new HashMap<Integer, ServerDevice.ChannelAndPreviousState>();
        var channelUID = new ChannelUID("supla:test:1:1");
        map.put(5, new ServerDevice.ChannelAndPreviousState(channelUID, null));
        when(serverDevice.getSenderIdToChannelUID()).thenReturn(map);
        var newValueResult =
                new pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult((short) 0, 5, (byte) 1);

        channelUtil.consumeSuplaChannelNewValueResult(newValueResult);

        assertThat(map).isEmpty();
        verify(serverDevice, never()).handleRefreshCommand(any(ChannelUID.class));
    }

    @Test
    void shouldRefreshWhenSenderMissing() {
        var map = new HashMap<Integer, ServerDevice.ChannelAndPreviousState>();
        when(serverDevice.getSenderIdToChannelUID()).thenReturn(map);
        var channelUID = new ChannelUID("supla:test:1:2");
        var channel = org.mockito.Mockito.mock(Channel.class);
        when(channel.getUID()).thenReturn(channelUID);
        when(thing.getChannels()).thenReturn(new ArrayList<>(java.util.List.of(channel)));
        var newValueResult =
                new pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult((short) 2, 8, (byte) 0);

        channelUtil.consumeSuplaChannelNewValueResult(newValueResult);

        verify(serverDevice).handleRefreshCommand(channelUID);
    }
}
