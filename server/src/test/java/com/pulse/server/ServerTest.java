package com.pulse.server;

import org.junit.*;
import static org.junit.Assert.*;
import com.google.gson.*;
import com.pulse.core.model.*;
import com.pulse.core.plan.*;
import com.pulse.core.util.JsonCodec;
import java.net.*;
import java.net.http.*;
import java.time.Duration;

public class ServerTest {
    private FitnessServer server;
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private static final String TOKEN = "test-token-with-24-characters";

    @After
    public void stop() {
        if (server != null) server.stop();
    }

    private void start(PlanGenerator generator) throws Exception {
        server = new FitnessServer("127.0.0.1", 0, TOKEN, "test", generator);
        server.start();
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        HttpRequest.Builder b =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        return client.send(
                b.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void authValidationAndLocalModeAreHonest() throws Exception {
        start(null);
        String body = JsonCodec.profile(UserProfile.defaults());
        assertEquals(401, post("/api/v1/plans", body, null).statusCode());
        assertEquals(400, post("/api/v1/plans", "{}", TOKEN).statusCode());
        assertEquals(
                400,
                post(
                                "/api/v1/plans",
                                JsonCodec.profile(
                                        UserProfile.defaults().toBuilder().age(16).build()),
                                TOKEN)
                        .statusCode());
        HttpResponse<String> response = post("/api/v1/plans", body, TOKEN);
        assertEquals(200, response.statusCode());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        WeekPlan p = JsonCodec.plan(json.get("plan").toString());
        assertEquals(WeekPlan.Source.LOCAL, p.source);
        PlanSafety.validate(p, UserProfile.defaults());
        assertEquals(413, post("/api/v1/plans", "x".repeat(32769), TOKEN).statusCode());
        assertEquals(404, post("/api/v1/plans/unknown", body, TOKEN).statusCode());
    }

    @Test
    public void modelFailureFallsBackInsteadOfPretendingAiSucceeded() throws Exception {
        start(
                p -> {
                    throw new java.io.IOException("provider secret must never leak");
                });
        HttpResponse<String> response =
                post("/api/v1/plans", JsonCodec.profile(UserProfile.defaults()), TOKEN);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("LOCAL"));
        assertTrue(response.body().contains("本地规则"));
        assertFalse(response.body().contains("provider secret"));
    }

    @Test
    public void compatibleAdapterAcceptsValidJsonAndRejectsHallucinations() throws Exception {
        UserProfile p = UserProfile.defaults();
        WeekPlan local = new LocalPlanGenerator().generate(p);
        JsonObject payload = JsonParser.parseString(JsonCodec.plan(local)).getAsJsonObject();
        WeekPlan ai =
                CompatibleLlmAdapter.adapt(
                        "<think>hidden</think>\n```json\n" + payload + "\n```", p);
        assertEquals(WeekPlan.Source.AI, ai.source);
        String bad = payload.toString().replace("wall_push", "invented_dangerous_action");
        // Find an actual first exercise rather than depending on goal sort order.
        bad =
                payload.toString()
                        .replace(
                                local.days.get(0).exercises.get(0).exerciseId,
                                "invented_dangerous_action");
        String invalid = bad;
        assertThrows(RuntimeException.class, () -> CompatibleLlmAdapter.adapt(invalid, p));
    }

    @Test
    public void actualHttpAdapterSendsOnlyNeededModelProfile() throws Exception {
        UserProfile p = UserProfile.defaults().toBuilder().name("private-name").sex("女").build();
        WeekPlan local = new LocalPlanGenerator().generate(p);
        com.sun.net.httpserver.HttpServer upstream =
                com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        java.util.concurrent.atomic.AtomicReference<String> request =
                new java.util.concurrent.atomic.AtomicReference<>();
        upstream.createContext(
                "/v1/chat/completions",
                e -> {
                    request.set(
                            new String(
                                    e.getRequestBody().readAllBytes(),
                                    java.nio.charset.StandardCharsets.UTF_8));
                    JsonObject message = new JsonObject();
                    message.addProperty("content", JsonCodec.plan(local));
                    JsonObject choice = new JsonObject();
                    choice.add("message", message);
                    JsonArray choices = new JsonArray();
                    choices.add(choice);
                    JsonObject response = new JsonObject();
                    response.add("choices", choices);
                    FitnessServer.json(e, 200, response.toString());
                });
        upstream.start();
        try {
            start(
                    new CompatibleLlmAdapter(
                            "http://127.0.0.1:" + upstream.getAddress().getPort() + "/v1",
                            "test",
                            ""));
            HttpResponse<String> response = post("/api/v1/plans", JsonCodec.profile(p), TOKEN);
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("\"source\":\"AI\""));
            assertFalse(request.get().contains("private-name"));
            assertFalse(request.get().contains("\\\"sex\\\""));
            assertTrue(request.get().contains("wall_push"));
        } finally {
            upstream.stop(0);
        }
    }
}
