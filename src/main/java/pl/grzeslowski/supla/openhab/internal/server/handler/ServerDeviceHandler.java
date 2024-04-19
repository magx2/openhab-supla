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
package pl.grzeslowski.supla.openhab.internal.server.handler;

import static java.lang.String.valueOf;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Objects.requireNonNull;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.BRIDGE_UNINITIALIZED;
import static org.openhab.core.thing.ThingStatusDetail.NONE;
import static reactor.core.publisher.Flux.just;

import java.util.*;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.javatuples.Pair;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.jsupla.protocoljava.api.channels.values.*;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.DeviceChannel;
import pl.grzeslowski.jsupla.protocoljava.api.entities.ds.DeviceChannels;
import pl.grzeslowski.jsupla.protocoljava.api.entities.sd.ChannelNewValue;
import pl.grzeslowski.supla.openhab.internal.handler.AbstractDeviceHandler;
import pl.grzeslowski.supla.openhab.internal.server.ChannelCallback;
import pl.grzeslowski.supla.openhab.internal.server.ChannelValueToState;
import pl.grzeslowski.supla.openhab.internal.server.SuplaDeviceRegistry;
import reactor.core.Disposable;

/**
 * The {@link ServerDeviceHandler} is responsible for handling commands, which are sent to one of the channels.
 *
 * @author Grzeslowski - Initial contribution
 */
@NonNullByDefault
public class ServerDeviceHandler extends AbstractDeviceHandler {
    private final Logger logger = LoggerFactory.getLogger(ServerDeviceHandler.class);

    private pl.grzeslowski.jsupla.server.api.@Nullable Channel suplaChannel;
    private final Object channelLock = new Object();

    private final Map<ChannelUID, Integer> channelUIDS = new HashMap<>();
    private final ChannelValueSwitch<State> valueSwitch = new ChannelValueSwitch<>(new ChannelValueToState());

    @Nullable
    private Disposable subscription;

    private final SuplaDeviceRegistry suplaDeviceRegistry;

    public ServerDeviceHandler(Thing thing, SuplaDeviceRegistry suplaDeviceRegistry) {
        super(thing);
        this.suplaDeviceRegistry = suplaDeviceRegistry;
    }

    @Override
    protected void internalInitialize() {
        if (getBridge() == null) {
            logger.debug("No bridge for thing with UID {}", thing.getUID());
            updateStatus(
                    OFFLINE, BRIDGE_UNINITIALIZED, "There is no bridge for this thing. Remove it and add it again.");
            return;
        }

        synchronized (channelLock) {
            if (suplaChannel == null) {
                updateStatus(OFFLINE, NONE, "Channel in server is not yet opened");
            } else {
                updateStatus(ThingStatus.ONLINE);
            }
        }

        suplaDeviceRegistry.addSuplaDevice(this);
    }

    private void sendCommandToSuplaServer(ChannelUID channelUID, ChannelValue channelValue, Command command) {
        var channelNumber = channelUIDS.get(channelUID);
        if (channelNumber == null) {
            logger.debug("There is no channel number for channelUID={}", channelUID);
            return;
        }
        var channelNewValue = new ChannelNewValue(1, channelNumber, 100, channelValue);
        subscription = requireNonNull(suplaChannel)
                .write(just(channelNewValue))
                .subscribe(
                        date -> logger.debug(
                                "Changed value of channel for {} command {}, {}",
                                channelUID,
                                command,
                                date.format(ISO_DATE_TIME)),
                        ex -> logger.debug(
                                "Couldn't Change value of channel for {} command {}", channelUID, command, ex));
    }

    @Override
    protected void handleRefreshCommand(final ChannelUID channelUID) {
        // TODO handle this command
    }

    @Override
    protected void handleOnOffCommand(final ChannelUID channelUID, final OnOffType command) {
        final OnOff toSend;
        if (command == OnOffType.ON) {
            toSend = OnOff.ON;
        } else {
            toSend = OnOff.OFF;
        }
        sendCommandToSuplaServer(channelUID, toSend, command);
    }

