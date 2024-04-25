package pl.grzeslowski.supla.openhab.internal.server.handler;

import lombok.NonNull;
import org.eclipse.jdt.annotation.Nullable;

record AuthData(@Nullable LocationAuthData locationAuthData, @Nullable EmailAuthData emailAuthData) {

    public AuthData {
        if (locationAuthData == null && emailAuthData == null) {
            throw new IllegalArgumentException("AuthData must have at least one value");
        }
    }

    public static record LocationAuthData(int serverAccessId, @NonNull String serverAccessIdPassword) {}

    public static record EmailAuthData(@NonNull String email) {}
}
