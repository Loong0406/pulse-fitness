package com.pulse.core.model;

/** Flyweight: catalog information is immutable; sets and reps live in prescriptions. */
public final class Exercise {
    public final String id, name, group, equipmentLabel, cue;
    public final java.util.List<String> steps;
    public final UserProfile.Equipment equipment;
    public final boolean timed, kneeLoad, backLoad, impact;

    public Exercise(
            String id,
            String name,
            String group,
            UserProfile.Equipment equipment,
            boolean timed,
            boolean kneeLoad,
            boolean backLoad,
            boolean impact,
            String cue,
            String... steps) {
        this.id = id;
        this.name = name;
        this.group = group;
        this.equipment = equipment;
        this.timed = timed;
        this.kneeLoad = kneeLoad;
        this.backLoad = backLoad;
        this.impact = impact;
        this.cue = cue;
        this.steps = java.util.Collections.unmodifiableList(java.util.Arrays.asList(steps.clone()));
        equipmentLabel =
                equipment == UserProfile.Equipment.BODYWEIGHT
                        ? "徒手"
                        : equipment == UserProfile.Equipment.DUMBBELL ? "哑铃" : "健身房";
    }

    public boolean isAllowed(UserProfile p) {
        return (equipment == UserProfile.Equipment.BODYWEIGHT
                        || equipment == p.equipment
                        || p.equipment == UserProfile.Equipment.GYM)
                && !(p.protectKnees && kneeLoad)
                && !(p.protectBack && backLoad)
                && !((p.lowImpact || p.age >= 60 || p.experience == UserProfile.Experience.BEGINNER)
                        && impact);
    }
}
