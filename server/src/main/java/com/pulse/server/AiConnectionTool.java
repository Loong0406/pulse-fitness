package com.pulse.server;

import com.google.gson.*;
import com.pulse.core.model.*;
import com.pulse.core.plan.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/** Explicit live check: model access plus one synthetic plan. Never treats local fallback as an AI success. */
public final class AiConnectionTool {
    public static void main(String[] args) {
        try {
            AiSettings settings = new AiSettings();
            CompatibleLlmAdapter generator = settings.generator();
            if (generator == null)
                throw new IllegalArgumentException("请填写 AI_BASE_URL、AI_MODEL 和 AI_API_KEY");
            HttpRequest.Builder request =
                    HttpRequest.newBuilder(URI.create(settings.base() + "/models?sub_type=chat"))
                            .timeout(Duration.ofSeconds(20))
                            .GET();
            if (!settings.key().isBlank())
                request.header("Authorization", "Bearer " + settings.key());
            HttpResponse<byte[]> models =
                    HttpClient.newBuilder()
                            .connectTimeout(Duration.ofSeconds(8))
                            .build()
                            .send(request.build(), new LimitedBodyHandler(1048576));
            if (models.statusCode() / 100 != 2)
                throw CompatibleLlmAdapter.providerFailure(models.statusCode());
            JsonArray entries =
                    JsonParser.parseString(
                                    new String(
                                            models.body(), java.nio.charset.StandardCharsets.UTF_8))
                            .getAsJsonObject()
                            .getAsJsonArray("data");
            boolean found = false;
            for (JsonElement entry : entries)
                if (settings.model().equals(entry.getAsJsonObject().get("id").getAsString())) {
                    found = true;
                    break;
                }
            if (!found) throw new IllegalArgumentException("所选模型 ID 不在当前 Key 可见列表中，请检查模型名称与账号权限");
            WeekPlan plan = generator.generate(UserProfile.defaults());
            if (plan.source != WeekPlan.Source.AI) throw new IllegalStateException("返回的不是 AI 计划");
            PlanSafety.validate(plan, UserProfile.defaults());
            System.out.println(
                    "AI authentication, selected model access and validated seven-day AI plan: OK");
        } catch (Exception failure) {
            // Only controlled configuration/status messages are printed, never provider bodies or
            // secret keys.
            if (failure instanceof IllegalArgumentException
                    || failure instanceof IllegalStateException) {
                String message = failure.getMessage();
                if (message != null
                        && (message.startsWith("请")
                                || message.startsWith("AI ")
                                || message.startsWith("所选")
                                || message.startsWith("模型输出"))) System.err.println(message);
                else
                    System.err.println(
                            "AI response did not pass JSON/plan validation. Check model compatibility.");
            } else
                System.err.println(
                        "AI connection failed or timed out. Check network and provider availability.");
            System.exit(1);
        }
    }
}
