package com.android.keyguard;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.negative.MiuiKeyguardMoveLeftControlCenterView;
import com.android.keyguard.utils.ContentProviderUtils;
import com.android.keyguard.utils.PackageUtils;
import com.android.systemui.Application;
import com.android.systemui.statusbar.phone.StatusBar;
import miui.util.Log;

public class WallpaperProvider extends ContentProvider {
    public boolean onCreate() {
        return true;
    }

    public Bundle call(String method, String arg, Bundle extras) {
        Intent intent;
        String str;
        Log.d("WallpaperProvider", "call method = " + method);
        Bundle bundle = new Bundle();
        boolean z = false;
        if (method.equals("SET_LOCK_SCREEN_MAGAZINE_STATUS")) {
            LockScreenMagazineUtils.setLockScreenMagazineStatus(getContext(), extras.getBoolean("status", false));
        } else if (method.equals("GET_ELECTRIC_TORCH_STATUS")) {
            if (Settings.Global.getInt(getContext().getContentResolver(), "torch_state", 0) != 0) {
                z = true;
            }
            bundle.putBoolean("electric_torch_status", z);
        } else if (method.equals("SET_ELECTRIC_TORCH_STATUS")) {
            try {
                getContext().sendBroadcast(PackageUtils.getToggleTorchIntent(extras.getBoolean("status", false)));
            } catch (Exception e) {
                Log.e("WallpaperProvider", "call METHOD_SET_ELECTRIC_TORCH_STATUS", e);
            }
        } else if (method.equals("CHECK_TSM_CLIENT_STATUS")) {
            try {
                if (PackageUtils.resolveIntent(getContext(), PackageUtils.getTSMClientIntent()) != null) {
                    z = true;
                }
                bundle.putBoolean("TSM_client_status", z);
            } catch (Exception e2) {
                Log.e("WallpaperProvider", "call METHOD_CHECK_TSM_CLIENT_STATUS", e2);
            }
        } else if (method.equals("OPEN_TSM_CLIENT")) {
            try {
                getContext().startActivityAsUser(PackageUtils.getTSMClientIntent(), UserHandle.CURRENT);
            } catch (Exception e3) {
                Log.e("WallpaperProvider", "call METHOD_OPEN_TSM_CLIENT", e3);
            }
        } else if (method.equals("CHECK_SMART_HOME_STATUS")) {
            boolean isShow = false;
            String devicesNumString = "";
            if (PackageUtils.isAppInstalledForUser(getContext(), "com.xiaomi.smarthome", KeyguardUpdateMonitor.getCurrentUser()) && MiuiKeyguardUtils.isRegionSupportMiHome(getContext())) {
                isShow = true;
                Bundle countBundle = ContentProviderUtils.getResultFromProvider(getContext(), MiuiKeyguardUtils.maybeAddUserId(MiuiKeyguardMoveLeftControlCenterView.KEYGUARD_SMART_HOME, KeyguardUpdateMonitor.getCurrentUser()), "online_devices_count", (String) null, (Bundle) null);
                if (countBundle == null) {
                    str = "";
                } else {
                    str = countBundle.getString("count", "");
                }
                devicesNumString = str;
            }
            bundle.putBoolean("smart_home_status", isShow);
            if (isShow) {
                bundle.putString("smart_home_online_devices_count", devicesNumString);
            }
        } else if (method.equals("OPEN_SMART_HOME")) {
            try {
                if (PackageUtils.isAppInstalledForUser(getContext(), "com.xiaomi.smarthome", KeyguardUpdateMonitor.getCurrentUser())) {
                    intent = PackageUtils.getSmartHomeMainIntent();
                } else {
                    intent = PackageUtils.getMarketDownloadIntent("com.xiaomi.smarthome");
                }
                StatusBar statusBar = (StatusBar) ((Application) getContext().getApplicationContext()).getSystemUIApplication().getComponent(StatusBar.class);
                if (statusBar != null) {
                    statusBar.startActivity(intent, true);
                }
            } catch (Exception e4) {
                Log.e("WallpaperProvider", "call METHOD_OPEN_SMART_HOME", e4);
            }
        }
        return bundle;
    }

    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    public String getType(Uri uri) {
        return null;
    }
}
