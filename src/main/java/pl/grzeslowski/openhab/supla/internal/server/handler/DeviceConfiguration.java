package pl.grzeslowski.openhab.supla.internal.server.handler;

import org.eclipse.jdt.annotation.NonNull;

record DeviceConfiguration(@NonNull TimeoutConfiguration timeoutConfiguration, @NonNull AuthData authData) {}
