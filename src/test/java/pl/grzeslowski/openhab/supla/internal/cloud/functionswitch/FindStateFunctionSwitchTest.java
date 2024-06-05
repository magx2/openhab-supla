package pl.grzeslowski.openhab.supla.internal.cloud.functionswitch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static pl.grzeslowski.openhab.supla.internal.cloud.AdditionalChannelType.EXTRA_LIGHT_ACTIONS;
import static pl.grzeslowski.openhab.supla.internal.cloud.AdditionalChannelType.HUMIDITY;
import static pl.grzeslowski.openhab.supla.internal.cloud.AdditionalChannelType.TEMPERATURE;

import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import io.swagger.client.model.Channel;
import io.swagger.client.model.ChannelState;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import pl.grzeslowski.openhab.supla.internal.cloud.ChannelInfo;
import pl.grzeslowski.openhab.supla.internal.cloud.ChannelInfoParser;
import pl.grzeslowski.openhab.supla.internal.cloud.executors.LedCommandExecutor;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "unused"})
@ExtendWith(MockitoExtension.class)
@ExtendWith(RandomBeansExtension.class)
class FindStateFunctionSwitchTest {
    @InjectMocks
    FindStateFunctionSwitch functionSwitch;

    @Mock
    LedCommandExecutor ledCommandExecutor;

    @Mock
    ChannelUID channelUID;

    @Mock
    ChannelInfoParser channelInfoParser;

    @Test
    @DisplayName("should return temperature state with adjustment")
    void onHumidityAndTemperatureTemperature(@Random BigDecimal temp, @Random int param2) {
        // given
        Channel tempChannel =
                new Channel().state(new ChannelState().temperature(temp)).param2(param2);
        final ChannelInfo channelInfo = new ChannelInfo(1, TEMPERATURE, null);
        given(channelInfoParser.parse(channelUID)).willReturn(channelInfo);

        // when
        final Optional<? extends State> state = functionSwitch.onHumidityAndTemperature(tempChannel);

        // then
        assertThat(state).isPresent();
        final State tempState = state.get();
        assertThat(tempState).isEqualTo(new DecimalType(temp.add(new BigDecimal(param2 / 100))));
    }

    @Test
    @DisplayName("should return temperature state without adjustment")
    void onHumidityAndTemperatureNullTemperatureParam2(@Random BigDecimal temp) {
        // given
        Channel tempChannel =
                new Channel().state(new ChannelState().temperature(temp)).param2(null);
        final ChannelInfo channelInfo = new ChannelInfo(1, TEMPERATURE, null);
        given(channelInfoParser.parse(channelUID)).willReturn(channelInfo);

        // when
        final Optional<? extends State> state = functionSwitch.onHumidityAndTemperature(tempChannel);

        // then
        assertThat(state).isPresent();
        final State tempState = state.get();
        assertThat(tempState).isEqualTo(new DecimalType(temp));
    }

    @Test
    @DisplayName("should return state with adjustment")
    void onHumidityAndTemperatureHumidity(@Random BigDecimal humidity, @Random int param2) {
        // given
        Channel humidityChannel =
                new Channel().state(new ChannelState().humidity(humidity)).param2(param2);
        final ChannelInfo channelInfo = new ChannelInfo(1, HUMIDITY, null);
        given(channelInfoParser.parse(channelUID)).willReturn(channelInfo);

        // when
        final Optional<? extends State> state = functionSwitch.onHumidityAndTemperature(humidityChannel);

        // then
        assertThat(state).isPresent();
        final State humidityState = state.get();
        assertThat(humidityState).isEqualTo(new DecimalType(humidity.add(new BigDecimal(param2 / 100))));
    }

    @Test
    @DisplayName("should return state without adjustment")
    void onHumidityAndTemperatureHumidityNullParam2(@Random BigDecimal humidity) {
        // given
        Channel humidityChannel =
                new Channel().state(new ChannelState().humidity(humidity)).param2(null);
        final ChannelInfo channelInfo = new ChannelInfo(1, HUMIDITY, null);
        given(channelInfoParser.parse(channelUID)).willReturn(channelInfo);

        // when
        final Optional<? extends State> state = functionSwitch.onHumidityAndTemperature(humidityChannel);

        // then
        assertThat(state).isPresent();
        final State HumidityState = state.get();
        assertThat(HumidityState).isEqualTo(new DecimalType(humidity));
    }

    @Test
    @DisplayName("should return throw nullPointerException if additional type is null")
    void onHumidityAndTemperatureNullAdditionalType() {
        // given
        final ChannelInfo channelInfo = new ChannelInfo(1, null, null);
        given(channelInfoParser.parse(channelUID)).willReturn(channelInfo);

        // when
        final Executable state = () -> functionSwitch.onHumidityAndTemperature(new Channel());

        // then
        assertThrows(NullPointerException.class, state);
    }

    @Test
    @DisplayName("should return throw IllegalStateException if additional type is wrong")
    void onHumidityAndTemperatureWrongAdditionalType() {
        // given
        final ChannelInfo channelInfo = new ChannelInfo(1, EXTRA_LIGHT_ACTIONS, null);
        given(channelInfoParser.parse(channelUID)).willReturn(channelInfo);

        // when
        final Executable state = () -> functionSwitch.onHumidityAndTemperature(new Channel());

        // then
        assertThrows(IllegalStateException.class, state);
    }
}
