package com.pulse.server;

import com.google.gson.*;
import org.junit.*;
import static org.junit.Assert.*;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Real JDBC/SQL tests on isolated H2 in MySQL mode. Production MySQL needs a separate live connection check. */
public final class AccountDatabaseTest {
    private static final String PHONE = "13800138000", TOKEN = "test-database-gateway-token-123";
    private String url;
    private JdbcAccountRepository repository;
    private FitnessServer server;

    @Before
    public void setup() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        repository = JdbcAccountRepository.connect(url, "sa", "");
        repository.checkConnection();
        repository.initialize();
        repository.initialize();
        repository.checkSchema();
    }

    @After
    public void stop() {
        if (server != null) server.stop();
    }

    private int count(String table) throws Exception {
        try (Connection c = DriverManager.getConnection(url, "sa", "");
                Statement q = c.createStatement();
                ResultSet r = q.executeQuery("SELECT COUNT(*) FROM " + table)) {
            r.next();
            return r.getInt(1);
        }
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        HttpRequest.Builder q =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
                        .header("Content-Type", "application/json");
        if (token != null) q.header("Authorization", "Bearer " + token);
        return HttpClient.newHttpClient()
                .send(
                        q.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                        HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void repeatPhoneUsesSameUserAndPersistsSeparateLoginEvents() throws Exception {
        JdbcAccountRepository.Account first = repository.recordDemoLogin(PHONE),
                second = repository.recordDemoLogin(PHONE);
        assertEquals(first.id, second.id);
        assertEquals(1, count("pulse_users"));
        assertEquals(2, count("pulse_login_events"));
        try (Connection c = DriverManager.getConnection(url, "sa", "");
                Statement q = c.createStatement();
                ResultSet r = q.executeQuery("SELECT phone_verified FROM pulse_users")) {
            assertTrue(r.next());
            assertFalse(r.getBoolean(1));
        }
        JdbcAccountRepository reopened = JdbcAccountRepository.connect(url, "sa", "");
        assertEquals(first.id, reopened.recordDemoLogin(PHONE).id);
    }

    @Test
    public void writeVerificationDoesNotKeepSyntheticUsersOrEvents() throws Exception {
        repository.verifyWriteAccess();
        assertEquals(0, count("pulse_users"));
        assertEquals(0, count("pulse_login_events"));
        repository.recordDemoLogin(PHONE);
        repository.verifyWriteAccess();
        assertEquals(1, count("pulse_users"));
        assertEquals(1, count("pulse_login_events"));
        try (Connection c = DriverManager.getConnection(url, "sa", "");
                Statement q = c.createStatement()) {
            q.executeUpdate(
                    "ALTER TABLE pulse_login_events ADD CONSTRAINT reject_probe CHECK(login_method <> 'CONNECTION_PROBE')");
        }
        assertThrows(SQLException.class, () -> repository.verifyWriteAccess());
        assertEquals(1, count("pulse_users"));
        assertEquals(1, count("pulse_login_events"));
    }

    @Test
    public void failedEventInsertRollsBackNewUser() throws Exception {
        try (Connection c = DriverManager.getConnection(url, "sa", "");
                Statement q = c.createStatement()) {
            q.executeUpdate("ALTER TABLE pulse_login_events ALTER COLUMN login_method VARCHAR(1)");
        }
        assertThrows(SQLException.class, () -> repository.recordDemoLogin(PHONE));
        assertEquals(0, count("pulse_users"));
        assertEquals(0, count("pulse_login_events"));
    }

    @Test
    public void concurrentSamePhoneDoesNotDuplicateUsers() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<JdbcAccountRepository.Account>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++)
                results.add(pool.submit(() -> repository.recordDemoLogin(PHONE)));
            Set<String> ids = new HashSet<>();
            for (Future<JdbcAccountRepository.Account> result : results) ids.add(result.get().id);
            assertEquals(1, ids.size());
            assertEquals(1, count("pulse_users"));
            assertEquals(8, count("pulse_login_events"));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void challengeChecksPhoneCodeAndReplayBeforeWritingSql() throws Exception {
        DemoAccountService service = new DemoAccountService(repository);
        DemoAccountService.Challenge challenge = service.request(PHONE);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.verify(challenge.challengeId, "13900139000", challenge.code));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.verify(challenge.challengeId, PHONE, "abcdef"));
        assertEquals(0, count("pulse_users"));
        service.verify(challenge.challengeId, PHONE, challenge.code);
        assertEquals(1, count("pulse_users"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.verify(challenge.challengeId, PHONE, challenge.code));
        assertThrows(IllegalArgumentException.class, () -> service.request(PHONE));
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.recordDemoLogin("13800138000' OR 1=1"));
    }

    @Test
    public void sqlFailureCanRetrySameVerifiedChallenge() throws Exception {
        DemoAccountService service = new DemoAccountService(repository);
        DemoAccountService.Challenge challenge = service.request(PHONE);
        try (Connection c = DriverManager.getConnection(url, "sa", "");
                Statement q = c.createStatement()) {
            q.executeUpdate("ALTER TABLE pulse_login_events ALTER COLUMN login_method VARCHAR(1)");
        }
        assertThrows(
                SQLException.class,
                () -> service.verify(challenge.challengeId, PHONE, challenge.code));
        try (Connection c = DriverManager.getConnection(url, "sa", "");
                Statement q = c.createStatement()) {
            q.executeUpdate("ALTER TABLE pulse_login_events ALTER COLUMN login_method VARCHAR(24)");
        }
        service.verify(challenge.challengeId, PHONE, challenge.code);
        assertEquals(1, count("pulse_users"));
        assertEquals(1, count("pulse_login_events"));
    }

    @Test
    public void httpFlowRequiresGatewayTokenAndReturnsActualSqlUserId() throws Exception {
        server =
                new FitnessServer(
                        "127.0.0.1", 0, TOKEN, "test", null, new DemoAccountService(repository));
        server.start();
        assertEquals(
                401,
                post("/api/v1/auth/demo-code", "{\"phone\":\"" + PHONE + "\"}", null).statusCode());
        assertEquals(400, post("/api/v1/auth/demo-code", "{}", TOKEN).statusCode());
        assertEquals(404, post("/api/v1/auth/demo-login/unknown", "{}", TOKEN).statusCode());
        assertEquals(413, post("/api/v1/auth/demo-code", "x".repeat(4097), TOKEN).statusCode());
        JsonObject challenge =
                JsonParser.parseString(
                                post(
                                                "/api/v1/auth/demo-code",
                                                "{\"phone\":\"" + PHONE + "\"}",
                                                TOKEN)
                                        .body())
                        .getAsJsonObject();
        assertTrue(challenge.get("demo").getAsBoolean());
        assertFalse(challenge.get("phoneVerified").getAsBoolean());
        JsonObject login = new JsonObject();
        login.addProperty("phone", PHONE);
        login.add("challengeId", challenge.get("challengeId"));
        login.add("code", challenge.get("code"));
        HttpResponse<String> response = post("/api/v1/auth/demo-login", login.toString(), TOKEN);
        assertEquals(200, response.statusCode());
        JsonObject account = JsonParser.parseString(response.body()).getAsJsonObject();
        assertFalse(account.get("userId").getAsString().isBlank());
        assertEquals(1, count("pulse_users"));
        assertEquals(1, count("pulse_login_events"));
        assertEquals(400, post("/api/v1/auth/demo-login", login.toString(), TOKEN).statusCode());
    }

    @Test
    public void unconfiguredDatabaseDoesNotPretendLoginSucceeded() throws Exception {
        server = new FitnessServer("127.0.0.1", 0, TOKEN, "test", null);
        server.start();
        assertEquals(
                503,
                post("/api/v1/auth/demo-code", "{\"phone\":\"" + PHONE + "\"}", TOKEN)
                        .statusCode());
        assertEquals(0, count("pulse_users"));
    }
}
