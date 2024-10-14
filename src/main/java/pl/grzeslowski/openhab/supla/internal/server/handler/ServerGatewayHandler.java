package pl.grzeslowski.openhab.supla.internal.server.handler;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.javatuples.Pair;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocol.api.channeltype.decoders.ChannelTypeDecoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.LocalTimeRequest;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaPingServer;
import pl.grzeslowski.jsupla.protocol.api.structs.dcs.SuplaSetActivityTimeout;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelExtendedValue;
import pl.grzeslowski.jsupla.protocol.api.structs.ds.SuplaDeviceChannelValue;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaChannelNewValue;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaRegisterDeviceResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaPingServerResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.SuplaSetActivityTimeoutResult;
import pl.grzeslowski.jsupla.protocol.api.structs.sdc.UserLocalTimeResult;
import pl.grzeslowski.jsupla.protocol.api.types.ToServerProto;
import pl.grzeslowski.jsupla.server.api.Writer;
import pl.grzeslowski.openhab.supla.internal.handler.AbstractDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.server.ChannelCallback;
import pl.grzeslowski.openhab.supla.internal.server.ChannelValueToState;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannelTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterEmailDeviceTrait;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterLocationDeviceTrait;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Short.parseShort;
import static java.lang.String.valueOf;
import static java.time.Instant.now;
import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static org.openhab.core.types.RefreshType.REFRESH;
import static pl.grzeslowski.jsupla.protocol.api.ProtocolHelpers.parseString;
import static pl.grzeslowski.jsupla.protocol.api.ResultCode.SUPLA_RESULTCODE_TRUE;
import static pl.grzeslowski.openhab.supla.internal.Documentation.THING_BRIDGE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ServerDevicesProperties.*;

@NonNullByDefault
@ToString(onlyExplicitlyIncluded = true)
public class ServerGatewayHandler extends ServerDeviceHandler implements SuplaBridge{
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
        return List.of( ServerBridgeHandler.class);
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
            updateStatus(OFFLINE, CONFIGURATION_ERROR,"Device can only register sub devices!");
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
