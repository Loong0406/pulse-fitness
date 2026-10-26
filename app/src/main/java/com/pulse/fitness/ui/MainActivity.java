package com.pulse.fitness.ui;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.pulse.fitness.*;
import com.pulse.fitness.presentation.FitnessViewModel;
import com.pulse.core.model.*;
import com.pulse.core.export.PlanExporter;
import com.pulse.core.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends BackendActivity implements NavigationMediator {
    private final int[] navigation = {
        R.id.nav_home, R.id.nav_plan, R.id.nav_exercise, R.id.nav_progress, R.id.nav_profile
    };
    private FitnessViewModel vm;
    private Screen screen;
    private Tab current = Tab.HOME;
    private WeekPlan exportSnapshot;
    private boolean exportJson;
    private final ObservableState.Listener<FitnessViewModel.UiState> observer =
            state -> {
                if (screen != null) screen.bind(state);
            };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!AppContainer.get().sessions.canEnterApp()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        Ui.insets(this, findViewById(R.id.root), 0);
        vm = AppContainer.get().viewModel;
        for (int i = 0; i < navigation.length; i++) {
            final Tab tab = Tab.values()[i];
            findViewById(navigation[i]).setOnClickListener(v -> show(tab));
        }
        if (state != null) {
            try {
                current = Tab.valueOf(state.getString("tab", "HOME"));
                String plan = state.getString("exportPlan");
                if (plan != null) exportSnapshot = JsonCodec.plan(plan);
                exportJson = state.getBoolean("exportJson");
            } catch (RuntimeException ignored) {
            }
        }
        show(current);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (vm != null) {
            vm.refresh();
            vm.state.observe(observer);
        }
    }

    @Override
    protected void onStop() {
        if (vm != null) vm.state.remove(observer);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("tab", current.name());
        if (exportSnapshot != null) state.putString("exportPlan", JsonCodec.plan(exportSnapshot));
        state.putBoolean("exportJson", exportJson);
    }

    @Override
    public void show(Tab tab) {
        current = tab;
        switch (tab) {
            case HOME:
                screen = new HomeScreen(this, vm, this);
                break;
            case PLAN:
                screen = new PlanScreen(this, vm, this);
                break;
            case EXERCISES:
                screen = new ExercisesScreen(this);
                break;
            case PROGRESS:
                screen = new ProgressScreen(this, vm);
                break;
            default:
                screen = new ProfileScreen(this, vm, this);
        }
        FrameLayout container = findViewById(R.id.page_container);
        container.removeAllViews();
        container.addView(screen.view());
        screen.bind(vm.state.get());
        for (int i = 0; i < navigation.length; i++) {
            TextView item = findViewById(navigation[i]);
            boolean selected = i == tab.ordinal();
            item.setSelected(selected);
            int color = getColor(selected ? R.color.accent : R.color.muted);
            item.setTextColor(color);
            for (android.graphics.drawable.Drawable icon : item.getCompoundDrawables())
                if (icon != null) icon.setTint(color);
        }
    }

    @Override
    public void startTraining(DayPlan day) {
        if (day == null || day.isRest()) {
            Ui.toast(this, "今天是恢复日，轻松活动即可。");
            return;
        }
        AppContainer c = AppContainer.get();
        WeekPlan plan = vm.state.get().plan;
        if (plan == null) {
            show(Tab.PLAN);
            return;
        }
        String key = plan.id + ":" + day.dayIndex;
        if (c.data.contains("session") && !key.equals(c.data.getString("session_day", ""))) {
            new AlertDialog.Builder(this)
                    .setTitle("有一场训练尚未结束")
                    .setMessage("开始新的训练会替换未完成进度。")
                    .setNegativeButton(
                            "保留进度", (d, w) -> openWorkout(c.data.getString("session_day", "")))
                    .setPositiveButton(
                            "开始新的训练",
                            (d, w) -> {
                                c.data.edit().remove("session").remove("session_day").apply();
                                openWorkout(key);
                            })
                    .show();
        } else openWorkout(key);
    }

    private void openWorkout(String key) {
        Intent intent = new Intent(this, WorkoutActivity.class);
        intent.putExtra("dayKey", key);
        startActivity(intent);
    }

    @Override
    public void exportPlan() {
        WeekPlan p = vm.state.get().plan;
        if (p == null) {
            Ui.toast(this, "请先生成训练计划");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("导出训练计划")
                .setItems(
                        new String[] {"易读文本 .txt", "结构化数据 .json"},
                        (dialog, which) -> {
                            exportSnapshot = p;
                            exportJson = which == 1;
                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType(exportJson ? "application/json" : "text/plain");
                            intent.putExtra(
                                    Intent.EXTRA_TITLE,
                                    "PULSE-训练计划-"
                                            + java.time.LocalDate.now()
                                            + (exportJson ? ".json" : ".txt"));
                            startActivityForResult(intent, 41);
                        })
                .show();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 41
                || result != RESULT_OK
                || data == null
                || data.getData() == null
                || exportSnapshot == null) return;
        try {
            PlanExporter.Sink sink = new AndroidDocumentSink(getContentResolver(), data.getData());
            PlanExporter exporter =
                    exportJson ? new PlanExporter.Json(sink) : new PlanExporter.Text(sink);
            exporter.export(exportSnapshot);
            Ui.toast(this, "训练计划已导出");
        } catch (Exception e) {
            Ui.toast(this, "导出失败，请选择可写入的位置");
        } finally {
            exportSnapshot = null;
        }
    }

    /** Bridge implementation: the same sink supports every export format. */
    private static final class AndroidDocumentSink implements PlanExporter.Sink {
        private final ContentResolver resolver;
        private final android.net.Uri uri;

        AndroidDocumentSink(ContentResolver resolver, android.net.Uri uri) {
            this.resolver = resolver;
            this.uri = uri;
        }

        public void write(String text) throws IOException {
            try (OutputStream out = resolver.openOutputStream(uri, "wt")) {
                if (out == null) throw new IOException("无法创建文件");
                out.write(text.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
