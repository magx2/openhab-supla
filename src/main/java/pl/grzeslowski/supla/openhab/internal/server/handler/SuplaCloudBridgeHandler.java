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

import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static pl.grzeslowski.jsupla.server.api.ServerProperties.fromList;
import static pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory.PORT;
import static pl.grzeslowski.jsupla.server.netty.api.NettyServerFactory.SSL_CTX;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatusDetail;
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
import pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants;
import pl.grzeslowski.supla.openhab.internal.server.SuplaChannel;
import pl.grzeslowski.supla.openhab.internal.server.SuplaDeviceRegistry;
import pl.grzeslowski.supla.openhab.internal.server.discovery.ServerDiscoveryService;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public class SuplaCloudBridgeHandler extends BaseBridgeHandler {
    private Logger logger = LoggerFactory.getLogger(SuplaCloudBridgeHandler.class);
    private final SuplaDeviceRegistry suplaDeviceRegistry;

    @Nullable
    private Server server;

    @Nullable
    private ServerDiscoveryService serverDiscoveryService;

    private final AtomicInteger numberOfConnectedDevices = new AtomicInteger();

    public SuplaCloudBridgeHandler(final Bridge bridge, final SuplaDeviceRegistry suplaDeviceRegistry) {
        super(bridge);
        this.suplaDeviceRegistry = suplaDeviceRegistry;
    }

    @Override
    public void initialize() {
        updateConnectedDevices(0);
        final ServerFactory factory = buildServerFactory();
        try {
            var config = this.getConfigAs(SuplaCloudBridgeHandlerConfig.class);
            if (!config.isValid()) {
                updateStatus(OFFLINE, CONFIGURATION_ERROR, "Configuration is not correct!");
                return;
            }
            var serverAccessId = config.getServerAccessId();
            var serverAccessIdPassword = config.getServerAccessIdPassword();
            var email = config.getEmail();
            var authKey = config.getAuthKey();
            var port = config.getPort().intValue();

            var scheduledPool = ThreadPoolManager.getScheduledPool(SuplaBindingConstants.BINDING_ID + "." + port);

            var localServer = server = factory.createNewServer(buildServerProperties(port));
            localServer
                    .getNewChannelsPipe()
                    .subscribe(
                            o -> channelConsumer(
                                    o, serverAccessId, serverAccessIdPassword, email, authKey, scheduledPool),
                            this::errorOccurredInChannel);

            logger = LoggerFactory.getLogger(SuplaCloudBridgeHandler.class.getName() + "." + port);
            logger.debug("jSuplaServer running on port {}", port);
            updateStatus(ONLINE);
        } catch (CertificateException | SSLException ex) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "Cannot start server! " + ex.getLocalizedMessage());
        }
    }

    private void channelConsumer(
            Channel channel,
            @Nullable Integer serverAccessId,
            @Nullable String serverAccessIdPassword,
            @Nullable final String email,
            @Nullable final String authKey,
            final ScheduledExecutorService scheduledPool) {
        logger.debug("Device connected");
        changeNumberOfConnectedDevices(1);
        newChannel(channel, serverAccessId, serverAccessIdPassword, email, authKey, scheduledPool);
    }

    private void newChannel(
            final Channel channel,
            @Nullable Integer serverAccessId,
            @Nullable String serverAccessIdPassword,
            @Nullable final String email,
            @Nullable final String authKey,
            final ScheduledExecutorService scheduledPool) {
        logger.debug("New channel {}", channel);
        var jSuplaChannel = new SuplaChannel(
                this,
                serverAccessId,
                serverAccessIdPassword,
                requireNonNull(serverDiscoveryService),
                channel,
                requireNonNull(scheduledPool),
                suplaDeviceRegistry,
                email,
                authKey);

        channel.getMessagePipe().subscribe(jSuplaChannel::onNext, jSuplaChannel::onError, jSuplaChannel::onComplete);
    }

    private void errorOccurredInChannel(Throwable ex) {
        logger.error("Error occurred in server pipe", ex);
        updateStatus(
                OFFLINE,
                ThingStatusDetail.COMMUNICATION_ERROR,
                "Error occurred in server pipe. Message: " + ex.getLocalizedMessage());
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
        updateState(SuplaBindingConstants.CONNECTED_DEVICES_CHANNEL_ID, new DecimalType(numberOfConnectedDevices));
    }

    private ServerFactory buildServerFactory() {
        return new NettyServerFactory(
                new CallTypeParserImpl(), DecoderFactoryImpl.INSTANCE, EncoderFactoryImpl.INSTANCE);
    }

    private ServerProperties buildServerProperties(int port) throws CertificateException, SSLException {
        return fromList(Arrays.asList(PORT, port, SSL_CTX, buildSslContext()));
    }

    private SslContext buildSslContext() throws CertificateException, SSLException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
    }

    @Override
    public void dispose() {
        super.dispose();
        var local = server;
        if (local != null) {
            try {
                local.close();
            } catch (Exception ex) {
                logger.error("Could not close server! Probably you need to restart Open HAB (or machine)", ex);
            } finally {
                server = null;
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
