package pl.grzeslowski.supla.openhab.internal.cloud.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
@FunctionalInterface
public interface IoDevicesCloudApiFactory {
    IoDevicesCloudApi newIoDevicesCloudApi(String token);
}
