package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static pl.grzeslowski.jsupla.protocol.api.HvacFlag.SUPLA_HVAC_VALUE_FLAG_SETPOINT_TEMP_COOL_SET;
import static pl.grzeslowski.jsupla.protocol.api.HvacFlag.SUPLA_HVAC_VALUE_FLAG_SETPOINT_TEMP_HEAT_SET;
import static pl.grzeslowski.jsupla.protocol.api.HvacMode.*;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.RgbValue.Command.*;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.Hvac.*;
import static pl.grzeslowski.openhab.supla.internal.server.ChannelUtil.findSuplaChannelNumber;
import static tech.units.indriya.unit.Units.CELSIUS;

import io.netty.channel.ChannelFuture;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.util.ColorUtil;
import pl.grzeslowski.jsupla.protocol.api.HvacFlag;
import pl.grzeslowski.jsupla.protocol.api.HvacMode;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoder;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaChannelNewValue;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.RgbwLed;

@NonNullByDefault
@RequiredArgsConstructor
public class HandlerCommandTrait implements HandleCommand {
    private final ServerDevice serverDevice;

    private record ValueAndPrevState(ChannelValue value, State prev) {}

    @Override
    public void handleRefreshCommand(ChannelUID channelUID) {
        serverDevice.findState(channelUID).ifPresent(state -> serverDevice.updateState(channelUID, state));
    }

    @Override
    public void handleOnOffCommand(ChannelUID channelUID, OnOffType command) {
        var toSend =
                switch (command) {
                    case ON -> new ValueAndPrevState(OnOffValue.ON, OnOffType.OFF);
                    case OFF -> new ValueAndPrevState(OnOffValue.OFF, OnOffType.ON);
                };
        sendCommandToSuplaServer(channelUID, toSend.value, command, toSend.prev);
    }

    @Override
    public void handleUpDownCommand(ChannelUID channelUID, UpDownType command) {
        var toSend =
                switch (command) {
                    case UP -> new ValueAndPrevState(OnOffValue.ON, OnOffType.OFF);
                    case DOWN -> new ValueAndPrevState(OnOffValue.OFF, OnOffType.ON);
                };
        sendCommandToSuplaServer(channelUID, toSend.value, command, toSend.prev);
    }

    @Override
    public void handleOpenClosedCommand(ChannelUID channelUID, OpenClosedType command) {
        var toSend =
                switch (command) {
                    case OPEN -> new ValueAndPrevState(OnOffValue.ON, OnOffType.OFF);
                    case CLOSED -> new ValueAndPrevState(OnOffValue.OFF, OnOffType.ON);
                };
        sendCommandToSuplaServer(channelUID, toSend.value, command, toSend.prev);
    }

    @Override
    public void handleStopMoveTypeCommand(ChannelUID channelUID, StopMoveType command) {
        var toSend =
                switch (command) {
                    case MOVE -> new ValueAndPrevState(OnOffValue.ON, OnOffType.OFF);
                    case STOP -> new ValueAndPrevState(OnOffValue.OFF, OnOffType.ON);
                };
        sendCommandToSuplaServer(channelUID, toSend.value, command, toSend.prev);
    }

    @Override
    public void handleHsbCommand(ChannelUID channelUID, HSBType command) {
        if (RgbwLed.COLOR.equals(channelUID.getIdWithoutGroup())) {
            ledChangeRgb(channelUID, command);
        } else {
            serverDevice
                    .getLogger()
                    .warn(
                            "Not handling `{}` ({}) on channel `{}`",
                            command,
                            command.getClass().getSimpleName(),
                            channelUID);
        }
    }

    @Override
    public void handlePercentCommand(ChannelUID channelUID, PercentType command) {
        if (RgbwLed.BRIGHTNESS.equals(channelUID.getIdWithoutGroup())) {
            ledChangeDim(channelUID, command);
        } else if (RgbwLed.BRIGHTNESS_CCT.equals(channelUID.getIdWithoutGroup())) {
            ledChangeDimCct(channelUID, command);
        } else {
            sendCommandToSuplaServer(channelUID, new PercentValue(command.intValue()), command, null);
        }
    }

    @Override
    public void handleStringCommand(ChannelUID channelUID, StringType command) {
        var id = channelUID.getIdWithoutGroup();
        if (id.equals(HVAC_MODE)) {
            handleHvacModeCommand(channelUID, command);
            return;
        }
        serverDevice
                .getLogger()
                .warn(
                        "Not handling `{}` ({}) on channel `{}`",
                        command,
                        command.getClass().getSimpleName(),
                        channelUID);
    }

