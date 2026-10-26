package com.pulse.fitness.presentation;

import com.pulse.fitness.*;
import com.pulse.fitness.data.RemotePlanGenerator;
import com.pulse.core.data.*;
import com.pulse.core.model.*;
import com.pulse.core.plan.*;
import com.pulse.core.util.*;
import com.google.gson.*;
import java.util.*;
import java.util.concurrent.Future;

/** MVVM: views send intentions and observe immutable UI state; no Activity is retained. */
public final class FitnessViewModel {
    public static final class UiState {
        public final UserProfile profile;
        public final WeekPlan plan;
        public final boolean busy;
        public final String message, connection;

        UiState(
                UserProfile profile,
                WeekPlan plan,
                boolean busy,
                String message,
                String connection) {
            this.profile = profile;
            this.plan = plan;
            this.busy = busy;
            this.message = message;
            this.connection = connection;
        }
    }

    private final AppContainer container;
    public final ObservableState<UiState> state;
    private boolean busy;
    private String message = "", connection = "";
    private long revision;
    private Future<?> generation;
    private ProfileCommand lastSave;

    public FitnessViewModel(AppContainer c) {
        container = c;
        state = new ObservableState<>(read());
    }

    private UiState read() {
        UserProfile p = container.repositories.profiles().get();
        WeekPlan plan = container.training.current();
        if (plan != null) {
            try {
                if (p == null) throw new IllegalArgumentException();
                PlanSafety.validate(plan, p);
            } catch (RuntimeException invalid) {
                container.training.invalidate();
                plan = null;
            }
        }
        return new UiState(p, plan, busy, message, connection);
    }

    public void refresh() {
        state.set(read());
    }

    public List<TrainingRecord> history() {
        return container.repositories.history().all();
    }

    public void saveProfile(UserProfile p) {
        ProfileCommand command = new ProfileCommand(container.repositories.profiles(), p);
        command.execute();
        lastSave = command;
        cancel();
        container.training.invalidate();
        message = "档案已保存。请根据新档案生成计划。";
        refresh();
    }

    public boolean canUndo() {
        return lastSave != null && lastSave.canUndo();
    }

    public void undo() {
        if (canUndo()) {
            lastSave.undo();
            cancel();
            container.training.invalidate();
            message = "已恢复上一次档案，请重新生成计划。";
            refresh();
        }
    }

    private void cancel() {
        revision++;
        busy = false;
        if (generation != null) generation.cancel(true);
    }

    public void generate(boolean remote) {
        if (busy) return;
        UserProfile p = container.repositories.profiles().get();
        try {
            com.pulse.core.validation.ProfileValidator.validateForTraining(p);
        } catch (IllegalArgumentException e) {
            message = e.getMessage();
            refresh();
            return;
        }
        if (remote && container.endpoint().isEmpty()) {
            message = "请先在“我的”中设置 AI 服务地址";
            refresh();
            return;
        }
        busy = true;
        message = remote ? "正在生成专属计划，模型推理可能需要 1–3 分钟…" : "正在匹配本地训练计划…";
        long request = ++revision;
        PlanGenerator generator =
                remote
                        ? new RemotePlanGenerator.Factory(
                                        container.endpoint(), container.tokens.get())
                                .createSafeGenerator()
                        : new PlanGeneratorFactory.Local().createSafeGenerator();
        refresh();
        generation =
                container.worker.submit(
                        () -> {
                            try {
                                WeekPlan result = container.training.generate(generator);
                                container.main.post(
                                        () -> {
                                            if (request != revision) return;
                                            try {
                                                container.training.commit(result);
                                                message =
                                                        result.source == WeekPlan.Source.AI
                                                                ? "AI 计划已生成，动作与训练量已通过校验。"
                                                                : remote
                                                                        ? "模型尚未就绪或输出未通过校验，当前为本地规则计划。"
                                                                        : "本地规则计划已生成，可以离线训练。";
                                            } catch (RuntimeException e) {
                                                message = e.getMessage();
                                            }
                                            busy = false;
                                            refresh();
                                        });
                            } catch (Exception e) {
                                container.main.post(
                                        () -> {
                                            if (request != revision) return;
                                            busy = false;
                                            message = "生成失败：" + friendly(e) + "。可检查服务连接，或选择本地计划。";
                                            refresh();
                                        });
                            }
                        });
    }

    private static String friendly(Exception e) {
        if (e instanceof java.net.ConnectException) return "无法连接服务";
        if (e instanceof java.net.SocketTimeoutException) return "服务连接超时";
        String s = e.getMessage();
        return s == null ? "服务暂时不可用" : s.substring(0, Math.min(160, s.length()));
    }

    public void copyPlan() {
        WeekPlan p = state.get().plan;
        if (p == null || busy) return;
        container.training.commit(p.copyForNextWeek());
        container.data.edit().remove("session").remove("session_day").apply();
        message = "已创建新的训练周期，历史记录已保留。";
        refresh();
    }

    public void testConnection(String endpoint, String token) {
        try {
            container.saveConnection(endpoint, token);
        } catch (Exception e) {
            connection = "保存失败：" + friendly(e);
            refresh();
            return;
        }
        final long requestedRevision = revision;
        connection = "连接已保存，正在检测服务…";
        refresh();
        container.worker.submit(
                () -> {
                    String result;
                    try {
                        JsonObject o =
                                JsonParser.parseString(
                                                RemotePlanGenerator.health(
                                                        container.endpoint(),
                                                        container.tokens.get()))
                                        .getAsJsonObject();
                        if (!"ok".equals(JsonCodec.str(o, "status")))
                            throw new IllegalArgumentException("服务状态异常");
                        result =
                                o.get("modelConfigured").getAsBoolean()
                                        ? "服务在线 · 模型："
                                                + o.get("model").getAsString()
                                                + "。可前往计划页生成，实际推理结果以计划来源为准。"
                                        : "服务在线，尚未配置模型；目前可生成本地规则计划。";
                    } catch (Exception e) {
                        result = "检测失败：" + friendly(e) + "。请确认服务已启动和地址正确。";
                    }
                    String value = result;
                    container.main.post(
                            () -> {
                                if (requestedRevision != revision) return;
                                connection = value;
                                refresh();
                            });
                });
    }

    public void reset() {
        cancel();
        container.data.edit().clear().apply();
        container.settings.edit().clear().apply();
        container.sessions.clear();
        container.phoneAuth.clear();
        lastSave = null;
        message = "本机个人数据已清除。";
        connection = "";
        refresh();
    }
}
