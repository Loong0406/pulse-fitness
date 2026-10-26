package com.pulse.fitness.ui;

import android.content.Context;
import android.view.*;
import android.widget.*;
import com.pulse.fitness.R;
import com.pulse.fitness.presentation.FitnessViewModel;
import com.pulse.fitness.ui.widget.WeeklyChartView;
import com.pulse.core.model.TrainingRecord;
import java.text.SimpleDateFormat;
import java.util.*;
import java.time.*;

public final class ProgressScreen implements Screen {
    private final View root;
    private final Context context;
    private final FitnessViewModel vm;

    public ProgressScreen(Context c, FitnessViewModel vm) {
        context = c;
        this.vm = vm;
        root = Ui.screen(c, R.layout.screen_progress);
    }

    public View view() {
        return root;
    }

    public void bind(FitnessViewModel.UiState s) {
        List<TrainingRecord> records = vm.history();
        int seconds = 0, sets = 0;
        float[] week = new float[7];
        for (TrainingRecord r : records) {
            seconds += r.activeSeconds;
            sets += r.completedSets;
            if (r.completedAt >= Ui.weekStart()) {
                int day =
                        Instant.ofEpochMilli(r.completedAt)
                                        .atZone(ZoneId.systemDefault())
                                        .getDayOfWeek()
                                        .getValue()
                                - 1;
                week[day] += r.activeSeconds / 60f;
            }
        }
        Ui.text(root, R.id.progress_total, records.size() + " 次训练");
        Ui.text(root, R.id.progress_meta, (seconds / 60) + " 分钟投入  /  " + sets + " 组动作完成");
        ((WeeklyChartView) root.findViewById(R.id.progress_chart)).setMinutes(week);
        LinearLayout history = root.findViewById(R.id.progress_history);
        history.removeAllViews();
        if (records.isEmpty())
            history.addView(Ui.paragraph(context, "还没有训练记录。\n完成计划中的一场训练后，你的足迹会出现在这里。"));
        int shown = 0;
        for (TrainingRecord r : records) {
            if (shown++ >= 50) break;
            TextView card =
                    Ui.paragraph(
                            context,
                            r.title
                                    + "\n"
                                    + new SimpleDateFormat("MM 月 dd 日  HH:mm", Locale.CHINA)
                                            .format(new Date(r.completedAt))
                                    + "  ·  "
                                    + Ui.clock(r.activeSeconds)
                                    + "  ·  "
                                    + r.completedSets
                                    + " 组");
            card.setBackgroundResource(R.drawable.bg_card);
            int padding = Ui.dp(context, 18);
            card.setPadding(padding, padding, padding, padding);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = Ui.dp(context, 12);
            history.addView(card, lp);
        }
        if (records.size() > 50) history.addView(Ui.paragraph(context, "展示最近 50 次训练，累计统计包含全部记录。"));
    }
}
