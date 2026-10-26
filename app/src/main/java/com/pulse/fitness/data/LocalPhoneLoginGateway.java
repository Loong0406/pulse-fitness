package com.pulse.fitness.data;

import com.pulse.core.auth.*;

/** Keeps the existing offline demo available when no SQL gateway has been configured. */
public final class LocalPhoneLoginGateway implements PhoneLoginGateway {
    private final DemoPhoneAuth auth;

    public LocalPhoneLoginGateway(DemoPhoneAuth auth) {
        this.auth = auth;
    }

    public Challenge request(String phone) {
        DemoPhoneAuth.CodeDelivery d = auth.requestCode(phone);
        return new Challenge(java.util.UUID.randomUUID().toString(), d.phone, d.code);
    }

    public Result login(String challenge, String phone, String code) {
        auth.verify(phone, code);
        return new Result("", DemoPhoneAuth.normalizePhone(phone));
    }
}
