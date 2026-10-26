package com.pulse.fitness.data;

import android.content.SharedPreferences;
import com.pulse.core.auth.DemoPhoneAuth;

/** Remembers a local demo identity or guest choice; never a server authentication token. */
public final class LocalSessionStore {
    private final SharedPreferences prefs;

    public LocalSessionStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public boolean isPhoneSession() {
        String mode = prefs.getString("mode", "");
        return ("PHONE_DEMO".equals(mode) || "SQL_DEMO".equals(mode))
                && prefs.getString("phone", "").matches("1[3-9][0-9]{9}");
    }

    public boolean isDatabaseSession() {
        return isPhoneSession() && "SQL_DEMO".equals(prefs.getString("mode", ""));
    }

    public boolean canEnterApp() {
        return isPhoneSession() || "GUEST".equals(prefs.getString("mode", ""));
    }

    public void enterPhoneDemo(String rawPhone) {
        prefs.edit()
                .putString("mode", "PHONE_DEMO")
                .putString("phone", DemoPhoneAuth.normalizePhone(rawPhone))
                .remove("user_id")
                .apply();
    }

    public void enterDatabaseDemo(String rawPhone, String userId) {
        prefs.edit()
                .putString("mode", "SQL_DEMO")
                .putString("phone", DemoPhoneAuth.normalizePhone(rawPhone))
                .putString("user_id", userId)
                .apply();
    }

    public void enterGuest() {
        prefs.edit().putString("mode", "GUEST").remove("phone").remove("user_id").apply();
    }

    public String maskedPhone() {
        String p = prefs.getString("phone", "");
        return isPhoneSession() ? p.substring(0, 3) + " **** " + p.substring(7) : "";
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
