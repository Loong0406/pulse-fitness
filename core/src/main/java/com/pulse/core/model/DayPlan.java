package com.pulse.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.pulse.core.plan.PlanVisitor;

public final class DayPlan implements PlanNode {
    public final int dayIndex;
    public final String title;
    public final List<Prescription> exercises;

    public DayPlan(int day, String title, List<Prescription> exercises) {
        dayIndex = day;
        this.title = title;
        this.exercises = Collections.unmodifiableList(new ArrayList<>(exercises));
    }

    public boolean isRest() {
        return exercises.isEmpty();
    }

    public DayPlan copy() {
        List<Prescription> list = new ArrayList<>();
        for (Prescription p : exercises) list.add(p.copy());
        return new DayPlan(dayIndex, title, list);
    }

    @Override
    public void accept(PlanVisitor visitor) {
        visitor.visitDay(this);
        for (Prescription p : exercises) p.accept(visitor);
    }
}
