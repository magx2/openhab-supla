package pl.grzeslowski.openhab.supla.internal.server.handler.trait;

import pl.grzeslowski.jsupla.server.MessageHandler;
import pl.grzeslowski.openhab.supla.internal.server.netty.OpenHabMessageHandler;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;

public interface SuplaThing extends MessageHandler {
    boolean register(RegisterDeviceTrait register, OpenHabMessageHandler openHabMessageHandler);

    String getGuid();
}
