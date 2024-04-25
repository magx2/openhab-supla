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
package pl.grzeslowski.supla.openhab.internal.cloud.handler;

import static io.swagger.client.model.ChannelFunctionActionEnum.*;
import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openhab.core.library.types.OnOffType.ON;
import static org.openhab.core.library.types.UpDownType.UP;
import static org.openhab.core.thing.ThingStatus.OFFLINE;
import static org.openhab.core.thing.ThingStatus.ONLINE;
import static org.openhab.core.thing.ThingStatusDetail.*;
import static org.openhab.core.types.RefreshType.REFRESH;
import static pl.grzeslowski.supla.openhab.internal.SuplaBindingConstants.SUPLA_DEVICE_CLOUD_ID;
import static pl.grzeslowski.supla.openhab.internal.cloud.ChannelFunctionDispatcher.DISPATCHER;

import io.swagger.client.model.ChannelExecuteActionRequest;
import io.swagger.client.model.ChannelFunctionActionEnum;
import io.swagger.client.model.Device;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.supla.openhab.internal.cloud.AdditionalChannelType;
import pl.grzeslowski.supla.openhab.internal.cloud.ChannelInfo;
import pl.grzeslowski.supla.openhab.internal.cloud.ChannelInfoParser;
import pl.grzeslowski.supla.openhab.internal.cloud.api.ChannelsCloudApi;
import pl.grzeslowski.supla.openhab.internal.cloud.api.IoDevicesCloudApi;
import pl.grzeslowski.supla.openhab.internal.cloud.executors.LedCommandExecutor;
import pl.grzeslowski.supla.openhab.internal.cloud.executors.LedCommandExecutorFactory;
import pl.grzeslowski.supla.openhab.internal.cloud.executors.SuplaLedCommandExecutorFactory;
import pl.grzeslowski.supla.openhab.internal.cloud.functionswitch.CreateChannelFunctionSwitch;
import pl.grzeslowski.supla.openhab.internal.cloud.functionswitch.FindStateFunctionSwitch;
import pl.grzeslowski.supla.openhab.internal.handler.AbstractDeviceHandler;

/**
 * This is handler for all Supla devices.
 *
 * <p>Channels are created at runtime after connecting to Supla Cloud
 *
 * @author Martin Grześlowski - initial contributor
 */
@NonNullByDefault
public final class CloudDeviceHandler extends AbstractDeviceHandler {
    private final Logger logger = LoggerFactory.getLogger(CloudDeviceHandler.class);
    private final LedCommandExecutorFactory ledCommandExecutorFactory;

    @Nullable
    private ChannelsCloudApi channelsApi;

    private int cloudId;

    @Nullable
    private IoDevicesCloudApi ioDevicesApi;

    // CommandExecutors
    @Nullable
    private LedCommandExecutor ledCommandExecutor;

    CloudDeviceHandler(Thing thing, LedCommandExecutorFactory ledCommandExecutorFactory) {
        super(thing);
        this.ledCommandExecutorFactory = ledCommandExecutorFactory;
    }

    public CloudDeviceHandler(final Thing thing) {
        this(thing, SuplaLedCommandExecutorFactory.FACTORY);
    }

    @Override
    protected void internalInitialize() throws Exception {
        @Nullable final Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("No bridge for thing with UID {}", thing.getUID());
            updateStatus(
                    OFFLINE, BRIDGE_UNINITIALIZED, "There is no bridge for this thing. Remove it and add it again.");
            return;
        }
        final @Nullable BridgeHandler bridgeHandler = bridge.getHandler();
        if (!(bridgeHandler instanceof CloudBridgeHandler handler)) {
            logger.debug(
                    "Bridge is not instance of {}! Current bridge class {}, Thing UID {}",
                    CloudBridgeHandler.class.getSimpleName(),
                    bridgeHandler != null ? bridgeHandler.getClass().getSimpleName() : "<null>",
                    thing.getUID());
            updateStatus(OFFLINE, BRIDGE_UNINITIALIZED, "There is wrong type of bridge for cloud device!");
            return;
        }
        initApi(handler);

        if (!initCloudApi()) {
            return;
        }

