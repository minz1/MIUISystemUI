package android.support.v4.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.os.Process;
import android.util.Log;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class TypefaceCompatUtil {
    public static File getTempFile(Context context) {
        String prefix = ".font" + Process.myPid() + "-" + Process.myTid() + "-";
        int i = 0;
        while (i < 100) {
            File file = new File(context.getCacheDir(), prefix + i);
            try {
                if (file.createNewFile()) {
                    return file;
                }
                i++;
            } catch (IOException e) {
            }
        }
        return null;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0020, code lost:
        r3 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0021, code lost:
        r9 = r3;
        r3 = r2;
        r2 = r9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x001b, code lost:
        r2 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x001c, code lost:
        r3 = null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static java.nio.ByteBuffer mmap(java.io.File r10) {
        /*
            r0 = 0
            java.io.FileInputStream r1 = new java.io.FileInputStream     // Catch:{ IOException -> 0x0033 }
            r1.<init>(r10)     // Catch:{ IOException -> 0x0033 }
            java.nio.channels.FileChannel r2 = r1.getChannel()     // Catch:{ Throwable -> 0x001e, all -> 0x001b }
            long r7 = r2.size()     // Catch:{ Throwable -> 0x001e, all -> 0x001b }
            java.nio.channels.FileChannel$MapMode r4 = java.nio.channels.FileChannel.MapMode.READ_ONLY     // Catch:{ Throwable -> 0x001e, all -> 0x001b }
            r5 = 0
            r3 = r2
            java.nio.MappedByteBuffer r3 = r3.map(r4, r5, r7)     // Catch:{ Throwable -> 0x001e, all -> 0x001b }
            r1.close()     // Catch:{ IOException -> 0x0033 }
            return r3
        L_0x001b:
            r2 = move-exception
            r3 = r0
            goto L_0x0024
        L_0x001e:
            r2 = move-exception
            throw r2     // Catch:{ all -> 0x0020 }
        L_0x0020:
            r3 = move-exception
            r9 = r3
            r3 = r2
            r2 = r9
        L_0x0024:
            if (r3 == 0) goto L_0x002f
            r1.close()     // Catch:{ Throwable -> 0x002a }
            goto L_0x0032
        L_0x002a:
            r4 = move-exception
            r3.addSuppressed(r4)     // Catch:{ IOException -> 0x0033 }
            goto L_0x0032
        L_0x002f:
            r1.close()     // Catch:{ IOException -> 0x0033 }
        L_0x0032:
            throw r2     // Catch:{ IOException -> 0x0033 }
        L_0x0033:
            r1 = move-exception
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.graphics.TypefaceCompatUtil.mmap(java.io.File):java.nio.ByteBuffer");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0037, code lost:
        r4 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0038, code lost:
        r5 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x003c, code lost:
        r5 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x003d, code lost:
        r11 = r5;
        r5 = r4;
        r4 = r11;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x004f, code lost:
        r3 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0050, code lost:
        r4 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0054, code lost:
        r4 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x0055, code lost:
        r11 = r4;
        r4 = r3;
        r3 = r11;
     */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x004f A[ExcHandler: all (th java.lang.Throwable)] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.nio.ByteBuffer mmap(android.content.Context r12, android.os.CancellationSignal r13, android.net.Uri r14) {
        /*
            android.content.ContentResolver r0 = r12.getContentResolver()
            r1 = 0
            java.lang.String r2 = "r"
            android.os.ParcelFileDescriptor r2 = r0.openFileDescriptor(r14, r2, r13)     // Catch:{ IOException -> 0x0069 }
            if (r2 != 0) goto L_0x0014
            if (r2 == 0) goto L_0x0013
            r2.close()     // Catch:{ IOException -> 0x0069 }
        L_0x0013:
            return r1
        L_0x0014:
            java.io.FileInputStream r3 = new java.io.FileInputStream     // Catch:{ Throwable -> 0x0052, all -> 0x004f }
            java.io.FileDescriptor r4 = r2.getFileDescriptor()     // Catch:{ Throwable -> 0x0052, all -> 0x004f }
            r3.<init>(r4)     // Catch:{ Throwable -> 0x0052, all -> 0x004f }
            java.nio.channels.FileChannel r4 = r3.getChannel()     // Catch:{ Throwable -> 0x003a, all -> 0x0037 }
            long r9 = r4.size()     // Catch:{ Throwable -> 0x003a, all -> 0x0037 }
            java.nio.channels.FileChannel$MapMode r6 = java.nio.channels.FileChannel.MapMode.READ_ONLY     // Catch:{ Throwable -> 0x003a, all -> 0x0037 }
            r7 = 0
            r5 = r4
            java.nio.MappedByteBuffer r5 = r5.map(r6, r7, r9)     // Catch:{ Throwable -> 0x003a, all -> 0x0037 }
            r3.close()     // Catch:{ Throwable -> 0x0052, all -> 0x004f }
            if (r2 == 0) goto L_0x0036
            r2.close()     // Catch:{ IOException -> 0x0069 }
        L_0x0036:
            return r5
        L_0x0037:
            r4 = move-exception
            r5 = r1
            goto L_0x0040
        L_0x003a:
            r4 = move-exception
            throw r4     // Catch:{ all -> 0x003c }
        L_0x003c:
            r5 = move-exception
            r11 = r5
            r5 = r4
            r4 = r11
        L_0x0040:
            if (r5 == 0) goto L_0x004b
            r3.close()     // Catch:{ Throwable -> 0x0046, all -> 0x004f }
            goto L_0x004e
        L_0x0046:
            r6 = move-exception
            r5.addSuppressed(r6)     // Catch:{ Throwable -> 0x0052, all -> 0x004f }
            goto L_0x004e
        L_0x004b:
            r3.close()     // Catch:{ Throwable -> 0x0052, all -> 0x004f }
        L_0x004e:
            throw r4     // Catch:{ Throwable -> 0x0052, all -> 0x004f }
        L_0x004f:
            r3 = move-exception
            r4 = r1
            goto L_0x0058
        L_0x0052:
            r3 = move-exception
            throw r3     // Catch:{ all -> 0x0054 }
        L_0x0054:
            r4 = move-exception
            r11 = r4
            r4 = r3
            r3 = r11
        L_0x0058:
            if (r2 == 0) goto L_0x0068
            if (r4 == 0) goto L_0x0065
            r2.close()     // Catch:{ Throwable -> 0x0060 }
            goto L_0x0068
        L_0x0060:
            r5 = move-exception
            r4.addSuppressed(r5)     // Catch:{ IOException -> 0x0069 }
            goto L_0x0068
        L_0x0065:
            r2.close()     // Catch:{ IOException -> 0x0069 }
        L_0x0068:
            throw r3     // Catch:{ IOException -> 0x0069 }
        L_0x0069:
            r2 = move-exception
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.graphics.TypefaceCompatUtil.mmap(android.content.Context, android.os.CancellationSignal, android.net.Uri):java.nio.ByteBuffer");
    }

    public static ByteBuffer copyToDirectBuffer(Context context, Resources res, int id) {
        File tmpFile = getTempFile(context);
        ByteBuffer byteBuffer = null;
        if (tmpFile == null) {
            return null;
        }
        try {
            if (copyToFile(tmpFile, res, id)) {
                byteBuffer = mmap(tmpFile);
            }
            return byteBuffer;
        } finally {
            tmpFile.delete();
        }
    }

    public static boolean copyToFile(File file, InputStream is) {
        FileOutputStream os = null;
        boolean z = false;
        try {
            os = new FileOutputStream(file, false);
            byte[] buffer = new byte[1024];
            while (true) {
                int read = is.read(buffer);
                int readLen = read;
                if (read == -1) {
                    break;
                }
                os.write(buffer, 0, readLen);
            }
            z = true;
        } catch (IOException e) {
            Log.e("TypefaceCompatUtil", "Error copying resource contents to temp file: " + e.getMessage());
        } catch (Throwable th) {
            closeQuietly(null);
            throw th;
        }
        closeQuietly(os);
        return z;
    }

    public static boolean copyToFile(File file, Resources res, int id) {
        InputStream is = null;
        try {
            is = res.openRawResource(id);
            return copyToFile(file, is);
        } finally {
            closeQuietly(is);
        }
    }

    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }
}
