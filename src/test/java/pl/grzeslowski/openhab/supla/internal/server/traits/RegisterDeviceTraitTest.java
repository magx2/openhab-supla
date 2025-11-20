package pl.grzeslowski.openhab.supla.internal.server.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseHexString;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceA;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceD;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaRegisterDeviceE;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;

@ExtendWith(MockitoExtension.class)
class RegisterDeviceTraitTest {
    private static final byte[] GUID = new byte[] {0x01, 0x02, 0x03, 0x04};
    private static final byte[] NAME = "Test Device".getBytes();
    private static final byte[] SOFT_VER = "1.0".getBytes();

    @Mock
    private ToServerProto toServerProto;

    @Mock
    private SuplaRegisterDeviceA registerDeviceA;

    @Mock
    private SuplaRegisterDeviceD registerDeviceD;

    @Mock
    private SuplaRegisterDeviceE registerDeviceE;

    @Test
    void shouldReturnEmptyOptionalWhenNotRegisterDevice() {
        Optional<RegisterDeviceTrait> result = RegisterDeviceTrait.fromProto(toServerProto);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapRegisterDeviceAToLocationTrait() {
        byte[] locationPassword = new byte[] {0x0A};
        when(registerDeviceA.guid()).thenReturn(GUID);
        when(registerDeviceA.name()).thenReturn(NAME);
        when(registerDeviceA.softVer()).thenReturn(SOFT_VER);
        when(registerDeviceA.channels())
                .thenReturn(new pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelA[0]);
        when(registerDeviceA.locationId()).thenReturn(5);
        when(registerDeviceA.locationPwd()).thenReturn(locationPassword);

        RegisterDeviceTrait trait = RegisterDeviceTrait.fromProto(registerDeviceA);

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
        byte[] authKey = new byte[] {0x01, 0x02};
        when(registerDeviceD.guid()).thenReturn(GUID);
        when(registerDeviceD.name()).thenReturn(NAME);
        when(registerDeviceD.softVer()).thenReturn(SOFT_VER);
        when(registerDeviceD.channels())
                .thenReturn(new pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelE[0]);
        when(registerDeviceD.email()).thenReturn("user@example.com".getBytes());
        when(registerDeviceD.authKey()).thenReturn(authKey);
        when(registerDeviceD.serverName()).thenReturn("server.supla.org".getBytes());

        RegisterDeviceTrait trait = RegisterDeviceTrait.fromProto(registerDeviceD);

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
        byte[] authKey = new byte[] {0x05};
        when(registerDeviceE.guid()).thenReturn(GUID);
        when(registerDeviceE.name()).thenReturn(NAME);
        when(registerDeviceE.softVer()).thenReturn(SOFT_VER);
        when(registerDeviceE.channels())
                .thenReturn(new pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelE[0]);
        when(registerDeviceE.manufacturerId()).thenReturn(10L);
        when(registerDeviceE.productId()).thenReturn(20L);
        when(registerDeviceE.email()).thenReturn("another@example.com".getBytes());
        when(registerDeviceE.authKey()).thenReturn(authKey);
        when(registerDeviceE.serverName()).thenReturn("prod.server".getBytes());

        RegisterDeviceTrait trait = RegisterDeviceTrait.fromProto(registerDeviceE);

        assertThat(trait).isInstanceOf(RegisterEmailDeviceTrait.class);
        RegisterEmailDeviceTrait emailTrait = (RegisterEmailDeviceTrait) trait;
        assertThat(emailTrait.manufacturerId()).isEqualTo(10);
        assertThat(emailTrait.productId()).isEqualTo(20);
        assertThat(emailTrait.email()).isEqualTo("another@example.com");
        assertThat(emailTrait.authKey()).containsExactly(authKey);
        assertThat(emailTrait.serverName()).isEqualTo("prod.server");
    }
}
