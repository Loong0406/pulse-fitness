package com.pulse.fitness.data;

import android.content.SharedPreferences;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Only the application gateway token is stored on-device, encrypted using Android Keystore. */
public final class SecureTokenStore {
    private static final String ALIAS = "pulse_gateway_token";
    private final SharedPreferences prefs;

    public SecureTokenStore(SharedPreferences p) {
        prefs = p;
    }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS))
            return ((KeyStore.SecretKeyEntry) store.getEntry(ALIAS, null)).getSecretKey();
        KeyGenerator g =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        g.init(
                new KeyGenParameterSpec.Builder(
                                ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build());
        return g.generateKey();
    }

    public void save(String value) throws Exception {
        if (value.isEmpty()) {
            prefs.edit().remove("token").remove("token_iv").apply();
            return;
        }
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, key());
        prefs.edit()
                .putString(
                        "token",
                        Base64.encodeToString(
                                c.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                                Base64.NO_WRAP))
                .putString("token_iv", Base64.encodeToString(c.getIV(), Base64.NO_WRAP))
                .apply();
    }

    public String get() {
        try {
            String token = prefs.getString("token", null), iv = prefs.getString("token_iv", null);
            if (token == null || iv == null) return "";
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(
                    Cipher.DECRYPT_MODE,
                    key(),
                    new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            return new String(
                    c.doFinal(Base64.decode(token, Base64.NO_WRAP)),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
