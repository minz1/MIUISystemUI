package com.xiaomi.mistatistic.sdk.network;

import android.os.SystemClock;
import com.xiaomi.mistatistic.sdk.controller.g;
import com.xiaomi.mistatistic.sdk.data.HttpEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

/* compiled from: MIHttpsConnection */
public class d extends HttpsURLConnection {
    private String a = null;
    private long b;
    private long c;
    private long d;
    private int e = -1;
    private boolean f = false;
    private String g = null;
    private HttpsURLConnection h;

    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        try {
            return this.h.getPeerPrincipal();
        } catch (SSLPeerUnverifiedException e2) {
            a((Exception) e2);
            throw e2;
        }
    }

    public Principal getLocalPrincipal() {
        return this.h.getLocalPrincipal();
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.h.setHostnameVerifier(hostnameVerifier);
    }

    public HostnameVerifier getHostnameVerifier() {
        return this.h.getHostnameVerifier();
    }

    public void setSSLSocketFactory(SSLSocketFactory sSLSocketFactory) {
        this.h.setSSLSocketFactory(sSLSocketFactory);
    }

    public SSLSocketFactory getSSLSocketFactory() {
        return this.h.getSSLSocketFactory();
    }

    public InputStream getErrorStream() {
        return this.h.getErrorStream();
    }

    public Permission getPermission() throws IOException {
        try {
            return this.h.getPermission();
        } catch (IOException e2) {
            a((Exception) e2);
            throw e2;
        }
    }

    public String getRequestMethod() {
        return this.h.getRequestMethod();
    }

    public int getResponseCode() throws IOException {
        try {
            f();
            this.e = this.h.getResponseCode();
            g();
            return this.e;
        } catch (IOException e2) {
            a((Exception) e2);
            throw e2;
        }
    }

    public String getResponseMessage() throws IOException {
        try {
            return this.h.getResponseMessage();
        } catch (IOException e2) {
            a((Exception) e2);
            throw e2;
        }
    }

    public void setRequestMethod(String str) throws ProtocolException {
        try {
            this.h.setRequestMethod(str);
        } catch (ProtocolException e2) {
            a((Exception) e2);
            throw e2;
        }
    }

    public String getContentEncoding() {
        return this.h.getContentEncoding();
    }

    public boolean getInstanceFollowRedirects() {
        return this.h.getInstanceFollowRedirects();
    }

    public void setInstanceFollowRedirects(boolean z) {
        this.h.setInstanceFollowRedirects(z);
    }

    public long getHeaderFieldDate(String str, long j) {
        return this.h.getHeaderFieldDate(str, j);
    }

    public void setFixedLengthStreamingMode(long j) {
        try {
            this.h.getClass().getMethod("setFixedLengthStreamingMode", new Class[]{Long.TYPE}).invoke(this.h, new Object[]{Long.valueOf(j)});
        } catch (Exception e2) {
            throw new UnsupportedOperationException(e2);
        }
    }

    public void setFixedLengthStreamingMode(int i) {
        this.h.setFixedLengthStreamingMode(i);
    }

    public void setChunkedStreamingMode(int i) {
        this.h.setChunkedStreamingMode(i);
    }

    public boolean getAllowUserInteraction() {
        return this.h.getAllowUserInteraction();
    }

    public Object getContent() throws IOException {
        return this.h.getContent();
    }

    public Object getContent(Class[] clsArr) throws IOException {
        try {
            return this.h.getContent(clsArr);
        } catch (IOException e2) {
            a((Exception) e2);
            throw e2;
        }
    }

    public int getContentLength() {
        return this.h.getContentLength();
    }

    public String getContentType() {
        return this.h.getContentType();
    }

    public long getDate() {
        return this.h.getDate();
    }

    public boolean getDefaultUseCaches() {
        return this.h.getDefaultUseCaches();
    }

    public boolean getDoInput() {
        return this.h.getDoInput();
    }

    public boolean getDoOutput() {
        return this.h.getDoOutput();
    }

    public long getExpiration() {
        return this.h.getExpiration();
    }

    public String getHeaderField(int i) {
        return this.h.getHeaderField(i);
    }

    public Map<String, List<String>> getHeaderFields() {
        return this.h.getHeaderFields();
    }

    public Map<String, List<String>> getRequestProperties() {
        return this.h.getRequestProperties();
    }

    public void addRequestProperty(String str, String str2) {
        this.h.addRequestProperty(str, str2);
    }

    public String getHeaderField(String str) {
        return this.h.getHeaderField(str);
    }

    public int getHeaderFieldInt(String str, int i) {
        return this.h.getHeaderFieldInt(str, i);
    }

    public String getHeaderFieldKey(int i) {
        return this.h.getHeaderFieldKey(i);
    }

    public long getIfModifiedSince() {
        return this.h.getIfModifiedSince();
    }

    public InputStream getInputStream() throws IOException {
        try {
            f();
            e eVar = new e(this, this.h.getInputStream());
            g();
            c();
            return eVar;
        } catch (IOException e2) {
            d();
            a((Exception) e2);
            throw e2;
        }
    }

    public long getLastModified() {
        return this.h.getLastModified();
    }

    public OutputStream getOutputStream() throws IOException {
        try {
            f();
            f fVar = new f(this, this.h.getOutputStream());
            g();
            c();
            return fVar;
        } catch (IOException e2) {
            d();
            a((Exception) e2);
            throw e2;
        }
    }

    public String getRequestProperty(String str) {
        return this.h.getRequestProperty(str);
    }

    public URL getURL() {
        return this.h.getURL();
    }

    public boolean getUseCaches() {
        return this.h.getUseCaches();
    }

    public void setAllowUserInteraction(boolean z) {
        this.h.setAllowUserInteraction(z);
    }

    public void setDefaultUseCaches(boolean z) {
        this.h.setDefaultUseCaches(z);
    }

    public void setDoInput(boolean z) {
        this.h.setDoInput(z);
    }

    public void setDoOutput(boolean z) {
        this.h.setDoOutput(z);
    }

    public void setIfModifiedSince(long j) {
        this.h.setIfModifiedSince(j);
    }

    public void setRequestProperty(String str, String str2) {
        if ("x-mistats-header".equals(str)) {
            this.a = str2;
        }
        this.h.setRequestProperty(str, str2);
    }

    public void setUseCaches(boolean z) {
        this.h.setUseCaches(z);
    }

    public void setConnectTimeout(int i) {
        this.h.setConnectTimeout(i);
    }

    public int getConnectTimeout() {
        return this.h.getConnectTimeout();
    }

    public void setReadTimeout(int i) {
        this.h.setReadTimeout(i);
    }

    public int getReadTimeout() {
        return this.h.getReadTimeout();
    }

    public String toString() {
        return this.h.toString();
    }

    public d(HttpsURLConnection httpsURLConnection) {
        super(httpsURLConnection.getURL());
        this.h = httpsURLConnection;
        this.b = SystemClock.elapsedRealtime();
    }

    public void a(long j) {
        this.b = j;
    }

    public void disconnect() {
        this.h.disconnect();
        b();
    }

    public boolean usingProxy() {
        return this.h.usingProxy();
    }

    public void connect() throws IOException {
        try {
            this.h.connect();
        } catch (IOException e2) {
            a((Exception) e2);
            throw e2;
        }
    }

    public String getCipherSuite() {
        return this.h.getCipherSuite();
    }

    public Certificate[] getLocalCertificates() {
        return this.h.getLocalCertificates();
    }

    public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
        try {
            return this.h.getServerCertificates();
        } catch (SSLPeerUnverifiedException e2) {
            a((Exception) e2);
            throw e2;
        }
    }

    public void a() {
        b();
    }

    public void b() {
        if (!this.f) {
            this.f = true;
            HttpEvent httpEvent = new HttpEvent(getURL().toString(), SystemClock.elapsedRealtime() - this.b, 0, e());
            httpEvent.setIp(this.g);
            httpEvent.setFirstPacketCost(this.d);
            httpEvent.setRequestId(this.a);
            g.a().a(httpEvent);
        }
    }

    /* access modifiers changed from: package-private */
    public void a(Exception exc) {
        if (!this.f) {
            this.f = true;
            HttpEvent httpEvent = new HttpEvent(getURL().toString(), -1, e(), exc.getClass().getSimpleName());
            httpEvent.setIp(this.g);
            httpEvent.setRequestId(this.a);
            g.a().a(httpEvent);
        }
    }

    private void c() {
        String host = this.url.getHost();
        if (this.g == null && host != null && !a(host)) {
            a.a().execute(new Runnable() {
                public void run() {
                    d.this.d();
                }
            });
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0034, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void d() {
        /*
            r2 = this;
            monitor-enter(r2)
            java.lang.String r0 = r2.g     // Catch:{ all -> 0x0035 }
            boolean r0 = android.text.TextUtils.isEmpty(r0)     // Catch:{ all -> 0x0035 }
            if (r0 == 0) goto L_0x0033
            boolean r0 = r2.f     // Catch:{ all -> 0x0035 }
            if (r0 == 0) goto L_0x000e
            goto L_0x0033
        L_0x000e:
            java.net.URL r0 = r2.url     // Catch:{ all -> 0x0035 }
            java.lang.String r0 = r0.getHost()     // Catch:{ all -> 0x0035 }
            java.lang.String r1 = r2.g     // Catch:{ all -> 0x0035 }
            if (r1 != 0) goto L_0x0031
            if (r0 == 0) goto L_0x0031
            boolean r1 = r2.a((java.lang.String) r0)     // Catch:{ all -> 0x0035 }
            if (r1 != 0) goto L_0x0031
            java.net.InetAddress r0 = java.net.InetAddress.getByName(r0)     // Catch:{ Exception -> 0x002b }
            java.lang.String r0 = r0.getHostAddress()     // Catch:{ Exception -> 0x002b }
            r2.g = r0     // Catch:{ Exception -> 0x002b }
            goto L_0x0031
        L_0x002b:
            r0 = move-exception
            java.lang.String r1 = "can not get ip exception:"
            com.xiaomi.mistatistic.sdk.controller.h.a((java.lang.String) r1, (java.lang.Throwable) r0)     // Catch:{ all -> 0x0035 }
        L_0x0031:
            monitor-exit(r2)
            return
        L_0x0033:
            monitor-exit(r2)
            return
        L_0x0035:
            r0 = move-exception
            monitor-exit(r2)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.xiaomi.mistatistic.sdk.network.d.d():void");
    }

    private boolean a(String str) {
        if (b.a(str) || b.d(str)) {
            return true;
        }
        return false;
    }

    private int e() {
        if (this.e != -1) {
            return this.e;
        }
        try {
            return this.h.getResponseCode();
        } catch (Exception e2) {
            return -1;
        }
    }

    private synchronized void f() {
        if (this.c == 0) {
            this.c = SystemClock.elapsedRealtime();
            this.b = this.c;
        }
    }

    private synchronized void g() {
        if (this.d == 0 && this.c != 0) {
            this.d = SystemClock.elapsedRealtime() - this.c;
        }
    }
}
