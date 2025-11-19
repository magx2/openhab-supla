package pl.grzeslowski.openhab.supla.internal;

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
}
