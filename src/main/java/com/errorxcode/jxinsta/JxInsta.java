package com.errorxcode.jxinsta;

import com.errorxcode.jxinsta.auth.AuthInfo;
import com.errorxcode.jxinsta.auth.AuthenticationType;
import com.errorxcode.jxinsta.auth.LoginType;
import com.errorxcode.jxinsta.endpoints.DirectMessage;
import com.errorxcode.jxinsta.endpoints.Post;
import com.errorxcode.jxinsta.endpoints.Profile;
import com.errorxcode.jxinsta.endpoints.Story;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.errorxcode.jxinsta.Constants.MAX_CAROUSEL_LENGTH;

public class JxInsta {
    private static final Logger LOGGER = Logger.getLogger(JxInsta.class.getName());
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().followRedirects(false).build();
    private final AuthInfo authInfo;

    public AuthInfo getAuthInfo() {
        return authInfo;
    }

    public JxInsta(String username, String password, LoginType loginType) {
        AuthInfo.AuthInfoBuilder infoBuilder = new AuthInfo.AuthInfoBuilder();

        String encodedPassword = "#PWD_INSTAGRAM_BROWSER:0:" + new Date().getTime() + ":" + password;
        FormBody body = new FormBody.Builder()
                .add("enc_password", encodedPassword)
                .add("username", username)
                .add("optIntoOneTap", "false")
                .add("queryParams", "{}")
                .add("trustedDeviceRecords", "{}")
                .build();

        Request.Builder builder = new Request.Builder()
                .url(Constants.LOGIN_URL)
                .method("POST", body)
                .headers(Headers.of(Constants.BASE_HEADERS));

        switch (loginType) {
            case APP_AUTHENTICATION -> createAppAuthentication(builder);
            case WEB_AUTHENTICATION -> createWebAuthentication(builder);
            default -> throw new IllegalStateException("Unexpected value: " + loginType);
        }

        Request authenticationRequest = builder.build();
        Response authenticationResponse;
        String responseBodyString = "";
        boolean successful = false;
        try {
            authenticationResponse = HTTP_CLIENT.newCall(authenticationRequest).execute();
            if (authenticationResponse.isSuccessful()) {
                responseBodyString = authenticationResponse.body().string();
                successful = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JSONObject jsonObject = new JSONObject(responseBodyString);
        String status = jsonObject.optString("status", "");
        String message = jsonObject.optString("message", "");
        String errorType = jsonObject.optString("error_type", "");

        if (!successful) {
            throw new RuntimeException("Autenticazione con Instagram fallita");
        }

        if ("ok".equalsIgnoreCase(status) && jsonObject.optBoolean("user", false) && !jsonObject.optBoolean(
                "authenticated",
                true)) {
            throw new RuntimeException("Autenticazione con Instagram fallita");
        }

        if ("fail".equalsIgnoreCase(status)) {
            String errorMessage = !message.isEmpty() ? message : errorType;
            throw new RuntimeException(errorMessage);
        }


        switch (loginType) {
            case WEB_AUTHENTICATION ->
                    infoBuilder.setCookie(Utils.extractCookie(authenticationResponse.headers("set-cookie")))
                            .setAuthorization(infoBuilder.getCookie())
                            .setCrsf(Utils.extractCsrfToken(infoBuilder.getCookie()));
            case APP_AUTHENTICATION -> infoBuilder.setToken(authenticationResponse.header("ig-set-authorization"))
                    .setAuthorization(infoBuilder.getToken());
            default -> LOGGER.warning("Unrecognized login type: " + loginType);
        }

        authInfo = infoBuilder.build();
        authenticationResponse.close();
    }

    private void createAppAuthentication(Request.Builder builder) {
        builder.addHeader("user-agent", Constants.MOBILE_USER_AGENT);
    }

    private void createWebAuthentication(Request.Builder builder) {
        String csrf;
        try {
            csrf = Utils.getCSRF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        builder.addHeader("x-csrftoken", csrf);
        builder.addHeader("user-agent", Constants.WEB_USER_AGENT);
    }

    public Profile getProfile(String username) throws IOException, InstagramException {
        return new Profile(authInfo, username);
    }

    @AuthenticationType(value = AuthenticationType.Method.WEB_AUTH)
    public List<Post> getFeedPosts(int count, @Nullable String cursor) throws IOException, InstagramException {
        var params = new HashMap<String, Object>();
        params.put("fetch_media_item_count", count);
        params.put("fetch_media_item_cursor", cursor == null ? "" : cursor);
        params.put("fetch_comment_count", 0);
        params.put("fetch_like", 0);
        params.put("has_stories", false);
        params.put("has_threaded_comments", false);

        try (var res = Utils.graphql("17842794232208280", params, authInfo.authorization())) {
            JSONObject json = new JSONObject(res.body().string());
            JSONObject timeline = json.getJSONObject("data")
                    .getJSONObject("user")
                    .getJSONObject("edge_web_feed_timeline");
            JSONArray posts = timeline.getJSONArray("edges");
            List<Post> list = new ArrayList<>();

            for (int i = 0; i < posts.length(); i++) {
                JSONObject postNode = posts.getJSONObject(i).getJSONObject("node");
                Post post = new Post(authInfo, postNode.getLong("id"));

                post.isVideo = postNode.getBoolean("is_video");
                post.caption = postNode.has("edge_media_to_caption") ? postNode.getJSONObject("edge_media_to_caption")
                        .getJSONArray("edges")
                        .getJSONObject(0)
                        .getJSONObject("node")
                        .getString("text") : "";

                post.likes = postNode.getJSONObject("edge_media_preview_like").getInt("count");
                post.comments = postNode.getJSONObject("edge_media_to_comment").getInt("count");
                post.shortcode = postNode.getString("shortcode");
                post.download_url = postNode.getString("display_url");
                post.next_cursor = timeline.getJSONObject("page_info").getString("end_cursor");
                list.add(post);
            }
            return list;
        }
    }

    public List<Story[]> getFeedStories() throws InstagramException, IOException {
        List<Story[]> list = new ArrayList<>();
        Request request = Utils.createGetRequest("feed/reels_tray/?is_following_feed=false", authInfo);
        List<Story> stories = new ArrayList<>();


        try (Response response = Utils.call(request)) {
            JSONArray users = new JSONObject(response.body().string()).getJSONArray("tray");
            for (int i = 0; i < users.length(); i++) {
                long userStoryId = users.getJSONObject(i).getLong("id");
                List<Story> actualStory = Story.getStoryFromId(String.valueOf(userStoryId), authInfo);
                if (actualStory == null || actualStory.isEmpty()) continue;
                list.add(stories.toArray(new Story[0]));
            }

        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        return list;
    }

    public void postPictures(@NotNull String caption,
            boolean disableInteractions,
            File... files) throws IOException, InstagramException {
        if (files.length > MAX_CAROUSEL_LENGTH) {
            throw new InstagramException("You can post only 10 files maximum in one post",
                    InstagramException.Reason.TOO_MUCH_PICTURES);
        }

        List<String> mediaIds = Utils.uploadPictures(authInfo.cookie() != null ? authInfo.cookie() : authInfo.token(),
                files);

        JSONArray mediaIdsJson = new JSONArray();
        for (String mediaId : mediaIds) {
            JSONObject obj = new JSONObject();
            obj.put("upload_id", mediaId);
            mediaIdsJson.put(obj);
        }

        System.out.print("LOADED mediaIdsJson:" + mediaIdsJson);
        JSONObject body = new JSONObject();

        body.put("archive_only", false);
        body.put("caption", caption);
        body.put("children_metadata", mediaIdsJson);
        body.put("client_sidecar_id", String.valueOf(System.currentTimeMillis()));
        body.put("disable_comments", disableInteractions ? "1" : "0");
        body.put("is_meta_only_post", false);
        body.put("is_open_to_public_submission", false);
        body.put("jazoest", Utils.generateJazoest(authInfo.crsf()));
        body.put("like_and_view_counts_disabled", disableInteractions ? 1 : 0);
        body.put("media_share_flow", "creation_flow");
        body.put("share_to_facebook", "");
        body.put("share_to_fb_destination_type", "USER");
        body.put("source_type", "library");

        Request req = Utils.createPostRequest(authInfo, "media/configure_sidecar/", body);
        req = req.newBuilder().addHeader("x-ig-app-id", "936619743392459").build();

        try (Response res = Utils.call(req)) {
            JSONObject json = new JSONObject(res.body().string());
            if (json.getString("status").equals("ok")) {
                System.out.println("Post successful");
            } else {
                LOGGER.log(Level.SEVERE, json.getString("message"), json);
            }
        }
    }

    public void postPicture(InputStream inputStream,
            String caption,
            boolean disableInteractions) throws IOException, InstagramException {

        String id = Utils.uploadPicture(inputStream,
                authInfo.cookie() != null ? authInfo.cookie() : authInfo.token(),
                System.currentTimeMillis());

        HashMap<String, Object> body = new HashMap<>();
        body.put("caption", caption);
        body.put("upload_id", id);
        body.put("archive_only", "false");
        body.put("clips_share_preview_to_feed", "1");
        body.put("disable_comments", disableInteractions ? "1" : "0");
        body.put("igtv_share_preview_to_feed", "1");
        body.put("is_unified_video", "1");
        body.put("like_and_view_counts_disabled", disableInteractions ? "1" : "0");
        body.put("source_type", "library");
        body.put("video_subtitles_enabled", "0");

        Request req = Utils.createPostRequest(authInfo, "media/configure/", body);
        req = req.newBuilder().addHeader("x-ig-app-id", "936619743392459").build();

        try (Response res = Utils.call(req)) {
            JSONObject json = new JSONObject(res.body().string());
            if (json.getString("status").equals("ok")) {
                LOGGER.info("Post successful");
            } else {
                LOGGER.log(Level.SEVERE, json.getString("message"), json);
            }
        }
    }

    @AuthenticationType(value = AuthenticationType.Method.MOBILE_AUTH)
    public void uploadStory(@NotNull File photo) throws IOException {
        if (authInfo.token() == null) {
            LOGGER.severe("Story upload is only supported for mobile authenticated users");
            return;
        }

        var extension = photo.getName().substring(photo.getName().lastIndexOf("."));
        if (!(extension.equals(".jpg") || extension.equals(".jpeg") || extension.equals(".png"))) {
            throw new IllegalArgumentException("Invalid file type. Only videos are supported");
        }

        var uploadId = Utils.uploadPicture(new FileInputStream(photo), authInfo.token(), System.currentTimeMillis());
        System.out.println(uploadId);
        var body = new HashMap<String, Object>();
        body.put("upload_id", uploadId);
        body.put("original_media_type", "photo");
        body.put("configure_mode", 1);
        body.put("creation_surface", "camera");
        body.put("has_original_sound", 1);
        body.put("capture_type", "normal");

        var req = Utils.createPostRequest(AuthInfo.forMobile(authInfo.token()), "media/configure_to_story/", body);
        try (Response res = HTTP_CLIENT.newCall(req).execute()) {
            if (!res.isSuccessful()) {
                return;
            }
            String bodyString = res.body().string();
            JSONObject json = new JSONObject(bodyString);
            if (json.getString("status").equals("ok")) {
                System.out.println("Story uploaded successfully");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @AuthenticationType(value = AuthenticationType.Method.MOBILE_AUTH)
    public static DirectMessage instagramDirect(@NotNull String bearerToken) {
        return new DirectMessage(AuthInfo.forMobile(bearerToken));
    }

    @AuthenticationType(value = AuthenticationType.Method.MOBILE_AUTH)
    public DirectMessage directMessaging() {
        if (authInfo.token() == null) {
            throw new IllegalArgumentException(
                    "Direct messaging is only supported for mobile authenticated users. Make sure that you have Bearer token");
        }

        return new DirectMessage(AuthInfo.forMobile(authInfo.token()));
    }

    @AuthenticationType(value = AuthenticationType.Method.WEB_AUTH)
    public Map<String, String> search(@NotNull String username) throws InstagramException, IOException {
        var req = Utils.createGetRequest("web/search/topsearch/?context=user&count=0&query=" + username, authInfo);
        try (var res = Utils.call(req)) {
            var json = new JSONObject(res.body().string());
            var users = json.getJSONArray("users");
            var map = new HashMap<String, String>();
            for (int i = 0; i < users.length(); i++) {
                var user = users.getJSONObject(i).getJSONObject("user");
                map.put(user.getString("username"), user.getString("profile_pic_url"));
            }
            return map;
        }
    }
}