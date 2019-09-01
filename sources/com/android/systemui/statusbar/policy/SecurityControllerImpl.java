package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManagerCompat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserHandleCompat;
import android.os.UserManager;
import android.os.UserManagerCompat;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.SecurityController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class SecurityControllerImpl extends CurrentUserTracker implements SecurityController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("SecurityController", 3);
    private static final NetworkRequest REQUEST = new NetworkRequest.Builder().removeCapability(15).removeCapability(13).removeCapability(14).build();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.security.action.TRUST_STORE_CHANGED".equals(intent.getAction())) {
                SecurityControllerImpl.this.refreshCACerts();
            }
        }
    };
    @GuardedBy("mCallbacks")
    private final ArrayList<SecurityController.SecurityControllerCallback> mCallbacks = new ArrayList<>();
    private final ConnectivityManager mConnectivityManager;
    private final IConnectivityManager mConnectivityManagerService;
    /* access modifiers changed from: private */
    public final Context mContext;
    private int mCurrentUserId;
    private SparseArray<VpnConfig> mCurrentVpns = new SparseArray<>();
    private final DevicePolicyManager mDevicePolicyManager;
    /* access modifiers changed from: private */
    public ArrayMap<Integer, Boolean> mHasCACerts = new ArrayMap<>();
    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        public void onAvailable(Network network) {
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onAvailable " + network.netId);
            }
            SecurityControllerImpl.this.updateState();
            SecurityControllerImpl.this.fireCallbacks();
        }

        public void onLost(Network network) {
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onLost " + network.netId);
            }
            SecurityControllerImpl.this.updateState();
            SecurityControllerImpl.this.fireCallbacks();
        }
    };
    private final PackageManager mPackageManager;
    private final UserManager mUserManager;
    private int mVpnUserId;

    protected class CACertLoader extends AsyncTask<Integer, Void, Pair<Integer, Boolean>> {
        protected CACertLoader() {
        }

        /* access modifiers changed from: protected */
        /* JADX WARNING: Code restructure failed: missing block: B:10:0x003a, code lost:
            r4 = null;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:14:0x003e, code lost:
            r4 = move-exception;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:15:0x003f, code lost:
            r7 = r4;
            r4 = r3;
            r3 = r7;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:25:0x0053, code lost:
            r2 = move-exception;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:29:0x005c, code lost:
            android.util.Log.i("SecurityController", r2.getMessage());
         */
        /* JADX WARNING: Code restructure failed: missing block: B:30:0x0065, code lost:
            new android.os.Handler((android.os.Looper) com.android.systemui.Dependency.get(com.android.systemui.Dependency.BG_LOOPER)).postDelayed(new com.android.systemui.statusbar.policy.SecurityControllerImpl.CACertLoader.AnonymousClass1(r8), 30000);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:31:0x0083, code lost:
            return new android.util.Pair<>(r9[0], null);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:9:0x0039, code lost:
            r3 = th;
         */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x0053 A[ExcHandler: RemoteException | AssertionError | InterruptedException (r2v0 'e' java.lang.Throwable A[CUSTOM_DECLARE]), Splitter:B:1:0x0002] */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public android.util.Pair<java.lang.Integer, java.lang.Boolean> doInBackground(final java.lang.Integer... r9) {
            /*
                r8 = this;
                r0 = 0
                r1 = 0
                com.android.systemui.statusbar.policy.SecurityControllerImpl r2 = com.android.systemui.statusbar.policy.SecurityControllerImpl.this     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
                android.content.Context r2 = r2.mContext     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
                r3 = r9[r1]     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
                int r3 = r3.intValue()     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
                android.os.UserHandle r3 = android.os.UserHandleCompat.of(r3)     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
                android.security.KeyChain$KeyChainConnection r2 = android.security.KeyChain.bindAsUser(r2, r3)     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
                android.security.IKeyChainService r3 = r2.getService()     // Catch:{ Throwable -> 0x003c, all -> 0x0039 }
                android.content.pm.StringParceledListSlice r3 = r3.getUserCaAliases()     // Catch:{ Throwable -> 0x003c, all -> 0x0039 }
                java.util.List r3 = r3.getList()     // Catch:{ Throwable -> 0x003c, all -> 0x0039 }
                boolean r3 = r3.isEmpty()     // Catch:{ Throwable -> 0x003c, all -> 0x0039 }
                r3 = r3 ^ 1
                android.util.Pair r4 = new android.util.Pair     // Catch:{ Throwable -> 0x003c, all -> 0x0039 }
                r5 = r9[r1]     // Catch:{ Throwable -> 0x003c, all -> 0x0039 }
                java.lang.Boolean r6 = java.lang.Boolean.valueOf(r3)     // Catch:{ Throwable -> 0x003c, all -> 0x0039 }
                r4.<init>(r5, r6)     // Catch:{ Throwable -> 0x003c, all -> 0x0039 }
                if (r2 == 0) goto L_0x0038
                r2.close()     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
            L_0x0038:
                return r4
            L_0x0039:
                r3 = move-exception
                r4 = r0
                goto L_0x0042
            L_0x003c:
                r3 = move-exception
                throw r3     // Catch:{ all -> 0x003e }
            L_0x003e:
                r4 = move-exception
                r7 = r4
                r4 = r3
                r3 = r7
            L_0x0042:
                if (r2 == 0) goto L_0x0052
                if (r4 == 0) goto L_0x004f
                r2.close()     // Catch:{ Throwable -> 0x004a, RemoteException | AssertionError | InterruptedException -> 0x0053, RemoteException | AssertionError | InterruptedException -> 0x0053 }
                goto L_0x0052
            L_0x004a:
                r5 = move-exception
                r4.addSuppressed(r5)     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
                goto L_0x0052
            L_0x004f:
                r2.close()     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
            L_0x0052:
                throw r3     // Catch:{ RemoteException | AssertionError | InterruptedException -> 0x0053 }
            L_0x0053:
                r2 = move-exception
                if (r2 == 0) goto L_0x0065
                java.lang.String r3 = r2.getMessage()
                if (r3 == 0) goto L_0x0065
                java.lang.String r3 = "SecurityController"
                java.lang.String r4 = r2.getMessage()
                android.util.Log.i(r3, r4)
            L_0x0065:
                android.os.Handler r3 = new android.os.Handler
                com.android.systemui.Dependency$DependencyKey<android.os.Looper> r4 = com.android.systemui.Dependency.BG_LOOPER
                java.lang.Object r4 = com.android.systemui.Dependency.get(r4)
                android.os.Looper r4 = (android.os.Looper) r4
                r3.<init>(r4)
                com.android.systemui.statusbar.policy.SecurityControllerImpl$CACertLoader$1 r4 = new com.android.systemui.statusbar.policy.SecurityControllerImpl$CACertLoader$1
                r4.<init>(r9)
                r5 = 30000(0x7530, double:1.4822E-319)
                r3.postDelayed(r4, r5)
                android.util.Pair r3 = new android.util.Pair
                r1 = r9[r1]
                r3.<init>(r1, r0)
                return r3
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.SecurityControllerImpl.CACertLoader.doInBackground(java.lang.Integer[]):android.util.Pair");
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Pair<Integer, Boolean> result) {
            if (SecurityControllerImpl.DEBUG) {
                Log.d("SecurityController", "onPostExecute " + result);
            }
            if (result.second != null) {
                SecurityControllerImpl.this.mHasCACerts.put((Integer) result.first, (Boolean) result.second);
                SecurityControllerImpl.this.fireCallbacks();
            }
        }
    }

    public SecurityControllerImpl(Context context) {
        super(context);
        this.mContext = context;
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService("device_policy");
        this.mConnectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        this.mConnectivityManagerService = IConnectivityManager.Stub.asInterface(ServiceManager.getService("connectivity"));
        this.mPackageManager = context.getPackageManager();
        this.mUserManager = (UserManager) context.getSystemService("user");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.security.action.TRUST_STORE_CHANGED");
        context.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter, null, new Handler((Looper) Dependency.get(Dependency.BG_LOOPER)));
        this.mConnectivityManager.registerNetworkCallback(REQUEST, this.mNetworkCallback);
        onUserSwitched(ActivityManager.getCurrentUser());
        startTracking();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SecurityController state:");
        pw.print("  mCurrentVpns={");
        for (int i = 0; i < this.mCurrentVpns.size(); i++) {
            if (i > 0) {
                pw.print(", ");
            }
            pw.print(this.mCurrentVpns.keyAt(i));
            pw.print('=');
            pw.print(this.mCurrentVpns.valueAt(i).user);
        }
        pw.println("}");
    }

    public boolean isDeviceManaged() {
        return DevicePolicyManagerCompat.isDeviceManaged(this.mDevicePolicyManager);
    }

    public CharSequence getDeviceOwnerOrganizationName() {
        return DevicePolicyManagerCompat.getDeviceOwnerOrganizationName(this.mDevicePolicyManager);
    }

    public CharSequence getWorkProfileOrganizationName() {
        int profileId = getWorkProfileUserId(this.mCurrentUserId);
        if (profileId == -10000) {
            return null;
        }
        return DevicePolicyManagerCompat.getOrganizationNameForUser(this.mDevicePolicyManager, profileId);
    }

    public String getPrimaryVpnName() {
        VpnConfig cfg = this.mCurrentVpns.get(this.mVpnUserId);
        if (cfg != null) {
            return getNameForVpnConfig(cfg, new UserHandle(this.mVpnUserId));
        }
        return null;
    }

    private int getWorkProfileUserId(int userId) {
        for (UserInfo userInfo : this.mUserManager.getProfiles(userId)) {
            if (userInfo.isManagedProfile()) {
                return userInfo.id;
            }
        }
        return -10000;
    }

    public boolean hasWorkProfile() {
        return getWorkProfileUserId(this.mCurrentUserId) != -10000;
    }

    public String getWorkProfileVpnName() {
        int profileId = getWorkProfileUserId(this.mVpnUserId);
        if (profileId == -10000) {
            return null;
        }
        VpnConfig cfg = this.mCurrentVpns.get(profileId);
        if (cfg != null) {
            return getNameForVpnConfig(cfg, UserHandleCompat.of(profileId));
        }
        return null;
    }

    public boolean isNetworkLoggingEnabled() {
        return DevicePolicyManagerCompat.isNetworkLoggingEnabled(this.mDevicePolicyManager);
    }

    public boolean isVpnEnabled() {
        for (int profileId : UserManagerCompat.getProfileIdsWithDisabled(this.mUserManager, this.mVpnUserId)) {
            if (this.mCurrentVpns.get(profileId) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isSilentVpnPackage() {
        return "com.miui.vpnsdkmanager".equals(getVpnPackageName());
    }

    public boolean hasCACertInCurrentUser() {
        Boolean hasCACerts = this.mHasCACerts.get(Integer.valueOf(this.mCurrentUserId));
        return hasCACerts != null && hasCACerts.booleanValue();
    }

    public boolean hasCACertInWorkProfile() {
        int userId = getWorkProfileUserId(this.mCurrentUserId);
        boolean z = false;
        if (userId == -10000) {
            return false;
        }
        Boolean hasCACerts = this.mHasCACerts.get(Integer.valueOf(userId));
        if (hasCACerts != null && hasCACerts.booleanValue()) {
            z = true;
        }
        return z;
    }

    public void removeCallback(SecurityController.SecurityControllerCallback callback) {
        synchronized (this.mCallbacks) {
            if (callback == null) {
                try {
                } catch (Throwable th) {
                    throw th;
                }
            } else {
                if (DEBUG) {
                    Log.d("SecurityController", "removeCallback " + callback);
                }
                this.mCallbacks.remove(callback);
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0032, code lost:
        return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void addCallback(com.android.systemui.statusbar.policy.SecurityController.SecurityControllerCallback r5) {
        /*
            r4 = this;
            java.util.ArrayList<com.android.systemui.statusbar.policy.SecurityController$SecurityControllerCallback> r0 = r4.mCallbacks
            monitor-enter(r0)
            if (r5 == 0) goto L_0x0031
            java.util.ArrayList<com.android.systemui.statusbar.policy.SecurityController$SecurityControllerCallback> r1 = r4.mCallbacks     // Catch:{ all -> 0x002f }
            boolean r1 = r1.contains(r5)     // Catch:{ all -> 0x002f }
            if (r1 == 0) goto L_0x000e
            goto L_0x0031
        L_0x000e:
            boolean r1 = DEBUG     // Catch:{ all -> 0x002f }
            if (r1 == 0) goto L_0x0028
            java.lang.String r1 = "SecurityController"
            java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch:{ all -> 0x002f }
            r2.<init>()     // Catch:{ all -> 0x002f }
            java.lang.String r3 = "addCallback "
            r2.append(r3)     // Catch:{ all -> 0x002f }
            r2.append(r5)     // Catch:{ all -> 0x002f }
            java.lang.String r2 = r2.toString()     // Catch:{ all -> 0x002f }
            android.util.Log.d(r1, r2)     // Catch:{ all -> 0x002f }
        L_0x0028:
            java.util.ArrayList<com.android.systemui.statusbar.policy.SecurityController$SecurityControllerCallback> r1 = r4.mCallbacks     // Catch:{ all -> 0x002f }
            r1.add(r5)     // Catch:{ all -> 0x002f }
            monitor-exit(r0)     // Catch:{ all -> 0x002f }
            return
        L_0x002f:
            r1 = move-exception
            goto L_0x0033
        L_0x0031:
            monitor-exit(r0)     // Catch:{ all -> 0x002f }
            return
        L_0x0033:
            monitor-exit(r0)     // Catch:{ all -> 0x002f }
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.systemui.statusbar.policy.SecurityControllerImpl.addCallback(com.android.systemui.statusbar.policy.SecurityController$SecurityControllerCallback):void");
    }

    public void onUserSwitched(int newUserId) {
        this.mCurrentUserId = newUserId;
        if (this.mUserManager.getUserInfo(newUserId).isRestricted()) {
            this.mVpnUserId = -10000;
        } else {
            this.mVpnUserId = this.mCurrentUserId;
        }
        refreshCACerts();
        fireCallbacks();
    }

    public String getVpnPackageName() {
        VpnConfig cfg = this.mCurrentVpns.get(this.mVpnUserId);
        if (cfg != null) {
            return cfg.user;
        }
        return null;
    }

    /* access modifiers changed from: private */
    public void refreshCACerts() {
        if (Build.VERSION.SDK_INT >= 26) {
            new CACertLoader().execute(new Integer[]{Integer.valueOf(this.mCurrentUserId)});
            int workProfileId = getWorkProfileUserId(this.mCurrentUserId);
            if (workProfileId != -10000) {
                new CACertLoader().execute(new Integer[]{Integer.valueOf(workProfileId)});
            }
        }
    }

    private String getNameForVpnConfig(VpnConfig cfg, UserHandle user) {
        if (cfg.legacy) {
            return this.mContext.getString(R.string.legacy_vpn_name);
        }
        String vpnPackage = cfg.user;
        try {
            return VpnConfig.getVpnLabel(this.mContext.createPackageContextAsUser(this.mContext.getPackageName(), 0, user), vpnPackage).toString();
        } catch (PackageManager.NameNotFoundException nnfe) {
            Log.e("SecurityController", "Package " + vpnPackage + " is not present", nnfe);
            return null;
        }
    }

    /* access modifiers changed from: private */
    public void fireCallbacks() {
        synchronized (this.mCallbacks) {
            Iterator<SecurityController.SecurityControllerCallback> it = this.mCallbacks.iterator();
            while (it.hasNext()) {
                it.next().onStateChanged();
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateState() {
        SparseArray<VpnConfig> vpns = new SparseArray<>();
        try {
            for (UserInfo user : this.mUserManager.getUsers()) {
                VpnConfig cfg = this.mConnectivityManagerService.getVpnConfig(user.id);
                if (cfg != null) {
                    if (cfg.legacy) {
                        LegacyVpnInfo legacyVpn = this.mConnectivityManagerService.getLegacyVpnInfo(user.id);
                        if (legacyVpn != null) {
                            if (legacyVpn.state != 3) {
                            }
                        }
                    }
                    vpns.put(user.id, cfg);
                }
            }
            this.mCurrentVpns = vpns;
        } catch (RemoteException rme) {
            Log.e("SecurityController", "Unable to list active VPNs", rme);
        }
    }
}
