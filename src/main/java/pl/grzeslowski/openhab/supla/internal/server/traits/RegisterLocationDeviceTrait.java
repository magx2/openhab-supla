package pl.grzeslowski.openhab.supla.internal.server.traits;

import java.util.List;
import java.util.Set;
import pl.grzeslowski.jsupla.protocol.api.DeviceFlag;

public record RegisterLocationDeviceTrait(
        String guid,
        String name,
        String softVer,
        Integer manufacturerId,
        Integer productId,
        Set<DeviceFlag> flags,
        List<DeviceChannel> channels,
        int locationId,
        byte[] locationPwd)
        implements RegisterDeviceTrait {}
