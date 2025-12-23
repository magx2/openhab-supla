package pl.grzeslowski.openhab.supla.internal.extension.random;

import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_GUID_SIZE;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.SUPLA_LOCATION_PWD_MAXSIZE;

import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex;

@Slf4j
public class RandomExtension implements ParameterResolver {
    public static RandomExtension INSTANCE;
    private static final String ALPHANUM = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final Random random;

    public RandomExtension(long seed) {
        log.info("Starting random extension with seed {}", seed);
        random = new Random(seed);
        INSTANCE = this;
    }

    public RandomExtension() {
        this(System.currentTimeMillis());
    }

    public String randomGuid() {
        var bytes = new byte[SUPLA_GUID_SIZE];
        random.nextBytes(bytes);
        return ByteArrayToHex.bytesToHex(bytes);
    }

    public String randomLocationPassword() {
        return randomString(random.nextInt(SUPLA_LOCATION_PWD_MAXSIZE) + 1);
    }

    public String randomString() {
        return randomString(random.nextInt(5) + 5);
    }

    public String randomString(int length) {
        var sb = new StringBuilder(length);
        for (var idx = 0; idx < length; idx++) {
            sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }

    public boolean randomBool() {
        return random.nextBoolean();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.isAnnotated(Guid.class)) {
            return parameterContext.getParameter().getType() == String.class;
        }
        if (parameterContext.isAnnotated(Port.class)) {
            return parameterContext.getParameter().getType() == int.class;
        }
        if (parameterContext.isAnnotated(LocationPassword.class)) {
            return parameterContext.getParameter().getType() == String.class;
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (parameterContext.isAnnotated(Guid.class)) {
            return randomGuid();
        }
        if (parameterContext.isAnnotated(Port.class)) {
            return random.nextInt(0xFFFF) + 1;
        }
        if (parameterContext.isAnnotated(LocationPassword.class)) {
            return randomLocationPassword();
        }
        return null;
    }
}
