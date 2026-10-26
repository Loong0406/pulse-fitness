package com.pulse.core.plan;

/** Factory Method: adding a backend requires a new creator, not changing existing creators. */
public abstract class PlanGeneratorFactory {
    protected abstract PlanGenerator createGenerator();

    public final PlanGenerator createSafeGenerator() {
        return new SafePlanGeneratorDecorator(createGenerator());
    }

    public static final class Local extends PlanGeneratorFactory {
        @Override
        protected PlanGenerator createGenerator() {
            return new LocalPlanGenerator();
        }
    }
}
