package com.errorxcode.jxinsta;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.errorxcode.jxinsta.auth.AuthInfo;
import com.errorxcode.jxinsta.auth.LoginType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

public class Utils {
    private static final String ENCODING_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    protected static OkHttpClient client = new OkHttpClient().newBuilder().followRedirects(false).build();

    public static String getDeviceId() {
        return "android-" + Math.random();
    }

    public static Response call(Request request) throws IOException, InstagramException {
        try (Response res = client.newCall(request).execute()) {
            if (res.isSuccessful()) {
                return res;
            }

            String bodyString = res.body() != null ? res.body().string() : "";
            JSONObject jsonObject = new JSONObject(bodyString);
            String message = jsonObject.optString("message", "");

            switch (message) {
                case "checkpoint_required":
                    throw new InstagramException("Checkpoint required", InstagramException.Reason.CHECKPOINT_REQUIRED);
                case "Please wait a few minutes before you try again.":
                    throw new InstagramException("Rate limited", InstagramException.Reason.RATE_LIMITED);
                default:
                    if (message.startsWith("The password you entered is incorrect")) {
                        throw new InstagramException(message, InstagramException.Reason.INCORRECT_PASSWORD);
                    } else if (message.startsWith("The username you entered doesn't appear to belong to an account")) {
                        throw new InstagramException(message, InstagramException.Reason.INCORRECT_USERNAME);
                    } else if (message.startsWith("CSRF token missing or incorrect")) {
                        return handleCsrfError(request, res);
                    } else {
                        throw new InstagramException(message.isEmpty() ? "Unknown error" : message,
                                InstagramException.Reason.UNKNOWN);
                    }
            }
        }
    }

    private static Response handleCsrfError(Request originalRequest,
            Response response) throws IOException, InstagramException {
        final int MAX_RETRIES = 3;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String newCsrfToken = extractCsrfToken(response);
            if (newCsrfToken == null) {
                throw new InstagramException("Unable to extract CSRF token",
                        InstagramException.Reason.CSRF_AUTHENTICATION_FAILED);
            }

            Request newRequest = originalRequest.newBuilder()
                    .removeHeader("x-csrftoken")
                    .addHeader("x-csrftoken", newCsrfToken)
                    .build();

            try (Response retryResponse = client.newCall(newRequest).execute()) {
                if (retryResponse.isSuccessful()) {
                    return retryResponse;
                }

                String bodyString = retryResponse.body() != null ? retryResponse.body().string() : "";
                JSONObject jsonObject = new JSONObject(bodyString);
                String message = jsonObject.optString("message", "");

                if (!message.startsWith("CSRF token missing or incorrect")) {
                    throw new InstagramException(message.isEmpty() ? "Unknown error" : message,
                            InstagramException.Reason.UNKNOWN);
                }
                response = retryResponse;
            }
        }

