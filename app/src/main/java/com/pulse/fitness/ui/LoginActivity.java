package com.pulse.fitness.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.pulse.core.auth.PhoneLoginGateway;
import com.pulse.core.util.ObservableState;
import com.pulse.fitness.AppContainer;
import com.pulse.fitness.data.*;
import com.pulse.fitness.R;
import com.pulse.fitness.presentation.LoginViewModel;

/** Phone-code entry flow. The demo never sends SMS or authenticates a remote account. */
public final class LoginActivity extends BackendActivity {
    public static final String FORCE_LOGIN = "force_login";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private LoginViewModel vm;
    private EditText phone, code;
    private CheckBox agreed;
    private CheckBox useServer;
    private Button sendCode;
    private TextView error;
    private String pendingCode, pendingPhone;
    private String displayedChallenge;
    private boolean observing;
    private final ObservableState.Listener<LoginViewModel.State> observer = this::render;
    private final Runnable countdown =
            new Runnable() {
                public void run() {
                    if (sendCode == null) return;
                    updateCountdown();
                    handler.postDelayed(this, 1000);
                }
            };

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        AppContainer container = AppContainer.get();
        if (container.sessions.canEnterApp() && !getIntent().getBooleanExtra(FORCE_LOGIN, false)) {
            openMain();
            return;
        }
        setContentView(R.layout.activity_login);
        View root = findViewById(R.id.auth_root);
        Ui.insets(this, root, 0);
        phone = findViewById(R.id.auth_phone);
        code = findViewById(R.id.auth_code);
        agreed = findViewById(R.id.auth_agree);
        sendCode = findViewById(R.id.auth_get_code);
        error = findViewById(R.id.auth_error);
        useServer = findViewById(R.id.auth_use_server);
        useServer.setChecked(container.settings.getBoolean("sql_login", false));
        vm = (LoginViewModel) getLastNonConfigurationInstance();
        if (vm == null) vm = newModel();
        updateNotice();
        useServer.setOnCheckedChangeListener(
                (button, checked) -> {
                    container.settings.edit().putBoolean("sql_login", checked).apply();
                    replaceModel();
                    updateNotice();
                });
        findViewById(R.id.auth_connection).setOnClickListener(v -> configureConnection());
        if (Build.VERSION.SDK_INT >= 28) {
            findViewById(R.id.auth_headline).setAccessibilityHeading(true);
            findViewById(R.id.auth_title).setAccessibilityHeading(true);
        }
        root.addOnLayoutChangeListener(
                (v, l, t, r, b, ol, ot, or, ob) -> {
                    View content = findViewById(R.id.auth_content);
                    int width = (r - l) > Ui.dp(this, 600) ? Ui.dp(this, 560) : -1;
                    if (content.getLayoutParams().width != width) {
                        android.view.ViewGroup.LayoutParams p = content.getLayoutParams();
                        p.width = width;
                        content.setLayoutParams(p);
                    }
                });
        sendCode.setOnClickListener(v -> requestCode());
        findViewById(R.id.auth_login).setOnClickListener(v -> login());
        findViewById(R.id.auth_guest)
                .setOnClickListener(
                        v -> {
                            vm.enterGuest();
                            openMain();
                        });
        findViewById(R.id.auth_terms).setOnClickListener(v -> showLegal(false));
        findViewById(R.id.auth_privacy).setOnClickListener(v -> showLegal(true));
        code.setOnEditorActionListener(
                (view, action, event) -> {
                    if (action == EditorInfo.IME_ACTION_DONE) {
                        login();
                        return true;
                    }
                    return false;
                });
        if (saved != null) {
            String oldError = saved.getString("error");
            if (oldError != null && !oldError.isEmpty()) showError(oldError);
            pendingCode = saved.getString("pendingCode");
            pendingPhone = saved.getString("pendingPhone");
            displayedChallenge = saved.getString("displayedChallenge");
            if (pendingCode != null && pendingPhone != null) showDemoCode();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sendCode != null) {
            observing = true;
            vm.state.observe(observer);
            handler.post(countdown);
        }
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(countdown);
        if (vm != null) vm.state.remove(observer);
        observing = false;
        super.onStop();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return vm;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (error != null)
            out.putString(
                    "error",
                    error.getVisibility() == View.VISIBLE ? error.getText().toString() : "");
        out.putString("pendingCode", pendingCode);
        out.putString("pendingPhone", pendingPhone);
        out.putString("displayedChallenge", displayedChallenge);
    }

    private void requestCode() {
        if (useServer.isChecked()) {
            runWithBackendAccess(this::requestCodeNow);
            return;
        }
        requestCodeNow();
    }

    private void requestCodeNow() {
        try {
            vm.requestCode(phone.getText().toString(), agreed.isChecked());
        } catch (IllegalArgumentException failure) {
            showError(failure.getMessage());
        }
    }

    private void showDemoCode() {
        View body = Ui.screen(this, R.layout.dialog_demo_code);
        Ui.text(body, R.id.demo_code_value, pendingCode);
        String masked = pendingPhone.substring(0, 3) + " **** " + pendingPhone.substring(7);
        Ui.text(
                body,
                R.id.demo_code_description,
                getString(
                        useServer.isChecked()
                                ? R.string.auth_server_code_explanation
                                : R.string.auth_demo_code_explanation,
                        masked));
        new AlertDialog.Builder(this, R.style.AuthDialogTheme)
                .setTitle(R.string.auth_demo_title)
                .setView(body)
                .setPositiveButton(
                        R.string.auth_fill_code,
                        (dialog, which) -> {
                            code.setText(pendingCode);
                            pendingCode = null;
                            pendingPhone = null;
                            code.clearFocus();
                            InputMethodManager keyboard =
                                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            keyboard.hideSoftInputFromWindow(code.getWindowToken(), 0);
                        })
                .setNegativeButton(
                        R.string.auth_manual_code,
                        (dialog, which) -> {
                            pendingCode = null;
                            pendingPhone = null;
                            code.requestFocus();
                        })
                .setOnCancelListener(
                        dialog -> {
                            pendingCode = null;
                            pendingPhone = null;
                        })
                .show();
    }

    private void login() {
        if (useServer.isChecked()) {
            runWithBackendAccess(this::loginNow);
            return;
        }
        loginNow();
    }

    private void loginNow() {
        try {
            vm.login(phone.getText().toString(), code.getText().toString(), agreed.isChecked());
        } catch (IllegalArgumentException failure) {
            showError(failure.getMessage());
        }
    }

    private void updateCountdown() {
        int remaining = vm.resendSeconds();
        sendCode.setEnabled(remaining == 0 && !vm.state.get().busy);
        sendCode.setText(
                remaining == 0
                        ? getString(R.string.auth_get_code)
                        : getString(R.string.auth_resend_countdown, remaining));
    }

    private LoginViewModel newModel() {
        AppContainer c = AppContainer.get();
        PhoneLoginGateway gateway =
                useServer.isChecked()
                        ? new RemotePhoneLoginGateway(c.endpoint(), c.tokens.get())
                        : new LocalPhoneLoginGateway(c.phoneAuth);
        return new LoginViewModel(gateway, c.sessions, c.worker, task -> c.main.post(task));
    }

    private void replaceModel() {
        if (observing) vm.state.remove(observer);
        vm = newModel();
        pendingCode = null;
        pendingPhone = null;
        displayedChallenge = null;
        code.setText("");
        if (observing) vm.state.observe(observer);
    }

    private void updateNotice() {
        Ui.text(
                findViewById(R.id.auth_root),
                R.id.auth_demo_notice,
                getString(
                        useServer.isChecked()
                                ? R.string.auth_server_notice
                                : R.string.auth_demo_notice));
    }

    private void render(LoginViewModel.State state) {
        findViewById(R.id.auth_login).setEnabled(!state.busy);
        findViewById(R.id.auth_guest).setEnabled(!state.busy);
        useServer.setEnabled(!state.busy);
        findViewById(R.id.auth_connection).setEnabled(!state.busy);
        phone.setEnabled(!state.busy);
        code.setEnabled(!state.busy);
        updateCountdown();
        if (!state.error.isEmpty()) showError(state.error);
        else error.setVisibility(View.GONE);
        if (state.loggedIn) {
            openMain();
            return;
        }
        if (state.delivery != null && !state.delivery.id.equals(displayedChallenge)) {
            displayedChallenge = state.delivery.id;
            pendingPhone = state.delivery.phone;
            pendingCode = state.delivery.code;
            showDemoCode();
        }
    }

    private void configureConnection() {
        AppContainer c = AppContainer.get();
        View body = Ui.screen(this, R.layout.dialog_login_connection);
        EditText address = body.findViewById(R.id.login_endpoint),
                token = body.findViewById(R.id.login_gateway_token);
        address.setText(c.endpoint());
        token.setText(c.tokens.get());
        AlertDialog dialog =
                new AlertDialog.Builder(this, R.style.AuthDialogTheme)
                        .setTitle(R.string.auth_connection)
                        .setView(body)
                        .setNegativeButton(R.string.auth_cancel, null)
                        .setPositiveButton(R.string.auth_save_connection, null)
                        .create();
        dialog.setOnShowListener(
                v ->
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setOnClickListener(
                                        button -> {
                                            try {
                                                c.saveConnection(
                                                        address.getText().toString(),
                                                        token.getText().toString());
                                                replaceModel();
                                                dialog.dismiss();
                                            } catch (Exception failure) {
                                                address.setError(failure.getMessage());
                                            }
                                        }));
        dialog.show();
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisibility(View.VISIBLE);
    }

    private void showLegal(boolean privacy) {
        startActivity(
                new Intent(this, AuthLegalActivity.class)
                        .putExtra(AuthLegalActivity.PRIVACY, privacy));
    }

    private void openMain() {
        startActivity(
                new Intent(this, MainActivity.class)
                        .addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    public static void openFresh(Activity owner) {
        owner.startActivity(
                new Intent(owner, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        owner.finish();
    }
}
