package com.pulse.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

/** Explicit local development setup. Never changes a server password or an existing application account. */
public final class BootstrapMysql {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: BootstrapMysql <admin-properties> <application-properties>");
            System.exit(1);
            return;
        }
        try {
            initialize(Path.of(args[0]), Path.of(args[1]));
        } catch (Exception failure) {
            // Do not print the JDBC URL, credentials, SQL text or a credential-bearing exception.
            String message =
                    failure instanceof SetupException
                            ? failure.getMessage()
                            : "MySQL setup failed. Check administrator password, port and CREATE DATABASE/CREATE USER/GRANT privileges. Existing data was not deleted.";
            System.err.println(message);
            System.exit(1);
        }
    }

    private static Properties read(Path file) throws IOException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    private static boolean isPlaceholder(Path file) throws IOException {
        return !Files.exists(file)
                || "REPLACE_WITH_YOUR_DATABASE_PASSWORD"
                        .equals(read(file).getProperty("DB_PASSWORD"));
    }

    private static void initialize(Path adminFile, Path appFile) throws Exception {
        Properties admin = read(adminFile);
        String host = admin.getProperty("MYSQL_HOST", "127.0.0.1"),
                user = admin.getProperty("MYSQL_ADMIN_USER", "root"),
                password = admin.getProperty("MYSQL_ADMIN_PASSWORD", "");
        int port = Integer.parseInt(admin.getProperty("MYSQL_PORT", "3306"));
        if (!Set.of("127.0.0.1", "localhost").contains(host) || port < 1 || port > 65535)
            throw new SetupException(
                    "Setup is limited to localhost MySQL and database pulse_fitness.");
        if (password.isEmpty() || password.equals("REPLACE_WITH_MYSQL_ADMIN_PASSWORD"))
            throw new SetupException(
                    "Fill MYSQL_ADMIN_USER and MYSQL_ADMIN_PASSWORD in config/database-admin.local.properties first. Do not send the password in chat.");
        if (!isPlaceholder(appFile))
            throw new SetupException(
                    "Application configuration already contains credentials. It was not overwritten. Use Test-Database.ps1 to check the existing account.");
        byte[] initial = Files.exists(appFile) ? Files.readAllBytes(appFile) : null;
        String query = "?connectionTimeZone=UTC&connectTimeout=5000&socketTimeout=10000";
        String base = "jdbc:mysql://" + host + ":" + port + "/";
        SecureRandom random = new SecureRandom();
        byte[] seed = new byte[32];
        random.nextBytes(seed);
        String
                appUser =
                        "pulse_app_"
                                + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                appPassword = "Pf9!" + HexFormat.of().formatHex(seed);
        // SQL identifiers and passwords below come only from fixed literals and generated
        // hexadecimal strings.
        try (Connection c = DriverManager.getConnection(base + query, user, password);
                Statement statement = c.createStatement()) {
            try (ResultSet r = statement.executeQuery("SELECT VERSION()")) {
                if (!r.next()) throw new SQLException("No MySQL version");
            }
            statement.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS pulse_fitness CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        JdbcAccountRepository.connect(base + "pulse_fitness" + query, user, password).initialize();
        try (Connection c = DriverManager.getConnection(base + query, user, password);
                Statement statement = c.createStatement()) {
            for (String allowedHost : List.of("localhost", "127.0.0.1")) {
                String account = "'" + appUser + "'@'" + allowedHost + "'";
                statement.executeUpdate(
                        "CREATE USER " + account + " IDENTIFIED BY '" + appPassword + "'");
                statement.executeUpdate(
                        "GRANT SELECT, INSERT, UPDATE ON pulse_fitness.pulse_users TO " + account);
                statement.executeUpdate(
                        "GRANT SELECT, INSERT, UPDATE ON pulse_fitness.pulse_login_events TO "
                                + account);
            }
        }
        JdbcAccountRepository repository =
                JdbcAccountRepository.connect(base + "pulse_fitness" + query, appUser, appPassword);
        repository.checkConnection();
        repository.checkSchema();
        repository.verifyWriteAccess();
        // A user may have edited this file while the database operations were running; preserve
        // their changes.
        byte[] current = Files.exists(appFile) ? Files.readAllBytes(appFile) : null;
        if (!Arrays.equals(initial, current))
            throw new SetupException(
                    "Application configuration changed during setup; it was not overwritten. Contact the project maintainer before retrying.");
        Properties output = new Properties();
        output.setProperty("DB_URL", base + "pulse_fitness" + query);
        output.setProperty("DB_USER", appUser);
        output.setProperty("DB_PASSWORD", appPassword);
        output.setProperty("AUTH_DEMO_ENABLED", "true");
        Path target = appFile.toAbsolutePath(),
                temp = Files.createTempFile(target.getParent(), "mysql-connection-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                output.store(
                        writer,
                        "Generated application-only MySQL credentials. Never share this file.");
            }
            Files.move(
                    temp,
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
        System.out.println(
                "MySQL pulse_fitness: database, account tables and dedicated application account ready.");
        System.out.println(
                "SQL insert/update probes were rolled back; no sample users or login events were retained.");
        System.out.println(
                "Application configuration saved. No administrator password was copied to it.");
    }

    private static final class SetupException extends Exception {
        SetupException(String message) {
            super(message);
        }
    }
}
