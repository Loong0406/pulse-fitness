package com.pulse.fitness.ui;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.pulse.fitness.R;
import com.pulse.fitness.presentation.FitnessViewModel;
import com.pulse.core.model.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class HomeScreen implements Screen {
    private final View root;
    private final Context context;
    private final NavigationMediator navigation;
    private FitnessViewModel.UiState state;

    public HomeScreen(Context c, FitnessViewModel vm, NavigationMediator navigation) {
        context = c;
        this.navigation = navigation;
        root = Ui.screen(c, R.layout.screen_home);
        root.findViewById(R.id.home_avatar)
                .setOnClickListener(v -> navigation.show(NavigationMediator.Tab.PROFILE));
        root.findViewById(R.id.home_all_plan)
                .setOnClickListener(v -> navigation.show(NavigationMediator.Tab.PLAN));
        root.findViewById(R.id.home_start)
                .setOnClickListener(
                        v -> {
                            if (state.profile == null)
                                navigation.show(NavigationMediator.Tab.PROFILE);
                            else if (state.plan == null)
                                navigation.show(NavigationMediator.Tab.PLAN);
                            else
                                navigation.startTraining(
                                        state.plan.days.get(
                                                LocalDate.now().getDayOfWeek().getValue() - 1));
                        });
    }

    public View view() {
        return root;
    }

    public void bind(FitnessViewModel.UiState s) {
        state = s;
        Ui.text(
                root,
                R.id.home_date,
                LocalDate.now()
                                .format(
                                        DateTimeFormatter.ofPattern(
                                                "MM 月 dd 日  ·  EEEE", java.util.Locale.CHINA))
                        + "  /  LET'S MOVE");
        Ui.text(
                root,
                R.id.home_greeting,
                s.profile == null ? "今天，为自己开练。" : s.profile.name + "，今天也要加油。");
        Ui.text(root, R.id.home_avatar, s.profile == null ? "你" : s.profile.name.substring(0, 1));
        Ui.text(
                root,
                R.id.home_subtitle,
                s.profile == null
                        ? "一点进步，也值得认真对待。"
                        : Ui.goal(s.profile)
                                + "  ·  "
                                + s.profile.daysPerWeek
                                + " 天 / 周  ·  按自己的节奏前进");
        Ui.text(
                root,
                R.id.home_source,
                s.plan == null
                        ? "PERSONAL TRAINING"
                        : s.plan.source == WeekPlan.Source.AI
                                ? "AI PERSONAL PLAN"
                                : "LOCAL PERSONAL PLAN");
        DayPlan today =
                s.plan == null
                        ? null
                        : s.plan.days.get(LocalDate.now().getDayOfWeek().getValue() - 1);
        Ui.text(
                root,
                R.id.home_plan_hint,
                today == null ? "从一份专属计划开始" : today.isRest() ? "今天轻松恢复，明天继续进步" : today.title);
        Ui.text(
                root,
                R.id.home_start,
                s.profile == null
                        ? "完善档案  ↗"
                        : s.plan == null ? "创建计划  ↗" : today.isRest() ? "查看恢复日  ↗" : "开始今日训练  ↗");
        root.findViewById(R.id.home_start).setEnabled(!s.busy);
        root.findViewById(R.id.home_start)
                .setOnClickListener(
                        v -> {
                            if (s.profile == null) navigation.show(NavigationMediator.Tab.PROFILE);
                            else if (s.plan == null || today.isRest())
                                navigation.show(NavigationMediator.Tab.PLAN);
                            else navigation.startTraining(today);
                        });
        int count = 0, seconds = 0;
        for (TrainingRecord r : com.pulse.fitness.AppContainer.get().viewModel.history()) {
            seconds += r.activeSeconds;
            if (r.completedAt >= Ui.weekStart()) count++;
        }
        Ui.text(root, R.id.home_sessions, count + " 次");
        Ui.text(root, R.id.home_minutes, (seconds / 60) + " 分钟");
        LinearLayout week = root.findViewById(R.id.home_week);
        week.removeAllViews();
        int currentDay = LocalDate.now().getDayOfWeek().getValue() - 1;
        for (int i = 0; i < 7; i++) {
            final int index = i;
            boolean train = s.plan != null && !s.plan.days.get(i).isRest();
            TextView chip = new TextView(context);
            chip.setGravity(Gravity.CENTER);
            chip.setText("一二三四五六日".substring(i, i + 1) + "\n\n" + (train ? "●" : "—"));
            chip.setTextSize(12);
            chip.setTextColor(context.getColor(i == currentDay ? R.color.ink : R.color.muted));
            chip.setBackgroundResource(i == currentDay ? R.drawable.bg_accent : R.drawable.bg_card);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
            if (i > 0) lp.setMarginStart(Ui.dp(context, 5));
            week.addView(chip, lp);
            chip.setContentDescription("星期" + "一二三四五六日".charAt(i) + (train ? "，训练日" : "，恢复日"));
            chip.setOnClickListener(v -> navigation.show(NavigationMediator.Tab.PLAN));
        }
        Ui.text(
                root,
                R.id.home_tip,
                s.plan == null ? "先完善档案，再为自己安排一周的训练节奏。" : "实心圆点为训练日。恢复和训练一样，都是进步的一部分。");
    }
}
