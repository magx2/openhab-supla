package pl.grzeslowski.openhab.supla.internal.server.discovery;

import static pl.grzeslowski.openhab.supla.internal.GuidLogger.attachGuid;
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
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.openhab.supla.internal.GuidLogger;
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

    public void addSubDevice(int id, String name) {
        logger.info("Registering sub device: {}/{}", name, id);
        var discoveryResult = buildDiscoveryResult(id, name);
        discoveryResults.add(discoveryResult);
        thingDiscovered(discoveryResult);
    }

    @GuidLogger.GuidLogged
    public void removeDevice(String guid) {
        attachGuid(guid, () -> {
            val result = discoveryResults.stream()
                    .filter(r -> r.getThingUID().getId().equals(guid))
                    .findAny();
            if (result.isPresent()) {
                logger.info("Removing device: {}", guid);
                thingRemoved(result.get().getThingUID());
            } else {
                logger.warn("Failed to remove device: {}", guid);
            }
        });
    }

    @GuidLogger.GuidLogged
    public void removeSubDevice(int id) {
        removeDevice(String.valueOf(id));
    }

    private DiscoveryResult buildDiscoveryResult(RegisterDeviceTrait trait) {
        var guid = trait.guid();
        var name = trait.name();
        var gateway = trait.channels().stream().anyMatch(c -> c.subDeviceId() != null && c.subDeviceId() > 0);
        var type = gateway ? SUPLA_GATEWAY_DEVICE_TYPE : SUPLA_SERVER_DEVICE_TYPE;

        var builder = buildDiscoveryResult(SUPLA_DEVICE_GUID, guid, name, type);
        if (trait instanceof RegisterEmailDeviceTrait registerDevice) {
            var authKey = registerDevice.authKey();
            var serverName = registerDevice.serverName();
            builder.withProperty(CONFIG_AUTH_PROPERTY, bytesToHex(authKey));
            builder.withProperty(SERVER_NAME_PROPERTY, serverName);
        }
        return builder.build();
    }

    private DiscoveryResult buildDiscoveryResult(int id, String name) {
        return buildDiscoveryResult(SUPLA_SUB_DEVICE_ID, String.valueOf(id), name + " #" + id, SUPLA_SUB_DEVICE_TYPE)
                .build();
    }

    private DiscoveryResultBuilder buildDiscoveryResult(
            String idKey, String id, @Nullable String name, ThingTypeUID type) {
        var thingUID = new ThingUID(type, bridgeThingUID, id);
        var label = buildLabel(id, name);
        return DiscoveryResultBuilder.create(thingUID)
                .withBridge(bridgeThingUID)
                .withProperties(Map.of(idKey, id))
                .withRepresentationProperty(idKey)
                .withLabel(label);
    }

    private static String buildLabel(String guid, @Nullable String name) {
        if (name == null || name.isEmpty()) {
            return guid;
        }
        return name;
    }
}
