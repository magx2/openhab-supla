package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Arrays.stream;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseHexString;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;

import jakarta.annotation.Nullable;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.jdt.annotation.NonNullByDefault;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDevice;

@RequiredArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@NonNullByDefault
public abstract sealed class RegisterDeviceTrait permits RegisterLocationDeviceTrait, RegisterEmailDeviceTrait {
    private final String guid;
    private final String name;
    private final String softVer;

    @Nullable
    private final Integer manufacturerId;

    @Nullable
    private final Integer productId;

    private final Flags flags;
    private final List<DeviceChannelTrait> channels;

    public RegisterDeviceTrait(
            byte[] guid,
            byte[] name,
            byte[] softVer,
            @Nullable Integer manufacturerId,
            @Nullable Integer productId,
            int flags,
            List<DeviceChannelTrait> list) {
        this(
                parseHexString(guid),
                parseString(name),
                parseString(softVer),
                manufacturerId,
                productId,
                new Flags(flags),
                list);
    }

    public RegisterDeviceTrait(SuplaRegisterDevice register) {
        this(
                register.guid(),
                register.name(),
                register.softVer(),
                null,
                null,
                0,
                stream(register.channels()).map(DeviceChannelTrait::new).toList());
    }
}
