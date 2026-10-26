package com.pulse.core.plan;

import com.pulse.core.model.*;

public interface PlanVisitor {
    void visitWeek(WeekPlan week);

    void visitDay(DayPlan day);

    void visitExercise(Prescription exercise);
}
