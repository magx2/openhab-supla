package pl.grzeslowski.supla.openhab.internal.cloud;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import javax.validation.constraints.Positive;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;

@SuppressWarnings("WeakerAccess")
@ExtendWith(MockitoExtension.class)
@ExtendWith(RandomBeansExtension.class)
class ChannelInfoParserTest {
    @InjectMocks
    ChannelInfoParser parser;

    @Random
    @Positive
    int channelId;

    @Random
    AdditionalChannelType additionalChannelType;

    @Test
    @DisplayName("should parse simple ChannelInfo")
    void simple() {

        // given
        final ChannelUID channelUID = new ChannelUID(new ThingUID("thingUID:part2:part3"), valueOf(channelId));

        // when
        final ChannelInfo channelInfo = parser.parse(channelUID);

        // then
        assertThat(channelInfo).isNotNull();
        assertThat(channelInfo.getChannelId()).isEqualTo(channelId);
        assertThat(channelInfo.getAdditionalChannelType()).isNull();
    }

    @Test
    @DisplayName("should parse ChannelInfo with ChannelType")
    void withType() {

        // given
        final ChannelUID channelUID =
                new ChannelUID(new ThingUID("thingUID:part2:part3"), channelId + additionalChannelType.getSuffix());

        // when
        final ChannelInfo channelInfo = parser.parse(channelUID);

        // then
        assertThat(channelInfo).isNotNull();
        assertThat(channelInfo.getChannelId()).isEqualTo(channelId);
        assertThat(channelInfo.getAdditionalChannelType()).isEqualTo(additionalChannelType);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException if channelId is not int")
    void exception() {

        // given
        final String notProperChannelId = "boo";
        final ChannelUID channelUID = new ChannelUID(new ThingUID("thingUID:part2:part3"), notProperChannelId);

        // when
        final ThrowableAssert.ThrowingCallable function = () -> parser.parse(channelUID);

        // then
        assertThatThrownBy(function).isInstanceOf(IllegalArgumentException.class);
    }
}
