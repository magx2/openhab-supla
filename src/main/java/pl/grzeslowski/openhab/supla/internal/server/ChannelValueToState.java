package pl.grzeslowski.openhab.supla.internal.server;

import static java.lang.String.valueOf;
import static java.util.Optional.ofNullable;
import static org.openhab.core.types.UnDefType.NULL;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.javatuples.Pair;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;

@NonNullByDefault
@RequiredArgsConstructor
public class ChannelValueToState implements ChannelValueSwitch.Callback<Stream<Pair<ChannelUID, State>>> {
    private final ThingUID thingUID;
    private final int channelNumber;

    @Override
    public Stream<Pair<ChannelUID, State>> onDecimalValue(@Nullable final DecimalValue decimalValue) {
        val id = createChannelUid(channelNumber);
        if (decimalValue == null) {
            return Stream.of(Pair.with(id, NULL));
        }
        return Stream.of(Pair.with(id, new DecimalType(decimalValue.getValue())));
    }

    @Override
    public Stream<Pair<ChannelUID, State>> onOnOff(@Nullable final OnOff onOff) {
        val id = createChannelUid(channelNumber);
        if (onOff == null) {
            return Stream.of(Pair.with(id, NULL));
        }
        return switch (onOff) {
            case ON -> Stream.of(Pair.with(id, OnOffType.ON));
            case OFF -> Stream.of(Pair.with(id, OnOffType.OFF));
        };
    }

    @Override
    public Stream<Pair<ChannelUID, State>> onOpenClose(@Nullable final OpenClose openClose) {
        val id = createChannelUid(channelNumber);
        if (openClose == null) {
            return Stream.of(Pair.with(id, NULL));
        }
        return switch (openClose) {
            case OPEN -> Stream.of(Pair.with(id, OpenClosedType.OPEN));
            case CLOSE -> Stream.of(Pair.with(id, OpenClosedType.CLOSED));
        };
    }

    @Override
    public Stream<Pair<ChannelUID, State>> onPercentValue(@Nullable final PercentValue percentValue) {
        val id = createChannelUid(channelNumber);
        if (percentValue == null) {
            return Stream.of(Pair.with(id, NULL));
        }
        return Stream.of(Pair.with(id, new PercentType(percentValue.getValue())));
    }

    @Override
    public Stream<Pair<ChannelUID, State>> onRgbValue(@Nullable final RgbValue rgbValue) {
        val id = createChannelUid(channelNumber);
        if (rgbValue == null) {
            return Stream.of(Pair.with(id, NULL));
        }
        return Stream.of(Pair.with(id, HSBType.fromRGB(rgbValue.getRed(), rgbValue.getGreen(), rgbValue.getBlue())));
    }

    @Override
    public Stream<Pair<ChannelUID, State>> onStoppableOpenClose(@Nullable final StoppableOpenClose stoppableOpenClose) {
        val id = createChannelUid(channelNumber);
        if (stoppableOpenClose == null) {
            return Stream.of(Pair.with(id, NULL));
        }
        return switch (stoppableOpenClose) {
            case OPEN -> Stream.of(Pair.with(id, OpenClosedType.OPEN));
            case CLOSE, STOP -> Stream.of(Pair.with(id, OpenClosedType.CLOSED));
        };
    }

    @Override
    public Stream<Pair<ChannelUID, State>> onTemperatureValue(@Nullable final TemperatureValue temperatureValue) {
        val id = createChannelUid(channelNumber);
        if (temperatureValue == null) {
            return Stream.of(Pair.with(id, NULL));
        }
        return Stream.of(Pair.with(id, new DecimalType(temperatureValue.getTemperature())));
    }

