package pl.grzeslowski.openhab.supla.internal;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public final class Localization {
    private Localization() {}

    public static String text(String key, Object... args) {
        if (args.length == 0) {
            return "@text/" + key;
        }
        var arguments = Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(", "));
        return "@text/" + key + " [" + arguments + "]";
    }
}
