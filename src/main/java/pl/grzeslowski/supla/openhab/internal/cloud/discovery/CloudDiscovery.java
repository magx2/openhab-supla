package pl.grzeslowski.supla.openhab.internal.cloud.discovery;

import static java.util.Collections.singletonList;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.*;

import io.swagger.client.model.Device;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.supla.openhab.internal.cloud.handler.CloudBridgeHandler;

/** @author Martin Grzeslowski - Initial contribution */
@NonNullByDefault
public final class CloudDiscovery extends AbstractDiscoveryService {
    private final Logger logger;
    private final CloudBridgeHandler bridgeHandler;

    public CloudDiscovery(CloudBridgeHandler bridgeHandler) {
        super(SUPPORTED_THING_TYPES_UIDS, 10, true);
        logger = LoggerFactory.getLogger(CloudDiscovery.class.getName() + "."
                + bridgeHandler.getThing().getUID().getId());
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    protected void startScan() {
        try {
            bridgeHandler.getIoDevices(singletonList("channels")).forEach(this::addThing);
        } catch (Exception e) {
            logger.error("Cannot get IO devices from Supla Cloud!", e);
            stopScan();
        }
    }

    private void addThing(Device device) {
        final ThingUID thingUID = new ThingUID(SUPLA_CLOUD_DEVICE_TYPE, findBridgeUID(), device.getGUIDString());
        final DiscoveryResult discoveryResult =
                createDiscoveryResult(thingUID, buildThingLabel(device), buildThingProperties(device));
        thingDiscovered(discoveryResult);
    }

    private ThingUID findBridgeUID() {
        return bridgeHandler.getThing().getUID();
    }

    private DiscoveryResult createDiscoveryResult(ThingUID thingUID, String label, Map<String, Object> properties) {
        return DiscoveryResultBuilder.create(thingUID)
                .withBridge(findBridgeUID())
                .withProperties(properties)
                .withLabel(label)
                .build();
    }

    private String buildThingLabel(Device device) {
        var sb = new StringBuilder();

        var name = device.getName();
        if (!isNullOrEmpty(name)) {
            var comment = device.getComment();
            if (!isNullOrEmpty(comment)) {
                sb.append(comment).append(" (").append(name).append(")");
            } else {
                sb.append(name);
            }
        }

        var primaryLabel = sb.toString();
        if (!isNullOrEmpty(primaryLabel)) {
            return primaryLabel;
        }

        return device.getGUIDString();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isNullOrEmpty(@Nullable String name) {
        return name == null || name.isEmpty();
    }

    private Map<String, Object> buildThingProperties(Device device) {
        return Map.of(SUPLA_DEVICE_GUID, device.getGUIDString(), SUPLA_DEVICE_CLOUD_ID, device.getId());
    }
}
