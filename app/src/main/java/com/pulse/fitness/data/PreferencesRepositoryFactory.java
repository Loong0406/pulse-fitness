package com.pulse.fitness.data;

import android.content.SharedPreferences;
import com.google.gson.reflect.TypeToken;
import com.pulse.core.data.Repositories;
import com.pulse.core.model.*;
import com.pulse.core.util.JsonCodec;
import java.util.*;

/** Abstract Factory's persistent Android family. No screen knows SharedPreferences. */
public final class PreferencesRepositoryFactory implements Repositories.Factory {
    private final SharedPreferences prefs;

    public PreferencesRepositoryFactory(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public Repositories.Profiles profiles() {
        return new Repositories.Profiles() {
            public UserProfile get() {
                try {
                    String s = prefs.getString("profile", null);
                    return s == null ? null : JsonCodec.profile(s);
                } catch (RuntimeException e) {
                    return null;
                }
            }

            public void save(UserProfile p) {
                prefs.edit().putString("profile", JsonCodec.profile(p)).apply();
            }
        };
    }

    public Repositories.Plans plans() {
        return new Repositories.Plans() {
            public WeekPlan get() {
                try {
                    String s = prefs.getString("plan", null);
                    return s == null ? null : JsonCodec.plan(s);
                } catch (RuntimeException e) {
                    return null;
                }
            }

            public void save(WeekPlan p) {
                prefs.edit()
                        .putString("plan", JsonCodec.plan(p))
                        .remove("session")
                        .remove("session_day")
                        .apply();
            }

            public void clear() {
                prefs.edit().remove("plan").remove("session").remove("session_day").apply();
            }
        };
    }

    public Repositories.History history() {
        return new Repositories.History() {
            public List<TrainingRecord> all() {
                return records();
            }

            public void add(TrainingRecord r) {
                List<TrainingRecord> list = new ArrayList<>(records());
                for (TrainingRecord old : list) if (old.id.equals(r.id)) return;
                list.add(0, r);
                prefs.edit()
                        .putString("history", JsonCodec.GSON.toJson(list))
                        .remove("session")
                        .remove("session_day")
                        .apply();
            }

            public void clear() {
                prefs.edit().remove("history").apply();
            }
        };
    }

    private List<TrainingRecord> records() {
        try {
            List<TrainingRecord> result =
                    JsonCodec.GSON.fromJson(
                            prefs.getString("history", "[]"),
                            new TypeToken<List<TrainingRecord>>() {}.getType());
            if (result == null) return Collections.emptyList();
            result.removeIf(
                    r ->
                            r == null
                                    || r.id == null
                                    || r.title == null
                                    || r.activeSeconds < 0
                                    || r.completedSets < 1);
            return Collections.unmodifiableList(result);
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }
}
