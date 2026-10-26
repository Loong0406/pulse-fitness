package com.pulse.core.model;

import com.pulse.core.plan.PlanVisitor;

/** Composite contract used uniformly by week, day and exercise nodes. */
public interface PlanNode {
    void accept(PlanVisitor visitor);
}
