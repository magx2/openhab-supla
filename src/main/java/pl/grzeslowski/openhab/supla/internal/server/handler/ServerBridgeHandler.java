package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;
import static org.openhab.core.thing.ThingStatusDetail.HANDLER_INITIALIZING_ERROR;
import static org.openhab.core.types.RefreshType.REFRESH;
import static pl.grzeslowski.jsupla.server.NettyConfig.DEFAULT_TIMEOUT;
import static pl.grzeslowski.openhab.supla.internal.Documentation.SSL_PROBLEM;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.CONNECTED_DEVICES_CHANNEL_ID;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;
import lombok.Getter;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.server.MessageHandler;
import pl.grzeslowski.jsupla.server.NettyConfig;
import pl.grzeslowski.jsupla.server.NettyServer;
import pl.grzeslowski.openhab.supla.internal.Documentation;
import pl.grzeslowski.openhab.supla.internal.handler.InitializationException;
import pl.grzeslowski.openhab.supla.internal.handler.OfflineInitializationException;
import pl.grzeslowski.openhab.supla.internal.handler.SuplaBridge;
import pl.grzeslowski.openhab.supla.internal.server.discovery.ServerDiscoveryService;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerBridge;
import pl.grzeslowski.openhab.supla.internal.server.netty.OpenHabMessageHandler;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.AuthData;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerBridgeHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

@NonNullByDefault
public class ServerBridgeHandler extends SuplaBridge implements ServerBridge {
    private static final int PROPER_AES_KEY_SIZE = 2147483647;

    @Getter
    private Logger logger = LoggerFactory.getLogger(ServerBridgeHandler.class);

    @Nullable
    private NettyServer server;

    private final ServerDiscoveryService serverDiscoveryService;

    private final AtomicInteger numberOfConnectedDevices = new AtomicInteger();

    private final Collection<ServerSuplaDeviceHandler> childHandlers = Collections.synchronizedList(new ArrayList<>());

    @Getter
    @Nullable
    private TimeoutConfiguration timeoutConfiguration;

    @Getter
    @Nullable
    private AuthData authData;

    private int port;

    public ServerBridgeHandler(Bridge bridge, ServerDiscoveryService serverDiscoveryService) {
        super(bridge);
        this.serverDiscoveryService = serverDiscoveryService;
    }

