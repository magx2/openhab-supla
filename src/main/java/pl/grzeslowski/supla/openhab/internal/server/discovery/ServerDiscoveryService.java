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

import static java.util.Objects.requireNonNull;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.*;

import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.supla.openhab.internal.server.handler.ServerBridgeHandler;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public class ServerDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(ServerDiscoveryService.class);
    private final ServerBridgeHandler serverBridgeHandler;

    public ServerDiscoveryService(final ServerBridgeHandler serverBridgeHandler) {
        super(SUPPORTED_THING_TYPES_UIDS, 10, true);
        this.serverBridgeHandler = requireNonNull(serverBridgeHandler);
    }

    @Override
    protected void startScan() {
        // Active scan started, but there is no active scan for supla ;)
        stopScan();
    }

    public void addSuplaDevice(String guid, String name) {
        var bridgeUID = serverBridgeHandler.getThing().getUID();
        var thingUID = new ThingUID(SUPLA_SERVER_DEVICE_TYPE, bridgeUID, guid);
        var discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withBridge(bridgeUID)
                .withProperties(buildProperties(guid))
                .withRepresentationProperty(SUPLA_DEVICE_GUID)
                .withLabel(String.format("%s (%s)", name, guid))
                .build();
        logger.debug("Discovered thing with GUID [{}]", guid);
        thingDiscovered(discoveryResult);
    }

    private Map<String, Object> buildProperties(final String guid) {
        return Map.of(SUPLA_DEVICE_GUID, guid);
    }
}
