package com.pulse.core.model;

import java.util.Locale;

/** Immutable profile. Builder is the only construction API. */
public final class UserProfile {
    public enum Goal {
        FAT_LOSS,
        STRENGTH,
        WELLBEING
    }

    public enum Experience {
        BEGINNER,
        REGULAR,
        ADVANCED
    }

    public enum Equipment {
        BODYWEIGHT,
        DUMBBELL,
        GYM
    }

    public final String name;
    public final int age;
    public final String sex;
    public final double heightCm, weightKg;
    public final Goal goal;
    public final Experience experience;
    public final Equipment equipment;
    public final int daysPerWeek, minutesPerSession;
    public final boolean protectKnees, protectBack, lowImpact, needsProfessional;

    private UserProfile(Builder b) {
        name = b.name;
        age = b.age;
        sex = b.sex;
        heightCm = b.heightCm;
        weightKg = b.weightKg;
        goal = b.goal;
        experience = b.experience;
        equipment = b.equipment;
        daysPerWeek = b.days;
        minutesPerSession = b.minutes;
        protectKnees = b.knees;
        protectBack = b.back;
        lowImpact = b.lowImpact;
        needsProfessional = b.professional;
    }

    public String signature() {
        return String.format(
                Locale.ROOT,
                "%d|%s|%.1f|%.1f|%s|%s|%s|%d|%d|%b|%b|%b|%b",
                age,
                sex,
                heightCm,
                weightKg,
                goal,
                experience,
                equipment,
                daysPerWeek,
                minutesPerSession,
                protectKnees,
                protectBack,
                lowImpact,
                needsProfessional);
    }

    public Builder toBuilder() {
        return new Builder()
                .name(name)
                .age(age)
                .sex(sex)
                .body(heightCm, weightKg)
                .goal(goal)
                .experience(experience)
                .equipment(equipment)
                .schedule(daysPerWeek, minutesPerSession)
                .limitations(protectKnees, protectBack, lowImpact, needsProfessional);
    }

    public static UserProfile defaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private String name = "训练者", sex = "不透露";
        private int age = 25, days = 3, minutes = 30;
        private double heightCm = 170, weightKg = 65;
        private Goal goal = Goal.WELLBEING;
        private Experience experience = Experience.BEGINNER;
        private Equipment equipment = Equipment.BODYWEIGHT;
        private boolean knees, back, lowImpact, professional;

        public Builder name(String v) {
            name = v == null ? "" : v.trim();
            return this;
        }

        public Builder age(int v) {
            age = v;
            return this;
        }

        public Builder sex(String v) {
            sex = v;
            return this;
        }

        public Builder body(double h, double w) {
            heightCm = h;
            weightKg = w;
            return this;
        }

        public Builder goal(Goal v) {
            goal = v;
            return this;
        }

        public Builder experience(Experience v) {
            experience = v;
            return this;
        }

        public Builder equipment(Equipment v) {
            equipment = v;
            return this;
        }

        public Builder schedule(int d, int m) {
            days = d;
            minutes = m;
            return this;
        }

        public Builder limitations(boolean k, boolean b, boolean l, boolean p) {
            knees = k;
            back = b;
            lowImpact = l;
            professional = p;
            return this;
        }

        public UserProfile build() {
            return new UserProfile(this);
        }
    }
}
