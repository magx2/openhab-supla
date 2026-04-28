package pl.grzeslowski.openhab.supla.internal.updates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.Test;

class SuplaUpdatesClientTest {
    private final SuplaUpdatesClient client = new SuplaUpdatesClient();

    @Test
    void shouldBuildCheckUpdatesUri() {
        var uri = SuplaUpdatesClient.buildUri(
                URI.create("https://updates.supla.org/check-updates"),
                new SuplaUpdatesClient.Request(4, 6000, "ZAMEL THW-01", "1.2.3"));

        assertThat(uri.toString())
                .isEqualTo(
                        "https://updates.supla.org/check-updates?manufacturerId=4&productId=6000&productName=ZAMEL+THW-01&version=1.2.3");
    }

    @Test
    void shouldSkipBlankVersionWhenBuildingUri() {
        var uri = SuplaUpdatesClient.buildUri(
                URI.create("https://updates.supla.org/check-updates"),
                new SuplaUpdatesClient.Request(4, 6000, "ZAMEL THW-01", " "));

        assertThat(uri.toString())
                .isEqualTo(
                        "https://updates.supla.org/check-updates?manufacturerId=4&productId=6000&productName=ZAMEL+THW-01");
    }

    @Test
    void shouldParseAvailableUpdateResponse() throws Exception {
        var result = client.parseResponse("""
                {
                  "status": "Update available",
                  "latestUpdate": {
                    "version": "2.0.0",
                    "updateUrl": "https://updates.example/device"
                  }
                }
                """);

        assertThat(result.status()).isEqualTo(SuplaUpdatesClient.Status.UPDATE_AVAILABLE);
        assertThat(result.updateAvailable()).isTrue();
        assertThat(result.latestVersion()).isEqualTo("2.0.0");
        assertThat(result.updateUrl()).isEqualTo("https://updates.example/device");
    }

    @Test
    void shouldParseNotAvailableUpdateResponse() throws Exception {
        var result = client.parseResponse("{\"status\":\"Update not available\"}");

        assertThat(result.status()).isEqualTo(SuplaUpdatesClient.Status.UPDATE_NOT_AVAILABLE);
        assertThat(result.updateAvailable()).isFalse();
        assertThat(result.latestVersion()).isNull();
        assertThat(result.updateUrl()).isNull();
    }

    @Test
    void shouldRejectUnknownStatus() {
        assertThatThrownBy(() -> client.parseResponse("{\"status\":\"Unexpected\"}"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Unknown update check status");
    }
}
