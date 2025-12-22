package pl.grzeslowski.openhab.supla.internal;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatus.UNKNOWN;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_DEVICE_TYPE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_TYPE;

import io.github.glytching.junit.extension.random.Random;
import io.github.glytching.junit.extension.random.RandomBeansExtension;
import java.util.Map;
import javax.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingImpl;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentContext;
import pl.grzeslowski.jsupla.protocol.api.structs.SuplaTimeval;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResultA;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaPingServerResult;
import pl.grzeslowski.openhab.supla.internal.device.ZamelRow01;
import pl.grzeslowski.openhab.supla.internal.random.Guid;
import pl.grzeslowski.openhab.supla.internal.random.LocationPassword;
import pl.grzeslowski.openhab.supla.internal.random.Port;
import pl.grzeslowski.openhab.supla.internal.random.RandomExtension;
import pl.grzeslowski.openhab.supla.internal.server.handler.ServerBridgeHandler;

@Slf4j
@ExtendWith({MockitoExtension.class, RandomExtension.class, RandomBeansExtension.class})
public class MainIT {
    final SuplaHandlerFactory factory = new SuplaHandlerFactory();

    private @Nullable ServerBridgeHandler serverHandler;
    private final BridgeImpl serverThing = new BridgeImpl(SUPLA_SERVER_TYPE, "2016");

    @BeforeEach
    void setUp() throws Exception {
        setUpHandlerFactory();
    }

    @SuppressWarnings({"unchecked"})
    private void setUpHandlerFactory() throws Exception {
        var componentContext = mock(ComponentContext.class);
        var bundleContext = mock(BundleContext.class);
        var serviceRegistration = mock(ServiceRegistration.class);
        var serviceReference = mock(ServiceReference.class);
        var bundle = mock(Bundle.class);
        var filter = mock(Filter.class);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.createFilter(anyString())).thenReturn(filter);
        when(bundleContext.getBundle()).thenReturn(bundle);
        when(bundle.getBundleId()).thenReturn(1L);
        when(bundleContext.registerService(anyString(), any(), any())).thenReturn(serviceRegistration);
        when(serviceRegistration.getReference()).thenReturn(serviceReference);
        var method = factory.getClass().getSuperclass().getDeclaredMethod("activate", ComponentContext.class);
        method.setAccessible(true);
        method.invoke(factory, componentContext);
    }

    private void setUpServerHandler(int port) {
        var configuration = new Configuration(
                Map.of("port", port, "ssl", false, "serverAccessId", 123, "serverAccessIdPassword", "none"));
        serverThing.setConfiguration(configuration);
        serverHandler = (ServerBridgeHandler) factory.createHandler(serverThing);
        assertNotNull(serverHandler);
        serverHandler.setCallback(OpenHabDevice.builder().build());
        serverThing.setHandler(serverHandler);
        serverHandler.initialize();
    }

    @AfterEach
    void tearDown() {
        if (serverHandler != null) {
            serverHandler.dispose();
        }
    }

    @Test
    @DisplayName("should ")
    void zamelRow01(
            @Guid String guid,
            @Port int port,
            @Random @Min(1) int serverAccessId,
            @LocationPassword String serverAccessIdPassword)
            throws Exception {
        setUpServerHandler(port);
        log.info(
                "Testing Zamel ROW-01 with GUID={}, "
                        + "using socket on port={}, "
                        + "locationId={}, "
                        + "locationPassword={}",
                guid,
                port,
                serverAccessId,
                serverAccessIdPassword);
        // THING
        var deviceThing = new ThingImpl(SUPLA_SERVER_DEVICE_TYPE, guid);
        var configuration = new Configuration(Map.of(
                "guid", guid,
                "serverAccessId", serverAccessId,
                "serverAccessIdPassword", serverAccessIdPassword));
        deviceThing.setConfiguration(configuration);
        deviceThing.setBridgeUID(requireNonNull(serverHandler).getThing().getUID());
        // HANDLER
        var deviceHandler = factory.createHandler(deviceThing);
        assertNotNull(deviceHandler);
        var openHabDevice =
                OpenHabDevice.builder().thing(deviceThing).bridge(serverThing).build();
        deviceHandler.setCallback(openHabDevice);
        log.info("Initializing server device handler");
        deviceHandler.initialize();
        requireNonNull(serverHandler).childHandlerInitialized(deviceHandler, deviceThing);
        assertThat(openHabDevice.findThingStatus())
                .isEqualTo(new ThingStatusInfo(
                        UNKNOWN, HANDLER_CONFIGURATION_PENDING, "@text/supla.server.waiting-for-connection"));
        // DEVICE
        try (var device = new ZamelRow01(guid, serverAccessId, serverAccessIdPassword)) {
            device.initialize("localhost", port);
            // register
            device.register();
            var registerResult = device.readRegisterDeviceResultA();
            assertThat(registerResult).isEqualTo(new SuplaRegisterDeviceResultA(3, (short) 100, (short) 5, (short) 1));
            assertThat(openHabDevice.findThingStatus())
                    .isEqualTo(new ThingStatusInfo(
                            UNKNOWN, CONFIGURATION_PENDING, "@text/supla.offline.waiting-for-registration"));
            // ping
            device.sendPing();
            assertThat(device.readPing()).isEqualTo(new SuplaPingServerResult(new SuplaTimeval(0, 0)));
            await().untilAsserted(() -> assertThat(openHabDevice.findThingStatus())
                    .isEqualTo(ThingStatusInfoBuilder.create(ONLINE, NONE).build()));

            var channel = openHabDevice.findChannel();
            { // device updates it's state with OH
                var previousState = device.isState();
                device.toggleSwitch();
                var currentState = device.isState();
                assertThat(currentState).isNotEqualTo(previousState);
                log.info("Waiting for OH to propagate state change");
                await().untilAsserted(() -> {
                    var channelState = openHabDevice.findChannelState(channel);
                    assertThat(channelState).isEqualTo(OnOffType.from(currentState));
                });
            }
            {
                // OH updates it's state with the device
                var previousState = device.isState();
                deviceHandler.handleCommand(channel, OnOffType.from(!previousState));
                device.updateChannel();
                assertThat(device.isState()).isNotEqualTo(previousState);
            }
        }
        // device is closed
        log.info("Waiting for the device to disconnect");
        await().untilAsserted(() -> assertThat(openHabDevice.findThingStatus())
                .isEqualTo(
                        new ThingStatusInfo(OFFLINE, COMMUNICATION_ERROR, "@text/supla.offline.channel-disconnected")));
        deviceHandler.dispose();
    }
}
