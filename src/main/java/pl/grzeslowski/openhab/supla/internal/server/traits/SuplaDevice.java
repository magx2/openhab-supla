package pl.grzeslowski.openhab.supla.internal.server.traits;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import pl.grzeslowski.jsupla.protocol.api.DeviceFlag;

public record SuplaDevice(
        Type type,
        String guid,
        String name,
        String softVer,
        @Nullable Integer manufacturerId,
        @Nullable Integer productId,
        Set<DeviceFlag> flags,
        List<DeviceChannel> channels) {
    public static SuplaDevice of(RegisterDeviceTrait trait) {
        return new SuplaDevice(
                switch (trait) {
                    case RegisterEmailDeviceTrait __ -> Type.EMAIL;
                    case RegisterLocationDeviceTrait __ -> Type.LOCATION;
                },
                trait.guid(),
                trait.name(),
                trait.softVer(),
                trait.manufacturerId(),
                trait.productId(),
                trait.flags(),
                trait.channels());
    }

    public enum Type {
        EMAIL,
        LOCATION
    }
}
