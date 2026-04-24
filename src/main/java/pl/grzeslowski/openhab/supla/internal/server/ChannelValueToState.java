package pl.grzeslowski.openhab.supla.internal.server;

import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static org.openhab.core.library.unit.CurrencyUnits.BASE_CURRENCY;
import static org.openhab.core.library.unit.CurrencyUnits.BASE_ENERGY_PRICE;
import static org.openhab.core.library.unit.CurrencyUnits.createCurrency;
import static org.openhab.core.library.unit.SIUnits.CELSIUS;
import static org.openhab.core.library.unit.Units.AMPERE;
import static org.openhab.core.library.unit.Units.DEGREE_ANGLE;
import static org.openhab.core.library.unit.Units.HERTZ;
import static org.openhab.core.library.unit.Units.KILOVAR_HOUR;
import static org.openhab.core.library.unit.Units.KILOWATT_HOUR;
import static org.openhab.core.library.unit.Units.PERCENT;
import static org.openhab.core.library.unit.Units.SECOND;
import static org.openhab.core.library.unit.Units.VAR;
import static org.openhab.core.library.unit.Units.VOLT;
import static org.openhab.core.library.unit.Units.VOLT_AMPERE;
import static org.openhab.core.library.unit.Units.WATT;
import static org.openhab.core.types.UnDefType.NULL;
import static org.openhab.core.types.UnDefType.UNDEF;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.measure.Quantity;
import javax.measure.Unit;
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

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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
            case ElectricityMeterSimpleValue value -> onElectricityMeter(value);
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
                        new QuantityType<>(value.measuredTemperature(), CELSIUS)),
                new ChannelState(
                        new ChannelUID(channelGroupUid, "presetTemperature"),
                        new QuantityType<>(value.presetTemperature(), CELSIUS)));
        return Stream.concat(basicStream, flagsStream);
    }

    private Stream<ChannelState> onHumidityValue(HumidityValue value) {
        return Stream.of(new ChannelState(createChannelUid(), humidityToState(value)));
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
                .map(this::humidityToState)
                .orElse(UNDEF);

        return Stream.of(
                buildTemperatureChannel(new ChannelUID(groupUid, "temperature"), temperature),
                new ChannelState(new ChannelUID(groupUid, "humidity"), humidity));
    }

    private State humidityToState(HumidityValue value) {
        return new QuantityType<>(value.humidity(), PERCENT);
    }

    private Stream<ChannelState> onElectricityMeter(@Nullable ElectricityMeterSimpleValue electricityMeterValue) {
        val groupUid = createChannelGroupUid();
        val pairs = new ArrayList<ChannelState>();
        val optionalMeter = ofNullable(electricityMeterValue);
        {
            val id = new ChannelUID(groupUid, "totalForwardActiveEnergy");
            val stateValue = optionalMeter
                    .map(ElectricityMeterSimpleValue::totalForwardActiveEnergy)
                    .map(value -> quantityState(value, KILOWATT_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        return pairs.stream();
    }

    private Stream<ChannelState> onElectricityMeter(@Nullable ElectricityMeterValue electricityMeterValue) {
        val groupUid = createChannelGroupUid();
        val pairs = new ArrayList<ChannelState>();
        val optionalMeter = ofNullable(electricityMeterValue);
        {
            val id = new ChannelUID(groupUid, "totalForwardActiveEnergy");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::totalForwardActiveEnergy)
                    .map(number -> quantityState(number, KILOWATT_HOUR))
                    .orElse(UNDEF);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalReverseActiveEnergy");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::totalReverseActiveEnergy)
                    .map(value -> quantityState(value, KILOWATT_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalForwardReactiveEnergy");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::totalForwardReactiveEnergy)
                    .map(value -> quantityState(value, KILOVAR_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalReverseReactiveEnergy");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::totalReverseReactiveEnergy)
                    .map(value -> quantityState(value, KILOVAR_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalForwardActiveEnergyBalanced");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::totalForwardActiveEnergyBalanced)
                    .map(value -> quantityState(value, KILOWATT_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalReverseActiveEnergyBalanced");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::totalReverseActiveEnergyBalanced)
                    .map(value -> quantityState(value, KILOWATT_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalCost");
            val stateValue = optionalMeter
                    .map(value -> quantityState(value.totalCost(), meterCurrency(value)))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "pricePerUnit");
            val stateValue = optionalMeter
                    .map(value -> quantityState(value.pricePerUnit(), meterEnergyPrice(value)))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "currency");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::currency)
                    .map(currency -> currency.map(Object::toString)
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
                    .map(ElectricityMeterValue::period)
                    .map(value -> quantityState(value, SECOND))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "voltagePhaseAngle12");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::voltagePhaseAngle12)
                    .map(value -> quantityState(value, DEGREE_ANGLE))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "voltagePhaseAngle13");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::voltagePhaseAngle13)
                    .map(value -> quantityState(value, DEGREE_ANGLE))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "phaseSequenceVoltage");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::phaseSequence)
                    .map(phaseSequence -> phaseSequence
                            .map(ElectricityMeterValue.PhaseSequence::voltage)
                            .map(Enum::name)
                            .<State>map(StringType::new)
                            .orElse(UNDEF))
                    .orElse(NULL);
            pairs.add(new ChannelState(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "phaseSequenceCurrent");
            val stateValue = optionalMeter
                    .map(ElectricityMeterValue::phaseSequence)
                    .map(phaseSequence -> phaseSequence
                            .map(ElectricityMeterValue.PhaseSequence::current)
                            .map(Enum::name)
                            .<State>map(StringType::new)
                            .orElse(UNDEF))
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
        val phase = ofNullable(meter).flatMap(value -> switch (phaseNumber) {
            case 1 -> value.phase1();
            case 2 -> value.phase2();
            case 3 -> value.phase3();
            default -> Optional.empty();
        });
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalForwardActiveEnergy");
            val stateValue = phase.map(ElectricityMeterValue.Phase::totalForwardActiveEnergy)
                    .map(value -> quantityState(value, KILOWATT_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalReverseActiveEnergy");
            val stateValue = phase.map(ElectricityMeterValue.Phase::totalReverseActiveEnergy)
                    .map(value -> quantityState(value, KILOWATT_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalForwardReactiveEnergy");
            val stateValue = phase.map(ElectricityMeterValue.Phase::totalForwardReactiveEnergy)
                    .map(value -> quantityState(value, KILOVAR_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalReverseReactiveEnergy");
            val stateValue = phase.map(ElectricityMeterValue.Phase::totalReverseReactiveEnergy)
                    .map(value -> quantityState(value, KILOVAR_HOUR))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "voltage");
            val stateValue = phase.map(ElectricityMeterValue.Phase::voltage)
                    .map(value -> quantityState(value, VOLT))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "current");
            val stateValue = phase.map(ElectricityMeterValue.Phase::current)
                    .map(value -> quantityState(value, AMPERE))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerActive");
            val stateValue = phase.map(ElectricityMeterValue.Phase::powerActive)
                    .map(value -> quantityState(value, WATT))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerReactive");
            val stateValue = phase.map(ElectricityMeterValue.Phase::powerReactive)
                    .map(value -> quantityState(value, VAR))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerApparent");
            val stateValue = phase.map(ElectricityMeterValue.Phase::powerApparent)
                    .map(value -> quantityState(value, VOLT_AMPERE))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerFactor");
            val stateValue = phase.map(ElectricityMeterValue.Phase::powerFactor)
                    .map(ChannelValueToState::decimalState)
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "phaseAngle");
            val stateValue = phase.map(ElectricityMeterValue.Phase::phaseAngle)
                    .map(value -> quantityState(value, DEGREE_ANGLE))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "frequency");
            val stateValue = phase.map(ElectricityMeterValue.Phase::frequency)
                    .map(value -> quantityState(value, HERTZ))
                    .orElse(NULL);
            pairs.add(new ChannelState(channelUid, stateValue));
        }
        return pairs;
    }

    private static <T extends Quantity<T>> State quantityState(Optional<? extends Number> value, Unit<T> unit) {
        return value.<State>map(number -> new QuantityType<>(number, unit)).orElse(UNDEF);
    }

    private static <T extends Quantity<T>> State quantityState(Number value, Unit<T> unit) {
        return new QuantityType<>(value, unit);
    }

    private static Unit<org.openhab.core.library.dimension.Currency> meterCurrency(ElectricityMeterValue value) {
        return value.currency().map(ChannelValueToState::currencyUnit).orElse(BASE_CURRENCY);
    }

    @SuppressWarnings("unchecked")
    private static Unit<org.openhab.core.library.dimension.EnergyPrice> meterEnergyPrice(ElectricityMeterValue value) {
        return value.currency()
                .map(currency -> (Unit<org.openhab.core.library.dimension.EnergyPrice>)
                        currencyUnit(currency).divide(KILOWATT_HOUR))
                .orElse(BASE_ENERGY_PRICE);
    }

    private static Unit<org.openhab.core.library.dimension.Currency> currencyUnit(Currency currency) {
        val currencyCode = currency.getCurrencyCode();
        return createCurrency(currencyCode, currencyCode);
    }

    private static State decimalState(Optional<? extends Number> value) {
        return value.<State>map(DecimalType::new).orElse(UNDEF);
    }

    private static State decimalState(Number value) {
        return new DecimalType(value.toString());
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
