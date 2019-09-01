package com.android.keyguard;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtilsCompat;
import com.android.internal.widget.LockPatternView;
import com.android.keyguard.analytics.AnalyticsHelper;
import java.util.List;
import miui.os.Build;

public final class LockPatternChecker {
    private static final String CURRENT_DEVICE = Build.DEVICE;
    private static final boolean IS_NEED_COMPUTE_ATTEMPT_TIMES_DEVICE = ("libra".equals(CURRENT_DEVICE) || "aqua".equals(CURRENT_DEVICE) || "kenzo".equals(CURRENT_DEVICE) || "kate".equals(CURRENT_DEVICE));
    /* access modifiers changed from: private */
    public static String TAG = "miui_keyguard_password";

    public static AsyncTask<?, ?, ?> checkPatternForUsers(LockPatternUtilsWrapper lockPatternUtilsWrapper, LockPatternUtils lockPatternUtils, List<LockPatternView.Cell> pattern, Context context, OnCheckForUsersCallback callback) {
        BoostFrameworkHelper.setBoost(3);
        final List<LockPatternView.Cell> list = pattern;
        final LockPatternUtilsWrapper lockPatternUtilsWrapper2 = lockPatternUtilsWrapper;
        final OnCheckForUsersCallback onCheckForUsersCallback = callback;
        final Context context2 = context;
        final LockPatternUtils lockPatternUtils2 = lockPatternUtils;
        AnonymousClass1 r1 = new AsyncTask<Void, Void, Boolean>() {
            private int mThrottleTimeout;
            private int mUserIdMatched = -10000;

            /* access modifiers changed from: protected */
            /* JADX WARNING: Removed duplicated region for block: B:19:0x006e  */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public java.lang.Boolean doInBackground(java.lang.Void... r11) {
                /*
                    r10 = this;
                    int r0 = com.android.keyguard.KeyguardUpdateMonitor.getCurrentUser()
                    r10.mUserIdMatched = r0
                    r1 = 0
                    r2 = 1
                    java.util.List r3 = r2     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    if (r3 == 0) goto L_0x003c
                    java.util.List r3 = r2     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    boolean r3 = r3.isEmpty()     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    if (r3 != 0) goto L_0x003c
                    com.android.keyguard.LockPatternUtilsWrapper r3 = r3     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    java.util.List r4 = r2     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    com.android.keyguard.OnCheckForUsersCallback r5 = r4     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    boolean r3 = r3.checkPattern(r4, r0, r5)     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    if (r3 == 0) goto L_0x002c
                    r10.mUserIdMatched = r0     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    android.content.Context r3 = r5     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    com.android.keyguard.LockPatternChecker.computeAttemptTimes(r3, r0, r2)     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    java.lang.Boolean r3 = java.lang.Boolean.valueOf(r2)     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    return r3
                L_0x002c:
                    android.content.Context r3 = r5     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    com.android.keyguard.LockPatternChecker.computeAttemptTimes(r3, r0, r1)     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    android.content.Context r3 = r5     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    com.android.internal.widget.LockPatternUtils r4 = r6     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    int r3 = com.android.keyguard.LockPatternChecker.computeRetryTimeout(r3, r4, r0)     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    r10.mThrottleTimeout = r3     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    goto L_0x005d
                L_0x003c:
                    java.lang.String r3 = "miui_keyguard"
                    java.lang.String r4 = "pattern is null when check pattern for currentUserId"
                    android.util.Log.e(r3, r4)     // Catch:{ KeyguardRequestThrottledException -> 0x0056, Exception -> 0x0044 }
                    goto L_0x005d
                L_0x0044:
                    r3 = move-exception
                    java.lang.String r4 = com.android.keyguard.LockPatternChecker.TAG
                    java.lang.String r5 = "checkPatternForUsers failed"
                    android.util.Slog.e(r4, r5, r3)
                    java.lang.String r4 = android.util.Log.getStackTraceString(r3)
                    com.android.keyguard.analytics.AnalyticsHelper.trackCheckPasswordFailedException(r4)
                    goto L_0x005e
                L_0x0056:
                    r3 = move-exception
                    int r4 = r3.getTimeoutMs()
                    r10.mThrottleTimeout = r4
                L_0x005d:
                L_0x005e:
                    android.content.Context r3 = r5
                    java.util.List r3 = com.android.keyguard.MiuiKeyguardUtils.getUserList(r3)
                    java.util.Iterator r4 = r3.iterator()
                L_0x0068:
                    boolean r5 = r4.hasNext()
                    if (r5 == 0) goto L_0x00db
                    java.lang.Object r5 = r4.next()
                    android.content.pm.UserInfo r5 = (android.content.pm.UserInfo) r5
                    int r6 = r5.id
                    if (r6 != r0) goto L_0x0079
                    goto L_0x0068
                L_0x0079:
                    java.util.List r7 = r2     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    if (r7 == 0) goto L_0x00be
                    java.util.List r7 = r2     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    boolean r7 = r7.isEmpty()     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    if (r7 != 0) goto L_0x00be
                    com.android.keyguard.LockPatternUtilsWrapper r7 = r3     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    boolean r7 = com.android.keyguard.LockPatternChecker.isPatternPasswordEnable(r7, r6)     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    if (r7 == 0) goto L_0x00d9
                    com.android.internal.widget.LockPatternUtils r7 = r6     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    boolean r7 = com.android.keyguard.LockPatternChecker.shouldCheck(r6, r7)     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    if (r7 == 0) goto L_0x00d9
                    com.android.keyguard.LockPatternUtilsWrapper r7 = r3     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    java.util.List r8 = r2     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    boolean r7 = r7.checkPattern(r8, r6)     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    if (r7 == 0) goto L_0x00ab
                    r10.mUserIdMatched = r6     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    android.content.Context r7 = r5     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    com.android.keyguard.LockPatternChecker.computeAttemptTimes(r7, r6, r2)     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    java.lang.Boolean r7 = java.lang.Boolean.valueOf(r2)     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    return r7
                L_0x00ab:
                    android.content.Context r7 = r5     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    com.android.keyguard.LockPatternChecker.computeAttemptTimes(r7, r6, r1)     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    android.content.Context r7 = r5     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    com.android.internal.widget.LockPatternUtils r8 = r6     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    int r7 = com.android.keyguard.LockPatternChecker.computeRetryTimeout(r7, r8, r6)     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    com.android.internal.widget.LockPatternUtils r8 = r6     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    r8.setLockoutAttemptDeadline(r6, r7)     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    goto L_0x00d9
                L_0x00be:
                    java.lang.String r7 = "miui_keyguard"
                    java.lang.String r8 = "pattern is null when check pattern foe other user"
                    android.util.Log.e(r7, r8)     // Catch:{ KeyguardRequestThrottledException -> 0x00d8, Exception -> 0x00c6 }
                    goto L_0x00d9
                L_0x00c6:
                    r7 = move-exception
                    java.lang.String r8 = com.android.keyguard.LockPatternChecker.TAG
                    java.lang.String r9 = "checkPatternForUsers other users failed"
                    android.util.Slog.e(r8, r9, r7)
                    java.lang.String r8 = android.util.Log.getStackTraceString(r7)
                    com.android.keyguard.analytics.AnalyticsHelper.trackCheckPasswordFailedException(r8)
                    goto L_0x00da
                L_0x00d8:
                    r7 = move-exception
                L_0x00d9:
                L_0x00da:
                    goto L_0x0068
                L_0x00db:
                    java.lang.Boolean r1 = java.lang.Boolean.valueOf(r1)
                    return r1
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.keyguard.LockPatternChecker.AnonymousClass1.doInBackground(java.lang.Void[]):java.lang.Boolean");
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Boolean result) {
                onCheckForUsersCallback.onChecked(result.booleanValue(), this.mUserIdMatched, this.mThrottleTimeout);
            }
        };
        r1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        return r1;
    }

    public static AsyncTask<?, ?, ?> checkPasswordForUsers(LockPatternUtilsWrapper lockPatternUtilsWrapper, LockPatternUtils lockPatternUtils, String password, Context context, OnCheckForUsersCallback callback) {
        BoostFrameworkHelper.setBoost(3);
        final LockPatternUtilsWrapper lockPatternUtilsWrapper2 = lockPatternUtilsWrapper;
        final String str = password;
        final OnCheckForUsersCallback onCheckForUsersCallback = callback;
        final Context context2 = context;
        final LockPatternUtils lockPatternUtils2 = lockPatternUtils;
        AnonymousClass2 r1 = new AsyncTask<Void, Void, Boolean>() {
            private int mThrottleTimeout;
            private int mUserIdMatched = -10000;

            /* access modifiers changed from: protected */
            public Boolean doInBackground(Void... args) {
                int currentUserId = KeyguardUpdateMonitor.getCurrentUser();
                this.mUserIdMatched = currentUserId;
                try {
                    if (LockPatternUtilsWrapper.this.checkPassword(str, currentUserId, onCheckForUsersCallback)) {
                        this.mUserIdMatched = currentUserId;
                        LockPatternChecker.computeAttemptTimes(context2, currentUserId, true);
                        return true;
                    }
                    LockPatternChecker.computeAttemptTimes(context2, currentUserId, false);
                    this.mThrottleTimeout = LockPatternChecker.computeRetryTimeout(context2, lockPatternUtils2, currentUserId);
                    for (UserInfo info : MiuiKeyguardUtils.getUserList(context2)) {
                        int userId = info.id;
                        if (userId != currentUserId) {
                            try {
                                if (LockPatternChecker.isPasswordEnable(LockPatternUtilsWrapper.this, userId) && LockPatternChecker.shouldCheck(userId, lockPatternUtils2)) {
                                    if (LockPatternUtilsWrapper.this.checkPassword(str, userId)) {
                                        this.mUserIdMatched = userId;
                                        LockPatternChecker.computeAttemptTimes(context2, currentUserId, true);
                                        return true;
                                    }
                                    LockPatternChecker.computeAttemptTimes(context2, currentUserId, false);
                                    lockPatternUtils2.setLockoutAttemptDeadline(userId, LockPatternChecker.computeRetryTimeout(context2, lockPatternUtils2, userId));
                                }
                            } catch (KeyguardRequestThrottledException e) {
                            } catch (Exception ex) {
                                Slog.e(LockPatternChecker.TAG, "checkPasswordForUsers other users failed", ex);
                                AnalyticsHelper.trackCheckPasswordFailedException(Log.getStackTraceString(ex));
                            }
                        }
                    }
                    return false;
                } catch (KeyguardRequestThrottledException ex2) {
                    this.mThrottleTimeout = ex2.getTimeoutMs();
                } catch (Exception ex3) {
                    Slog.e(LockPatternChecker.TAG, "checkPasswordForUsers failed", ex3);
                    AnalyticsHelper.trackCheckPasswordFailedException(Log.getStackTraceString(ex3));
                }
            }

            /* access modifiers changed from: protected */
            public void onPostExecute(Boolean result) {
                onCheckForUsersCallback.onChecked(result.booleanValue(), this.mUserIdMatched, this.mThrottleTimeout);
            }
        };
        r1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        return r1;
    }

    /* access modifiers changed from: private */
    public static boolean isPatternPasswordEnable(LockPatternUtilsWrapper mLockPatternUtilsWrapper, int userId) {
        boolean isEnable = mLockPatternUtilsWrapper.getActivePasswordQuality(userId) == 65536;
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("check pattern password enable for userId : ");
        sb.append(userId);
        sb.append(isEnable ? "   enable" : "   disable");
        Log.d(str, sb.toString());
        return isEnable;
    }

    /* access modifiers changed from: private */
    public static boolean isPasswordEnable(LockPatternUtilsWrapper mLockPatternUtilsWrapper, int userId) {
        int passwordQuality = mLockPatternUtilsWrapper.getActivePasswordQuality(userId);
        boolean isEnable = passwordQuality == 262144 || passwordQuality == 327680 || passwordQuality == 393216 || passwordQuality == 131072 || passwordQuality == 196608;
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("check password enable for userId : ");
        sb.append(userId);
        sb.append(isEnable ? "   enable" : "   disable");
        Log.d(str, sb.toString());
        return isEnable;
    }

    /* access modifiers changed from: private */
    public static boolean shouldCheck(int userId, LockPatternUtils utils) {
        boolean z = true;
        if (!IS_NEED_COMPUTE_ATTEMPT_TIMES_DEVICE) {
            return true;
        }
        if (utils.getLockoutAttemptDeadline(userId) != 0) {
            z = false;
        }
        return z;
    }

    /* access modifiers changed from: private */
    public static void computeAttemptTimes(Context context, int userId, boolean sucess) {
        if (IS_NEED_COMPUTE_ATTEMPT_TIMES_DEVICE) {
            int times = 0;
            if (!sucess) {
                times = Settings.Secure.getIntForUser(context.getContentResolver(), MiuiSettings.Secure.UNLOCK_FAILED_ATTEMPTS, 0, userId) + 1;
            }
            Settings.Secure.putIntForUser(context.getContentResolver(), MiuiSettings.Secure.UNLOCK_FAILED_ATTEMPTS, times, userId);
        }
    }

    /* access modifiers changed from: private */
    public static int computeRetryTimeout(Context context, LockPatternUtils lockPatternUtils, int userId) {
        if (!IS_NEED_COMPUTE_ATTEMPT_TIMES_DEVICE) {
            return 0;
        }
        int timeout = 0;
        int failedAttempts = Settings.Secure.getIntForUser(context.getContentResolver(), MiuiSettings.Secure.UNLOCK_FAILED_ATTEMPTS, 0, userId);
        if (failedAttempts == 5) {
            timeout = 30000;
        } else if (failedAttempts >= 10 && failedAttempts < 30) {
            timeout = 30000;
        } else if (failedAttempts >= 30 && failedAttempts < 140) {
            timeout = (int) (30000.0d * Math.pow(2.0d, ((double) (failedAttempts - 30)) / 10.0d));
        } else if (failedAttempts >= 140) {
            timeout = 86400000;
        }
        if (timeout > 0) {
            LockPatternUtilsCompat.requireStrongAuth(lockPatternUtils, 8, userId);
        }
        return timeout;
    }
}
