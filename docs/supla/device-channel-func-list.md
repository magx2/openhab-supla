# TDS_SuplaDeviceChannel_B.FuncList (server view)

## What it is
`TDS_SuplaDeviceChannel_B` is the per-channel structure sent by devices during
registration (`TDS_SuplaRegisterDevice_*`). The `FuncList` field tells the
server which channel functions the device reports as supported for that channel.
It is distinct from the single `Default`/`Func` channel function stored in the
same registration payload.

On the server side, `FuncList` is consumed during registration and stored in the
DB. When a device re-registers, the server merges new function support with what
is already stored (ORs the bitmask). It also uses `FuncList` to validate whether
a requested function change is allowed.

Key references:
- `supla-core/supla-common/proto.h` defines the bit values.
- `supla-core/supla-server/src/device/call_handler/abstract_register_device.cpp`
reads and stores `FuncList`.
- `supla-core/supla-server/src/user/user.cpp` checks `FuncList` when changing a
channel function.

## Is it a mask?
Yes. `FuncList` is a 32-bit bitmask. Each bit corresponds to a function the
channel supports. Multiple bits can be set at once.

Note: for `SUPLA_CHANNELTYPE_ACTIONTRIGGER`, the server filters the list to 0
(`supla_device_channel::func_list_filter`). For RGBW/dimmer channel types, the
protocol also defines a separate `RGBW_FuncList` bitmask and states that the
list of supported functions should be determined based on that bitmap. Newer
firmware also changed HVAC usage so `FuncList` stores supported *functions* (not
modes), per the device changelog.

## Supported bit values
Defined in `supla-core/supla-common/proto.h` as `SUPLA_BIT_FUNC_*`:

- `0x00000001` `SUPLA_BIT_FUNC_CONTROLLINGTHEGATEWAYLOCK`
- `0x00000002` `SUPLA_BIT_FUNC_CONTROLLINGTHEGATE`
- `0x00000004` `SUPLA_BIT_FUNC_CONTROLLINGTHEGARAGEDOOR`
- `0x00000008` `SUPLA_BIT_FUNC_CONTROLLINGTHEDOORLOCK`
- `0x00000010` `SUPLA_BIT_FUNC_CONTROLLINGTHEROLLERSHUTTER`
- `0x00000020` `SUPLA_BIT_FUNC_POWERSWITCH`
- `0x00000040` `SUPLA_BIT_FUNC_LIGHTSWITCH`
- `0x00000080` `SUPLA_BIT_FUNC_STAIRCASETIMER`
- `0x00000100` `SUPLA_BIT_FUNC_THERMOMETER`
- `0x00000200` `SUPLA_BIT_FUNC_HUMIDITYANDTEMPERATURE`
- `0x00000400` `SUPLA_BIT_FUNC_HUMIDITY`
- `0x00000800` `SUPLA_BIT_FUNC_WINDSENSOR`
- `0x00001000` `SUPLA_BIT_FUNC_PRESSURESENSOR`
- `0x00002000` `SUPLA_BIT_FUNC_RAINSENSOR`
- `0x00004000` `SUPLA_BIT_FUNC_WEIGHTSENSOR`
- `0x00008000` `SUPLA_BIT_FUNC_CONTROLLINGTHEROOFWINDOW`
- `0x00010000` `SUPLA_BIT_FUNC_CONTROLLINGTHEFACADEBLIND`
- `0x00020000` `SUPLA_BIT_FUNC_HVAC_THERMOSTAT`
- `0x00040000` `SUPLA_BIT_FUNC_HVAC_THERMOSTAT_HEAT_COOL`
- `0x00080000` `SUPLA_BIT_FUNC_HVAC_THERMOSTAT_DIFFERENTIAL`
- `0x00100000` `SUPLA_BIT_FUNC_HVAC_DOMESTIC_HOT_WATER`
- `0x00200000` `SUPLA_BIT_FUNC_TERRACE_AWNING`
- `0x00400000` `SUPLA_BIT_FUNC_PROJECTOR_SCREEN`
- `0x00800000` `SUPLA_BIT_FUNC_CURTAIN`
- `0x01000000` `SUPLA_BIT_FUNC_VERTICAL_BLIND`
- `0x02000000` `SUPLA_BIT_FUNC_ROLLER_GARAGE_DOOR`
- `0x04000000` `SUPLA_BIT_FUNC_PUMPSWITCH`
- `0x08000000` `SUPLA_BIT_FUNC_HEATORCOLDSOURCESWITCH`

`FuncList` can be 0 when a channel doesn’t advertise any optional/switchable
functions, or when the server filters it (e.g., action trigger channels).

## Appendix: RGBW and HVAC handling (server)
- RGBW_FuncList is defined in the protocol for dimmer/RGB channel types (ver. >= 28), but in the supla-core server codebase there is no use of it beyond the `proto.h` definition. Registration persists only `FuncList` (see `supla-core/supla-server/src/device/call_handler/abstract_register_device.cpp`).
- The protocol comment states that supported RGBW-related functions should be determined from `RGBW_FuncList`; the server does not enforce that and therefore relies on the device to keep `FuncList` consistent for RGBW channels.
- HVAC support bits exist in `FuncList`, but the supla-core server does not gate HVAC actions with those bits; HVAC behavior is driven by the channel function (`Func`) and HVAC value payloads. The only `FuncList` gate in the server is for `SUPLA_CHANNELTYPE_BRIDGE` when changing a channel function (see `supla-core/supla-server/src/user/user.cpp`).
- supla-cloud consumes `FuncList` to derive supported functions for channels (`supla-cloud/src/SuplaBundle/Enums/ChannelFunctionBitsFlist.php`).


## SUPLA_BIT_FUNC_* vs SUPLA_CHANNELFNC_*
- `SUPLA_BIT_FUNC_*` are bit flags used in `FuncList` to advertise a *set* of supported functions.
- `SUPLA_CHANNELFNC_*` are discrete numeric IDs describing a single assigned function for a channel (what it "is" at runtime).

## Parsing from TDS_SuplaDeviceChannel*
- Read `Default` (or `Func` in later variants) as the current channel function; it is one `SUPLA_CHANNELFNC_*` value.
- Read `FuncList` as a 32-bit bitmask; each set bit maps to a supported function (`SUPLA_BIT_FUNC_*`).
- Use the server-side mapping in `supla-core/supla-server/src/device/device.cpp` (`funclist_contains_function`) to test whether a `SUPLA_CHANNELFNC_*` is allowed by a `FuncList`.
- For RGBW/dimmer channels (proto ver >= 28), `RGBW_FuncList` exists separately in `supla-common/proto.h` and should drive RGBW-related support per protocol comments.
