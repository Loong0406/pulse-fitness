package com.pulse.core.plan;

import com.pulse.core.model.*;

/** Simple Factory selects the small, fixed set of supported goals. */
public final class GoalStrategyFactory {
    public static GoalStrategy create(UserProfile.Goal goal) {
        switch (goal) {
            case FAT_LOSS:
                return new FatLoss();
            case STRENGTH:
                return new Strength();
            default:
                return new Wellbeing();
        }
    }

    public static final class FatLoss implements GoalStrategy {
        public int repetitions() {
            return 12;
        }

        public int priority(Exercise e) {
            return e.group.equals("有氧") ? 0 : e.group.equals("活动度") ? 2 : 1;
        }

        public String title() {
            return "低冲击燃动";
        }
    }

    public static final class Strength implements GoalStrategy {
        public int repetitions() {
            return 10;
        }

        public int priority(Exercise e) {
            return e.group.equals("有氧") || e.group.equals("活动度") ? 2 : 0;
        }

        public String title() {
            return "基础力量";
        }
    }

    public static final class Wellbeing implements GoalStrategy {
        public int repetitions() {
            return 10;
        }

        public int priority(Exercise e) {
            return e.group.equals("活动度") ? 0 : 1;
        }

        public String title() {
            return "全身唤醒";
        }
    }

    private GoalStrategyFactory() {}
}
