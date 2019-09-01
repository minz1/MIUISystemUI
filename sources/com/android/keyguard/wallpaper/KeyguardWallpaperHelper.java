package com.android.keyguard.wallpaper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.magazine.LockScreenMagazineUtils;
import com.android.keyguard.utils.ContentProviderUtils;
import com.android.keyguard.utils.HomeUtils;
import com.android.keyguard.utils.PortableUtils;
import com.android.keyguard.utils.ThemeUtils;
import com.android.keyguard.wallpaper.mode.RequestInfo;
import com.android.keyguard.wallpaper.mode.ResultInfo;
import com.android.keyguard.wallpaper.mode.WallpaperInfo;
import com.google.gson.Gson;
import java.util.List;
import miui.os.Build;

public class KeyguardWallpaperHelper extends KeyguardUpdateMonitorCallback {
    /* access modifiers changed from: private */
    public Context mContext;
    private Gson mGson = new Gson();
    /* access modifiers changed from: private */
    public boolean mIsChangingLockWallpaper = false;
    private KeyguardUpdateMonitor mKeyguardUpdateMonitor;

    public KeyguardWallpaperHelper(Context context) {
        this.mContext = context;
        this.mKeyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mKeyguardUpdateMonitor.registerCallback(this);
    }

    public void onStartedGoingToSleep(int why) {
        LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(this.mContext, "Screen_OFF");
        if (KeyguardWallpaperUtils.isDefaultLockStyle(this.mContext) && !this.mIsChangingLockWallpaper && Build.IS_INTERNATIONAL_BUILD && this.mKeyguardUpdateMonitor.isSupportLockScreenMagazineLeft()) {
            final String provider = WallpaperAuthorityUtils.getWallpaperAuthority();
            if (!TextUtils.isEmpty(provider) && WallpaperAuthorityUtils.isLockScreenMagazineOpenedWallpaper()) {
                Context context = this.mContext;
                if (ContentProviderUtils.isProviderExists(context, Uri.parse("content://" + provider))) {
                    new AsyncTask<Void, Void, Boolean>() {
                        /* access modifiers changed from: protected */
                        public Boolean doInBackground(Void... params) {
                            boolean unused = KeyguardWallpaperHelper.this.mIsChangingLockWallpaper = true;
                            return Boolean.valueOf(KeyguardWallpaperHelper.this.setLockWallpaperFromProvider(provider, false, false));
                        }

                        /* access modifiers changed from: protected */
                        public void onPostExecute(Boolean result) {
                            boolean unused = KeyguardWallpaperHelper.this.mIsChangingLockWallpaper = false;
                            if (result.booleanValue()) {
                                Intent intent = new Intent("com.miui.keyguard.setwallpaper");
                                intent.putExtra("set_lock_wallpaper_result", result);
                                KeyguardWallpaperHelper.this.mContext.sendBroadcast(intent);
                                ThemeUtils.tellThemeLockWallpaperPath(KeyguardWallpaperHelper.this.mContext, "");
                            }
                        }

                        /* access modifiers changed from: protected */
                        public void onCancelled() {
                            boolean unused = KeyguardWallpaperHelper.this.mIsChangingLockWallpaper = false;
                        }
                    }.execute(new Void[0]);
                }
            }
        }
    }

    public void onStartedWakingUp() {
        LockScreenMagazineUtils.sendLockScreenMagazineEventBrodcast(this.mContext, "Screen_ON");
    }

    /* access modifiers changed from: private */
    public boolean setLockWallpaperFromProvider(String providerName, boolean forceRefresh, boolean needLast) {
        Uri wallpaperUri;
        String str = providerName;
        try {
            String contentUri = "content://" + HomeUtils.HOME_LAUNCHER_SETTINGS_AUTHORITY + "/" + "preference";
            String currentWallpaperInfo = PortableUtils.getCurrentWallpaperInfo(this.mContext, contentUri);
            RequestInfo requestInfo = new RequestInfo();
            requestInfo.mode = 1;
            requestInfo.currentWallpaperInfo = (WallpaperInfo) this.mGson.fromJson(currentWallpaperInfo, WallpaperInfo.class);
            try {
                requestInfo.needLast = needLast;
                requestInfo.packageName = "com.android.systemui";
                String requestJson = this.mGson.toJson(requestInfo);
                Bundle extras = new Bundle();
                try {
                    extras.putBoolean("force_refresh", forceRefresh);
                    extras.putString("extra_current_provider", str);
                    extras.putString("request_json", requestJson);
                    if (!ContentProviderUtils.isProviderExists(this.mContext, Uri.parse("content://" + str))) {
                        return false;
                    }
                    Bundle result = ContentProviderUtils.getResultFromProvider(this.mContext, "content://" + str, "getNextLockWallpaperUri", (String) null, extras);
                    if (result == null) {
                        return false;
                    }
                    String resultJson = result.getString("result_json");
                    if (!TextUtils.isEmpty(resultJson)) {
                        ResultInfo resultInfo = (ResultInfo) this.mGson.fromJson(resultJson, ResultInfo.class);
                        if (resultInfo == null) {
                            return false;
                        }
                        List<WallpaperInfo> wallpaperList = resultInfo.wallpaperInfos;
                        if (wallpaperList != null) {
                            if (wallpaperList.size() > 0) {
                                WallpaperInfo info = wallpaperList.get(0);
                                String uriString = info.wallpaperUri;
                                if (TextUtils.isEmpty(uriString)) {
                                    return false;
                                }
                                wallpaperUri = Uri.parse(uriString);
                                PortableUtils.updateCurrentWallpaperInfo(this.mContext, this.mGson.toJson(info), contentUri);
                                String str2 = uriString;
                            }
                        }
                        return false;
                    }
                    String uriString2 = result.getString("result_string");
                    if (TextUtils.isEmpty(uriString2)) {
                        return false;
                    }
                    wallpaperUri = Uri.parse(uriString2);
                    PortableUtils.updateCurrentWallpaperInfo(this.mContext, "", contentUri);
                    return KeyguardWallpaperUtils.setLockWallpaper(this.mContext, wallpaperUri, true);
                } catch (Exception e) {
                    e = e;
                    Log.e("KeyguardWallpaperHelper", "setLockWallpaperFromProvider", e);
                    return false;
                }
            } catch (Exception e2) {
                e = e2;
                boolean z = forceRefresh;
                Log.e("KeyguardWallpaperHelper", "setLockWallpaperFromProvider", e);
                return false;
            }
        } catch (Exception e3) {
            e = e3;
            boolean z2 = forceRefresh;
            boolean z3 = needLast;
            Log.e("KeyguardWallpaperHelper", "setLockWallpaperFromProvider", e);
            return false;
        }
    }
}
