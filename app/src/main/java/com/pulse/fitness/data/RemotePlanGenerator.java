package com.pulse.fitness.data;

import com.google.gson.*;
import com.pulse.core.model.*;
import com.pulse.core.plan.*;
import com.pulse.core.util.JsonCodec;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class RemotePlanGenerator extends PlanGenerationTemplate {
    private final String endpoint, token;

    public RemotePlanGenerator(String endpoint, String token) {
        this.endpoint = endpoint;
        this.token = token;
    }

    @Override
    protected WeekPlan createCandidate(UserProfile p) throws Exception {
        JsonObject payload =
                JsonParser.parseString(
                                request(endpoint + "/api/v1/plans", token, JsonCodec.profile(p)))
                        .getAsJsonObject();
        return JsonCodec.plan(payload.get("plan").toString());
    }

    public static String health(String endpoint, String token) throws Exception {
        return request(endpoint + "/health", token, null);
    }

    private static String request(String url, String token, String body) throws Exception {
        return request(url, token, body, body == null ? 10000 : 175000);
    }

    static String request(String url, String token, String body, int readTimeout) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(readTimeout);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(bytes);
                }
            }
            int code = connection.getResponseCode();
            InputStream input =
                    code / 100 == 2 ? connection.getInputStream() : connection.getErrorStream();
            if (input == null) throw new IOException("服务未返回数据（" + code + "）");
            byte[] bytes;
            try (InputStream in = input;
                    ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    if (out.size() + count > 262144) throw new IOException("返回数据过大");
                    out.write(buffer, 0, count);
                }
                bytes = out.toByteArray();
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (code / 100 != 2) {
                String message = "服务请求失败（" + code + "）";
                try {
                    message =
                            JsonParser.parseString(text)
                                    .getAsJsonObject()
                                    .get("error")
                                    .getAsString();
                } catch (RuntimeException ignored) {
                }
                throw new IOException(message);
            }
            return text;
        } finally {
            connection.disconnect();
        }
    }

    public static final class Factory extends PlanGeneratorFactory {
        private final String endpoint, token;

        public Factory(String endpoint, String token) {
            this.endpoint = endpoint;
            this.token = token;
        }

        @Override
        protected PlanGenerator createGenerator() {
            return new RemotePlanGenerator(endpoint, token);
        }
    }
}
