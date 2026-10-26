package com.pulse.core;

import org.junit.Test;
import static org.junit.Assert.*;
import com.pulse.core.model.*;
import com.pulse.core.plan.*;
import com.pulse.core.data.*;
import com.pulse.core.export.PlanExporter;
import com.pulse.core.util.JsonCodec;
import com.pulse.core.validation.ProfileValidator;
import java.util.*;

public class PlanningTest {
    @Test
    public void allSupportedProfilesRespectTimeEquipmentAndLimitations() throws Exception {
        int checked = 0;
        for (UserProfile.Goal goal : UserProfile.Goal.values())
            for (UserProfile.Experience experience : UserProfile.Experience.values())
                for (UserProfile.Equipment equipment : UserProfile.Equipment.values())
                    for (int days = 2; days <= 5; days++)
                        for (int minutes : new int[] {15, 20, 30, 40, 45, 60})
                            for (int flags = 0; flags < 8; flags++)
                                for (int age : new int[] {18, 35, 65}) {
                                    UserProfile p =
                                            UserProfile.defaults().toBuilder()
                                                    .goal(goal)
                                                    .experience(experience)
                                                    .equipment(equipment)
                                                    .age(age)
                                                    .schedule(days, minutes)
                                                    .limitations(
                                                            (flags & 1) != 0,
                                                            (flags & 2) != 0,
                                                            (flags & 4) != 0,
                                                            false)
                                                    .build();
                                    WeekPlan plan =
                                            new PlanGeneratorFactory.Local()
                                                    .createSafeGenerator()
                                                    .generate(p);
                                    PlanSafety.validate(plan, p);
                                    int count = 0;
                                    for (DayPlan day : plan) {
                                        if (day.isRest()) continue;
                                        count++;
                                        assertTrue(PlanStatisticsVisitor.minutes(day) <= minutes);
                                        for (Prescription rx : day.exercises)
                                            assertTrue(
                                                    ExerciseCatalog.get(rx.exerciseId)
                                                            .isAllowed(p));
                                    }
                                    assertEquals(days, count);
                                    checked++;
                                }
        assertEquals(15552, checked);
    }

    @Test
    public void healthGateRejectsUnsupportedPopulationWithoutLosingStoredProfile() {
        for (UserProfile p :
                new UserProfile[] {
                    UserProfile.defaults().toBuilder().age(16).build(),
                    UserProfile.defaults().toBuilder().age(75).build(),
                    UserProfile.defaults().toBuilder()
                            .limitations(false, false, false, true)
                            .build()
                }) {
            ProfileValidator.validateForStorage(p);
            assertThrows(
                    IllegalArgumentException.class, () -> new LocalPlanGenerator().generate(p));
        }
    }

    @Test
    public void incompatibleOrUnsafeModelPlanIsRejected() throws Exception {
        UserProfile p = UserProfile.defaults();
        WeekPlan original = new LocalPlanGenerator().generate(p);
        List<DayPlan> days = new ArrayList<>(original.days);
        days.set(
                0,
                new DayPlan(
                        0,
                        "unsafe",
                        Arrays.asList(
                                new Prescription("db_row", 4, 100, 1),
                                new Prescription("squat", 2, 10, 45))));
        WeekPlan unsafe = new WeekPlan("x", 1, p.signature(), WeekPlan.Source.AI, "", days);
        assertThrows(IllegalArgumentException.class, () -> PlanSafety.validate(unsafe, p));
        UserProfile changed = p.toBuilder().equipment(UserProfile.Equipment.GYM).build();
        assertThrows(IllegalArgumentException.class, () -> PlanSafety.validate(original, changed));
    }

    @Test
    public void copyingAndExportingKeepsOriginalIndependent() throws Exception {
        WeekPlan p = new LocalPlanGenerator().generate(UserProfile.defaults());
        WeekPlan copy = p.copyForNextWeek();
        assertNotEquals(p.id, copy.id);
        assertNotSame(p.days.get(0), copy.days.get(0));
        assertNotSame(p.days.get(0).exercises.get(0), copy.days.get(0).exercises.get(0));
        assertThrows(UnsupportedOperationException.class, () -> copy.days.clear());
        PlanExporter.MemorySink sink = new PlanExporter.MemorySink();
        new PlanExporter.Json(sink).export(copy);
        WeekPlan loaded = JsonCodec.plan(sink.value);
        PlanSafety.validate(loaded, UserProfile.defaults());
        new PlanExporter.Text(sink).export(p);
        assertTrue(sink.value.contains("本地规则"));
        assertTrue(
                sink.value.contains(
                        ExerciseCatalog.get(p.days.get(0).exercises.get(0).exerciseId).name));
        PlanStatisticsVisitor stats = new PlanStatisticsVisitor();
        p.accept(stats);
        assertEquals(3, stats.trainingDays);
        assertTrue(stats.totalSets > 0);
    }

    @Test
    public void saveUndoAndDuplicateRecordAreConsistent() {
        Repositories.Factory f = new MemoryRepositoryFactory();
        f.profiles().save(UserProfile.defaults());
        ProfileCommand command =
                new ProfileCommand(
                        f.profiles(), UserProfile.defaults().toBuilder().name("小林").build());
        command.execute();
        assertEquals("小林", f.profiles().get().name);
        command.undo();
        assertEquals("训练者", f.profiles().get().name);
        TrainingRecord r = new TrainingRecord("once", "训练", 1, 100, 4);
        f.history().add(r);
        f.history().add(r);
        assertEquals(1, f.history().all().size());
    }

    @Test
    public void filterGrammarRespectsAndBeforeOr() {
        ExerciseQuery.Expression query = ExerciseQuery.parse("group:上肢 & equipment:徒手 | group:核心");
        assertTrue(query.matches(ExerciseCatalog.get("wall_push")));
        assertTrue(query.matches(ExerciseCatalog.get("dead_bug")));
        assertFalse(query.matches(ExerciseCatalog.get("db_row")));
        assertFalse(query.matches(ExerciseCatalog.get("squat")));
        assertThrows(IllegalArgumentException.class, () -> ExerciseQuery.parse("上肢 &"));
    }

    @Test
    public void jsonBoundariesRejectFractionalIntegersAndInvalidBodies() {
        String p = JsonCodec.profile(UserProfile.defaults());
        assertThrows(
                RuntimeException.class,
                () -> JsonCodec.profile(p.replace("\"age\":25", "\"age\":25.5")));
        assertThrows(
                RuntimeException.class,
                () -> JsonCodec.profile(p.replace("\"heightCm\":170.0", "\"heightCm\":1")));
    }
}
