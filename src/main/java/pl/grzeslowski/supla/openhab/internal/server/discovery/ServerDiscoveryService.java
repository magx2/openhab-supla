/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * <p>See the NOTICE file(s) distributed with this work for additional information.
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package pl.grzeslowski.supla.openhab.internal.server.discovery;

import static java.util.Collections.synchronizedSet;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public class ServerDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(ServerDiscoveryService.class);
    private final Set<SuplaDevice> suplaDevices = synchronizedSet(new HashSet<>());
    private final ThingUID bridgeThingUID;

    public ServerDiscoveryService(org.openhab.core.thing.ThingUID bridgeThingUID) {
        super(SUPPORTED_THING_TYPES_UIDS, 10, true);
        this.bridgeThingUID = bridgeThingUID;
    }

    @Override
    protected void startScan() {
        suplaDevices.stream().map(this::buildDiscoveryResult).forEach(this::thingDiscovered);
        suplaDevices.clear();
        stopScan();
    }

    private DiscoveryResult buildDiscoveryResult(SuplaDevice suplaDevice) {
        var thingUID = new ThingUID(SUPLA_SERVER_DEVICE_TYPE, bridgeThingUID, suplaDevice.guid);
        var label = buildLabel(suplaDevice);
        return DiscoveryResultBuilder.create(thingUID)
                .withBridge(bridgeThingUID)
                .withProperties(Map.of(SUPLA_DEVICE_GUID, suplaDevice.guid))
                .withRepresentationProperty(SUPLA_DEVICE_GUID)
                .withLabel(label)
                .build();
    }

    private static String buildLabel(SuplaDevice suplaDevice) {
        if (suplaDevice.name == null || suplaDevice.name.isEmpty()) {
            return suplaDevice.guid;
        }
        return String.format("%s (%s)", suplaDevice.name, suplaDevice.guid);
    }

    public void addSuplaDevice(SuplaDevice suplaDevice) {
        logger.debug("Discovered thing with {}", suplaDevice);
        suplaDevices.add(suplaDevice);
    }

    public void addSuplaDevice(String guid, @Nullable String name) {
        addSuplaDevice(new SuplaDevice(guid, name));
    }

    public void removeSuplaDevice(String guid) {
        var remove = suplaDevices.remove(new SuplaDevice(guid, null));
        if (remove) {
            logger.debug("Removing discovered thing with GUID [{}]", guid);
        }
    }

    public record SuplaDevice(String guid, @Nullable String name) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SuplaDevice that = (SuplaDevice) o;
            return guid.equals(that.guid);
        }

        @Override
        public int hashCode() {
            return guid.hashCode();
        }
    }
}
