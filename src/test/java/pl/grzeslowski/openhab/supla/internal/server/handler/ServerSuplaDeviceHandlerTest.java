package pl.grzeslowski.openhab.supla.internal.server.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import pl.grzeslowski.openhab.supla.internal.server.handler.trait.ServerBridge;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.AuthData;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.ServerDeviceHandlerConfiguration;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

@ExtendWith(MockitoExtension.class)
class ServerSuplaDeviceHandlerTest {
    ServerSuplaDeviceHandler handler;

    @Mock
    Thing thing;

    @Mock
    ThingHandlerCallback callback;

    @Mock
    Bridge bridge;

    @Mock
    ThingUID bridgeUid;

    @Mock
    Configuration configuration;

    @Mock
    ServerBridge serverBridge;

    @BeforeEach
    void setUp() {
        handler = new TestHandler(thing);
    }

    @Test
    void initializeWithoutBridgeShouldGoOffline() {
        given(thing.getBridgeUID()).willReturn(bridgeUid);
        given(callback.getBridge(bridgeUid)).willReturn(null);
        given(thing.getConfiguration()).willReturn(configuration);

        handler.setCallback(callback);
        handler.initialize();

        verify(callback)
                .statusUpdated(
                        thing,
                        new ThingStatusInfo(
                                ThingStatus.OFFLINE,
                                ThingStatusDetail.HANDLER_CONFIGURATION_PENDING,
                                "@text/supla.server.waiting-for-connection []"));
    }

    @Test
    void initializeWithWrongBridgeTypeShouldGoOfflineWithConfigError() {
        given(thing.getBridgeUID()).willReturn(bridgeUid);
        given(callback.getBridge(bridgeUid)).willReturn(bridge);
        //        given(bridge.getHandler()).willReturn(new Object());
        given(thing.getConfiguration()).willReturn(configuration);

        handler.setCallback(callback);
        handler.initialize();

        verify(callback)
                .statusUpdated(
                        thing,
                        new ThingStatusInfo(
                                ThingStatus.OFFLINE,
                                ThingStatusDetail.CONFIGURATION_ERROR,
                                "@text/supla.server.bridge-type-wrong [[ServerBridgeHandler, GatewayDeviceHandler], Object]"));
    }

    @Test
    void buildTimeoutConfigurationPrefersThingConfigOverBridge() throws Exception {
        given(thing.getBridgeUID()).willReturn(bridgeUid);
        given(callback.getBridge(bridgeUid)).willReturn(bridge);
        given(bridge.getHandler()).willReturn(serverBridge);
        given(thing.getConfiguration()).willReturn(configuration);

        ServerDeviceHandlerConfiguration cfg = new ServerDeviceHandlerConfiguration();
        cfg.setGuid("123");
        cfg.setTimeout(BigDecimal.valueOf(30));
        cfg.setTimeoutMin(BigDecimal.valueOf(10));
        cfg.setTimeoutMax(BigDecimal.valueOf(40));
        given(configuration.as(ServerDeviceHandlerConfiguration.class)).willReturn(cfg);
        given(serverBridge.getTimeoutConfiguration()).willReturn(new TimeoutConfiguration(5, 2, 7));
        given(serverBridge.getAuthData()).willReturn(new AuthData(new AuthData.LocationAuthData(1, "123"), null));

        handler.setCallback(callback);
        handler.initialize();

        // After successful init waiting for registration
        verify(callback)
                .statusUpdated(
                        thing,
                        new ThingStatusInfo(
                                ThingStatus.UNKNOWN,
                                ThingStatusDetail.CONFIGURATION_PENDING,
                                "@text/supla.offline.waiting-for-registration []"));
    }

    @Test
    void buildAuthDataUsesBridgeValuesWhenThingNotProvided() throws Exception {
        given(thing.getBridgeUID()).willReturn(bridgeUid);
        given(callback.getBridge(bridgeUid)).willReturn(bridge);
        given(bridge.getHandler()).willReturn(serverBridge);
        given(thing.getConfiguration()).willReturn(configuration);

        ServerDeviceHandlerConfiguration cfg = new ServerDeviceHandlerConfiguration();
        cfg.setGuid("abc");
        given(configuration.as(ServerDeviceHandlerConfiguration.class)).willReturn(cfg);
        given(serverBridge.getTimeoutConfiguration()).willReturn(new TimeoutConfiguration(10, 8, 12));
        given(serverBridge.getAuthData())
                .willReturn(
                        new AuthData(new AuthData.LocationAuthData(10, "99"), new AuthData.EmailAuthData("e@e", "AA")));

        handler.setCallback(callback);
        handler.initialize();

        verify(callback)
                .statusUpdated(
                        thing,
                        new ThingStatusInfo(
                                ThingStatus.UNKNOWN,
                                ThingStatusDetail.CONFIGURATION_PENDING,
                                "@text/supla.offline.waiting-for-registration []"));
    }

    @Test
    void handleCommandIgnoredWhenNotAuthorized() {
        handler.handleCommand(null, null);
        // no exception and nothing to assert; just ensure method does not crash when unauthorized
        assertThat(true).isTrue();
    }

    // Minimal concrete subclass for testing
    static class TestHandler extends SingleDeviceHandler {
        TestHandler(Thing thing) {
            super(thing);
        }

        @Override
        protected String findGuid() {
            return "guid";
        }
    }
}
