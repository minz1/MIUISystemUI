package com.xiaomi.mistatistic.sdk.controller;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.xiaomi.mistatistic.sdk.BuildSetting;
import com.xiaomi.mistatistic.sdk.CustomSettings;
import com.xiaomi.mistatistic.sdk.controller.d;
import com.xiaomi.xmsf.push.service.a;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/* compiled from: NetworkUtils */
public abstract class j {
    private static boolean a = true;

    /* compiled from: NetworkUtils */
    public static final class a extends FilterInputStream {
        private boolean a;

        public a(InputStream inputStream) {
            super(inputStream);
        }

        public int read(byte[] bArr, int i, int i2) throws IOException {
            if (!this.a) {
                int read = super.read(bArr, i, i2);
                if (read != -1) {
                    return read;
                }
            }
            this.a = true;
            return -1;
        }
    }

    /* compiled from: NetworkUtils */
    public interface b {
        void a(String str);
    }

    public static boolean a(Context context) {
        if (context != null) {
            try {
                NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    return activeNetworkInfo.isConnectedOrConnecting();
                }
            } catch (Exception e) {
                h.a("isNetworkConnected", (Throwable) e);
            }
        }
        return false;
    }

    public static boolean b(Context context) {
        boolean z = false;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
            if (connectivityManager == null) {
                return false;
            }
            try {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null) {
                    return false;
                }
                if (1 == activeNetworkInfo.getType()) {
                    z = true;
                }
                return z;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e2) {
            return false;
        }
    }

    public static String c(Context context) {
        if (b(context)) {
            return "WIFI";
        }
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
            if (connectivityManager == null) {
                return "";
            }
            try {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null) {
                    return "";
                }
                return (activeNetworkInfo.getTypeName() + "-" + activeNetworkInfo.getSubtypeName() + "-" + activeNetworkInfo.getExtraInfo()).toLowerCase();
            } catch (Exception e) {
                return "";
            }
        } catch (Exception e2) {
            return "";
        }
    }

    public static void d(Context context) {
        a = k.a(context, "pref_key_enable_network_connection", true);
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r7v6, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r7v7, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r7v11, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r7v15, resolved type: java.io.BufferedReader} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r7v19, resolved type: java.lang.String} */
    /* JADX WARNING: type inference failed for: r7v2, types: [java.io.BufferedReader] */
    /* JADX WARNING: type inference failed for: r7v8 */
    /* JADX WARNING: type inference failed for: r7v12 */
    /* JADX WARNING: type inference failed for: r7v17 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x00f7 A[SYNTHETIC, Splitter:B:46:0x00f7] */
    /* JADX WARNING: Removed duplicated region for block: B:51:0x00ff A[Catch:{ IOException -> 0x00fb }] */
    /* JADX WARNING: Unknown variable types count: 1 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void a(android.content.Context r6, java.lang.String r7, java.util.Map<java.lang.String, java.lang.String> r8, com.xiaomi.mistatistic.sdk.controller.j.b r9) throws java.io.IOException {
        /*
            boolean r0 = a
            if (r0 != 0) goto L_0x0011
            java.lang.String r6 = "NetworkUtils"
            java.lang.String r7 = "Network connection is disabled."
            com.xiaomi.mistatistic.sdk.controller.h.d(r6, r7)
            java.lang.String r6 = ""
            r9.a(r6)
            return
        L_0x0011:
            boolean r0 = android.text.TextUtils.isEmpty(r7)
            if (r0 != 0) goto L_0x0106
            r0 = 0
            java.net.URL r1 = new java.net.URL     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.String r2 = com.xiaomi.mistatistic.sdk.controller.q.a((android.content.Context) r6, (java.lang.String) r7)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            r1.<init>(r2)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.net.HttpURLConnection r6 = a(r6, r1)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            r1 = 10000(0x2710, float:1.4013E-41)
            r6.setConnectTimeout(r1)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            r1 = 15000(0x3a98, float:2.102E-41)
            r6.setReadTimeout(r1)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.String r1 = "POST"
            r6.setRequestMethod(r1)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.String r1 = "doHttpPost:"
            java.lang.String r2 = "paramsMap:%s"
            r3 = 1
            java.lang.Object[] r4 = new java.lang.Object[r3]     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            r5 = 0
            r4[r5] = r8     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.String r2 = java.lang.String.format(r2, r4)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            com.xiaomi.mistatistic.sdk.controller.h.b(r1, r2)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            a((java.util.Map<java.lang.String, java.lang.String>) r8)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.String r8 = b((java.util.Map<java.lang.String, java.lang.String>) r8)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            if (r8 == 0) goto L_0x00d0
            r6.setDoOutput(r3)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            byte[] r8 = r8.getBytes()     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.io.OutputStream r1 = r6.getOutputStream()     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            int r2 = r8.length     // Catch:{ IOException -> 0x00cc, Throwable -> 0x00c8, all -> 0x00c4 }
            r1.write(r8, r5, r2)     // Catch:{ IOException -> 0x00cc, Throwable -> 0x00c8, all -> 0x00c4 }
            r1.flush()     // Catch:{ IOException -> 0x00cc, Throwable -> 0x00c8, all -> 0x00c4 }
            r1.close()     // Catch:{ IOException -> 0x00cc, Throwable -> 0x00c8, all -> 0x00c4 }
            int r8 = r6.getResponseCode()     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.String r1 = "doHttpPost:"
            java.lang.String r2 = "url:%s,responseCode:%d"
            r4 = 2
            java.lang.Object[] r4 = new java.lang.Object[r4]     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            r4[r5] = r7     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.Integer r7 = java.lang.Integer.valueOf(r8)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            r4[r3] = r7     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.String r7 = java.lang.String.format(r2, r4)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            com.xiaomi.mistatistic.sdk.controller.h.b(r1, r7)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.io.BufferedReader r7 = new java.io.BufferedReader     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.io.InputStreamReader r8 = new java.io.InputStreamReader     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            com.xiaomi.mistatistic.sdk.controller.j$a r1 = new com.xiaomi.mistatistic.sdk.controller.j$a     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.io.InputStream r6 = r6.getInputStream()     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            r1.<init>(r6)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            r8.<init>(r1)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            r7.<init>(r8)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.String r6 = r7.readLine()     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
            java.lang.StringBuffer r8 = new java.lang.StringBuffer     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
            r8.<init>()     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
            java.lang.String r1 = "line.separator"
            java.lang.String r1 = java.lang.System.getProperty(r1)     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
        L_0x00a4:
            if (r6 == 0) goto L_0x00b1
            r8.append(r6)     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
            r8.append(r1)     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
            java.lang.String r6 = r7.readLine()     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
            goto L_0x00a4
        L_0x00b1:
            java.lang.String r6 = r8.toString()     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
            r9.a(r6)     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
            r7.close()     // Catch:{ IOException -> 0x00c2, Throwable -> 0x00c0 }
            return
        L_0x00c0:
            r6 = move-exception
            goto L_0x00dd
        L_0x00c2:
            r6 = move-exception
            goto L_0x00ee
        L_0x00c4:
            r6 = move-exception
            r7 = r0
            r0 = r1
            goto L_0x00f5
        L_0x00c8:
            r6 = move-exception
            r7 = r0
            r0 = r1
            goto L_0x00dd
        L_0x00cc:
            r6 = move-exception
            r7 = r0
            r0 = r1
            goto L_0x00ee
        L_0x00d0:
            java.lang.IllegalArgumentException r6 = new java.lang.IllegalArgumentException     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            java.lang.String r7 = "nameValuePairs"
            r6.<init>(r7)     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
            throw r6     // Catch:{ IOException -> 0x00ec, Throwable -> 0x00db, all -> 0x00d8 }
        L_0x00d8:
            r6 = move-exception
            r7 = r0
            goto L_0x00f5
        L_0x00db:
            r6 = move-exception
            r7 = r0
        L_0x00dd:
            java.lang.String r8 = "doHttpPost Throwable:"
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r8, (java.lang.Throwable) r6)     // Catch:{ all -> 0x00f4 }
            java.io.IOException r8 = new java.io.IOException     // Catch:{ all -> 0x00f4 }
            java.lang.String r6 = r6.getMessage()     // Catch:{ all -> 0x00f4 }
            r8.<init>(r6)     // Catch:{ all -> 0x00f4 }
            throw r8     // Catch:{ all -> 0x00f4 }
        L_0x00ec:
            r6 = move-exception
            r7 = r0
        L_0x00ee:
            java.lang.String r8 = "doHttpPost IOException:"
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r8, (java.lang.Throwable) r6)     // Catch:{ all -> 0x00f4 }
            throw r6     // Catch:{ all -> 0x00f4 }
        L_0x00f4:
            r6 = move-exception
        L_0x00f5:
            if (r0 == 0) goto L_0x00fd
            r0.close()     // Catch:{ IOException -> 0x00fb }
            goto L_0x00fd
        L_0x00fb:
            r7 = move-exception
            goto L_0x0103
        L_0x00fd:
            if (r7 == 0) goto L_0x0104
            r7.close()     // Catch:{ IOException -> 0x00fb }
            goto L_0x0104
        L_0x0103:
            goto L_0x0105
        L_0x0104:
        L_0x0105:
            throw r6
        L_0x0106:
            java.lang.IllegalArgumentException r6 = new java.lang.IllegalArgumentException
            java.lang.String r7 = "url"
            r6.<init>(r7)
            throw r6
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.controller.j.a(android.content.Context, java.lang.String, java.util.Map, com.xiaomi.mistatistic.sdk.controller.j$b):void");
    }

    protected static void a(Map<String, String> map) {
        try {
            StringBuilder sb = new StringBuilder();
            new HashMap();
            if (map != null) {
                ArrayList<String> arrayList = new ArrayList<>(map.keySet());
                Collections.sort(arrayList);
                if (!arrayList.isEmpty()) {
                    for (String str : arrayList) {
                        sb.append(str + map.get(str));
                    }
                }
            }
            sb.append("mistats_sdkconfig_jafej!@#)(*e@!#");
            map.put("sign", q.b(sb.toString()).toLowerCase(Locale.getDefault()));
        } catch (Exception e) {
            h.a("sign exception:", (Throwable) e);
        }
    }

    public static String b(Map<String, String> map) {
        if (map == null || map.size() <= 0) {
            return null;
        }
        StringBuffer stringBuffer = new StringBuffer();
        for (Map.Entry next : map.entrySet()) {
            if (!(next.getKey() == null || next.getValue() == null)) {
                try {
                    stringBuffer.append(URLEncoder.encode((String) next.getKey(), "UTF-8"));
                    stringBuffer.append("=");
                    stringBuffer.append(URLEncoder.encode((String) next.getValue(), "UTF-8"));
                    stringBuffer.append("&");
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            }
        }
        if (stringBuffer.length() > 0) {
            stringBuffer = stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        }
        return stringBuffer.toString();
    }

    public static HttpURLConnection a(Context context, URL url) throws IOException {
        if (!"http".equals(url.getProtocol())) {
            return (HttpURLConnection) url.openConnection();
        }
        if (f(context)) {
            return (HttpURLConnection) url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("10.0.0.200", 80)));
        }
        if (!e(context)) {
            return (HttpURLConnection) url.openConnection();
        }
        String host = url.getHost();
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(a(url)).openConnection();
        httpURLConnection.addRequestProperty("X-Online-Host", host);
        return httpURLConnection;
    }

    public static String a(URL url) {
        StringBuilder sb = new StringBuilder();
        sb.append(url.getProtocol());
        sb.append("://");
        sb.append("10.0.0.172");
        sb.append(url.getPath());
        if (!TextUtils.isEmpty(url.getQuery())) {
            sb.append("?");
            sb.append(url.getQuery());
        }
        return sb.toString();
    }

    public static boolean e(Context context) {
        if (!"CN".equalsIgnoreCase(((TelephonyManager) context.getSystemService("phone")).getSimCountryIso())) {
            return false;
        }
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
            if (connectivityManager == null) {
                return false;
            }
            try {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null) {
                    return false;
                }
                String extraInfo = activeNetworkInfo.getExtraInfo();
                if (TextUtils.isEmpty(extraInfo) || extraInfo.length() < 3 || extraInfo.contains("ctwap")) {
                    return false;
                }
                return extraInfo.regionMatches(true, extraInfo.length() - 3, "wap", 0, 3);
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e2) {
            return false;
        }
    }

    public static boolean f(Context context) {
        if (!"CN".equalsIgnoreCase(((TelephonyManager) context.getSystemService("phone")).getSimCountryIso())) {
            return false;
        }
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
            if (connectivityManager == null) {
                return false;
            }
            try {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null) {
                    return false;
                }
                String extraInfo = activeNetworkInfo.getExtraInfo();
                if (TextUtils.isEmpty(extraInfo) || extraInfo.length() < 3 || !extraInfo.contains("ctwap")) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e2) {
            return false;
        }
    }

    public static void a(String str, Map<String, String> map, b bVar) throws IOException {
        if (!CustomSettings.isDataUploadingEnabled()) {
            h.d("upload is disabled.");
            bVar.a(null);
            return;
        }
        if (map != null) {
            map.put("bc", BuildSetting.getMiuiBuildCode());
        }
        if (!CustomSettings.isUseSystemUploadingService()) {
            a(c.a(), str, map, bVar);
        } else if (a()) {
            try {
                b(c.a(), str, map, bVar);
            } catch (Exception e) {
                throw new IOException("exception thrown from IPC." + e.getMessage());
            }
        } else {
            bVar.a(null);
            h.a("Uploading via sys service, metered network connected, skip");
        }
    }

    @SuppressLint({"NewApi"})
    public static boolean a() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) c.a().getSystemService("connectivity");
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                if (activeNetworkInfo.getType() == 1) {
                    return true;
                }
                if (Build.VERSION.SDK_INT >= 16) {
                    return !connectivityManager.isActiveNetworkMetered();
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v1, resolved type: java.io.DataOutputStream} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v3, resolved type: java.util.Map<java.lang.String, java.lang.String>} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v5, resolved type: java.util.Map<java.lang.String, java.lang.String>} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v6, resolved type: java.util.Map<java.lang.String, java.lang.String>} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v7, resolved type: java.io.DataOutputStream} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v12, resolved type: java.io.DataOutputStream} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v18, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v19, resolved type: java.lang.String} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v23, resolved type: java.io.BufferedReader} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v16, resolved type: java.util.Map<java.lang.String, java.lang.String>} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v17, resolved type: java.io.DataOutputStream} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v18, resolved type: java.util.Map<java.lang.String, java.lang.String>} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v19, resolved type: java.util.Map<java.lang.String, java.lang.String>} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v20, resolved type: java.io.DataOutputStream} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v21, resolved type: java.util.Map<java.lang.String, java.lang.String>} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r6v22, resolved type: java.io.DataOutputStream} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v29, resolved type: java.lang.String} */
    /* JADX WARNING: type inference failed for: r0v2, types: [java.io.BufferedReader] */
    /* JADX WARNING: type inference failed for: r0v7 */
    /* JADX WARNING: type inference failed for: r0v15 */
    /* JADX WARNING: type inference failed for: r0v25 */
    /* JADX WARNING: type inference failed for: r0v28 */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x016e A[SYNTHETIC, Splitter:B:64:0x016e] */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0176 A[Catch:{ IOException -> 0x0172 }] */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x017b A[Catch:{ IOException -> 0x0172 }] */
    /* JADX WARNING: Unknown variable types count: 1 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String a(java.lang.String r5, java.util.Map<java.lang.String, java.lang.String> r6, java.io.File r7, java.lang.String r8) throws java.io.IOException {
        /*
            boolean r0 = r7.exists()
            r1 = 0
            if (r0 != 0) goto L_0x0008
            return r1
        L_0x0008:
            java.lang.String r0 = r7.getName()
            java.net.URL r2 = new java.net.URL     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            android.content.Context r3 = com.xiaomi.mistatistic.sdk.controller.c.a()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.lang.String r5 = com.xiaomi.mistatistic.sdk.controller.q.a((android.content.Context) r3, (java.lang.String) r5)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            r2.<init>(r5)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.net.URLConnection r5 = r2.openConnection()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.net.HttpURLConnection r5 = (java.net.HttpURLConnection) r5     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            r2 = 15000(0x3a98, float:2.102E-41)
            r5.setReadTimeout(r2)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            r2 = 10000(0x2710, float:1.4013E-41)
            r5.setConnectTimeout(r2)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            r2 = 1
            r5.setDoInput(r2)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            r5.setDoOutput(r2)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            r2 = 0
            r5.setUseCaches(r2)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.lang.String r3 = "POST"
            r5.setRequestMethod(r3)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.lang.String r3 = "Connection"
            java.lang.String r4 = "Keep-Alive"
            r5.setRequestProperty(r3, r4)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.lang.String r3 = "Content-Type"
            java.lang.String r4 = "multipart/form-data;boundary=*****"
            r5.setRequestProperty(r3, r4)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            if (r6 == 0) goto L_0x0074
            java.util.Set r6 = r6.entrySet()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.util.Iterator r6 = r6.iterator()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
        L_0x0058:
            boolean r3 = r6.hasNext()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            if (r3 == 0) goto L_0x0074
            java.lang.Object r3 = r6.next()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.util.Map$Entry r3 = (java.util.Map.Entry) r3     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.lang.Object r4 = r3.getKey()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.lang.String r4 = (java.lang.String) r4     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.lang.Object r3 = r3.getValue()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.lang.String r3 = (java.lang.String) r3     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            r5.setRequestProperty(r4, r3)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            goto L_0x0058
        L_0x0074:
            r6 = 77
            int r0 = r0.length()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            int r6 = r6 + r0
            long r3 = r7.length()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            int r0 = (int) r3     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            int r6 = r6 + r0
            int r0 = r8.length()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            int r6 = r6 + r0
            r5.setFixedLengthStreamingMode(r6)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.io.DataOutputStream r6 = new java.io.DataOutputStream     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.io.OutputStream r0 = r5.getOutputStream()     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            r6.<init>(r0)     // Catch:{ IOException -> 0x0167, Throwable -> 0x015a, all -> 0x0156 }
            java.lang.String r0 = "--*****\r\n"
            r6.writeBytes(r0)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            java.lang.StringBuilder r0 = new java.lang.StringBuilder     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            r0.<init>()     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            java.lang.String r3 = "Content-Disposition: form-data; name=\""
            r0.append(r3)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            r0.append(r8)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            java.lang.String r8 = "\";filename=\""
            r0.append(r8)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            java.lang.String r8 = r7.getName()     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            r0.append(r8)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            java.lang.String r8 = "\""
            r0.append(r8)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            java.lang.String r8 = "\r\n"
            r0.append(r8)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            java.lang.String r8 = r0.toString()     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            r6.writeBytes(r8)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            java.lang.String r8 = "\r\n"
            r6.writeBytes(r8)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            java.io.FileInputStream r8 = new java.io.FileInputStream     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            r8.<init>(r7)     // Catch:{ IOException -> 0x0153, Throwable -> 0x0150, all -> 0x014d }
            r7 = 1024(0x400, float:1.435E-42)
            byte[] r7 = new byte[r7]     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
        L_0x00d2:
            int r0 = r8.read(r7)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            r3 = -1
            if (r0 == r3) goto L_0x00e0
            r6.write(r7, r2, r0)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            r6.flush()     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            goto L_0x00d2
        L_0x00e0:
            java.lang.String r7 = "\r\n"
            r6.writeBytes(r7)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            java.lang.String r7 = "--"
            r6.writeBytes(r7)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            java.lang.String r7 = "*****"
            r6.writeBytes(r7)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            java.lang.String r7 = "--"
            r6.writeBytes(r7)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            java.lang.String r7 = "\r\n"
            r6.writeBytes(r7)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            r6.flush()     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            java.lang.StringBuffer r7 = new java.lang.StringBuffer     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            r7.<init>()     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            java.io.BufferedReader r0 = new java.io.BufferedReader     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            java.io.InputStreamReader r2 = new java.io.InputStreamReader     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            com.xiaomi.mistatistic.sdk.controller.j$a r3 = new com.xiaomi.mistatistic.sdk.controller.j$a     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            java.io.InputStream r5 = r5.getInputStream()     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            r3.<init>(r5)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            r2.<init>(r3)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
            r0.<init>(r2)     // Catch:{ IOException -> 0x0149, Throwable -> 0x0145, all -> 0x0141 }
        L_0x0114:
            java.lang.String r5 = r0.readLine()     // Catch:{ IOException -> 0x013f, Throwable -> 0x013d, all -> 0x013b }
            if (r5 == 0) goto L_0x011e
            r7.append(r5)     // Catch:{ IOException -> 0x013f, Throwable -> 0x013d, all -> 0x013b }
            goto L_0x0114
        L_0x011e:
            java.lang.String r5 = r7.toString()     // Catch:{ IOException -> 0x013f, Throwable -> 0x013d, all -> 0x013b }
            r8.close()     // Catch:{ IOException -> 0x0127 }
            goto L_0x0129
        L_0x0127:
            r6 = move-exception
            goto L_0x0132
        L_0x0129:
            r6.close()     // Catch:{ IOException -> 0x0127 }
            r0.close()     // Catch:{ IOException -> 0x0127 }
            goto L_0x0139
        L_0x0132:
            java.lang.String r7 = ""
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r7, (java.lang.Throwable) r6)
            goto L_0x013a
        L_0x0139:
        L_0x013a:
            return r5
        L_0x013b:
            r5 = move-exception
            goto L_0x0143
        L_0x013d:
            r5 = move-exception
            goto L_0x0147
        L_0x013f:
            r5 = move-exception
            goto L_0x014b
        L_0x0141:
            r5 = move-exception
            r0 = r1
        L_0x0143:
            r1 = r8
            goto L_0x016c
        L_0x0145:
            r5 = move-exception
            r0 = r1
        L_0x0147:
            r1 = r8
            goto L_0x015d
        L_0x0149:
            r5 = move-exception
            r0 = r1
        L_0x014b:
            r1 = r8
            goto L_0x016a
        L_0x014d:
            r5 = move-exception
            r0 = r1
            goto L_0x016c
        L_0x0150:
            r5 = move-exception
            r0 = r1
            goto L_0x015d
        L_0x0153:
            r5 = move-exception
            r0 = r1
            goto L_0x016a
        L_0x0156:
            r5 = move-exception
            r6 = r1
            r0 = r6
            goto L_0x016c
        L_0x015a:
            r5 = move-exception
            r6 = r1
            r0 = r6
        L_0x015d:
            java.io.IOException r7 = new java.io.IOException     // Catch:{ all -> 0x016b }
            java.lang.String r5 = r5.getMessage()     // Catch:{ all -> 0x016b }
            r7.<init>(r5)     // Catch:{ all -> 0x016b }
            throw r7     // Catch:{ all -> 0x016b }
        L_0x0167:
            r5 = move-exception
            r6 = r1
            r0 = r6
        L_0x016a:
            throw r5     // Catch:{ all -> 0x016b }
        L_0x016b:
            r5 = move-exception
        L_0x016c:
            if (r1 == 0) goto L_0x0174
            r1.close()     // Catch:{ IOException -> 0x0172 }
            goto L_0x0174
        L_0x0172:
            r6 = move-exception
            goto L_0x017f
        L_0x0174:
            if (r6 == 0) goto L_0x0179
            r6.close()     // Catch:{ IOException -> 0x0172 }
        L_0x0179:
            if (r0 == 0) goto L_0x0186
            r0.close()     // Catch:{ IOException -> 0x0172 }
            goto L_0x0186
        L_0x017f:
            java.lang.String r7 = ""
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r7, (java.lang.Throwable) r6)
            goto L_0x0187
        L_0x0186:
        L_0x0187:
            throw r5
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.controller.j.a(java.lang.String, java.util.Map, java.io.File, java.lang.String):java.lang.String");
    }

    public static void b(final Context context, final String str, final Map<String, String> map, final b bVar) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.xiaomi.xmsf", "com.xiaomi.xmsf.push.service.HttpService");
            if (!context.bindService(intent, new ServiceConnection() {
                /* access modifiers changed from: private */
                public boolean e = false;

                public void onServiceDisconnected(ComponentName componentName) {
                    h.a("error while perform IPC connection.", (Throwable) null);
                    if (!this.e) {
                        bVar.a(null);
                        h.a("disconnected, remote http post hasn't not processed");
                    }
                }

                public void onServiceConnected(ComponentName componentName, final IBinder iBinder) {
                    d.a().a((d.a) new d.a() {
                        public void a() {
                            try {
                                bVar.a(a.C0003a.a(iBinder).a(q.a(context, str), map));
                                boolean unused = AnonymousClass1.this.e = true;
                                h.a("connected, do remote http post");
                                context.unbindService(this);
                            } catch (Throwable th) {
                                h.a("error while uploading the logs by IPC.", th);
                                bVar.a(null);
                                boolean unused2 = AnonymousClass1.this.e = true;
                            }
                        }
                    });
                }
            }, 1)) {
                h.a("failed to bind");
                bVar.a(null);
            }
        } catch (Exception e) {
            h.a("uploadDataThroughSystemService", (Throwable) e);
            bVar.a(null);
        }
    }
}
