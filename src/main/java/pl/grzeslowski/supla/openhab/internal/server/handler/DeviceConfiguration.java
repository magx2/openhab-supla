package pl.grzeslowski.supla.openhab.internal.server.handler;

import org.eclipse.jdt.annotation.NonNull;

record DeviceConfiguration(@NonNull TimeoutConfiguration timeoutConfiguration, @NonNull AuthData authData) {}
