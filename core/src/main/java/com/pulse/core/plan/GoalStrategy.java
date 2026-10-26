package com.pulse.core.plan;

import com.pulse.core.model.Exercise;

public interface GoalStrategy {
    int repetitions();

    int priority(Exercise exercise);

    String title();
}
