package com.errorxcode.jxinsta.auth;

import org.jetbrains.annotations.NotNull;

public record AuthInfo(
        String crsf,
        String token,
        String cookie,
        String authorization,
        LoginType loginType
) {

    public static AuthInfo forMobile(@NotNull String bearerToken) {
        return new AuthInfo(bearerToken, bearerToken, null, bearerToken, LoginType.APP_AUTHENTICATION);
    }

    public static class AuthInfoBuilder {
        private String crsf;
        private String token;
        private String cookie;
        private String authorization;
        private LoginType loginType;

        public AuthInfoBuilder setCrsf(String crsf) {
            this.crsf = crsf;
            return this;
        }

        public AuthInfoBuilder setToken(String token) {
            this.token = token;
            return this;
        }

        public AuthInfoBuilder setCookie(String cookie) {
            this.cookie = cookie;
            return this;
        }

        public AuthInfoBuilder setAuthorization(String authorization) {
            this.authorization = authorization;
            return this;
        }

        public AuthInfoBuilder setLoginType(LoginType loginType) {
            this.loginType = loginType;
            return this;
        }

        public AuthInfo build() {
            return new AuthInfo(crsf, token, cookie, authorization, loginType);
        }

        public String getCrsf() {
            return crsf;
        }

        public String getToken() {
            return token;
        }

        public String getCookie() {
            return cookie;
        }

        public String getAuthorization() {
            return authorization;
        }

        public LoginType getLoginType() {
            return loginType;
        }
    }

}
