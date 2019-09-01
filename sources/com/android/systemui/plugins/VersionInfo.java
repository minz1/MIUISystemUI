package com.android.systemui.plugins;

import android.util.ArrayMap;
import com.android.systemui.plugins.annotations.Dependencies;
import com.android.systemui.plugins.annotations.DependsOn;
import com.android.systemui.plugins.annotations.ProvidesInterface;
import com.android.systemui.plugins.annotations.Requirements;
import com.android.systemui.plugins.annotations.Requires;
import java.util.Iterator;
import java.util.Map;

public class VersionInfo {
    private Class<?> mDefault;
    private final ArrayMap<Class<?>, Version> mVersions = new ArrayMap<>();

    public static class InvalidVersionException extends RuntimeException {
        private final boolean mTooNew;

        public InvalidVersionException(String str, boolean tooNew) {
            super(str);
            this.mTooNew = tooNew;
        }

        public InvalidVersionException(Class<?> cls, boolean tooNew, int expected, int actual) {
            super(cls.getSimpleName() + " expected version " + expected + " but had " + actual);
            this.mTooNew = tooNew;
        }

        public boolean isTooNew() {
            return this.mTooNew;
        }
    }

    private static class Version {
        /* access modifiers changed from: private */
        public final boolean mRequired;
        /* access modifiers changed from: private */
        public final int mVersion;

        public Version(int version, boolean required) {
            this.mVersion = version;
            this.mRequired = required;
        }
    }

    public boolean hasVersionInfo() {
        return !this.mVersions.isEmpty();
    }

    public int getDefaultVersion() {
        return this.mVersions.get(this.mDefault).mVersion;
    }

    public VersionInfo addClass(Class<?> cls) {
        if (this.mDefault == null) {
            this.mDefault = cls;
        }
        addClass(cls, false);
        return this;
    }

    private void addClass(Class<?> cls, boolean required) {
        if (!this.mVersions.containsKey(cls)) {
            ProvidesInterface provider = (ProvidesInterface) cls.getDeclaredAnnotation(ProvidesInterface.class);
            if (provider != null) {
                this.mVersions.put(cls, new Version(provider.version(), true));
            }
            Requires requires = (Requires) cls.getDeclaredAnnotation(Requires.class);
            if (requires != null) {
                this.mVersions.put(requires.target(), new Version(requires.version(), required));
            }
            Requirements requirements = (Requirements) cls.getDeclaredAnnotation(Requirements.class);
            if (requirements != null) {
                for (Requires r : requirements.value()) {
                    this.mVersions.put(r.target(), new Version(r.version(), required));
                }
            }
            DependsOn depends = (DependsOn) cls.getDeclaredAnnotation(DependsOn.class);
            if (depends != null) {
                addClass(depends.target(), true);
            }
            Dependencies dependencies = (Dependencies) cls.getDeclaredAnnotation(Dependencies.class);
            if (dependencies != null) {
                for (DependsOn d : dependencies.value()) {
                    addClass(d.target(), true);
                }
            }
        }
    }

    public void checkVersion(VersionInfo plugin) throws InvalidVersionException {
        ArrayMap<Class<?>, Version> versions = new ArrayMap<>(this.mVersions);
        Iterator<Map.Entry<Class<?>, Version>> it = versions.entrySet().iterator();
        while (true) {
            boolean z = false;
            if (it.hasNext()) {
                Map.Entry<Class<?>, Version> entry = it.next();
                try {
                    Class<?> aClass = entry.getKey();
                    Version version = entry.getValue();
                    Version v = versions.remove(aClass);
                    if (v == null) {
                        v = createVersion(aClass);
                    }
                    if (v == null) {
                        throw new InvalidVersionException(aClass.getSimpleName() + " does not provide an interface", false);
                    } else if (v.mVersion != version.mVersion) {
                        if (v.mVersion < version.mVersion) {
                            z = true;
                        }
                        throw new InvalidVersionException(aClass, z, v.mVersion, version.mVersion);
                    }
                } catch (IllegalStateException e) {
                }
            } else {
                for (Map.Entry<Class<?>, Version> entry2 : versions.entrySet()) {
                    try {
                        Class<?> aClass2 = entry2.getKey();
                        if (entry2.getValue().mRequired) {
                            throw new InvalidVersionException("Missing required dependency " + aClass2.getSimpleName(), false);
                        }
                    } catch (IllegalStateException e2) {
                    }
                }
                return;
            }
        }
    }

    private Version createVersion(Class<?> cls) {
        ProvidesInterface provider = (ProvidesInterface) cls.getDeclaredAnnotation(ProvidesInterface.class);
        if (provider != null) {
            return new Version(provider.version(), false);
        }
        return null;
    }

    public <T> boolean hasClass(Class<T> cls) {
        return this.mVersions.containsKey(cls);
    }
}
