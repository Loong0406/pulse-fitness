package com.pulse.server;

import com.google.gson.*;
import com.pulse.core.model.*;
import com.pulse.core.plan.*;
import com.pulse.core.util.JsonCodec;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Adapter: provider chat-completion messages become the domain PlanGenerator contract. */
public final class CompatibleLlmAdapter extends PlanGenerationTemplate {
    private final URI endpoint;
    private final String model, key;
    private final HttpClient client;
    private final String responseFormat;
    private final Boolean enableThinking;

    public CompatibleLlmAdapter(String baseUrl, String model, String key) {
        this(baseUrl, model, key, "json_schema", null);
    }

    public CompatibleLlmAdapter(
            String baseUrl,
            String model,
            String key,
            String responseFormat,
            Boolean enableThinking) {
        endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/chat/completions");
        if (!Set.of("http", "https").contains(endpoint.getScheme()) || endpoint.getHost() == null)
            throw new IllegalArgumentException("AI_BASE_URL 必须是 http(s) 地址");
        this.model = model;
        this.key = key;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        if (!Set.of("json_schema", "json_object").contains(responseFormat))
            throw new IllegalArgumentException("AI_RESPONSE_FORMAT 只能为 json_schema 或 json_object");
        this.responseFormat = responseFormat;
        this.enableThinking = enableThinking;
    }

