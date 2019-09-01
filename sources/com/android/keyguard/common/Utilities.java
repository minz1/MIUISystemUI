package com.android.keyguard.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class Utilities {
    public static boolean isUriFileExists(Context context, Uri uri) {
        assertNoUIThread();
        if (uri == null) {
            return false;
        }
        try {
            closeFileSafely(context.getContentResolver().openInputStream(uri));
            return true;
        } catch (Exception e) {
            Log.e("Utilities", "isUriFileExists", e);
            return false;
        }
    }

    private static void assertNoUIThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("function cannot run no UI thread!");
        }
    }

    private static void closeFileSafely(Closeable file) {
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
                Log.e("Utilities", "closeFileSafely", e);
            }
        }
    }

    public static Bitmap createBitmapSafely(int width, int height, Bitmap.Config config) {
        try {
            return Bitmap.createBitmap(width, height, config);
        } catch (OutOfMemoryError e) {
            Log.e("Utilities", "createBitmapSafely", e);
            return null;
        }
    }

    public static int getImageRotation(InputStream is) {
        if (is == null) {
            return 0;
        }
        byte[] buf = new byte[8];
        int length = 0;
        while (read(is, buf, 2) && (buf[0] & 255) == 255) {
            int marker = buf[1] & 255;
            if (!(marker == 255 || marker == 216 || marker == 1)) {
                if (marker == 217 || marker == 218 || !read(is, buf, 2)) {
                    return 0;
                }
                int length2 = pack(buf, 0, 2, false);
                if (length2 < 2) {
                    Log.e("Utilities", "Invalid length");
                    return 0;
                }
                length = length2 - 2;
                if (marker == 225 && length >= 6) {
                    if (read(is, buf, 6)) {
                        length -= 6;
                        if (pack(buf, 0, 4, false) == 1165519206 && pack(buf, 4, 2, false) == 0) {
                            break;
                        }
                    } else {
                        return 0;
                    }
                }
                try {
                    is.skip((long) length);
                    length = 0;
                } catch (IOException e) {
                    return 0;
                }
            }
        }
        if (length > 8) {
            byte[] jpeg = new byte[length];
            if (!read(is, jpeg, length)) {
                return 0;
            }
            int tag = pack(jpeg, 0, 4, false);
            if (tag == 1229531648 || tag == 1296891946) {
                boolean littleEndian = tag == 1229531648;
                int count = pack(jpeg, 0 + 4, 4, littleEndian) + 2;
                if (count >= 10 && count <= length) {
                    int offset = 0 + count;
                    int length3 = length - count;
                    int count2 = pack(jpeg, offset - 2, 2, littleEndian);
                    while (true) {
                        int count3 = count2 - 1;
                        if (count2 <= 0 || length3 < 12) {
                            break;
                        }
                        if (pack(jpeg, offset, 2, littleEndian) == 274) {
                            int orientation = pack(jpeg, offset + 8, 2, littleEndian);
                            if (orientation == 1) {
                                return 0;
                            }
                            if (orientation == 3) {
                                return 180;
                            }
                            if (orientation == 6) {
                                return 90;
                            }
                            if (orientation == 8) {
                                return 270;
                            }
                            Log.i("Utilities", "Unsupported orientation");
                        }
                        offset += 12;
                        length3 -= 12;
                        count2 = count3;
                    }
                } else {
                    Log.e("Utilities", "Invalid offset");
                    return 0;
                }
            } else {
                Log.e("Utilities", "Invalid byte order");
                return 0;
            }
        }
        Log.i("Utilities", "Orientation not found");
        return 0;
    }

    private static int pack(byte[] bytes, int offset, int length, boolean littleEndian) {
        int step = 1;
        if (littleEndian) {
            offset += length - 1;
            step = -1;
        }
        int value = 0;
        while (true) {
            int length2 = length - 1;
            if (length <= 0) {
                return value;
            }
            value = (value << 8) | (bytes[offset] & 255);
            offset += step;
            length = length2;
        }
    }

    private static boolean read(InputStream is, byte[] buf, int length) {
        boolean z = false;
        try {
            if (is.read(buf, 0, length) == length) {
                z = true;
            }
            return z;
        } catch (IOException e) {
            return false;
        }
    }
}
