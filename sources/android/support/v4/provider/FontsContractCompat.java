package android.support.v4.provider;

import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.support.v4.graphics.TypefaceCompat;
import android.support.v4.graphics.TypefaceCompatUtil;
import android.support.v4.provider.SelfDestructiveThread;
import android.support.v4.util.LruCache;
import android.support.v4.util.Preconditions;
import android.support.v4.util.SimpleArrayMap;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FontsContractCompat {
    private static final SelfDestructiveThread sBackgroundThread = new SelfDestructiveThread("fonts", 10, 10000);
    private static final Comparator<byte[]> sByteArrayComparator = new Comparator<byte[]>() {
        public int compare(byte[] l, byte[] r) {
            if (l.length != r.length) {
                return l.length - r.length;
            }
            for (int i = 0; i < l.length; i++) {
                if (l[i] != r[i]) {
                    return l[i] - r[i];
                }
            }
            return 0;
        }
    };
    static final Object sLock = new Object();
    static final SimpleArrayMap<String, ArrayList<SelfDestructiveThread.ReplyCallback<TypefaceResult>>> sPendingReplies = new SimpleArrayMap<>();
    static final LruCache<String, Typeface> sTypefaceCache = new LruCache<>(16);

    public static class FontFamilyResult {
        private final FontInfo[] mFonts;
        private final int mStatusCode;

        public FontFamilyResult(int statusCode, FontInfo[] fonts) {
            this.mStatusCode = statusCode;
            this.mFonts = fonts;
        }

        public int getStatusCode() {
            return this.mStatusCode;
        }

        public FontInfo[] getFonts() {
            return this.mFonts;
        }
    }

    public static class FontInfo {
        private final boolean mItalic;
        private final int mResultCode;
        private final int mTtcIndex;
        private final Uri mUri;
        private final int mWeight;

        public FontInfo(Uri uri, int ttcIndex, int weight, boolean italic, int resultCode) {
            this.mUri = (Uri) Preconditions.checkNotNull(uri);
            this.mTtcIndex = ttcIndex;
            this.mWeight = weight;
            this.mItalic = italic;
            this.mResultCode = resultCode;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public int getTtcIndex() {
            return this.mTtcIndex;
        }

        public int getWeight() {
            return this.mWeight;
        }

        public boolean isItalic() {
            return this.mItalic;
        }

        public int getResultCode() {
            return this.mResultCode;
        }
    }

    private static final class TypefaceResult {
        final int mResult;
        final Typeface mTypeface;

        TypefaceResult(Typeface typeface, int result) {
            this.mTypeface = typeface;
            this.mResult = result;
        }
    }

    static TypefaceResult getFontInternal(Context context, FontRequest request, int style) {
        try {
            FontFamilyResult result = fetchFonts(context, null, request);
            int resultCode = -3;
            if (result.getStatusCode() == 0) {
                Typeface typeface = TypefaceCompat.createFromFontInfo(context, null, result.getFonts(), style);
                if (typeface != null) {
                    resultCode = 0;
                }
                return new TypefaceResult(typeface, resultCode);
            }
            if (result.getStatusCode() == 1) {
                resultCode = -2;
            }
            return new TypefaceResult(null, resultCode);
        } catch (PackageManager.NameNotFoundException e) {
            return new TypefaceResult(null, -1);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:34:0x007c, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x008d, code lost:
        sBackgroundThread.postAndReply(r2, new android.support.v4.provider.FontsContractCompat.AnonymousClass3());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x0097, code lost:
        return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static android.graphics.Typeface getFontSync(final android.content.Context r8, final android.support.v4.provider.FontRequest r9, final android.support.v4.content.res.ResourcesCompat.FontCallback r10, final android.os.Handler r11, boolean r12, int r13, final int r14) {
        /*
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.String r1 = r9.getIdentifier()
            r0.append(r1)
            java.lang.String r1 = "-"
            r0.append(r1)
            r0.append(r14)
            java.lang.String r0 = r0.toString()
            android.support.v4.util.LruCache<java.lang.String, android.graphics.Typeface> r1 = sTypefaceCache
            java.lang.Object r1 = r1.get(r0)
            android.graphics.Typeface r1 = (android.graphics.Typeface) r1
            if (r1 == 0) goto L_0x0028
            if (r10 == 0) goto L_0x0027
            r10.onFontRetrieved(r1)
        L_0x0027:
            return r1
        L_0x0028:
            if (r12 == 0) goto L_0x0045
            r2 = -1
            if (r13 != r2) goto L_0x0045
            android.support.v4.provider.FontsContractCompat$TypefaceResult r2 = getFontInternal(r8, r9, r14)
            if (r10 == 0) goto L_0x0042
            int r3 = r2.mResult
            if (r3 != 0) goto L_0x003d
            android.graphics.Typeface r3 = r2.mTypeface
            r10.callbackSuccessAsync(r3, r11)
            goto L_0x0042
        L_0x003d:
            int r3 = r2.mResult
            r10.callbackFailAsync(r3, r11)
        L_0x0042:
            android.graphics.Typeface r3 = r2.mTypeface
            return r3
        L_0x0045:
            android.support.v4.provider.FontsContractCompat$1 r2 = new android.support.v4.provider.FontsContractCompat$1
            r2.<init>(r8, r9, r14, r0)
            r3 = 0
            if (r12 == 0) goto L_0x005a
            android.support.v4.provider.SelfDestructiveThread r4 = sBackgroundThread     // Catch:{ InterruptedException -> 0x0058 }
            java.lang.Object r4 = r4.postAndWait(r2, r13)     // Catch:{ InterruptedException -> 0x0058 }
            android.support.v4.provider.FontsContractCompat$TypefaceResult r4 = (android.support.v4.provider.FontsContractCompat.TypefaceResult) r4     // Catch:{ InterruptedException -> 0x0058 }
            android.graphics.Typeface r4 = r4.mTypeface     // Catch:{ InterruptedException -> 0x0058 }
            return r4
        L_0x0058:
            r4 = move-exception
            return r3
        L_0x005a:
            if (r10 != 0) goto L_0x005e
            r4 = r3
            goto L_0x0063
        L_0x005e:
            android.support.v4.provider.FontsContractCompat$2 r4 = new android.support.v4.provider.FontsContractCompat$2
            r4.<init>(r10, r11)
        L_0x0063:
            java.lang.Object r5 = sLock
            monitor-enter(r5)
            android.support.v4.util.SimpleArrayMap<java.lang.String, java.util.ArrayList<android.support.v4.provider.SelfDestructiveThread$ReplyCallback<android.support.v4.provider.FontsContractCompat$TypefaceResult>>> r6 = sPendingReplies     // Catch:{ all -> 0x0098 }
            boolean r6 = r6.containsKey(r0)     // Catch:{ all -> 0x0098 }
            if (r6 == 0) goto L_0x007d
            if (r4 == 0) goto L_0x007b
            android.support.v4.util.SimpleArrayMap<java.lang.String, java.util.ArrayList<android.support.v4.provider.SelfDestructiveThread$ReplyCallback<android.support.v4.provider.FontsContractCompat$TypefaceResult>>> r6 = sPendingReplies     // Catch:{ all -> 0x0098 }
            java.lang.Object r6 = r6.get(r0)     // Catch:{ all -> 0x0098 }
            java.util.ArrayList r6 = (java.util.ArrayList) r6     // Catch:{ all -> 0x0098 }
            r6.add(r4)     // Catch:{ all -> 0x0098 }
        L_0x007b:
            monitor-exit(r5)     // Catch:{ all -> 0x0098 }
            return r3
        L_0x007d:
            if (r4 == 0) goto L_0x008c
            java.util.ArrayList r6 = new java.util.ArrayList     // Catch:{ all -> 0x0098 }
            r6.<init>()     // Catch:{ all -> 0x0098 }
            r6.add(r4)     // Catch:{ all -> 0x0098 }
            android.support.v4.util.SimpleArrayMap<java.lang.String, java.util.ArrayList<android.support.v4.provider.SelfDestructiveThread$ReplyCallback<android.support.v4.provider.FontsContractCompat$TypefaceResult>>> r7 = sPendingReplies     // Catch:{ all -> 0x0098 }
            r7.put(r0, r6)     // Catch:{ all -> 0x0098 }
        L_0x008c:
            monitor-exit(r5)     // Catch:{ all -> 0x0098 }
            android.support.v4.provider.SelfDestructiveThread r5 = sBackgroundThread
            android.support.v4.provider.FontsContractCompat$3 r6 = new android.support.v4.provider.FontsContractCompat$3
            r6.<init>(r0)
            r5.postAndReply(r2, r6)
            return r3
        L_0x0098:
            r3 = move-exception
            monitor-exit(r5)     // Catch:{ all -> 0x0098 }
            throw r3
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.provider.FontsContractCompat.getFontSync(android.content.Context, android.support.v4.provider.FontRequest, android.support.v4.content.res.ResourcesCompat$FontCallback, android.os.Handler, boolean, int, int):android.graphics.Typeface");
    }

    public static Map<Uri, ByteBuffer> prepareFontData(Context context, FontInfo[] fonts, CancellationSignal cancellationSignal) {
        HashMap<Uri, ByteBuffer> out = new HashMap<>();
        for (FontInfo font : fonts) {
            if (font.getResultCode() == 0) {
                Uri uri = font.getUri();
                if (!out.containsKey(uri)) {
                    out.put(uri, TypefaceCompatUtil.mmap(context, cancellationSignal, uri));
                }
            }
        }
        return Collections.unmodifiableMap(out);
    }

    public static FontFamilyResult fetchFonts(Context context, CancellationSignal cancellationSignal, FontRequest request) throws PackageManager.NameNotFoundException {
        ProviderInfo providerInfo = getProvider(context.getPackageManager(), request, context.getResources());
        if (providerInfo == null) {
            return new FontFamilyResult(1, null);
        }
        return new FontFamilyResult(0, getFontFromProvider(context, request, providerInfo.authority, cancellationSignal));
    }

    public static ProviderInfo getProvider(PackageManager packageManager, FontRequest request, Resources resources) throws PackageManager.NameNotFoundException {
        String providerAuthority = request.getProviderAuthority();
        ProviderInfo info = packageManager.resolveContentProvider(providerAuthority, 0);
        if (info == null) {
            throw new PackageManager.NameNotFoundException("No package found for authority: " + providerAuthority);
        } else if (info.packageName.equals(request.getProviderPackage())) {
            List<byte[]> signatures = convertToByteArrayList(packageManager.getPackageInfo(info.packageName, 64).signatures);
            Collections.sort(signatures, sByteArrayComparator);
            List<List<byte[]>> requestCertificatesList = getCertificates(request, resources);
            for (int i = 0; i < requestCertificatesList.size(); i++) {
                List<byte[]> requestSignatures = new ArrayList<>(requestCertificatesList.get(i));
                Collections.sort(requestSignatures, sByteArrayComparator);
                if (equalsByteArrayList(signatures, requestSignatures)) {
                    return info;
                }
            }
            return null;
        } else {
            throw new PackageManager.NameNotFoundException("Found content provider " + providerAuthority + ", but package was not " + request.getProviderPackage());
        }
    }

    private static List<List<byte[]>> getCertificates(FontRequest request, Resources resources) {
        if (request.getCertificates() != null) {
            return request.getCertificates();
        }
        return FontResourcesParserCompat.readCerts(resources, request.getCertificatesArrayResId());
    }

    private static boolean equalsByteArrayList(List<byte[]> signatures, List<byte[]> requestSignatures) {
        if (signatures.size() != requestSignatures.size()) {
            return false;
        }
        for (int i = 0; i < signatures.size(); i++) {
            if (!Arrays.equals(signatures.get(i), requestSignatures.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<byte[]> convertToByteArrayList(Signature[] signatures) {
        List<byte[]> shas = new ArrayList<>();
        for (Signature byteArray : signatures) {
            shas.add(byteArray.toByteArray());
        }
        return shas;
    }

    static FontInfo[] getFontFromProvider(Context context, FontRequest request, String authority, CancellationSignal cancellationSignal) {
        Cursor cursor;
        Uri fileUri;
        String str = authority;
        ArrayList<FontInfo> result = new ArrayList<>();
        Uri uri = new Uri.Builder().scheme("content").authority(str).build();
        Uri fileBaseUri = new Uri.Builder().scheme("content").authority(str).appendPath("file").build();
        Cursor cursor2 = null;
        try {
            int i = 0;
            if (Build.VERSION.SDK_INT > 16) {
                cursor = context.getContentResolver().query(uri, new String[]{"_id", "file_id", "font_ttc_index", "font_variation_settings", "font_weight", "font_italic", "result_code"}, "query = ?", new String[]{request.getQuery()}, null, cancellationSignal);
            } else {
                cursor = context.getContentResolver().query(uri, new String[]{"_id", "file_id", "font_ttc_index", "font_variation_settings", "font_weight", "font_italic", "result_code"}, "query = ?", new String[]{request.getQuery()}, null);
            }
            cursor2 = cursor;
            if (cursor2 != null && cursor2.getCount() > 0) {
                int resultCodeColumnIndex = cursor2.getColumnIndex("result_code");
                result = new ArrayList<>();
                int idColumnIndex = cursor2.getColumnIndex("_id");
                int fileIdColumnIndex = cursor2.getColumnIndex("file_id");
                int ttcIndexColumnIndex = cursor2.getColumnIndex("font_ttc_index");
                int weightColumnIndex = cursor2.getColumnIndex("font_weight");
                int italicColumnIndex = cursor2.getColumnIndex("font_italic");
                while (cursor2.moveToNext()) {
                    int resultCode = resultCodeColumnIndex != -1 ? cursor2.getInt(resultCodeColumnIndex) : i;
                    int ttcIndex = ttcIndexColumnIndex != -1 ? cursor2.getInt(ttcIndexColumnIndex) : i;
                    if (fileIdColumnIndex == -1) {
                        fileUri = ContentUris.withAppendedId(uri, cursor2.getLong(idColumnIndex));
                    } else {
                        fileUri = ContentUris.withAppendedId(fileBaseUri, cursor2.getLong(fileIdColumnIndex));
                    }
                    FontInfo fontInfo = new FontInfo(fileUri, ttcIndex, weightColumnIndex != -1 ? cursor2.getInt(weightColumnIndex) : 400, italicColumnIndex != -1 && cursor2.getInt(italicColumnIndex) == 1, resultCode);
                    result.add(fontInfo);
                    i = 0;
                }
            }
            return (FontInfo[]) result.toArray(new FontInfo[0]);
        } finally {
            if (cursor2 != null) {
                cursor2.close();
            }
        }
    }
}
