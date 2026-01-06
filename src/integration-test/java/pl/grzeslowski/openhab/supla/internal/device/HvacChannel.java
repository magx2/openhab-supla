package pl.grzeslowski.openhab.supla.internal.device;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.grzeslowski.jsupla.protocol.api.HvacFlag;
import pl.grzeslowski.jsupla.protocol.api.HvacMode;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class HvacChannel {
    private boolean on;
    private HvacMode mode;

    @Nullable
    private BigDecimal setPointTemperatureHeat;

    @Nullable
    private BigDecimal setPointTemperatureCool;

    private Set<HvacFlag> flags;

    public HvacChannel(HvacValue hvac) {
        this.on = hvac.on();
        this.mode = hvac.mode();
        this.setPointTemperatureHeat = hvac.setPointTemperatureHeat();
        this.setPointTemperatureCool = hvac.setPointTemperatureCool();
        this.flags = hvac.flags();
    }

    public HVACValue toHvacValue() {
        return new HVACValue(
                (short) 1,
                (short) mode.getValue(),
                mapTemp(setPointTemperatureHeat),
                mapTemp(setPointTemperatureCool),
                (int) HvacFlag.toMask(flags));
    }

    private short mapTemp(@Nullable BigDecimal temp) {
        return Optional.ofNullable(temp)
                .map(t -> t.multiply(BigDecimal.valueOf(100)))
                .map(BigDecimal::shortValue)
                .orElse((short) 0);
    }
}
