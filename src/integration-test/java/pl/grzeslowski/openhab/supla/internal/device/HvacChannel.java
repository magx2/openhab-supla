package pl.grzeslowski.openhab.supla.internal.device;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.structs.HVACValue;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class HvacChannel {
    private boolean on;
    private HvacValue.Mode mode;

    @Nullable
    private BigDecimal setPointTemperatureHeat;

    @Nullable
    private BigDecimal setPointTemperatureCool;

    private HvacValue.Flags flags;

    public HVACValue toHvacValue() {
        return new HVACValue(
                (short) 1,
                (short) mode.getMask(),
                mapTemp(setPointTemperatureHeat),
                mapTemp(setPointTemperatureCool),
                flags.toInt());
    }

    private short mapTemp(@Nullable BigDecimal temp) {
        return Optional.ofNullable(temp)
                .map(t -> t.multiply(BigDecimal.valueOf(100)))
                .map(BigDecimal::shortValue)
                .orElse((short) 0);
    }
}
