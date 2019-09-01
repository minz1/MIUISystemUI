package com.android.systemui.recents.model;

import android.util.Log;
import android.util.LruCache;
import com.android.systemui.recents.model.Task;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskKeyLruCache<V> {
    private final LruCache<Integer, V> mCache;
    /* access modifiers changed from: private */
    public final EvictionCallback mEvictionCallback;
    /* access modifiers changed from: private */
    public final Map<Integer, Task.TaskKey> mKeys;

    public interface EvictionCallback {
        void onEntryEvicted(Task.TaskKey taskKey);
    }

    public TaskKeyLruCache(int cacheSize) {
        this(cacheSize, null);
    }

    public TaskKeyLruCache(int cacheSize, EvictionCallback evictionCallback) {
        this.mKeys = new ConcurrentHashMap();
        this.mEvictionCallback = evictionCallback;
        this.mCache = new LruCache<Integer, V>(cacheSize) {
            /* access modifiers changed from: protected */
            public void entryRemoved(boolean evicted, Integer taskId, V v, V v2) {
                if (TaskKeyLruCache.this.mEvictionCallback != null) {
                    TaskKeyLruCache.this.mEvictionCallback.onEntryEvicted((Task.TaskKey) TaskKeyLruCache.this.mKeys.get(taskId));
                }
                TaskKeyLruCache.this.mKeys.remove(taskId);
            }
        };
    }

    /* access modifiers changed from: package-private */
    public final V get(Task.TaskKey key) {
        return this.mCache.get(Integer.valueOf(key.id));
    }

    /* access modifiers changed from: package-private */
    public final V getAndInvalidateIfModified(Task.TaskKey key) {
        Task.TaskKey lastKey = this.mKeys.get(Integer.valueOf(key.id));
        if (lastKey == null || (lastKey.stackId == key.stackId && lastKey.lastActiveTime == key.lastActiveTime)) {
            return this.mCache.get(Integer.valueOf(key.id));
        }
        remove(key);
        return null;
    }

    /* access modifiers changed from: package-private */
    public final void put(Task.TaskKey key, V value) {
        if (key == null || value == null) {
            Log.e("TaskKeyLruCache", "Unexpected null key or value: " + key + ", " + value);
            return;
        }
        this.mKeys.put(Integer.valueOf(key.id), key);
        this.mCache.put(Integer.valueOf(key.id), value);
    }

    /* access modifiers changed from: package-private */
    public final void remove(Task.TaskKey key) {
        this.mCache.remove(Integer.valueOf(key.id));
        this.mKeys.remove(Integer.valueOf(key.id));
    }

    /* access modifiers changed from: package-private */
    public final void evictAll() {
        this.mCache.evictAll();
        this.mKeys.clear();
    }

    /* access modifiers changed from: package-private */
    public final void trimToSize(int cacheSize) {
        this.mCache.trimToSize(cacheSize);
    }

    public void dump(String prefix, PrintWriter writer) {
        String innerPrefix = prefix + "  ";
        writer.print(prefix);
        writer.print("TaskKeyLruCache");
        writer.print(" numEntries=");
        writer.print(this.mKeys.size());
        writer.println();
        for (Integer num : this.mKeys.keySet()) {
            writer.print(innerPrefix);
            writer.println(this.mKeys.get(num));
        }
    }
}
