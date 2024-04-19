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
package pl.grzeslowski.supla.openhab.internal.server;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.osgi.service.component.annotations.Component;
import pl.grzeslowski.supla.openhab.internal.server.handler.SuplaDeviceHandler;

/** @author Grzeslowski - Initial contribution */
@Slf4j
@NonNullByDefault
@Component(service = SuplaDeviceRegistry.class, immediate = true, configurationPid = "binding.supla")
public class SuplaDeviceRegistry {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<SuplaDeviceHandler> suplaDeviceHandlers = new HashSet<>();

    public void addSuplaDevice(final SuplaDeviceHandler suplaDeviceHandler) {
        lock.writeLock().lock();
        try {
            suplaDeviceHandlers.add(suplaDeviceHandler);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<SuplaDeviceHandler> getSuplaDevice(final String guid) {
        requireNonNull(guid);
        lock.readLock().lock();
        try {
            return suplaDeviceHandlers.stream()
                    .filter(device -> guid.equals(device.getThing().getUID().getId()))
                    .findAny();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void removeSuplaDevice(SuplaDeviceHandler suplaDeviceHandler) {
        lock.writeLock().lock();
        try {
            var remove = suplaDeviceHandlers.remove(suplaDeviceHandler);
            if (!remove) {
                log.warn("Could not remove SuplaDeviceHandler from registry: {}", suplaDeviceHandler);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
