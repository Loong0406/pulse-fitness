package com.pulse.core.session;

import com.pulse.core.model.*;
import com.pulse.core.plan.ExerciseCatalog;
import java.util.UUID;

/** State + Memento. UI owns the clock; this class has no Android or wall-clock dependency. */
public final class WorkoutSession {
    public static final class Snapshot {
        public String id, title, phase, resumePhase;
        public int exerciseIndex, setIndex, remainingSeconds, activeSeconds, completedSets;
    }

    private interface Phase {
        String name();

        String action();

        void primary();

        void tick(int seconds);
    }

    private abstract class Base implements Phase {
        public void tick(int seconds) {}
    }

    private final class Warmup extends Base {
        public String name() {
            return "WARMUP";
        }

        public String action() {
            return "热身完成，开始训练";
        }

        public void primary() {
            remainingSeconds = 0;
            phase = new Running();
        }

        public void tick(int s) {
            remainingSeconds = Math.max(0, remainingSeconds - s);
        }
    }

    private final class Running extends Base {
        public String name() {
            return "RUNNING";
        }

        public String action() {
            return "完成本组";
        }

        public void primary() {
            Prescription previous = current();
            completedSets++;
            setIndex++;
            if (setIndex >= previous.sets) {
                setIndex = 0;
                exerciseIndex++;
            }
            if (exerciseIndex >= day.exercises.size()) {
                remainingSeconds = 240;
                phase = new Cooldown();
            } else {
                remainingSeconds = previous.restSeconds;
                phase = new Rest();
            }
        }
    }

    private final class Rest extends Base {
        public String name() {
            return "REST";
        }

        public String action() {
            return "跳过休息";
        }

        public void primary() {
            remainingSeconds = 0;
            phase = new Running();
        }

        public void tick(int s) {
            remainingSeconds = Math.max(0, remainingSeconds - s);
            if (remainingSeconds == 0) phase = new Running();
        }
    }

    private final class Cooldown extends Base {
        public String name() {
            return "COOLDOWN";
        }

        public String action() {
            return "放松完成，保存记录";
        }

        public void primary() {
            remainingSeconds = 0;
            phase = new Complete();
        }

        public void tick(int s) {
            remainingSeconds = Math.max(0, remainingSeconds - s);
        }
    }

    private final class Paused extends Base {
        public String name() {
            return "PAUSED";
        }

        public String action() {
            return "继续训练";
        }

        public void primary() {
            phase = restorePhase(resumePhase);
        }
    }

    private final class Complete extends Base {
        public String name() {
            return "COMPLETE";
        }

        public String action() {
            return "训练已完成";
        }

        public void primary() {}
    }

    private final DayPlan day;
    private final String id;
    private Phase phase;
    private String resumePhase = "RUNNING";
    private int exerciseIndex, setIndex, remainingSeconds = 240, activeSeconds, completedSets;

    public WorkoutSession(DayPlan day) {
        if (day == null || day.isRest()) throw new IllegalArgumentException("恢复日无需启动训练");
        this.day = day;
        id = UUID.randomUUID().toString();
        phase = new Warmup();
    }

    public WorkoutSession(DayPlan day, Snapshot s) {
        if (day == null
                || day.isRest()
                || s == null
                || s.id == null
                || s.phase == null
                || s.exerciseIndex < 0
                || s.exerciseIndex > day.exercises.size()
                || s.setIndex < 0
                || s.remainingSeconds < 0
                || s.remainingSeconds > 240
                || s.activeSeconds < 0
                || s.completedSets < 0) throw new IllegalArgumentException("训练快照无效");
        this.day = day;
        id = s.id;
        exerciseIndex = s.exerciseIndex;
        setIndex = s.setIndex;
        remainingSeconds = s.remainingSeconds;
        activeSeconds = s.activeSeconds;
        completedSets = s.completedSets;
        resumePhase = s.resumePhase;
        if (exerciseIndex < day.exercises.size()
                && setIndex >= day.exercises.get(exerciseIndex).sets)
            throw new IllegalArgumentException("训练组数无效");
        phase = restorePhase(s.phase);
        if (exerciseIndex == day.exercises.size()
                && !(phase instanceof Cooldown)
                && !(phase instanceof Complete)
                && !(phase instanceof Paused)) throw new IllegalArgumentException("训练进度无效");
        if (phase instanceof Paused) restorePhase(resumePhase);
    }

    private Phase restorePhase(String name) {
        if (name == null) throw new IllegalArgumentException("训练状态缺失");
        switch (name) {
            case "WARMUP":
                return new Warmup();
            case "RUNNING":
                return new Running();
            case "REST":
                return new Rest();
            case "COOLDOWN":
                return new Cooldown();
            case "PAUSED":
                return new Paused();
            case "COMPLETE":
                return new Complete();
            default:
                throw new IllegalArgumentException("未知训练状态");
        }
    }

    public void primary() {
        phase.primary();
    }

    public void pause() {
        if (!(phase instanceof Paused) && !(phase instanceof Complete)) {
            resumePhase = phase.name();
            phase = new Paused();
        }
    }

    public void tick(int seconds) {
        if (seconds < 0) throw new IllegalArgumentException("时钟不能倒退");
        if (!(phase instanceof Paused) && !(phase instanceof Complete)) {
            activeSeconds += seconds;
            phase.tick(seconds);
        }
    }

    public Snapshot snapshot() {
        Snapshot s = new Snapshot();
        s.id = id;
        s.title = day.title;
        s.phase = phase.name();
        s.resumePhase = resumePhase;
        s.exerciseIndex = exerciseIndex;
        s.setIndex = setIndex;
        s.remainingSeconds = remainingSeconds;
        s.activeSeconds = activeSeconds;
        s.completedSets = completedSets;
        return s;
    }

    public String phase() {
        return phase.name();
    }

    public String action() {
        return phase.action();
    }

    public boolean complete() {
        return phase instanceof Complete;
    }

    public Prescription current() {
        return day.exercises.get(Math.min(exerciseIndex, day.exercises.size() - 1));
    }

    public Exercise exercise() {
        return ExerciseCatalog.get(current().exerciseId);
    }

    public int exerciseIndex() {
        return exerciseIndex;
    }

    public int setIndex() {
        return setIndex;
    }

    public int remaining() {
        return remainingSeconds;
    }

    public int seconds() {
        return activeSeconds;
    }

    public DayPlan day() {
        return day;
    }

    public TrainingRecord record(long time) {
        if (!complete()) throw new IllegalStateException("训练尚未完成");
        return new TrainingRecord(id, day.title, time, activeSeconds, completedSets);
    }
}
