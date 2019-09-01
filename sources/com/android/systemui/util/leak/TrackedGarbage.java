package com.android.systemui.util.leak;

import android.os.SystemClock;
import android.util.ArrayMap;
import java.io.PrintWriter;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class TrackedGarbage {
    private final HashSet<LeakReference> mGarbage = new HashSet<>();
    private final ReferenceQueue<Object> mRefQueue = new ReferenceQueue<>();
    private final TrackedCollections mTrackedCollections;

    private static class LeakReference extends WeakReference<Object> {
        /* access modifiers changed from: private */
        public final Class<?> clazz;
        /* access modifiers changed from: private */
        public final long createdUptimeMillis = SystemClock.uptimeMillis();

        LeakReference(Object t, ReferenceQueue<Object> queue) {
            super(t, queue);
            this.clazz = t.getClass();
        }
    }

    public TrackedGarbage(TrackedCollections trackedCollections) {
        this.mTrackedCollections = trackedCollections;
    }

    public synchronized void track(Object o) {
        cleanUp();
        this.mGarbage.add(new LeakReference(o, this.mRefQueue));
        this.mTrackedCollections.track(this.mGarbage, "Garbage");
    }

    private void cleanUp() {
        while (true) {
            Reference<? extends Object> poll = this.mRefQueue.poll();
            Reference<? extends Object> reference = poll;
            if (poll != null) {
                this.mGarbage.remove(reference);
            } else {
                return;
            }
        }
    }

    public synchronized void dump(PrintWriter pw) {
        cleanUp();
        long now = SystemClock.uptimeMillis();
        ArrayMap<Class<?>, Integer> acc = new ArrayMap<>();
        ArrayMap<Class<?>, Integer> accOld = new ArrayMap<>();
        Iterator<LeakReference> it = this.mGarbage.iterator();
        while (it.hasNext()) {
            LeakReference ref = it.next();
            acc.put(ref.clazz, Integer.valueOf(getOrDefault(acc, ref.clazz, 0).intValue() + 1));
            if (isOld(ref.createdUptimeMillis, now)) {
                accOld.put(ref.clazz, Integer.valueOf(getOrDefault(accOld, ref.clazz, 0).intValue() + 1));
            }
        }
        for (Map.Entry<Class<?>, Integer> entry : acc.entrySet()) {
            pw.print(entry.getKey().getName());
            pw.print(": ");
            pw.print(entry.getValue());
            pw.print(" total, ");
            pw.print(getOrDefault(accOld, entry.getKey(), 0));
            pw.print(" old");
            pw.println();
        }
    }

    private Integer getOrDefault(Map<Class<?>, Integer> map, Object key, Integer defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    public synchronized int countOldGarbage() {
        int result;
        cleanUp();
        long now = SystemClock.uptimeMillis();
        result = 0;
        Iterator<LeakReference> it = this.mGarbage.iterator();
        while (it.hasNext()) {
            if (isOld(it.next().createdUptimeMillis, now)) {
                result++;
            }
        }
        return result;
    }

    private boolean isOld(long createdUptimeMillis, long now) {
        return 60000 + createdUptimeMillis < now;
    }
}
