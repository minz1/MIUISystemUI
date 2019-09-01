package android.support.v4.content.res;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;

public final class ResourcesCompat {

    public static abstract class FontCallback {
        public abstract void onFontRetrievalFailed(int i);

        public abstract void onFontRetrieved(Typeface typeface);

        public final void callbackSuccessAsync(final Typeface typeface, Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrieved(typeface);
                }
            });
        }

        public final void callbackFailAsync(final int reason, Handler handler) {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(new Runnable() {
                public void run() {
                    FontCallback.this.onFontRetrievalFailed(reason);
                }
            });
        }
    }

    public static Drawable getDrawable(Resources res, int id, Resources.Theme theme) throws Resources.NotFoundException {
        if (Build.VERSION.SDK_INT >= 21) {
            return res.getDrawable(id, theme);
        }
        return res.getDrawable(id);
    }

    public static Typeface getFont(Context context, int id, TypedValue value, int style, FontCallback fontCallback) throws Resources.NotFoundException {
        if (context.isRestricted()) {
            return null;
        }
        return loadFont(context, id, value, style, fontCallback, null, true);
    }

    private static Typeface loadFont(Context context, int id, TypedValue value, int style, FontCallback fontCallback, Handler handler, boolean isRequestFromLayoutInflator) {
        Resources resources = context.getResources();
        resources.getValue(id, value, true);
        Typeface typeface = loadFont(context, resources, value, id, style, fontCallback, handler, isRequestFromLayoutInflator);
        if (typeface != null || fontCallback != null) {
            return typeface;
        }
        throw new Resources.NotFoundException("Font resource ID #0x" + Integer.toHexString(id) + " could not be retrieved.");
    }

    /* JADX WARNING: Removed duplicated region for block: B:64:0x00f1  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static android.graphics.Typeface loadFont(android.content.Context r20, android.content.res.Resources r21, android.util.TypedValue r22, int r23, int r24, android.support.v4.content.res.ResourcesCompat.FontCallback r25, android.os.Handler r26, boolean r27) {
        /*
            r9 = r21
            r10 = r22
            r11 = r23
            r12 = r24
            r13 = r25
            r14 = r26
            java.lang.CharSequence r0 = r10.string
            if (r0 == 0) goto L_0x00f5
            java.lang.CharSequence r0 = r10.string
            java.lang.String r15 = r0.toString()
            java.lang.String r0 = "res/"
            boolean r0 = r15.startsWith(r0)
            r16 = 0
            r8 = -3
            if (r0 != 0) goto L_0x0027
            if (r13 == 0) goto L_0x0026
            r13.callbackFailAsync(r8, r14)
        L_0x0026:
            return r16
        L_0x0027:
            android.graphics.Typeface r7 = android.support.v4.graphics.TypefaceCompat.findFromCache(r9, r11, r12)
            if (r7 == 0) goto L_0x0033
            if (r13 == 0) goto L_0x0032
            r13.callbackSuccessAsync(r7, r14)
        L_0x0032:
            return r7
        L_0x0033:
            java.lang.String r0 = r15.toLowerCase()     // Catch:{ XmlPullParserException -> 0x00d2, IOException -> 0x00b5 }
            java.lang.String r1 = ".xml"
            boolean r0 = r0.endsWith(r1)     // Catch:{ XmlPullParserException -> 0x00d2, IOException -> 0x00b5 }
            if (r0 == 0) goto L_0x0093
            android.content.res.XmlResourceParser r0 = r9.getXml(r11)     // Catch:{ XmlPullParserException -> 0x008c, IOException -> 0x0085 }
            android.support.v4.content.res.FontResourcesParserCompat$FamilyResourceEntry r1 = android.support.v4.content.res.FontResourcesParserCompat.parse(r0, r9)     // Catch:{ XmlPullParserException -> 0x008c, IOException -> 0x0085 }
            r17 = r1
            if (r17 != 0) goto L_0x0069
            java.lang.String r1 = "ResourcesCompat"
            java.lang.String r2 = "Failed to find font-family tag"
            android.util.Log.e(r1, r2)     // Catch:{ XmlPullParserException -> 0x0061, IOException -> 0x0059 }
            if (r13 == 0) goto L_0x0058
            r13.callbackFailAsync(r8, r14)     // Catch:{ XmlPullParserException -> 0x0061, IOException -> 0x0059 }
        L_0x0058:
            return r16
        L_0x0059:
            r0 = move-exception
            r1 = r20
            r18 = r7
            r10 = r8
            goto L_0x00bb
        L_0x0061:
            r0 = move-exception
            r1 = r20
            r18 = r7
            r10 = r8
            goto L_0x00d8
        L_0x0069:
            r1 = r20
            r2 = r17
            r3 = r9
            r4 = r11
            r5 = r12
            r6 = r13
            r18 = r7
            r7 = r14
            r10 = r8
            r8 = r27
            android.graphics.Typeface r1 = android.support.v4.graphics.TypefaceCompat.createFromResourcesFamilyXml(r1, r2, r3, r4, r5, r6, r7, r8)     // Catch:{ XmlPullParserException -> 0x0080, IOException -> 0x007c }
            return r1
        L_0x007c:
            r0 = move-exception
            r1 = r20
            goto L_0x00bb
        L_0x0080:
            r0 = move-exception
            r1 = r20
            goto L_0x00d8
        L_0x0085:
            r0 = move-exception
            r18 = r7
            r10 = r8
            r1 = r20
            goto L_0x00bb
        L_0x008c:
            r0 = move-exception
            r18 = r7
            r10 = r8
            r1 = r20
            goto L_0x00d8
        L_0x0093:
            r18 = r7
            r10 = r8
            r1 = r20
            android.graphics.Typeface r0 = android.support.v4.graphics.TypefaceCompat.createFromResourcesFontFile(r1, r9, r11, r15, r12)     // Catch:{ XmlPullParserException -> 0x00b3, IOException -> 0x00b1 }
            r7 = r0
            if (r13 == 0) goto L_0x00b0
            if (r7 == 0) goto L_0x00ad
            r13.callbackSuccessAsync(r7, r14)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x00a5 }
            goto L_0x00b0
        L_0x00a5:
            r0 = move-exception
            r18 = r7
            goto L_0x00bb
        L_0x00a9:
            r0 = move-exception
            r18 = r7
            goto L_0x00d8
        L_0x00ad:
            r13.callbackFailAsync(r10, r14)     // Catch:{ XmlPullParserException -> 0x00a9, IOException -> 0x00a5 }
        L_0x00b0:
            return r7
        L_0x00b1:
            r0 = move-exception
            goto L_0x00bb
        L_0x00b3:
            r0 = move-exception
            goto L_0x00d8
        L_0x00b5:
            r0 = move-exception
            r1 = r20
            r18 = r7
            r10 = r8
        L_0x00bb:
            java.lang.String r2 = "ResourcesCompat"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "Failed to read xml resource "
            r3.append(r4)
            r3.append(r15)
            java.lang.String r3 = r3.toString()
            android.util.Log.e(r2, r3, r0)
            goto L_0x00ef
        L_0x00d2:
            r0 = move-exception
            r1 = r20
            r18 = r7
            r10 = r8
        L_0x00d8:
            java.lang.String r2 = "ResourcesCompat"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "Failed to parse xml resource "
            r3.append(r4)
            r3.append(r15)
            java.lang.String r3 = r3.toString()
            android.util.Log.e(r2, r3, r0)
        L_0x00ef:
            if (r13 == 0) goto L_0x00f4
            r13.callbackFailAsync(r10, r14)
        L_0x00f4:
            return r16
        L_0x00f5:
            r1 = r20
            android.content.res.Resources$NotFoundException r0 = new android.content.res.Resources$NotFoundException
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "Resource \""
            r2.append(r3)
            java.lang.String r3 = r9.getResourceName(r11)
            r2.append(r3)
            java.lang.String r3 = "\" ("
            r2.append(r3)
            java.lang.String r3 = java.lang.Integer.toHexString(r23)
            r2.append(r3)
            java.lang.String r3 = ") is not a Font: "
            r2.append(r3)
            r3 = r22
            r2.append(r3)
            java.lang.String r2 = r2.toString()
            r0.<init>(r2)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.content.res.ResourcesCompat.loadFont(android.content.Context, android.content.res.Resources, android.util.TypedValue, int, int, android.support.v4.content.res.ResourcesCompat$FontCallback, android.os.Handler, boolean):android.graphics.Typeface");
    }
}
