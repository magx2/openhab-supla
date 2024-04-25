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

import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.*;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.ServerDevicesProperties.CONFIG_AUTH_PROPERTY;

import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.RegisterDeviceD;
import pl.grzeslowski.jsupla.protocoljava.api.types.traits.RegisterDeviceTrait;
import pl.grzeslowski.jsupla.server.api.Channel;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public class ServerDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger;
    private final ThingUID bridgeThingUID;

    @Nullable
    Flux<? extends Channel> newDeviceFlux;

    @Nullable
    private Disposable subscription;

    public ServerDiscoveryService(org.openhab.core.thing.ThingUID bridgeThingUID) {
        super(SUPPORTED_THING_TYPES_UIDS, DEVICE_REGISTER_MAX_DELAY * 2, false);
        logger = LoggerFactory.getLogger(ServerDiscoveryService.class.getName() + "." + bridgeThingUID.getId());
        this.bridgeThingUID = bridgeThingUID;
    }

    @Override
    protected void startScan() {
        var channel = newDeviceFlux;
        if (channel == null) {
            logger.debug("No channel, canceling scan");
            stopScan();
            return;
        }
        logger.info("Starting scan...");
        cancelSubscription();
        subscription = newDeviceFlux
                .flatMap(Channel::getMessagePipe)
                .filter(entity -> entity instanceof RegisterDeviceTrait)
                .cast(RegisterDeviceTrait.class)
                .take(Duration.ofSeconds(getScanTimeout()))
                .map(this::buildDiscoveryResult)
                .log(logger.getName(), Level.FINE)
                .subscribe(
                        this::thingDiscovered,
                        ex -> logger.error("Error occurred during discovery", ex),
                        this::stopScan);
    }

    @Override
    protected synchronized void stopScan() {
        cancelSubscription();
        super.stopScan();
    }

    private synchronized void cancelSubscription() {
        var local = subscription;
        subscription = null;
        if (local != null) {
            logger.debug("Cancelling current subscription");
            local.dispose();
        }
    }

    private DiscoveryResult buildDiscoveryResult(RegisterDeviceTrait registerDeviceTrait) {
        var builder = buildDiscoveryResult(registerDeviceTrait.getGuid(), registerDeviceTrait.getName());
        if (registerDeviceTrait instanceof RegisterDeviceD registerDevice) {
            builder.withProperties(Map.of(CONFIG_AUTH_PROPERTY, registerDevice.getAuthKey()));
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

    public void setNewDeviceFlux(@Nullable Flux<? extends Channel> newDeviceFlux) {
        cancelSubscription();
        this.newDeviceFlux = newDeviceFlux;
    }
}