    @Override
    protected void handleUpDownCommand(final ChannelUID channelUID, final UpDownType command) {
        // TODO handle this command
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void handleHsbCommand(final ChannelUID channelUID, final HSBType command) {
        final RgbValue toSend = new RgbValue(
                command.getBrightness().intValue(),
                255, // TODO I don't know if this is
                // correct
                command.getRed().intValue(),
                command.getGreen().intValue(),
                command.getBlue().intValue());
        sendCommandToSuplaServer(channelUID, toSend, command);
    }

    @Override
    protected void handleOpenClosedCommand(final ChannelUID channelUID, final OpenClosedType command) {
        final OnOff toSend;
        if (command == OpenClosedType.OPEN) {
            toSend = OnOff.ON;
        } else {
            toSend = OnOff.OFF;
        }
        sendCommandToSuplaServer(channelUID, toSend, command);
    }

    @Override
    protected void handlePercentCommand(final ChannelUID channelUID, final PercentType command) {
        sendCommandToSuplaServer(channelUID, new PercentValue(command.intValue()), command);
    }

    @Override
    protected void handleDecimalCommand(final ChannelUID channelUID, final DecimalType command) {
        sendCommandToSuplaServer(channelUID, new DecimalValue(command.toBigDecimal()), command);
    }

    @Override
    protected void handleStopMoveTypeCommand(final @NonNull ChannelUID channelUID, final @NonNull StopMoveType command)
            throws Exception {
        logger.warn(
                "Not handling `{}` ({}) on channel `{}`",
                command,
                command.getClass().getSimpleName(),
                channelUID);
    }

    @Override
    protected void handleStringCommand(final ChannelUID channelUID, final StringType command) throws Exception {
        logger.warn(
                "Not handling `{}` ({}) on channel `{}`",
                command,
                command.getClass().getSimpleName(),
                channelUID);
    }

    public void setSuplaChannel(final pl.grzeslowski.jsupla.server.api.Channel suplaChannel) {
        synchronized (channelLock) {
            this.suplaChannel = suplaChannel;
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @SuppressWarnings("deprecation")
    public void setChannels(final DeviceChannels deviceChannels) {
        logger.debug("Registering channels {}", deviceChannels);
        var channels = deviceChannels.getChannels().stream()
                .sorted(Comparator.comparingInt(DeviceChannel::getNumber))
                .map(this::createChannel)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        updateChannels(channels);
        deviceChannels.getChannels().stream()
                .map(this::channelForUpdate)
                .forEach(pair -> updateState(pair.getValue0(), pair.getValue1()));
    }

    @SuppressWarnings("deprecation")
    private Pair<ChannelUID, State> channelForUpdate(final DeviceChannel deviceChannel) {
        return Pair.with(createChannelUid(deviceChannel.getNumber()), findState(deviceChannel.getValue()));
    }

    private ChannelUID createChannelUid(final int channelNumber) {
        return new ChannelUID(getThing().getUID(), valueOf(channelNumber));
    }

    private State findState(ChannelValue value) {
        return valueSwitch.doSwitch(value);
    }

    public void updateStatus(final int channelNumber, final ChannelValue channelValue) {
        var channelUid = createChannelUid(channelNumber);
        var state = findState(channelValue);
        updateState(channelUid, state);
    }

    @SuppressWarnings("deprecation")
    private Optional<Channel> createChannel(final DeviceChannel deviceChannel) {
        var channelCallback = new ChannelCallback(getThing().getUID(), deviceChannel.getNumber());
        var channelValueSwitch = new ChannelValueSwitch<>(channelCallback);
        var channel = channelValueSwitch.doSwitch(deviceChannel.getValue());
        if (channel != null) {
            channelUIDS.put(channel.getUID(), deviceChannel.getNumber());
        }
        return Optional.ofNullable(channel);
    }

    private void updateChannels(final List<Channel> channels) {
        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(channels);
        updateThing(thingBuilder.build());
    }

    /** Only to change visibility from protected to public */
    @Override
    public void updateStatus(
            final ThingStatus status, final ThingStatusDetail statusDetail, @Nullable final String description) {
        super.updateStatus(status, statusDetail, description);
    }

    @Override
    public String toString() {
        return valueOf(getThing());
    }

    @Override
    public void dispose() {
        {
            var local = subscription;
            subscription = null;
            if (local != null) {
                local.dispose();
            }
        }
        suplaDeviceRegistry.removeSuplaDevice(this);
        super.dispose();
    }
}
