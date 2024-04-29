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

import static org.openhab.core.types.UnDefType.NULL;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.*;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.*;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public class ChannelValueToState implements ChannelValueSwitch.Callback<State> {
    @Override
    public State onDecimalValue(@Nullable final DecimalValue decimalValue) {
        if (decimalValue == null) {
            return NULL;
        }
        return new DecimalType(decimalValue.getValue());
    }

    @Override
    public State onOnOff(@Nullable final OnOff onOff) {
        if (onOff == null) {
            return NULL;
        }
        return switch (onOff) {
            case ON -> OnOffType.ON;
            case OFF -> OnOffType.OFF;
        };
    }

    @Override
    public State onOpenClose(@Nullable final OpenClose openClose) {
        if (openClose == null) {
            return NULL;
        }
        return switch (openClose) {
            case OPEN -> OpenClosedType.OPEN;
            case CLOSE -> OpenClosedType.CLOSED;
        };
    }

    @Override
    public State onPercentValue(@Nullable final PercentValue percentValue) {
        if (percentValue == null) {
            return NULL;
        }
        return new PercentType(percentValue.getValue());
    }

    @Override
    public State onRgbValue(@Nullable final RgbValue rgbValue) {
        if (rgbValue == null) {
            return NULL;
        }
        return HSBType.fromRGB(rgbValue.getRed(), rgbValue.getGreen(), rgbValue.getBlue());
    }

    @Override
    public State onStoppableOpenClose(@Nullable final StoppableOpenClose stoppableOpenClose) {
        if (stoppableOpenClose == null) {
            return NULL;
        }
        return switch (stoppableOpenClose) {
            case OPEN -> OpenClosedType.OPEN;
            case CLOSE, STOP -> OpenClosedType.CLOSED;
        };
    }

    @Override
    public State onTemperatureValue(@Nullable final TemperatureValue temperatureValue) {
        if (temperatureValue == null) {
            return NULL;
        }
        return new DecimalType(temperatureValue.getTemperature());
    }

    @Override
    public State onTemperatureAndHumidityValue(
            @Nullable final TemperatureAndHumidityValue temperatureAndHumidityValue) {
        if (temperatureAndHumidityValue == null) {
            return NULL;
        }
        return new DecimalType(temperatureAndHumidityValue.getTemperature()); // TODO support humidity also
    }

    @Override
    public State onUnknownValue(@Nullable final UnknownValue unknownValue) {
        if (unknownValue == null) {
            return NULL;
        }
        return StringType.valueOf(unknownValue.getMessage());
    }
}
