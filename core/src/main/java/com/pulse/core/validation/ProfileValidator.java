package com.pulse.core.validation;

import com.pulse.core.model.UserProfile;

/** Chain of Responsibility: independently replaceable numeric, schedule and health handlers. */
public final class ProfileValidator {
    public abstract static class Handler {
        private Handler next;

        public Handler then(Handler handler) {
            next = handler;
            return handler;
        }

        public final void validate(UserProfile p) {
            check(p);
            if (next != null) next.validate(p);
        }

        protected abstract void check(UserProfile p);
    }

    public static final class Basics extends Handler {
        @Override
        protected void check(UserProfile p) {
            if (p == null || p.name == null || p.name.isEmpty() || p.name.length() > 24)
                fail("昵称请输入 1–24 个字");
            if (p.age < 12 || p.age > 90) fail("年龄请输入 12–90 岁");
            if (!Double.isFinite(p.heightCm)
                    || !Double.isFinite(p.weightKg)
                    || p.heightCm < 100
                    || p.heightCm > 230
                    || p.weightKg < 25
                    || p.weightKg > 250) fail("请检查身高（100–230 cm）和体重（25–250 kg）");
            if (p.goal == null
                    || p.experience == null
                    || p.equipment == null
                    || p.sex == null
                    || p.sex.length() > 12) fail("请完整选择个人档案");
        }
    }

    public static final class Schedule extends Handler {
        @Override
        protected void check(UserProfile p) {
            if (p.daysPerWeek < 2
                    || p.daysPerWeek > 5
                    || p.minutesPerSession < 15
                    || p.minutesPerSession > 60) fail("每周安排 2–5 天，每次 15–60 分钟");
        }
    }

    public static final class Health extends Handler {
        @Override
        protected void check(UserProfile p) {
            if (p.age < 18 || p.age > 69 || p.needsProfessional)
                fail("此档案需要专业人员评估后制定训练计划。当前版本仅为 18–69 岁、无医疗运动限制的成年人自动生成计划。");
        }
    }

    public static void validateForStorage(UserProfile p) {
        Handler root = new Basics();
        root.then(new Schedule());
        root.validate(p);
    }

    public static void validateForTraining(UserProfile p) {
        Handler root = new Basics();
        root.then(new Schedule()).then(new Health());
        root.validate(p);
    }

    private static void fail(String message) {
        throw new IllegalArgumentException(message);
    }

    private ProfileValidator() {}
}
