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

import static java.lang.String.valueOf;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.*;

import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Grzeslowski - Initial contribution */
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "binding.supla")
@NonNullByDefault
public class BridgeDiscovery extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(BridgeDiscovery.class);

    public BridgeDiscovery() {
        super(Set.of(SUPLA_SERVER_TYPE), 10, true);
    }

    @Override
    protected void startScan() {
        discover();
        stopScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        discover();
    }

    private void discover() {
        ThingUID thingUID = new ThingUID(SUPLA_SERVER_TYPE, valueOf(DEFAULT_PORT));
        final String label = "Supla Server";
        var discoveryResult = DiscoveryResultBuilder.create(thingUID)
                .withProperties(buildThingProperties())
                .withRepresentationProperty(CONFIG_PORT)
                .withLabel(label)
                .build();
        logger.debug("Adding server to discovery; {}", label);
        thingDiscovered(discoveryResult);
    }

    private Map<String, Object> buildThingProperties() {
        return Map.of(CONFIG_PORT, DEFAULT_PORT);
    }
}
