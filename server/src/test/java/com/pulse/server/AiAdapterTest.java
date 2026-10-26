package com.pulse.server;

import com.google.gson.*;
import com.pulse.core.model.*;
import com.pulse.core.plan.*;
import com.pulse.core.util.JsonCodec;
import com.sun.net.httpserver.HttpServer;
import org.junit.*;
import static org.junit.Assert.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;

/** Provider requests are checked against local fake HTTP; no external API key or billable call is used. */
public final class AiAdapterTest {
    private HttpServer upstream;
    private final AtomicReference<JsonObject> captured = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicInteger requests = new AtomicInteger();

    @After
    public void stop() {
        if (upstream != null) upstream.stop(0);
    }

    private CompatibleLlmAdapter start(String content, String finish, int status) throws Exception {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext(
                "/v1/chat/completions",
                e -> {
                    requests.incrementAndGet();
                    authorization.set(e.getRequestHeaders().getFirst("Authorization"));
                    captured.set(
                            JsonParser.parseString(
                                            new String(
                                                    e.getRequestBody().readAllBytes(),
                                                    StandardCharsets.UTF_8))
                                    .getAsJsonObject());
                    if (status != 200) {
                        FitnessServer.json(
                                e, status, "{\"error\":\"private-provider-error-test-secret\"}");
                        return;
                    }
                    JsonObject message = new JsonObject();
                    message.addProperty("content", content);
                    message.addProperty("reasoning_content", "not-the-plan");
                    JsonObject choice = new JsonObject();
                    choice.add("message", message);
                    choice.addProperty("finish_reason", finish);
                    JsonArray choices = new JsonArray();
                    choices.add(choice);
                    JsonObject result = new JsonObject();
                    result.add("choices", choices);
                    FitnessServer.json(e, 200, result.toString());
                });
        upstream.start();
        return new CompatibleLlmAdapter(
                "http://127.0.0.1:" + upstream.getAddress().getPort() + "/v1",
                "deepseek-ai/DeepSeek-V4.1-Flash",
                "test-key",
                "json_object",
                false);
    }

    @Test
    public void siliconflowJsonModeUsesExactModelAndStillChecksPlan() throws Exception {
        UserProfile profile =
                UserProfile.defaults().toBuilder().name("private-profile-name").sex("女").build();
        WeekPlan result =
                start(JsonCodec.plan(new LocalPlanGenerator().generate(profile)), "stop", 200)
                        .generate(profile);
        assertEquals(WeekPlan.Source.AI, result.source);
        PlanSafety.validate(result, profile);
        JsonObject body = captured.get();
        assertEquals("deepseek-ai/DeepSeek-V4.1-Flash", body.get("model").getAsString());
        assertEquals(
                "json_object", body.getAsJsonObject("response_format").get("type").getAsString());
        assertFalse(body.getAsJsonObject("response_format").has("json_schema"));
        assertFalse(body.get("enable_thinking").getAsBoolean());
        assertFalse(body.get("stream").getAsBoolean());
        assertEquals("Bearer test-key", authorization.get());
        assertFalse(body.toString().contains("private-profile-name"));
        assertFalse(body.toString().contains("\\\"sex\\\""));
        assertFalse(body.toString().contains("/no_think"));
        assertEquals(1, requests.get());
    }

    @Test
    public void jsonModeCannotBypassExerciseWhitelist() throws Exception {
        UserProfile profile = UserProfile.defaults();
        WeekPlan plan = new LocalPlanGenerator().generate(profile);
        String invalid =
                JsonCodec.plan(plan)
                        .replace(plan.days.get(0).exercises.get(0).exerciseId, "invented_action");
        CompatibleLlmAdapter adapter = start(invalid, "stop", 200);
        assertThrows(RuntimeException.class, () -> adapter.generate(profile));
        assertEquals(1, requests.get());
    }

    @Test
    public void truncatedResponseIsRejectedEvenWhenPartialContentLooksLikeValidJson()
            throws Exception {
        UserProfile profile = UserProfile.defaults();
        CompatibleLlmAdapter adapter =
                start(JsonCodec.plan(new LocalPlanGenerator().generate(profile)), "length", 200);
        assertThrows(IllegalStateException.class, () -> adapter.generate(profile));
    }

    @Test
    public void providerFailuresReturnControlledStatusAndDoNotRetryOrLeakBody() throws Exception {
        CompatibleLlmAdapter adapter = start("", "stop", 401);
        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () -> adapter.generate(UserProfile.defaults()));
        assertTrue(error.getMessage().contains("401"));
        assertFalse(error.getMessage().contains("private-provider"));
        assertFalse(error.getMessage().contains("test-secret"));
        assertEquals(1, requests.get());
    }
}
