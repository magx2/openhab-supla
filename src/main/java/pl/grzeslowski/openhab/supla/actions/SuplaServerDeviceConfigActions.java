package pl.grzeslowski.openhab.supla.actions;

import static java.lang.System.arraycopy;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static pl.grzeslowski.openhab.supla.internal.Localization.text;
import static pl.grzeslowski.openhab.supla.internal.SuplaBindingConstants.ACTION_SCOPE_DEVICE_CONFIG;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import pl.grzeslowski.jsupla.protocol.api.DeviceConfigField;
import pl.grzeslowski.jsupla.protocol.api.structs.sd.SetDeviceConfig;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfig;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigResult;
import pl.grzeslowski.openhab.supla.internal.server.device_config.DeviceConfigUtil;

@Component(scope = ServiceScope.PROTOTYPE, service = SuplaServerDeviceConfigActions.class)
@ThingActionsScope(name = ACTION_SCOPE_DEVICE_CONFIG)
@NonNullByDefault
public class SuplaServerDeviceConfigActions extends SuplaServerActionsSupport {
    @RuleAction(
            label = "@text/action.set-device-config.label",
            description = "@text/action.set-device-config.description")
    public String setDeviceConfig(
            @ActionInput(
                            name = "deviceConfigs",
                            label = "@text/action.input.device-configs.label",
                            description = "@text/action.input.device-configs.description")
                    String... deviceConfigs) {
        return setDeviceConfig(Arrays.asList(deviceConfigs));
    }

    @RuleAction(
            label = "@text/action.set-device-config.label",
            description = "@text/action.set-device-config.description")
    public String setDeviceConfig(
            @ActionInput(
                            name = "configs",
                            label = "@text/action.input.device-configs.label",
                            description = "@text/action.input.device-configs.description")
                    Collection<String> configs) {
        return runAction("setDeviceConfig", () -> setDeviceConfigOrThrow(configs));
    }

    private String setDeviceConfigOrThrow(Collection<String> configs) throws InterruptedException, TimeoutException {
        if (configs.isEmpty()) {
            throw new IllegalArgumentException("You need to pass configs!");
        }
        var localHandler = getThingHandlerOrWarn();
        if (localHandler == null) {
            throw new IllegalStateException("Thing handler is null");
        }

        var deviceConfig = configs.stream()
                .map(DeviceConfigUtil::parseDeviceConfig)
                .collect(Collectors.toCollection(TreeSet::new));

        var encodedConfig = deviceConfig.stream().map(DeviceConfig::encode).toList();

        var size = encodedConfig.stream().mapToInt(e -> e.length).sum();

        var config = new byte[size];
        var offset = 0;
        for (var encoded : encodedConfig) {
            arraycopy(encoded, 0, config, offset, encoded.length);
            offset += encoded.length;
        }

        var fields = BigInteger.valueOf(deviceConfig.stream()
                .map(DeviceConfig::field)
                .map(DeviceConfigField::getValue)
                .reduce(0L, (field, mask) -> field | mask));

        var message = new SetDeviceConfig(
                (short) 1, // end of data
                new short[8],
                requireNonNullElse(localHandler.getAvailableFields(), fields),
                fields,
                size,
                config);

        var writer = localHandler.getWriter().get();
        if (writer == null) {
            throw new IllegalStateException("There is no socket writer!");
        }
        var timeout = localHandler.getConfiguration().getSetDeviceConfigActionTimeout();
        writer.write(message).await(timeout.toMillis(), MILLISECONDS);
        var deviceConfigResult = localHandler.listenForSetDeviceConfigResult(timeout.toMillis(), MILLISECONDS);
        var result = DeviceConfigResult.findConfigResult(deviceConfigResult.result());
        if (!result.isSuccess()) {
            throw new RuntimeException("Setting device config did not succeed! configs=%s. %s"
                    .formatted(
                            configs.stream().map(Object::toString).collect(Collectors.joining(", ")),
                            "SetDeviceConfig=" + message));
        }
        localHandler.consumeSetDeviceConfig(fields.longValue(), config);
        return text(
                "action.set-device-config.result.success",
                configs.stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    public static String setDeviceConfig(@Nullable ThingActions actions, String... deviceConfigs) {
        if (actions instanceof SuplaServerDeviceConfigActions serverActions) {
            return serverActions.setDeviceConfig(deviceConfigs);
        }
        return unavailableActionService("setDeviceConfig", actions, SuplaServerDeviceConfigActions.class);
    }

    public static String setDeviceConfig(@Nullable ThingActions actions, Collection<String> configs) {
        if (actions instanceof SuplaServerDeviceConfigActions serverActions) {
            return serverActions.setDeviceConfig(configs);
        }
        return unavailableActionService("setDeviceConfig", actions, SuplaServerDeviceConfigActions.class);
    }
}
