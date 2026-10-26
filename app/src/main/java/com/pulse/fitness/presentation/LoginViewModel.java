package com.pulse.fitness.presentation;

import com.pulse.core.auth.*;
import com.pulse.core.util.ObservableState;
import com.pulse.fitness.data.LocalSessionStore;
import java.util.concurrent.Executor;
import android.os.SystemClock;

/** Login intentions are independent of Activity widgets and Android storage details. */
public final class LoginViewModel {
    public static final class State {
        public final boolean busy, loggedIn;
        public final String error;
        public final PhoneLoginGateway.Challenge delivery;

        State(boolean busy, boolean loggedIn, String error, PhoneLoginGateway.Challenge delivery) {
            this.busy = busy;
            this.loggedIn = loggedIn;
            this.error = error;
            this.delivery = delivery;
        }
    }

    private final PhoneLoginGateway gateway;
    private final LocalSessionStore sessions;
    private final Executor worker, main;
    private PhoneLoginGateway.Challenge challenge;
    private long nextRequestAt;
    public final ObservableState<State> state =
            new ObservableState<>(new State(false, false, "", null));

    public LoginViewModel(
            PhoneLoginGateway gateway, LocalSessionStore sessions, Executor worker, Executor main) {
        this.gateway = gateway;
        this.sessions = sessions;
        this.worker = worker;
        this.main = main;
    }

    public void requestCode(String rawPhone, boolean agreed) {
        requireAgreement(agreed);
        String phone = DemoPhoneAuth.normalizePhone(rawPhone);
        if (state.get().busy) return;
        if (resendSeconds() > 0) throw new IllegalArgumentException("请等待倒计时结束后重试");
        state.set(new State(true, false, "", null));
        worker.execute(
                () -> {
                    try {
                        PhoneLoginGateway.Challenge result = gateway.request(phone);
                        main.execute(
                                () -> {
                                    challenge = result;
                                    nextRequestAt = SystemClock.elapsedRealtime() + 60000;
                                    state.set(new State(false, false, "", result));
                                });
                    } catch (Exception failure) {
                        main.execute(
                                () -> state.set(new State(false, false, friendly(failure), null)));
                    }
                });
    }

    public void login(String rawPhone, String code, boolean agreed) {
        requireAgreement(agreed);
        String phone = DemoPhoneAuth.normalizePhone(rawPhone);
        if (state.get().busy) return;
        if (challenge == null) throw new IllegalArgumentException("请先获取演示验证码");
        if (!challenge.phone.equals(phone)) throw new IllegalArgumentException("手机号已更改，请重新获取验证码");
        PhoneLoginGateway.Challenge requested = challenge;
        state.set(new State(true, false, "", null));
        worker.execute(
                () -> {
                    try {
                        PhoneLoginGateway.Result result = gateway.login(requested.id, phone, code);
                        main.execute(
                                () -> {
                                    if (result.userId.isEmpty())
                                        sessions.enterPhoneDemo(result.phone);
                                    else sessions.enterDatabaseDemo(result.phone, result.userId);
                                    state.set(new State(false, true, "", null));
                                });
                    } catch (Exception failure) {
                        main.execute(
                                () -> state.set(new State(false, false, friendly(failure), null)));
                    }
                });
    }

    public int resendSeconds() {
        return (int) Math.max(0, (nextRequestAt - SystemClock.elapsedRealtime() + 999) / 1000);
    }

    public void enterGuest() {
        sessions.enterGuest();
    }

    private static String friendly(Exception failure) {
        if (failure instanceof java.io.IOException)
            return failure.getMessage() == null ? "无法连接服务，请检查服务地址与后端是否启动" : failure.getMessage();
        return failure.getMessage() == null ? "登录失败，请重试" : failure.getMessage();
    }

    private void requireAgreement(boolean agreed) {
        if (!agreed) throw new IllegalArgumentException("请先阅读并同意用户协议和隐私政策");
    }
}
