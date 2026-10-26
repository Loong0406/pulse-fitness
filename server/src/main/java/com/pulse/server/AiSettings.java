package com.pulse.server;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Optional backend-only AI configuration. Environment values override the local ignored file. */
final class AiSettings {
    private final Properties values = new Properties();
    private final Map<String, String> environment;

    AiSettings() throws IOException {
        this(configPath(System.getenv("AI_CONFIG_FILE")), System.getenv());
    }

    AiSettings(Path file, Map<String, String> environment) throws IOException {
        this.environment = environment;
        if (file != null)
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                values.load(reader);
            }
    }

    private static Path configPath(String file) {
        return file == null || file.isBlank() ? null : Path.of(file);
    }

    String get(String name, String fallback) {
        String value = environment.get(name);
        return value == null ? values.getProperty(name, fallback) : value;
    }

    String base() {
        return get("AI_BASE_URL", "").trim().replaceAll("/+$", "");
    }

    String model() {
        return get("AI_MODEL", "configured-model").trim();
    }

    String key() {
        return get("AI_API_KEY", "").trim();
    }

    CompatibleLlmAdapter generator() {
        String base = base();
        if (base.isEmpty()) return null;
        URI uri = URI.create(base);
        if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("AI_BASE_URL 请只填写服务基础地址，不要包含密钥、查询参数或片段");
        if (model().isEmpty()) throw new IllegalArgumentException("请填写 AI_MODEL");
        boolean siliconflow = "api.siliconflow.cn".equalsIgnoreCase(uri.getHost());
        if (key().equals("REPLACE_WITH_YOUR_SILICONFLOW_API_KEY")
                || (siliconflow && key().isBlank()))
            throw new IllegalArgumentException("请先在 config/ai.local.properties 填写 AI_API_KEY");
        if (siliconflow && !"https".equalsIgnoreCase(uri.getScheme()))
            throw new IllegalArgumentException("硅基流动 API 地址必须使用 HTTPS");
        if (key().contains("\n") || key().contains("\r"))
            throw new IllegalArgumentException("AI_API_KEY 不能包含换行");
        String thinking = get("AI_ENABLE_THINKING", "").trim();
        Boolean enabled = null;
        if (!thinking.isEmpty()) {
            if (!thinking.equalsIgnoreCase("true") && !thinking.equalsIgnoreCase("false"))
                throw new IllegalArgumentException("AI_ENABLE_THINKING 只能填 true 或 false");
            enabled = Boolean.valueOf(thinking);
        }
        return new CompatibleLlmAdapter(
                base, model(), key(), get("AI_RESPONSE_FORMAT", "json_schema").trim(), enabled);
    }
}
