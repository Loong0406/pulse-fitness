package com.pulse.server;

import com.sun.net.httpserver.*;
import com.google.gson.*;
import com.pulse.core.model.*;
import com.pulse.core.plan.*;
import com.pulse.core.util.JsonCodec;
import com.pulse.core.validation.ProfileValidator;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Small Java 17 HTTP service. Model keys never ship in the Android APK. */
public final class FitnessServer {
    private final HttpServer server;
    private final ExecutorService executor;
    private final Semaphore modelSlots = new Semaphore(2);
    private final PlanGenerator generator;
    private final String model;

    public FitnessServer(String host, int port, String token, String model, PlanGenerator generator)
            throws IOException {
        this(host, port, token, model, generator, null);
    }

    public FitnessServer(
            String host,
            int port,
            String token,
            String model,
            PlanGenerator generator,
            DemoAccountService accounts)
            throws IOException {
        if (!InetAddress.getByName(host).isLoopbackAddress() && token.length() < 24)
            throw new IllegalArgumentException("对外监听时 APP_TOKEN 至少 24 字符");
        if (accounts != null && token.length() < 24)
            throw new IllegalArgumentException("数据库演示登录需要至少 24 字符的 APP_TOKEN");
        this.model = model;
        this.generator = generator;
        server = HttpServer.create(new InetSocketAddress(host, port), 16);
        executor =
                new ThreadPoolExecutor(
                        4,
                        4,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(16),
                        new ThreadPoolExecutor.AbortPolicy());
        server.setExecutor(executor);
        server.createContext(
                "/health",
                e -> {
                    if (!e.getRequestURI().getPath().equals("/health")) {
                        json(e, 404, "{\"error\":\"接口不存在\"}");
                        return;
                    }
                    if (!e.getRequestMethod().equals("GET")) {
                        json(e, 405, "{\"error\":\"仅支持 GET\"}");
                        return;
                    }
                    JsonObject o = new JsonObject();
                    o.addProperty("status", "ok");
                    o.addProperty("model", model);
                    o.addProperty("modelConfigured", generator != null);
                    o.addProperty("databaseLoginEnabled", accounts != null);
                    json(e, 200, o.toString());
                });
        server.createContext("/api/v1/plans", new AuthorizedHandlerProxy(token, this::handlePlan));
        server.createContext(
                "/api/v1/auth/",
                new AuthorizedHandlerProxy(token, new AccountHttpHandler(accounts)));
    }

    private void handlePlan(HttpExchange e) throws IOException {
        try {
            if (!e.getRequestURI().getPath().equals("/api/v1/plans")) {
                json(e, 404, "{\"error\":\"接口不存在\"}");
                return;
            }
            if (!e.getRequestMethod().equals("POST")) {
                e.getResponseHeaders().set("Allow", "POST");
                json(e, 405, "{\"error\":\"仅支持 POST\"}");
                return;
            }
            String type = e.getRequestHeaders().getFirst("Content-Type");
            if (type == null
                    || !type.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
                json(e, 415, "{\"error\":\"请求需为 JSON\"}");
                return;
            }
            byte[] bytes;
            try (InputStream input = e.getRequestBody()) {
                bytes = input.readNBytes(32769);
            }
            if (bytes.length > 32768) {
                json(e, 413, "{\"error\":\"请求过大\"}");
                return;
            }
            UserProfile p;
            try {
                p = JsonCodec.profile(new String(bytes, StandardCharsets.UTF_8));
                ProfileValidator.validateForTraining(p);
            } catch (RuntimeException invalid) {
                JsonObject o = new JsonObject();
                o.addProperty(
                        "error",
                        invalid instanceof IllegalArgumentException
                                ? "档案不完整或不适合自动训练，请检查年龄、时间和健康限制"
                                : "档案格式无效");
                json(e, 400, o.toString());
                return;
            }
            if (!modelSlots.tryAcquire()) {
                json(e, 429, "{\"error\":\"计划生成繁忙，请稍后重试\"}");
                return;
            }
            try {
                WeekPlan plan;
                String message;
                if (generator == null) {
                    plan = new LocalPlanGenerator().generate(p);
                    message = "服务尚未配置模型，已生成本地规则计划";
                } else {
                    try {
                        plan = new SafePlanGeneratorDecorator(generator).generate(p);
                        message = "大模型已生成并通过规则校验";
                    } catch (Exception unavailable) {
                        plan = new LocalPlanGenerator().generate(p);
                        message = "模型连接失败或输出未通过校验，已改用本地规则计划";
                    }
                }
                JsonObject payload = new JsonObject();
                payload.add("plan", JsonParser.parseString(JsonCodec.plan(plan)));
                payload.addProperty("message", message);
                json(e, 200, payload.toString());
            } finally {
                modelSlots.release();
            }
        } catch (Exception failure) {
            if (e.getResponseCode() == -1) json(e, 500, "{\"error\":\"服务暂时不可用\"}");
        } finally {
            e.close();
        }
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void stop() {
        server.stop(0);
        executor.shutdownNow();
    }

    static void json(HttpExchange e, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        e.getResponseHeaders().set("Cache-Control", "no-store");
        e.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        e.sendResponseHeaders(code, bytes.length);
        try (OutputStream out = e.getResponseBody()) {
            out.write(bytes);
        }
        e.close();
    }

    private static String env(String name, String fallback) {
        String s = System.getenv(name);
        return s == null || s.isBlank() ? fallback : s;
    }

    public static void main(String[] args) throws Exception {
        String host = env("HOST", "127.0.0.1");
        AiSettings ai = new AiSettings();
        PlanGenerator generator = ai.generator();
        String model = ai.model();
        DatabaseSettings database = new DatabaseSettings();
        JdbcAccountRepository repository = database.repository();
        if (repository != null) {
            repository.checkConnection();
            repository.checkSchema();
        }
        DemoAccountService accounts =
                repository != null
                                && Boolean.parseBoolean(database.get("AUTH_DEMO_ENABLED", "false"))
                        ? new DemoAccountService(repository)
                        : null;
        FitnessServer service =
                new FitnessServer(
                        host,
                        Integer.parseInt(env("PORT", "8080")),
                        env("APP_TOKEN", ""),
                        model,
                        generator,
                        accounts);
        Runtime.getRuntime().addShutdownHook(new Thread(service::stop));
        service.start();
        System.out.println(
                "PULSE service: http://"
                        + host
                        + ":"
                        + service.port()
                        + " | model configured="
                        + (generator != null));
    }
}
