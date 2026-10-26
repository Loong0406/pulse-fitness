package com.pulse.core.data;

import com.pulse.core.model.*;
import java.util.*;

/** In-memory product family for isolated tests and embedded integrations. */
public final class MemoryRepositoryFactory implements Repositories.Factory {
    private UserProfile profile;
    private WeekPlan plan;
    private final List<TrainingRecord> records = new ArrayList<>();

    public Repositories.Profiles profiles() {
        return new Repositories.Profiles() {
            public UserProfile get() {
                return profile;
            }

            public void save(UserProfile p) {
                profile = p;
            }
        };
    }

    public Repositories.Plans plans() {
        return new Repositories.Plans() {
            public WeekPlan get() {
                return plan;
            }

            public void save(WeekPlan p) {
                plan = p;
            }

            public void clear() {
                plan = null;
            }
        };
    }

    public Repositories.History history() {
        return new Repositories.History() {
            public List<TrainingRecord> all() {
                return Collections.unmodifiableList(new ArrayList<>(records));
            }

            public void add(TrainingRecord r) {
                for (TrainingRecord old : records) if (old.id.equals(r.id)) return;
                records.add(0, r);
            }

            public void clear() {
                records.clear();
            }
        };
    }
}
