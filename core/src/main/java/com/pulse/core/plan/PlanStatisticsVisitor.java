package com.pulse.core.plan;

import com.pulse.core.model.*;

/** Visitor: a new aggregate operation without changing week/day/prescription classes. */
public final class PlanStatisticsVisitor implements PlanVisitor {
    public int trainingDays, exerciseCount, totalSets, estimatedSeconds;

    @Override
    public void visitWeek(WeekPlan week) {}

    @Override
    public void visitDay(DayPlan day) {
        if (!day.isRest()) {
            trainingDays++;
            estimatedSeconds += 480;
        }
    }

    @Override
    public void visitExercise(Prescription p) {
        Exercise e = ExerciseCatalog.get(p.exerciseId);
        exerciseCount++;
        totalSets += p.sets;
        estimatedSeconds += p.sets * ((e.timed ? p.amount : p.amount * 3) + p.restSeconds);
    }

    public static int minutes(DayPlan day) {
        PlanStatisticsVisitor v = new PlanStatisticsVisitor();
        day.accept(v);
        return (v.estimatedSeconds + 59) / 60;
    }
}
