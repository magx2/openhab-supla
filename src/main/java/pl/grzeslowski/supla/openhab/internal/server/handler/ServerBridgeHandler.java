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
package pl.grzeslowski.supla.openhab.internal.server.handler;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static pl.grzeslowski.jsupla.server.api.ServerProperties.fromList;
import static pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory.PORT;
import static pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory.SSL_CTX;
import static pl.grzeslowski.supla.openhab.internal.Documentation.DISABLED_ALGORITHMS_PROBLEM;
import static pl.grzeslowski.supla.openhab.internal.Documentation.SSL_PROBLEM;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.BINDING_ID;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.CONNECTED_DEVICES_CHANNEL_ID;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.impl.calltypes.CallTypeParserImpl;
import pl.grzeslowski.jsupla.protocol.impl.decoders.DecoderFactoryImpl;
import pl.grzeslowski.jsupla.protocol.impl.encoders.EncoderFactoryImpl;
import pl.grzeslowski.jsupla.server.api.Channel;
import pl.grzeslowski.jsupla.server.api.Server;
import pl.grzeslowski.jsupla.server.api.ServerFactory;
import pl.grzeslowski.jsupla.server.api.ServerProperties;
import pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory;
import pl.grzeslowski.supla.openhab.internal.server.ServerChannel;
import pl.grzeslowski.supla.openhab.internal.server.SuplaDeviceRegistry;
import pl.grzeslowski.supla.openhab.internal.server.discovery.ServerDiscoveryService;
import reactor.core.Disposable;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public class ServerBridgeHandler extends BaseBridgeHandler {
    private static final int PROPER_AES_KEY_SIZE = 2147483647;
    private Logger logger = LoggerFactory.getLogger(ServerBridgeHandler.class);
    private final SuplaDeviceRegistry suplaDeviceRegistry;

    @Nullable
    private Server server;

    @Nullable
    private ServerDiscoveryService serverDiscoveryService;

    private final AtomicInteger numberOfConnectedDevices = new AtomicInteger();
    private final Collection<ServerChannel> channels = new ArrayList<>();
    private final Object channelsLock = new Object();

    @Nullable
    private Disposable newChannelsPipeline;

    public ServerBridgeHandler(final Bridge bridge, final SuplaDeviceRegistry suplaDeviceRegistry) {
        super(bridge);
        this.suplaDeviceRegistry = suplaDeviceRegistry;
    }

    @Override
    public void initialize() {
        try {
            internalInitialize();
        } catch (CertificateException | SSLException ex) {
            logger.debug("Problem with generating certificates! ", ex);
            updateStatus(
                    OFFLINE,
                    CONFIGURATION_ERROR,
                    "Problem with generating certificates! " + ex.getLocalizedMessage() + ". See: " + SSL_PROBLEM);
        } catch (Exception ex) {
            logger.debug("Cannot start server! ", ex);
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "Cannot start server! " + ex.getLocalizedMessage());
        }
    }

    private void internalInitialize() throws Exception {
        var config = this.getConfigAs(ServerBridgeHandlerConfig.class);
        if (!config.isServerAuth() && !config.isEmailAuth()) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "You need to pass either server auth or email auth!");
            return;
        }
        if (config.getPort().intValue() <= 0) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "You need to pass port!");
            return;
        }
        var authData = buildAuthData(config);
        var port = config.getPort().intValue();
        var protocols =
                stream(config.getProtocols().split(",")).map(String::trim).collect(toSet());
        if (protocols.isEmpty()) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "You need to pass at least one protocol!");
            return;
        }

        logger = LoggerFactory.getLogger(ServerBridgeHandler.class.getName() + "." + port);

        var scheduledPool = ThreadPoolManager.getScheduledPool(BINDING_ID + "." + port);

        if (config.isSsl()) {
            var maxKeySize = javax.crypto.Cipher.getMaxAllowedKeyLength("AES");
            if (maxKeySize < PROPER_AES_KEY_SIZE) {
                logger.warn(
                        "AES key size is too small, {} < {}! Probably you need to enable unlimited crypto. See: {}",
                        maxKeySize,
                        PROPER_AES_KEY_SIZE,
                        SSL_PROBLEM);
            }
        } else {
            logger.info("Disabling SSL is not supported");
        }

        try {
            var propsFiled = Security.class.getDeclaredField("props");
            propsFiled.setAccessible(true);
            var props = (Properties) propsFiled.get(null); // Static field, so null object.
            var disabled = props.get("jdk.tls.disabledAlgorithms");
            logger.debug("jdk.tls.disabledAlgorithms={}", disabled);
            if (disabled != null) {
                var disabledAlgorithms =
                        stream(disabled.toString().split(",")).map(String::trim).collect(toCollection(HashSet::new));
                var protocolsWithSslv3 = new HashSet<>(protocols);
                protocolsWithSslv3.add("SSLv3");
                disabledAlgorithms.retainAll(protocolsWithSslv3);
                if (!disabledAlgorithms.isEmpty()) {
                    var algorithms = String.join(", ", disabledAlgorithms);
                    updateStatus(
                            OFFLINE,
                            CONFIGURATION_ERROR,
                            "Those protocols are disabled in java.security: %s. See: %s"
                                    .formatted(algorithms, DISABLED_ALGORITHMS_PROBLEM));
                    return;
                }
            } else {
                logger.debug("jdk.tls.disabledAlgorithms is null, should not be a problem, carry on...");
            }
        } catch (Exception ex) {
            logger.warn("Cannot get disabled algorithms! {}", ex.getLocalizedMessage());
        }

        var factory = buildServerFactory();
        var localServer = server = factory.createNewServer(buildServerProperties(port, protocols));
        newChannelsPipeline = localServer
                .getNewChannelsPipe()
                .subscribe(o -> channelConsumer(o, authData, scheduledPool), this::errorOccurredInChannel);

        logger.debug("jSuplaServer running on port {}", port);
        updateStatus(ONLINE);
        updateConnectedDevices(0);
    }

    private static AuthData buildAuthData(ServerBridgeHandlerConfig config) {
        AuthData.@Nullable LocationAuthData locationAuthData;
        if (config.getServerAccessId() != null && config.getServerAccessIdPassword() != null) {
            locationAuthData = new AuthData.LocationAuthData(
                    config.getServerAccessId().intValue(), config.getServerAccessIdPassword());
        } else {
            locationAuthData = null;
        }
        AuthData.@Nullable EmailAuthData emailAuthData;
        if (config.getEmail() != null && config.getAuthKey() != null) {
            emailAuthData = new AuthData.EmailAuthData(config.getEmail(), config.getAuthKey());
        } else {
            emailAuthData = null;
        }
        return new AuthData(locationAuthData, emailAuthData);
    }

    private void channelConsumer(Channel channel, AuthData authData, @NonNull ScheduledExecutorService scheduledPool) {
        logger.debug("Device connected");
        changeNumberOfConnectedDevices(1);
        var jSuplaChannel = new ServerChannel(
                suplaDeviceRegistry, this, authData, requireNonNull(serverDiscoveryService), channel, scheduledPool);
        synchronized (channelsLock) {
            channels.add(jSuplaChannel);
        }
    }

    private void errorOccurredInChannel(Throwable ex) {
        logger.error("Error occurred in server pipe", ex);
        updateStatus(
                OFFLINE, COMMUNICATION_ERROR, "Error occurred in server pipe. Message: " + ex.getLocalizedMessage());
    }

    public void completedChannel() {
        logger.debug("Device disconnected from");
        changeNumberOfConnectedDevices(-1);
    }

    private void changeNumberOfConnectedDevices(int delta) {
        var number = numberOfConnectedDevices.addAndGet(delta);
        updateConnectedDevices(number);
    }

    private void updateConnectedDevices(int numberOfConnectedDevices) {
        updateState(CONNECTED_DEVICES_CHANNEL_ID, new DecimalType(numberOfConnectedDevices));
    }

    private ServerFactory buildServerFactory() {
        return new NettyServerFactory(
                new CallTypeParserImpl(), DecoderFactoryImpl.INSTANCE, EncoderFactoryImpl.INSTANCE);
    }

    private ServerProperties buildServerProperties(int port, Set<String> protocols)
            throws CertificateException, SSLException {
        return fromList(Arrays.asList(PORT, port, SSL_CTX, buildSslContext(protocols)));
    }

    private SslContext buildSslContext(Set<String> protocols) throws CertificateException, SSLException {
        var ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .protocols(protocols)
                .build();
    }

    @Override
    public void dispose() {
        disposeNewChannelsPipeline();
        disposeChannels();
        disposeServer();
        logger = LoggerFactory.getLogger(ServerBridgeHandler.class);
        super.dispose();
    }

    private void disposeNewChannelsPipeline() {
        logger.debug("Disposing newChannelsPipeline");
        var local = newChannelsPipeline;
        newChannelsPipeline = null;
        if (local != null) {
            local.dispose();
        }
    }

    private void disposeChannels() {
        synchronized (channelsLock) {
            if (channels.isEmpty()) {
                logger.debug("No channels to close");
                return;
            }
            var channelsCopy = new ArrayList<>(channels);
            channels.clear();
            for (var channel : channelsCopy) {
                try {
                    logger.debug("Closing channel {}", channel);
                    channel.close();
                } catch (Exception ex) {
                    logger.error("Could not close channel! Probably you need to restart Open HAB (or machine)", ex);
                }
            }
        }
    }

    private void disposeServer() {
        var local = server;
        server = null;
        if (local != null) {
            try {
                local.close();
            } catch (Exception ex) {
                logger.error("Could not close server! Probably you need to restart Open HAB (or machine)", ex);
            }
        }
    }

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        // no commands in this bridge
    }

    public void setSuplaDiscoveryService(final ServerDiscoveryService serverDiscoveryService) {
        logger.trace("setSuplaDiscoveryService#{}", serverDiscoveryService.hashCode());
        this.serverDiscoveryService = serverDiscoveryService;
    }
}
