package pl.grzeslowski.openhab.supla.internal.server.traits;

import static java.util.Arrays.stream;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseHexString;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.*;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;

public sealed interface RegisterDeviceTrait permits RegisterLocationDeviceTrait, RegisterEmailDeviceTrait {
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static Optional<RegisterDeviceTrait> fromProto(ToServerProto proto) {
        if (proto instanceof SuplaRegisterDevice trait) {
            return Optional.of(fromProto(trait));
        }

        return Optional.empty();
    }

    public static RegisterDeviceTrait fromProto(SuplaRegisterDevice proto) {
        return switch (proto) {
                // location
            case SuplaRegisterDeviceA register -> new RegisterLocationDeviceTrait(
                    parseHexString(register.guid()),
                    parseString(register.name()),
                    parseString(register.softVer()),
                    null,
                    null,
                    new Flags(0),
                    stream(register.channels()).map(DeviceChannel::fromProto).toList(),
                    register.locationId(),
                    register.locationPwd());
            case SuplaRegisterDeviceB register -> new RegisterLocationDeviceTrait(
                    parseHexString(register.guid()),
                    parseString(register.name()),
                    parseString(register.softVer()),
                    null,
                    null,
                    new Flags(0),
                    stream(register.channels()).map(DeviceChannel::fromProto).toList(),
                    register.locationId(),
                    register.locationPwd());
            case SuplaRegisterDeviceC register -> new RegisterLocationDeviceTrait(
                    parseHexString(register.guid()),
                    parseString(register.name()),
                    parseString(register.softVer()),
                    null,
                    null,
                    new Flags(0),
                    stream(register.channels()).map(DeviceChannel::fromProto).toList(),
                    register.locationId(),
                    register.locationPwd());
                // email
            case SuplaRegisterDeviceD register -> new RegisterEmailDeviceTrait(
                    parseHexString(register.guid()),
                    parseString(register.name()),
                    parseString(register.softVer()),
                    null,
                    null,
                    new Flags(0),
                    stream(register.channels()).map(DeviceChannel::fromProto).toList(),
                    parseString(register.email()),
                    register.authKey(),
                    parseString(register.serverName()));
            case SuplaRegisterDeviceE register -> new RegisterEmailDeviceTrait(
                    parseHexString(register.guid()),
                    parseString(register.name()),
                    parseString(register.softVer()),
                    (int) register.manufacturerId(),
                    (int) register.productId(),
                    new Flags(0),
                    stream(register.channels()).map(DeviceChannel::fromProto).toList(),
                    parseString(register.email()),
                    register.authKey(),
                    parseString(register.serverName()));
            case SuplaRegisterDeviceF register -> new RegisterEmailDeviceTrait(
                    parseHexString(register.guid()),
                    parseString(register.name()),
                    parseString(register.softVer()),
                    (int) register.manufacturerId(),
                    (int) register.productId(),
                    new Flags(0),
                    stream(register.channels()).map(DeviceChannel::fromProto).toList(),
                    parseString(register.email()),
                    register.authKey(),
                    parseString(register.serverName()));
            case SuplaRegisterDeviceG register -> new RegisterEmailDeviceTrait(
                    parseHexString(register.guid()),
                    parseString(register.name()),
                    parseString(register.softVer()),
                    (int) register.manufacturerId(),
                    (int) register.productId(),
                    new Flags(0),
                    stream(register.channels()).map(DeviceChannel::fromProto).toList(),
                    parseString(register.email()),
                    register.authKey(),
                    parseString(register.serverName()));
        };
    }

    String guid();

    String name();

    String softVer();

    @Nullable
    Integer manufacturerId();

    @Nullable
    Integer productId();

    Flags flags();

    List<DeviceChannel> channels();
}