        throw new InstagramException(
                "CSRF authentication failed after multiple attempts. Instagram may have blacklisted your device or IP.",
                InstagramException.Reason.CSRF_AUTHENTICATION_FAILED
        );
    }

    private static String extractCsrfToken(Response response) {
        for (String header : response.headers("set-cookie")) {
            if (header.startsWith("csrftoken=")) {
                String[] parts = header.split(";", 2);
                if (parts.length > 0) {
                    String[] tokenPair = parts[0].split("=", 2);
                    if (tokenPair.length == 2) {
                        return tokenPair[1];
                    }
                }
            }
        }
        return null;
    }

    public static String generateJazoest(String input) {
        int total = 0;
        for (char c : input.toCharArray()) {
            total += c;
        }
        return "2" + total;
    }

    public static String extractCsrfToken(String cookieString) {
        String[] cookies = cookieString.split(";");
        for (String cookie : cookies) {
            String[] keyValue = cookie.trim().split("=", 2);
            if (keyValue.length == 2 && keyValue[0].equals("csrftoken")) {
                return keyValue[1];
            }
        }
        return null;
    }

    protected static String getCSRF() throws IOException {
        Request request = new Request.Builder()
                .url("https://www.instagram.com/")
                .addHeader("user-agent", Constants.WEB_USER_AGENT)
                .addHeader("accept", "*/*")
                .addHeader("accept-language", "en-US,en;q=0.9")
                .addHeader("x-requested-with", "XMLHttpRequest")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            List<String> cookies = response.headers("Set-Cookie");

            for (String cookie : cookies) {
                if (cookie.startsWith("csrftoken=")) {
                    return cookie.split(";")[0].split("=")[1];
                }
            }
            throw new IOException("csrftoken not found in cookies");
        }
    }

    public static String extractCookie(List<String> headers) {
        StringBuilder cookie = new StringBuilder();
        for (String header : headers) {
            var crsf = header.split(";")[0];
            cookie.append(crsf).append(";");
        }
        return cookie.toString().replaceFirst(".$", "");
    }

    public static Response graphql(@NotNull String queryId,
            @NotNull Map<String, Object> params,
            @Nullable String authorization) throws IOException, InstagramException {
        var furl = Constants.GraphQl.BASE_URL + queryId + "&" + params.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return call(new Request.Builder()
                .url(furl)
                .headers(Headers.of(Constants.BASE_HEADERS))
                .addHeader(authorization == null ? "origin" : "cookie",
                        authorization == null ? "https://instagram.com" : authorization)
                .addHeader("user-agent", Constants.WEB_USER_AGENT)
                .get().build());
    }

    public static Request createPublicRequest(String endpoint) {
        return new Request.Builder()
                .url(Constants.BASE_URL + endpoint)
                .headers(Headers.of(Constants.BASE_HEADERS))
                .addHeader("user-agent", Constants.MOBILE_USER_AGENT)
                .get()
                .build();
    }

    public static Request createGetRequest(String endpoint, AuthInfo info) {
        boolean isDesktop = info.loginType() == LoginType.BOTH_WEB_AND_APP_AUTHENTICATION || info.loginType() == LoginType.WEB_AUTHENTICATION;
        return new Request.Builder()
                .url(Constants.BASE_URL + endpoint)
                .headers(Headers.of(Constants.BASE_HEADERS))
                .addHeader(isDesktop ? "cookie" : "authorization", info.authorization())
                .addHeader(isDesktop ? "x-csrftoken" : "null", isDesktop ? info.crsf() : "null")
                .addHeader("user-agent", isDesktop ? Constants.WEB_USER_AGENT : Constants.MOBILE_USER_AGENT)
                .get().build();
    }

    public static Request createPostRequest(@NotNull AuthInfo authInfo, String endpoint, Object body) {
        boolean isDesktop = authInfo.loginType() == LoginType.BOTH_WEB_AND_APP_AUTHENTICATION ||
                authInfo.loginType() == LoginType.WEB_AUTHENTICATION;

        Request.Builder req = new Request.Builder()
                .url(Constants.BASE_URL + endpoint)
                .headers(Headers.of(Constants.BASE_HEADERS_WITHOUT_CONTENT_TYPE))
                .addHeader(isDesktop ? "cookie" : "authorization", authInfo.authorization())
                .addHeader(isDesktop ? "x-csrftoken" : "null", isDesktop ? authInfo.crsf() : "na")
                .addHeader("user-agent", isDesktop ? Constants.WEB_USER_AGENT : Constants.MOBILE_USER_AGENT);

        if (body == null) {
            req.addHeader("content-type", "application/x-www-form-urlencoded");
            req.method("POST", RequestBody.create(new byte[0]));
            return req.build();
        } else if (body instanceof Map map) {
            FormBody.Builder form = new FormBody.Builder();
            map.forEach((key, value) -> {
                form.addEncoded(key.toString(), value.toString());
            });

            req.addHeader("content-type", "application/x-www-form-urlencoded");
            req.method("POST", form.build());
        } else if (body instanceof JSONObject) {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            req.addHeader("content-type", "application/json");
            req.method("POST", RequestBody.create(body.toString(), JSON));
        } else {
            throw new IllegalArgumentException("Unsupported body type: " + body.getClass().getName());
        }

        return req.build();
    }


    public static Request injectAppId(Request request) {
        return request.newBuilder().addHeader("x-ig-app-id", Constants.X_APP_ID).build();
    }

    public static List<String> uploadPictures(@NotNull String token, File... pictures) {
        long timestamp = System.currentTimeMillis();
        ArrayList<String> medias = new ArrayList<>();
        for (File picture : pictures) {
            try (InputStream in = new FileInputStream(picture)) {
                String uploadId = Utils.uploadPicture(in, token, timestamp);
                timestamp += 1;
                medias.add(uploadId);
            } catch (IOException e) {
                System.err.println("Error uploading the picture: " + picture.getName());
                e.printStackTrace();
            }
        }
        return medias;
    }

    public static String uploadPicture(@NotNull InputStream in, @NotNull String token) throws IOException {
        return uploadPicture(in, token, System.currentTimeMillis());
    }

    public static String uploadPicture(@NotNull InputStream in, @NotNull String token, long ts) throws IOException {
        var uploadId = Long.toString(ts);
        byte[] bytes = new byte[in.available()];
        in.read(bytes);

        var isCookie = token.contains("sessionid");

        var uploadReq = new Request.Builder()
                .url("https://i.instagram.com/rupload_igphoto/" + "fb_uploader_" + uploadId)
                .post(RequestBody.create(bytes))
                .headers(Headers.of(Constants.BASE_HEADERS))
                .addHeader("x-instagram-rupload-params", "{\"media_type\":1,\"upload_id\":\"" + uploadId + "\"}")
                .addHeader("x-entity-length", String.valueOf(bytes.length))
                .addHeader("x-entity-name", "fb_uploader_" + uploadId)
                .addHeader("x-entity-type", "image/jpeg")
                .addHeader("offset", String.valueOf(0))
                .removeHeader("content-type")
                .addHeader(isCookie ? "cookie" : "authorization", token)
                .addHeader("content-type", "application/octet-stream");

        var res = client.newCall(uploadReq.build()).execute();
        if (res.isSuccessful()) {
            return uploadId;
        } else {
            System.out.println(res.body().string());
            throw new IOException("Failed to upload picture");
        }
    }

    public static String map2query(Map<String, Object> map) {
        return map.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&"));
    }

    public static long shortCode2id(@NotNull String shortCode) {
        int base = ENCODING_CHARS.length();
        int strlen = shortCode.length();
        long num = 0;
        for (int i = 0; i < strlen; i++) {
            char charAtI = shortCode.charAt(i);
            int power = strlen - (i + 1);
            num += ENCODING_CHARS.indexOf(charAtI) * Math.pow(base, power);
        }
        return num;
    }

    public static String id2shortCode(long id) {
        int base = ENCODING_CHARS.length();
        StringBuilder builder = new StringBuilder();
        while (id > 0) {
            builder.append(ENCODING_CHARS.charAt((int) (id % base)));
            id /= base;
        }
        return builder.reverse().toString();
    }
}
