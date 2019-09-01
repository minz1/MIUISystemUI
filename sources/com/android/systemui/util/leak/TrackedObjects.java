package com.android.systemui.util.leak;

import java.util.Collection;
import java.util.WeakHashMap;

public class TrackedObjects {
    private final WeakHashMap<Class<?>, TrackedClass<?>> mTrackedClasses = new WeakHashMap<>();
    private final TrackedCollections mTrackedCollections;

    private static class TrackedClass<T> extends AbstractCollection<T> {
        final WeakIdentityHashMap<T, Void> instances;

        private TrackedClass() {
            this.instances = new WeakIdentityHashMap<>();
        }

        /* access modifiers changed from: package-private */
        public void track(T object) {
            this.instances.put(object, null);
        }

        public int size() {
            return this.instances.size();
        }

        public boolean isEmpty() {
            return this.instances.isEmpty();
        }
    }

    public TrackedObjects(TrackedCollections trackedCollections) {
        this.mTrackedCollections = trackedCollections;
    }

    public synchronized <T> void track(T object) {
        Class<?> clazz = object.getClass();
        TrackedClass<T> trackedClass = this.mTrackedClasses.get(clazz);
        if (trackedClass == null) {
            trackedClass = new TrackedClass<>();
            this.mTrackedClasses.put(clazz, trackedClass);
        }
        trackedClass.track(object);
        this.mTrackedCollections.track(trackedClass, clazz.getName());
    }

    public static boolean isTrackedObject(Collection<?> collection) {
        return collection instanceof TrackedClass;
    }
}
