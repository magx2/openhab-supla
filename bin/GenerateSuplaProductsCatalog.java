import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

	public class GenerateSuplaProductsCatalog {
	private static final String PRODUCTS_URL = "https://updates.supla.org/products";
	private static final String PROTO_H_URL =
			"https://raw.githubusercontent.com/SUPLA/supla-core/master/supla-common/proto.h";
	private static final Pattern SUPLA_MFR_REGEX = Pattern.compile("#define\\s+(SUPLA_MFR_[A-Z0-9_]+)\\s+([0-9]+)");

		private record ProductEntry(int manufacturerId, int productId, String manufacturer, String name) {}

		public static void main(String[] args) throws IOException, InterruptedException {
			String output = null;
			boolean failOnNetworkError = false;
			for (int i = 0; i < args.length - 1; i++) {
				if ("--output".equals(args[i])) {
					output = args[i + 1];
				}
				if ("--fail-on-network-error".equals(args[i])) {
					failOnNetworkError = Boolean.parseBoolean(args[i + 1]);
				}
			}
			if (output == null || output.isBlank()) {
				throw new IllegalArgumentException("Missing required --output argument");
			}

			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
			List<ProductEntry> entries = fetchProducts(client, failOnNetworkError);
			String javaContent = buildJavaContent(entries);

		Path outputPath = Path.of(output);
		Files.createDirectories(outputPath.getParent());
			Files.writeString(outputPath, javaContent, StandardCharsets.UTF_8);
			System.out.printf("Generated %s with %d product rows%n", outputPath, entries.size());
		}

		private static List<ProductEntry> fetchProducts(HttpClient client, boolean failOnNetworkError)
				throws IOException, InterruptedException {
			try {
				String productsJson = fetch(client, PRODUCTS_URL);
				String protoH = fetch(client, PROTO_H_URL);
				Map<Integer, String> manufacturers = parseManufacturers(protoH);
				return parseProducts(productsJson, manufacturers);
			} catch (IOException | InterruptedException | RuntimeException ex) {
				if (failOnNetworkError) {
					throw ex;
				}
				System.err.printf("WARN: Could not fetch products catalog, generating empty map: %s%n", ex.getMessage());
				return List.of();
			}
		}

	private static String fetch(HttpClient client, String url) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(url)).timeout(Duration.ofSeconds(30)).build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() != 200) {
			throw new IllegalStateException("Failed to fetch %s, HTTP %d".formatted(url, response.statusCode()));
		}
		return response.body();
	}

	private static Map<Integer, String> parseManufacturers(String protoH) {
		Map<Integer, String> manufacturers = new LinkedHashMap<>();
		for (String line : protoH.split("\\R")) {
			var match = SUPLA_MFR_REGEX.matcher(line);
			if (!match.find()) {
				continue;
			}
			var manufacturerName = match.group(1).replace("SUPLA_MFR_", "").replace("_", " ");
			var manufacturerId = Integer.parseInt(match.group(2));
			manufacturers.put(manufacturerId, manufacturerName);
		}
		return manufacturers;
	}

	private static List<ProductEntry> parseProducts(String productsJson, Map<Integer, String> manufacturers) {
		List<ProductEntry> entries = new ArrayList<>();
		for (String row : splitJsonRows(productsJson)) {
			Integer manufacturerId = extractInt(row, "manufacturerId");
			Integer productId = extractInt(row, "productId");
			String productName = extractString(row, "productName");
			if (manufacturerId == null || productId == null || productName == null) {
				continue;
			}
			if (manufacturerId <= 0 || productId <= 0 || productName.isBlank()) {
				continue;
			}
			String manufacturer = manufacturers.getOrDefault(manufacturerId, "UNKNOWN (%d)".formatted(manufacturerId));
			entries.add(new ProductEntry(manufacturerId, productId, manufacturer, productName.trim()));
		}
		entries.sort(Comparator.comparingInt(ProductEntry::manufacturerId).thenComparingInt(ProductEntry::productId));
		return entries;
	}

	private static List<String> splitJsonRows(String json) {
		var rows = new ArrayList<String>();
		boolean inString = false;
		int depth = 0;
		int start = -1;
		for (int i = 0; i < json.length(); i++) {
			char ch = json.charAt(i);
			if (ch == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
				inString = !inString;
			}
			if (inString) {
				continue;
			}
			if (ch == '{') {
				if (depth == 0) {
					start = i;
				}
				depth++;
			} else if (ch == '}') {
				depth--;
				if (depth == 0 && start >= 0) {
					rows.add(json.substring(start, i + 1));
				}
			}
		}
		return rows;
	}

	private static Integer extractInt(String row, String key) {
		var pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?[0-9]+)");
		var matcher = pattern.matcher(row);
		if (!matcher.find()) {
			return null;
		}
		return Integer.parseInt(matcher.group(1));
	}

	private static String extractString(String row, String key) {
		var pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");
		var matcher = pattern.matcher(row);
		if (!matcher.find()) {
			return null;
		}
		return unescapeJsonString(matcher.group(1));
	}

	private static String unescapeJsonString(String value) {
		var out = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch != '\\' || i + 1 >= value.length()) {
				out.append(ch);
				continue;
			}
			char escaped = value.charAt(++i);
			switch (escaped) {
				case '"':
				case '\\':
				case '/':
					out.append(escaped);
					break;
				case 'b':
					out.append('\b');
					break;
				case 'f':
					out.append('\f');
					break;
				case 'n':
					out.append('\n');
					break;
				case 'r':
					out.append('\r');
					break;
				case 't':
					out.append('\t');
					break;
				case 'u':
					if (i + 4 >= value.length()) {
						throw new IllegalArgumentException("Invalid unicode escape in JSON");
					}
					String code = value.substring(i + 1, i + 5);
					out.append((char) Integer.parseInt(code, 16));
					i += 4;
					break;
				default:
					throw new IllegalArgumentException("Unsupported escape sequence: \\" + escaped);
			}
		}
		return out.toString();
	}

	private static String buildJavaContent(List<ProductEntry> entries) {
		var entriesCode = new StringBuilder();
		for (int i = 0; i < entries.size(); i++) {
			ProductEntry entry = entries.get(i);
			entriesCode.append("            entry(new ProductKey(")
					.append(entry.manufacturerId())
					.append(", ")
					.append(entry.productId())
					.append("), new ProductInfo(")
					.append(toJavaString(entry.manufacturer()))
					.append(", ")
					.append(toJavaString(entry.name()))
					.append("))");
			if (i < entries.size() - 1) {
				entriesCode.append(",");
			}
			entriesCode.append(System.lineSeparator());
		}

		return """
				package pl.grzeslowski.openhab.supla.internal.server.discovery;

				import static java.util.Map.entry;

				import java.util.Map;
				import java.util.Optional;
				import org.eclipse.jdt.annotation.NonNullByDefault;
				import org.eclipse.jdt.annotation.Nullable;

				@NonNullByDefault
				public final class SuplaProductsCatalog {
					private SuplaProductsCatalog() {}

					public record ProductKey(int manufacturerId, int productId) {}

					public record ProductInfo(String manufacturer, String name) {
						public String description() {
							var normalizedManufacturer = manufacturer.toLowerCase();
							var normalizedName = name.toLowerCase();
							if (normalizedName.startsWith(normalizedManufacturer + " ")) {
								return name;
							}
							return manufacturer + " " + name;
						}
					}

					private static final Map<ProductKey, ProductInfo> SUPLA_PRODUCTS = Map.ofEntries(
				%s				    );

					public static Optional<ProductInfo> findProductInfo(@Nullable Integer manufacturerId, @Nullable Integer productId) {
						if (manufacturerId == null || productId == null || manufacturerId <= 0 || productId <= 0) {
							return Optional.empty();
						}
						return Optional.ofNullable(SUPLA_PRODUCTS.get(new ProductKey(manufacturerId, productId)));
					}
				}
				"""
				.formatted(entriesCode);
	}

	private static String toJavaString(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
