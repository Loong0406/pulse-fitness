package com.pulse.core.plan;

import com.pulse.core.model.*;
import com.pulse.core.validation.ProfileValidator;

/** Template Method keeps validation mandatory for local and remote implementations. */
public abstract class PlanGenerationTemplate implements PlanGenerator {
    @Override
    public final WeekPlan generate(UserProfile profile) throws Exception {
        ProfileValidator.validateForTraining(profile);
        WeekPlan candidate = createCandidate(profile);
        PlanSafety.validate(candidate, profile);
        return candidate;
    }

    protected abstract WeekPlan createCandidate(UserProfile profile) throws Exception;
}
