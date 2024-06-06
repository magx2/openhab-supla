package pl.grzeslowski.openhab.supla.internal.cloud.functionswitch;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.openhab.core.library.types.OnOffType.OFF;
import static org.openhab.core.library.types.OnOffType.ON;

import io.swagger.client.model.Channel;
import io.swagger.client.model.ChannelState;
import io.swagger.client.model.ElectricityMeterStatePhase;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.grzeslowski.openhab.supla.internal.cloud.*;
import pl.grzeslowski.openhab.supla.internal.cloud.executors.LedCommandExecutor;

@NonNullByDefault
@SuppressWarnings("PackageAccessibility")
public class FindStateFunctionSwitch implements ChannelFunctionDispatcher.FunctionSwitch<Optional<? extends State>> {
    private final Logger logger = LoggerFactory.getLogger(FindStateFunctionSwitch.class);
    private final LedCommandExecutor ledCommandExecutor;
    private final ChannelUID channelUID;
    private final ChannelInfoParser channelInfoParser;

    public FindStateFunctionSwitch(
            LedCommandExecutor ledCommandExecutor, final ChannelUID channelUID, ChannelInfoParser channelInfoParser) {
        this.ledCommandExecutor = ledCommandExecutor;
        this.channelUID = channelUID;
        this.channelInfoParser = channelInfoParser;
    }

    public FindStateFunctionSwitch(LedCommandExecutor ledCommandExecutor, final ChannelUID channelUID) {
        this(ledCommandExecutor, channelUID, ChannelInfoParser.PARSER);
    }

    @Override
    public Optional<? extends State> onNone(Channel channel) {
        return empty();
    }

