package com.android.systemui.miui;

import android.util.Log;
import com.android.systemui.Constants;
import com.android.systemui.Dependency;
import com.miui.systemui.annotation.Inject;
import com.miui.systemui.dependencies.BaseResolver;
import com.miui.systemui.dependencies.Provider;
import com.miui.systemui.dependencies.Resolver;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Dependencies extends BaseResolver {
    private static Dependencies sInstance;

    private static class Builder {
        private List<Resolver> mResolvers;

        private Builder() {
            this.mResolvers = new ArrayList();
        }

        /* access modifiers changed from: package-private */
        public Builder addAll(Resolver... resolvers) {
            this.mResolvers.addAll(Arrays.asList(resolvers));
            return this;
        }

        /* access modifiers changed from: package-private */
        public Dependencies build() {
            return new Dependencies(this.mResolvers);
        }
    }

    private Dependencies(List<Resolver> resolvers) {
        for (Resolver resolver : resolvers) {
            addProviders(resolver);
        }
    }

    private void addProviders(Resolver another) {
        for (Map.Entry<Class<?>, Map<String, Provider>> entry : another.getProviders().entrySet()) {
            Map<String, Provider> localResolvers = getProviders().get(entry.getKey());
            if (localResolvers == null) {
                localResolvers = new HashMap<>();
                getProviders().put(entry.getKey(), localResolvers);
            }
            Set<String> intersection = new HashSet<>(localResolvers.keySet());
            if (!intersection.retainAll(entry.getValue().keySet())) {
                localResolvers.putAll(entry.getValue());
            } else {
                throw new IllegalArgumentException("Duplicate dependency found " + intersection);
            }
        }
    }

    /* access modifiers changed from: protected */
    public <T> T create(Class<T> cls, String tag) {
        T obj = super.create(cls, tag);
        injectDependencies(obj);
        return obj;
    }

    public void injectDependencies(Object target) {
        for (Field field : target.getClass().getDeclaredFields()) {
            Inject inject = (Inject) field.getAnnotation(Inject.class);
            Inject inject2 = inject;
            if (inject != null) {
                injectForField(target, field, inject2);
            }
        }
    }

    private void injectForField(Object target, Field field, Inject inject) {
        Object obj;
        String tag = inject.tag();
        if (Constants.DEBUG) {
            Log.i("Dependencies", "injecting " + field.getType() + " for " + target.getClass() + " with tag: " + tag);
        }
        try {
            field.setAccessible(true);
            if (tag.isEmpty()) {
                obj = Dependency.get(field.getType());
            } else {
                obj = get(field.getType(), tag);
            }
            if (Constants.DEBUG) {
                Log.i("Dependencies", "dependency found: " + obj);
            }
            field.set(target, obj);
        } catch (IllegalAccessException e) {
            Log.e("Dependencies", "unable to inject " + field, e);
        }
    }

    public static void initialize(Resolver... resolvers) {
        sInstance = new Builder().addAll(resolvers).build();
    }

    public static Dependencies getInstance() {
        return sInstance;
    }
}
