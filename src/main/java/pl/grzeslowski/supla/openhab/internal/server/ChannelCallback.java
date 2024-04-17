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
import static java.util.Objects.requireNonNull;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.Channels.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.*;
import pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public class ChannelCallback implements ChannelValueSwitch.Callback<@Nullable Channel> {
    private final ThingUID thingUID;
    private final int number;

    public ChannelCallback(final ThingUID thingUID, final int number) {
        this.thingUID = requireNonNull(thingUID);
        this.number = number;
    }

    private ChannelUID createChannelUid() {
        return new ChannelUID(thingUID, valueOf(number));
    }

    private ChannelTypeUID createChannelTypeUID(String id) {
        return new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, id);
    }

    @Override
    @Nullable
    public Channel onDecimalValue(@Nullable final DecimalValue decimalValue) {
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(DECIMAL_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                .withType(channelTypeUID)
                .build();
    }

    @Override
    @Nullable
    public Channel onOnOff(@Nullable final OnOff onOff) {
        return switchChannel();
    }

    @Override
    @Nullable
    public Channel onOpenClose(@Nullable final OpenClose openClose) {
        return switchChannel();
    }

    private Channel switchChannel() {
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(SWITCH_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, "Switch")
                .withType(channelTypeUID)
                .build();
    }

    @Override
    @Nullable
    public Channel onPercentValue(@Nullable final PercentValue percentValue) {
        return null;
    }

    @Override
    @Nullable
    public Channel onRgbValue(@Nullable final RgbValue rgbValue) {
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(RGB_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                .withType(channelTypeUID)
                .build();
    }

    @Override
    @Nullable
    public Channel onStoppableOpenClose(@Nullable final StoppableOpenClose stoppableOpenClose) {
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(ROLLER_SHUTTER_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, "Rollershutter")
                .withType(channelTypeUID)
                .build();
    }

    @Override
    @Nullable
    public Channel onTemperatureValue(@Nullable final TemperatureValue temperatureValue) {
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(TEMPERATURE_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                .withType(channelTypeUID)
                .build();
    }

    @Override
    @Nullable
    public Channel onTemperatureAndHumidityValue(
            @Nullable final TemperatureAndHumidityValue temperatureAndHumidityValue) {
        // final ChannelUID channelUid = createChannelUid();
        // final ChannelTypeUID channelTypeUID = createChannelTypeUID(TEMPERATURE_AND_HUMIDITY_CHANNEL_ID);
        //
        // return ChannelBuilder.create(channelUid, null) // TODO should it be null?
        // .withType(channelTypeUID)
        // .build();
        return null; // TODO support returning 2 channels: temp and humidity
    }

    @Override
    @Nullable
    public Channel onUnknownValue(@Nullable final UnknownValue unknownValue) {
        final ChannelUID channelUid = createChannelUid();
        final ChannelTypeUID channelTypeUID = createChannelTypeUID(UNKNOWN_CHANNEL_ID);

        return ChannelBuilder.create(channelUid, null) // TODO should it be null?
                .withType(channelTypeUID)
                .build();
    }
}
