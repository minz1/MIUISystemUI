package com.miui.systemui.dependencies;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BaseResolver implements Resolver {
    private Map<Class<?>, Map<String, Object>> mDependencies = new HashMap();
    private Map<Class<?>, Map<String, Provider>> mProviders = new HashMap();

    public <T> T get(Class<T> cls, String tag) {
        return getInner(cls, tag);
    }

    public Map<Class<?>, Map<String, Provider>> getProviders() {
        return this.mProviders;
    }

    public Set<Class<?>> getAllClassesFor(Class<?> cls) {
        Set<Class<?>> result = new HashSet<>();
        String name = cls.getName();
        Map<String, Provider> entries = getProviders().get(Class.class);
        if (entries != null) {
            for (Map.Entry<String, Provider> entry : entries.entrySet()) {
                if (entry.getKey().startsWith(name)) {
                    result.add((Class) entry.getValue().create(this));
                }
            }
        }
        return result;
    }

    private synchronized <T> T getInner(Class<T> cls, String tag) {
        T obj;
        Map<String, Object> dependencies = this.mDependencies.get(cls);
        if (dependencies == null) {
            dependencies = new HashMap<>();
            this.mDependencies.put(cls, dependencies);
        }
        obj = dependencies.get(tag);
        if (obj == null) {
            obj = create(cls, tag);
            dependencies.put(tag, obj);
        }
        return obj;
    }

    /* access modifiers changed from: protected */
    public <T> T create(Class<T> cls, String tag) {
        Map<String, Provider> providers = this.mProviders.get(cls);
        if (providers != null) {
            Provider<T> provider = providers.get(tag);
            if (provider != null) {
                return provider.create(this);
            }
            throw new IllegalArgumentException("No dependency found for " + cls + " with tag: " + tag);
        }
        throw new IllegalArgumentException("No dependencies found for " + cls);
    }

    /* access modifiers changed from: protected */
    public <T> void put(Class<T> cls, String tag, Provider<T> provider) {
        Map<String, Provider> providers = this.mProviders.get(cls);
        if (providers == null) {
            providers = new HashMap<>();
            this.mProviders.put(cls, providers);
        }
        providers.put(tag, provider);
    }
}
