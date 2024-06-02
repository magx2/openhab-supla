package pl.grzeslowski.supla.openhab.internal.cloud.api;

import static java.util.Objects.requireNonNull;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
final class OneLineHttpLoggingInterceptor implements Interceptor {
    private final HttpLoggingInterceptor.Logger logger;
    private final HttpLoggingInterceptor.Level level;

    OneLineHttpLoggingInterceptor(HttpLoggingInterceptor.Logger logger, HttpLoggingInterceptor.Level level) {
        this.logger = logger;
        this.level = level;
    }

    @Override
    public Response intercept(@Nullable final Chain chain) throws IOException {
        final StringBuilderLogger stringBuilderLogger = new StringBuilderLogger();
        final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(stringBuilderLogger);
        interceptor.setLevel(level);
        final Response response = interceptor.intercept(requireNonNull(chain));
        this.logger.log(stringBuilderLogger.wholeMessage());
        return response;
    }

    private static final class StringBuilderLogger implements HttpLoggingInterceptor.Logger {
        private final StringBuilder stringBuilder = new StringBuilder("Log for request:\n");

        @Override
        public void log(@Nullable final String message) {
            stringBuilder.append(message).append("\n");
        }

        String wholeMessage() {
            return stringBuilder.toString();
        }
    }
}
