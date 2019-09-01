package com.android.systemui.util.leak;

import android.os.Build;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;
import com.android.systemui.Dumpable;
import com.android.systemui.util.function.Predicate;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collection;

public class LeakDetector implements Dumpable {
    public static final boolean ENABLED = Build.IS_DEBUGGABLE;
    private final TrackedCollections mTrackedCollections;
    private final TrackedGarbage mTrackedGarbage;
    private final TrackedObjects mTrackedObjects;

    @VisibleForTesting
    public LeakDetector(TrackedCollections trackedCollections, TrackedGarbage trackedGarbage, TrackedObjects trackedObjects) {
        this.mTrackedCollections = trackedCollections;
        this.mTrackedGarbage = trackedGarbage;
        this.mTrackedObjects = trackedObjects;
    }

    public <T> void trackInstance(T object) {
        if (this.mTrackedObjects != null) {
            this.mTrackedObjects.track(object);
        }
    }

    public <T> void trackCollection(Collection<T> collection, String tag) {
        if (this.mTrackedCollections != null) {
            this.mTrackedCollections.track(collection, tag);
        }
    }

    public void trackGarbage(Object o) {
        if (this.mTrackedGarbage != null) {
            this.mTrackedGarbage.track(o);
        }
    }

    /* access modifiers changed from: package-private */
    public TrackedGarbage getTrackedGarbage() {
        return this.mTrackedGarbage;
    }

    public void dump(FileDescriptor df, PrintWriter w, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(w, "  ");
        pw.println("SYSUI LEAK DETECTOR");
        pw.increaseIndent();
        if (this.mTrackedCollections == null || this.mTrackedGarbage == null) {
            pw.println("disabled");
        } else {
            pw.println("TrackedCollections:");
            pw.increaseIndent();
            this.mTrackedCollections.dump(pw, new Predicate<Collection<?>>() {
                public boolean test(Collection<?> col) {
                    return !TrackedObjects.isTrackedObject(col);
                }
            });
            pw.decreaseIndent();
            pw.println();
            pw.println("TrackedObjects:");
            pw.increaseIndent();
            this.mTrackedCollections.dump(pw, new Predicate<Collection<?>>() {
                public boolean test(Collection<?> col) {
                    return TrackedObjects.isTrackedObject(col);
                }
            });
            pw.decreaseIndent();
            pw.println();
            pw.print("TrackedGarbage:");
            pw.increaseIndent();
            this.mTrackedGarbage.dump(pw);
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
        pw.println();
    }

    public static LeakDetector create() {
        if (!ENABLED) {
            return new LeakDetector(null, null, null);
        }
        TrackedCollections collections = new TrackedCollections();
        return new LeakDetector(collections, new TrackedGarbage(collections), new TrackedObjects(collections));
    }
}
