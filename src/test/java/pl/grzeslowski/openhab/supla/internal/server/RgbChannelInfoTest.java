package pl.grzeslowski.openhab.supla.internal.server;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DHT11;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DIMMER;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DIMMERANDRGBLED;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_RGBLEDCONTROLLER;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_DIMMER;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_DIMMER_CCT;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_DIMMER_CCT_AND_RGB;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.SUPLA_RGBW_BIT_FUNC_RGB_LIGHTING;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.grzeslowski.jsupla.protocol.api.ChannelType;
import pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

@ExtendWith(MockitoExtension.class)
class RgbChannelInfoTest {

    // Helper method to create DeviceChannel for tests
    private DeviceChannel mockDeviceChannel(ChannelType type, Set<RgbwBitFunction> rgbwBitFunctions) {
        return new DeviceChannel(
                1, // number
                type,
                Collections.emptySet(), // flags
                null, // channelFunction
                rgbwBitFunctions,
                new byte[8], // value
                null, // action
                null, // hvacValue
                null // subDeviceId
                );
    }
    // Test cases for when rgbwBitFunctions is empty
    @SuppressWarnings("deprecation")
    private static Stream<Arguments> testCasesWhenRgbwBitFunctionsEmpty() {
        return Stream.of(
                Arguments.of(SUPLA_CHANNELTYPE_RGBLEDCONTROLLER, true, false, false),
                Arguments.of(SUPLA_CHANNELTYPE_DIMMERANDRGBLED, true, true, false),
                Arguments.of(SUPLA_CHANNELTYPE_DIMMER, false, true, false),
                Arguments.of(SUPLA_CHANNELTYPE_DHT11, false, false, false), // Other type
                Arguments.of(null, false, false, false) // Null type
                );
    }

    @ParameterizedTest
    @MethodSource("testCasesWhenRgbwBitFunctionsEmpty")
    @DisplayName("Should build RgbChannelInfo correctly when rgbwBitFunctions is empty")
    void shouldBuildRgbChannelInfoWhenRgbwBitFunctionsEmpty(
            ChannelType channelType,
            boolean expectedSupportRgb,
            boolean expectedSupportDimmer,
            boolean expectedSupportDimmerCct) {
        // Given
        DeviceChannel deviceChannel = mockDeviceChannel(channelType, Collections.emptySet());

        // When
        RgbChannelInfo info = RgbChannelInfo.build(deviceChannel);

        // Then
        assertThat(info.supportRgb()).isEqualTo(expectedSupportRgb);
        assertThat(info.supportDimmer()).isEqualTo(expectedSupportDimmer);
        assertThat(info.supportDimmerCct()).isEqualTo(expectedSupportDimmerCct);
    }

    // Test cases for when rgbwBitFunctions is not empty
    private static Stream<Arguments> testCasesWhenRgbwBitFunctionsNotEmpty() {
        return Stream.of(
                Arguments.of(Set.of(SUPLA_RGBW_BIT_FUNC_RGB_LIGHTING), true, false, false),
                Arguments.of(Set.of(SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING), true, true, false),
                Arguments.of(Set.of(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT_AND_RGB), true, false, true),
                Arguments.of(Set.of(SUPLA_RGBW_BIT_FUNC_DIMMER), false, true, false),
                Arguments.of(Set.of(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT), false, false, true),
                Arguments.of(Set.of(), false, false, false), // Replaced SUPLA_RGBW_BIT_FUNC_NONE with empty set
                Arguments.of(
                        Set.of(SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING, SUPLA_RGBW_BIT_FUNC_DIMMER_CCT),
                        true,
                        true,
                        true) // Combination
                );
    }

    @ParameterizedTest
    @MethodSource("testCasesWhenRgbwBitFunctionsNotEmpty")
    @DisplayName("Should build RgbChannelInfo correctly when rgbwBitFunctions is not empty")
    void shouldBuildRgbChannelInfoWhenRgbwBitFunctionsNotEmpty(
            Set<RgbwBitFunction> rgbwBitFunctions,
            boolean expectedSupportRgb,
            boolean expectedSupportDimmer,
            boolean expectedSupportDimmerCct) {
        // Given
        DeviceChannel deviceChannel = mockDeviceChannel(null, rgbwBitFunctions); // Type can be null here

        // When
        RgbChannelInfo info = RgbChannelInfo.build(deviceChannel);

        // Then
        assertThat(info.supportRgb()).isEqualTo(expectedSupportRgb);
        assertThat(info.supportDimmer()).isEqualTo(expectedSupportDimmer);
        assertThat(info.supportDimmerCct()).isEqualTo(expectedSupportDimmerCct);
    }
}
