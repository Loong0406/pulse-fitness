package com.pulse.fitness.ui;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.view.*;
import android.widget.*;
import com.pulse.fitness.R;
import com.pulse.core.model.*;
import com.pulse.core.plan.ExerciseCatalog;
import java.time.*;
import java.util.*;

public final class Ui {
    public static int dp(Context c, int n) {
        return Math.round(c.getResources().getDisplayMetrics().density * n);
    }

    public static View screen(Context c, int layout) {
        return LayoutInflater.from(c).inflate(layout, new FrameLayout(c), false);
    }

    public static void text(View root, int id, String value) {
        ((TextView) root.findViewById(id)).setText(value);
    }

    public static String goal(UserProfile p) {
        if (p == null) return "未设置目标";
        switch (p.goal) {
            case FAT_LOSS:
                return "减脂塑形";
            case STRENGTH:
                return "增肌力量";
            default:
                return "改善体能";
        }
    }

    public static String equipment(UserProfile p) {
        switch (p.equipment) {
            case DUMBBELL:
                return "哑铃";
            case GYM:
                return "健身房";
            default:
                return "徒手";
        }
    }

    public static String clock(int s) {
        return String.format(Locale.CHINA, "%02d:%02d", s / 60, s % 60);
    }

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    public static long weekStart() {
        LocalDate monday =
                LocalDate.now()
                        .with(
                                java.time.temporal.TemporalAdjusters.previousOrSame(
                                        DayOfWeek.MONDAY));
        return monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static void insets(Activity activity, View root, int padding) {
        if (Build.VERSION.SDK_INT >= 30) activity.getWindow().setDecorFitsSystemWindows(false);
        root.setOnApplyWindowInsetsListener(
                (v, insets) -> {
                    int top, bottom, left, right;
                    if (Build.VERSION.SDK_INT >= 30) {
                        android.graphics.Insets i =
                                insets.getInsets(
                                        WindowInsets.Type.systemBars()
                                                | WindowInsets.Type.displayCutout()
                                                | WindowInsets.Type.ime());
                        top = i.top;
                        bottom = i.bottom;
                        left = i.left;
                        right = i.right;
                    } else {
                        top = insets.getSystemWindowInsetTop();
                        bottom = insets.getSystemWindowInsetBottom();
                        left = insets.getSystemWindowInsetLeft();
                        right = insets.getSystemWindowInsetRight();
                    }
                    v.setPadding(left + padding, top + padding, right + padding, bottom + padding);
                    return insets.consumeSystemWindowInsets();
                });
        root.requestApplyInsets();
    }

    public static TextView paragraph(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextColor(c.getColor(R.color.secondary));
        t.setTextSize(14);
        t.setLineSpacing(dp(c, 5), 1);
        t.setPadding(0, dp(c, 10), 0, dp(c, 10));
        return t;
    }

    public static void detail(Context c, Exercise e) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 24);
        box.setPadding(p, p / 2, p, p / 2);
        box.addView(
                paragraph(
                        c,
                        e.group
                                + "  /  "
                                + e.equipmentLabel
                                + "  /  "
                                + (e.timed ? "计时动作" : "计次动作")));
        box.addView(paragraph(c, e.cue));
        int n = 1;
        for (String step : e.steps) box.addView(paragraph(c, (n++) + ". " + step));
        box.addView(paragraph(c, "动作只在舒适、无痛的范围内完成；双侧动作的次数为两侧合计。"));
        ScrollView scroll = new ScrollView(c);
        scroll.addView(box);
        new AlertDialog.Builder(c)
                .setTitle(e.name)
                .setView(scroll)
                .setPositiveButton("知道了", null)
                .show();
    }

    public static String rx(Prescription p) {
        Exercise e = ExerciseCatalog.get(p.exerciseId);
        return p.sets
                + " 组 × "
                + p.amount
                + (e.timed ? " 秒" : " 次")
                + "  ·  休息 "
                + p.restSeconds
                + " 秒";
    }

    private Ui() {}
}
