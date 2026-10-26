package com.pulse.server;

import com.pulse.core.auth.DemoPhoneAuth;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;

/** Demo-only challenges live on the gateway. Returning a code to the caller does not verify phone ownership. */
public final class DemoAccountService {
    public static final class Challenge {
        public final String challengeId, phone, code;

        Challenge(String id, DemoPhoneAuth.CodeDelivery d) {
            challengeId = id;
            phone = d.phone;
            code = d.code;
        }
    }

    private static final class Pending {
        final DemoPhoneAuth auth = new DemoPhoneAuth();
        final long expires = System.nanoTime() + 300_000_000_000L;
        String phone, verifiedCode;
    }

    private final JdbcAccountRepository accounts;
    private final Map<String, Pending> pending = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    public DemoAccountService(JdbcAccountRepository accounts) {
        this.accounts = accounts;
    }

    public synchronized Challenge request(String rawPhone) {
        String phone = DemoPhoneAuth.normalizePhone(rawPhone);
        long now = System.nanoTime();
        pending.entrySet().removeIf(e -> e.getValue().expires <= now);
        cooldowns.entrySet().removeIf(e -> e.getValue() <= now);
        if (cooldowns.containsKey(phone)) throw new IllegalArgumentException("请等待 60 秒后重新获取验证码");
        if (pending.size() >= 1000 || cooldowns.size() >= 1000)
            throw new IllegalArgumentException("登录请求繁忙，请稍后重试");
        Pending p = new Pending();
        p.phone = phone;
        DemoPhoneAuth.CodeDelivery delivery = p.auth.requestCode(phone);
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pending.put(id, p);
        cooldowns.put(phone, now + 60_000_000_000L);
        return new Challenge(id, delivery);
    }

    public synchronized JdbcAccountRepository.Account verify(
            String challengeId, String rawPhone, String code) throws SQLException {
        String phone = DemoPhoneAuth.normalizePhone(rawPhone);
        Pending p = pending.get(challengeId);
        if (p == null || System.nanoTime() >= p.expires) {
            pending.remove(challengeId);
            throw new IllegalArgumentException("演示验证码已失效，请重新获取");
        }
        if (!p.phone.equals(phone)) throw new IllegalArgumentException("手机号已更改，请重新获取验证码");
        if (p.verifiedCode == null) {
            p.auth.verify(phone, code);
            p.verifiedCode = code;
        } else if (!p.verifiedCode.equals(code)) throw new IllegalArgumentException("验证码不正确");
        // Keep the verified challenge on SQL failure so a connection retry does not consume a new
        // code.
        JdbcAccountRepository.Account result = accounts.recordDemoLogin(phone);
        pending.remove(challengeId);
        return result;
    }
}
