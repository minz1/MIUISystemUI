package com.android.systemui.util.leak;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WeakIdentityHashMap<K, V> {
    private final HashMap<WeakReference<K>, V> mMap = new HashMap<>();
    private final ReferenceQueue<Object> mRefQueue = new ReferenceQueue<>();

    private static class CmpWeakReference<K> extends WeakReference<K> {
        private final int mHashCode;

        public CmpWeakReference(K key) {
            super(key);
            this.mHashCode = System.identityHashCode(key);
        }

        public CmpWeakReference(K key, ReferenceQueue<Object> refQueue) {
            super(key, refQueue);
            this.mHashCode = System.identityHashCode(key);
        }

        public boolean equals(Object o) {
            boolean z = true;
            if (o == this) {
                return true;
            }
            K k = get();
            if (k == null || !(o instanceof CmpWeakReference)) {
                return false;
            }
            if (((CmpWeakReference) o).get() != k) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return this.mHashCode;
        }
    }

    private void cleanUp() {
        while (true) {
            Reference<? extends Object> poll = this.mRefQueue.poll();
            Reference<? extends Object> reference = poll;
            if (poll != null) {
                this.mMap.remove(reference);
            } else {
                return;
            }
        }
    }

    public void put(K key, V value) {
        cleanUp();
        this.mMap.put(new CmpWeakReference(key, this.mRefQueue), value);
    }

    public V get(K key) {
        cleanUp();
        return this.mMap.get(new CmpWeakReference(key));
    }

    public Set<Map.Entry<WeakReference<K>, V>> entrySet() {
        return this.mMap.entrySet();
    }

    public int size() {
        cleanUp();
        return this.mMap.size();
    }

    public boolean isEmpty() {
        cleanUp();
        return this.mMap.isEmpty();
    }
}
