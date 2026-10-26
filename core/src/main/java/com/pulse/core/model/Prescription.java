package com.pulse.core.model;

import com.pulse.core.plan.PlanVisitor;

public final class Prescription implements PlanNode {
    public final String exerciseId;
    public final int sets, amount, restSeconds;

    public Prescription(String id, int sets, int amount, int restSeconds) {
        this.exerciseId = id;
        this.sets = sets;
        this.amount = amount;
        this.restSeconds = restSeconds;
    }

    public Prescription copy() {
        return new Prescription(exerciseId, sets, amount, restSeconds);
    }

    @Override
    public void accept(PlanVisitor visitor) {
        visitor.visitExercise(this);
    }
}
