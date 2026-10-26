package com.pulse.core.util;

import java.util.concurrent.CopyOnWriteArrayList;

/** Observer. Owners explicitly unsubscribe at the lifecycle boundary. */
public final class ObservableState<T> {
    public interface Listener<T> {
        void changed(T value);
    }

    private final CopyOnWriteArrayList<Listener<T>> listeners = new CopyOnWriteArrayList<>();
    private T value;

    public ObservableState(T initial) {
        value = initial;
    }

    public T get() {
        return value;
    }

    public void set(T next) {
        value = next;
        for (Listener<T> l : listeners) l.changed(next);
    }

    public void observe(Listener<T> l) {
        listeners.addIfAbsent(l);
        l.changed(value);
    }

    public void remove(Listener<T> l) {
        listeners.remove(l);
    }
}
