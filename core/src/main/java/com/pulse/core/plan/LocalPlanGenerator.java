package com.pulse.core.plan;

import com.pulse.core.model.*;
import java.util.*;

public final class LocalPlanGenerator extends PlanGenerationTemplate {
    @Override
    protected WeekPlan createCandidate(UserProfile p) {
        GoalStrategy strategy = GoalStrategyFactory.create(p.goal);
        List<Exercise> allowed = ExerciseCatalog.allowed(p);
        allowed.sort(Comparator.comparingInt(strategy::priority));
        Set<Integer> scheduled = new HashSet<>();
        for (int i = 0; i < p.daysPerWeek; i++) scheduled.add(i * 7 / p.daysPerWeek);
        List<DayPlan> days = new ArrayList<>();
        int trainingIndex = 0;
        for (int d = 0; d < 7; d++) {
            List<Prescription> items = new ArrayList<>();
            if (scheduled.contains(d)) {
                int targetCount = Math.min(5, 2 + (p.minutesPerSession - 15) / 8);
                int sets =
                        p.experience == UserProfile.Experience.BEGINNER
                                        || p.age >= 60
                                        || p.daysPerWeek >= 4
                                        || p.minutesPerSession < 20
                                ? 2
                                : 3;
                for (int i = 0; i < allowed.size() && items.size() < targetCount; i++) {
                    Exercise e = allowed.get((i + trainingIndex * 3) % allowed.size());
                    // Separate consecutive-day emphasis and keep high-impact moves out of basic
                    // plans.
                    if (e.impact) continue;
                    Prescription rx =
                            new Prescription(e.id, sets, e.timed ? 30 : strategy.repetitions(), 45);
                    items.add(rx);
                    if (PlanStatisticsVisitor.minutes(new DayPlan(d, "", items))
                            > p.minutesPerSession) items.remove(items.size() - 1);
                }
                trainingIndex++;
            }
            days.add(
                    new DayPlan(
                            d,
                            items.isEmpty() ? "主动恢复" : strategy.title() + " · " + (trainingIndex),
                            items));
        }
        String note = "依据你的目标、器械、时间和动作限制匹配；每次含约 4 分钟热身与 4 分钟放松。保持能控制动作的强度，出现疼痛、胸闷或头晕时停止。";
        return new WeekPlan(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                p.signature(),
                WeekPlan.Source.LOCAL,
                note,
                days);
    }
}
