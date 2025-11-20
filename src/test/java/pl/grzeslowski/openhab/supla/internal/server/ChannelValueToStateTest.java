package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openhab.core.types.UnDefType.NULL;
import static org.openhab.core.types.UnDefType.UNDEF;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
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
        var converter = new ChannelValueToState(thingUID, 2);
        org.mockito.Mockito.when(decimalValue.value()).thenReturn(BigDecimal.TEN);

        List<State> states = converter
                .onDecimalValue(decimalValue)
                .map(pair -> pair.getValue1())
                .toList();

        assertThat(states).containsExactly(new DecimalType(BigDecimal.TEN));
    }

    @Test
    void shouldReturnNullForMissingDecimal() {
        var converter = new ChannelValueToState(thingUID, 4);

        List<State> states =
                converter.onDecimalValue(null).map(pair -> pair.getValue1()).toList();

        assertThat(states).containsExactly(NULL);
    }

    @Test
    void shouldConvertPercentAndRgb() {
        var converter = new ChannelValueToState(thingUID, 6);
        org.mockito.Mockito.when(percentValue.value()).thenReturn(new BigDecimal("55"));
        org.mockito.Mockito.when(rgbValue.red()).thenReturn((short) 1);
        org.mockito.Mockito.when(rgbValue.green()).thenReturn((short) 2);
        org.mockito.Mockito.when(rgbValue.blue()).thenReturn((short) 3);

        List<State> states = converter
                .onPercentValue(percentValue)
                .map(pair -> pair.getValue1())
                .concat(converter.onRgbValue(rgbValue).map(pair -> pair.getValue1()))
                .toList();

        assertThat(states).containsExactly(new PercentType(55), HSBType.fromRGB((short) 1, (short) 2, (short) 3));
    }

    @Test
    void shouldReturnUndefForSpecialTemperatureValue() {
        var converter = new ChannelValueToState(thingUID, 7);
        org.mockito.Mockito.when(temperatureValue.temperature()).thenReturn(BigDecimal.valueOf(-275));

        State state = converter
                .onTemperatureValue(temperatureValue)
                .findFirst()
                .orElseThrow()
                .getValue1();

        assertThat(state).isEqualTo(UNDEF);
    }

    @Test
    void shouldMapTemperatureAndHumidityGroup() {
        var converter = new ChannelValueToState(thingUID, 8);
        org.mockito.Mockito.when(temperatureAndHumidityValue.temperature()).thenReturn(BigDecimal.valueOf(21));
        org.mockito.Mockito.when(temperatureAndHumidityValue.humidity()).thenReturn(BigDecimal.valueOf(-1));

        List<State> states = converter
                .onTemperatureAndHumidityValue(temperatureAndHumidityValue)
                .map(pair -> pair.getValue1())
                .toList();

        assertThat(states)
                .containsExactly(
                        new QuantityType<>(BigDecimal.valueOf(21), org.openhab.core.library.unit.SIUnits.CELSIUS),
                        UNDEF);
    }

    @Test
    void shouldHandleStoppableOpenClose() {
        var converter = new ChannelValueToState(thingUID, 9);

        State state = converter
                .onStoppableOpenClose(StoppableOpenClose.STOP)
                .findFirst()
                .orElseThrow()
                .getValue1();

        assertThat(state).isEqualTo(OnOffType.from(false));
    }

    @Test
    void shouldHandleUnknownValueNulls() {
        var converter = new ChannelValueToState(thingUID, 5);

        List<State> nullStates =
                converter.onUnknownValue(null).map(pair -> pair.getValue1()).toList();
        List<State> messageStates = converter
                .onUnknownValue(unknownValue)
                .map(pair -> pair.getValue1())
                .toList();

        assertThat(nullStates).containsExactly(NULL);
        org.mockito.Mockito.when(unknownValue.message()).thenReturn("oops");
        assertThat(messageStates).containsExactly(new StringType("oops"));
    }

    @Test
    void shouldReturnGroupedUidsForTemperatureAndHumidity() {
        var converter = new ChannelValueToState(thingUID, 12);

        List<ChannelUID> uids = converter
                .onTemperatureAndHumidityValue(null)
                .map(pair -> pair.getValue0())
                .toList();

        assertThat(uids)
                .containsExactly(
                        new ChannelUID(new ChannelGroupUID(thingUID, "12"), "temperature"),
                        new ChannelUID(new ChannelGroupUID(thingUID, "12"), "humidity"));
    }
}
