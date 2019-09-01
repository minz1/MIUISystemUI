package com.android.keyguard.utils;

import android.content.Context;
import android.content.IContentProvider;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class ContentProviderUtils {
    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0022, code lost:
        if (r0 != null) goto L_0x0024;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0024, code lost:
        r8.getContentResolver().releaseUnstableProvider(r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0036, code lost:
        if (r0 == null) goto L_0x0039;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0039, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void updateData(android.content.Context r8, android.net.Uri r9, android.content.ContentValues r10) {
        /*
            r0 = 0
            android.content.ContentResolver r1 = r8.getContentResolver()     // Catch:{ Exception -> 0x002e }
            android.content.IContentProvider r1 = r1.acquireUnstableProvider(r9)     // Catch:{ Exception -> 0x002e }
            r0 = r1
            if (r0 != 0) goto L_0x0016
            if (r0 == 0) goto L_0x0015
            android.content.ContentResolver r1 = r8.getContentResolver()
            r1.releaseUnstableProvider(r0)
        L_0x0015:
            return
        L_0x0016:
            java.lang.String r3 = r8.getPackageName()     // Catch:{ Exception -> 0x002e }
            r6 = 0
            r7 = 0
            r2 = r0
            r4 = r9
            r5 = r10
            r2.update(r3, r4, r5, r6, r7)     // Catch:{ Exception -> 0x002e }
            if (r0 == 0) goto L_0x0039
        L_0x0024:
            android.content.ContentResolver r1 = r8.getContentResolver()
            r1.releaseUnstableProvider(r0)
            goto L_0x0039
        L_0x002c:
            r1 = move-exception
            goto L_0x003a
        L_0x002e:
            r1 = move-exception
            java.lang.String r2 = "ContentProviderUtils"
            java.lang.String r3 = "updateData"
            android.util.Log.d(r2, r3, r1)     // Catch:{ all -> 0x002c }
            if (r0 == 0) goto L_0x0039
            goto L_0x0024
        L_0x0039:
            return
        L_0x003a:
            if (r0 == 0) goto L_0x0043
            android.content.ContentResolver r2 = r8.getContentResolver()
            r2.releaseUnstableProvider(r0)
        L_0x0043:
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.utils.ContentProviderUtils.updateData(android.content.Context, android.net.Uri, android.content.ContentValues):void");
    }

    public static boolean isProviderExists(Context context, Uri uri) {
        IContentProvider provider = context.getContentResolver().acquireUnstableProvider(uri);
        if (provider == null) {
            return false;
        }
        context.getContentResolver().releaseUnstableProvider(provider);
        return true;
    }

    public static Bundle getResultFromProvider(Context context, String providerUri, String callMethod, String args, Bundle extras) {
        return getResultFromProvider(context, Uri.parse(providerUri), callMethod, args, extras);
    }

    public static Bundle getResultFromProvider(Context context, Uri providerUri, String callMethod, String args, Bundle extras) {
        IContentProvider provider = null;
        try {
            provider = context.getContentResolver().acquireUnstableProvider(providerUri);
            if (provider == null) {
                if (provider != null) {
                    context.getContentResolver().releaseUnstableProvider(provider);
                }
                return null;
            }
            Bundle result = provider.call(context.getPackageName(), callMethod, args, extras);
            if (provider != null) {
                context.getContentResolver().releaseUnstableProvider(provider);
            }
            return result;
        } catch (Exception e) {
            Log.e("ContentProviderUtils", "getResultFromProvider", e);
            if (provider != null) {
                context.getContentResolver().releaseUnstableProvider(provider);
            }
            return null;
        } catch (Throwable th) {
            if (provider != null) {
                context.getContentResolver().releaseUnstableProvider(provider);
            }
            throw th;
        }
    }
}
