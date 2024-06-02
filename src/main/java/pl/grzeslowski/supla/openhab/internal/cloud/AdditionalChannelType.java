package pl.grzeslowski.supla.openhab.internal.cloud;

public enum AdditionalChannelType {
    LED_BRIGHTNESS("_brightness"),
    EXTRA_LIGHT_ACTIONS("_extra_light_actions"),
    TEMPERATURE("_temp"),
    HUMIDITY("_humidity"),
    TOTAL_COST("_total_cost"),
    PRICE_PER_UNIT("_price_per_unit"),
    FREQUENCY("_frequency"),
    VOLTAGE("_voltage"),
    CURRENT("_current"),
    POWER_ACTIVE("_power_active"),
    POWER_REACTIVE("_power_reactive"),
    POWER_APPARENT("_power_apparent"),
    POWER_FACTOR("_power_factor"),
    PHASE_ANGLE("_phase_angle"),
    TOTAL_FORWARD_ACTIVE_ENERGY("_total_forward_active_energy"),
    TOTAL_REVERSED_ACTIVE_ENERGY("_total_reversed_active_energy"),
    TOTAL_FORWARD_REACTIVE_ENERGY("_total_forward_reactive_energy"),
    TOTAL_REVERSED_REACTIVE_ENERGY("_total_reversed_reactive_energy");

    private final String suffix;

    AdditionalChannelType(final String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }
}
