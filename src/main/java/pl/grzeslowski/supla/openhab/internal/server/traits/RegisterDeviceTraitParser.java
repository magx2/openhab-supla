package pl.grzeslowski.supla.openhab.internal.server.traits;

import java.util.Optional;
import lombok.experimental.UtilityClass;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;

@UtilityClass
public class RegisterDeviceTraitParser {

    public static Optional<RegisterDeviceTrait> parse(ToServerProto proto) {
        if (proto instanceof SuplaRegisterDevice register) {
            return Optional.of(new RegisterLocationDeviceTrait(register));
        }
        if (proto instanceof SuplaRegisterDeviceB register) {
            return Optional.of(new RegisterLocationDeviceTrait(register));
        }
        if (proto instanceof SuplaRegisterDeviceC register) {
            return Optional.of(new RegisterLocationDeviceTrait(register));
        }
        if (proto instanceof SuplaRegisterDeviceD register) {
            return Optional.of(new RegisterEmailDeviceTrait(register));
        }
        if (proto instanceof SuplaRegisterDeviceE register) {
            return Optional.of(new RegisterEmailDeviceTrait(register));
        }
        if (proto instanceof SuplaRegisterDeviceF register) {
            return Optional.of(new RegisterEmailDeviceTrait(register));
        }

        return Optional.empty();
    }
}
