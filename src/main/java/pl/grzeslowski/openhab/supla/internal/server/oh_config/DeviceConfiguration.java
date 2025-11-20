package pl.grzeslowski.openhab.supla.internal.server.oh_config;

import org.eclipse.jdt.annotation.NonNull;

public record DeviceConfiguration(@NonNull TimeoutConfiguration timeoutConfiguration, @NonNull AuthData authData) {}