    @Override
    public Stream<Pair<ChannelUID, State>> onTemperatureAndHumidityValue(
            @Nullable final TemperatureAndHumidityValue temperatureAndHumidityValue) {
        val groupUid = new ChannelGroupUID(thingUID, valueOf(channelNumber));
        val tempId = new ChannelUID(groupUid, "temperature");
        val humidityId = new ChannelUID(groupUid, "humidity");
        if (temperatureAndHumidityValue == null) {
            return Stream.of(Pair.with(tempId, NULL), Pair.with(humidityId, NULL));
        }
        return Stream.of(
                Pair.with(tempId, new DecimalType(temperatureAndHumidityValue.getTemperature())),
                Pair.with(humidityId, new DecimalType(temperatureAndHumidityValue.getHumidity())));
    }

    @Override
    public Stream<Pair<ChannelUID, State>> onElectricityMeter(@Nullable ElectricityMeterValue electricityMeterValue) {
        val groupUid = new ChannelGroupUID(thingUID, valueOf(channelNumber));
        val pairs = new ArrayList<Pair<ChannelUID, State>>();
        val optionalMeter = ofNullable(electricityMeterValue);
        {
            val id = new ChannelUID(groupUid, "totalForwardActiveEnergyBalanced");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.getTotalForwardActiveEnergyBalanced()))
                    .orElse(NULL);
            pairs.add(Pair.with(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalReverseActiveEnergyBalanced");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.getTotalReverseActiveEnergyBalanced()))
                    .orElse(NULL);
            pairs.add(Pair.with(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "totalCost");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.getTotalCost()))
                    .orElse(NULL);
            pairs.add(Pair.with(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "pricePerUnit");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.getPricePerUnit()))
                    .orElse(NULL);
            pairs.add(Pair.with(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "currency");
            val stateValue = optionalMeter
                    .map(value -> ofNullable(value.getCurrency())
                            .map(Object::toString)
                            .<State>map(StringType::new)
                            .orElse(NULL))
                    .orElse(NULL);
            pairs.add(Pair.with(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "measuredValues");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.getMeasuredValues()))
                    .orElse(NULL);
            pairs.add(Pair.with(id, stateValue));
        }
        {
            val id = new ChannelUID(groupUid, "period");
            val stateValue = optionalMeter
                    .<State>map(value -> new DecimalType(value.getPeriod()))
                    .orElse(NULL);
            pairs.add(Pair.with(id, stateValue));
        }

        pairs.addAll(buildStateForPhase(groupUid, 1, electricityMeterValue));
        pairs.addAll(buildStateForPhase(groupUid, 2, electricityMeterValue));
        pairs.addAll(buildStateForPhase(groupUid, 3, electricityMeterValue));

        return pairs.stream();
    }

    private List<Pair<ChannelUID, State>> buildStateForPhase(
            ChannelGroupUID groupUid, int phaseNumber, @Nullable ElectricityMeterValue meter) {
        val pairs = new ArrayList<Pair<ChannelUID, State>>();
        val phase = ofNullable(meter).map(m -> meter.getPhases()).map(p -> p.get(phaseNumber - 1));
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalForwardActiveEnergy");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getTotalForwardActiveEnergy()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalReverseActiveEnergy");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getTotalReverseActiveEnergy()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalForwardReactiveEnergy");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getTotalForwardReactiveEnergy()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalReverseReactiveEnergy");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getTotalReverseReactiveEnergy()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "voltage");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getVoltage()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "current");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getCurrent()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerActive");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getPowerActive()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerReactive");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getPowerReactive()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerApparent");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getPowerApparent()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerApparent");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getPowerApparent()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerApparent");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getPowerApparent()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "frequency");
            val stateValue = phase.<State>map(value -> new DecimalType(value.getFrequency()))
                    .orElse(NULL);
            pairs.add(Pair.with(channelUid, stateValue));
        }
        return pairs;
    }

    @Override
    public Stream<Pair<ChannelUID, State>> onUnknownValue(@Nullable final UnknownValue unknownValue) {
        val id = createChannelUid(channelNumber);
        if (unknownValue == null) {
            return Stream.of(Pair.with(id, NULL));
        }

        return Stream.of(Pair.with(id, StringType.valueOf(unknownValue.getMessage())));
    }

    private ChannelUID createChannelUid(int channelNumber) {
        return new ChannelUID(thingUID, valueOf(channelNumber));
    }
}
