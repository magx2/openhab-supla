package pl.grzeslowski.openhab.supla.internal.cloud.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
@FunctionalInterface
public interface IoDevicesCloudApiFactory {
    IoDevicesCloudApi newIoDevicesCloudApi(String token);
}
