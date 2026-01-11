package pl.grzeslowski.openhab.supla.internal.extension.supla;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.openhab.core.thing.ThingStatus.UNKNOWN;
import static org.openhab.core.thing.ThingStatusDetail.HANDLER_CONFIGURATION_PENDING;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_TYPE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPPORTED_THING_TYPES_UIDS;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.extension.*;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.internal.BridgeImpl;
import org.openhab.core.thing.internal.ThingImpl;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentContext;
import pl.grzeslowski.openhab.supla.internal.OpenHabDevice;
import pl.grzeslowski.openhab.supla.internal.SuplaHandlerFactory;
import pl.grzeslowski.openhab.supla.internal.extension.random.RandomExtension;
import pl.grzeslowski.openhab.supla.internal.server.oh_config.TimeoutConfiguration;

@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
@Slf4j
public class SuplaExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private final SuplaHandlerFactory factory = new SuplaHandlerFactory();
    private final MultiValuedMap<String, ThingHandler> handlers = new ArrayListValuedHashMap<>();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        setUpHandlerFactory();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var uniqueId = context.getUniqueId();
        var local = handlers.get(uniqueId);
        log.info("Disposing {} handlers for unique ID={}", local.size(), uniqueId);
        handlers.remove(uniqueId);
        local.forEach(this::disposeHandler);
    }

    private void disposeHandler(ThingHandler handler) {
        var uid = handler.getThing().getUID();
        try {
            log.info("Disposing handler {}", uid);
            handler.dispose();
        } catch (Exception e) {
            log.warn("Got exception while disposing {}", uid, e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void setUpHandlerFactory() throws Exception {
        var componentContext = mock(ComponentContext.class);
        var bundleContext = mock(BundleContext.class);
        var serviceRegistration = mock(ServiceRegistration.class);
        var serviceReference = mock(ServiceReference.class);
        var bundle = mock(Bundle.class);
        var filter = mock(Filter.class);
        lenient().when(componentContext.getBundleContext()).thenReturn(bundleContext);
        lenient().when(bundleContext.createFilter(anyString())).thenReturn(filter);
        lenient().when(bundleContext.getBundle()).thenReturn(bundle);
        lenient().when(bundle.getBundleId()).thenReturn(1L);
        lenient().when(bundleContext.registerService(anyString(), any(), any())).thenReturn(serviceRegistration);
        lenient().when(serviceRegistration.getReference()).thenReturn(serviceReference);
        var method = factory.getClass().getSuperclass().getDeclaredMethod("activate", ComponentContext.class);
        method.setAccessible(true);
        method.invoke(factory, componentContext);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.isAnnotated(CreateHandler.class)) {
            return parameterContext.getParameter().getType() == Ctx.ThingCtx.class
                    || parameterContext.getParameter().getType() == Ctx.BridgeCtx.class;
        }

        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.isAnnotated(CreateHandler.class)) {
            return generateHandler(parameterContext, extensionContext.getUniqueId());
        }
        return null;
    }

    private Ctx generateHandler(ParameterContext ctx, String uniqueId) {
        var createHandler = ctx.findAnnotation(CreateHandler.class).orElseThrow();
        var bridge = ctx.getParameter().getType() == Ctx.BridgeCtx.class;
        var thingTypeId = createHandler.thingTypeId();
        var thingTypeUID = SUPPORTED_THING_TYPES_UIDS.stream()
                .filter(uid -> uid.getId().equals(thingTypeId))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Can not find thingTypeId=" + thingTypeId));
        var thing = buildThing(bridge, thingTypeUID);
        var handler = requireNonNull(factory.createHandler(thing));
        handlers.put(uniqueId, handler);
        thing.setHandler(handler);
        var openHabDevice = OpenHabDevice.builder().build();
        handler.setCallback(openHabDevice);
        if (bridge) {
            return new Ctx.BridgeCtx((BridgeImpl) thing, (BridgeHandler) handler, openHabDevice);
        }
        return new Ctx.ThingCtx(thing, handler, openHabDevice);
    }

    private ThingImpl buildThing(boolean bridge, ThingTypeUID thingTypeUID) {
        if (bridge) {
            return new BridgeImpl(SUPLA_SERVER_TYPE, RandomExtension.INSTANCE.randomString());
        }
        return new ThingImpl(thingTypeUID, RandomExtension.INSTANCE.randomGuid());
    }

    public static void serverInitialize(Ctx.BridgeCtx serverCtx, int port) {
        // configure server handler
        var configuration = new Configuration(
                Map.of("port", port, "ssl", false, "serverAccessId", 123, "serverAccessIdPassword", "none"));
        serverCtx.thing().setConfiguration(configuration);
        serverCtx.handler().initialize();
    }

    public static void deviceInitialize(
            Ctx.ThingCtx deviceCtx,
            Ctx.BridgeCtx serverCtx,
            int serverAccessId,
            String serverAccessIdPassword,
            String guid) {
        // configure device handler
        var configuration = Map.<String, Object>of(
                "guid", guid,
                "serverAccessId", serverAccessId,
                "serverAccessIdPassword", serverAccessIdPassword);
        deviceInitialize(deviceCtx, serverCtx, configuration, guid);
    }

    public static void deviceInitialize(
            Ctx.ThingCtx deviceCtx, Ctx.BridgeCtx serverCtx, String email, String authKey, String guid) {
        deviceInitialize(deviceCtx, serverCtx, email, authKey, guid, null);
    }

    public static void deviceInitialize(
            Ctx.ThingCtx deviceCtx,
            Ctx.BridgeCtx serverCtx,
            String email,
            String authKey,
            String guid,
            @Nullable TimeoutConfiguration timeout) {
        // configure device handler
        var configuration = Map.<String, Object>of(
                "guid", guid,
                "email", email,
                "authKey", authKey);
        if (timeout != null) {
            log.info("Using timeout={}", timeout);
            configuration = new HashMap<>(configuration);
            configuration.put("timeout", Long.toString(timeout.timeout().toSeconds()));
            configuration.put("timeoutMin", Long.toString(timeout.min().toSeconds()));
            configuration.put("timeoutMax", Long.toString(timeout.max().toSeconds()));
            configuration = Map.copyOf(configuration);
        }
        deviceInitialize(deviceCtx, serverCtx, configuration, guid);
    }

    public static void deviceInitialize(
            Ctx.ThingCtx deviceCtx, Ctx.BridgeCtx serverCtx, Map<String, Object> configuration, String guid) {
        deviceCtx.thing().setConfiguration(new Configuration(configuration));
        deviceCtx.thing().setBridgeUID(serverCtx.thing().getUID());
        deviceCtx.openHabDevice().setGuid(guid);
        deviceCtx.openHabDevice().setBridge(serverCtx.thing());
        deviceCtx.openHabDevice().setThing(deviceCtx.thing());
        log.info("Initializing server device handler");
        deviceCtx.handler().initialize();
        serverCtx.handler().childHandlerInitialized(deviceCtx.handler(), deviceCtx.thing());
        assertThat(deviceCtx.openHabDevice().findThingStatus())
                .isEqualTo(new ThingStatusInfo(
                        UNKNOWN, HANDLER_CONFIGURATION_PENDING, "@text/supla.server.waiting-for-connection"));
    }
}
