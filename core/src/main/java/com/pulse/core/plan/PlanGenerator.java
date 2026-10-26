package com.pulse.core.plan;

import com.pulse.core.model.*;

public interface PlanGenerator {
    WeekPlan generate(UserProfile profile) throws Exception;
}
