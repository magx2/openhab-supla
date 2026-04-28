package pl.grzeslowski.openhab.supla.internal.updates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SuplaUpdatesClientTest {
    private HttpClient httpClient;
    private SuplaUpdatesClient client;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        client = new SuplaUpdatesClient(
                httpClient,
                URI.create("https://updates.supla.org/check-updates"),
                URI.create("https://updates.supla.org/list-updates"));
    }

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
    void shouldBuildCheckUpdatesUriWithUpdateFilters() {
        var uri = SuplaUpdatesClient.buildUri(
                URI.create("https://updates.supla.org/check-updates"),
                new SuplaUpdatesClient.Request(4, 6000, "ZAMEL THW-01", "23.12.01", 2, 0, 1, 0, 0));

        assertThat(uri.toString())
                .isEqualTo(
                        "https://updates.supla.org/check-updates?manufacturerId=4&productId=6000&productName=ZAMEL+THW-01&version=23.12.01&platform=2&param1=0&param2=1&param3=0&param4=0");
    }

    @Test
    void shouldBuildListUpdatesUri() {
        var uri = SuplaUpdatesClient.buildListUri(
                URI.create("https://updates.supla.org/list-updates"),
                new SuplaUpdatesClient.Request(4, 6000, "ZAMEL THW-01", "23.12.01"));

        assertThat(uri.toString())
                .isEqualTo(
                        "https://updates.supla.org/list-updates?manufacturerId=4&productId=6000&productName=ZAMEL+THW-01&ignoreUpdateFilters=true");
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

    @Test
    void shouldParseUnknownProductEvenWhenResponseIsNotSuccessful() throws Exception {
        givenResponses(response(404, "{\"status\":\"Unknown product\"}"));

        var result = client.checkUpdates(new SuplaUpdatesClient.Request(4, 6000, "Unknown", "23.12.01"));

        assertThat(result.status()).isEqualTo(SuplaUpdatesClient.Status.UNKNOWN_PRODUCT);
        assertThat(result.updateAvailable()).isFalse();
    }

    @Test
    void shouldDiscoverUpdateFilterBeforeCheckingUpdates() throws Exception {
        givenResponses(response(200, """
                        [
                          {
                            "version": "23.12.02",
                            "platform": 2,
                            "param1": 0,
                            "param2": 0,
                            "param3": 0,
                            "param4": 0,
                            "updateUrl": "https://updates.example/thw"
                          }
                        ]
                        """), response(200, "{\"status\":\"Update not available\"}"));

        var result = client.checkUpdates(new SuplaUpdatesClient.Request(4, 6000, "ZAMEL THW-01", "23.12.02"));

        assertThat(result.status()).isEqualTo(SuplaUpdatesClient.Status.UPDATE_NOT_AVAILABLE);
        assertThat(sentRequests().get(0).uri().toString())
                .isEqualTo(
                        "https://updates.supla.org/list-updates?manufacturerId=4&productId=6000&productName=ZAMEL+THW-01&ignoreUpdateFilters=true");
        assertThat(sentRequests().get(1).uri().toString())
                .isEqualTo(
                        "https://updates.supla.org/check-updates?manufacturerId=4&productId=6000&productName=ZAMEL+THW-01&version=23.12.02&platform=2&param1=0&param2=0&param3=0&param4=0");
    }

    @Test
    void shouldInferResultFromListedUpdatesWhenCheckUpdatesRejectsAmbiguousFilters() throws Exception {
        givenResponses(response(200, """
                        [
                          {
                            "version": "2.8.61",
                            "platform": 1,
                            "param1": 5,
                            "param2": 1,
                            "updateUrl": "https://updates.example/user1"
                          },
                          {
                            "version": "2.8.61",
                            "platform": 1,
                            "param1": 5,
                            "param2": 0,
                            "updateUrl": "https://updates.example/user2"
                          }
                        ]
                        """), response(404, "{\"status\":\"Unknown product\"}"));

        var result = client.checkUpdates(new SuplaUpdatesClient.Request(0, 0, "ZAMEL MEW-01", "2.8.60"));

        assertThat(result.status()).isEqualTo(SuplaUpdatesClient.Status.UPDATE_AVAILABLE);
        assertThat(result.latestVersion()).isEqualTo("2.8.61");
        assertThat(result.updateUrl()).isEqualTo("https://updates.example/user1");
    }

    private void givenResponses(HttpResponse<String> first, HttpResponse<String>... rest)
            throws IOException, InterruptedException {
        when(httpClient.send(any(HttpRequest.class), anyStringBodyHandler())).thenReturn(first, rest);
    }

    private List<HttpRequest> sentRequests() throws IOException, InterruptedException {
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(), anyStringBodyHandler());
        return requests.getAllValues();
    }

    private static BodyHandler<String> anyStringBodyHandler() {
        return any();
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int statusCode, String body) {
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }
}
