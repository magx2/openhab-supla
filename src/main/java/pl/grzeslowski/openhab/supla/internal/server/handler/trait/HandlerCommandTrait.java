package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue.Mode.*;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue.Mode.NOT_SET;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.RgbValue.Command.*;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.Hvac.*;
import static pl.grzeslowski.openhab.supla.internal.server.ChannelUtil.findSuplaChannelNumber;
import static tech.units.indriya.unit.Units.CELSIUS;

import io.netty.channel.ChannelFuture;
import jakarta.annotation.Nullable;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.util.ColorUtil;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoderImpl;
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
        var state = serverDevice.findStateDeprecated(channelUID);
        if (state == null) {
            return;
        }
        serverDevice.updateState(channelUID, state);
    }

    @Override
    public void handleOnOffCommand(ChannelUID channelUID, OnOffType command) {
        var toSend =
                switch (command) {
                    case ON -> new ValueAndPrevState(OnOff.ON, OnOffType.OFF);
                    case OFF -> new ValueAndPrevState(OnOff.OFF, OnOffType.ON);
                };
        sendCommandToSuplaServer(channelUID, toSend.value, command, toSend.prev);
    }

    @Override
    public void handleUpDownCommand(ChannelUID channelUID, UpDownType command) {
        var toSend =
                switch (command) {
                    case UP -> new ValueAndPrevState(OnOff.ON, OnOffType.OFF);
                    case DOWN -> new ValueAndPrevState(OnOff.OFF, OnOffType.ON);
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
    public void handleOpenClosedCommand(ChannelUID channelUID, OpenClosedType command) {
        var toSend =
                switch (command) {
                    case OPEN -> new ValueAndPrevState(OnOff.ON, OnOffType.OFF);
                    case CLOSED -> new ValueAndPrevState(OnOff.OFF, OnOffType.ON);
                };
        sendCommandToSuplaServer(channelUID, toSend.value, command, toSend.prev);
    }

    @Override
    public void handlePercentCommand(ChannelUID channelUID, PercentType command) {
        if (RgbwLed.BRIGHTNESS.equals(channelUID.getIdWithoutGroup())) {
            ledChangeDim(channelUID, command);
        } else {
            sendCommandToSuplaServer(channelUID, new PercentValue(command.intValue()), command, null);
        }
    }

    @Override
    public void handleDecimalCommand(ChannelUID channelUID, DecimalType command) {
        sendCommandToSuplaServer(channelUID, new DecimalValue(command.toBigDecimal()), command, null);
    }

    @Override
    public void handleStopMoveTypeCommand(ChannelUID channelUID, StopMoveType command) {
        serverDevice
                .getLogger()
                .warn(
                        "Not handling `{}` ({}) on channel `{}`",
                        command,
                        command.getClass().getSimpleName(),
                        channelUID);
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
        var mode = HvacValue.Mode.valueOf(command.toString());
        var value =
                switch (mode) {
                    case NOT_SET ->
                        throw new IllegalArgumentException(
                                "Cannot set mode channel to NOT_SET. channelUID=" + channelUID);
                    case OFF, DRY ->
                        new HvacValue(
                                true,
                                mode,
                                null,
                                null,
                                new HvacValue.Flags(
                                        false, false, false, false, false, false, false, false, false, false, false,
                                        false, false));
                    case HEAT ->
                        new HvacValue(
                                true,
                                HEAT,
                                findHeatValue(channelUID),
                                null,
                                new HvacValue.Flags(
                                        true, false, false, false, false, false, false, false, false, false, false,
                                        false, false));
                    case COOL ->
                        new HvacValue(
                                true,
                                COOL,
                                null,
                                findCoolValue(channelUID),
                                new HvacValue.Flags(
                                        false, true, false, false, false, false, false, false, false, false, false,
                                        false, false));
                    case HEAT_COOL ->
                        new HvacValue(
                                true,
                                HEAT_COOL,
                                findHeatValue(channelUID),
                                findCoolValue(channelUID),
                                new HvacValue.Flags(
                                        true, true, false, false, false, false, false, false, false, false, false,
                                        false, false));
                    case FAN_ONLY ->
                        new HvacValue(
                                true,
                                FAN_ONLY,
                                null,
                                null,
                                new HvacValue.Flags(
                                        false, false, false, false, false, false, true, false, false, false, false,
                                        false, false));
                };

        var previousMode = serverDevice.findStateDeprecated(channelUID);
        var future = sendCommandToSuplaServer(channelUID, value, command, previousMode);
    }

    @Nullable
    private Double findHeatValue(ChannelUID channelUID) {
        return findTemperature(channelUID, HVAC_SET_POINT_TEMPERATURE_HEAT);
    }

    @Nullable
    private Double findCoolValue(ChannelUID channelUID) {
        return findTemperature(channelUID, HVAC_SET_POINT_TEMPERATURE_COOL);
    }

    @Nullable
    private Double findTemperature(ChannelUID channelUID, String id) {
        return Optional.of(channelUID)
                .map(uid -> siblingChannel(uid, id))
                .map(serverDevice::findStateDeprecated)
                .<QuantityType<?>>map(state -> (QuantityType<?>) state)
                .filter(state -> state.getUnit().isCompatible(CELSIUS))
                .map(state -> state.toUnit(CELSIUS))
                .map(QuantityType::doubleValue)
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
            var celsiusValue = celsiusQuantity.doubleValue();

            var on = true;
            Double setPointHeat;
            Double setPointCool;
            HvacValue.Flags flags;
            if (id.equals(HVAC_SET_POINT_TEMPERATURE_HEAT)) {
                setPointHeat = celsiusValue;
                setPointCool = null;
                flags = new HvacValue.Flags(
                        true, false, false, false, false, false, false, false, false, false, false, false, false);
            } else {
                setPointHeat = null;
                setPointCool = celsiusValue;
                flags = new HvacValue.Flags(
                        false, true, false, false, false, false, false, false, false, false, false, false, false);
            }

            var value = new HvacValue(on, NOT_SET, setPointHeat, setPointCool, flags);
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

        var encode = ChannelTypeEncoderImpl.INSTANCE.encode(channelValue);
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
        var brightnessChannelUid =
                new ChannelUID(channelUID.getThingUID(), channelUID.getGroupId(), RgbwLed.BRIGHTNESS);
        var brightness = serverDevice
                .findState(brightnessChannelUid)
                .filter(PercentType.class::isInstance)
                .map(PercentType.class::cast)
                .map(DecimalType::intValue)
                .orElse(0);
        {
            var rgb = ColorUtil.hsbToRgb(command);
            var rgbProto = new RgbValue(brightness, command.getBrightness().intValue(), rgb[0], rgb[1], rgb[2]);
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
        var toSend = new RgbValue(brightness, 0, 0, 0, 0, protoCommand);
        sendCommandToSuplaServer(channelUID, toSend, command);
    }
    // ❌ LED
}
