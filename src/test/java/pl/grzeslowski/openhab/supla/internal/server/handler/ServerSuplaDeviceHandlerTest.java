package pl.grzeslowski.openhab.supla.internal.server.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DIMMER;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import pl.grzeslowski.jsupla.protocol.api.BitFunction;
import pl.grzeslowski.jsupla.protocol.api.ChannelFlag;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

class ServerSuplaDeviceHandlerTest {
    @Test
    void shouldBuildChannelProperties() {
        var firstChannel = new DeviceChannel(
                0,
                false,
                SUPLA_CHANNELTYPE_DIMMER,
                Set.of(ChannelFlag.SUPLA_CHANNEL_FLAG_CHANNELSTATE, ChannelFlag.SUPLA_CHANNEL_FLAG_WEEKLY_SCHEDULE),
                SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER,
                Set.of(),
                new byte[8],
                null,
                null,
                null,
                0L,
                Set.of(BitFunction.SUPLA_BIT_FUNC_LIGHTSWITCH),
                0);
        var secondChannel = new DeviceChannel(
                1,
                false,
                SUPLA_CHANNELTYPE_DIMMER,
                Set.of(),
                null,
                Set.of(),
                new byte[8],
                null,
                null,
                null,
                0L,
                Set.of(),
                0);

        var properties = ServerSuplaDeviceHandler.buildChannelProperties(List.of(firstChannel, secondChannel));

        assertThat(properties)
                .containsEntry("CHANNEL_FUNCTION_0", "SUPLA_CHANNELFNC_CONTROLLINGTHEROLLERSHUTTER")
                .containsEntry("CHANNEL_FLAGS_1", "[]")
                .containsEntry("CHANNEL_FUNCTIONS_1", "[]")
                .doesNotContainKey("CHANNEL_FUNCTION_1");
        assertThat(properties.get("CHANNEL_FLAGS_0"))
                .startsWith("[")
                .endsWith("]")
                .contains("SUPLA_CHANNEL_FLAG_CHANNELSTATE", "SUPLA_CHANNEL_FLAG_WEEKLY_SCHEDULE");
        assertThat(properties.get("CHANNEL_FUNCTIONS_0"))
                .startsWith("[")
                .endsWith("]")
                .contains("SUPLA_BIT_FUNC_LIGHTSWITCH");
    }
}
