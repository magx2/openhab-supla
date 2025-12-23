package pl.grzeslowski.openhab.supla.internal.extension.supla;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CreateHandler {
    String thingTypeId();
}
