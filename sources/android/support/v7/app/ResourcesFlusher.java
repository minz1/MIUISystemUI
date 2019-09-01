package android.support.v7.app;

import android.content.res.Resources;
import android.os.Build;
import android.support.v4.os.BuildCompat;
import android.util.Log;
import android.util.LongSparseArray;
import java.lang.reflect.Field;
import java.util.Map;

class ResourcesFlusher {
    private static Field sDrawableCacheField;
    private static boolean sDrawableCacheFieldFetched;
    private static Field sResourcesImplField;
    private static boolean sResourcesImplFieldFetched;
    private static Class sThemedResourceCacheClazz;
    private static boolean sThemedResourceCacheClazzFetched;
    private static Field sThemedResourceCache_mUnthemedEntriesField;
    private static boolean sThemedResourceCache_mUnthemedEntriesFieldFetched;

    static void flush(Resources resources) {
        if (!BuildCompat.isAtLeastP()) {
            if (Build.VERSION.SDK_INT >= 24) {
                flushNougats(resources);
            } else if (Build.VERSION.SDK_INT >= 23) {
                flushMarshmallows(resources);
            } else if (Build.VERSION.SDK_INT >= 21) {
                flushLollipops(resources);
            }
        }
    }

    private static void flushLollipops(Resources resources) {
        if (!sDrawableCacheFieldFetched) {
            try {
                sDrawableCacheField = Resources.class.getDeclaredField("mDrawableCache");
                sDrawableCacheField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.e("ResourcesFlusher", "Could not retrieve Resources#mDrawableCache field", e);
            }
            sDrawableCacheFieldFetched = true;
        }
        if (sDrawableCacheField != null) {
            Map drawableCache = null;
            try {
                drawableCache = (Map) sDrawableCacheField.get(resources);
            } catch (IllegalAccessException e2) {
                Log.e("ResourcesFlusher", "Could not retrieve value from Resources#mDrawableCache", e2);
            }
            if (drawableCache != null) {
                drawableCache.clear();
            }
        }
    }

    private static void flushMarshmallows(Resources resources) {
        if (!sDrawableCacheFieldFetched) {
            try {
                sDrawableCacheField = Resources.class.getDeclaredField("mDrawableCache");
                sDrawableCacheField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.e("ResourcesFlusher", "Could not retrieve Resources#mDrawableCache field", e);
            }
            sDrawableCacheFieldFetched = true;
        }
        Object drawableCache = null;
        if (sDrawableCacheField != null) {
            try {
                drawableCache = sDrawableCacheField.get(resources);
            } catch (IllegalAccessException e2) {
                Log.e("ResourcesFlusher", "Could not retrieve value from Resources#mDrawableCache", e2);
            }
        }
        if (drawableCache != null) {
            flushThemedResourcesCache(drawableCache);
        }
    }

    private static void flushNougats(Resources resources) {
        if (!sResourcesImplFieldFetched) {
            try {
                sResourcesImplField = Resources.class.getDeclaredField("mResourcesImpl");
                sResourcesImplField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.e("ResourcesFlusher", "Could not retrieve Resources#mResourcesImpl field", e);
            }
            sResourcesImplFieldFetched = true;
        }
        if (sResourcesImplField != null) {
            Object resourcesImpl = null;
            try {
                resourcesImpl = sResourcesImplField.get(resources);
            } catch (IllegalAccessException e2) {
                Log.e("ResourcesFlusher", "Could not retrieve value from Resources#mResourcesImpl", e2);
            }
            if (resourcesImpl != null) {
                if (!sDrawableCacheFieldFetched) {
                    try {
                        sDrawableCacheField = resourcesImpl.getClass().getDeclaredField("mDrawableCache");
                        sDrawableCacheField.setAccessible(true);
                    } catch (NoSuchFieldException e3) {
                        Log.e("ResourcesFlusher", "Could not retrieve ResourcesImpl#mDrawableCache field", e3);
                    }
                    sDrawableCacheFieldFetched = true;
                }
                Object drawableCache = null;
                if (sDrawableCacheField != null) {
                    try {
                        drawableCache = sDrawableCacheField.get(resourcesImpl);
                    } catch (IllegalAccessException e4) {
                        Log.e("ResourcesFlusher", "Could not retrieve value from ResourcesImpl#mDrawableCache", e4);
                    }
                }
                if (drawableCache != null) {
                    flushThemedResourcesCache(drawableCache);
                }
            }
        }
    }

    private static void flushThemedResourcesCache(Object cache) {
        if (!sThemedResourceCacheClazzFetched) {
            try {
                sThemedResourceCacheClazz = Class.forName("android.content.res.ThemedResourceCache");
            } catch (ClassNotFoundException e) {
                Log.e("ResourcesFlusher", "Could not find ThemedResourceCache class", e);
            }
            sThemedResourceCacheClazzFetched = true;
        }
        if (sThemedResourceCacheClazz != null) {
            if (!sThemedResourceCache_mUnthemedEntriesFieldFetched) {
                try {
                    sThemedResourceCache_mUnthemedEntriesField = sThemedResourceCacheClazz.getDeclaredField("mUnthemedEntries");
                    sThemedResourceCache_mUnthemedEntriesField.setAccessible(true);
                } catch (NoSuchFieldException ee) {
                    Log.e("ResourcesFlusher", "Could not retrieve ThemedResourceCache#mUnthemedEntries field", ee);
                }
                sThemedResourceCache_mUnthemedEntriesFieldFetched = true;
            }
            if (sThemedResourceCache_mUnthemedEntriesField != null) {
                LongSparseArray unthemedEntries = null;
                try {
                    unthemedEntries = (LongSparseArray) sThemedResourceCache_mUnthemedEntriesField.get(cache);
                } catch (IllegalAccessException e2) {
                    Log.e("ResourcesFlusher", "Could not retrieve value from ThemedResourceCache#mUnthemedEntries", e2);
                }
                if (unthemedEntries != null) {
                    unthemedEntries.clear();
                }
            }
        }
    }
}
