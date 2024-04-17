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
package pl.grzeslowski.supla.openhab.internal.cloud.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
@FunctionalInterface
public interface IoDevicesCloudApiFactory {
    IoDevicesCloudApi newIoDevicesCloudApi(String token);
}
