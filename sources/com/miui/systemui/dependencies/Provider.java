package com.miui.systemui.dependencies;

public interface Provider<T> {
    T create(Resolver resolver);
}
