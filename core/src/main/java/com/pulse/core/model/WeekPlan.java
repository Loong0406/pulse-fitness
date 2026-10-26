package com.pulse.core.model;

import com.pulse.core.plan.PlanVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class WeekPlan implements PlanNode, Iterable<DayPlan> {
    public enum Source {
        LOCAL,
        AI
    }

    public final String id, profileSignature, note;
    public final long createdAt;
    public final Source source;
    public final List<DayPlan> days;

    public WeekPlan(
            String id,
            long time,
            String signature,
            Source source,
            String note,
            List<DayPlan> days) {
        this.id = id;
        createdAt = time;
        profileSignature = signature;
        this.source = source;
        this.note = note;
        this.days = Collections.unmodifiableList(new ArrayList<>(days));
    }

    /** Prototype: deep copies every mutable aggregate boundary and assigns a fresh identity. */
    public WeekPlan copyForNextWeek() {
        List<DayPlan> copies = new ArrayList<>();
        for (DayPlan day : days) copies.add(day.copy());
        String copiedNote = note.startsWith("沿用上一周期") ? note : "沿用上一周期 · " + note;
        return new WeekPlan(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                profileSignature,
                source,
                copiedNote.substring(0, Math.min(1200, copiedNote.length())),
                copies);
    }

    @Override
    public Iterator<DayPlan> iterator() {
        return days.iterator();
    }

    @Override
    public void accept(PlanVisitor visitor) {
        visitor.visitWeek(this);
        for (DayPlan day : this) day.accept(visitor);
    }
}
