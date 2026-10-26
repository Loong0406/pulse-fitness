package com.pulse.core.auth;

/** Swap local demonstration and gateway-backed SQL login without changing the page's business intentions. */
public interface PhoneLoginGateway {
    final class Challenge {
        public final String id, phone, code;

        public Challenge(String id, String phone, String code) {
            this.id = id;
            this.phone = phone;
            this.code = code;
        }
    }

    final class Result {
        public final String userId, phone;

        public Result(String userId, String phone) {
            this.userId = userId;
            this.phone = phone;
        }
    }

    Challenge request(String phone) throws Exception;

    Result login(String challenge, String phone, String code) throws Exception;
}
