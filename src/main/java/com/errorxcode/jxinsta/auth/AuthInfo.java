package com.errorxcode.jxinsta.auth;

import org.jetbrains.annotations.NotNull;

public record AuthInfo(
        String csrf,
        String token,
        String cookie,
        String authorization,
        LoginType loginType
) {

    public static AuthInfo forMobile(@NotNull String bearerToken) {
        return new AuthInfo(bearerToken, bearerToken, null, bearerToken, LoginType.APP_AUTHENTICATION);
    }
}
