package com.miui.systemui.dependencies;

import java.util.Map;

public interface Resolver {
    <T> T get(Class<T> cls, String str);

    Map<Class<?>, Map<String, Provider>> getProviders();
}
