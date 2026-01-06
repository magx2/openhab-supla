package pl.grzeslowski.openhab.supla.internal.server;

import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.openhab.core.library.unit.SIUnits.CELSIUS;
import static org.openhab.core.types.UnDefType.NULL;
import static org.openhab.core.types.UnDefType.UNDEF;

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
import pl.grzeslowski.jsupla.protocol.api.HvacFlag;
import pl.grzeslowski.jsupla.protocol.api.ThermostatValueFlag;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ActionTrigger;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.RgbwLed;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

@Slf4j
@NonNullByDefault
@RequiredArgsConstructor
public class ChannelValueToState {
    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal UNDEF_TEMPERATURE_VALUE = BigDecimal.valueOf(-275);
    private final ThingUID thingUID;
    private final DeviceChannel deviceChannel;

    public record ChannelState(ChannelUID uid, State state) {}

    public Stream<ChannelState> switchOn(@lombok.NonNull ChannelValue channelValue) {
        return switch (channelValue) {
            case ElectricityMeterValue value -> onElectricityMeter(value);
            case HeatpolThermostatValue value -> onHeatpolThermostatValue(value);
            case HumidityValue value -> onHumidityValue(value);
            case HvacValue value -> onHvacValue(value);
            case OnOffValue value -> onOnOff(value);
            case PercentValue value -> onPercentValue(value);
            case PressureValue value -> onPressureValue(value);
            case RainValue value -> onRainValue(value);
            case RgbValue value -> onRgbValue(value);
            case TemperatureAndHumidityValue value -> onTemperatureAndHumidityValue(value);
            case TemperatureDoubleValue value -> onTemperatureValue(value);
            case TimerValue value -> onTimerValue(value);
            case UnknownValue value -> onUnknownValue(value);
            case WeightValue value -> onWeightValue(value);
            case WindValue value -> onWindValue(value);
            case ActionTrigger value -> onActionTrigger(value);
        };
    }

    private Stream<ChannelState> onHeatpolThermostatValue(HeatpolThermostatValue value) {
        var channelGroupUid = createChannelGroupUid();
        var flagsStream = stream(ThermostatValueFlag.values()).map(flag -> {
            var flagUid = new ChannelUID(channelGroupUid, "flag-" + flag.name());
            var flagState = OnOffType.from(value.flags().contains(flag));
            return new ChannelState(flagUid, flagState);
        });
        var basicStream = Stream.of(
                new ChannelState(new ChannelUID(channelGroupUid, "on"), OnOffType.from(value.on())),
                new ChannelState(
                        new ChannelUID(channelGroupUid, "measuredTemperature"),
                        new DecimalType(value.measuredTemperature())),
                new ChannelState(
                        new ChannelUID(channelGroupUid, "presetTemperature"),
                        new DecimalType(value.presetTemperature())));
        return Stream.concat(basicStream, flagsStream);
    }

    private Stream<ChannelState> onHumidityValue(HumidityValue value) {
        return Stream.of(new ChannelState(createChannelUid(), new PercentType(value.humidity())));
    }

    private Stream<ChannelState> onPressureValue(PressureValue value) {
        return Stream.of(new ChannelState(createChannelUid(), new DecimalType(value.value())));
    }

    private Stream<ChannelState> onRainValue(RainValue value) {
        return Stream.of(new ChannelState(createChannelUid(), new DecimalType(value.value())));
    }

    private Stream<ChannelState> onWeightValue(WeightValue value) {
        return Stream.of(new ChannelState(createChannelUid(), new DecimalType(value.value())));
    }

    private Stream<ChannelState> onWindValue(WindValue value) {
        return Stream.of(new ChannelState(createChannelUid(), new DecimalType(value.value())));
    }

    private Stream<ChannelState> onOnOff(OnOffValue onOff) {
        val id = createChannelUid();
        return switch (onOff) {
            case ON -> Stream.of(new ChannelState(id, OnOffType.ON));
            case OFF -> Stream.of(new ChannelState(id, OnOffType.OFF));
        };
    }

    private Stream<ChannelState> onPercentValue(PercentValue percentValue) {
        return Stream.of(new ChannelState(createChannelUid(), new PercentType(percentValue.value())));
    }