    private void handleHvacModeCommand(ChannelUID channelUID, StringType command) {
        var mode = HvacMode.valueOf(addPrefix("SUPLA_HVAC_MODE_", command));
        var value =
                switch (mode) {
                    case SUPLA_HVAC_MODE_NOT_SET ->
                        throw new IllegalArgumentException(
                                "Cannot set mode channel to NOT_SET. channelUID=" + channelUID);
                    case SUPLA_HVAC_MODE_OFF, SUPLA_HVAC_MODE_DRY -> new HvacValue(true, mode, null, null, Set.of());
                    case SUPLA_HVAC_MODE_HEAT ->
                        new HvacValue(
                                true,
                                SUPLA_HVAC_MODE_HEAT,
                                findHeatValue(channelUID),
                                null,
                                Set.of(SUPLA_HVAC_VALUE_FLAG_SETPOINT_TEMP_HEAT_SET));
                    case SUPLA_HVAC_MODE_COOL ->
                        new HvacValue(
                                true,
                                SUPLA_HVAC_MODE_COOL,
                                null,
                                findCoolValue(channelUID),
                                Set.of(SUPLA_HVAC_VALUE_FLAG_SETPOINT_TEMP_COOL_SET));
                    case SUPLA_HVAC_MODE_HEAT_COOL ->
                        new HvacValue(
                                true,
                                SUPLA_HVAC_MODE_HEAT_COOL,
                                findHeatValue(channelUID),
                                findCoolValue(channelUID),
                                Set.of(
                                        SUPLA_HVAC_VALUE_FLAG_SETPOINT_TEMP_HEAT_SET,
                                        SUPLA_HVAC_VALUE_FLAG_SETPOINT_TEMP_COOL_SET));
                    case SUPLA_HVAC_MODE_FAN_ONLY ->
                        new HvacValue(true, SUPLA_HVAC_MODE_FAN_ONLY, null, null, Set.of());
                    case SUPLA_HVAC_MODE_CMD_TURN_ON,
                            SUPLA_HVAC_MODE_CMD_WEEKLY_SCHEDULE,
                            SUPLA_HVAC_MODE_CMD_SWITCH_TO_MANUAL ->
                        throw new UnsupportedOperationException("Do not know how to handle " + mode);
                };

        var previousMode = serverDevice.findState(channelUID).orElse(null);
        var future = sendCommandToSuplaServer(channelUID, value, command, previousMode);
    }

    @SuppressWarnings("SameParameterValue")
    private static @NonNull String addPrefix(String prefix, StringType command) {
        return command.toString().startsWith(prefix) ? command.toString() : prefix + command;
    }

    @Nullable
    private BigDecimal findHeatValue(ChannelUID channelUID) {
        return findTemperature(channelUID, HVAC_SET_POINT_TEMPERATURE_HEAT);
    }

    @Nullable
    private BigDecimal findCoolValue(ChannelUID channelUID) {
        return findTemperature(channelUID, HVAC_SET_POINT_TEMPERATURE_COOL);
    }

    @Nullable
    private BigDecimal findTemperature(ChannelUID channelUID, String id) {
        return Optional.of(channelUID)
                .map(uid -> siblingChannel(uid, id))
                .flatMap(serverDevice::findState)
                .<QuantityType<?>>map(state -> (QuantityType<?>) state)
                .filter(state -> state.getUnit().isCompatible(CELSIUS))
                .map(state -> state.toUnit(CELSIUS))
                .map(QuantityType::toBigDecimal)
                .orElse(null);
    }

    private static ChannelUID siblingChannel(ChannelUID channelUID, String id) {
        return new ChannelUID(channelUID.getThingUID(), channelUID.getGroupId(), id);
    }

