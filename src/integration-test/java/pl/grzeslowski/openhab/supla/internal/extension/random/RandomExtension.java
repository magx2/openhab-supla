package pl.grzeslowski.openhab.supla.internal.extension.random;

import static java.math.RoundingMode.HALF_UP;
import static java.util.Locale.ROOT;
import static java.util.stream.Stream.generate;
import static pl.grzeslowski.jsupla.protocol.api.JavaConsts.UNSIGNED_BYTE_MAX;
import static pl.grzeslowski.jsupla.protocol.api.consts.ProtoConsts.*;
import static pl.grzeslowski.openhab.supla.internal.server.ByteArrayToHex.bytesToHex;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.HvacValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.PercentValue;
import pl.grzeslowski.jsupla.protocol.api.channeltype.value.RgbValue;
import pl.grzeslowski.openhab.supla.internal.device.HvacChannel;

@Slf4j
public class RandomExtension implements ParameterResolver {
    public static RandomExtension INSTANCE;
    private static final String ALPHANUM = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String[] DOMAINS = {"gmail.com", "outlook.com", "icloud.com", "proton.me", "fastmail.com"};
    private static final String[] WORDS = {"dev", "io", "ai", "lab", "ops", "fx", "pm", "cloud", "code", "tech"};
    private final Random random;

    public RandomExtension(long seed) {
        log.info("Starting random extension with seed {}", seed);
        random = new Random(seed);
        INSTANCE = this;
    }

    public RandomExtension() {
        this(System.currentTimeMillis());
    }

    public BigDecimal randomTemperature() {
        return BigDecimal.valueOf(random.nextDouble())
                .multiply(BigDecimal.valueOf(20))
                .add(BigDecimal.valueOf(15))
                .setScale(14, HALF_UP);
    }

    public BigDecimal randomUpdateTemperature(BigDecimal temperature) {
        var offset = BigDecimal.ZERO;
        while (offset.compareTo(BigDecimal.ZERO) == 0) {
            offset = temperature.multiply(BigDecimal.valueOf(random.nextGaussian()));
        }
        return temperature.add(offset).setScale(14, HALF_UP);
    }

    public HvacChannel randomHvac() {
        return new HvacChannel(
                true,
                HvacValue.Mode.HEAT,
                randomTemperature(),
                null,
                new HvacValue.Flags(
                        true, false, false, false, false, false, false, false, false, false, false, false, false));
    }

    private String randomEmail() {
        String base = randomString(2 + random.nextInt(4));
        String local;

        int style = random.nextInt(4);
        switch (style) {
            case 0 -> local = base;
            case 1 -> local = base + "." + randomString(1);
            case 2 -> local = base + "." + WORDS[random.nextInt(WORDS.length)];
            default -> local = base + (10 + random.nextInt(90));
        }

        String domain = DOMAINS[random.nextInt(DOMAINS.length)];
        return local.toLowerCase(ROOT) + "@" + domain;
    }

    private String randomAuthKey() {
        var bytes = new byte[SUPLA_AUTHKEY_SIZE];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    public String randomGuid() {
        var bytes = new byte[SUPLA_GUID_SIZE];
        random.nextBytes(bytes);
        return bytesToHex(bytes);
    }

    public PercentValue randomPercentage() {
        return new PercentValue(random.nextInt(101));
    }

    public RgbValue randomRgbValue() {
        return new RgbValue(
                randomPercentage().value(),
                randomPercentage().value(),
                random.nextInt(UNSIGNED_BYTE_MAX + 1),
                random.nextInt(UNSIGNED_BYTE_MAX + 1),
                random.nextInt(UNSIGNED_BYTE_MAX + 1));
    }

    public HSBType randomHsb() {
        return new HSBType(
                new DecimalType(random.nextInt(361)),
                new PercentType(random.nextInt(101)),
                new PercentType(random.nextInt(101)));
    }

    public PercentValue randomPercentage(PercentValue value) {
        return generate(this::randomPercentage)
                .filter(p -> !p.equals(value))
                .parallel()
                .findAny()
                .orElseThrow();
    }

    private int randomLocationId() {
        return random.nextInt(1_000);
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

    private int openPort() {
        try (var serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException exception) {
            throw new ParameterResolutionException("Could not allocate open port", exception);
        }
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
        if (parameterContext.isAnnotated(LocationId.class)) {
            return parameterContext.getParameter().getType() == int.class;
        }
        if (parameterContext.isAnnotated(LocationPassword.class)) {
            return parameterContext.getParameter().getType() == String.class;
        }
        if (parameterContext.isAnnotated(Email.class)) {
            return parameterContext.getParameter().getType() == String.class;
        }
        if (parameterContext.isAnnotated(AuthKey.class)) {
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
            return openPort();
        }
        if (parameterContext.isAnnotated(LocationId.class)) {
            return randomLocationId();
        }
        if (parameterContext.isAnnotated(LocationPassword.class)) {
            return randomLocationPassword();
        }
        if (parameterContext.isAnnotated(Email.class)) {
            return randomEmail();
        }
        if (parameterContext.isAnnotated(AuthKey.class)) {
            return randomAuthKey();
        }
        return null;
    }
}
