package pl.grzeslowski.openhab.supla.internal.server;

import static pl.grzeslowski.jsupla.protocol.api.ChannelType.*;
import static pl.grzeslowski.jsupla.protocol.api.ChannelType.SUPLA_CHANNELTYPE_DIMMERANDRGBLED;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.*;

import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

public record RgbChannelInfo(boolean supportRgb, boolean supportDimmer, boolean supportDimmerCct) {
    public static RgbChannelInfo build(DeviceChannel deviceChannel) {
        var rgbwBitFunctions = deviceChannel.rgbwBitFunctions();
        if (rgbwBitFunctions.isEmpty()) {
            var type = deviceChannel.type();
            return new RgbChannelInfo(
                    type == SUPLA_CHANNELTYPE_RGBLEDCONTROLLER || type == SUPLA_CHANNELTYPE_DIMMERANDRGBLED,
                    type == SUPLA_CHANNELTYPE_DIMMER || type == SUPLA_CHANNELTYPE_DIMMERANDRGBLED,
                    false);
        }
        return new RgbChannelInfo(
                rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_RGB_LIGHTING)
                        || rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING)
                        || rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT_AND_RGB),
                rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING)
                        || rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER),
                rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT)
                        || rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT_AND_RGB));
    }
}
