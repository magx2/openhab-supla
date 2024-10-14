package pl.grzeslowski.openhab.supla.internal.server.handler;

import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static org.openhab.core.types.RefreshType.REFRESH;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;

@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public class ServerGatewayHandler extends ServerDeviceHandler implements SuplaBridge {
    private final AtomicInteger numberOfConnectedDevices = new AtomicInteger();

    private final Collection<ServerDeviceHandler> childHandlers = Collections.synchronizedList(new ArrayList<>());

    @Getter
    @Nullable
    private TimeoutConfiguration timeoutConfiguration;

    @Getter
    @Nullable
    private AuthData authData;

    public ServerGatewayHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected void internalInitialize() {
        super.internalInitialize();

        var config = this.getConfigAs(ServerBridgeHandlerConfig.class);
        authData = SuplaBridge.buildAuthData(config);
        timeoutConfiguration = SuplaBridge.buildTimeoutConfiguration(config);
    }

    @Override
    protected List<Class<? extends SuplaBridge>> findAllowedBridgeClasses() {
        return List.of(ServerBridgeHandler.class);
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

    @Override
    public void handleCommand(final ChannelUID channelUID, final Command command) {
        if (command != REFRESH) {
            return;
        }
        updateConnectedDevices(numberOfConnectedDevices.get());
    }

    @Override
    protected boolean afterRegister(RegisterDeviceTrait registerEntity) {
        var flags = registerEntity.getFlags();
        if (!flags.isCalcfgSubdevicePairing()) {
            updateStatus(OFFLINE, CONFIGURATION_ERROR, "Device can only register sub devices!");
            return false;
        }

        // todo

        return true;
    }

    @Override
    public void handle(ToServerProto entity) {
        logger.info("Handling proto: {}", entity);
        super.handle(entity);
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        if (!(childHandler instanceof ServerDeviceHandler serverDevice)) {
            return;
        }
        logger.debug("Add Handler {}", serverDevice.getGuid());
        childHandlers.add(serverDevice);
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        if (!(childHandler instanceof ServerDeviceHandler serverDevice)) {
            return;
        }
        logger.debug("Remove Handler {}", serverDevice.getGuid());
        childHandlers.remove(serverDevice);
    }
}
