package pl.grzeslowski.openhab.supla.internal.server.handler;

import pl.grzeslowski.jsupla.server.MessageHandler;
import pl.grzeslowski.openhab.supla.internal.server.traits.RegisterDeviceTrait;

public interface SuplaThing extends MessageHandler {
    boolean register(RegisterDeviceTrait register, OpenHabMessageHandler openHabMessageHandler);

    String getGuid();
}
