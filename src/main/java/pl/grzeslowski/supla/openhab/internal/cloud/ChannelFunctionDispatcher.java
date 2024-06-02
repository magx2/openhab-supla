package pl.grzeslowski.supla.openhab.internal.cloud;

import io.swagger.client.model.Channel;
import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
@SuppressWarnings("PackageAccessibility")
public class ChannelFunctionDispatcher {
    public static final ChannelFunctionDispatcher DISPATCHER = new ChannelFunctionDispatcher();

    public <T> T dispatch(Channel channel, FunctionSwitch<T> functionSwitch) {
        return switch (channel.getFunction().getName()) {
            case NONE -> functionSwitch.onNone(channel);
            case CONTROLLINGTHEGATEWAYLOCK -> functionSwitch.onControllingTheGatewayLock(channel);
            case CONTROLLINGTHEGATE -> functionSwitch.onControllingTheGate(channel);
            case CONTROLLINGTHEGARAGEDOOR -> functionSwitch.onControllingTheGarageDoor(channel);
            case THERMOMETER -> functionSwitch.onThermometer(channel);
            case HUMIDITY -> functionSwitch.onHumidity(channel);
            case HUMIDITYANDTEMPERATURE -> functionSwitch.onHumidityAndTemperature(channel);
            case OPENINGSENSOR_GATEWAY -> functionSwitch.onOpeningSensorGateway(channel);
            case OPENINGSENSOR_GATE -> functionSwitch.onOpeningSensorGate(channel);
            case OPENINGSENSOR_GARAGEDOOR -> functionSwitch.onOpeningSensorGarageDoor(channel);
            case NOLIQUIDSENSOR -> functionSwitch.onNoLiquidSensor(channel);
            case CONTROLLINGTHEDOORLOCK -> functionSwitch.onControllingTheDoorLock(channel);
            case OPENINGSENSOR_DOOR -> functionSwitch.onOpeningSensorDoor(channel);
            case CONTROLLINGTHEROLLERSHUTTER -> functionSwitch.onControllingTheRollerShutter(channel);
            case OPENINGSENSOR_ROLLERSHUTTER -> functionSwitch.onOpeningSensorRollerShutter(channel);
            case POWERSWITCH -> functionSwitch.onPowerSwitch(channel);
            case LIGHTSWITCH -> functionSwitch.onLightSwitch(channel);
            case DIMMER -> functionSwitch.onDimmer(channel);
            case RGBLIGHTING -> functionSwitch.onRgbLighting(channel);
            case DIMMERANDRGBLIGHTING -> functionSwitch.onDimmerAndRgbLightning(channel);
            case DEPTHSENSOR -> functionSwitch.onDepthSensor(channel);
            case DISTANCESENSOR -> functionSwitch.onDistanceSensor(channel);
            case OPENINGSENSOR_WINDOW -> functionSwitch.onOpeningSensorWindow(channel);
            case MAILSENSOR -> functionSwitch.onMailSensor(channel);
            case WINDSENSOR -> functionSwitch.onWindSensor(channel);
            case PRESSURESENSOR -> functionSwitch.onPressureSensor(channel);
            case RAINSENSOR -> functionSwitch.onRainSensor(channel);
            case WEIGHTSENSOR -> functionSwitch.onWeightSensor(channel);
            case WEATHER_STATION -> functionSwitch.onWeatherStation(channel);
            case STAIRCASETIMER -> functionSwitch.onStaircaseTimer(channel);
            case ELECTRICITYMETER -> functionSwitch.onElectricityMeter(channel);
        };
    }

    public interface FunctionSwitch<T> {
        T onNone(Channel channel);

        T onControllingTheGatewayLock(Channel channel);

        T onControllingTheGate(Channel channel);

        T onControllingTheGarageDoor(Channel channel);

        T onThermometer(Channel channel);

        T onHumidity(Channel channel);

        T onHumidityAndTemperature(Channel channel);

        T onOpeningSensorGateway(Channel channel);

        T onOpeningSensorGate(Channel channel);

        T onOpeningSensorGarageDoor(Channel channel);

        T onNoLiquidSensor(Channel channel);

        T onControllingTheDoorLock(Channel channel);

        T onOpeningSensorDoor(Channel channel);

        T onControllingTheRollerShutter(Channel channel);

        T onOpeningSensorRollerShutter(Channel channel);

        T onPowerSwitch(Channel channel);

        T onLightSwitch(Channel channel);

        T onDimmer(Channel channel);

        T onRgbLighting(Channel channel);

        T onDimmerAndRgbLightning(Channel channel);

        T onDepthSensor(Channel channel);

        T onDistanceSensor(Channel channel);

        T onOpeningSensorWindow(Channel channel);

        T onMailSensor(Channel channel);

        T onWindSensor(Channel channel);

        T onPressureSensor(Channel channel);

        T onRainSensor(Channel channel);

        T onWeightSensor(Channel channel);

        T onWeatherStation(Channel channel);

        T onStaircaseTimer(Channel channel);

        T onDefault(Channel channel);

        T onElectricityMeter(Channel channel);
    }
}
