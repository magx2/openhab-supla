package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Arrays.stream;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseHexString;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDevice;

@RequiredArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class RegisterDeviceTrait {
    private final String guid;
    private final String name;
    private final String softVer;
    private final List<DeviceChannelTrait> channels;

    public RegisterDeviceTrait(byte[] guid, byte[] name, byte[] softVer, List<DeviceChannelTrait> list) {
        this(parseHexString(guid), parseString(name), parseString(softVer), list);
    }

    public RegisterDeviceTrait(SuplaRegisterDevice register) {
        this(
                register.guid,
                register.name,
                register.softVer,
                stream(register.channels).map(DeviceChannelTrait::new).toList());
    }
}
