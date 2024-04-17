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
package pl.grzeslowski.supla.openhab.internal.handler;

import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatusDetail.CONFIGURATION_ERROR;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author Grzeslowski - Initial contribution */
@NonNullByDefault
public abstract class AbstractDeviceHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(AbstractDeviceHandler.class);

    public AbstractDeviceHandler(final Thing thing) {
        super(thing);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void initialize() {
        try {
            internalInitialize();
        } catch (Exception e) {
            logger.error("Error occurred while initializing Supla device!", e);
            updateStatus(
                    OFFLINE,
                    CONFIGURATION_ERROR,
                    "Error occurred while initializing Supla device! " + e.getLocalizedMessage());
        }
    }

    protected abstract void internalInitialize() throws Exception;

    @Override
    public final void handleCommand(final ChannelUID channelUID, final Command command) {
        try {
            if (command instanceof RefreshType) {
                handleRefreshCommand(channelUID);
            } else if (command instanceof OnOffType) {
                handleOnOffCommand(channelUID, (OnOffType) command);
            } else if (command instanceof UpDownType) {
                handleUpDownCommand(channelUID, (UpDownType) command);
            } else if (command instanceof HSBType) {
                handleHsbCommand(channelUID, (HSBType) command);
            } else if (command instanceof OpenClosedType) {
                handleOpenClosedCommand(channelUID, (OpenClosedType) command);
            } else if (command instanceof PercentType) {
                handlePercentCommand(channelUID, (PercentType) command);
            } else if (command instanceof DecimalType) {
                handleDecimalCommand(channelUID, (DecimalType) command);
            } else if (command instanceof StopMoveType) {
                handleStopMoveTypeCommand(channelUID, (StopMoveType) command);
            } else if (command instanceof StringType) {
                handleStringCommand(channelUID, (StringType) command);
            } else {
                logger.warn(
                        "Does not know how to handle command `{}` ({}) on channel `{}`!",
                        command,
                        command.getClass().getSimpleName(),
                        channelUID);
            }
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Error occurred while handling command `" + command + "` ("
                            + command.getClass().getSimpleName() + ") " + //
                            "on channel `"
                            + channelUID + "`!",
                    ex);
        }
    }

    protected abstract void handleRefreshCommand(final ChannelUID channelUID) throws Exception;

    protected abstract void handleOnOffCommand(final ChannelUID channelUID, final OnOffType command) throws Exception;

    protected abstract void handleUpDownCommand(final ChannelUID channelUID, final UpDownType command) throws Exception;

    protected abstract void handleHsbCommand(final ChannelUID channelUID, final HSBType command) throws Exception;

    protected abstract void handleOpenClosedCommand(final ChannelUID channelUID, final OpenClosedType command)
            throws Exception;

    protected abstract void handlePercentCommand(final ChannelUID channelUID, final PercentType command)
            throws Exception;

    protected abstract void handleDecimalCommand(final ChannelUID channelUID, final DecimalType command)
            throws Exception;

    protected abstract void handleStopMoveTypeCommand(final ChannelUID channelUID, final StopMoveType command)
            throws Exception;

    protected abstract void handleStringCommand(final ChannelUID channelUID, final StringType command) throws Exception;
}
