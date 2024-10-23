package pl.grzeslowski.openhab.supla.internal.server;

import static java.lang.System.arraycopy;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static pl.grzeslowski.jsupla.protocol.api.ChannelFunction.SUPLA_CHANNELFNC_NONE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.BINDING_ID;

import java.math.BigInteger;
import java.time.Duration;
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
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import pl.grzeslowski.jsupla.protocol.api.encoders.ChannelConfigHVACEncoder;
import pl.grzeslowski.jsupla.protocol.api.structs.ChannelConfigHVAC;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACTemperatureCfg;
import pl.grzeslowski.jsupla.protocol.api.structs.HvacParameterFlags;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.ChannelConfig;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SetDeviceConfig;
import pl.grzeslowski.openhab.supla.internal.server.handler.ServerAbstractDeviceHandler;
import pl.grzeslowski.openhab.supla.internal.server.handler.device_config.DeviceConfig;
import pl.grzeslowski.openhab.supla.internal.server.handler.device_config.DeviceConfigField;
import pl.grzeslowski.openhab.supla.internal.server.handler.device_config.DeviceConfigResult;
import pl.grzeslowski.openhab.supla.internal.server.handler.device_config.DeviceConfigUtil;

@ThingActionsScope(name = BINDING_ID)
@NonNullByDefault
@Slf4j
public class SuplaServerDeviceActions implements ThingActions {
    @Getter
    @Nullable
    private ServerAbstractDeviceHandler thingHandler;

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (!(handler instanceof ServerAbstractDeviceHandler suplaHandler)) {
            var handlerClass = Optional.of(handler)
                    .map(ThingHandler::getClass)
                    .map(Class::getSimpleName)
                    .orElse("<null>");
            log.warn(
                    "Handler {} has wrong class, actualClass={}, expectedClass={}",
                    handlerClass,
                    ServerAbstractDeviceHandler.class.getSimpleName());
            return;
        }
        this.thingHandler = suplaHandler;
    }

    @RuleAction(label = "HVAC Config", description = "todo")
    public void hvacConfig() throws InterruptedException {
        try {
            var localHandler = thingHandler;
            if (localHandler == null) {
                log.warn("Thing handler is null!");
                return;
            }
            int mainThermometerChannelId = 12; // channel z auratona
            int masterThermostatChannelId = 11;
            var hvacConfig = new ChannelConfigHVAC(
                    null, // mainThermometerChannelId
                    (short) mainThermometerChannelId, // mainThermometerChannelNo
                    0, // auxThermometerChannelId
                    null, // auxThermometerChannelNo
                    0, // binarySensorChannelId
                    null, // binarySensorChannelNo
                    (byte) SUPLA_HVAC_AUX_THERMOMETER_TYPE_NOT_SET, // auxThermometerType
                    (short) 1, // antiFreezeAndOverheatProtectionEnabled
                    SUPLA_HVAC_ALGORITHM_NOT_SET, // availableAlgorithms
                    (int) SUPLA_HVAC_ALGORITHM_ON_OFF_SETPOINT_MIDDLE, // usedAlgorithm
                    (int) Duration.ofSeconds(60).toSeconds(), // minOnTimeS
                    (int) Duration.ofSeconds(60).toSeconds(), // minOffTimeS
                    (byte) 0, // outputValueOnError
                    (short) SUPLA_HVAC_SUBFUNCTION_HEAT, // subfunction
                    (short) 1, // temperatureSetpointChangeSwitchesToManualMode
                    (short) 0, // auxMinMaxSetpointEnabled
                    (short) 0, // useSeparateHeatCoolOutputs
                    HvacParameterFlags.builder().build(), // parameterFlags
                    null, // masterThermostatChannelId
                    (short) 1, // masterThermostatIsSet
                    (short) masterThermostatChannelId, // masterThermostatChannelNo */
                    /*masterThermostatChannelId,
                    null,
                    null, //*/
                    0, // heatOrColdSourceSwitchChannelId
                    0, // pumpSwitchChannelId
                    new HVACTemperatureCfg(0, new short[24])); // temperatures
            var message = new ChannelConfig(
                    (short) 6,
                    SUPLA_CHANNELFNC_NONE.getValue(),
                    (short) SUPLA_CONFIG_TYPE_DEFAULT,
                    hvacConfig.size(),
                    ChannelConfigHVACEncoder.INSTANCE.encode(hvacConfig));
            /*
            var message = new SetDeviceConfig(
                    (byte) 1,
                    new short[8],
                    BigInteger.ZERO,
                    BigInteger.ZERO,
                    hvacConfig.size(),
                    ChannelConfigHVACEncoder.INSTANCE.encode(hvacConfig));
             */
            var writer = localHandler.getWriter().get();
            if (writer == null) {
                throw new IllegalStateException("There is no socket writer!");
            }
            writer.write(message).await(30, SECONDS);
        } catch (Exception ex) {
            log.error("Error writing config", ex);
            throw ex;
        }
    }

    public static void hvacConfig(@Nullable ThingActions actions) throws InterruptedException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).hvacConfig();
    }

    @RuleAction(label = "Device Config Config", description = "todo")
    public void setDeviceConfig(String... deviceConfigs) throws InterruptedException, TimeoutException {
        setDeviceConfig(Arrays.asList(deviceConfigs));
    }

    @RuleAction(label = "Device Config Config", description = "todo")
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
                .map(DeviceConfig::getField)
                .map(DeviceConfigField::getMask)
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
        var result = DeviceConfigResult.findConfigResult(deviceConfigResult.result);
        if (!result.isSuccess()) {
            throw new RuntimeException("Setting device config did not succeed! configs=%s. %s"
                    .formatted(
                            configs.stream().map(Object::toString).collect(Collectors.joining(", ")),
                            "SetDeviceConfig=" + message));
        }
        localHandler.consumeSetDeviceConfig(fields.longValue(), config);
    }

    public static void setDeviceConfig(@Nullable ThingActions actions, String... deviceConfigs)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).setDeviceConfig(deviceConfigs);
    }

    public static void setDeviceConfig(@Nullable ThingActions actions, Collection<String> configs)
            throws InterruptedException, TimeoutException {
        ((SuplaServerDeviceActions) requireNonNull(actions)).setDeviceConfig(configs);
    }
}
