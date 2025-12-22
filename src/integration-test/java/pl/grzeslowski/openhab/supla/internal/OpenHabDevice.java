package pl.grzeslowski.openhab.supla.internal;

import static java.util.Collections.synchronizedMap;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;

@Slf4j
@Builder
public class OpenHabDevice implements ThingHandlerCallback {
    private final Map<Thing, ThingStatusInfo> thingStatus = synchronizedMap(new HashMap<>());
    private final Map<ChannelUID, State> channelStates = synchronizedMap(new HashMap<>());

    @Getter
    @Nullable
    private Thing thing;

    private final Bridge bridge;

    @Override
    public void stateUpdated(ChannelUID channelUID, State state) {
        channelStates.put(channelUID, state);
    }

    public State findChannelState(ChannelUID channelUID) {
        return requireNonNull(channelStates.get(channelUID));
    }

    public ChannelUID findChannel() {
        var channelUIDS = channelStates.keySet();
        assertThat(channelUIDS).hasSize(1);
        return channelUIDS.iterator().next();
    }

    @Override
    public void postCommand(ChannelUID channelUID, Command command) {
        throw new UnsupportedOperationException("OpenHabDevice.postCommand(channelUID, command)");
    }

    @Override
    public void sendTimeSeries(ChannelUID channelUID, TimeSeries timeSeries) {
        throw new UnsupportedOperationException("OpenHabDevice.sendTimeSeries(channelUID, timeSeries)");
    }

    @Override
    public void statusUpdated(Thing thing, ThingStatusInfo thingStatus) {
        this.thingStatus.put(thing, thingStatus);
    }

    public ThingStatusInfo findThingStatus(Thing thing) {
        return requireNonNull(this.thingStatus.get(thing));
    }

    public ThingStatusInfo findThingStatus() {
        return findThingStatus(requireNonNull(thing));
    }

    @Override
    public void thingUpdated(Thing thing) {
        this.thing = thing;
    }

    @Override
    public void validateConfigurationParameters(Thing thing, Map<String, Object> configurationParameters) {
        log.info("OpenHabDevice.validateConfigurationParameters({}, {})", thing, configurationParameters);
    }

    @Override
    public void validateConfigurationParameters(Channel channel, Map<String, Object> configurationParameters) {
        log.info("OpenHabDevice.validateConfigurationParameters({}, {})", channel, configurationParameters);
    }

    @Override
    public @Nullable ConfigDescription getConfigDescription(ChannelTypeUID channelTypeUID) {
        throw new UnsupportedOperationException("OpenHabDevice.getConfigDescription(channelTypeUID)");
    }

    @Override
    public @Nullable ConfigDescription getConfigDescription(ThingTypeUID thingTypeUID) {
        throw new UnsupportedOperationException("OpenHabDevice.getConfigDescription(thingTypeUID)");
    }

    @Override
    public void configurationUpdated(Thing thing) {
        throw new UnsupportedOperationException("OpenHabDevice.configurationUpdated(thing)");
    }

    @Override
    public void migrateThingType(Thing thing, ThingTypeUID thingTypeUID, Configuration configuration) {
        throw new UnsupportedOperationException("OpenHabDevice.migrateThingType(thing, thingTypeUID, configuration)");
    }

    @Override
    public void channelTriggered(Thing thing, ChannelUID channelUID, String event) {
        throw new UnsupportedOperationException("OpenHabDevice.channelTriggered(thing, channelUID, event)");
    }

    @Override
    public ChannelBuilder createChannelBuilder(ChannelUID channelUID, ChannelTypeUID channelTypeUID) {
        throw new UnsupportedOperationException("OpenHabDevice.createChannelBuilder(channelUID, channelTypeUID)");
    }

    @Override
    public ChannelBuilder editChannel(Thing thing, ChannelUID channelUID) {
        throw new UnsupportedOperationException("OpenHabDevice.editChannel(thing, channelUID)");
    }

    @Override
    public List<ChannelBuilder> createChannelBuilders(
            ChannelGroupUID channelGroupUID, ChannelGroupTypeUID channelGroupTypeUID) {
        throw new UnsupportedOperationException(
                "OpenHabDevice.createChannelBuilders(channelGroupUID, channelGroupTypeUID)");
    }

    @Override
    public boolean isChannelLinked(ChannelUID channelUID) {
        throw new UnsupportedOperationException("OpenHabDevice.isChannelLinked(channelUID)");
    }

    @Override
    public @Nullable Bridge getBridge(ThingUID bridgeUID) {
        return bridge;
    }
}
