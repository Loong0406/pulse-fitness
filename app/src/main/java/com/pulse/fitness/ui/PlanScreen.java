package com.pulse.fitness.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.*;
import android.widget.*;
import com.pulse.fitness.R;
import com.pulse.fitness.presentation.FitnessViewModel;
import com.pulse.core.model.*;
import com.pulse.core.plan.*;

public final class PlanScreen implements Screen {
    private final View root;
    private final Context context;
    private final FitnessViewModel vm;
    private final NavigationMediator navigation;
    private WeekPlan rendered;

    public PlanScreen(Context c, FitnessViewModel vm, NavigationMediator navigation) {
        context = c;
        this.vm = vm;
        this.navigation = navigation;
        root = Ui.screen(c, R.layout.screen_plan);
        root.findViewById(R.id.plan_ai)
                .setOnClickListener(
                        v -> {
                            if (vm.state.get().profile == null) {
                                navigation.show(NavigationMediator.Tab.PROFILE);
                                return;
                            }
                            new AlertDialog.Builder(c)
                                    .setTitle("为你生成专属计划")
                                    .setMessage(
                                            "将年龄、身高体重、目标、训练经验、器械、可用时间和动作限制发送到你配置的 AI 服务。模型调用由该服务完成，模型不会收到昵称或性别。请确认服务地址可信。")
                                    .setNegativeButton("取消", null)
                                    .setPositiveButton(
                                            "同意并生成",
                                            (d, w) ->
                                                    navigation.runWithBackendAccess(
                                                            () -> vm.generate(true)))
                                    .show();
                        });
        root.findViewById(R.id.plan_local)
                .setOnClickListener(
                        v -> {
                            if (vm.state.get().profile == null)
                                navigation.show(NavigationMediator.Tab.PROFILE);
                            else vm.generate(false);
                        });
        root.findViewById(R.id.plan_copy)
                .setOnClickListener(
                        v ->
                                new AlertDialog.Builder(c)
                                        .setTitle("开始新的训练周期")
                                        .setMessage("沿用这份计划创建新周期；未完成的训练进度会重置，历史记录会保留。")
                                        .setNegativeButton("取消", null)
                                        .setPositiveButton("创建新周期", (d, w) -> vm.copyPlan())
                                        .show());
        root.findViewById(R.id.plan_export).setOnClickListener(v -> navigation.exportPlan());
    }

    public View view() {
        return root;
    }

    public void bind(FitnessViewModel.UiState s) {
        Ui.text(
                root,
                R.id.plan_summary,
                s.profile == null
                        ? "先完善档案，让训练围绕你的实际情况展开。"
                        : Ui.goal(s.profile)
                                + "  /  "
                                + Ui.equipment(s.profile)
                                + "  /  每周 "
                                + s.profile.daysPerWeek
                                + " 天  /  每次 "
                                + s.profile.minutesPerSession
                                + " 分钟");
        Ui.text(
                root,
                R.id.plan_source,
                s.plan == null
                        ? "✦ 训练计划，因人而异"
                        : s.plan.source == WeekPlan.Source.AI
                                ? "✦ 大模型生成 · 已通过规则校验"
                                : "◉ 本地规则计划 · 可离线使用");
        Ui.text(
                root,
                R.id.plan_note,
                s.plan == null ? "告诉我们你的目标、时间和身体情况，创建一份有训练、有恢复的一周计划。" : s.plan.note);
        Ui.text(root, R.id.plan_message, s.message);
        root.findViewById(R.id.plan_loading).setVisibility(s.busy ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.plan_ai).setEnabled(!s.busy);
        root.findViewById(R.id.plan_local).setEnabled(!s.busy);
        root.findViewById(R.id.plan_copy).setEnabled(!s.busy && s.plan != null);
        root.findViewById(R.id.plan_export).setEnabled(s.plan != null);
        Ui.text(root, R.id.plan_ai, s.busy ? "正在安排你的训练…" : "✦ 生成 AI 专属计划");
        if (rendered == s.plan
                && ((LinearLayout) root.findViewById(R.id.plan_days)).getChildCount() > 0) return;
        rendered = s.plan;
        LinearLayout list = root.findViewById(R.id.plan_days);
        list.removeAllViews();
        if (s.plan == null) {
            list.addView(Ui.paragraph(context, "计划还未开始。\n点击上方按钮，创建属于你的第一周。"));
            return;
        }
        // Iterator: screens traverse the aggregate without accessing its collection implementation.
        for (DayPlan day : s.plan) {
            View item = LayoutInflater.from(context).inflate(R.layout.item_day, list, false);
            Ui.text(item, R.id.day_number, "周" + "一二三四五六日".charAt(day.dayIndex));
            Ui.text(item, R.id.day_title, day.title);
            Ui.text(
                    item,
                    R.id.day_meta,
                    day.isRest()
                            ? "休息 / 轻松活动 / 保持睡眠"
                            : day.exercises.size()
                                    + " 个动作 · 约 "
                                    + PlanStatisticsVisitor.minutes(day)
                                    + " 分钟（含热身放松）");
            View start = item.findViewById(R.id.day_start);
            start.setVisibility(day.isRest() ? View.GONE : View.VISIBLE);
            start.setEnabled(!s.busy);
            start.setOnClickListener(v -> navigation.startTraining(day));
            LinearLayout moves = item.findViewById(R.id.day_exercises);
            for (Prescription p : day.exercises) {
                TextView line =
                        Ui.paragraph(
                                context, ExerciseCatalog.get(p.exerciseId).name + "\n" + Ui.rx(p));
                line.setTextSize(12);
                line.setMinHeight(Ui.dp(context, 48));
                line.setOnClickListener(v -> Ui.detail(context, ExerciseCatalog.get(p.exerciseId)));
                moves.addView(line);
            }
            list.addView(item);
        }
    }
}
