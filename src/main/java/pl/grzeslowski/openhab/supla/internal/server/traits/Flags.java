package pl.grzeslowski.openhab.supla.internal.server.traits;

import static lombok.AccessLevel.PRIVATE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class Flags {
    boolean calcfgEnterCfgMode;
    boolean sleepModeEnabled;
    boolean calcfgSetTime;
    boolean deviceConfigSupported;
    boolean deviceLocked;
    boolean calcfgSubdevicePairing;
    boolean calcfgIdentifyDevice;
    boolean calcfgRestartDevice;
    boolean alwaysAllowChannelDeletion;
    boolean suplaDeviceFlagBlockAddingChannelsAfterDeletion;

    public Flags(int flags) {
        this(
                (flags & SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE) != 0,
                (flags & SUPLA_DEVICE_FLAG_SLEEP_MODE_ENABLED) != 0,
                (flags & SUPLA_DEVICE_FLAG_CALCFG_SET_TIME) != 0,
                (flags & SUPLA_DEVICE_FLAG_DEVICE_CONFIG_SUPPORTED) != 0,
                (flags & SUPLA_DEVICE_FLAG_DEVICE_LOCKED) != 0,
                (flags & SUPLA_DEVICE_FLAG_CALCFG_SUBDEVICE_PAIRING) != 0,
                (flags & SUPLA_DEVICE_FLAG_CALCFG_IDENTIFY_DEVICE) != 0,
                (flags & SUPLA_DEVICE_FLAG_CALCFG_RESTART_DEVICE) != 0,
                (flags & SUPLA_DEVICE_FLAG_ALWAYS_ALLOW_CHANNEL_DELETION) != 0,
                (flags & SUPLA_DEVICE_FLAG_BLOCK_ADDING_CHANNELS_AFTER_DELETION) != 0);
    }
}
