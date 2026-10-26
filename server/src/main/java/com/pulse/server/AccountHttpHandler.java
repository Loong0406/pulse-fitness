package com.pulse.server;

import com.google.gson.*;
import com.pulse.core.util.JsonCodec;
import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/** Gateway-token protected demo endpoints; no production SMS/session authentication is claimed. */
final class AccountHttpHandler implements HttpHandler {
    private final DemoAccountService accounts;

    AccountHttpHandler(DemoAccountService accounts) {
        this.accounts = accounts;
    }

    public void handle(HttpExchange e) throws IOException {
        try {
            String path = e.getRequestURI().getPath();
            if (!path.equals("/api/v1/auth/demo-code") && !path.equals("/api/v1/auth/demo-login")) {
                reply(e, 404, "接口不存在");
                return;
            }
            if (!e.getRequestMethod().equals("POST")) {
                reply(e, 405, "仅支持 POST");
                return;
            }
            if (accounts == null) {
                reply(e, 503, "数据库登录尚未配置或未开启演示验证码，请使用游客体验");
                return;
            }
            String type = e.getRequestHeaders().getFirst("Content-Type");
            if (type == null
                    || !type.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
                reply(e, 415, "请求需为 JSON");
                return;
            }
            byte[] body = e.getRequestBody().readNBytes(4097);
            if (body.length > 4096) {
                reply(e, 413, "请求过大");
                return;
            }
            JsonObject input =
                    JsonParser.parseString(new String(body, StandardCharsets.UTF_8))
                            .getAsJsonObject();
            String phone = JsonCodec.str(input, "phone");
            JsonObject result = new JsonObject();
            result.addProperty("demo", true);
            result.addProperty("phoneVerified", false);
            if (path.endsWith("demo-code")) {
                DemoAccountService.Challenge challenge = accounts.request(phone);
                result.addProperty("challengeId", challenge.challengeId);
                result.addProperty("phone", challenge.phone);
                result.addProperty("code", challenge.code);
            } else {
                JdbcAccountRepository.Account account =
                        accounts.verify(
                                JsonCodec.str(input, "challengeId"),
                                phone,
                                JsonCodec.str(input, "code"));
                result.addProperty("userId", account.id);
                result.addProperty("phone", account.phone);
            }
            FitnessServer.json(e, 200, result.toString());
        } catch (SQLException unavailable) {
            reply(e, 503, "数据库暂时不可用，未完成登录；可在验证码有效期内重试");
        } catch (RuntimeException invalid) {
            reply(
                    e,
                    400,
                    invalid instanceof IllegalArgumentException
                            ? invalid.getMessage()
                            : "登录参数格式不正确");
        } finally {
            e.close();
        }
    }

    private void reply(HttpExchange e, int status, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("error", message == null ? "请求无效" : message);
        FitnessServer.json(e, status, error.toString());
    }
}
