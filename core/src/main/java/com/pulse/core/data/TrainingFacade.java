package com.pulse.core.data;

import com.pulse.core.model.*;
import com.pulse.core.plan.*;

/** Facade coordinates generation and persistence without exposing storage to screens. */
public final class TrainingFacade {
    private final Repositories.Profiles profiles;
    private final Repositories.Plans plans;

    public TrainingFacade(Repositories.Factory factory) {
        profiles = factory.profiles();
        plans = factory.plans();
    }

    public WeekPlan generate(PlanGenerator generator) throws Exception {
        UserProfile p = profiles.get();
        if (p == null) throw new IllegalArgumentException("请先保存个人档案");
        return generator.generate(p);
    }

    public void commit(WeekPlan result) {
        UserProfile p = profiles.get();
        if (p == null) throw new IllegalArgumentException("请先保存个人档案");
        PlanSafety.validate(result, p);
        plans.save(result);
    }

    public WeekPlan current() {
        return plans.get();
    }

    public void invalidate() {
        plans.clear();
    }
}
