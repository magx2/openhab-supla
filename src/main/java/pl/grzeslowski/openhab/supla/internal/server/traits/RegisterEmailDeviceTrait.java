package pl.grzeslowski.openhab.supla.internal.server.traits;

import java.util.List;

public record RegisterEmailDeviceTrait(
        String guid,
        String name,
        String softVer,
        Integer manufacturerId,
        Integer productId,
        Flags flags,
        List<DeviceChannel> channels,
        String email,
        byte[] authKey,
        String serverName)
        implements RegisterDeviceTrait {}
