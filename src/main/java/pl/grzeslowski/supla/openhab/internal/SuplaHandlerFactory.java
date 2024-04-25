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
package pl.grzeslowski.supla.openhab.internal;

import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.*;

import java.util.Hashtable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.supla.openhab.internal.cloud.discovery.CloudDiscovery;
import pl.grzeslowski.supla.openhab.internal.cloud.handler.CloudBridgeHandler;
import pl.grzeslowski.supla.openhab.internal.cloud.handler.CloudDeviceHandler;
import pl.grzeslowski.supla.openhab.internal.server.discovery.ServerDiscoveryService;
import pl.grzeslowski.supla.openhab.internal.server.handler.ServerBridgeHandler;
import pl.grzeslowski.supla.openhab.internal.server.handler.ServerDeviceHandler;

/**
 * The {@link SuplaHandlerFactory} is responsible for creating things and thing handlers.
 *
 * @author Grzeslowski - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPid = "binding.supla")
@NonNullByDefault
public class SuplaHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(SuplaHandlerFactory.class);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPLA_SERVER_DEVICE_TYPE.equals(thingTypeUID)) {
            return newServerDeviceHandler(thing);
        }

        if (SUPLA_CLOUD_DEVICE_TYPE.equals(thingTypeUID)) {
            return newCloudDeviceHandler(thing);
        }

        if (SUPLA_SERVER_TYPE.equals(thingTypeUID)) {
            return newServerBridgeHandler((Bridge) thing);
        }

        if (SUPLA_CLOUD_SERVER_TYPE.equals(thingTypeUID)) {
            return newCloudBridgeHandler(thing);
        }

        return null;
    }

    @NonNull
    private ThingHandler newServerDeviceHandler(final Thing thing) {
        return new ServerDeviceHandler(thing);
    }

    @NonNull
    private ThingHandler newServerBridgeHandler(final Bridge thing) {
        var discovery = new ServerDiscoveryService(thing.getUID());
        var bridgeHandler = new ServerBridgeHandler(thing, discovery);
        registerThingDiscovery(discovery);
        return bridgeHandler;
    }

    @NonNull
    private ThingHandler newCloudBridgeHandler(final Thing thing) {
        var bridgeHandler = new CloudBridgeHandler((Bridge) thing);
        var cloudDiscovery = new CloudDiscovery(bridgeHandler);
        registerThingDiscovery(cloudDiscovery);
        return bridgeHandler;
    }

    @NonNull
    private ThingHandler newCloudDeviceHandler(final Thing thing) {
        return new CloudDeviceHandler(thing);
    }

    private synchronized void registerThingDiscovery(DiscoveryService discoveryService) {
        logger.trace(
                "Try to register Discovery service on BundleID: {} Service: {}",
                bundleContext.getBundle().getBundleId(),
                DiscoveryService.class.getName());
        bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<>());
    }
}