    private Stream<ChannelState> onRgbValue(RgbValue value) {
        var rgbwBitFunctions = deviceChannel.rgbwBitFunctions();
        val groupUid = createChannelGroupUid();

        var channels = new ArrayList<ChannelState>();
        var info = RgbChannelInfo.build(deviceChannel);
        if (info.supportRgb()) {
            var rgbUid = new ChannelUID(groupUid, RgbwLed.COLOR);
            var state = toHsbType(value);
            channels.add(new ChannelState(rgbUid, state));
        }
        if (info.supportDimmer()) {
            var brightnessUid = new ChannelUID(groupUid, RgbwLed.BRIGHTNESS);
            var state = (State) new PercentType(value.brightness());
            channels.add(new ChannelState(brightnessUid, state));
        }
        if (info.supportDimmerCct()) {
            var brightnessCctUid = new ChannelUID(groupUid, RgbwLed.BRIGHTNESS_CCT);
            var state = new PercentType(value.dimmerCct());
            channels.add(new ChannelState(brightnessCctUid, state));
        }

        return channels.stream();
    }

    private static @NonNull State toHsbType(@NonNull RgbValue rgbValue) {
        var hsbType = HSBType.fromRGB(rgbValue.red(), rgbValue.green(), rgbValue.blue());
        return new HSBType(hsbType.getHue(), hsbType.getSaturation(), new PercentType(rgbValue.colorBrightness()));
    }

    private Stream<ChannelState> onTemperatureValue(TemperatureDoubleValue temperatureValue) {
        return Stream.of(buildTemperatureChannel(createChannelUid(), temperatureValue.temperature()));
    }

    private static ChannelState buildTemperatureChannel(ChannelUID id, BigDecimal temperature) {
        if (temperature.compareTo(UNDEF_TEMPERATURE_VALUE) == 0) {
            return new ChannelState(id, UNDEF);
        }
        return new ChannelState(id, new QuantityType<>(temperature, CELSIUS));
    }

    private Stream<ChannelState> onTemperatureAndHumidityValue(
            TemperatureAndHumidityValue temperatureAndHumidityValue) {
        val groupUid = createChannelGroupUid();

        var temperature = temperatureAndHumidityValue.temperature();
        var humidity = temperatureAndHumidityValue
                .humidity()
                .map(HumidityValue::humidity)
                .map(h -> (State) new PercentType(h))
                .orElse(UNDEF);

        return Stream.of(
                buildTemperatureChannel(new ChannelUID(groupUid, "temperature"), temperature),
                new ChannelState(new ChannelUID(groupUid, "humidity"), humidity));
    }

    private Stream<ChannelState> onElectricityMeter(@Nullable ElectricityMeterValue electricityMeterValue) {
        val groupUid = createChannelGroupUid();
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

    private Stream<ChannelState> onHvacValue(HvacValue channelValue) {
        val groupUid = createChannelGroupUid();
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
            var flagChannelStates = stream(HvacFlag.values())
                    .map(flag -> {
                        val id = new ChannelUID(groupUid, "flag-" + flag.name());
                        val stateValue = OnOffType.from(flags.contains(flag));
                        return new ChannelState(id, stateValue);
                    })
                    .toList();
            pairs.addAll(flagChannelStates);
        } // flags
        return pairs.stream();
    }

    private Stream<ChannelState> onTimerValue(TimerValue channelValue) {
        // do not know what to do with this
        log.debug("Do not know how to handle timer={}", channelValue);
        return Stream.empty();
    }

    private Stream<ChannelState> onActionTrigger(@Nullable ActionTrigger value) {
        // action triggers does not have state
        return Stream.empty();
    }

    private Stream<ChannelState> onUnknownValue(UnknownValue unknownValue) {
        return Stream.of(new ChannelState(createChannelUid(), StringType.valueOf(unknownValue.message())));
    }

    private ChannelUID createChannelUid() {
        return new ChannelUID(thingUID, valueOf(deviceChannel.number()));
    }

    private @NonNull ChannelGroupUID createChannelGroupUid() {
        return new ChannelGroupUID(thingUID, valueOf(deviceChannel.number()));
    }
}
