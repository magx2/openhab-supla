package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.EV_TYPE_ELECTRICITY_METER_MEASUREMENT_V1;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_ACTIONTRIGGER;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_ELECTRICITY_METER;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.ACTION_TRIGGER_ID;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerDevice;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;
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
        lenient().when(serverDevice.getLogger()).thenReturn(logger);
        lenient().when(serverDevice.getThing()).thenReturn(thing);
        lenient().when(serverDevice.editThing()).thenReturn(thingBuilder);
        lenient().when(thingBuilder.withChannels(any(List.class))).thenReturn(thingBuilder);
    }

    @Test
    void shouldSkipOfflineStatusUpdate() {
        var channelValue = new DeviceChannelValue(4, new byte[] {0x01}, true, null);

        channelUtil.updateStatus(channelValue);

        verify(serverDevice, never()).updateState(any(), any());
        verify(serverDevice, never()).saveState(any(), any(), any(Duration.class));
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
        var channel = mock(Channel.class);
        when(channel.getUID()).thenReturn(channelUID);
        when(thing.getChannels()).thenReturn(new ArrayList<>(java.util.List.of(channel)));
        var newValueResult =
                new pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaChannelNewValueResult((short) 2, 8, (byte) 0);

        channelUtil.consumeSuplaChannelNewValueResult(newValueResult);

        verify(serverDevice).handleRefreshCommand(channelUID);
    }

    @Test
    void shouldCreateActionTriggerChannelInsteadOfUnknownChannel() {
        var thingUid = new ThingUID("supla:test:1");
        when(thing.getUID()).thenReturn(thingUid);
        when(thingBuilder.build()).thenReturn(thing);

        var actionTriggerChannel = new DeviceChannel(
                1,
                false,
                SUPLA_CHANNELTYPE_ACTIONTRIGGER,
                java.util.Set.of(),
                null,
                java.util.Set.of(),
                null,
                new ActionTrigger(ActionTrigger.Capabilities.SHORT_PRESS_x1.toMask()),
                null,
                null,
                0L,
                java.util.Set.of(),
                0);

        channelUtil.buildChannels(List.of(actionTriggerChannel));

        @SuppressWarnings("unchecked")
        var channelsCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(thingBuilder).withChannels(channelsCaptor.capture());
        @SuppressWarnings("unchecked")
        var channels = (List<Channel>) channelsCaptor.getValue();

        assertThat(channels).singleElement().satisfies(channel -> {
            assertThat(channel.getUID()).isEqualTo(new ChannelUID(thingUid, "1"));
            assertThat(channel.getChannelTypeUID())
                    .isEqualTo(new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, ACTION_TRIGGER_ID));
            assertThat(channel.getKind()).isEqualTo(ChannelKind.TRIGGER);
        });
    }

    @Test
    void shouldDecodeExtendedElectricityMeterValueForExtendedUpdates() {
        var thingUid = new ThingUID("supla:test:1");
        when(thing.getUID()).thenReturn(thingUid);
        when(thingBuilder.build()).thenReturn(thing);

        var electricityMeterChannel = new DeviceChannel(
                0,
                false,
                SUPLA_CHANNELTYPE_ELECTRICITY_METER,
                java.util.Set.of(),
                null,
                java.util.Set.of(),
                new byte[] {7, -60, -38, 2, 0, 0, 0, 0},
                null,
                null,
                null,
                0L,
                java.util.Set.of(),
                0);
        channelUtil.buildChannels(List.of(electricityMeterChannel));

        var rawPacketData = new byte[] {
            0, 10, -83, 1, 0, 0, -25, 27, 14, 11, 0, 0, 0, 0, 71, 13, 4, 0, 0, 0, 0, 0, -98, 8, 22, 0, 0, 0, 0, 0, -58,
            17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -79, 39, -77, 5, 0, 0, 0, 0, -121,
            -99, 0, 0, 0, 0, 0, 0, -47, -67, 4, 0, 0, 0, 0, 0, -54, -24, 29, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -50,
            -118, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, 0, 0, 1, 0, 0, 0, 5, 0, 0, 0, -124, 19,
            -108, 94, -94, 94, 70, 94, -46, 0, 0, 0, 16, 0, 76, -123, 55, 0, 120, 21, 0, 0, -90, 9, 0, 0, -26, -126, 3,
            0, 34, 3, 0, 0, -88, -57, -4, -1, -8, 78, 76, 0, 102, 38, 1, 0, 102, 102, 2, 0, -11, 2, -24, 3, -24, 3, 1,
            -7, -59, 4, 52, 1, -124, 19, -104, 94, -91, 94, 112, 94, -44, 0, 16, 0, 16, 0, -104, -113, 55, 0, -104, 24,
            0, 0, -120, -56, -4, -1, 18, 111, 3, 0, -2, -52, -4, -1, 24, -5, -1, -1, -26, 39, 77, 0, 0, 0, 0, 0, 0, 0,
            0, 0, -6, 2, -24, 3, -24, 3, -5, -8, -55, 5, -8, -8, -124, 19, -88, 94, -81, 94, 126, 94, -48, 0, 16, 0, 0,
            0, 2, 87, 55, 0, -40, 23, 0, 0, -32, 13, 0, 0, -96, 6, 3, 0, -60, -53, -4, -1, -100, 6, 0, 0, 86, -78, 75,
            0, 50, -77, 0, 0, 0, -128, 0, 0, -7, 2, -24, 3, -24, 3, 3, -7, -8, -8, 125, -6, -123, 19, -110, 94, -65, 94,
            86, 94, -47, 0, 16, 0, 16, 0, -110, 80, 55, 0, 60, 19, 0, 0, 28, 4, 0, 0, 18, 16, 3, 0, 96, -54, -4, -1,
            -102, -57, -4, -1, -10, -15, 75, 0, 50, -13, 0, 0, 50, -13, 2, 0, -6, 2, -24, 3, -24, 3, -5, -8, -75, -4,
            -8, -8, -123, 19, -83, 94, -66, 94, 93, 94, -43, 0, 16, 0, 0, 0, -32, 106, 55, 0, 114, 10, 0, 0, -16, 5, 0,
            0, 12, 64, 3, 0, 126, -54, -4, -1, -56, 3, 0, 0, -106, 100, 77, 0, 0, -64, 2, 0, 102, -26, 1, 0, -6, 2, -24,
            3, -24, 3, -4, -8, -83, -7, -8, -8
        };
        channelUtil.updateExtendedStatus(
                0,
                EV_TYPE_ELECTRICITY_METER_MEASUREMENT_V1,
                Arrays.copyOfRange(rawPacketData, 6, rawPacketData.length));

        verify(serverDevice)
                .updateState(
                        eq(new ChannelUID(new ChannelGroupUID(thingUid, "0"), "totalForwardActiveEnergy")),
                        argThat(state -> state instanceof QuantityType<?> quantityType
                                && quantityType.toBigDecimal().compareTo(new BigDecimal("1871.83564")) == 0));
    }
}
