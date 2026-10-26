package com.pulse.core.plan;

import com.pulse.core.model.*;
import com.pulse.core.validation.ProfileValidator;

/** Decorator also protects third-party generators that do not extend the template. */
public final class SafePlanGeneratorDecorator implements PlanGenerator {
    private final PlanGenerator delegate;

    public SafePlanGeneratorDecorator(PlanGenerator delegate) {
        this.delegate = delegate;
    }

    @Override
    public WeekPlan generate(UserProfile p) throws Exception {
        ProfileValidator.validateForTraining(p);
        WeekPlan result = delegate.generate(p);
        PlanSafety.validate(result, p);
        return result;
    }
}