    @Override
    protected void internalInitialize() throws InitializationException {
        var config = this.getConfigAs(ServerBridgeHandlerConfiguration.class);
        if (!config.isServerAuth() && !config.isEmailAuth()) {
            throw new OfflineInitializationException(CONFIGURATION_ERROR, text("supla.server.auth-missing"));
        }
        if (config.getPort().intValue() <= 0) {
            throw new OfflineInitializationException(CONFIGURATION_ERROR, text("supla.server.port-missing"));
        }
        authData = ServerBridge.buildAuthData(config);
        port = config.getPort().intValue();
        var protocols =
                stream(config.getProtocols().split(",")).map(String::trim).collect(toSet());
        if (protocols.isEmpty()) {
            throw new OfflineInitializationException(CONFIGURATION_ERROR, text("supla.server.protocol-missing"));
        }

        logger = LoggerFactory.getLogger(ServerBridgeHandler.class.getName() + "." + port);

        var scheduledPool = ThreadPoolManager.getScheduledPool(BINDING_ID + "." + port);

        if (config.isSsl()) {
            var algo = "AES";
            try {
                var maxKeySize = javax.crypto.Cipher.getMaxAllowedKeyLength(algo);
                if (maxKeySize < PROPER_AES_KEY_SIZE) {
                    logger.warn(
                            "AES key size is too small, {} < {}! Probably you need to enable unlimited crypto. See: {}",
                            maxKeySize,
                            PROPER_AES_KEY_SIZE,
                            SSL_PROBLEM);
                }
            } catch (NoSuchAlgorithmException ex) {
                throw new OfflineInitializationException(
                        HANDLER_INITIALIZING_ERROR,
                        text("supla.server.missing-algorithm", algo, ex.getLocalizedMessage(), SSL_PROBLEM));
            }
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
                    throw new OfflineInitializationException(
                            CONFIGURATION_ERROR,
                            text(
                                    "supla.server.disabled-protocols",
                                    algorithms,
                                    Documentation.DISABLED_ALGORITHMS_PROBLEM));
                }
            } else {
                logger.debug("jdk.tls.disabledAlgorithms is null, should not be a problem, carry on...");
            }
        } catch (Exception ex) {
            logger.warn("Cannot get disabled algorithms! {}", ex.getLocalizedMessage());
        }

        timeoutConfiguration = ServerBridge.buildTimeoutConfiguration(config);

        try {
            server = new NettyServer(buildNettyConfig(port, protocols, config.isSsl()), this::messageHandlerFactory);
        } catch (CertificateException | SSLException ex) {
            throw new OfflineInitializationException(
                    HANDLER_INITIALIZING_ERROR,
                    text("supla.server.certificate-problem", ex.getLocalizedMessage(), SSL_PROBLEM));
        }

        logger.debug("jSuplaServer running on port {}", port);
        updateStatus(ONLINE);
        numberOfConnectedDevices.set(0);
        updateConnectedDevices(0);
    }

    @Override
    protected String findGuid() {
        if (port > 0) {
            return String.valueOf(port);
        }
        return String.valueOf(this.getConfigAs(ServerBridgeHandlerConfiguration.class)
                .getPort()
                .intValue());
    }

    private MessageHandler messageHandlerFactory(SocketChannel ch) {
        logger.debug("Device connected");
        return new OpenHabMessageHandler(this, serverDiscoveryService, ch);
    }

    public Optional<ServerSuplaDeviceHandler> findSuplaThing(String guid) {
        return childHandlers.stream()
                .filter(handler -> Objects.equals(handler.getGuid(), guid))
                .findAny();
    }

    @Override
    public void deviceConnected() {
        logger.debug("Device connected to Server");
        changeNumberOfConnectedDevices(1);
    }

    @Override
    public void deviceDisconnected() {
        logger.debug("Device disconnected from Server");
        changeNumberOfConnectedDevices(-1);
    }

    private void changeNumberOfConnectedDevices(int delta) {
        var number = numberOfConnectedDevices.addAndGet(delta);
        logger.debug("Number of connected devices: {} (delta: {})", number, delta);
        updateConnectedDevices(number);
    }

    private void updateConnectedDevices(int numberOfConnectedDevices) {
        updateState(CONNECTED_DEVICES_CHANNEL_ID, new DecimalType(numberOfConnectedDevices));
    }

    private NettyConfig buildNettyConfig(int port, Set<String> protocols, boolean sslEnabled)
            throws CertificateException, SSLException {
        var sslCtx = sslEnabled ? buildSslContext(protocols) : null;
        return new NettyConfig(port, DEFAULT_TIMEOUT, sslCtx);
    }

    private SslContext buildSslContext(Set<String> protocols) throws CertificateException, SSLException {
        var ssc = new SelfSignedCertificate();
        return SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .protocols(protocols)
                .sslProvider(SslProvider.OPENSSL)
                .build();
    }

    @Override
    public void dispose() {
        logger.debug("Disposing ServerBridgeHandler");
        disposeServer();
        logger = LoggerFactory.getLogger(ServerBridgeHandler.class);
        super.dispose();
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
        if (command != REFRESH) {
            return;
        }
        updateConnectedDevices(numberOfConnectedDevices.get());
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        if (!(childHandler instanceof ServerSuplaDeviceHandler serverDevice)) {
            logger.warn(
                    "Child handler ({}) is not instance of ServerAbstractDeviceHandler.",
                    childHandler.getClass().getSimpleName());
            return;
        }
        logger.debug("Add Handler {}", serverDevice.getGuid());
        childHandlers.add(serverDevice);
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        if (!(childHandler instanceof ServerSuplaDeviceHandler serverDevice)) {
            logger.warn(
                    "Child handler ({}) is not instance of ServerAbstractDeviceHandler.",
                    childHandler.getClass().getSimpleName());
            return;
        }
        logger.debug("Remove Handler {}", serverDevice.getGuid());
        var remove = childHandlers.remove(serverDevice);
        if (!remove) {
            logger.warn("There was no child handler with id {} found", serverDevice.getGuid());
        }
    }
}
