package pl.grzeslowski.openhab.supla.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.MDC;

@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public interface GuidLogger {
    static final String GUID_KEY = "GUID";

    static void attachGuid(@Nullable String guid, Runnable runnable) {
        if (guid != null) MDC.put(GUID_KEY, guid);
        try {
            runnable.run();
        } finally {
            if (guid != null) MDC.remove(GUID_KEY);
        }
    }

    static void attachGuidX(@Nullable String guid, ThrowingRunnable runnable) throws Exception {
        if (guid != null) MDC.put(GUID_KEY, guid);
        try {
            runnable.run();
        } finally {
            if (guid != null) MDC.remove(GUID_KEY);
        }
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.METHOD)
    @interface GuidLogged {}
}
