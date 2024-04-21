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

import static java.time.Instant.now;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static pl.grzeslowski.jsupla.protocol.api.ResultCode.SUPLA_RESULTCODE_TRUE;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.DEVICE_TIMEOUT_SEC;
import static reactor.core.publisher.Flux.just;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.ToString;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocoljava.api.entities.dcs.PingServer;
import pl.grzeslowski.jsupla.protocoljava.api.entities.dcs.SetActivityTimeout;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.*;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sd.RegisterDeviceResult;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sdc.PingServerResultClient;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sdc.SetActivityTimeoutResult;
import pl.grzeslowski.jsupla.protocoljava.api.types.ToServerEntity;
import pl.grzeslowski.jsupla.server.api.Channel;
import pl.grzeslowski.supla.openhab.internal.server.discovery.ServerDiscoveryService;
import pl.grzeslowski.supla.openhab.internal.server.handler.AuthData;
import pl.grzeslowski.supla.openhab.internal.server.handler.ServerBridgeHandler;
import pl.grzeslowski.supla.openhab.internal.server.handler.ServerDeviceHandler;
import reactor.core.Disposable;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public final class ServerChannel implements AutoCloseable {
    private final SuplaDeviceRegistry suplaDeviceRegistry;
    private final Disposable subscription;
    private Logger logger = LoggerFactory.getLogger(ServerChannel.class);
    private final ServerBridgeHandler serverBridgeHandler;

    private final AuthData authData;

    private final ServerDiscoveryService serverDiscoveryService;
    private final Channel channel;
    private final ScheduledExecutorService scheduledPool;

    @ToString.Include
    private boolean authorized;

    @Nullable
    @ToString.Include
    private String guid;

    private final AtomicReference<ScheduledFuture<?>> pingSchedule = new AtomicReference<>();
    private final AtomicLong lastMessageFromDevice = new AtomicLong();

    @Nullable
    private ServerDeviceHandler serverDeviceHandler;

    public ServerChannel(
            SuplaDeviceRegistry suplaDeviceRegistry,
            ServerBridgeHandler serverBridgeHandler,
            AuthData authData,
            ServerDiscoveryService serverDiscoveryService,
            Channel channel,
            ScheduledExecutorService scheduledPool) {
        this.suplaDeviceRegistry = suplaDeviceRegistry;
        this.serverBridgeHandler = serverBridgeHandler;
        this.authData = authData;
        this.serverDiscoveryService = serverDiscoveryService;
        this.channel = channel;
        this.scheduledPool = scheduledPool;

        subscription = channel.getMessagePipe().subscribe(this::onNext, this::onError, this::onComplete);
    }

    @SuppressWarnings("deprecation")
    public synchronized void onNext(final ToServerEntity entity) {
        logger.trace("{} -> {}", guid, entity);
        lastMessageFromDevice.set(now().getEpochSecond());
        if (!authorized) {
            @Nullable final Runnable auth;
            @Nullable final DeviceChannels channels;
            @Nullable final String name;
            if (entity instanceof RegisterDevice registerDevice) {
                auth = () -> authorizeForLocation(
                        registerDevice.getGuid(), registerDevice.getLocationId(), registerDevice.getLocationPassword());
                this.guid = registerDevice.getGuid();
                channels = registerDevice.getChannels();
                if (registerDevice instanceof RegisterDeviceC registerDeviceC) {
                    final String serverName = registerDeviceC.getServerName();
                    if (isNullOrEmpty(serverName)) {
                        name = registerDeviceC.getName();
                    } else {
                        name = registerDeviceC.getName() + " " + serverName;
                    }
                } else {
                    name = registerDevice.getName();
                }
            } else if (entity instanceof RegisterDeviceD registerDevice) {
                auth = () -> authorizeForEmail(
                        registerDevice.getGuid(), registerDevice.getEmail(), registerDevice.getAuthKey());
                this.guid = registerDevice.getGuid();
                channels = new DeviceChannelsB(registerDevice.getChannels());
                name = registerDevice.getName();
            } else {
                logger.debug(
                        "Device in channel is not authorized in but is also not sending RegisterClient entity! {}",
                        entity);
                auth = null;
                channels = null;
                name = null;
            }
            if (auth != null && channels != null) {
                authorize(auth, channels, name);
            }
        } else if (entity instanceof SetActivityTimeout) {
            setActivityTimeout();
        } else if (entity instanceof PingServer) {
            pingServer((PingServer) entity);
        } else if (entity instanceof DeviceChannelValue) {
            deviceChannelValue((DeviceChannelValue) entity);
        } else {
            logger.debug("Do not handling this command: {}", entity);
        }
    }

    private boolean isNullOrEmpty(@Nullable String serverName) {
        return serverName == null || serverName.isEmpty();
    }

    private void authorize(
            Runnable authorize,
            @SuppressWarnings("deprecation") final DeviceChannels channels,
            @Nullable final String name) {
        logger = LoggerFactory.getLogger(this.getClass().getName() + "." + guid);
        authorize.run();
        if (authorized) {
            serverDiscoveryService.addSuplaDevice(requireNonNull(guid), name != null ? name : "<UNKNOWN>");
            sendRegistrationConfirmation();
            bindToThingHandler(channels, true);
        } else {
            logger.debug("Authorization failed for GUID {}", guid);
        }
    }

    private void authorizeForLocation(final String guid, final int accessId, final char[] accessIdPassword) {
        var locationAuthData = this.authData.locationAuthData();
        if (locationAuthData == null) {
            // not using access id authorization
            authorized = false;
            return;
        }
        if (locationAuthData.serverAccessId() != accessId) {
            logger.debug("Wrong accessId for GUID {}; {} != {}", guid, accessId, locationAuthData.serverAccessId());
            authorized = false;
            return;
        }
        if (!isGoodPassword(locationAuthData.serverAccessIdPassword().toCharArray(), accessIdPassword)) {
            logger.debug("Wrong accessIdPassword for GUID {}", guid);
            authorized = false;
            return;
        }
        logger.debug("Authorizing GUID {}", guid);
        authorized = true;
    }

    private void authorizeForEmail(final String guid, final String email, final String authKey) {
        var emailAuthData = this.authData.emailAuthData();
        if (emailAuthData == null) {
            // not using email authorization
            authorized = false;
            return;
        }
        if (!emailAuthData.email().equals(email)) {
            logger.debug("Wrong email for GUID {}; {} != {}", guid, email, emailAuthData.email());
            authorized = false;
            return;
        }
        if (!emailAuthData.authKey().equals(authKey)) {
            logger.debug("Wrong auth key for GUID {}; {} != {}", guid, authKey, emailAuthData.authKey());
            authorized = false;
            return;
        }
        logger.debug("Authorizing GUID {}", guid);
        authorized = true;
    }

    public void onError(final Throwable ex) {
        if (serverDeviceHandler != null) {
            logger.error("Error occurred in device. ", ex);
            serverDeviceHandler.updateStatus(
                    OFFLINE, COMMUNICATION_ERROR, "Error occurred in channel pipe. " + ex.getLocalizedMessage());
        }
    }

    public void onComplete() {
        logger.debug("onComplete() {}", this);
        this.serverBridgeHandler.completedChannel();
        if (serverDeviceHandler != null) {
            serverDeviceHandler.updateStatus(OFFLINE, ThingStatusDetail.NONE, "Device went offline");
        }
    }

    private void setActivityTimeout() {
        final SetActivityTimeoutResult data =
                new SetActivityTimeoutResult(DEVICE_TIMEOUT_SEC, DEVICE_TIMEOUT_SEC - 2, DEVICE_TIMEOUT_SEC + 2);
        channel.write(just(data))
                .subscribe(date -> logger.trace("setActivityTimeout {} {}", data, date.format(ISO_DATE_TIME)));
        final ScheduledFuture<?> pingSchedule = scheduledPool.scheduleWithFixedDelay(
                this::checkIfDeviceIsUp, DEVICE_TIMEOUT_SEC * 2, DEVICE_TIMEOUT_SEC, TimeUnit.SECONDS);
        this.pingSchedule.set(pingSchedule);
    }

    private void checkIfDeviceIsUp() {
        final long now = now().getEpochSecond();
        if (now - lastMessageFromDevice.get() > DEVICE_TIMEOUT_SEC) {
            logger.debug("Device {} is dead. Need to kill it!", guid);
            channel.close();
            var scheduledFuture = this.pingSchedule.getAndSet(null);
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            var localSuplaDeviceHandler = serverDeviceHandler;
            if (localSuplaDeviceHandler != null) {
                localSuplaDeviceHandler.updateStatus(
                        OFFLINE, ThingStatusDetail.NONE, "Device do not response on pings.");
            }
        }
    }

    private void pingServer(final PingServer entity) {
        final PingServerResultClient response = new PingServerResultClient(entity.getTimeval());
        channel.write(just(response))
                .subscribe(date -> logger.trace(
                        "pingServer {}s {}ms {}",
                        response.getTimeval().getSeconds(),
                        response.getTimeval().getSeconds(),
                        date.format(ISO_DATE_TIME)));
    }

    private void sendRegistrationConfirmation() {
        final RegisterDeviceResult result = new RegisterDeviceResult(SUPLA_RESULTCODE_TRUE.getValue(), 100, 5, 1);
        channel.write(just(result))
                .subscribe(date -> logger.trace("Send register response at {}", date.format(ISO_DATE_TIME)));
    }

    private boolean isGoodPassword(final char[] password, final char[] givenPassword) {
        if (password.length > givenPassword.length) {
            return false;
        }
        for (int i = 0; i < password.length; i++) {
            if (password[i] != givenPassword[i]) {
                return false;
            }
        }
        return true;
    }

    private void bindToThingHandler(
            @SuppressWarnings("deprecation") final DeviceChannels channels, boolean createTask) {
        logger.debug("Trying to bind channels...");
        var suplaDevice = suplaDeviceRegistry.getSuplaDevice(requireNonNull(guid));
        if (suplaDevice.isPresent()) {
            serverDeviceHandler = suplaDevice.get();
            serverDeviceHandler.setChannels(channels);
            serverDeviceHandler.setSuplaChannel(channel);
        } else {
            if (createTask) {
                logger.debug("Thing not found. Binding of channels will happen later...");
                scheduledPool.schedule(() -> bindToThingHandler(channels, false), DEVICE_TIMEOUT_SEC, SECONDS);
            }
        }
    }

    private void deviceChannelValue(final DeviceChannelValue entity) {
        requireNonNull(serverDeviceHandler).updateStatus(entity.getChannelNumber(), entity.getValue());
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (Exception ex) {
            logger.error("Could not close Supla channel! Probably you need to restart Open HAB (or machine)", ex);
        }
        subscription.dispose();
        {
            var scheduledFuture = this.pingSchedule.getAndSet(null);
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            serverDeviceHandler = null;
        }
    }
}
