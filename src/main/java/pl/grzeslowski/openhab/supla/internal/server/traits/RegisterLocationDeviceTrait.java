package pl.grzeslowski.openhab.supla.internal.server.traits;

import java.util.List;

public record RegisterLocationDeviceTrait(
        String guid,
        String name,
        String softVer,
        Integer manufacturerId,
        Integer productId,
        Flags flags,
        List<DeviceChannel> channels,
        int locationId,
        byte[] locationPwd)
        implements RegisterDeviceTrait {}
