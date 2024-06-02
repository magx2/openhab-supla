package pl.grzeslowski.supla.openhab.internal.cloud.executors;

import static io.swagger.client.model.ChannelFunctionActionEnum.*;
import static org.mockito.Mockito.verify;

import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import io.swagger.client.model.ChannelExecuteActionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import pl.grzeslowski.supla.openhab.internal.cloud.api.ChannelsCloudApi;

@SuppressWarnings("WeakerAccess")
@ExtendWith(MockitoExtension.class)
@ExtendWith(RandomBeansExtension.class)
class SuplaLedCommandExecutorTest {
    @InjectMocks
    SuplaLedCommandExecutor executor;

    @Mock
    ChannelsCloudApi channelsApi;

    @Random
    int channelId;

    @Test
    @DisplayName("should send update about change of brightness for dimmer")
    void dimmer(@Random int channelId) throws Exception {

        // given
        final int brightnessValue = 55;
        final PercentType brightness = new PercentType(brightnessValue);
        executor.setLedState(channelId, PercentType.ZERO);

        // when
        executor.changeBrightness(channelId, brightness);

        // then
        final ChannelExecuteActionRequest expectedAction =
                new ChannelExecuteActionRequest().action(SET_RGBW_PARAMETERS).brightness(brightnessValue);
        verify(channelsApi).executeAction(expectedAction, channelId);
    }

    @Test
    @DisplayName("should send update about change of brightness for dimmer and rgb")
    void dimmerAndRgbChangeOfBrightness() throws Exception {

        // given
        final int brightnessValue = 55;
        final PercentType brightness = new PercentType(brightnessValue);
        executor.setLedState(channelId, PercentType.ZERO);
        executor.setLedState(channelId, HSBType.BLUE);

        // when
        executor.changeBrightness(channelId, brightness);

        // then
        final ChannelExecuteActionRequest expectedAction = new ChannelExecuteActionRequest()
                .action(SET_RGBW_PARAMETERS)
                .color("0x0000FF")
                .colorBrightness(100)
                .brightness(brightnessValue);
        verify(channelsApi).executeAction(expectedAction, channelId);
    }

    @Test
    @DisplayName("should send update about change of color for dimmer and rgb")
    void dimmerAndRgbChangeOfColor() throws Exception {

        // given
        final int brightnessValue = 55;
        executor.setLedState(channelId, new PercentType(brightnessValue));
        executor.setLedState(channelId, HSBType.BLUE);

        // when
        executor.changeColor(channelId, HSBType.RED);

        // then
        final ChannelExecuteActionRequest expectedAction = new ChannelExecuteActionRequest()
                .action(SET_RGBW_PARAMETERS)
                .color("0xFF0000")
                .colorBrightness(100)
                .brightness(brightnessValue);
        verify(channelsApi).executeAction(expectedAction, channelId);
    }
}
