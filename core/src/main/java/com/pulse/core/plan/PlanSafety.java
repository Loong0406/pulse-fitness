package com.pulse.core.plan;

import com.pulse.core.model.*;
import java.util.*;

/** Model output is untrusted. Only catalog exercises and bounded loads can reach the UI. */
public final class PlanSafety {
    public static void validate(WeekPlan plan, UserProfile p) {
        if (plan == null
                || plan.days == null
                || plan.days.size() != 7
                || plan.source == null
                || plan.id == null
                || plan.id.length() > 128) bad("计划格式无效");
        if (!p.signature().equals(plan.profileSignature)) bad("计划与当前档案不匹配，请重新生成");
        if (plan.note == null || plan.note.length() > 1200) bad("计划说明过长");
        int trainingDays = 0;
        for (int i = 0; i < 7; i++) {
            DayPlan day = plan.days.get(i);
            if (day == null
                    || day.dayIndex != i
                    || day.title == null
                    || day.title.length() > 48
                    || day.exercises == null) bad("日程格式无效");
            if (day.isRest()) continue;
            trainingDays++;
            if (day.exercises.size() < 2 || day.exercises.size() > 6) bad("每次需安排 2–6 个动作");
            Set<String> ids = new HashSet<>();
            int totalSets = 0;
            for (Prescription rx : day.exercises) {
                if (rx == null) bad("动作缺失");
                Exercise e = ExerciseCatalog.get(rx.exerciseId);
                if (!e.isAllowed(p) || !ids.add(e.id)) bad("动作不符合档案限制或重复");
                int maxSets =
                        (p.experience == UserProfile.Experience.BEGINNER
                                        || p.age >= 60
                                        || p.daysPerWeek >= 4)
                                ? 2
                                : 4;
                if (rx.sets < 1 || rx.sets > maxSets || rx.restSeconds < 30 || rx.restSeconds > 120)
                    bad("组数或休息超出范围");
                if ((e.timed && (rx.amount < 15 || rx.amount > 60))
                        || (!e.timed && (rx.amount < 6 || rx.amount > 15))) bad("次数或时长超出范围");
                totalSets += rx.sets;
            }
            if (totalSets > 18 || PlanStatisticsVisitor.minutes(day) > p.minutesPerSession)
                bad("训练量超出可用时间");
        }
        if (trainingDays != p.daysPerWeek) bad("训练天数与档案不一致");
    }

    private static void bad(String m) {
        throw new IllegalArgumentException(m);
    }

    private PlanSafety() {}
}
