package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuthDataTest {
    @Test
    void shouldRejectEmptyAuthData() {
        assertThatThrownBy(() -> new AuthData(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("AuthData must have at least one value");
    }

    @Test
    void shouldKeepLocationAuthData() {
        var location = new AuthData.LocationAuthData(123, "password");

        var authData = new AuthData(location, null);

        assertThat(authData.locationAuthData()).isEqualTo(location);
        assertThat(authData.emailAuthData()).isNull();
    }

    @Test
    void shouldKeepEmailAuthData() {
        var email = new AuthData.EmailAuthData("mail@example.com", "key");

        var authData = new AuthData(null, email);

        assertThat(authData.locationAuthData()).isNull();
        assertThat(authData.emailAuthData()).isEqualTo(email);
    }
}
