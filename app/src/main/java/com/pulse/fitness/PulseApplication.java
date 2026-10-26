package com.pulse.fitness;

import android.app.Application;

public final class PulseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppContainer.initialize(this);
    }
}
