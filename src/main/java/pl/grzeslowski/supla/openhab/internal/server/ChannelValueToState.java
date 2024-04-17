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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.*;
import org.openhab.core.types.State;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.*;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public class ChannelValueToState implements ChannelValueSwitch.Callback<State> {
    @Override
    public State onDecimalValue(@Nullable final DecimalValue decimalValue) {
        Number value;
        if (decimalValue == null) {
            value = 0;
        } else {
            value = decimalValue.value;
        }
        return new DecimalType(value);
    }

    @Override
    public State onOnOff(@Nullable final OnOff onOff) {
        if (onOff == OnOff.ON) {
            return OnOffType.ON;
        } else {
            return OnOffType.OFF;
        }
    }

    @Override
    public State onOpenClose(@Nullable final OpenClose openClose) {
        if (openClose == OpenClose.OPEN) {
            return OpenClosedType.OPEN;
        } else {
            return OpenClosedType.CLOSED;
        }
    }

    @Override
    public State onPercentValue(@Nullable final PercentValue percentValue) {
        if (percentValue == null) {
            return PercentType.ZERO;
        }
        return new PercentType(percentValue.getValue());
    }

    @Override
    public State onRgbValue(@Nullable final RgbValue rgbValue) {
        if (rgbValue == null) {
            return HSBType.fromRGB(0, 0, 0);
        }
        return HSBType.fromRGB(rgbValue.red, rgbValue.green, rgbValue.blue);
    }

    @Override
    public State onStoppableOpenClose(@Nullable final StoppableOpenClose stoppableOpenClose) {
        if (stoppableOpenClose == StoppableOpenClose.OPEN) {
            return OpenClosedType.OPEN;
        } else {
            return OpenClosedType.CLOSED;
        }
    }

    @Override
    public State onTemperatureValue(@Nullable final TemperatureValue temperatureValue) {
        Number temperature;
        if (temperatureValue == null) {
            temperature = 0;
        } else {
            temperature = temperatureValue.temperature;
        }
        return new DecimalType(temperature);
    }

    @Override
    public State onTemperatureAndHumidityValue(
            @Nullable final TemperatureAndHumidityValue temperatureAndHumidityValue) {
        Number temperature;
        if (temperatureAndHumidityValue == null) {
            temperature = 0;
        } else {
            temperature = temperatureAndHumidityValue.temperature;
        }
        return new DecimalType(temperature); // TODO support humidity also
    }

    @Override
    public State onUnknownValue(@Nullable final UnknownValue unknownValue) {
        String message;
        if (unknownValue == null) {
            message = "null";
        } else {
            message = unknownValue.message;
        }
        return StringType.valueOf(message);
    }
}
