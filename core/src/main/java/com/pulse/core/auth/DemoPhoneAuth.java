package com.pulse.core.auth;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/** Local UI demonstration only. No SMS is sent and phone ownership is not established. */
public final class DemoPhoneAuth {
    public static final int RESEND_SECONDS = 60, VALID_SECONDS = 300, MAX_ATTEMPTS = 5;

    public static final class CodeDelivery {
        public final String phone, code;

        private CodeDelivery(String phone, String code) {
            this.phone = phone;
            this.code = code;
        }
    }

    private final LongSupplier clock;
    private final IntSupplier numbers;
    private String phone, code;
    private long issuedAt, nextRequestAt = -1;
    private int attempts;

    public DemoPhoneAuth() {
        this(() -> System.nanoTime() / 1_000_000L, new SecureRandom()::nextInt);
    }

    public DemoPhoneAuth(LongSupplier clock, IntSupplier numbers) {
        this.clock = clock;
        this.numbers = numbers;
    }

    public static String normalizePhone(String raw) {
        String value = raw == null ? "" : raw.replace(" ", "").replace("-", "").trim();
        if (!value.matches("1[3-9][0-9]{9}"))
            throw new IllegalArgumentException("请输入正确的 11 位中国大陆手机号");
        return value;
    }

    public CodeDelivery requestCode(String raw) {
        String value = normalizePhone(raw);
        if (resendSeconds() > 0) throw new IllegalArgumentException("请等待倒计时结束后重新获取验证码");
        phone = value;
        code = String.format(Locale.ROOT, "%06d", Math.floorMod(numbers.getAsInt(), 1_000_000));
        issuedAt = clock.getAsLong();
        nextRequestAt = issuedAt + RESEND_SECONDS * 1000L;
        attempts = 0;
        return new CodeDelivery(phone, code);
    }

    public void verify(String rawPhone, String rawCode) {
        String value = normalizePhone(rawPhone);
        if (code == null) throw new IllegalArgumentException("请先获取本机演示验证码");
        if (!phone.equals(value)) throw new IllegalArgumentException("手机号已更改，请为当前手机号重新获取验证码");
        long elapsed = clock.getAsLong() - issuedAt;
        if (elapsed < 0 || elapsed >= VALID_SECONDS * 1000L) {
            code = null;
            throw new IllegalArgumentException("验证码已过期，请重新获取");
        }
        String entered = rawCode == null ? "" : rawCode.trim();
        if (!entered.matches("[0-9]{6}")) throw new IllegalArgumentException("请输入 6 位验证码");
        if (!code.equals(entered)) {
            attempts++;
            if (attempts >= MAX_ATTEMPTS) {
                code = null;
                throw new IllegalArgumentException("验证码连续错误，请重新获取");
            }
            throw new IllegalArgumentException("验证码不正确，请检查本机演示验证码");
        }
        code = null;
        phone = null;
        attempts = 0;
    }

    public int resendSeconds() {
        return nextRequestAt < 0
                ? 0
                : (int) Math.max(0, (nextRequestAt - clock.getAsLong() + 999) / 1000);
    }

    public void clear() {
        phone = null;
        code = null;
        attempts = 0;
        nextRequestAt = -1;
    }
}
