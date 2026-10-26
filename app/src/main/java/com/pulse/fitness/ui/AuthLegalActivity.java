package com.pulse.fitness.ui;

import android.app.Activity;
import android.os.Bundle;
import com.pulse.fitness.R;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class AuthLegalActivity extends Activity {
    public static final String PRIVACY = "privacy";

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_auth_legal);
        Ui.insets(this, findViewById(R.id.auth_legal_root), 0);
        boolean privacy = getIntent().getBooleanExtra(PRIVACY, false);
        Ui.text(
                findViewById(R.id.auth_legal_root),
                R.id.auth_legal_title,
                getString(privacy ? R.string.auth_privacy_title : R.string.auth_terms_title));
        findViewById(R.id.auth_legal_back).setOnClickListener(v -> finish());
        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                getAssets().open(privacy ? "auth_privacy.txt" : "auth_terms.txt"),
                                StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
            Ui.text(findViewById(R.id.auth_legal_root), R.id.auth_legal_text, text.toString());
        } catch (IOException failure) {
            Ui.text(
                    findViewById(R.id.auth_legal_root),
                    R.id.auth_legal_text,
                    getString(R.string.auth_read_error));
        }
    }
}