    @Override
    protected WeekPlan createCandidate(UserProfile p) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("stream", false);
        body.addProperty("temperature", 0.35);
        body.addProperty("max_tokens", 4500);
        JsonObject format = new JsonObject();
        format.addProperty("type", responseFormat);
        if (responseFormat.equals("json_schema")) {
            JsonObject definition = new JsonObject();
            definition.addProperty("name", "fitness_plan");
            definition.addProperty("strict", true);
            definition.add("schema", outputSchema(p));
            format.add("json_schema", definition);
        }
        body.add("response_format", format);
        if (enableThinking != null) body.addProperty("enable_thinking", enableThinking);
        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", systemPrompt(p));
        messages.add(system);
        // No name or sex is sent: neither is needed to choose safe movements.
        JsonObject trainingProfile = JsonParser.parseString(JsonCodec.profile(p)).getAsJsonObject();
        trainingProfile.remove("name");
        trainingProfile.remove("sex");
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty(
                "content",
                "为此档案生成七天训练计划，只返回 JSON："
                        + trainingProfile
                        + (enableThinking == null ? " /no_think" : ""));
        messages.add(user);
        body.add("messages", messages);
        HttpRequest.Builder request =
                HttpRequest.newBuilder(endpoint)
                        .timeout(Duration.ofSeconds(150))
                        .header("Content-Type", "application/json");
        if (key != null && !key.isBlank()) request.header("Authorization", "Bearer " + key);
        java.util.concurrent.CompletableFuture<HttpResponse<byte[]>> pending =
                client.sendAsync(
                        request.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),
                        new LimitedBodyHandler(262144));
        HttpResponse<byte[]> response;
        try {
            response = pending.get(155, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception failure) {
            pending.cancel(true);
            throw failure;
        }
        byte[] bytes = response.body();
        if (response.statusCode() / 100 != 2) throw providerFailure(response.statusCode());
        JsonObject choice =
                JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject()
                        .getAsJsonArray("choices")
                        .get(0)
                        .getAsJsonObject();
        if (choice.has("finish_reason")
                && !choice.get("finish_reason").isJsonNull()
                && !"stop".equals(choice.get("finish_reason").getAsString()))
            throw new IllegalStateException("模型输出未完整结束，请检查生成长度或服务限制");
        String content = choice.getAsJsonObject("message").get("content").getAsString();
        return adapt(content, p);
    }

    static IllegalStateException providerFailure(int status) {
        String message;
        switch (status) {
            case 401:
                message = "AI API Key 无效或已失效";
                break;
            case 403:
                message = "AI 账号没有调用权限，请核对模型和账号状态";
                break;
            case 404:
                message = "AI 模型 ID 或服务地址不存在";
                break;
            case 429:
                message = "AI 服务限流或额度不足，请查看服务商控制台";
                break;
            case 400:
            case 422:
                message = "AI 服务拒绝请求参数，请核对模型与 JSON 输出模式";
                break;
            default:
                message = "AI 服务暂时不可用";
        }
        return new IllegalStateException(message + "（HTTP " + status + "）");
    }

    public static WeekPlan adapt(String content, UserProfile p) {
        String json = content.trim();
        int endThink = json.lastIndexOf("</think>");
        if (endThink >= 0) json = json.substring(endThink + 8).trim();
        if (json.startsWith("```")) {
            int newline = json.indexOf('\n');
            int end = json.lastIndexOf("```");
            if (newline >= 0 && end > newline) json = json.substring(newline + 1, end).trim();
        }
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String note = JsonCodec.str(o, "note");
        WeekPlan plan =
                new WeekPlan(
                        UUID.randomUUID().toString(),
                        System.currentTimeMillis(),
                        p.signature(),
                        WeekPlan.Source.AI,
                        note,
                        JsonCodec.days(o.getAsJsonArray("days")));
        PlanSafety.validate(plan, p);
        return plan;
    }

    private static String systemPrompt(UserProfile p) {
        JsonArray catalog = new JsonArray();
        for (Exercise e : ExerciseCatalog.allowed(p)) {
            JsonObject x = new JsonObject();
            x.addProperty("id", e.id);
            x.addProperty("name", e.name);
            x.addProperty("group", e.group);
            x.addProperty("timed", e.timed);
            catalog.add(x);
        }
        int maxSets = schemaMaxSets(p);
        return "你是一般健身计划助手，不提供医疗、康复或饮食处方。档案已通过年龄与健康门槛。只使用下列允许动作，不添加自由文本动作。根据目标、经验、器械和限制选择不同动作与负荷。训练日交替侧重不同部位，避免连续高负荷。"
                + "严格返回 JSON 对象：{\"note\":\"中文简短个性化理由和强度建议\",\"days\":[{\"dayIndex\":0,\"title\":\"中文标题\",\"exercises\":[{\"exerciseId\":\"wall_push\",\"sets\":2,\"amount\":10,\"restSeconds\":45}]}]}。"
                + "days 必须恰好7项，dayIndex按0到6排序。训练日索引为"
                + scheduled(p)
                + "，其余为休息日exercises=[]。每训练日2到"
                + schemaMaxExercises(p)
                + "个不同动作；每动作1到"
                + maxSets
                + "组；计次动作6到15次，计时动作15到45秒；休息固定45秒；每天不超过18组。note只简短解释目标与动作选择，不重复负荷数字，不提供医疗或饮食建议。"
                + "时间约束严格满足：480 + sum(sets*(timed ? amount : amount*3)+sets*restSeconds) <= "
                + (p.minutesPerSession * 60)
                + "秒，480秒是热身和放松。较短的训练优先2动作×2组、10次、休息45秒。"
                + "不要解释思考过程，不要Markdown。允许动作："
                + catalog;
    }

    private static int schemaMaxSets(UserProfile p) {
        return p.experience == UserProfile.Experience.BEGINNER
                        || p.age >= 60
                        || p.daysPerWeek >= 4
                        || p.minutesPerSession < 20
                ? 2
                : 4;
    }

    private static int schemaMaxExercises(UserProfile p) {
        return Math.min(
                5,
                Math.min(
                        18 / schemaMaxSets(p),
                        (p.minutesPerSession * 60 - 480) / (schemaMaxSets(p) * 90)));
    }

    private static Set<Integer> scheduled(UserProfile p) {
        Set<Integer> result = new LinkedHashSet<>();
        for (int i = 0; i < p.daysPerWeek; i++) result.add(i * 7 / p.daysPerWeek);
        return result;
    }

    /** JSON Schema constrains structure at decoding time, including exact training/rest dates.
     * Per-set time is <= 45 seconds + 45 rest, so the item/sets bounds guarantee the time budget. */
    static JsonObject outputSchema(UserProfile p) {
        JsonObject schema = object();
        JsonObject properties = schema.getAsJsonObject("properties");
        JsonObject note = type("string");
        note.addProperty("maxLength", 600);
        properties.add("note", note);
        JsonObject days = type("array");
        days.addProperty("minItems", 7);
        days.addProperty("maxItems", 7);
        JsonArray prefix = new JsonArray();
        Set<Integer> training = scheduled(p);
        for (int day = 0; day < 7; day++) {
            JsonObject d = object(), dp = d.getAsJsonObject("properties");
            JsonObject index = type("integer");
            index.addProperty("const", day);
            dp.add("dayIndex", index);
            JsonObject title = type("string");
            title.addProperty("maxLength", 48);
            dp.add("title", title);
            JsonObject exercises = type("array");
            exercises.addProperty("minItems", training.contains(day) ? 2 : 0);
            exercises.addProperty("maxItems", training.contains(day) ? schemaMaxExercises(p) : 0);
            JsonObject rx = object(), rp = rx.getAsJsonObject("properties");
            JsonObject id = type("string");
            JsonArray ids = new JsonArray();
            for (Exercise e : ExerciseCatalog.allowed(p)) ids.add(e.id);
            id.add("enum", ids);
            rp.add("exerciseId", id);
            rp.add("sets", integer(1, schemaMaxSets(p)));
            rp.add("amount", integer(6, 45));
            JsonObject rest = type("integer");
            rest.addProperty("const", 45);
            rp.add("restSeconds", rest);
            required(rx, "exerciseId", "sets", "amount", "restSeconds");
            // Conditional amount ranges retain compatibility with timed and repetition-based
            // catalog actions.
            JsonArray alternatives = new JsonArray();
            for (boolean timed : new boolean[] {false, true}) {
                JsonArray subset = new JsonArray();
                for (Exercise e : ExerciseCatalog.allowed(p))
                    if (e.timed == timed) subset.add(e.id);
                if (subset.isEmpty()) continue;
                JsonObject branch = object(), bp = branch.getAsJsonObject("properties");
                JsonObject bid = type("string");
                bid.add("enum", subset);
                bp.add("exerciseId", bid);
                bp.add("sets", integer(1, schemaMaxSets(p)));
                bp.add("amount", integer(timed ? 15 : 6, timed ? 45 : 15));
                bp.add("restSeconds", rest);
                required(branch, "exerciseId", "sets", "amount", "restSeconds");
                alternatives.add(branch);
            }
            JsonObject choices = new JsonObject();
            choices.add("anyOf", alternatives);
            exercises.add("items", choices);
            dp.add("exercises", exercises);
            required(d, "dayIndex", "title", "exercises");
            prefix.add(d);
        }
        days.add("prefixItems", prefix);
        days.add("items", new JsonObject());
        properties.add("days", days);
        required(schema, "note", "days");
        return schema;
    }

    private static JsonObject type(String value) {
        JsonObject o = new JsonObject();
        o.addProperty("type", value);
        return o;
    }

    private static JsonObject integer(int min, int max) {
        JsonObject o = type("integer");
        o.addProperty("minimum", min);
        o.addProperty("maximum", max);
        return o;
    }

    private static JsonObject object() {
        JsonObject o = type("object");
        o.add("properties", new JsonObject());
        o.addProperty("additionalProperties", false);
        return o;
    }

    private static void required(JsonObject o, String... fields) {
        JsonArray array = new JsonArray();
        for (String f : fields) array.add(f);
        o.add("required", array);
    }
}
