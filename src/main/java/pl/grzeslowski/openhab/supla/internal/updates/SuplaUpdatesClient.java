package pl.grzeslowski.openhab.supla.internal.updates;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toUnmodifiableMap;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class SuplaUpdatesClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final URI CHECK_UPDATES_URI = URI.create("https://updates.supla.org/check-updates");
    private static final URI LIST_UPDATES_URI = URI.create("https://updates.supla.org/list-updates");

    private final HttpClient httpClient;
    private final URI checkUpdatesUri;
    private final URI listUpdatesUri;
    private final Gson gson = new Gson();

    public SuplaUpdatesClient() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(REQUEST_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                CHECK_UPDATES_URI,
                LIST_UPDATES_URI);
    }

    SuplaUpdatesClient(HttpClient httpClient, URI checkUpdatesUri) {
        this(httpClient, checkUpdatesUri, LIST_UPDATES_URI);
    }

    SuplaUpdatesClient(HttpClient httpClient, URI checkUpdatesUri, URI listUpdatesUri) {
        this.httpClient = httpClient;
        this.checkUpdatesUri = checkUpdatesUri;
        this.listUpdatesUri = listUpdatesUri;
    }

    public Result checkUpdates(Request request) throws IOException, InterruptedException {
        var updates = request.hasUpdateFilter() ? List.<UpdateEntry>of() : listUpdatesOrEmpty(request);
        var enrichedRequest = discoverUpdateFilter(request, updates);
        var requestToCheck = enrichedRequest == null ? request : request.withFilter(enrichedRequest);
        try {
            var result = executeCheckUpdates(requestToCheck);
            if (result.status() != Status.UNKNOWN_PRODUCT) {
                return result;
            }
            var fallbackResult = inferResultFromUpdates(request, updates);
            return fallbackResult == null ? result : fallbackResult;
        } catch (IOException exception) {
            var fallbackResult = inferResultFromUpdates(request, updates);
            if (fallbackResult != null) {
                return fallbackResult;
            }
            throw exception;
        }
    }

    private Result executeCheckUpdates(Request request) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder(buildUri(checkUpdatesUri, request))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        var response = httpClient.send(httpRequest, BodyHandlers.ofString(UTF_8));
        var responseBody = response.body();
        if (response.statusCode() != 200) {
            var parsedResponse = tryParseResponse(responseBody);
            if (parsedResponse != null) {
                return parsedResponse;
            }
            throw new IOException("Unexpected update check response status " + response.statusCode());
        }
        return parseResponse(responseBody);
    }

    private List<UpdateEntry> listUpdatesOrEmpty(Request request) throws IOException, InterruptedException {
        try {
            return listUpdates(request);
        } catch (IOException | JsonSyntaxException e) {
            return List.of();
        }
    }

    private List<UpdateEntry> listUpdates(Request request) throws IOException, InterruptedException {
        var httpRequest = HttpRequest.newBuilder(buildListUri(listUpdatesUri, request))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        var response = httpClient.send(httpRequest, BodyHandlers.ofString(UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Unexpected update list response status " + response.statusCode());
        }
        var updates = gson.fromJson(response.body(), UpdateEntry[].class);
        if (updates == null) {
            return List.of();
        }
        return stream(updates).filter(Objects::nonNull).toList();
    }

    static URI buildUri(URI baseUri, Request request) {
        var query = new StringJoiner("&");
        addQueryParam(query, "manufacturerId", Integer.toString(request.manufacturerId()));
        addQueryParam(query, "productId", Integer.toString(request.productId()));
        addQueryParam(query, "productName", request.productName());
        if (request.version() != null && !request.version().isBlank()) {
            addQueryParam(query, "version", request.version());
        }
        addOptionalQueryParam(query, "platform", request.platform());
        addOptionalQueryParam(query, "param1", request.param1());
        addOptionalQueryParam(query, "param2", request.param2());
        addOptionalQueryParam(query, "param3", request.param3());
        addOptionalQueryParam(query, "param4", request.param4());

        var base = baseUri.toString();
        var separator = base.contains("?") ? "&" : "?";
        return URI.create(base + separator + query);
    }

    static URI buildListUri(URI baseUri, Request request) {
        var query = new StringJoiner("&");
        addQueryParam(query, "manufacturerId", Integer.toString(request.manufacturerId()));
        addQueryParam(query, "productId", Integer.toString(request.productId()));
        addQueryParam(query, "productName", request.productName());
        addQueryParam(query, "ignoreUpdateFilters", Boolean.TRUE.toString());

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

    private @Nullable Result tryParseResponse(String responseBody) {
        try {
            return parseResponse(responseBody);
        } catch (IOException | JsonSyntaxException e) {
            return null;
        }
    }

    private static void addQueryParam(StringJoiner query, String name, String value) {
        query.add(encode(name, UTF_8) + "=" + encode(value, UTF_8));
    }

    private static void addOptionalQueryParam(StringJoiner query, String name, @Nullable Integer value) {
        if (value != null) {
            addQueryParam(query, name, value.toString());
        }
    }

    private static @Nullable UpdateFilter discoverUpdateFilter(Request request, List<UpdateEntry> updates) {
        if (updates.isEmpty()) {
            return null;
        }
        var matchingCurrentVersion = updates.stream()
                .filter(update -> sameVersion(request.version(), update.version))
                .toList();
        var candidates = matchingCurrentVersion.isEmpty() ? updates : matchingCurrentVersion;
        var filters = candidates.stream()
                .map(UpdateFilter::fromUpdate)
                .filter(UpdateFilter::hasAny)
                .distinct()
                .toList();
        if (filters.size() == 1) {
            return filters.getFirst();
        }
        var platforms = candidates.stream()
                .map(update -> update.platform)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (platforms.size() == 1) {
            return new UpdateFilter(platforms.getFirst(), null, null, null, null);
        }
        return null;
    }

    private static @Nullable Result inferResultFromUpdates(Request request, List<UpdateEntry> updates) {
        var latestUpdate = updates.stream()
                .filter(UpdateEntry::isDefaultUpdate)
                .max(SuplaUpdatesClient::compareUpdates)
                .orElse(null);
        if (latestUpdate == null) {
            return null;
        }
        var latestVersion = emptyToNull(latestUpdate.version);
        if (latestVersion == null) {
            return null;
        }
        if (request.version() != null && compareVersions(latestVersion, request.version()) <= 0) {
            return new Result(Status.UPDATE_NOT_AVAILABLE, null, null);
        }
        return new Result(Status.UPDATE_AVAILABLE, latestVersion, emptyToNull(latestUpdate.updateUrl));
    }

    private static boolean sameVersion(@Nullable String first, @Nullable String second) {
        var firstVersion = emptyToNull(first);
        var secondVersion = emptyToNull(second);
        return firstVersion != null && firstVersion.equals(secondVersion);
    }

    private static int compareUpdates(UpdateEntry first, UpdateEntry second) {
        var releaseDateComparison =
                compareNullableInstants(parseInstant(first.releasedAt), parseInstant(second.releasedAt));
        if (releaseDateComparison != 0) {
            return releaseDateComparison;
        }
        return compareVersions(first.version, second.version);
    }

    private static int compareNullableInstants(@Nullable Instant first, @Nullable Instant second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        return first.compareTo(second);
    }

    private static @Nullable Instant parseInstant(@Nullable String value) {
        var instant = emptyToNull(value);
        if (instant == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(instant).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static int compareVersions(@Nullable String first, @Nullable String second) {
        var firstVersion = emptyToNull(first);
        var secondVersion = emptyToNull(second);
        if (firstVersion == null && secondVersion == null) {
            return 0;
        }
        if (firstVersion == null) {
            return -1;
        }
        if (secondVersion == null) {
            return 1;
        }
        var firstParts = firstVersion.split("[._-]");
        var secondParts = secondVersion.split("[._-]");
        var maxLength = Math.max(firstParts.length, secondParts.length);
        for (var i = 0; i < maxLength; i++) {
            var firstPart = i < firstParts.length ? firstParts[i] : "0";
            var secondPart = i < secondParts.length ? secondParts[i] : "0";
            var comparison = compareVersionParts(firstPart, secondPart);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static int compareVersionParts(String first, String second) {
        if (first.chars().allMatch(Character::isDigit) && second.chars().allMatch(Character::isDigit)) {
            var normalizedFirst = removeLeadingZeroes(first);
            var normalizedSecond = removeLeadingZeroes(second);
            var lengthComparison = Integer.compare(normalizedFirst.length(), normalizedSecond.length());
            if (lengthComparison != 0) {
                return lengthComparison;
            }
            return normalizedFirst.compareTo(normalizedSecond);
        }
        return first.compareToIgnoreCase(second);
    }

    private static String removeLeadingZeroes(String value) {
        var stripped = value.replaceFirst("^0+", "");
        return stripped.isEmpty() ? "0" : stripped;
    }

    private static @Nullable String emptyToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public record Request(
            int manufacturerId,
            int productId,
            String productName,
            @Nullable String version,
            @Nullable Integer platform,
            @Nullable Integer param1,
            @Nullable Integer param2,
            @Nullable Integer param3,
            @Nullable Integer param4) {
        public Request(int manufacturerId, int productId, String productName, @Nullable String version) {
            this(manufacturerId, productId, productName, version, null, null, null, null, null);
        }

        public Request {
            requireNonNull(productName);
        }

        boolean hasUpdateFilter() {
            return platform != null || param1 != null || param2 != null || param3 != null || param4 != null;
        }

        Request withFilter(UpdateFilter filter) {
            return new Request(
                    manufacturerId,
                    productId,
                    productName,
                    version,
                    filter.platform(),
                    filter.param1(),
                    filter.param2(),
                    filter.param3(),
                    filter.param4());
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

    private record UpdateFilter(
            @Nullable Integer platform,
            @Nullable Integer param1,
            @Nullable Integer param2,
            @Nullable Integer param3,
            @Nullable Integer param4) {
        static UpdateFilter fromUpdate(UpdateEntry update) {
            return new UpdateFilter(update.platform, update.param1, update.param2, update.param3, update.param4);
        }

        boolean hasAny() {
            return platform != null || param1 != null || param2 != null || param3 != null || param4 != null;
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

        @Nullable
        String releasedAt;

        @Nullable
        Integer platform;

        @Nullable
        Integer param1;

        @Nullable
        Integer param2;

        @Nullable
        Integer param3;

        @Nullable
        Integer param4;

        @Nullable
        Boolean isBeta;

        @Nullable
        Boolean hiddenInFrontend;

        @Nullable
        Boolean otaTest;

        boolean isDefaultUpdate() {
            return !Boolean.TRUE.equals(isBeta)
                    && !Boolean.TRUE.equals(hiddenInFrontend)
                    && !Boolean.TRUE.equals(otaTest);
        }
    }
}
