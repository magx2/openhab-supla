package pl.grzeslowski.openhab.supla.internal.cloud;

import static java.util.Arrays.stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ChannelUID;

@NonNullByDefault
public class ChannelInfoParser {
    public static final ChannelInfoParser PARSER = new ChannelInfoParser();
    public static final String ID_DIVIDER = "__";

    public ChannelInfo parse(ChannelUID channelUID) {
        var fullId = channelUID.getId();
        var additionalChannelType = stream(AdditionalChannelType.values())
                .filter(type -> fullId.endsWith(type.getSuffix()))
                .findAny();
        Integer idx;
        String trimmedId;
        if (additionalChannelType.isPresent()) {
            var type = additionalChannelType.get();
            // idx suffix can exist only if additionalChannelType is present
            var tmpFullId =
                    fullId.substring(0, fullId.length() - type.getSuffix().length());
            if (tmpFullId.contains(ID_DIVIDER)) {
                var split = tmpFullId.split(ID_DIVIDER);
                var last = split[split.length - 1];
                try {
                    idx = Integer.parseInt(last);
                    trimmedId = tmpFullId.substring(0, tmpFullId.length() - last.length() - ID_DIVIDER.length());
                } catch (NumberFormatException ex) {
                    idx = null;
                    trimmedId = tmpFullId;
                }
            } else {
                idx = null;
                trimmedId = tmpFullId;
            }
        } else {
            trimmedId = fullId;
            idx = null;
        }
        return new ChannelInfo(parse(trimmedId, fullId), additionalChannelType.orElse(null), idx);
    }

    private int parse(String id, String fullId) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Given id `" + id + "` is not int! Full ID = `" + fullId + "`", ex);
        }
    }
}
