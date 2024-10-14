package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Arrays.stream;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceD;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceE;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceF;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceG;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RegisterEmailDeviceTrait extends RegisterDeviceTrait {
    private final String email;
    private final byte[] authKey;
    private final String serverName;

    public RegisterEmailDeviceTrait(SuplaRegisterDeviceD register) {
        super(
                register.guid,
                register.name,
                register.softVer,
                null,
                null,
                0,
                stream(register.channels).map(DeviceChannelTrait::new).toList());
        this.email = parseString(register.email);
        this.authKey =register.authKey;
        this.serverName = parseString(register.serverName);
    }

    public RegisterEmailDeviceTrait(SuplaRegisterDeviceE register) {
        super(
                register.guid,
                register.name,
                register.softVer,
                (int) register.manufacturerId,
                (int) register.productId,
                register.flags,
                stream(register.channels).map(DeviceChannelTrait::new).toList());
        this.email = parseString(register.email);
        this.authKey =register.authKey;
        this.serverName = parseString(register.serverName);
    }

    public RegisterEmailDeviceTrait(SuplaRegisterDeviceF register) {
        super(
                register.guid,
                register.name,
                register.softVer,
                (int) register.manufacturerId,
                (int) register.productId,
                register.flags,
                stream(register.channels).map(DeviceChannelTrait::new).toList());
        this.email = parseString(register.email);
        this.authKey =register.authKey;
        this.serverName = parseString(register.serverName);
    }

    public RegisterEmailDeviceTrait(SuplaRegisterDeviceG register) {
        super(
                register.guid,
                register.name,
                register.softVer,
                (int) register.manufacturerId,
                (int) register.productId,
                register.flags,
                stream(register.channels).map(DeviceChannelTrait::new).toList());
        this.email = parseString(register.email);
        this.authKey =register.authKey;
        this.serverName = parseString(register.serverName);
    }
}
