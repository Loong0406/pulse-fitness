package com.pulse.fitness;

import android.content.*;
import android.os.*;
import com.pulse.fitness.data.*;
import com.pulse.fitness.presentation.FitnessViewModel;
import com.pulse.core.data.*;
import com.pulse.core.auth.DemoPhoneAuth;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

/** Singleton composition root. Domain classes still receive interfaces explicitly. */
public final class AppContainer {
    private static volatile AppContainer instance;
    public final SharedPreferences data, settings;
    public final SecureTokenStore tokens;
    public final LocalSessionStore sessions;
    public final DemoPhoneAuth phoneAuth =
            new DemoPhoneAuth(
                    SystemClock::elapsedRealtime, new java.security.SecureRandom()::nextInt);
    public final Repositories.Factory repositories;
    public final TrainingFacade training;
    public final ExecutorService worker = Executors.newFixedThreadPool(2);
    public final Handler main = new Handler(Looper.getMainLooper());
    public final FitnessViewModel viewModel;

    private AppContainer(Context context) {
        data = context.getSharedPreferences("pulse_data", Context.MODE_PRIVATE);
        settings = context.getSharedPreferences("pulse_settings", Context.MODE_PRIVATE);
        tokens = new SecureTokenStore(settings);
        sessions =
                new LocalSessionStore(
                        context.getSharedPreferences("pulse_auth", Context.MODE_PRIVATE));
        repositories = new PreferencesRepositoryFactory(data);
        training = new TrainingFacade(repositories);
        viewModel = new FitnessViewModel(this);
    }

    public static void initialize(Context context) {
        if (instance == null)
            synchronized (AppContainer.class) {
                if (instance == null) instance = new AppContainer(context.getApplicationContext());
            }
    }

    public static AppContainer get() {
        if (instance == null) throw new IllegalStateException("Application 未初始化");
        return instance;
    }

    public String endpoint() {
        return settings.getString("endpoint", BuildConfig.DEBUG ? "http://10.0.2.2:8080" : "");
    }

    public void saveConnection(String endpoint, String token) throws Exception {
        URI uri = URI.create(endpoint.trim());
        if (uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || endpoint.length() > 2048
                || !("https".equals(uri.getScheme())
                        || (BuildConfig.DEBUG && "http".equals(uri.getScheme()))))
            throw new IllegalArgumentException(
                    BuildConfig.DEBUG ? "请输入完整的 http(s) 服务地址" : "正式版本的服务地址必须使用 HTTPS");
        if (token.length() > 512 || token.contains("\n") || token.contains("\r"))
            throw new IllegalArgumentException("服务令牌格式无效");
        tokens.save(token);
        settings.edit().putString("endpoint", endpoint.trim().replaceAll("/+$", "")).apply();
    }

    public Set<String> favorites() {
        return new HashSet<>(data.getStringSet("favorites", Collections.emptySet()));
    }

    public void favorite(String id) {
        Set<String> values = favorites();
        if (!values.add(id)) values.remove(id);
        data.edit().putStringSet("favorites", values).apply();
    }
}
