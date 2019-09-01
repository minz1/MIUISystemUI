package com.miui.internal.policy.impl.AwesomeLockScreenImp;

import java.io.InputStream;
import miui.content.res.ThemeResources;
import miui.maml.ResourceLoader;

public class LockScreenResourceLoader extends ResourceLoader {
    public boolean resourceExists(String path) {
        return ThemeResources.getSystem().containsAwesomeLockscreenEntry(path);
    }

    public InputStream getInputStream(String path, long[] size) {
        return ThemeResources.getSystem().getAwesomeLockscreenFileStream(path, size);
    }
}