        if (!checkIfIsOnline()) {
            return;
        }

        if (!checkIfIsEnabled()) {
            return;
        }

        initChannels();
        initCommandExecutors();

        // done
        updateStatus(ONLINE);
    }

    private boolean initCloudApi() {
        final String cloudIdString = String.valueOf(getConfig().get(SUPLA_DEVICE_CLOUD_ID));
        try {
            this.cloudId = parseInt(cloudIdString);
            return true;
        } catch (NumberFormatException e) {
            updateStatus(
                    OFFLINE,
                    CONFIGURATION_ERROR,
                    "Cannot parse cloud ID `" + cloudIdString + "` to integer! " + e.getLocalizedMessage());
            return false;
        }
    }

    private void initApi(final CloudBridgeHandler handler) {
        ioDevicesApi = handler;
        channelsApi = handler;
    }

    private boolean checkIfIsOnline() throws Exception {
        try {
            var device = findDevice(singletonList("connected"));
            if (device.isConnected() == null || !device.isConnected()) {
                updateStatus(OFFLINE, NONE, "This device is is not connected to Supla Cloud.");
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.debug("Error when loading IO device from Supla Cloud!", e);
            updateStatus(
                    OFFLINE,
                    COMMUNICATION_ERROR,
                    "Error when loading IO device from Supla Cloud! " + e.getLocalizedMessage());
            return false;
        }
    }

    private Device findDevice(List<String> include) throws Exception {
        return requireNonNull(ioDevicesApi).getIoDevice(cloudId, include);
    }

    private boolean checkIfIsEnabled() throws Exception {
        final Device device = findDevice(emptyList());
        if (device.isEnabled() == null || !device.isEnabled()) {
            updateStatus(OFFLINE, NONE, "This device is turned off in Supla Cloud.");
            return false;
        }

        return true;
    }

    private void initChannels() {
        try {
            final List<Channel> channels = findDevice(singletonList("channels")) //
                    .getChannels() //
                    .stream() //
                    .filter(channel -> !channel.isHidden()) //
                    .map(channel -> DISPATCHER.dispatch(channel, new CreateChannelFunctionSwitch(thing.getUID()))) //
                    .flatMap(List::stream) //
                    .collect(Collectors.toList());
            updateChannels(channels);
        } catch (Exception e) {
            updateStatus(
                    OFFLINE,
                    COMMUNICATION_ERROR,
                    "Error when loading IO device from Supla Cloud! " + e.getLocalizedMessage());
        }
    }

    private void initCommandExecutors() {
        ledCommandExecutor = ledCommandExecutorFactory.newLedCommandExecutor(requireNonNull(channelsApi));
    }

    private void updateChannels(final List<Channel> channels) {
        ThingBuilder thingBuilder = editThing();
        thingBuilder.withChannels(channels);
        updateThing(thingBuilder.build());
    }

    @Override
    protected void handleRefreshCommand(final ChannelUID channelUID) throws Exception {
        var channelInfo = ChannelInfoParser.PARSER.parse(channelUID);
        var channelId = channelInfo.getChannelId();
        logger.trace("Refreshing channel `{}`", channelUID);
        var channel = queryForChannel(channelId);
        var findStateFunctionSwitch = new FindStateFunctionSwitch(requireNonNull(ledCommandExecutor), channelUID);
        var foundState = DISPATCHER.dispatch(channel, findStateFunctionSwitch);
        if (foundState.isPresent()) {
            final State state = foundState.get();
            logger.trace("Updating state `{}` to `{}`", channelUID, state);
            updateState(channelUID, state);
        } else {
            logger.warn(
                    "There was no found state for channel `{}` channelState={}, function={}",
                    channelUID,
                    channel.getState(),
                    channel.getFunction());
        }
    }

    @Override
    protected void handleOnOffCommand(final ChannelUID channelUID, final OnOffType command) throws Exception {
        final ChannelInfo channelInfo = ChannelInfoParser.PARSER.parse(channelUID);
        final int channelId = channelInfo.getChannelId();
        final io.swagger.client.model.Channel channel = queryForChannel(channelId);
        switch (channel.getFunction().getName()) {
            case CONTROLLINGTHEGATE:
            case CONTROLLINGTHEGARAGEDOOR:
                var action = new ChannelExecuteActionRequest().action(OPEN_CLOSE);
                requireNonNull(channelsApi).executeAction(action, channelId);
                break;
            default:
                handleOneZeroCommand(channelId, command == ON, TURN_ON, TURN_OFF);
        }
    }

    @Override
    protected void handleUpDownCommand(final ChannelUID channelUID, final UpDownType command) throws Exception {
        final ChannelInfo channelInfo = ChannelInfoParser.PARSER.parse(channelUID);
        final int channelId = channelInfo.getChannelId();
        final io.swagger.client.model.Channel channel = queryForChannel(channelId);
        //noinspection SwitchStatementWithTooFewBranches
        switch (channel.getFunction().getName()) {
            case CONTROLLINGTHEROLLERSHUTTER:
                handleOneZeroCommand(channelId, command == UP, REVEAL, SHUT);
                final int value = command == UP ? 100 : 0;
                updateState(channelUID, new PercentType(value));
                break;
        }
    }

    @Override
    protected void handleHsbCommand(final ChannelUID channelUID, final HSBType command) throws Exception {
        final ChannelInfo channelInfo = ChannelInfoParser.PARSER.parse(channelUID);
        final int channelId = channelInfo.getChannelId();
        final io.swagger.client.model.Channel channel = queryForChannel(channelId);
        handleHsbCommand(channel, channelUID, command);
    }

    private void handleHsbCommand(
            final io.swagger.client.model.Channel channel, final ChannelUID channelUID, final HSBType command)
            throws Exception {
        switch (channel.getFunction().getName()) {
            case RGBLIGHTING:
            case DIMMERANDRGBLIGHTING:
                requireNonNull(ledCommandExecutor).changeColor(channel.getId(), command);
                return;
            default:
                logger.warn(
                        "Not handling `{}` ({}) on channel `{}`",
                        command,
                        command.getClass().getSimpleName(),
                        channelUID);
        }
    }

    @Override
    protected void handleOpenClosedCommand(final ChannelUID channelUID, final OpenClosedType command) throws Exception {
        final ChannelInfo channelInfo = ChannelInfoParser.PARSER.parse(channelUID);
        final int channelId = channelInfo.getChannelId();
        final io.swagger.client.model.Channel channel = queryForChannel(channelId);
        switch (channel.getFunction().getName()) {
            case CONTROLLINGTHEGATE:
            case CONTROLLINGTHEGARAGEDOOR:
                handleOneZeroCommand(channelId, command == OpenClosedType.OPEN, OPEN, CLOSE);
        }
    }

    @Override
    protected void handlePercentCommand(final ChannelUID channelUID, final PercentType command) throws Exception {
        if (channelsApi == null) {
            logger.debug("Cannot handle `{}` on channel `{}` because channelsApi is null", command, channelUID);
            return;
        }
        if (ledCommandExecutor == null) {
            logger.debug("Cannot handle `{}` on channel `{}` because ledCommandExecutor is null", command, channelUID);
            return;
        }
        final ChannelInfo channelInfo = ChannelInfoParser.PARSER.parse(channelUID);
        final int channelId = channelInfo.getChannelId();
        final io.swagger.client.model.Channel channel = queryForChannel(channelId);
        switch (channel.getFunction().getName()) {
            case CONTROLLINGTHEROLLERSHUTTER:
                final int shut = 100 - command.intValue();
                logger.debug("Channel `{}` is roller shutter; setting shut={}%", channelUID, shut);
                final ChannelExecuteActionRequest action = new ChannelExecuteActionRequest()
                        .action(REVEAL_PARTIALLY)
                        .percentage(shut);
                channelsApi.executeAction(action, channelId);
                return;
            case RGBLIGHTING:
            case DIMMERANDRGBLIGHTING:
                if (channelInfo.getAdditionalChannelType() == null) {
                    ledCommandExecutor.changeColorBrightness(channelId, command);
                } else if (channelInfo.getAdditionalChannelType() == AdditionalChannelType.LED_BRIGHTNESS) {
                    ledCommandExecutor.changeBrightness(channelId, command);
                }
                return;
            case DIMMER:
                ledCommandExecutor.changeBrightness(channelId, command);
                break;
            default:
                logger.warn(
                        "Not handling `{}` ({}) on channel `{}`",
                        command,
                        command.getClass().getSimpleName(),
                        channelUID);
        }
    }

    @Override
    protected void handleDecimalCommand(final ChannelUID channelUID, final DecimalType command) {
        // TODO handle this command
        logger.warn(
                "Not handling `{}` ({}) on channel `{}`",
                command,
                command.getClass().getSimpleName(),
                channelUID);
    }

    private void handleOneZeroCommand(
            final int channelId,
            final boolean firstOrSecond,
            final ChannelFunctionActionEnum first,
            final ChannelFunctionActionEnum second)
            throws Exception {
        if (channelsApi == null) {
            logger.debug("Cannot handle `{}` on channel `{}` because channelsApi is null", firstOrSecond, channelId);
            return;
        }
        final ChannelFunctionActionEnum action = firstOrSecond ? first : second;
        logger.trace("Executing 0/1 command `{}`", action);
        channelsApi.executeAction(new ChannelExecuteActionRequest().action(action), channelId);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    protected void handleStopMoveTypeCommand(final @NonNull ChannelUID channelUID, final @NonNull StopMoveType command)
            throws Exception {
        final ChannelInfo channelInfo = ChannelInfoParser.PARSER.parse(channelUID);
        final int channelId = channelInfo.getChannelId();
        final io.swagger.client.model.Channel channel = queryForChannel(channelId);
        switch (channel.getFunction().getName()) {
            case CONTROLLINGTHEROLLERSHUTTER:
                handleStopMoveTypeCommandOnRollerShutter(channelUID, channel, command);
                return;
            default:
                logger.warn(
                        "Not handling `{}` ({}) on channel `{}`",
                        command,
                        command.getClass().getSimpleName(),
                        channelUID);
        }
    }

    private void handleStopMoveTypeCommandOnRollerShutter(
            final ChannelUID channelUID, final io.swagger.client.model.Channel channel, final StopMoveType command)
            throws Exception {
        if (channelsApi == null) {
            logger.debug("Cannot handle `{}` on channel `{}` because channelsApi is null", command, channelUID);
            return;
        }
        switch (command) {
            case MOVE:
                logger.trace(
                        "Do not know how to handle command `{}` on roller shutter with id `{}`", command, channelUID);
                return;
            case STOP:
                final ChannelFunctionActionEnum action = ChannelFunctionActionEnum.STOP;
                logger.trace("Sending stop action `{}` to channel with UUID `{}`", action, channelUID);
                channelsApi.executeAction(new ChannelExecuteActionRequest().action(action), channel.getId());
        }
    }

    @Override
    protected void handleStringCommand(final ChannelUID channelUID, final StringType command) throws Exception {
        logger.warn(
                "Not handling `{}` ({}) on channel `{}`",
                command,
                command.getClass().getSimpleName(),
                channelUID);
    }

    private void changeColorOfRgb(
            final io.swagger.client.model.Channel channel, final HSBType hsbType, final ChannelUID rgbChannelUid)
            throws Exception {
        logger.trace("Setting color to `{}` for channel `{}`", hsbType, rgbChannelUid);
        handleHsbCommand(channel, rgbChannelUid, hsbType);
        updateState(rgbChannelUid, hsbType);
    }

    void refresh() {
        logger.trace("Refreshing `{}`", thing.getUID());
        try {
            if (checkIfIsOnline() && checkIfIsEnabled()) {
                updateStatus(ONLINE);
                logger.trace("Thing `{}` is connected & enabled. Refreshing channels", thing.getUID());
                thing.getChannels().stream()
                        .map(Channel::getUID)
                        .forEach(channelUID -> handleCommand(channelUID, REFRESH));
            }
        } catch (Exception e) {
            logger.error("Cannot check if device `{}` is online/enabled", thing.getUID(), e);
        }
    }

    private io.swagger.client.model.Channel queryForChannel(final int channelId) throws Exception {
        return requireNonNull(channelsApi).getChannel(channelId, List.of("state"));
    }
}
