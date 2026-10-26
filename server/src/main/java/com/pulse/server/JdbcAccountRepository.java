package com.pulse.server;

import com.pulse.core.auth.DemoPhoneAuth;
import java.sql.*;
import java.util.UUID;

/** Only the gateway knows database credentials. Prepared statements keep phone input out of SQL syntax. */
public final class JdbcAccountRepository {
    public interface Connections {
        Connection open() throws SQLException;
    }

    public static final class Account {
        public final String id, phone;

        public Account(String id, String phone) {
            this.id = id;
            this.phone = phone;
        }
    }

    private final Connections connections;

    public JdbcAccountRepository(Connections connections) {
        this.connections = connections;
    }

    public static JdbcAccountRepository connect(String url, String user, String password) {
        if (!url.startsWith("jdbc:mysql:") && !url.startsWith("jdbc:h2:"))
            throw new IllegalArgumentException("当前接入支持 MySQL，H2 仅用于测试");
        return new JdbcAccountRepository(() -> DriverManager.getConnection(url, user, password));
    }

    public void checkConnection() throws SQLException {
        try (Connection c = connections.open();
                Statement s = c.createStatement();
                ResultSet r = s.executeQuery("SELECT 1")) {
            if (!r.next() || r.getInt(1) != 1) throw new SQLException("数据库连接检测失败");
        }
    }

    public void checkSchema() throws SQLException {
        try (Connection c = connections.open();
                Statement s = c.createStatement()) {
            try (ResultSet ignored =
                    s.executeQuery(
                            "SELECT id,phone,phone_verified,created_at,last_login_at FROM pulse_users WHERE 1=0")) {}
            try (ResultSet ignored =
                    s.executeQuery(
                            "SELECT id,user_id,logged_in_at,login_method FROM pulse_login_events WHERE 1=0")) {}
        }
    }

    /** Exercise both tables and UPDATE, then roll everything back. Never leave a synthetic user behind. */
    public void verifyWriteAccess() throws SQLException {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                String id = UUID.randomUUID().toString(), phone = null;
                for (int i = 0; i < 100; i++) {
                    String candidate = String.format(java.util.Locale.ROOT, "%011d", i);
                    try (PreparedStatement q =
                            c.prepareStatement("SELECT id FROM pulse_users WHERE phone=?")) {
                        q.setString(1, candidate);
                        try (ResultSet r = q.executeQuery()) {
                            if (!r.next()) {
                                phone = candidate;
                                break;
                            }
                        }
                    }
                }
                if (phone == null) throw new SQLException("No free verification phone slot");
                long now = System.currentTimeMillis();
                try (PreparedStatement q =
                        c.prepareStatement(
                                "INSERT INTO pulse_users(id,phone,phone_verified,created_at,last_login_at) VALUES(?,?,FALSE,?,?)")) {
                    q.setString(1, id);
                    q.setString(2, phone);
                    q.setLong(3, now);
                    q.setLong(4, now);
                    q.executeUpdate();
                }
                try (PreparedStatement q =
                        c.prepareStatement(
                                "INSERT INTO pulse_login_events(id,user_id,logged_in_at,login_method) VALUES(?,?,?,?)")) {
                    q.setString(1, UUID.randomUUID().toString());
                    q.setString(2, id);
                    q.setLong(3, now);
                    q.setString(4, "CONNECTION_PROBE");
                    q.executeUpdate();
                }
                try (PreparedStatement q =
                        c.prepareStatement("UPDATE pulse_users SET last_login_at=? WHERE id=?")) {
                    q.setLong(1, now);
                    q.setString(2, id);
                    q.executeUpdate();
                }
            } finally {
                c.rollback();
            }
        }
    }

    /** Explicit setup action, never run implicitly against an existing unrelated database. */
    public void initialize() throws SQLException {
        try (Connection c = connections.open();
                Statement s = c.createStatement()) {
            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS pulse_users (id VARCHAR(36) PRIMARY KEY,phone VARCHAR(11) NOT NULL UNIQUE,phone_verified BOOLEAN NOT NULL DEFAULT FALSE,created_at BIGINT NOT NULL,last_login_at BIGINT NOT NULL)");
            s.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS pulse_login_events (id VARCHAR(36) PRIMARY KEY,user_id VARCHAR(36) NOT NULL,logged_in_at BIGINT NOT NULL,login_method VARCHAR(24) NOT NULL,CONSTRAINT fk_pulse_login_user FOREIGN KEY(user_id) REFERENCES pulse_users(id))");
        }
    }

    /** Serializes this gateway's upsert; a UNIQUE phone constraint also protects other gateway processes. */
    public synchronized Account recordDemoLogin(String rawPhone) throws SQLException {
        String phone = DemoPhoneAuth.normalizePhone(rawPhone);
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                long now = System.currentTimeMillis();
                String id = null;
                try (PreparedStatement q =
                        c.prepareStatement("SELECT id FROM pulse_users WHERE phone=?")) {
                    q.setString(1, phone);
                    try (ResultSet r = q.executeQuery()) {
                        if (r.next()) id = r.getString(1);
                    }
                }
                if (id == null) {
                    id = UUID.randomUUID().toString();
                    try (PreparedStatement q =
                            c.prepareStatement(
                                    "INSERT INTO pulse_users(id,phone,phone_verified,created_at,last_login_at) VALUES(?,?,FALSE,?,?)")) {
                        q.setString(1, id);
                        q.setString(2, phone);
                        q.setLong(3, now);
                        q.setLong(4, now);
                        q.executeUpdate();
                    }
                } else
                    try (PreparedStatement q =
                            c.prepareStatement(
                                    "UPDATE pulse_users SET last_login_at=? WHERE id=?")) {
                        q.setLong(1, now);
                        q.setString(2, id);
                        q.executeUpdate();
                    }
                try (PreparedStatement q =
                        c.prepareStatement(
                                "INSERT INTO pulse_login_events(id,user_id,logged_in_at,login_method) VALUES(?,?,?,?)")) {
                    q.setString(1, UUID.randomUUID().toString());
                    q.setString(2, id);
                    q.setLong(3, now);
                    q.setString(4, "PHONE_CODE_DEMO");
                    q.executeUpdate();
                }
                c.commit();
                return new Account(id, phone);
            } catch (SQLException | RuntimeException failure) {
                c.rollback();
                throw failure;
            }
        }
    }
}
