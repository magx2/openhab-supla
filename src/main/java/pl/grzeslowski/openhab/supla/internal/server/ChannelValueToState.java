package pl.grzeslowski.openhab.supla.internal.server;

import static java.lang.String.valueOf;
import static java.math.BigDecimal.ZERO;
import static java.util.Optional.ofNullable;
import static org.openhab.core.library.unit.SIUnits.CELSIUS;
import static org.openhab.core.types.UnDefType.NULL;
import static org.openhab.core.types.UnDefType.UNDEF;
import static pl.grzeslowski.jsupla.protocol.api.RgbwBitFunction.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.RgbwLed;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

@Slf4j
@NonNullByDefault
@RequiredArgsConstructor
public class ChannelValueToState implements ChannelValueSwitch.Callback<Stream<ChannelValueToState.ChannelState>> {
    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal UNDEF_TEMPERATURE_VALUE = BigDecimal.valueOf(-275);
    private final ThingUID thingUID;
    private final DeviceChannel deviceChannel;

    public record ChannelState(ChannelUID uid, State state) {}

    @Override
    public Stream<ChannelState> onDecimalValue(@Nullable final DecimalValue decimalValue) {
        val id = createChannelUid();
        if (decimalValue == null) {
            return Stream.of(new ChannelState(id, NULL));
        }
        return Stream.of(new ChannelState(id, new DecimalType(decimalValue.value())));
    }

    @Override
    public Stream<ChannelState> onOnOff(@Nullable final OnOff onOff) {
        val id = createChannelUid();
        if (onOff == null) {
            return Stream.of(new ChannelState(id, NULL));
        }
        return switch (onOff) {
            case ON -> Stream.of(new ChannelState(id, OnOffType.ON));
            case OFF -> Stream.of(new ChannelState(id, OnOffType.OFF));
        };
    }

    @Override
    public Stream<ChannelState> onOpenClose(@Nullable final OpenClose openClose) {
        val id = createChannelUid();
        if (openClose == null) {
            return Stream.of(new ChannelState(id, NULL));
        }
        return switch (openClose) {
            case OPEN -> Stream.of(new ChannelState(id, OpenClosedType.OPEN));
            case CLOSE -> Stream.of(new ChannelState(id, OpenClosedType.CLOSED));
        };
    }

    @Override
    public Stream<ChannelState> onPercentValue(@Nullable final PercentValue percentValue) {
        val id = createChannelUid();
        if (percentValue == null) {
            return Stream.of(new ChannelState(id, NULL));
        }
        return Stream.of(new ChannelState(id, new PercentType(percentValue.value())));
    }

