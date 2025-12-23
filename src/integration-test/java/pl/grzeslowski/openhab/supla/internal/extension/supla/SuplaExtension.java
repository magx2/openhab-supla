package pl.grzeslowski.openhab.supla.internal.extension.supla;

import static java.util.Collections.synchronizedList;
import static java.util.Objects.requireNonNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPLA_SERVER_TYPE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.SUPPORTED_THING_TYPES_UIDS;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.*;
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

@Slf4j
public class SuplaExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private final SuplaHandlerFactory factory = new SuplaHandlerFactory();
    private final List<ThingHandler> handlers = synchronizedList(new ArrayList<>());

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        setUpHandlerFactory();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        var local = new ArrayList<>(handlers);
        log.info("Disposing {} handlers", local.size());
        handlers.clear();
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
            return generateHandler(parameterContext);
        }
        return null;
    }

    private Ctx generateHandler(ParameterContext ctx) {
        var createHandler = ctx.findAnnotation(CreateHandler.class).orElseThrow();
        var bridge = ctx.getParameter().getType() == Ctx.BridgeCtx.class;
        var thingTypeId = createHandler.thingTypeId();
        var thingTypeUID = SUPPORTED_THING_TYPES_UIDS.stream()
                .filter(uid -> uid.getId().equals(thingTypeId))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Can not find thingTypeId=" + thingTypeId));
        var thing = buildThing(bridge, thingTypeUID);
        var handler = requireNonNull(factory.createHandler(thing));
        handlers.add(handler);
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
}
