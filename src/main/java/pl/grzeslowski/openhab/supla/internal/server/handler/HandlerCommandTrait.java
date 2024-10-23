package pl.grzeslowski.openhab.supla.internal.server.handler;

import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue.Mode.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.Hvac.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.HVAC_MODE_CHANNEL_ID;
import static pl.grzeslowski.openhab.supla.internal.server.ChannelUtil.findSuplaChannelNumber;
import static tech.units.indriya.unit.Units.CELSIUS;

import io.netty.channel.ChannelFuture;
import jakarta.annotation.Nullable;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.javatuples.Pair;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaChannelNewValue;

@NonNullByDefault
@RequiredArgsConstructor
class HandlerCommandTrait implements HandleCommand {
    private final SuplaDevice suplaDevice;

    @Override
    public void handleRefreshCommand(ChannelUID channelUID) {
        var state = suplaDevice.findState(channelUID);
        if (state == null) {
            return;
        }
        suplaDevice.updateState(channelUID, state);
    }

    @Override
    public void handleOnOffCommand(ChannelUID channelUID, OnOffType command) {
        var toSend =
                switch (command) {
                    case ON -> new Pair<>(OnOff.ON, OnOffType.OFF);
                    case OFF -> new Pair<>(OnOff.OFF, OnOffType.ON);
                };
        sendCommandToSuplaServer(channelUID, toSend.getValue0(), command, toSend.getValue1());
    }

    @Override
    public void handleUpDownCommand(ChannelUID channelUID, UpDownType command) {
        var toSend =
                switch (command) {
                    case UP -> new Pair<>(OnOff.ON, OnOffType.OFF);
                    case DOWN -> new Pair<>(OnOff.OFF, OnOffType.ON);
                };
        sendCommandToSuplaServer(channelUID, toSend.getValue0(), command, toSend.getValue1());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleHsbCommand(ChannelUID channelUID, HSBType command) {
        RgbValue toSend = new RgbValue(
                command.getBrightness().intValue(),
                255, // TODO I don't know if this is
                // correct
                command.getRed().intValue(),
                command.getGreen().intValue(),
                command.getBlue().intValue());
        sendCommandToSuplaServer(channelUID, toSend, command, null);
    }

    @Override
    public void handleOpenClosedCommand(ChannelUID channelUID, OpenClosedType command) {
        var toSend =
                switch (command) {
                    case OPEN -> new Pair<>(OnOff.ON, OnOffType.OFF);
                    case CLOSED -> new Pair<>(OnOff.OFF, OnOffType.ON);
                };
        sendCommandToSuplaServer(channelUID, toSend.getValue0(), command, toSend.getValue1());
    }

    @Override
    public void handlePercentCommand(ChannelUID channelUID, PercentType command) {
        sendCommandToSuplaServer(channelUID, new PercentValue(command.intValue()), command, null);
    }

    @Override
    public void handleDecimalCommand(ChannelUID channelUID, DecimalType command) {
        sendCommandToSuplaServer(channelUID, new DecimalValue(command.toBigDecimal()), command, null);
    }

    @Override
    public void handleStopMoveTypeCommand(ChannelUID channelUID, StopMoveType command) {
        suplaDevice
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
        if (id.equals(HVAC_MODE_CHANNEL_ID)) {
            handleHvacModeCommand(channelUID, command);
            return;
        }
        suplaDevice
                .getLogger()
                .warn(
                        "Not handling `{}` ({}) on channel `{}`",
                        command,
                        command.getClass().getSimpleName(),
                        channelUID);
    }

    private void handleHvacModeCommand(ChannelUID channelUID, StringType command) {
        var mode = valueOf(command.toString());
        var value =
                switch (mode) {
                    case NOT_SET -> throw new IllegalArgumentException(
                            "Cannot set mode channel to NOT_SET. channelUID=" + channelUID);
                    case OFF, DRY -> new HvacValue(
                            true, mode, null, null, HvacValue.Flags.builder().build());
                    case HEAT -> new HvacValue(
                            true,
                            HEAT,
                            findHeatValue(channelUID),
                            null,
                            HvacValue.Flags.builder().setPointTempHeatSet(true).build());
                    case COOL -> new HvacValue(
                            true,
                            COOL,
                            null,
                            findCoolValue(channelUID),
                            HvacValue.Flags.builder().setPointTempCoolSet(true).build());
                    case HEAT_COOL -> new HvacValue(
                            true,
                            HEAT_COOL,
                            findHeatValue(channelUID),
                            findCoolValue(channelUID),
                            HvacValue.Flags.builder()
                                    .setPointTempHeatSet(true)
                                    .setPointTempCoolSet(true)
                                    .build());
                    case FAN_ONLY -> new HvacValue(
                            true,
                            FAN_ONLY,
                            null,
                            null,
                            HvacValue.Flags.builder().fanEnabled(true).build());
                };

        var previousMode = suplaDevice.findState(channelUID);
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
                .map(suplaDevice::findState)
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
            var flags = HvacValue.Flags.builder();
            if (id.equals(HVAC_SET_POINT_TEMPERATURE_HEAT)) {
                setPointHeat = celsiusValue;
                setPointCool = null;
                flags.setPointTempHeatSet(true);
            } else {
                setPointHeat = null;
                setPointCool = celsiusValue;
                flags.setPointTempCoolSet(true);
            }

            var value = new HvacValue(on, NOT_SET, setPointHeat, setPointCool, flags.build());
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

        suplaDevice
                .getLogger()
                .warn(
                        "Not handling `{}` (unit={}, class={}) on channel `{}`",
                        command,
                        unit,
                        command.getClass().getSimpleName(),
                        channelUID);
    }

    private ChannelFuture sendCommandToSuplaServer(
            ChannelUID channelUID, ChannelValue channelValue, Command command, @Nullable State previousState) {
        var maybeChannelNumber = findSuplaChannelNumber(channelUID);
        if (maybeChannelNumber.isEmpty()) {
            throw new IllegalArgumentException("Cannot find channel number from " + channelUID);
        }
        var channelNumber = maybeChannelNumber.get();

        var encode = ChannelTypeEncoderImpl.INSTANCE.encode(channelValue);
        var senderId = suplaDevice.getSenderId().getAndIncrement();
        suplaDevice
                .getSenderIdToChannelUID()
                .put(senderId, new SuplaDevice.ChannelAndPreviousState(channelUID, previousState));
        var channelNewValue = new SuplaChannelNewValue(senderId, channelNumber, 100L, null, encode);
        try {
            ChannelFuture future = suplaDevice.write(channelNewValue);
            future.addListener(__ -> {
                suplaDevice.getLogger().debug("Changed value of channel for {} command {}", channelUID, command);
                suplaDevice.updateStatus(ONLINE);
            });
            return future;
        } catch (Exception ex) {
            var msg = "Couldn't Change value of channel for %s command %s.".formatted(channelUID, command);
            suplaDevice.updateStatus(OFFLINE, COMMUNICATION_ERROR, msg + ex.getLocalizedMessage());
            throw ex;
        }
    }
}
