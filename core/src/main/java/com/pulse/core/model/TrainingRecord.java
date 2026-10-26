package com.pulse.core.model;

public final class TrainingRecord {
    public final String id, title;
    public final long completedAt;
    public final int activeSeconds, completedSets;

    public TrainingRecord(String id, String title, long time, int seconds, int sets) {
        this.id = id;
        this.title = title;
        completedAt = time;
        activeSeconds = seconds;
        completedSets = sets;
    }
}
