package com.android.systemui.miui.statusbar.analytics;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5 {
    public static String getMd5Digest(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(input.getBytes());
            return String.format("%1$032X", new Object[]{new BigInteger(1, digest.digest())});
        } catch (NoSuchAlgorithmException lException) {
            throw new RuntimeException(lException);
        }
    }
}
