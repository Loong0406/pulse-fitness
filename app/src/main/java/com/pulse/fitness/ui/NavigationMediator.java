package com.pulse.fitness.ui;

import com.pulse.core.model.DayPlan;

/** Mediator: independent screens communicate through navigation intentions. */
public interface NavigationMediator {
    enum Tab {
        HOME,
        PLAN,
        EXERCISES,
        PROGRESS,
        PROFILE
    }

    void show(Tab tab);

    void startTraining(DayPlan day);

    void exportPlan();

    void runWithBackendAccess(Runnable action);
}
