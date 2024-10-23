package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Arrays.stream;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDevice;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceB;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceC;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RegisterLocationDeviceTrait extends RegisterDeviceTrait {
    private final int locationId;
    private final byte[] locationPwd;

    public RegisterLocationDeviceTrait(SuplaRegisterDevice register) {
        super(
                register.guid,
                register.name,
                register.softVer,
                null,
                null,
                0,
                stream(register.channels).map(DeviceChannelTrait::new).toList());
        this.locationId = register.locationId;
        this.locationPwd = register.locationPwd;
    }

    public RegisterLocationDeviceTrait(SuplaRegisterDeviceB register) {
        super(
                register.guid,
                register.name,
                register.softVer,
                null,
                null,
                0,
                stream(register.channels).map(DeviceChannelTrait::new).toList());
        this.locationId = register.locationId;
        this.locationPwd = register.locationPwd;
    }

    public RegisterLocationDeviceTrait(SuplaRegisterDeviceC register) {
        super(
                register.guid,
                register.name,
                register.softVer,
                null,
                null,
                0,
                stream(register.channels).map(DeviceChannelTrait::new).toList());
        this.locationId = register.locationId;
        this.locationPwd = register.locationPwd;
    }
}
