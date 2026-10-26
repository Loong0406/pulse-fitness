package com.pulse.fitness.data;

import com.google.gson.*;
import com.pulse.core.auth.*;
import com.pulse.core.util.JsonCodec;

/** Calls Java gateway APIs; the Android application never receives SQL credentials. */
public final class RemotePhoneLoginGateway implements PhoneLoginGateway {
    private final String endpoint, token;

    public RemotePhoneLoginGateway(String endpoint, String token) {
        this.endpoint = endpoint;
        this.token = token;
    }

    private JsonObject post(String path, JsonObject body) throws Exception {
        JsonObject result =
                JsonParser.parseString(
                                RemotePlanGenerator.request(
                                        endpoint + path, token, body.toString(), 12000))
                        .getAsJsonObject();
        if (!result.get("demo").getAsBoolean() || result.get("phoneVerified").getAsBoolean())
            throw new IllegalArgumentException("服务登录类型不匹配");
        return result;
    }

    public Challenge request(String phone) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("phone", phone);
        JsonObject result = post("/api/v1/auth/demo-code", body);
        String returnedPhone = DemoPhoneAuth.normalizePhone(JsonCodec.str(result, "phone")),
                id = JsonCodec.str(result, "challengeId"),
                code = JsonCodec.str(result, "code");
        if (!returnedPhone.equals(phone)
                || id.isEmpty()
                || id.length() > 128
                || !code.matches("[0-9]{6}")) throw new IllegalArgumentException("服务返回的验证码格式不正确");
        return new Challenge(id, returnedPhone, code);
    }

    public Result login(String challenge, String phone, String code) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("challengeId", challenge);
        body.addProperty("phone", phone);
        body.addProperty("code", code);
        JsonObject result = post("/api/v1/auth/demo-login", body);
        String id = java.util.UUID.fromString(JsonCodec.str(result, "userId")).toString(),
                returnedPhone = DemoPhoneAuth.normalizePhone(JsonCodec.str(result, "phone"));
        if (!returnedPhone.equals(phone)) throw new IllegalArgumentException("服务返回的手机号不匹配");
        return new Result(id, returnedPhone);
    }
}
