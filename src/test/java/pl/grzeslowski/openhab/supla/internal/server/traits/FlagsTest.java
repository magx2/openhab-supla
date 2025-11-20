package pl.grzeslowski.openhab.supla.internal.server.traits;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_DEVICE_FLAG_ALWAYS_ALLOW_CHANNEL_DELETION;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_DEVICE_FLAG_CALCFG_IDENTIFY_DEVICE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_DEVICE_FLAG_CALCFG_RESTART_DEVICE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_DEVICE_FLAG_CALCFG_SET_TIME;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_DEVICE_FLAG_CALCFG_SUBDEVICE_PAIRING;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_DEVICE_FLAG_DEVICE_CONFIG_SUPPORTED;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_DEVICE_FLAG_DEVICE_LOCKED;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_DEVICE_FLAG_SLEEP_MODE_ENABLED;

import org.junit.jupiter.api.Test;

class FlagsTest {
    @Test
    void shouldSetAllFlags() {
        int combined =
                SUPLA_DEVICE_FLAG_CALCFG_ENTER_CFG_MODE
                        | SUPLA_DEVICE_FLAG_SLEEP_MODE_ENABLED
                        | SUPLA_DEVICE_FLAG_CALCFG_SET_TIME
                        | SUPLA_DEVICE_FLAG_DEVICE_CONFIG_SUPPORTED
                        | SUPLA_DEVICE_FLAG_DEVICE_LOCKED
                        | SUPLA_DEVICE_FLAG_CALCFG_SUBDEVICE_PAIRING
                        | SUPLA_DEVICE_FLAG_CALCFG_IDENTIFY_DEVICE
                        | SUPLA_DEVICE_FLAG_CALCFG_RESTART_DEVICE
                        | SUPLA_DEVICE_FLAG_ALWAYS_ALLOW_CHANNEL_DELETION;

        Flags flags = new Flags(combined);

        assertThat(flags.calcfgEnterCfgMode()).isTrue();
        assertThat(flags.sleepModeEnabled()).isTrue();
        assertThat(flags.calcfgSetTime()).isTrue();
        assertThat(flags.deviceConfigSupported()).isTrue();
        assertThat(flags.deviceLocked()).isTrue();
        assertThat(flags.calcfgSubdevicePairing()).isTrue();
        assertThat(flags.calcfgIdentifyDevice()).isTrue();
        assertThat(flags.calcfgRestartDevice()).isTrue();
        assertThat(flags.alwaysAllowChannelDeletion()).isTrue();
        assertThat(flags.suplaDeviceFlagBlockAddingChannelsAfterDeletion()).isFalse();
    }

    @Test
    void shouldDefaultAllFlagsToFalse() {
        Flags flags = new Flags(0);

        assertThat(flags.calcfgEnterCfgMode()).isFalse();
        assertThat(flags.sleepModeEnabled()).isFalse();
        assertThat(flags.calcfgSetTime()).isFalse();
        assertThat(flags.deviceConfigSupported()).isFalse();
        assertThat(flags.deviceLocked()).isFalse();
        assertThat(flags.calcfgSubdevicePairing()).isFalse();
        assertThat(flags.calcfgIdentifyDevice()).isFalse();
        assertThat(flags.calcfgRestartDevice()).isFalse();
        assertThat(flags.alwaysAllowChannelDeletion()).isFalse();
        assertThat(flags.suplaDeviceFlagBlockAddingChannelsAfterDeletion()).isFalse();
    }
}
