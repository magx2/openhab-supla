package pl.grzeslowski.openhab.supla.internal.server.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseHexString;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;

import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValueA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceD;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceE;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;

class RegisterDeviceTraitTest {
    private static final byte[] GUID =
            new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16};
    private static final byte[] NAME = fillTo("Test Device".getBytes(), SUPLA_DEVICE_NAME_MAXSIZE);
    private static final byte[] SOFT_VER = fillTo("1.0".getBytes(), SUPLA_SOFTVER_MAXSIZE);

    private static byte[] fillTo(byte[] bytes, int length) {
        if (bytes.length == length) {
            return bytes;
        }
        return Arrays.copyOf(bytes, length);
    }

    private final ToServerProto toServerProto =
            new SuplaDeviceChannelValueA((short) 1, new byte[] {1, 2, 3, 4, 5, 6, 7, 8});

    @Test
    void shouldReturnEmptyOptionalWhenNotRegisterDevice() {
        Optional<RegisterDeviceTrait> result = RegisterDeviceTrait.fromProto(toServerProto);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapRegisterDeviceAToLocationTrait() {
        byte[] locationPassword = new byte[SUPLA_LOCATION_PWD_MAXSIZE];
        locationPassword[0] = 0x0A;
        SuplaRegisterDeviceA registerDeviceA = new SuplaRegisterDeviceA(
                5,
                locationPassword,
                GUID,
                NAME,
                SOFT_VER,
                (short) 0,
                new pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelA[0]);

        RegisterDeviceTrait trait = RegisterDeviceTrait.fromProto(
                (pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDevice) registerDeviceA);

        assertThat(trait).isInstanceOf(RegisterLocationDeviceTrait.class);
        RegisterLocationDeviceTrait locationTrait = (RegisterLocationDeviceTrait) trait;
        assertThat(locationTrait.guid()).isEqualTo(parseHexString(GUID));
        assertThat(locationTrait.name()).isEqualTo(parseString(NAME));
        assertThat(locationTrait.softVer()).isEqualTo(parseString(SOFT_VER));
        assertThat(locationTrait.manufacturerId()).isNull();
        assertThat(locationTrait.productId()).isNull();
        assertThat(locationTrait.channels()).isEmpty();
        assertThat(locationTrait.locationId()).isEqualTo(5);
        assertThat(locationTrait.locationPwd()).containsExactly(locationPassword);
    }

    @Test
    void shouldMapRegisterDeviceDToEmailTrait() {
        byte[] authKey = fillTo(new byte[] {0x01, 0x02}, SUPLA_AUTHKEY_SIZE);
        SuplaRegisterDeviceD registerDeviceD = new SuplaRegisterDeviceD(
                Arrays.copyOf("user@example.com".getBytes(), 256),
                authKey,
                GUID,
                NAME,
                SOFT_VER,
                Arrays.copyOf("server.supla.org".getBytes(), SUPLA_SERVER_NAME_MAXSIZE),
                (short) 0,
                new pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelB[0]);

        RegisterDeviceTrait trait = RegisterDeviceTrait.fromProto(
                (pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDevice) registerDeviceD);

        assertThat(trait).isInstanceOf(RegisterEmailDeviceTrait.class);
        RegisterEmailDeviceTrait emailTrait = (RegisterEmailDeviceTrait) trait;
        assertThat(emailTrait.guid()).isEqualTo(parseHexString(GUID));
        assertThat(emailTrait.name()).isEqualTo(parseString(NAME));
        assertThat(emailTrait.softVer()).isEqualTo(parseString(SOFT_VER));
        assertThat(emailTrait.manufacturerId()).isNull();
        assertThat(emailTrait.productId()).isNull();
        assertThat(emailTrait.channels()).isEmpty();
        assertThat(emailTrait.email()).isEqualTo("user@example.com");
        assertThat(emailTrait.authKey()).containsExactly(authKey);
        assertThat(emailTrait.serverName()).isEqualTo("server.supla.org");
    }

    @Test
    void shouldMapRegisterDeviceEWithManufacturerAndProduct() {
        byte[] authKey = fillTo(new byte[] {0x05}, SUPLA_AUTHKEY_SIZE);
        SuplaRegisterDeviceE registerDeviceE = new SuplaRegisterDeviceE(
                Arrays.copyOf("another@example.com".getBytes(), 256),
                authKey,
                GUID,
                NAME,
                SOFT_VER,
                Arrays.copyOf("prod.server".getBytes(), SUPLA_SERVER_NAME_MAXSIZE),
                0,
                (short) 10,
                (short) 20,
                (short) 0,
                new pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelC[0]);

        RegisterDeviceTrait trait = RegisterDeviceTrait.fromProto(
                (pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDevice) registerDeviceE);

        assertThat(trait).isInstanceOf(RegisterEmailDeviceTrait.class);
        RegisterEmailDeviceTrait emailTrait = (RegisterEmailDeviceTrait) trait;
        assertThat(emailTrait.manufacturerId()).isEqualTo(10);
        assertThat(emailTrait.productId()).isEqualTo(20);
        assertThat(emailTrait.email()).isEqualTo("another@example.com");
        assertThat(emailTrait.authKey()).containsExactly(authKey);
        assertThat(emailTrait.serverName()).isEqualTo("prod.server");
    }
}
