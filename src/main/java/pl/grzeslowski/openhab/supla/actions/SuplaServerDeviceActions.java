package pl.grzeslowski.openhab.supla.actions;

import static java.lang.System.arraycopy;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_CMD_RESET_COUNTERS;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_CALCFG_RESULT_DONE;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import pl.grzeslowski.jsupla.protocol.api.DeviceConfigField;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.DeviceCalCfgRequest;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SetDeviceConfig;
import pl.grzeslowski.openhab.supla.internal.server.ChannelUtil;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfig;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigResult;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigUtil;
import pl.grzeslowski.openhab.supla.internal.server.handler.ServerSuplaDeviceHandler;

@ThingActionsScope(name = BINDING_ID)
@NonNullByDefault
@Slf4j
public class SuplaServerDeviceActions implements ThingActions {
    private static final byte[] EMPTY_DATA = new byte[0];
    private static final byte SUPER_USER_AUTHORIZED = 1;

    @Getter
    @Nullable
    private ServerSuplaDeviceHandler thingHandler;

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (!(handler instanceof ServerSuplaDeviceHandler suplaHandler)) {
            var handlerClass = Optional.of(handler)
                    .map(ThingHandler::getClass)
                    .map(Class::getSimpleName)
                    .orElse("<null>");
            log.warn(
                    "Handler {} has wrong class, actualClass={}, expectedClass={}",
                    handlerClass,
                    ServerSuplaDeviceHandler.class.getSimpleName());
            return;
        }
        this.thingHandler = suplaHandler;
    }

    @RuleAction(label = "Device Config Config", description = "Set device config")
    public void setDeviceConfig(String... deviceConfigs) throws InterruptedException, TimeoutException {
        setDeviceConfig(Arrays.asList(deviceConfigs));
    }

    @RuleAction(label = "Device Config Config", description = "Set device config")
    public void setDeviceConfig(Collection<String> configs) throws InterruptedException, TimeoutException {
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("You need to pass configs!");
        }
        var localHandler = thingHandler;
        if (localHandler == null) {
            log.warn("Thing handler is null!");
            return;
        }

        var deviceConfig = configs.stream()
                .map(DeviceConfigUtil::parseDeviceConfig)
                .collect(Collectors.toCollection(TreeSet::new));

        var encodedConfig = deviceConfig.stream().map(DeviceConfig::encode).toList();

        var size = encodedConfig.stream().mapToInt(e -> e.length).sum();

        var config = new byte[size];
        var offset = 0;
        for (var encoded : encodedConfig) {
            arraycopy(encoded, 0, config, offset, encoded.length);
            offset += encoded.length;
        }

        var fields = BigInteger.valueOf(deviceConfig.stream()
                .map(DeviceConfig::field)
                .map(DeviceConfigField::getValue)
                .reduce(0L, (field, mask) -> field | mask));

        var message = new SetDeviceConfig(
                (short) 1, // end of data
                new short[8],
                requireNonNullElse(localHandler.getAvailableFields(), fields),
                fields,
                size,
                config);

        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }
        writer.write(message).await(30, SECONDS);
        var deviceConfigResult = localHandler.listenForSetDeviceConfigResult(30, SECONDS);
        var result = DeviceConfigResult.findConfigResult(deviceConfigResult.result());
        if (!result.isSuccess()) {
            throw new RuntimeException("Setting device config did not succeed! configs=%s. %s"
                    .formatted(
                            configs.stream().map(Object::toString).collect(Collectors.joining(", ")),
                            "SetDeviceConfig=" + message));
        }
        localHandler.consumeSetDeviceConfig(fields.longValue(), config);
    }

    @RuleAction(
            label = "Reset Electricity Meter Counters",
            description = "Send SUPLA_CALCFG_CMD_RESET_COUNTERS to a Supla channel")
    public synchronized void resetElectricMeterCounters(String channelUID)
            throws InterruptedException, TimeoutException {
        var localHandler = thingHandler;
        if (localHandler == null) {
            log.warn("Thing handler is null!");
            return;
        }
        var parsedChannelUID = parseChannelUID(channelUID);
        var thingUID = localHandler.getThing().getUID();
        if (!thingUID.equals(parsedChannelUID.getThingUID())) {
            throw new IllegalArgumentException(
                    "Channel UID %s does not belong to thing %s".formatted(parsedChannelUID, thingUID));
        }
        int channelNumber = ChannelUtil.findSuplaChannelNumber(parsedChannelUID)
                .map(Short::intValue)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find channel number from " + parsedChannelUID));
        resetElectricMeterCounters(channelNumber);
    }

    @RuleAction(
            label = "Reset Electricity Meter Counters",
            description = "Send SUPLA_CALCFG_CMD_RESET_COUNTERS to a Supla channel number")
    public synchronized void resetElectricMeterCounters(int channelNumber)
            throws InterruptedException, TimeoutException {
        var localHandler = thingHandler;
        if (localHandler == null) {
            log.warn("Thing handler is null!");
            return;
        }
        if (!localHandler.hasRegisteredElectricityMeterChannel(channelNumber)) {
            throw new IllegalArgumentException(
                    "Cannot find electricity meter device channel for channel number " + channelNumber);
        }

        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }

        var message = new DeviceCalCfgRequest(
                0, channelNumber, SUPLA_CALCFG_CMD_RESET_COUNTERS, SUPER_USER_AUTHORIZED, 0, 0L, EMPTY_DATA);
        localHandler.clearDeviceCalCfgResult();
        writer.write(message).await(30, SECONDS);

        var result = localHandler.listenForDeviceCalCfgResult(30, SECONDS);
        if (result.channelNumber() != channelNumber) {
            throw new RuntimeException("Reset counters returned a different channel number! request=%s, result=%s"
                    .formatted(message, result));
        }
        if (result.command() != SUPLA_CALCFG_CMD_RESET_COUNTERS) {
            throw new RuntimeException(
                    "Reset counters returned a different command! request=%s, result=%s".formatted(message, result));
        }
        if (result.result() != SUPLA_CALCFG_RESULT_DONE) {
            throw new RuntimeException(
                    "Reset counters did not succeed! request=%s, result=%s".formatted(message, result));
        }
    }

    private static ChannelUID parseChannelUID(String channelUID) {
        try {
            return new ChannelUID(channelUID);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse channel UID from " + channelUID, e);
        }
    }

    public static void setDeviceConfig(@Nullable ThingActions actions, String... deviceConfigs)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).setDeviceConfig(deviceConfigs);
    }

    public static void setDeviceConfig(@Nullable ThingActions actions, Collection<String> configs)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).setDeviceConfig(configs);
    }

    public static void resetElectricMeterCounters(@Nullable ThingActions actions, String channelUID)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).resetElectricMeterCounters(channelUID);
    }

    public static void resetElectricMeterCounters(@Nullable ThingActions actions, int channelNumber)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).resetElectricMeterCounters(channelNumber);
    }
}