    @Override
    public void handleQuantityType(ChannelUID channelUID, QuantityType<?> command) {
        var unit = command.getUnit();
        var id = channelUID.getIdWithoutGroup();
        if ((id.equals(HVAC_SET_POINT_TEMPERATURE_HEAT) || id.equals(HVAC_SET_POINT_TEMPERATURE_COOL))
                && unit.isCompatible(CELSIUS)) {
            var celsiusQuantity = requireNonNull(command.toUnit(CELSIUS));
            var celsiusValue = celsiusQuantity.toBigDecimal();

            var on = true;
            BigDecimal setPointHeat;
            BigDecimal setPointCool;
            Set<HvacFlag> flags;
            if (id.equals(HVAC_SET_POINT_TEMPERATURE_HEAT)) {
                setPointHeat = celsiusValue;
                setPointCool = null;
                flags = Set.of(SUPLA_HVAC_VALUE_FLAG_SETPOINT_TEMP_HEAT_SET);
            } else {
                setPointHeat = null;
                setPointCool = celsiusValue;
                flags = Set.of(SUPLA_HVAC_VALUE_FLAG_SETPOINT_TEMP_COOL_SET);
            }

            var value = new HvacValue(on, SUPLA_HVAC_MODE_NOT_SET, setPointHeat, setPointCool, flags);
            var future = sendCommandToSuplaServer(channelUID, value, command, null);
            future.addListener(__ -> {
                var groupId = channelUID.getGroupId();
                if (groupId == null) {
                    return;
                }
                var group = new ChannelGroupUID(channelUID.getThingUID(), groupId);
                var modeUid = new ChannelUID(group, HVAC_MODE);
                handleRefreshCommand(modeUid);
            });
            return;
        }

        serverDevice
                .getLogger()
                .warn(
                        "Not handling `{}` (unit={}, class={}) on channel `{}`",
                        command,
                        unit,
                        command.getClass().getSimpleName(),
                        channelUID);
    }

    @SuppressWarnings("UnusedReturnValue")
    private ChannelFuture sendCommandToSuplaServer(ChannelUID channelUID, ChannelValue channelValue, Command command) {
        return sendCommandToSuplaServer(channelUID, channelValue, command, null);
    }

    private ChannelFuture sendCommandToSuplaServer(
            ChannelUID channelUID, ChannelValue channelValue, Command command, @Nullable State previousState) {
        var maybeChannelNumber = findSuplaChannelNumber(channelUID);
        if (maybeChannelNumber.isEmpty()) {
            throw new IllegalArgumentException("Cannot find channel number from " + channelUID);
        }
        var channelNumber = maybeChannelNumber.get();

        var encode = ChannelTypeEncoder.INSTANCE.encode(channelValue);
        var senderId = serverDevice.getSenderId().getAndIncrement();
        serverDevice
                .getSenderIdToChannelUID()
                .put(senderId, new ServerDevice.ChannelAndPreviousState(channelUID, previousState));
        var channelNewValue = new SuplaChannelNewValue(senderId, channelNumber, 100L, null, encode);
        try {
            ChannelFuture future = serverDevice.write(channelNewValue);
            future.addListener(__ -> {
                serverDevice.getLogger().debug("Changed value of channel for {} command {}", channelUID, command);
                serverDevice.updateStatus(ONLINE);
            });
            return future;
        } catch (Exception ex) {
            var msg = text("supla.offline.channel-change-failed", channelUID, command, ex.getLocalizedMessage());
            serverDevice.updateStatus(OFFLINE, COMMUNICATION_ERROR, msg);
            throw ex;
        }
    }

    // ✅ LED
    private void ledChangeRgb(ChannelUID channelUID, HSBType command) {
        var brightness = serverDevice
                .findState(new ChannelUID(channelUID.getThingUID(), channelUID.getGroupId(), RgbwLed.BRIGHTNESS))
                .filter(PercentType.class::isInstance)
                .map(PercentType.class::cast)
                .map(DecimalType::intValue)
                .orElse(0);
        var brightnessCct = serverDevice
                .findState(new ChannelUID(channelUID.getThingUID(), channelUID.getGroupId(), RgbwLed.BRIGHTNESS_CCT))
                .filter(PercentType.class::isInstance)
                .map(PercentType.class::cast)
                .map(DecimalType::intValue)
                .orElse(0);
        {
            var rgb = ColorUtil.hsbToRgb(command);
            var rgbProto =
                    new RgbValue(brightness, command.getBrightness().intValue(), rgb[0], rgb[1], rgb[2], brightnessCct);
            serverDevice.getLogger().debug("Sending RGB command: {}", rgbProto);
            sendCommandToSuplaServer(
                    channelUID,
                    rgbProto,
                    command,
                    serverDevice.findState(channelUID).orElse(null));
        }
    }

    private void ledChangeDim(ChannelUID channelUID, PercentType command) {
        var brightness = command.intValue();
        var protoCommand = brightness > 0 ? TURN_ON_DIMMER : TURN_OFF_DIMMER;
        var toSend = new RgbValue(brightness, 0, 0, 0, 0, 0, protoCommand);
        sendCommandToSuplaServer(channelUID, toSend, command);
    }

    private void ledChangeDimCct(ChannelUID channelUID, PercentType command) {
        var brightness = command.intValue();
        var toSend = new RgbValue(0, 0, 0, 0, 0, brightness);
        sendCommandToSuplaServer(channelUID, toSend, command);
    }
    // ❌ LED
}
