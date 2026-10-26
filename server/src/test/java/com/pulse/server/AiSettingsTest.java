package com.pulse.server;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class AiSettingsTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private Path config(String text) throws Exception {
        Path p = temp.newFile().toPath();
        Files.writeString(p, text, StandardCharsets.UTF_8);
        return p;
    }

    @Test
    public void localOnlyOverrideKeepsDatabaseGatewayUsableWithoutAiKey() throws Exception {
        Path p =
                config(
                        "AI_BASE_URL=https://api.siliconflow.cn/v1\nAI_API_KEY=REPLACE_WITH_YOUR_SILICONFLOW_API_KEY\n");
        assertNull(new AiSettings(p, Map.of("AI_BASE_URL", "")).generator());
    }

    @Test
    public void placeholderOrMissingSiliconflowKeyIsRejectedBeforeAnyRequest() throws Exception {
        Path p =
                config(
                        "AI_BASE_URL=https://api.siliconflow.cn/v1\nAI_API_KEY=REPLACE_WITH_YOUR_SILICONFLOW_API_KEY\n");
        assertThrows(IllegalArgumentException.class, () -> new AiSettings(p, Map.of()).generator());
        Path noKey = config("AI_BASE_URL=https://api.siliconflow.cn/v1\n");
        assertThrows(
                IllegalArgumentException.class, () -> new AiSettings(noKey, Map.of()).generator());
    }

    @Test
    public void modelIdAndJsonModeLoadFromFileAndEnvironmentWins() throws Exception {
        Path p =
                config(
                        "AI_BASE_URL=https://api.siliconflow.cn/v1\nAI_MODEL=deepseek-ai/DeepSeek-V4.1-Flash\nAI_API_KEY=test-only-key\nAI_RESPONSE_FORMAT=json_object\nAI_ENABLE_THINKING=false\n");
        AiSettings settings = new AiSettings(p, Map.of());
        assertEquals("deepseek-ai/DeepSeek-V4.1-Flash", settings.model());
        assertNotNull(settings.generator());
        AiSettings override = new AiSettings(p, Map.of("AI_MODEL", "explicit-model"));
        assertEquals("explicit-model", override.model());
        assertEquals("test-only-key", settings.key());
    }

    @Test
    public void invalidModeThinkingAndInsecureSiliconflowAddressAreRejected() throws Exception {
        Path p = config("AI_BASE_URL=https://api.siliconflow.cn/v1\nAI_API_KEY=test-only-key\n");
        assertThrows(
                IllegalArgumentException.class,
                () -> new AiSettings(p, Map.of("AI_RESPONSE_FORMAT", "arbitrary")).generator());
        assertThrows(
                IllegalArgumentException.class,
                () -> new AiSettings(p, Map.of("AI_ENABLE_THINKING", "perhaps")).generator());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AiSettings(p, Map.of("AI_BASE_URL", "http://api.siliconflow.cn/v1"))
                                .generator());
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new AiSettings(
                                        p,
                                        Map.of(
                                                "AI_BASE_URL",
                                                "https://api.siliconflow.cn/v1?key=secret"))
                                .generator());
    }
}
