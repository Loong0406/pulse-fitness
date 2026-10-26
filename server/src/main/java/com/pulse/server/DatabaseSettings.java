package com.pulse.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

/** Database secrets stay in environment variables or an ignored local file, never in the APK. */
final class DatabaseSettings {
    private final Properties values = new Properties();

    DatabaseSettings() throws IOException {
        String file = System.getenv("DB_CONFIG_FILE");
        if (file != null && !file.isBlank())
            try (Reader r = Files.newBufferedReader(Path.of(file), StandardCharsets.UTF_8)) {
                values.load(r);
            }
    }

    String get(String key, String fallback) {
        String value = System.getenv(key);
        return value != null ? value : values.getProperty(key, fallback);
    }

    JdbcAccountRepository repository() {
        String url = get("DB_URL", "");
        if (url.isBlank()) return null;
        String user = get("DB_USER", ""), password = get("DB_PASSWORD", "");
        if (user.isBlank() || password.equals("REPLACE_WITH_YOUR_DATABASE_PASSWORD"))
            throw new IllegalArgumentException("请先填写 config/database.local.properties 的数据库账号和密码");
        return JdbcAccountRepository.connect(url, user, password);
    }
}
