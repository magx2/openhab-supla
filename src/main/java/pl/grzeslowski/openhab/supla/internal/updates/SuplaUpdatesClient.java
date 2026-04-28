package pl.grzeslowski.openhab.supla.internal.updates;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableMap;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class SuplaUpdatesClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final URI CHECK_UPDATES_URI = URI.create("https://updates.supla.org/check-updates");

    private final HttpClient httpClient;
    private final URI checkUpdatesUri;
    private final Gson gson = new Gson();

    public SuplaUpdatesClient() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(REQUEST_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                CHECK_UPDATES_URI);
    }

    SuplaUpdatesClient(HttpClient httpClient, URI checkUpdatesUri) {
        this.httpClient = httpClient;
        this.checkUpdatesUri = checkUpdatesUri;
    }

    public Result checkUpdates(Request request) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder(buildUri(checkUpdatesUri, request))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        var response = httpClient.send(httpRequest, BodyHandlers.ofString(UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected update check response status " + response.statusCode());
        }
        return parseResponse(response.body());
    }

    static URI buildUri(URI baseUri, Request request) {
        var query = new StringJoiner("&");
        addQueryParam(query, "manufacturerId", Integer.toString(request.manufacturerId()));
        addQueryParam(query, "productId", Integer.toString(request.productId()));
        addQueryParam(query, "productName", request.productName());
        if (request.version() != null && !request.version().isBlank()) {
            addQueryParam(query, "version", request.version());
        }

        var base = baseUri.toString();
        var separator = base.contains("?") ? "&" : "?";
        return URI.create(base + separator + query);
    }

    Result parseResponse(String responseBody) throws IOException {
        var response = gson.fromJson(responseBody, Response.class);
        if (response == null || response.status == null) {
            throw new IOException("Update check response does not contain status");
        }
        var status = Status.fromApiStatus(response.status);
        var latestUpdate = response.latestUpdate;
        return new Result(
                status,
                latestUpdate == null ? null : emptyToNull(latestUpdate.version),
                latestUpdate == null ? null : emptyToNull(latestUpdate.updateUrl));
    }

    private static void addQueryParam(StringJoiner query, String name, String value) {
        query.add(encode(name, UTF_8) + "=" + encode(value, UTF_8));
    }

    private static @Nullable String emptyToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    public record Request(
            int manufacturerId,
            int productId,
            String productName,
            @Nullable String version) {
        public Request {
            requireNonNull(productName);
        }
    }

    public record Result(
            Status status,
            @Nullable String latestVersion,
            @Nullable String updateUrl) {
        public Result {
            requireNonNull(status);
        }

        public boolean updateAvailable() {
            return status.updateAvailable();
        }
    }

    public enum Status {
        CHECKING(null, false),
        UPDATE_NOT_AVAILABLE("Update not available", false),
        UPDATE_AVAILABLE("Update available", true),
        GUID_SPECIFIC_UPDATE_AVAILABLE("GUID specific update available", true),
        MAIL_SPECIFIC_UPDATE_AVAILABLE("Mail specific update available", true),
        BETA_UPDATE_AVAILABLE("BETA update available", true),
        OTA_TEST_UPDATE_AVAILABLE("OTA test update available", true),
        UNKNOWN_PRODUCT("Unknown product", false),
        ERROR(null, false);

        private static final Map<String, Status> API_STATUSES = stream(values())
                .filter(status -> status.apiStatus != null)
                .collect(toUnmodifiableMap(status -> requireNonNull(status.apiStatus), status -> status));

        @Nullable
        private final String apiStatus;

        private final boolean updateAvailable;

        Status(@Nullable String apiStatus, boolean updateAvailable) {
            this.apiStatus = apiStatus;
            this.updateAvailable = updateAvailable;
        }

        static Status fromApiStatus(String apiStatus) throws IOException {
            var status = API_STATUSES.get(apiStatus);
            if (status == null) {
                throw new IOException("Unknown update check status " + apiStatus);
            }
            return status;
        }

        public boolean updateAvailable() {
            return updateAvailable;
        }
    }

    @SuppressWarnings("MemberName")
    private static class Response {
        @Nullable
        String status;

        @Nullable
        UpdateEntry latestUpdate;
    }

    @SuppressWarnings("MemberName")
    private static class UpdateEntry {
        @Nullable
        String version;

        @Nullable
        String updateUrl;
    }
}
