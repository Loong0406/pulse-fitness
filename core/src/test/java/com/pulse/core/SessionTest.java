package com.pulse.core;

import org.junit.Test;
import static org.junit.Assert.*;
import com.pulse.core.session.WorkoutSession;
import com.pulse.core.model.*;
import com.pulse.core.util.JsonCodec;
import java.util.*;

public class SessionTest {
    private DayPlan day() {
        return new DayPlan(
                0,
                "基础训练",
                Arrays.asList(
                        new Prescription("wall_push", 2, 10, 30),
                        new Prescription("calf_raise", 1, 10, 45)));
    }

    @Test
    public void pauseProcessRestoreAndCompletionPreserveProgress() {
        WorkoutSession s = new WorkoutSession(day());
        assertEquals("WARMUP", s.phase());
        s.tick(15);
        s.primary();
        s.tick(30);
        s.primary();
        assertEquals("REST", s.phase());
        s.tick(10);
        s.pause();
        s.tick(100);
        assertEquals(55, s.seconds());
        assertEquals(20, s.remaining());
        WorkoutSession.Snapshot snapshot =
                JsonCodec.GSON.fromJson(
                        JsonCodec.GSON.toJson(s.snapshot()), WorkoutSession.Snapshot.class);
        WorkoutSession restored = new WorkoutSession(day(), snapshot);
        restored.primary();
        restored.tick(20);
        assertEquals("RUNNING", restored.phase());
        assertEquals(1, restored.setIndex());
        restored.primary();
        assertEquals("REST", restored.phase());
        restored.primary();
        assertEquals(1, restored.exerciseIndex());
        restored.primary();
        assertEquals("COOLDOWN", restored.phase());
        assertThrows(IllegalStateException.class, () -> restored.record(1));
        restored.tick(240);
        restored.primary();
        assertTrue(restored.complete());
        assertEquals(3, restored.record(1).completedSets);
        assertEquals(snapshot.id, restored.record(1).id);
        int seconds = restored.seconds();
        restored.tick(100);
        assertEquals(seconds, restored.seconds());
    }

    @Test
    public void invalidProgressAndBackwardClockAreRejected() {
        WorkoutSession s = new WorkoutSession(day());
        WorkoutSession.Snapshot snapshot = s.snapshot();
        snapshot.exerciseIndex = 20;
        assertThrows(IllegalArgumentException.class, () -> new WorkoutSession(day(), snapshot));
        assertThrows(IllegalArgumentException.class, () -> s.tick(-1));
    }
}
