package pl.grzeslowski.openhab.supla.internal;

import static java.util.Collections.synchronizedMap;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.openhab.supla.internal.cloud.discovery.CloudDiscovery;
import pl.grzeslowski.openhab.supla.internal.cloud.handler.CloudBridgeHandler;
import pl.grzeslowski.openhab.supla.internal.cloud.handler.CloudDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.server.discovery.ServerDiscoveryService;
import pl.grzeslowski.openhab.supla.internal.server.handler.*;

/** The {@link SuplaHandlerFactory} is responsible for creating things and thing handlers. */
@Component(service = ThingHandlerFactory.class, immediate = true, configurationPid = "binding.supla")
@NonNullByDefault
public class SuplaHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(SuplaHandlerFactory.class);
    private final Map<BridgeHandler, ServiceReference<?>> servicesToDispose = synchronizedMap(new HashMap<>());

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
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
        return new SingleDeviceHandler(thing);
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
        return new CloudDeviceHandler(thing);
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
        var bridgeHandler = new GatewayDeviceHandler(thing, discovery);
        var serviceRegistration = registerThingDiscovery(discovery);
        servicesToDispose.put(bridgeHandler, serviceRegistration.getReference());
        return bridgeHandler;
    }

    private ThingHandler newSubDeviceHandler(Thing thing) {
        return new SubDeviceHandler(thing);
    }
}
