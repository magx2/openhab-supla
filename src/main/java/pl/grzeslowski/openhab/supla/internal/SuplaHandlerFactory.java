package pl.grzeslowski.openhab.supla.internal;

import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toSet;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.*;

import java.util.*;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.openhab.supla.internal.cloud.discovery.CloudDiscovery;
import pl.grzeslowski.openhab.supla.internal.cloud.handler.CloudBridgeHandler;
import pl.grzeslowski.openhab.supla.internal.cloud.handler.CloudDevice;
import pl.grzeslowski.openhab.supla.internal.server.discovery.ServerDiscoveryService;
import pl.grzeslowski.openhab.supla.internal.server.handler.*;

/** The {@link SuplaHandlerFactory} is responsible for creating things and thing handlers. */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPid = "binding.supla")
@NonNullByDefault
public class SuplaHandlerFactory extends BaseThingHandlerFactory implements ServerDeviceActionServiceRegistry {
    private final Logger logger = LoggerFactory.getLogger(SuplaHandlerFactory.class);
    private final Map<BridgeHandler, ServiceReference<?>> servicesToDispose = synchronizedMap(new HashMap<>());
    private final Map<ThingUID, ActionServiceRegistrations> actionServicesToDispose = synchronizedMap(new HashMap<>());

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    public @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPLA_SERVER_DEVICE_TYPE.equals(thingTypeUID)) {
            return newServerDeviceHandler(thing);
        }

        if (SUPLA_CLOUD_DEVICE_TYPE.equals(thingTypeUID)) {
            return newCloudDeviceHandler(thing);
        }

        if (SUPLA_SERVER_TYPE.equals(thingTypeUID)) {
            return newServerBridgeHandler((Bridge) thing);
        }

        if (SUPLA_CLOUD_SERVER_TYPE.equals(thingTypeUID)) {
            return newCloudBridgeHandler(thing);
        }

        if (SUPLA_GATEWAY_DEVICE_TYPE.equals(thingTypeUID)) {
            return newGatewayDeviceHandler(thing);
        }

        if (SUPLA_SUB_DEVICE_TYPE.equals(thingTypeUID)) {
            return newSubDeviceHandler(thing);
        }

        return null;
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof ServerSuplaDeviceHandler serverHandler) {
            unregisterActionServices(serverHandler);
        }
        if (!(thingHandler instanceof BridgeHandler bridgeHandler)) {
            return;
        }
        var reference = servicesToDispose.get(bridgeHandler);
        if (reference != null) {
            unRegisterThingDiscovery(reference);
            servicesToDispose.remove(bridgeHandler);
        }
    }

    @NonNull
    private ThingHandler newServerDeviceHandler(final Thing thing) {
        return new SingleDeviceHandler(thing, this);
    }

    @NonNull
    private ThingHandler newServerBridgeHandler(final Bridge thing) {
        var discovery = new ServerDiscoveryService(thing.getUID());
        var bridgeHandler = new ServerBridgeHandler(thing, discovery);
        var serviceRegistration = registerThingDiscovery(discovery);
        servicesToDispose.put(bridgeHandler, serviceRegistration.getReference());
        return bridgeHandler;
    }

    @NonNull
    private ThingHandler newCloudBridgeHandler(final Thing thing) {
        var bridgeHandler = new CloudBridgeHandler((Bridge) thing);
        var discovery = new CloudDiscovery(bridgeHandler);
        var serviceRegistration = registerThingDiscovery(discovery);
        servicesToDispose.put(bridgeHandler, serviceRegistration.getReference());
        return bridgeHandler;
    }

    @NonNull
    private ThingHandler newCloudDeviceHandler(final Thing thing) {
        return new CloudDevice(thing);
    }

    private synchronized ServiceRegistration<?> registerThingDiscovery(DiscoveryService discoveryService) {
        logger.debug(
                "Try to register Discovery service on BundleID: {} Service: {}",
                bundleContext.getBundle().getBundleId(),
                DiscoveryService.class.getName());
        return bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<>());
    }

    private synchronized void unRegisterThingDiscovery(ServiceReference<?> serviceReference) {
        logger.debug(
                "Try to unregister Discovery service on BundleID: {} Service: {}",
                bundleContext.getBundle().getBundleId(),
                DiscoveryService.class.getName());
        bundleContext.ungetService(serviceReference);
    }

    private ThingHandler newGatewayDeviceHandler(Thing thing) {
        var discovery = new ServerDiscoveryService(thing.getUID());
        var bridgeHandler = new GatewayDeviceHandler(thing, discovery, this);
        var serviceRegistration = registerThingDiscovery(discovery);
        servicesToDispose.put(bridgeHandler, serviceRegistration.getReference());
        return bridgeHandler;
    }

    private ThingHandler newSubDeviceHandler(Thing thing) {
        return new SubDeviceHandler(thing);
    }

    @Override
    public void updateActionServices(
            ServerSuplaDeviceHandler handler, Set<Class<? extends ThingActions>> actionServices) {
        var thingUID = handler.getThing().getUID();
        var existing = actionServicesToDispose.get(thingUID);
        if (existing != null && existing.actionServices().equals(actionServices)) {
            return;
        }

        unregisterActionServices(handler);
        var registrations = actionServices.stream()
                .map(actionService -> registerActionService(handler, actionService))
                .toList();
        actionServicesToDispose.put(thingUID, new ActionServiceRegistrations(registrations));
    }

    @Override
    public void unregisterActionServices(ServerSuplaDeviceHandler handler) {
        var registrations = actionServicesToDispose.remove(handler.getThing().getUID());
        if (registrations == null) {
            return;
        }
        registrations.registrations().forEach(ActionServiceRegistration::unregister);
    }

    private ActionServiceRegistration registerActionService(
            ServerSuplaDeviceHandler handler, Class<? extends ThingActions> actionService) {
        try {
            var service = actionService.getConstructor().newInstance();
            service.setThingHandler(handler);
            var registration = bundleContext.registerService(ThingActions.class, service, new Hashtable<>());
            service.initialize();
            return new ActionServiceRegistration(actionService, service, registration);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not register action service " + actionService.getName(), ex);
        }
    }

    private record ActionServiceRegistrations(List<ActionServiceRegistration> registrations) {
        private Set<Class<? extends ThingActions>> actionServices() {
            return registrations.stream()
                    .map(ActionServiceRegistration::actionService)
                    .collect(toSet());
        }
    }

    private record ActionServiceRegistration(
            Class<? extends ThingActions> actionService,
            ThingActions service,
            ServiceRegistration<ThingActions> registration) {
        private void unregister() {
            service.dispose();
            registration.unregister();
        }
    }
}
