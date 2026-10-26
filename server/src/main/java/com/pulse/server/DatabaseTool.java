package com.pulse.server;

/** Explicit command-line connection/schema check. Does not print SQL exceptions containing connection details. */
public final class DatabaseTool {
    public static void main(String[] args) {
        try {
            JdbcAccountRepository repository = new DatabaseSettings().repository();
            if (repository == null)
                throw new IllegalArgumentException("请通过 DB_CONFIG_FILE 或 DB_URL 配置数据库");
            repository.checkConnection();
            if (args.length == 1 && args[0].equals("init")) repository.initialize();
            repository.checkSchema();
            System.out.println("Database connection and pulse account tables: OK");
        } catch (Exception failure) {
            System.err.println(
                    "Database setup failed. Check database existence, username/password, port, permissions and account tables.");
            System.exit(1);
        }
    }
}
