package com.pulse.fitness.ui;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.pulse.fitness.*;
import com.pulse.core.model.*;
import com.pulse.core.session.WorkoutSession;
import com.pulse.core.util.JsonCodec;

/** Session clock and rendering are Android concerns; transitions live in the pure-Java State object. */
public final class WorkoutActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WorkoutSession session;
    private String dayKey;
    private long lastTick;
    private boolean completionShown;
    private final Runnable ticker =
            new Runnable() {
                public void run() {
                    if (session == null) return;
                    long now = SystemClock.elapsedRealtime();
                    int seconds = (int) ((now - lastTick) / 1000);
                    if (seconds > 0) {
                        session.tick(seconds);
                        lastTick += seconds * 1000L;
                        render();
                        if (session.seconds() % 5 == 0) persist();
                    }
                    handler.postDelayed(this, 250);
                }
            };

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_workout);
        Ui.insets(this, findViewById(R.id.workout_root), Ui.dp(this, 24));
        dayKey = getIntent().getStringExtra("dayKey");
        AppContainer c = AppContainer.get();
        WeekPlan plan = c.viewModel.state.get().plan;
        try {
            if (plan == null || dayKey == null || !dayKey.startsWith(plan.id + ":"))
                throw new IllegalArgumentException();
            int index = Integer.parseInt(dayKey.substring(dayKey.lastIndexOf(':') + 1));
            DayPlan day = plan.days.get(index);
            String snapshot =
                    saved != null
                            ? saved.getString("snapshot")
                            : dayKey.equals(c.data.getString("session_day", ""))
                                    ? c.data.getString("session", null)
                                    : null;
            if (snapshot == null) session = new WorkoutSession(day);
            else {
                try {
                    session =
                            new WorkoutSession(
                                    day,
                                    JsonCodec.GSON.fromJson(
                                            snapshot, WorkoutSession.Snapshot.class));
                } catch (RuntimeException corrupt) {
                    session = new WorkoutSession(day);
                    Ui.toast(this, "旧进度无法恢复，已重新开始本次训练");
                }
            }
        } catch (RuntimeException invalid) {
            Ui.toast(this, "训练计划已更新，请重新选择训练日");
            finish();
            return;
        }
        findViewById(R.id.workout_close).setOnClickListener(v -> finish());
        findViewById(R.id.workout_primary)
                .setOnClickListener(
                        v -> {
                            session.primary();
                            persist();
                            render();
                            if (session.complete()) complete();
                        });
        findViewById(R.id.workout_pause)
                .setOnClickListener(
                        v -> {
                            if (session.phase().equals("PAUSED")) finish();
                            else {
                                session.pause();
                                persist();
                                render();
                            }
                        });
        if (Build.VERSION.SDK_INT >= 33)
            getOnBackInvokedDispatcher()
                    .registerOnBackInvokedCallback(
                            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::finish);
        render();
        if (session.complete()) complete();
    }

    @Override
    protected void onStart() {
        super.onStart();
        lastTick = SystemClock.elapsedRealtime();
        handler.post(ticker);
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(ticker);
        if (session != null && !session.complete()) {
            session.pause();
            persist();
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (session != null) out.putString("snapshot", JsonCodec.GSON.toJson(session.snapshot()));
    }

    private void persist() {
        if (session == null || session.complete()) return;
        AppContainer.get()
                .data
                .edit()
                .putString("session_day", dayKey)
                .putString("session", JsonCodec.GSON.toJson(session.snapshot()))
                .apply();
    }

    private void render() {
        if (session == null) return;
        Prescription rx = session.current();
        String phase = session.phase();
        Ui.text(findViewById(R.id.workout_root), R.id.workout_heading, session.day().title);
        Ui.text(findViewById(R.id.workout_root), R.id.workout_elapsed, Ui.clock(session.seconds()));
        String title, target, cue, label;
        switch (phase) {
            case "WARMUP":
                title = "先唤醒身体。";
                target = Ui.clock(session.remaining());
                cue = "建议用约 4 分钟轻松踏步、活动肩踝，在舒适范围内逐渐进入状态。准备好后手动开始训练。";
                label = "01 / WARM UP";
                break;
            case "REST":
                title = "休息，也有意义。";
                target = Ui.clock(session.remaining());
                cue = "调整呼吸。接下来：" + session.exercise().name + "。\n" + session.exercise().cue;
                label = "RECOVER / NEXT SET";
                break;
            case "COOLDOWN":
                title = "把节奏，慢下来。";
                target = Ui.clock(session.remaining());
                cue = "建议用约 4 分钟缓慢走动、自然呼吸，并在舒适范围内轻柔放松。完成后保存训练记录。";
                label = "03 / COOL DOWN";
                break;
            case "PAUSED":
                title = "休息一下，随时回来。";
                target = "已暂停";
                cue = "进度已保存在本机。点击“继续训练”会从暂停的位置恢复。";
                label = "YOUR PACE / PAUSED";
                break;
            case "COMPLETE":
                title = "这一次，为自己。";
                target = "完成 ✓";
                cue = "你又为自己积累了一点进步。";
                label = "SESSION COMPLETE";
                break;
            default:
                title = session.exercise().name;
                target = rx.amount + (session.exercise().timed ? " 秒" : " 次");
                cue = session.exercise().cue + "\n\n" + String.join("\n", session.exercise().steps);
                label = "02 / MOVE WITH INTENTION";
        }
        View root = findViewById(R.id.workout_root);
        Ui.text(root, R.id.workout_phase, label);
        Ui.text(root, R.id.workout_name, title);
        Ui.text(root, R.id.workout_target, target);
        Ui.text(root, R.id.workout_cue, cue);
        Ui.text(
                root,
                R.id.workout_set,
                phase.equals("RUNNING")
                        ? "动作 "
                                + (session.exerciseIndex() + 1)
                                + " / "
                                + session.day().exercises.size()
                                + "  ·  第 "
                                + (session.setIndex() + 1)
                                + " / "
                                + rx.sets
                                + " 组"
                        : "按自己的节奏，完成每一步");
        Ui.text(root, R.id.workout_primary, session.action());
        Ui.text(root, R.id.workout_pause, phase.equals("PAUSED") ? "返回并保留进度" : "暂停训练");
        findViewById(R.id.workout_primary).setEnabled(!session.complete());
        findViewById(R.id.workout_pause).setEnabled(!session.complete());
        int total = 0;
        for (Prescription p : session.day().exercises) total += p.sets;
        ProgressBar progress = findViewById(R.id.workout_progress);
        progress.setMax(total);
        progress.setProgress(session.snapshot().completedSets);
        if (phase.equals("PAUSED") || session.complete())
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void complete() {
        if (completionShown) return;
        completionShown = true;
        handler.removeCallbacks(ticker);
        AppContainer c = AppContainer.get();
        c.repositories.history().add(session.record(System.currentTimeMillis()));
        c.viewModel.refresh();
        new AlertDialog.Builder(this)
                .setTitle("训练完成，做得很好。")
                .setMessage(
                        "本次投入 "
                                + Ui.clock(session.seconds())
                                + "，完成 "
                                + session.snapshot().completedSets
                                + " 组动作。\n记录已保存在本机。")
                .setCancelable(false)
                .setPositiveButton("完成", (d, w) -> finish())
                .show();
    }
}
