package pl.grzeslowski.openhab.supla.internal.server.handler;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.javatuples.Pair;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocol.api.channeltype.encoders.ChannelTypeEncoderImpl;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.jsupla.protocol.api.structs.csd.ChannelStateRequest;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SuplaChannelNewValue;

import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.COMMUNICATION_ERROR;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.Hvac.HVAC_SET_POINT_TEMPERATURE_COOL;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.Hvac.HVAC_SET_POINT_TEMPERATURE_HEAT;
import static pl.grzeslowski.openhab.supla.internal.server.ChannelUtil.findSuplaChannelNumber;
import static tech.units.indriya.unit.Units.CELSIUS;

@NonNullByDefault
@RequiredArgsConstructor
class HandlerProtoTrait implements HandleCommand {
    private final SuplaDevice suplaDevice;

    @Override
    public void handleRefreshCommand(ChannelUID channelUID) {
        var maybeChannelNumber = findSuplaChannelNumber(channelUID);
        if (maybeChannelNumber.isEmpty()) {
            suplaDevice.getLogger().warn("Cannot parse channelNumber from {}", channelUID);
            return;
        }
        var channelNumber = maybeChannelNumber.get();
        var request = new ChannelStateRequest(suplaDevice.getSenderId().getAndIncrement(), null, channelNumber);
        suplaDevice.write(request);
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
        suplaDevice
                .getLogger()
                .warn(
                        "Not handling `{}` ({}) on channel `{}`",
                        command,
                        command.getClass().getSimpleName(),
                        channelUID);
    }

    @Override
    public void handleQuantityType(ChannelUID channelUID, QuantityType<?> command) {
        var unit = command.getUnit();
        var id = channelUID.getIdWithoutGroup();
        if ((id.equals(HVAC_SET_POINT_TEMPERATURE_HEAT)
                || id.equals(HVAC_SET_POINT_TEMPERATURE_COOL))
                && unit.isCompatible(CELSIUS)) {
            var celsiusQuantity = requireNonNull(command.toUnit(CELSIUS));
            var celsiusValue = celsiusQuantity.doubleValue();

            var on = true;
           var mode = HvacValue.Mode.NOT_SET;
            Double setPointHeat;
            Double setPointCool;
            HvacValue.Flags flags;
            if (id.equals(HVAC_SET_POINT_TEMPERATURE_HEAT)) {
                setPointHeat = celsiusValue;
                setPointCool = null;
                flags = new HvacValue.Flags(true, false, false, false, false, false, false, false, false, false, false, false, false);
            } else {
                setPointHeat = null;
                setPointCool = celsiusValue;
                flags = new HvacValue.Flags(false, true, false, false, false, false, false, false, false, false, false, false, false);
            }

            var value = new HvacValue(on, mode, setPointHeat, setPointCool, flags);
            sendCommandToSuplaServer(channelUID, value, command, null);
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

    private void sendCommandToSuplaServer(
            ChannelUID channelUID, ChannelValue channelValue, Command command, @Nullable State previousState) {
        var maybeChannelNumber = findSuplaChannelNumber(channelUID);
        if (maybeChannelNumber.isEmpty()) {
            suplaDevice.getLogger().warn("Cannot parse channelNumber from {}", channelUID);
            return;
        }
        var channelNumber = maybeChannelNumber.get();

        var encode = ChannelTypeEncoderImpl.INSTANCE.encode(channelValue);
        var senderId = suplaDevice.getSenderId().getAndIncrement();
        suplaDevice
                .getSenderIdToChannelUID()
                .put(senderId, new SuplaDevice.ChannelAndPreviousState(channelUID, previousState));
        var channelNewValue = new SuplaChannelNewValue(senderId, channelNumber, 100L, null, encode);
        try {
            suplaDevice.write(channelNewValue).addCompleteListener(() -> {
                suplaDevice.getLogger().debug("Changed value of channel for {} command {}", channelUID, command);
                suplaDevice.updateStatus(ONLINE);
            });
        } catch (Exception ex) {
            var msg = "Couldn't Change value of channel for %s command %s.".formatted(channelUID, command);
            suplaDevice.getLogger().debug(msg, ex);
            suplaDevice.updateStatus(OFFLINE, COMMUNICATION_ERROR, msg + ex.getLocalizedMessage());
        }
    }
}
