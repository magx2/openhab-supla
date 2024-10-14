package pl.grzeslowski.openhab.supla.internal.server.discovery;

import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.CONFIG_AUTH_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.SERVER_NAME_PROPERTY;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.bytesToHex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.val;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterEmailDeviceTrait;

@NonNullByDefault
public class ServerDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger;
    private final ThingUID bridgeThingUID;
    private final List<DiscoveryResult> discoveryResults = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean scanning = new AtomicBoolean();

    public ServerDiscoveryService(org.openhab.core.thing.ThingUID bridgeThingUID) {
        super(SUPPORTED_THING_TYPES_UIDS, DEVICE_REGISTER_MAX_DELAY * 2, false);
        logger = LoggerFactory.getLogger(ServerDiscoveryService.class.getName() + "." + bridgeThingUID.getId());
        this.bridgeThingUID = bridgeThingUID;
    }

    @Override
    protected void startScan() {}

    public void addDevice(RegisterDeviceTrait registerDeviceTrait) {
        logger.info("Registering device: {}", registerDeviceTrait);
        var discoveryResult = buildDiscoveryResult(registerDeviceTrait);
        discoveryResults.add(discoveryResult);
        thingDiscovered(discoveryResult);
    }

    public void removeDevice(String guid) {
        val result = discoveryResults.stream()
                .filter(r -> r.getThingUID().getId().equals(guid))
                .findAny();
        if (result.isPresent()) {
            logger.info("Removing device: {}", guid);
            thingRemoved(result.get().getThingUID());
        } else {
            logger.warn("Failed to remove device: {}", guid);
        }
    }

    private DiscoveryResult buildDiscoveryResult(RegisterDeviceTrait registerDeviceTrait) {
        var guid = registerDeviceTrait.getGuid();
        var name = registerDeviceTrait.getName();
        var builder = buildDiscoveryResult(guid, name);
        if (registerDeviceTrait instanceof RegisterEmailDeviceTrait registerDevice) {
            var authKey = registerDevice.getAuthKey();
            var serverName = registerDevice.getServerName();
            builder.withProperty(CONFIG_AUTH_PROPERTY, bytesToHex(authKey));
            builder.withProperty(SERVER_NAME_PROPERTY, serverName);
        }
        return builder.build();
    }

    private DiscoveryResultBuilder buildDiscoveryResult(String guid, @Nullable String name) {
        var thingUID = new ThingUID(SUPLA_SERVER_DEVICE_TYPE, bridgeThingUID, guid);
        var label = buildLabel(guid, name);
        return DiscoveryResultBuilder.create(thingUID)
                .withBridge(bridgeThingUID)
                .withProperties(Map.of(SUPLA_DEVICE_GUID, guid))
                .withRepresentationProperty(SUPLA_DEVICE_GUID)
                .withLabel(label);
    }

    private static String buildLabel(String guid, @Nullable String name) {
        if (name == null || name.isEmpty()) {
            return guid;
        }
        return name;
    }
}
