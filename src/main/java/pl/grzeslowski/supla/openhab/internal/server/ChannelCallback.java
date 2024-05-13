/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * <p>See the NOTICE file(s) distributed with this work for additional information.
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package pl.grzeslowski.supla.openhab.internal.server;

import static java.lang.String.valueOf;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.Channels.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.ChannelClassSwitch;
import pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
@RequiredArgsConstructor
@Slf4j
public class ChannelCallback implements ChannelClassSwitch.Callback<Stream<Channel>> {
    private final ThingUID thingUID;
    private final int number;

    private ChannelUID createChannelUid() {
        return new ChannelUID(thingUID, valueOf(number));
    }

    private ChannelTypeUID createChannelTypeUID(String id) {
        return new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, id);
    }

    @Override
    @Nullable
    public Stream<Channel> onDecimalValue() {
        log.debug("{} {} onDecimalValue", thingUID, number);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, "Number")
                .withType(channelTypeUID)
                .build());
    }

    @Override
    @Nullable
    public Stream<Channel> onOnOff() {
        log.debug("{} {} onOnOff", thingUID, number);
        return switchChannel();
    }

    @Override
    @Nullable
    public Stream<Channel> onOpenClose() {
        log.debug("{} {} onOpenClose", thingUID, number);
        return switchChannel();
    }

    private Stream<Channel> switchChannel() {
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(SWITCH_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, "Switch")
                .withType(channelTypeUID)
                .build());
    }

    @Override
    @Nullable
    public Stream<Channel> onPercentValue() {
        log.debug("{} {} onPercentValue", thingUID, number);
        return null;
    }

    @Override
    @Nullable
    public Stream<Channel> onRgbValue() {
        log.debug("{} {} onRgbValue", thingUID, number);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(RGB_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, "String") // TODO what type?
                .withType(channelTypeUID)
                .build());
    }

    @Override
    @Nullable
    public Stream<Channel> onStoppableOpenClose() {
        log.debug("{} {} onStoppableOpenClose", thingUID, number);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(ROLLER_SHUTTER_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, "Rollershutter")
                .withType(channelTypeUID)
                .build());
    }

    @Override
    @Nullable
    public Stream<Channel> onTemperatureValue() {
        log.debug("{} {} onTemperatureValue", thingUID, number);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(TEMPERATURE_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, "Number:Temperature")
                .withType(channelTypeUID)
                .build());
    }

    @Override
    @Nullable
    public Stream<Channel> onTemperatureAndHumidityValue() {
        log.debug("{} {} onTemperatureAndHumidityValue", thingUID, number);
        val groupUid = new ChannelGroupUID(thingUID, valueOf(number));
        val channels = new ArrayList<Channel>();
        {
            val channelUid = new ChannelUID(groupUid, "temperature");
            val channelTypeUID = createChannelTypeUID(TEMPERATURE_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Temperature")
                    .withType(channelTypeUID)
                    .build());
        }
        {
            val channelUid = new ChannelUID(groupUid, "humidity");
            val channelTypeUID = createChannelTypeUID(HUMIDITY_CHANNEL_ID);
            channels.add(ChannelBuilder.create(channelUid, "Number:Dimensionless")
                    .withType(channelTypeUID)
                    .build());
        }
        return channels.stream();
    }

    @Override
    public Stream<Channel> onElectricityMeter() {
        log.debug("{} {} onElectricityMeter", thingUID, number);
        val groupUid = new ChannelGroupUID(thingUID, valueOf(number));
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
    @Nullable
    public Stream<Channel> onUnknownValue() {
        log.debug("{} {} onUnknownValue", thingUID, number);
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(UNKNOWN_CHANNEL_ID);

        return Stream.of(ChannelBuilder.create(channelUid, "String")
                .withType(channelTypeUID)
                .build());
    }
}