    @Override
    public Stream<ChannelState> onRgbValue(@Nullable final RgbValue rgbValue) {
        var maybeRgbValue = ofNullable(rgbValue);
        var rgbwBitFunctions = deviceChannel.rgbwBitFunctions();
        val groupUid = new ChannelGroupUID(thingUID, valueOf(deviceChannel.number()));

        var channels = new ArrayList<ChannelState>();

        if (rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_RGB_LIGHTING)
                || rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING)
                || rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT_AND_RGB)) {
            var rgbUid = new ChannelUID(groupUid, RgbwLed.COLOR);
            var state = maybeRgbValue.map(ChannelValueToState::toHsbType).orElse(NULL);
            channels.add(new ChannelState(rgbUid, state));
        }
        if (rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_AND_RGB_LIGHTING)
                || rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER)) {
            var brightnessUid = new ChannelUID(groupUid, RgbwLed.BRIGHTNESS);
            var state = maybeRgbValue
                    .map(rgb -> (State) new PercentType(rgb.brightness()))
                    .orElse(NULL);
            channels.add(new ChannelState(brightnessUid, state));
        }
        if (rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT)
                || rgbwBitFunctions.contains(SUPLA_RGBW_BIT_FUNC_DIMMER_CCT_AND_RGB)) {
            var brightnessCctUid = new ChannelUID(groupUid, RgbwLed.BRIGHTNESS_CCT);
            var state = maybeRgbValue
                    .map(rgb -> (State) new PercentType(rgb.dimmerCct()))
                    .orElse(NULL);
            channels.add(new ChannelState(brightnessCctUid, state));
        }

        return channels.stream();
    }

    private static @NonNull State toHsbType(@NonNull RgbValue rgbValue) {
        var hsbType = HSBType.fromRGB(rgbValue.red(), rgbValue.green(), rgbValue.blue());
        return new HSBType(hsbType.getHue(), hsbType.getSaturation(), new PercentType(rgbValue.colorBrightness()));
    }

    @Override
    public Stream<ChannelState> onStoppableOpenClose(@Nullable final StoppableOpenClose stoppableOpenClose) {
        val id = createChannelUid();
        if (stoppableOpenClose == null) {
            return Stream.of(new ChannelState(id, NULL));
        }
        return switch (stoppableOpenClose) {
            case OPEN -> Stream.of(new ChannelState(id, OpenClosedType.OPEN));
            case CLOSE, STOP -> Stream.of(new ChannelState(id, OpenClosedType.CLOSED));
        };
    }

    @Override
    public Stream<ChannelState> onTemperatureValue(@Nullable final TemperatureValue temperatureValue) {
        val id = createChannelUid();
        if (temperatureValue == null) {
            return Stream.of(new ChannelState(id, NULL));
        }
        var temperature = temperatureValue.temperature();
        if (temperature.compareTo(UNDEF_TEMPERATURE_VALUE) == 0) {
            return Stream.of(new ChannelState(id, UNDEF));
        }
        return Stream.of(buildTempPair(temperature, id));
    }

    private static ChannelState buildTempPair(BigDecimal temperature, ChannelUID id) {
        if (temperature.equals(UNDEF_TEMPERATURE_VALUE)) {
            return new ChannelState(id, UNDEF);
        }
        return new ChannelState(id, new QuantityType<>(temperature, CELSIUS));
    }

    @Override
    public Stream<ChannelState> onTemperatureAndHumidityValue(
            @Nullable final TemperatureAndHumidityValue temperatureAndHumidityValue) {
        val groupUid = new ChannelGroupUID(thingUID, valueOf(deviceChannel.number()));
        val tempId = new ChannelUID(groupUid, "temperature");
        val humidityId = new ChannelUID(groupUid, "humidity");
        if (temperatureAndHumidityValue == null) {
            return Stream.of(new ChannelState(tempId, NULL), new ChannelState(humidityId, NULL));
        }
        return Stream.of(
                buildTempPair(temperatureAndHumidityValue.temperature(), tempId),
                buildHumidityPair(temperatureAndHumidityValue, humidityId));
    }

    private static ChannelState buildHumidityPair(
            TemperatureAndHumidityValue temperatureAndHumidityValue, ChannelUID humidityId) {
        var humidity = temperatureAndHumidityValue.humidity();
        if (humidity.compareTo(BigDecimal.valueOf(-1)) <= 0) {
            return new ChannelState(humidityId, UNDEF);
        }
        return new ChannelState(
                humidityId,
                new PercentType(humidity.multiply(ONE_HUNDRED).max(ZERO).min(ONE_HUNDRED)));
    }

    @Override
    public Stream<ChannelState> onElectricityMeter(@Nullable ElectricityMeterValue electricityMeterValue) {
        val groupUid = new ChannelGroupUID(thingUID, valueOf(deviceChannel.number()));
        val pairs = new ArrayList<ChannelState>();
        val optionalMeter = ofNullable(electricityMeterValue);
        {
            val id = new ChannelUID(groupUid, "totalForwardActiveEnergyBalanced");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(new BigDecimal(value.totalForwardActiveEnergyBalanced())))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalReverseActiveEnergyBalanced");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(new BigDecimal(value.totalReverseActiveEnergyBalanced())))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalCost");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.totalCost()))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "pricePerUnit");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.pricePerUnit()))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "currency");
            val stateValue = optionalMeter
                    .map(value -> ofNullable(value.currency())
                            .map(Object::toString)
                            .<State>map(StringType::new)
                            .orElse(NULL))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "measuredValues");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.measuredValues()))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "period");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.period()))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }

        pairs.addAll(buildStateForPhase(groupUid, 1, electricityMeterValue));
        pairs.addAll(buildStateForPhase(groupUid, 2, electricityMeterValue));
        pairs.addAll(buildStateForPhase(groupUid, 3, electricityMeterValue));

        return pairs.stream();
    }

    private List<ChannelState> buildStateForPhase(
            ChannelGroupUID groupUid, int phaseNumber, @Nullable ElectricityMeterValue meter) {
        val pairs = new ArrayList<ChannelState>();
        val phase = ofNullable(meter).map(ElectricityMeterValue::phases).map(p -> p.get(phaseNumber - 1));
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalForwardActiveEnergy");
            val stateValue = phase.<State>map(value -> new DecimalType(value.totalForwardActiveEnergy()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalReverseActiveEnergy");
            val stateValue = phase.<State>map(value -> new DecimalType(value.totalReverseActiveEnergy()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalForwardReactiveEnergy");
            val stateValue = phase.<State>map(value -> new DecimalType(value.totalForwardReactiveEnergy()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalReverseReactiveEnergy");
            val stateValue = phase.<State>map(value -> new DecimalType(value.totalReverseReactiveEnergy()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "voltage");
            val stateValue =
                    phase.<State>map(value -> new DecimalType(value.voltage())).orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "current");
            val stateValue =
                    phase.<State>map(value -> new DecimalType(value.current())).orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerActive");
            val stateValue = phase.<State>map(value -> new DecimalType(value.powerActive()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerReactive");
            val stateValue = phase.<State>map(value -> new DecimalType(value.powerReactive()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerApparent");
            val stateValue = phase.<State>map(value -> new DecimalType(value.powerApparent()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerApparent");
            val stateValue = phase.<State>map(value -> new DecimalType(value.powerApparent()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerApparent");
            val stateValue = phase.<State>map(value -> new DecimalType(value.powerApparent()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "frequency");
            val stateValue = phase.<State>map(value -> new DecimalType(value.frequency()))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        return pairs;
    }

    @Override
    public Stream<ChannelState> onHvacValue(HvacValue channelValue) {
        val groupUid = new ChannelGroupUID(thingUID, valueOf(deviceChannel.number()));
        val pairs = new ArrayList<ChannelState>();
        {
            val id = new ChannelUID(groupUid, "on");
            val stateValue = OnOffType.from(channelValue.on());
            pairs.add(new ChannelState(id, stateValue));
        } // on
        {
            val id = new ChannelUID(groupUid, "mode");
            val stateValue = StringType.valueOf(channelValue.mode().name());
            pairs.add(new ChannelState(id, stateValue));
        } // mode
        {
            val id = new ChannelUID(groupUid, "setPointTemperatureHeat");
            val stateValue = ofNullable(channelValue.setPointTemperatureHeat())
                    .<State>map(temp -> new QuantityType<>(temp, CELSIUS))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        } // setPointTemperatureHeat
        {
            val id = new ChannelUID(groupUid, "setPointTemperatureCool");
            val stateValue = ofNullable(channelValue.setPointTemperatureCool())
                    .<State>map(temp -> new QuantityType<>(temp, CELSIUS))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        } // setPointTemperatureCool
        {
            val flags = channelValue.flags();
            {
                val id = new ChannelUID(groupUid, "flags-setPointTempHeatSet");
                val stateValue = OnOffType.from(flags.setPointTempHeatSet());
                pairs.add(new ChannelState(id, stateValue));
            } // setPointTempHeatSet
            {
                val id = new ChannelUID(groupUid, "flags-setPointTempCoolSet");
                val stateValue = OnOffType.from(flags.setPointTempCoolSet());
                pairs.add(new ChannelState(id, stateValue));
            } // setPointTempCoolSet
            {
                val id = new ChannelUID(groupUid, "flags-heating");
                val stateValue = OnOffType.from(flags.heating());
                pairs.add(new ChannelState(id, stateValue));
            } // heating
            {
                val id = new ChannelUID(groupUid, "flags-cooling");
                val stateValue = OnOffType.from(flags.cooling());
                pairs.add(new ChannelState(id, stateValue));
            } // cooling
            {
                val id = new ChannelUID(groupUid, "flags-weeklySchedule");
                val stateValue = OnOffType.from(flags.weeklySchedule());
                pairs.add(new ChannelState(id, stateValue));
            } // weeklySchedule
            {
                val id = new ChannelUID(groupUid, "flags-countdownTimer");
                val stateValue = OnOffType.from(flags.countdownTimer());
                pairs.add(new ChannelState(id, stateValue));
            } // countdownTimer
            {
                val id = new ChannelUID(groupUid, "flags-fanEnabled");
                val stateValue = OnOffType.from(flags.fanEnabled());
                pairs.add(new ChannelState(id, stateValue));
            } // fanEnabled
            {
                val id = new ChannelUID(groupUid, "flags-thermometerError");
                val stateValue = OnOffType.from(flags.thermometerError());
                pairs.add(new ChannelState(id, stateValue));
            } // thermometerError
            {
                val id = new ChannelUID(groupUid, "flags-clockError");
                val stateValue = OnOffType.from(flags.clockError());
                pairs.add(new ChannelState(id, stateValue));
            } // clockError
            {
                val id = new ChannelUID(groupUid, "flags-forcedOffBySensor");
                val stateValue = OnOffType.from(flags.forcedOffBySensor());
                pairs.add(new ChannelState(id, stateValue));
            } // forcedOffBySensor
            {
                val id = new ChannelUID(groupUid, "flags-cool");
                val stateValue = OnOffType.from(flags.cooling());
                pairs.add(new ChannelState(id, stateValue));
            } // cool
            {
                val id = new ChannelUID(groupUid, "flags-weeklyScheduleTemporalOverride");
                val stateValue = OnOffType.from(flags.weeklyScheduleTemporalOverride());
                pairs.add(new ChannelState(id, stateValue));
            } // weeklyScheduleTemporalOverride
            {
                val id = new ChannelUID(groupUid, "flags-batteryCoverOpen");
                val stateValue = OnOffType.from(flags.batteryCoverOpen());
                pairs.add(new ChannelState(id, stateValue));
            } // batteryCoverOpen
        } // flags
        return pairs.stream();
    }

    @Override
    public Stream<ChannelState> onTimerValue(TimerValue channelValue) {
        // do not know what to do with this
        log.debug("Do not know how to handle timer={}", channelValue);
        return Stream.empty();
    }

    @Override
    public Stream<ChannelState> onActionTrigger(@Nullable ActionTrigger actionTriggerValue) {
        // action triggers does not have state
        return Stream.empty();
    }

    @Override
    public Stream<ChannelState> onUnknownValue(@Nullable final UnknownValue unknownValue) {
        val id = createChannelUid();
        if (unknownValue == null) {
            return Stream.of(new ChannelState(id, NULL));
        }

        return Stream.of(new ChannelState(id, StringType.valueOf(unknownValue.message())));
    }

    private ChannelUID createChannelUid() {
        return new ChannelUID(thingUID, valueOf(deviceChannel.number()));
    }
}
