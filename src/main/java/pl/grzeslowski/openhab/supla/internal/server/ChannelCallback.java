package pl.grzeslowski.openhab.supla.internal.server;

import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.Hvac.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.*;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ItemType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import pl.grzeslowski.jsupla.protocol.api.HvacFlag;
import pl.grzeslowski.jsupla.protocol.api.ThermostatValueFlag;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ChannelClassSwitch;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ChannelIds.RgbwLed;
import pl.grzeslowski.openhab.supla.internal.server.traits.DeviceChannel;

@NonNullByDefault
@RequiredArgsConstructor
@Slf4j
public class ChannelCallback implements ChannelClassSwitch.Callback<Stream<Channel>> {
    private final ThingUID thingUID;
    private final DeviceChannel deviceChannel;

    private ChannelUID createChannelUid() {
        return new ChannelUID(thingUID, valueOf(deviceChannel.number()));
    }

    private ChannelTypeUID createChannelTypeUID(String id) {
        return new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, id);
    }

    @Override
    public Stream<Channel> onOnOff() {
        log.debug("{} {} onOnOff", thingUID, deviceChannel);
        return switchChannel();
    }

    private Stream<Channel> switchChannel() {
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(SWITCH_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, "Switch")
                .withType(channelTypeUID)
                .withLabel("Allows you to turn thing ON/OFF")
                .build());
    }

    @Override
    public Stream<Channel> onPercentValue() {
        log.debug("{} {} onPercentValue", thingUID, deviceChannel);

        var channelUid = createChannelUid();
        var channelTypeUID = createChannelTypeUID(DIMMER_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, "Dimmer")
                .withType(channelTypeUID)
                .withDefaultTags(Set.of("Control", "Brightness"))
                .build());
    }

    @Override
    public Stream<Channel> onRgbValue() {
        log.debug("{} {} onRgbValue", thingUID, deviceChannel);
        var groupUid = buildGroupUid();

        var channels = new ArrayList<Channel>();
        var info = RgbChannelInfo.build(deviceChannel);
        if (info.supportRgb()) {
            var colorChannelUid = new ChannelUID(groupUid, RgbwLed.COLOR);
            channels.add(ChannelBuilder.create(colorChannelUid, COLOR)
                    .withLabel("LED Color")
                    .withType(createChannelTypeUID(RGB_CHANNEL_ID))
                    .withDefaultTags(Set.of("Control", "Color"))
                    .build());
        }
        if (info.supportDimmer()) {
            var brightnessChannelUid = new ChannelUID(groupUid, RgbwLed.BRIGHTNESS);
            channels.add(ChannelBuilder.create(brightnessChannelUid, DIMMER)
                    .withLabel("White Brightness")
                    .withType(createChannelTypeUID(DIMMER_CHANNEL_ID))
                    .withDefaultTags(Set.of("Control", "Brightness"))
                    .build());
        }
        if (info.supportDimmerCct()) {
            var brightnessChannelUid = new ChannelUID(groupUid, RgbwLed.BRIGHTNESS_CCT);
            channels.add(ChannelBuilder.create(brightnessChannelUid, DIMMER)
                    .withLabel("CCT Brightness")
                    .withType(createChannelTypeUID(DIMMER_CHANNEL_ID))
                    .withDefaultTags(Set.of("Control", "Brightness"))
                    .build());
        }

        return channels.stream();
    }

    @Override
    public Stream<Channel> onHumidityValue() {
        log.debug("{} {} onHumidityValue", thingUID, deviceChannel);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(HUMIDITY_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, NUMBER_DIMENSIONLESS)
                .withType(channelTypeUID)
                .withLabel("Humidity")
                .build());
    }

    @Override
    public Stream<Channel> onTemperatureDoubleValue() {
        log.debug("{} {} onTemperatureValue", thingUID, deviceChannel);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(TEMPERATURE_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, NUMBER_TEMPERATURE)
                .withType(channelTypeUID)
                .withLabel("Temperature")
                .build());
    }

    @Override
    public Stream<Channel> onTemperatureAndHumidityValue() {
        log.debug("{} {} onTemperatureAndHumidityValue", thingUID, deviceChannel);
        val groupUid = buildGroupUid();
        val channels = new ArrayList<Channel>();
        {
            val channelUid = new ChannelUID(groupUid, "temperature");
            val channelTypeUID = createChannelTypeUID(TEMPERATURE_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, NUMBER_TEMPERATURE)
                    .withType(channelTypeUID)
                    .withLabel("Temperature")
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "humidity");
            val channelTypeUID = createChannelTypeUID(HUMIDITY_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, NUMBER_DIMENSIONLESS)
                    .withType(channelTypeUID)
                    .withLabel("Humidity")
                    .build());
        }
        return channels.stream();
    }

    @Override
    public Stream<Channel> onElectricityMeter() {
        log.debug("{} {} onElectricityMeter", thingUID, deviceChannel);
        val groupUid = buildGroupUid();
        val channels = new ArrayList<Channel>();

        // main
        {
            val channelUid = new ChannelUID(groupUid, "totalForwardActiveEnergyBalanced");
            var channelTypeUID = createChannelTypeUID(ENERGY_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:ElectricCurrent")
                    .withType(channelTypeUID)
                    .withLabel("Total Forward Active Energy")
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "totalReverseActiveEnergyBalanced");
            var channelTypeUID = createChannelTypeUID(ENERGY_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:ElectricCurrent")
                    .withType(channelTypeUID)
                    .withLabel("Total Reversed Active Energy")
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "totalCost");
            var channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Currency")
                    .withType(channelTypeUID)
                    .withLabel("Total Cost")
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "pricePerUnit");
            var channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:EnergyPrice")
                    .withType(channelTypeUID)
                    .withLabel("Price Per Unit")
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "currency");
            var channelTypeUID = createChannelTypeUID(STRING_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "String")
                    .withType(channelTypeUID)
                    .withLabel("Currency")
                    .withDescription("Currency Code A https://www.nationsonline.org/oneworld/currencies.htm")
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "measuredValues");
            var channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number")
                    .withType(channelTypeUID)
                    .withLabel("Measured Value")
                    .withDescription("Number of measured values")
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "period");
            var channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID); // todo add time channel
            channels.add(ChannelBuilder.create(channelUid, "Number")
                    .withType(channelTypeUID)
                    .withLabel("Period")
                    .withDescription("Approximate period between measurements in seconds")
                    .build());
        }

        channels.addAll(buildChannelsForPhase(groupUid, 1));
        channels.addAll(buildChannelsForPhase(groupUid, 2));
        channels.addAll(buildChannelsForPhase(groupUid, 3));

        return channels.stream();
    }

    private List<Channel> buildChannelsForPhase(ChannelGroupUID groupUid, int phaseNumber) {
        val channels = new ArrayList<Channel>();
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalForwardActiveEnergy");
            var channelTypeUID = createChannelTypeUID(ENERGY_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Energy")
                    .withType(channelTypeUID)
                    .withLabel("Total Forward Active Energy (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalReverseActiveEnergy");
            var channelTypeUID = createChannelTypeUID(ENERGY_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Energy")
                    .withType(channelTypeUID)
                    .withLabel("Total Reversed Active Energy (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalForwardReactiveEnergy");
            var channelTypeUID = createChannelTypeUID(ENERGY_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Energy")
                    .withType(channelTypeUID)
                    .withLabel("Total Forward Reactive Energy (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "totalReverseReactiveEnergy");
            var channelTypeUID = createChannelTypeUID(ENERGY_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Energy")
                    .withType(channelTypeUID)
                    .withLabel("Total Reversed Reactive Energy (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "voltage");
            var channelTypeUID = createChannelTypeUID(VOLTAGE_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:ElectricPotential")
                    .withType(channelTypeUID)
                    .withLabel("Voltage (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "current");
            var channelTypeUID = createChannelTypeUID(CURRENT_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:ElectricCurrent")
                    .withType(channelTypeUID)
                    .withLabel("Current (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerActive");
            var channelTypeUID = createChannelTypeUID(POWER_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Power")
                    .withType(channelTypeUID)
                    .withLabel("Power Active (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerReactive");
            var channelTypeUID = createChannelTypeUID(POWER_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Power")
                    .withType(channelTypeUID)
                    .withLabel("Power Reactive (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerApparent");
            var channelTypeUID = createChannelTypeUID(POWER_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Power")
                    .withType(channelTypeUID)
                    .withLabel("Power Apparent (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "powerFactor");
            var channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number")
                    .withType(channelTypeUID)
                    .withLabel("Power Factor (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "phaseAngle");
            var channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Angle")
                    .withType(channelTypeUID)
                    .withLabel("Phase Angle (Phase #%s)".formatted(phaseNumber))
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "phase-" + phaseNumber + "-" + "frequency");
            var channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Frequency")
                    .withType(channelTypeUID)
                    .withLabel("Frequency")
                    .build());
        }
        return channels;
    }

    @Override
    public Stream<Channel> onHvacValue() {
        log.debug("{} {} onHvacValue", thingUID, deviceChannel);
        val groupUid = buildGroupUid();
        val channels = new ArrayList<Channel>();
        {
            val channelUid = new ChannelUID(groupUid, HVAC_ON);
            final ChannelTypeUID channelTypeUID = createChannelTypeUID(HVAC_WORKING_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Switch")
                    .withLabel("Working")
                    .withType(channelTypeUID)
                    .build());
        } // on
        {
            val channelUid = new ChannelUID(groupUid, HVAC_MODE);
            final ChannelTypeUID channelTypeUID = createChannelTypeUID(HVAC_MODE_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "String")
                    .withLabel("Mode")
                    .withType(channelTypeUID)
                    .build());
        } // mode
        {
            val channelUid = new ChannelUID(groupUid, HVAC_SET_POINT_TEMPERATURE_HEAT);
            final ChannelTypeUID channelTypeUID = createChannelTypeUID(HVAC_TEMPERATURE_HEAT_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, NUMBER_TEMPERATURE)
                    .withLabel("Set Point Temperature Heat")
                    .withType(channelTypeUID)
                    .build());
        } // setPointTemperatureHeat
        {
            val channelUid = new ChannelUID(groupUid, HVAC_SET_POINT_TEMPERATURE_COOL);
            final ChannelTypeUID channelTypeUID = createChannelTypeUID(HVAC_TEMPERATURE_COOL_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, NUMBER_TEMPERATURE)
                    .withLabel("Set Point Temperature Cool")
                    .withType(channelTypeUID)
                    .build());
        } // setPointTemperatureCool
        {
            val flags = stream(HvacFlag.values())
                    .map(flag -> buildHvacFlag(groupUid, flag))
                    .toList();
            channels.addAll(flags);
        } // flags
        return channels.stream();
    }

    @Override
    public Stream<Channel> onTimerValue() {
        // do not know what to do with this
        return Stream.empty();
    }

    @Override
    public Stream<Channel> onPressureValue() {
        log.debug("{} {} onPressureValue", thingUID, deviceChannel);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(PRESSURE_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, number("Pressure"))
                .withType(channelTypeUID)
                .withLabel("Pressure")
                .build());
    }

    @Override
    public Stream<Channel> onRainValue() {
        log.debug("{} {} onRainValue", thingUID, deviceChannel);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(RAIN_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, number("Length"))
                .withType(channelTypeUID)
                .withLabel("Rain")
                .build());
    }

    @Override
    public Stream<Channel> onWeightValue() {
        log.debug("{} {} onWeightValue", thingUID, deviceChannel);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(WEIGHT_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, number("Mass"))
                .withType(channelTypeUID)
                .withLabel("Weight")
                .build());
    }

    @Override
    public Stream<Channel> onWindValue() {
        log.debug("{} {} onPressureValue", thingUID, deviceChannel);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(WIND_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, number("Speed"))
                .withType(channelTypeUID)
                .withLabel("Pressure")
                .build());
    }

    @Override
    public Stream<Channel> onHeatpolThermostatValue() {
        var groupUid = buildGroupUid();
        var flagsStream = stream(ThermostatValueFlag.values()).map(flag -> buildThermostatValueFlag(groupUid, flag));
        var basicStream = Stream.of(
                ChannelBuilder.create(new ChannelUID(groupUid, "on"), "Switch")
                        .withLabel("On")
                        .withType(createChannelTypeUID(SWITCH_CHANNEL_RO_ID))
                        .withDefaultTags(Set.of("Switch"))
                        .build(),
                ChannelBuilder.create(new ChannelUID(groupUid, "measuredTemperature"), NUMBER_TEMPERATURE)
                        .withLabel("Measured Temperature")
                        .withType(createChannelTypeUID(TEMPERATURE_CHANNEL_ID))
                        .withDefaultTags(Set.of("Temperature", "Measurement"))
                        .build(),
                ChannelBuilder.create(new ChannelUID(groupUid, "presetTemperature"), NUMBER_TEMPERATURE)
                        .withLabel("Preset Temperature")
                        .withType(createChannelTypeUID(TEMPERATURE_CHANNEL_ID))
                        .withDefaultTags(Set.of("Temperature", "Setpoint"))
                        .build());
        return Stream.concat(flagsStream, basicStream);
    }

    private Channel buildThermostatValueFlag(ChannelGroupUID groupUid, ThermostatValueFlag flag) {
        val channelUid = new ChannelUID(groupUid, "flag-" + flag);
        val channelTypeUID = createChannelTypeUID(FLAG_CHANNEL_ID);
        return ChannelBuilder.create(channelUid, "Switch")
                .withLabel("Flag \"%s\"".formatted(flag))
                .withType(channelTypeUID)
                .build();
    }

    private Channel buildHvacFlag(ChannelGroupUID groupUid, @MonotonicNonNull HvacFlag flag) {
        val channelUid = new ChannelUID(groupUid, "flag-" + flag);
        val channelTypeUID = createChannelTypeUID(FLAG_CHANNEL_ID);
        return ChannelBuilder.create(channelUid, "Switch")
                .withLabel("Flag \"%s\"".formatted(flag))
                .withType(channelTypeUID)
                .build();
    }

    @Override
    public Stream<Channel> onActionTrigger() {
        var channelUid = createChannelUid();
        var channelTypeUID = createChannelTypeUID(ACTION_TRIGGER_ID);

        return Stream.of(ChannelBuilder.create(channelUid)
                .withType(channelTypeUID)
                .withLabel("Action Trigger")
                .withKind(ChannelKind.TRIGGER)
                .build());
    }

    @Override
    public Stream<Channel> onUnknownValue() {
        log.debug("{} {} onUnknownValue", thingUID, deviceChannel);
        val channelUid = createChannelUid();
        val channelTypeUID = createChannelTypeUID(UNKNOWN_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, "String")
                .withType(channelTypeUID)
                .withLabel("Unknown")
                .build());
    }

    private @NonNull ChannelGroupUID buildGroupUid() {
        return new ChannelGroupUID(thingUID, valueOf(deviceChannel.number()));
    }
}
