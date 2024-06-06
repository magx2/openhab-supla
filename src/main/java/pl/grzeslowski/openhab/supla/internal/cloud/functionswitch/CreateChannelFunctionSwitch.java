package pl.grzeslowski.openhab.supla.internal.cloud.functionswitch;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.Channels.*;
import static pl.grzeslowski.openhab.supla.internal.cloud.AdditionalChannelType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants;
import pl.grzeslowski.openhab.supla.internal.cloud.ChannelFunctionDispatcher;

@NonNullByDefault
@SuppressWarnings("PackageAccessibility")
public class CreateChannelFunctionSwitch implements ChannelFunctionDispatcher.FunctionSwitch<List<Channel>> {
    private static final int MAX_NUMBER_OF_PHASES = 3;
    private final Logger logger = LoggerFactory.getLogger(CreateChannelFunctionSwitch.class);
    private final ThingUID thingUID;

    public CreateChannelFunctionSwitch(final ThingUID thingUID) {
        this.thingUID = requireNonNull(thingUID);
    }

    @Override
    public List<Channel> onNone(io.swagger.client.model.Channel channel) {
        return emptyList();
    }

    @Override
    public List<Channel> onControllingTheGatewayLock(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onControllingTheGate(io.swagger.client.model.Channel channel) {
        return createToggleGateChannel(channel);
    }

    @Override
    public List<Channel> onControllingTheGarageDoor(io.swagger.client.model.Channel channel) {
        return createToggleGateChannel(channel);
    }

    @Override
    public List<Channel> onThermometer(io.swagger.client.model.Channel channel) {
        return singletonList(createChannel(channel, TEMPERATURE_CHANNEL_ID, "Number"));
    }

    @Override
    public List<Channel> onHumidity(io.swagger.client.model.Channel channel) {
        return singletonList(createChannel(channel, HUMIDITY_CHANNEL_ID, "Number"));
    }

    @Override
    public List<Channel> onHumidityAndTemperature(io.swagger.client.model.Channel channel) {
        return asList(
                createChannel(
                        TEMPERATURE_CHANNEL_ID, "Number", channel.getId() + TEMPERATURE.getSuffix(), "Temperature"),
                createChannel(HUMIDITY_CHANNEL_ID, "Number", channel.getId() + HUMIDITY.getSuffix(), "Humidity"));
    }

    @Override
    public List<Channel> onOpeningSensorGateway(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onOpeningSensorGate(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onOpeningSensorGarageDoor(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onNoLiquidSensor(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onControllingTheDoorLock(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onOpeningSensorDoor(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onControllingTheRollerShutter(io.swagger.client.model.Channel channel) {
        return singletonList(createChannel(channel, ROLLER_SHUTTER_CHANNEL_ID, "Rollershutter"));
    }

    @Override
    public List<Channel> onOpeningSensorRollerShutter(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onPowerSwitch(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onLightSwitch(io.swagger.client.model.Channel channel) {
        return singletonList(createChannel(channel, LIGHT_CHANNEL_ID, "Switch"));
    }

    @Override
    public List<Channel> onDimmer(io.swagger.client.model.Channel channel) {
        return singletonList(createChannel(channel, DIMMER_CHANNEL_ID, "Dimmer"));
    }

    @Override
    public List<Channel> onRgbLighting(io.swagger.client.model.Channel channel) {
        return createLedChannels(channel);
    }

    @Override
    public List<Channel> onDimmerAndRgbLightning(io.swagger.client.model.Channel channel) {
        final List<Channel> ledChannels = createLedChannels(channel);
        final List<Channel> channels = new ArrayList<>(ledChannels);
        final Channel brightnessChannel =
                createChannel(DIMMER_CHANNEL_ID, "Dimmer", channel.getId() + "_brightness", "Brightness");
        channels.add(brightnessChannel);
        return channels;
    }

    @Override
    public List<Channel> onDepthSensor(io.swagger.client.model.Channel channel) {
        return singletonList(createChannel(channel, DECIMAL_CHANNEL_ID, "Number"));
    }

    @Override
    public List<Channel> onDistanceSensor(io.swagger.client.model.Channel channel) {
        return singletonList(createChannel(channel, DECIMAL_CHANNEL_ID, "Number"));
    }

    @Override
    public List<Channel> onOpeningSensorWindow(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onMailSensor(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onWindSensor(io.swagger.client.model.Channel channel) {
        return emptyList();
    }

    @Override
    public List<Channel> onPressureSensor(io.swagger.client.model.Channel channel) {
        return emptyList();
    }

    @Override
    public List<Channel> onRainSensor(io.swagger.client.model.Channel channel) {
        return emptyList();
    }

    @Override
    public List<Channel> onWeightSensor(io.swagger.client.model.Channel channel) {
        return emptyList();
    }

    @Override
    public List<Channel> onWeatherStation(io.swagger.client.model.Channel channel) {
        return emptyList();
    }

    @Override
    public List<Channel> onStaircaseTimer(io.swagger.client.model.Channel channel) {
        return createSwitchChannel(channel);
    }

    @Override
    public List<Channel> onDefault(io.swagger.client.model.Channel channel) {
        logger.warn(
                "Does not know type of this `{}` function",
                channel.getFunction().getName());
        return emptyList();
    }

    @Override
    public List<Channel> onElectricityMeter(io.swagger.client.model.Channel channel) {
        var channels = new ArrayList<Channel>();
        {
            // basic channels
            channels.add(createChannel(
                    DECIMAL_CHANNEL_ID,
                    "Number:Currency",
                    "%.2f",
                    channel.getId() + TOTAL_COST.getSuffix(),
                    "Total Cost"));
            channels.add(createChannel(
                    DECIMAL_CHANNEL_ID,
                    "Number:EnergyPrice",
                    "%.5f",
                    channel.getId() + PRICE_PER_UNIT.getSuffix(),
                    "Price Per Unit"));
        }
        {
            // total phase channels
            channels.add(createChannel(
                    CURRENT_CHANNEL_ID,
                    "Number:ElectricCurrent",
                    channel.getId() + CURRENT.getSuffix(),
                    "Current (Total)"));
            channels.add(createChannel(
                    POWER_CHANNEL_ID,
                    "Number:Power",
                    channel.getId() + POWER_ACTIVE.getSuffix(),
                    "Power Active (Total)"));
            channels.add(createChannel(
                    POWER_CHANNEL_ID,
                    "Number:Power",
                    channel.getId() + POWER_REACTIVE.getSuffix(),
                    "Power Reactive (Total)"));
            channels.add(createChannel(
                    POWER_CHANNEL_ID,
                    "Number:Power",
                    channel.getId() + POWER_APPARENT.getSuffix(),
                    "Power Apparent (Total)"));
            channels.add(createChannel(
                    ENERGY_CHANNEL_ID,
                    "Number:Energy",
                    channel.getId() + TOTAL_FORWARD_ACTIVE_ENERGY.getSuffix(),
                    "Total Forward Active Energy (Total)"));
            channels.add(createChannel(
                    ENERGY_CHANNEL_ID,
                    "Number:Energy",
                    channel.getId() + TOTAL_REVERSED_ACTIVE_ENERGY.getSuffix(),
                    "Total Reversed Active Energy (Total)"));
            channels.add(createChannel(
                    ENERGY_CHANNEL_ID,
                    "Number:Energy",
                    channel.getId() + TOTAL_FORWARD_REACTIVE_ENERGY.getSuffix(),
                    "Total Forward Reactive Energy (Total)"));
            channels.add(createChannel(
                    ENERGY_CHANNEL_ID,
                    "Number:Energy",
                    channel.getId() + TOTAL_REVERSED_REACTIVE_ENERGY.getSuffix(),
                    "Total Reversed Reactive Energy (Total)"));
        }
        for (int idx = 1; idx <= MAX_NUMBER_OF_PHASES; idx++) {
            channels.add(createChannel(
                    FREQUENCY_CHANNEL_ID,
                    "Number:Frequency",
                    channel.getId() + "__" + idx + FREQUENCY.getSuffix(),
                    "Frequency (Phase " + idx + ")"));
            channels.add(createChannel(
                    VOLTAGE_CHANNEL_ID,
                    "Number:ElectricPotential",
                    channel.getId() + "__" + idx + VOLTAGE.getSuffix(),
                    "Voltage (Phase " + idx + ")"));
            channels.add(createChannel(
                    CURRENT_CHANNEL_ID,
                    "Number:ElectricCurrent",
                    channel.getId() + "__" + idx + CURRENT.getSuffix(),
                    "Current (Phase " + idx + ")"));
            channels.add(createChannel(
                    POWER_CHANNEL_ID,
                    "Number:Power",
                    channel.getId() + "__" + idx + POWER_ACTIVE.getSuffix(),
                    "Power Active (Phase " + idx + ")"));
            channels.add(createChannel(
                    POWER_CHANNEL_ID,
                    "Number:Power",
                    channel.getId() + "__" + idx + POWER_REACTIVE.getSuffix(),
                    "Power Reactive (Phase " + idx + ")"));
            channels.add(createChannel(
                    POWER_CHANNEL_ID,
                    "Number:Power",
                    channel.getId() + "__" + idx + POWER_APPARENT.getSuffix(),
                    "Power Apparent (Phase " + idx + ")"));
            channels.add(createChannel(
                    DECIMAL_CHANNEL_ID,
                    "Number",
                    channel.getId() + "__" + idx + POWER_FACTOR.getSuffix(),
                    "Power Factor (Phase " + idx + ")"));
            channels.add(createChannel(
                    DECIMAL_CHANNEL_ID,
                    "Number:Angle",
                    channel.getId() + "__" + idx + PHASE_ANGLE.getSuffix(),
                    "Phase Angle (Phase " + idx + ")"));
            channels.add(createChannel(
                    ENERGY_CHANNEL_ID,
                    "Number:Energy",
                    channel.getId() + "__" + idx + TOTAL_FORWARD_ACTIVE_ENERGY.getSuffix(),
                    "Total Forward Active Energy (Phase " + idx + ")"));
            channels.add(createChannel(
                    ENERGY_CHANNEL_ID,
                    "Number:Energy",
                    channel.getId() + "__" + idx + TOTAL_REVERSED_ACTIVE_ENERGY.getSuffix(),
                    "Total Reversed Active Energy (Phase " + idx + ")"));
            channels.add(createChannel(
                    ENERGY_CHANNEL_ID,
                    "Number:Energy",
                    channel.getId() + "__" + idx + TOTAL_FORWARD_REACTIVE_ENERGY.getSuffix(),
                    "Total Forward Reactive Energy (Phase " + idx + ")"));
            channels.add(createChannel(
                    ENERGY_CHANNEL_ID,
                    "Number:Energy",
                    channel.getId() + "__" + idx + TOTAL_REVERSED_REACTIVE_ENERGY.getSuffix(),
                    "Total Reversed Reactive Energy (Phase " + idx + ")"));
        }

        return unmodifiableList(channels);
    }

    private Channel createChannel(
            final String id, final String acceptedItemType, final String channelId, final String caption) {
        final ChannelUID channelUid = new ChannelUID(thingUID, channelId);
        final ChannelTypeUID channelTypeUID = new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, id);

        final ChannelBuilder channelBuilder =
                ChannelBuilder.create(channelUid, acceptedItemType).withType(channelTypeUID);

        if (!isNullOrEmpty(caption)) {
            channelBuilder.withLabel(caption);
        }
        return channelBuilder.build();
    }

    @SuppressWarnings("SameParameterValue")
    private Channel createChannel(
            final String id,
            final String acceptedItemType,
            final String state,
            final String channelId,
            final String caption) {
        final ChannelUID channelUid = new ChannelUID(thingUID, channelId);
        final ChannelTypeUID channelTypeUID = new ChannelTypeUID(SuplaBindingConstants.BINDING_ID, id);

        final ChannelBuilder channelBuilder = ChannelBuilder.create(channelUid, acceptedItemType)
                .withType(channelTypeUID)
                .withProperties(Map.of("state", state));

        if (!isNullOrEmpty(caption)) {
            channelBuilder.withLabel(caption);
        }
        return channelBuilder.build();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isNullOrEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

    private Channel createChannel(
            final io.swagger.client.model.Channel channel, final String id, final String acceptedItemType) {
        return createChannel(id, acceptedItemType, String.valueOf(channel.getId()), channel.getCaption());
    }

    private List<Channel> createSwitchChannel(io.swagger.client.model.Channel channel) {
        var channelType = channel.getType().isOutput() ? SWITCH_CHANNEL_ID : SWITCH_CHANNEL_RO_ID;
        return singletonList(createChannel(channel, channelType, "Switch"));
    }

    private List<Channel> createToggleGateChannel(io.swagger.client.model.Channel channel) {
        return singletonList(createChannel(channel, TOGGLE_GAT_CHANNEL_ID, "Switch"));
    }

    private List<Channel> createLedChannels(final io.swagger.client.model.Channel channel) {
        return singletonList(createChannel(channel, RGB_CHANNEL_ID, "Color"));
    }
}
