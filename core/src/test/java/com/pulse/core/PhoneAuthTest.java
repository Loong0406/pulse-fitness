package com.pulse.core;

import com.pulse.core.auth.DemoPhoneAuth;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicLong;

public final class PhoneAuthTest {
    private static final String PHONE = "13800138000";

    @Test
    public void successConsumesTheCodeAndDoesNotVerifyAnotherPhone() {
        AtomicLong time = new AtomicLong(1000);
        DemoPhoneAuth auth = new DemoPhoneAuth(time::get, () -> 42);
        DemoPhoneAuth.CodeDelivery delivery = auth.requestCode(PHONE);
        assertEquals("000042", delivery.code);
        assertThrows(
                IllegalArgumentException.class, () -> auth.verify("13900139000", delivery.code));
        auth.verify(PHONE, delivery.code);
        assertThrows(IllegalArgumentException.class, () -> auth.verify(PHONE, delivery.code));
    }

    @Test
    public void resendCountdownCannotBeBypassedWithAnotherPhone() {
        AtomicLong time = new AtomicLong(1000);
        DemoPhoneAuth auth = new DemoPhoneAuth(time::get, () -> 123456);
        auth.requestCode(PHONE);
        assertEquals(60, auth.resendSeconds());
        time.addAndGet(1001);
        assertEquals(59, auth.resendSeconds());
        assertThrows(IllegalArgumentException.class, () -> auth.requestCode("13900139000"));
        time.addAndGet(59000);
        assertEquals(0, auth.resendSeconds());
        assertEquals("13900139000", auth.requestCode("13900139000").phone);
    }

    @Test
    public void expiredAndRepeatedlyWrongCodesAreRejected() {
        AtomicLong time = new AtomicLong(1000);
        DemoPhoneAuth auth = new DemoPhoneAuth(time::get, () -> 123456);
        auth.requestCode(PHONE);
        time.addAndGet(300000);
        assertThrows(IllegalArgumentException.class, () -> auth.verify(PHONE, "123456"));
        auth.requestCode(PHONE);
        for (int i = 0; i < DemoPhoneAuth.MAX_ATTEMPTS; i++)
            assertThrows(IllegalArgumentException.class, () -> auth.verify(PHONE, "999999"));
        assertThrows(IllegalArgumentException.class, () -> auth.verify(PHONE, "123456"));
    }

    @Test
    public void phoneAndCodeFormatsAndClockRollbackAreChecked() {
        assertEquals(PHONE, DemoPhoneAuth.normalizePhone("138 0013 8000"));
        assertThrows(IllegalArgumentException.class, () -> DemoPhoneAuth.normalizePhone("123"));
        assertThrows(
                IllegalArgumentException.class, () -> DemoPhoneAuth.normalizePhone("1380013800x"));
        AtomicLong time = new AtomicLong(1000);
        DemoPhoneAuth auth = new DemoPhoneAuth(time::get, () -> 123456);
        auth.requestCode(PHONE);
        assertThrows(IllegalArgumentException.class, () -> auth.verify(PHONE, "abc123"));
        time.set(0);
        assertThrows(IllegalArgumentException.class, () -> auth.verify(PHONE, "123456"));
    }
}