    @Override
    public Optional<? extends State> onControllingTheGatewayLock(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onControllingTheGate(Channel channel) {
        return optionalHiType(channel);
    }

    @Override
    public Optional<? extends State> onControllingTheGarageDoor(Channel channel) {
        return optionalHiType(channel);
    }

    @Override
    public Optional<? extends State> onThermometer(Channel channel) {
        return of(channel)
                .map(Channel::getState)
                .map(s -> findTemperature(s, channel.getParam2()))
                .map(DecimalType::new);
    }

    @Override
    public Optional<? extends State> onHumidity(Channel channel) {
        return of(channel)
                .map(Channel::getState)
                .map(channelState -> findHumidity(channelState, channel.getParam3()))
                .map(DecimalType::new);
    }

    @Override
    public Optional<? extends State> onHumidityAndTemperature(Channel channel) {
        final ChannelInfo channelInfo = channelInfoParser.parse(channelUID);
        final AdditionalChannelType channelType = channelInfo.getAdditionalChannelType();
        requireNonNull(channelType, "Additional type for channel " + channel + " cannot be null!");
        final Optional<ChannelState> state = of(channel).map(Channel::getState);
        return switch (channelType) {
            case TEMPERATURE -> state.map(s -> findTemperature(s, channel.getParam2()))
                    .map(DecimalType::new);
            case HUMIDITY -> state.map(s -> findHumidity(s, channel.getParam2()))
                    .map(DecimalType::new);
            default -> throw new IllegalStateException(
                    "Additional type " + channelType + " is not supported for HumidityAndTemperature channel");
        };
    }

    private BigDecimal findTemperature(ChannelState channelState, Integer param2) {
        return findValueWithAdjustment(channelState.getTemperature(), param2);
    }

    private BigDecimal findHumidity(ChannelState channelState, Integer param3) {
        return findValueWithAdjustment(channelState.getHumidity(), param3);
    }

    private BigDecimal findValueWithAdjustment(BigDecimal value, Integer adjustment) {
        return Optional.ofNullable(adjustment) //
                .map(v -> v / 100) //
                .map(BigDecimal::new) //
                .orElse(BigDecimal.ZERO) //
                .add(value);
    }

    @Override
    public Optional<? extends State> onOpeningSensorGateway(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onOpeningSensorGate(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onOpeningSensorGarageDoor(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onNoLiquidSensor(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onControllingTheDoorLock(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onOpeningSensorDoor(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onControllingTheRollerShutter(Channel channel) {
        return of(channel)
                .map(Channel::getState)
                .map(ChannelState::getShut)
                .map(shut -> 100 - shut)
                .map(PercentType::new);
    }

    @Override
    public Optional<? extends State> onOpeningSensorRollerShutter(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onPowerSwitch(Channel channel) {
        return onOffType(channel);
    }

    @Override
    public Optional<? extends State> onLightSwitch(Channel channel) {
        return onOffType(channel);
    }

    private Optional<? extends State> onOffType(Channel channel) {
        return of(channel).map(Channel::getState).map(ChannelState::isOn).map(on -> on ? ON : OFF);
    }

    @Override
    public Optional<? extends State> onDimmer(Channel channel) {
        final Optional<PercentType> state = of(channel)
                .map(Channel::getState)
                .map(ChannelState::getBrightness)
                .map(PercentType::new);
        state.ifPresent(s -> ledCommandExecutor.setLedState(channel.getId(), s));
        return state;
    }

    @Override
    public Optional<? extends State> onRgbLighting(Channel channel) {
        final Optional<HSBType> state = of(channel)
                .map(Channel::getState)
                .map(s -> HsbTypeConverter.INSTANCE.toHsbType(s.getColor(), s.getColorBrightness()));
        state.ifPresent(s -> ledCommandExecutor.setLedState(channel.getId(), s));
        return state;
    }

    @Override
    public Optional<? extends State> onDimmerAndRgbLightning(Channel channel) {
        final ChannelInfo channelInfo = channelInfoParser.parse(channelUID);
        AdditionalChannelType channelType = channelInfo.getAdditionalChannelType();
        if (channelType == null) {
            final Optional<HSBType> state = of(channel)
                    .map(Channel::getState)
                    .map(s -> HsbTypeConverter.INSTANCE.toHsbType(s.getColor(), s.getColorBrightness()));
            state.ifPresent(s -> ledCommandExecutor.setLedState(channel.getId(), s));
            return state;
        } else if (channelType == AdditionalChannelType.LED_BRIGHTNESS) {
            final Optional<PercentType> state = of(channel)
                    .map(Channel::getState)
                    .map(ChannelState::getBrightness)
                    .map(PercentType::new);
            state.ifPresent(s -> ledCommandExecutor.setLedState(channel.getId(), s));
            return state;
        } else {
            logger.warn("Do not know how to support {} on dimmer and RGB", channelType);
            return empty();
        }
    }

    @Override
    public Optional<? extends State> onDepthSensor(Channel channel) {
        return of(channel).map(Channel::getState).map(ChannelState::getDepth).map(DecimalType::new);
    }

    @Override
    public Optional<? extends State> onDistanceSensor(Channel channel) {
        return of(channel).map(Channel::getState).map(ChannelState::getDistance).map(DecimalType::new);
    }

    @Override
    public Optional<? extends State> onOpeningSensorWindow(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onMailSensor(Channel channel) {
        return hiType(channel);
    }

    @Override
    public Optional<? extends State> onWindSensor(Channel channel) {
        return empty();
    }

    @Override
    public Optional<? extends State> onPressureSensor(Channel channel) {
        return empty();
    }

    @Override
    public Optional<? extends State> onRainSensor(Channel channel) {
        return empty();
    }

    @Override
    public Optional<? extends State> onWeightSensor(Channel channel) {
        return empty();
    }

    @Override
    public Optional<? extends State> onWeatherStation(Channel channel) {
        return empty();
    }

    @Override
    public Optional<? extends State> onStaircaseTimer(Channel channel) {
        return onOffType(channel);
    }

    @Override
    public Optional<? extends State> onDefault(Channel channel) {
        logger.warn("Does not know how to map `{}` to OpenHAB state", channel.getState());
        return empty();
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Optional<? extends State> onElectricityMeter(Channel channel) {
        var channelInfo = channelInfoParser.parse(channelUID);
        var channelType = requireNonNull(
                channelInfo.getAdditionalChannelType(), "Additional type for channel " + channel + " cannot be null!");
        var idx = channelInfo.getIdx();

        var state = of(channel).map(Channel::getState);
        var phases = state.map(ChannelState::getPhases).stream().flatMap(Collection::stream);
        if (idx == null) {
            return switch (channelType) {
                case TOTAL_COST -> state.map(ChannelState::getTotalCost).map(DecimalType::new);
                case PRICE_PER_UNIT -> state.map(ChannelState::getPricePerUnit).map(DecimalType::new);
                    // total channels
                case CURRENT -> phases.map(ElectricityMeterStatePhase::getCurrent)
                        .reduce(of(new DecimalType()), this::accumulator, this::combiner);
                case POWER_ACTIVE -> phases.map(ElectricityMeterStatePhase::getPowerActive)
                        .reduce(of(new DecimalType()), this::accumulator, this::combiner);
                case POWER_REACTIVE -> phases.map(ElectricityMeterStatePhase::getPowerReactive)
                        .reduce(of(new DecimalType()), this::accumulator, this::combiner);
                case POWER_APPARENT -> phases.map(ElectricityMeterStatePhase::getPowerApparent)
                        .reduce(of(new DecimalType()), this::accumulator, this::combiner);
                case TOTAL_FORWARD_ACTIVE_ENERGY -> phases.map(ElectricityMeterStatePhase::getTotalForwardActiveEnergy)
                        .reduce(of(new DecimalType()), this::accumulator, this::combiner);
                case TOTAL_REVERSED_ACTIVE_ENERGY -> phases.map(ElectricityMeterStatePhase::getTotalReverseActiveEnergy)
                        .reduce(of(new DecimalType()), this::accumulator, this::combiner);
                case TOTAL_FORWARD_REACTIVE_ENERGY -> phases.map(
                                ElectricityMeterStatePhase::getTotalForwardReactiveEnergy)
                        .reduce(of(new DecimalType()), this::accumulator, this::combiner);
                case TOTAL_REVERSED_REACTIVE_ENERGY -> phases.map(
                                ElectricityMeterStatePhase::getTotalReverseReactiveEnergy)
                        .reduce(of(new DecimalType()), this::accumulator, this::combiner);
                default -> throw new IllegalStateException(
                        "Additional type " + channelType + " is not supported for ElectricityMeter channel");
            };
        }

        var phase = state.map(ChannelState::getPhases).map(p -> p.get(idx - 1));
        return switch (channelType) {
            case FREQUENCY -> phase.map(ElectricityMeterStatePhase::getFrequency)
                    .map(DecimalType::new);
            case VOLTAGE -> phase.map(ElectricityMeterStatePhase::getVoltage).map(DecimalType::new);
            case CURRENT -> phase.map(ElectricityMeterStatePhase::getCurrent).map(DecimalType::new);
            case POWER_ACTIVE -> phase.map(ElectricityMeterStatePhase::getPowerActive)
                    .map(DecimalType::new);
            case POWER_REACTIVE -> phase.map(ElectricityMeterStatePhase::getPowerReactive)
                    .map(DecimalType::new);
            case POWER_APPARENT -> phase.map(ElectricityMeterStatePhase::getPowerApparent)
                    .map(DecimalType::new);
            case POWER_FACTOR -> phase.map(ElectricityMeterStatePhase::getPowerFactor)
                    .map(DecimalType::new);
            case PHASE_ANGLE -> phase.map(ElectricityMeterStatePhase::getPhaseAngle)
                    .map(DecimalType::new);
            case TOTAL_FORWARD_ACTIVE_ENERGY -> phase.map(ElectricityMeterStatePhase::getTotalForwardActiveEnergy)
                    .map(DecimalType::new);
            case TOTAL_REVERSED_ACTIVE_ENERGY -> phase.map(ElectricityMeterStatePhase::getTotalReverseActiveEnergy)
                    .map(DecimalType::new);
            case TOTAL_FORWARD_REACTIVE_ENERGY -> phase.map(ElectricityMeterStatePhase::getTotalForwardReactiveEnergy)
                    .map(DecimalType::new);
            case TOTAL_REVERSED_REACTIVE_ENERGY -> phase.map(ElectricityMeterStatePhase::getTotalReverseReactiveEnergy)
                    .map(DecimalType::new);
            default -> throw new IllegalStateException(
                    "Additional type " + channelType + "_" + idx + " is not supported for ElectricityMeter channel");
        };
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<DecimalType> accumulator(Optional<DecimalType> type, BigDecimal bigDecimal) {
        return type.map(decimalType ->
                        new DecimalType(decimalType.toBigDecimal().add(bigDecimal)))
                .or(() -> of(new DecimalType(bigDecimal)));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<DecimalType> combiner(Optional<DecimalType> a, Optional<DecimalType> b) {
        if (a.isPresent() && b.isPresent()) {
            return of(new DecimalType(a.get().toBigDecimal().add(b.get().toBigDecimal())));
        }
        return a.isPresent() ? a : b;
    }

    private Optional<? extends State> hiType(Channel channel) {
        boolean invertedLogic = channel.getParam3() != null && channel.getParam3() > 0;
        return of(channel)
                .map(Channel::getState)
                .map(ChannelState::isHi)
                .map(hi -> invertedLogic != hi)
                .map(hi -> hi ? ON : OFF);
    }

    /**
     * For `CONTROLLINGTHEGATE` and `CONTROLLINGTHEGARAGEDOOR` `hi` exists only when `param2` is set.
     *
     * <p>From doc:
     *
     * <p>"hi is either true or false depending on paired opening sensor state; the hi value is provided only if the
     * channel has param2 set (i.e. has opening sensor chosen); partial_hi is either true or false depending on paired
     * secondary opening sensor state; the partial_hi value is provided only if the channel has param3 set (i.e. has
     * secondary opening sensor chosen)"
     *
     * <p>https://github.com/SUPLA/supla-cloud/wiki/Channel-Functions-states
     */
    private Optional<? extends State> optionalHiType(Channel channel) {
        boolean invertedLogic = channel.getParam3() != null && channel.getParam3() > 0;
        boolean param2Present = channel.getParam2() != null && channel.getParam2() > 0;
        if (param2Present || !channel.getType().isOutput()) {
            return of(channel)
                    .map(Channel::getState)
                    .map(ChannelState::isHi)
                    .map(hi -> invertedLogic != hi)
                    .map(hi -> hi ? ON : OFF);
        } else {
            return empty();
        }
    }
}
