package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.openhab.core.types.UnDefType.NULL;
import static org.openhab.core.types.UnDefType.UNDEF;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.DecimalValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.PercentValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.RgbValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.StoppableOpenClose;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.TemperatureAndHumidityValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.TemperatureValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.UnknownValue;
import pl.grzeslowski.openhab.supla.internal.server.ChannelValueToState.ChannelState;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

@ExtendWith(MockitoExtension.class)
class ChannelValueToStateTest {
    private final ThingUID thingUID = new ThingUID("supla:test:1");

    @Mock
    private DecimalValue decimalValue;

    @Mock
    private PercentValue percentValue;

    @Mock
    private RgbValue rgbValue;

    @Mock
    private TemperatureValue temperatureValue;

    @Mock
    private TemperatureAndHumidityValue temperatureAndHumidityValue;

    @Mock
    private UnknownValue unknownValue;

    @Test
    void shouldConvertDecimalValue() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(2));
        when(decimalValue.value()).thenReturn(BigDecimal.TEN);

        List<State> states =
                converter.onDecimalValue(decimalValue).map(ChannelState::state).toList();

        assertThat(states).containsExactly(new DecimalType(BigDecimal.TEN));
    }

    private DeviceChannel mockDeviceChannel(int number) {
        return new DeviceChannel(number, null, null, null, Set.of(), new byte[8], null, null, null);
    }

    @Test
    void shouldReturnNullForMissingDecimal() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(4));

        List<State> states =
                converter.onDecimalValue(null).map(ChannelState::state).toList();

        assertThat(states).containsExactly(NULL);
    }

    @Test
    void shouldConvertPercentAndRgb() {
        // given
        var rgbValue = new RgbValue(55, 77, 1, 2, 3, 0);
        var deviceChannel = new DeviceChannel(
                6,
                null,
                null,
                null,
                Set.of(SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING),
                new byte[8],
                null,
                null,
                null);
        var converter = new ChannelValueToState(thingUID, deviceChannel);

        // when
        List<State> states =
                converter.onRgbValue(rgbValue).map(ChannelState::state).toList();

        // then
        var hsbType = HSBType.fromRGB(1, 2, 3);
        var expected =
                new HSBType(hsbType.getHue(), hsbType.getSaturation(), new PercentType(rgbValue.colorBrightness()));
        assertThat(states).hasSize(2);
        assertThat(states.get(0)).isEqualTo(expected);
        assertThat(states.get(1)).isEqualTo(new PercentType(55));
    }

    @Test
    void shouldReturnUndefForSpecialTemperatureValue() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(7));
        when(temperatureValue.temperature()).thenReturn(BigDecimal.valueOf(-275.0));

        State state = converter
                .onTemperatureValue(temperatureValue)
                .findFirst()
                .orElseThrow()
                .state();

        assertThat(state).isEqualTo(UNDEF);
    }

    @Test
    void shouldMapTemperatureAndHumidityGroup() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(8));
        when(temperatureAndHumidityValue.temperature()).thenReturn(BigDecimal.valueOf(21.0));
        when(temperatureAndHumidityValue.humidity()).thenReturn(BigDecimal.valueOf(-1.0));

        List<State> states = converter
                .onTemperatureAndHumidityValue(temperatureAndHumidityValue)
                .map(ChannelState::state)
                .toList();

        assertThat(states)
                .containsExactly(
                        new QuantityType<>(BigDecimal.valueOf(21.0), org.openhab.core.library.unit.SIUnits.CELSIUS),
                        UNDEF);
    }

    @Test
    void shouldHandleStoppableOpenClose() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(9));

        State state = converter
                .onStoppableOpenClose(StoppableOpenClose.STOP)
                .findFirst()
                .orElseThrow()
                .state();

        assertThat(state).isEqualTo(OpenClosedType.CLOSED);
    }

    @Test
    void shouldHandleUnknownValueNulls() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(5));
        when(unknownValue.message()).thenReturn("oops");

        List<State> nullStates =
                converter.onUnknownValue(null).map(ChannelState::state).toList();
        List<State> messageStates =
                converter.onUnknownValue(unknownValue).map(ChannelState::state).toList();

        assertThat(nullStates).containsExactly(NULL);
        assertThat(messageStates).containsExactly(new StringType("oops"));
    }

    @Test
    void shouldReturnGroupedUidsForTemperatureAndHumidity() {
        var converter = new ChannelValueToState(thingUID, mockDeviceChannel(12));

        List<ChannelUID> uids = converter
                .onTemperatureAndHumidityValue(null)
                .map(ChannelState::uid)
                .toList();

        assertThat(uids)
                .containsExactly(
                        new ChannelUID(new ChannelGroupUID(thingUID, "12"), "temperature"),
                        new ChannelUID(new ChannelGroupUID(thingUID, "12"), "humidity"));
    }
}
