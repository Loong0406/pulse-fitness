package com.pulse.core.data;

import com.pulse.core.model.*;
import java.util.List;

public final class Repositories {
    public interface Profiles {
        UserProfile get();

        void save(UserProfile p);
    }

    public interface Plans {
        WeekPlan get();

        void save(WeekPlan p);

        void clear();
    }

    public interface History {
        List<TrainingRecord> all();

        void add(TrainingRecord r);

        void clear();
    }

    /** Abstract Factory: creates a consistent family for one storage backend. */
    public interface Factory {
        Profiles profiles();

        Plans plans();

        History history();
    }

    private Repositories() {}
}
