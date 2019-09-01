package com.android.systemui.statusbar;

import android.app.MiuiStatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.Util;
import com.android.systemui.plugins.R;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.DarkIconDispatcherHelper;
import com.android.systemui.statusbar.policy.DemoModeController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class NetworkSpeedView extends TextView implements DemoMode, DarkIconDispatcher.DarkReceiver {
    private static ArrayList<NetworkSpeedView> sViewList;
    Handler mBgHandler;
    private ConnectivityManager mConnectivityManager = ((ConnectivityManager) this.mContext.getSystemService("connectivity"));
    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        public void onReceive(Context content, Intent intent) {
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                if (intent.hasExtra("noConnectivity")) {
                    boolean unused = NetworkSpeedView.this.mIsNetworkConnected = !intent.getBooleanExtra("noConnectivity", false);
                    NetworkSpeedView.this.postUpdateNetworkSpeed();
                } else if (NetworkSpeedView.this.mBgHandler != null) {
                    NetworkSpeedView.this.mBgHandler.removeMessages(R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu);
                    NetworkSpeedView.this.mBgHandler.sendEmptyMessage(R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu);
                    NetworkSpeedView.this.postUpdateNetworkSpeed();
                }
            } else if ("android.intent.action.USER_SWITCHED".equals(intent.getAction()) && NetworkSpeedView.this.mBgHandler != null) {
                NetworkSpeedView.this.mBgHandler.removeMessages(100);
                NetworkSpeedView.this.mBgHandler.sendEmptyMessage(100);
                NetworkSpeedView.this.postUpdateNetworkSpeed();
            }
        }
    };
    private final DemoModeController.DemoModeCallback mDemoCallback = new DemoModeController.DemoModeCallback() {
        public void onDemoModeChanged(String command, Bundle args) {
            NetworkSpeedView.this.dispatchDemoCommand(command, args);
        }
    };
    private boolean mDemoMode;
    private boolean mDisabled;
    private boolean mForceHide;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == 200000) {
                int i = 0;
                boolean visible = msg.arg1 != 0;
                NetworkSpeedView networkSpeedView = NetworkSpeedView.this;
                if (!visible) {
                    i = 8;
                }
                networkSpeedView.setVisibilityToViewList(i);
                if (visible) {
                    NetworkSpeedView.this.setTextToViewList(NetworkSpeedView.formatSpeed(NetworkSpeedView.this.mContext, ((Long) msg.obj).longValue()));
                }
            }
        }
    };
    private boolean mIsDriveMode;
    private boolean mIsFirst = false;
    /* access modifiers changed from: private */
    public boolean mIsNetworkConnected;
    private long mLastTime;
    private int mMaxLength;
    private ContentObserver mNetworkSpeedObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            if (NetworkSpeedView.this.mBgHandler != null) {
                NetworkSpeedView.this.mBgHandler.removeMessages(R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle);
                NetworkSpeedView.this.mBgHandler.sendEmptyMessage(R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle);
                NetworkSpeedView.this.postUpdateNetworkSpeed();
            }
        }
    };
    private int mNetworkUpdateInterval;
    private Uri mNetworkUri;
    private boolean mNotch;
    private long mTotalBytes;
    private LinkedList<NetworkSpeedVisibilityListener> mVisibilityListeners = new LinkedList<>();

    public interface NetworkSpeedVisibilityListener {
        void onNetworkSpeedVisibilityChanged(boolean z);
    }

    private final class WorkHandler extends Handler {
        WorkHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case R.styleable.AppCompatTheme_textAppearancePopupMenuHeader:
                    NetworkSpeedView.this.updateSwitchState();
                    return;
                case R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle:
                    NetworkSpeedView.this.updateSwitchState();
                    NetworkSpeedView.this.updateInterval();
                    return;
                case R.styleable.AppCompatTheme_textAppearanceSearchResultTitle:
                    NetworkSpeedView.this.updateNetworkSpeed();
                    return;
                case R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu:
                    NetworkSpeedView.this.updateConnectedState();
                    return;
                default:
                    return;
            }
        }
    }

    public NetworkSpeedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initNetworkAssistantProviderUri();
    }

    private void addToViewList() {
        if (sViewList == null) {
            this.mIsFirst = true;
            sViewList = new ArrayList<>();
            this.mBgHandler = new WorkHandler((Looper) Dependency.get(Dependency.NET_BG_LOOPER));
        }
        sViewList.add(this);
    }

    private void removeFromViewList() {
        if (sViewList != null) {
            sViewList.remove(this);
            if (this.mIsFirst) {
                if (this.mBgHandler != null) {
                    this.mBgHandler.removeCallbacksAndMessages(null);
                    this.mBgHandler = null;
                }
                sViewList.clear();
                sViewList = null;
            }
        }
    }

    /* access modifiers changed from: private */
    public void setTextToViewList(CharSequence text) {
        if (sViewList != null) {
            Iterator<NetworkSpeedView> it = sViewList.iterator();
            while (it.hasNext()) {
                it.next().setText(text);
            }
        }
    }

    /* access modifiers changed from: private */
    public void setVisibilityToViewList(int visibility) {
        if (sViewList != null) {
            Iterator<NetworkSpeedView> it = sViewList.iterator();
            while (it.hasNext()) {
                NetworkSpeedView view = it.next();
                if (!view.mIsDriveMode && !view.mNotch && !view.mForceHide) {
                    view.setVisibility(visibility);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        addToViewList();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiverAsUser(this.mConnectivityReceiver, UserHandle.CURRENT, filter, null, this.mBgHandler);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_show_network_speed"), true, this.mNetworkSpeedObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("status_bar_network_speed_interval"), true, this.mNetworkSpeedObserver, -1);
        this.mNetworkSpeedObserver.onChange(true);
        ((DemoModeController) Dependency.get(DemoModeController.class)).addCallback(this.mDemoCallback);
    }

    /* access modifiers changed from: protected */
    public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        int length = TextUtils.isEmpty(text) ? 0 : (int) Math.ceil((double) getPaint().measureText(text.toString()));
        if (this.mMaxLength != length) {
            this.mMaxLength = length;
            setWidth(this.mMaxLength);
        }
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mContext.unregisterReceiver(this.mConnectivityReceiver);
        this.mContext.getContentResolver().unregisterContentObserver(this.mNetworkSpeedObserver);
        ((DemoModeController) Dependency.get(DemoModeController.class)).removeCallback(this.mDemoCallback);
        removeFromViewList();
    }

    private void initNetworkAssistantProviderUri() {
        this.mNetworkUri = Uri.parse("content://com.miui.networkassistant.provider/na_traffic_stats");
    }

    private long getTotalByte() {
        Cursor cursor = this.mContext.getContentResolver().query(this.mNetworkUri, null, null, null, null);
        long totalByte = 0;
        boolean hasException = false;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    totalByte = cursor.getLong(cursor.getColumnIndex("total_tx_byte")) + cursor.getLong(cursor.getColumnIndex("total_rx_byte"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                hasException = true;
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
            cursor.close();
        }
        if (hasException || cursor == null) {
            return TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
        }
        return totalByte;
    }

    /* access modifiers changed from: private */
    public void postUpdateNetworkSpeed() {
        postUpdateNetworkSpeedDelay(0);
    }

    private void postUpdateNetworkSpeedDelay(long delay) {
        if (this.mBgHandler != null) {
            this.mBgHandler.removeMessages(R.styleable.AppCompatTheme_textAppearanceSearchResultTitle);
            this.mBgHandler.sendEmptyMessageDelayed(R.styleable.AppCompatTheme_textAppearanceSearchResultTitle, delay);
        }
    }

    /* access modifiers changed from: private */
    public void updateSwitchState() {
        this.mDisabled = !MiuiStatusBarManager.isShowNetworkSpeedForUser(this.mContext, -2);
    }

    /* access modifiers changed from: private */
    public void updateInterval() {
        this.mNetworkUpdateInterval = Settings.System.getInt(this.mContext.getContentResolver(), "status_bar_network_speed_interval", 4000);
    }

    /* access modifiers changed from: private */
    public void updateNetworkSpeed() {
        if (!this.mDemoMode && !this.mIsDriveMode && this.mIsFirst) {
            Message msg = Message.obtain();
            msg.what = 200000;
            if (this.mDisabled || !this.mIsNetworkConnected) {
                msg.arg1 = 0;
                this.mHandler.removeMessages(200000);
                this.mHandler.sendMessage(msg);
                this.mLastTime = 0;
                this.mTotalBytes = 0;
                return;
            }
            long currentTime = System.currentTimeMillis();
            long totalBytes = getTotalByte();
            if (totalBytes == 0) {
                this.mLastTime = 0;
                this.mTotalBytes = 0;
                totalBytes = getTotalByte();
            }
            long currentSpeed = 0;
            if (!(this.mLastTime == 0 || currentTime <= this.mLastTime || this.mTotalBytes == 0 || totalBytes == 0 || totalBytes <= this.mTotalBytes)) {
                currentSpeed = ((totalBytes - this.mTotalBytes) * 1000) / (currentTime - this.mLastTime);
            }
            msg.arg1 = 1;
            msg.obj = Long.valueOf(currentSpeed);
            this.mHandler.removeMessages(200000);
            this.mHandler.sendMessage(msg);
            this.mLastTime = currentTime;
            this.mTotalBytes = totalBytes;
            postUpdateNetworkSpeedDelay((long) this.mNetworkUpdateInterval);
        }
    }

    /* access modifiers changed from: private */
    public void updateConnectedState() {
        NetworkInfo networkInfo = this.mConnectivityManager.getActiveNetworkInfo();
        this.mIsNetworkConnected = networkInfo != null && networkInfo.isConnected();
    }

    /* access modifiers changed from: private */
    public static String formatSpeed(Context context, long number) {
        String value;
        int suffix = com.android.systemui.R.string.kilobyte_per_second;
        float result = ((float) number) / 1024.0f;
        if (result > 999.0f) {
            suffix = com.android.systemui.R.string.megabyte_per_second;
            result /= 1024.0f;
        }
        if (result < 100.0f) {
            value = String.format("%.1f", new Object[]{Float.valueOf(result)});
        } else {
            value = String.format("%.0f", new Object[]{Float.valueOf(result)});
        }
        return context.getResources().getString(com.android.systemui.R.string.network_speed_suffix, new Object[]{value, context.getString(suffix)});
    }

    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        boolean showCtsSpecifiedColor = Util.showCtsSpecifiedColor();
        int i = com.android.systemui.R.color.status_bar_textColor;
        if (showCtsSpecifiedColor) {
            Resources resources = getResources();
            if (DarkIconDispatcherHelper.inDarkMode(area, this, darkIntensity)) {
                i = com.android.systemui.R.color.status_bar_icon_text_color_dark_mode_cts;
            }
            setTextColor(resources.getColor(i));
            return;
        }
        Resources resources2 = getResources();
        if (DarkIconDispatcherHelper.inDarkMode(area, this, darkIntensity)) {
            i = com.android.systemui.R.color.status_bar_textColor_darkmode;
        }
        setTextColor(resources2.getColor(i));
    }

    public void setDriveMode(boolean isDriveMode) {
        this.mIsDriveMode = isDriveMode;
        if (this.mIsDriveMode) {
            setVisibility(8);
        } else {
            postUpdateNetworkSpeed();
        }
    }

    public void setNotch() {
        this.mNotch = true;
        setVisibility(8);
    }

    public void dispatchDemoCommand(String command, Bundle args) {
        Log.d("demo_mode", "NetworkSpeedView mDemoMode = " + this.mDemoMode + ", command = " + command);
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            setVisibility(8);
        } else if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            postUpdateNetworkSpeed();
        }
    }

    /* access modifiers changed from: protected */
    public void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        Iterator it = this.mVisibilityListeners.iterator();
        while (it.hasNext()) {
            ((NetworkSpeedVisibilityListener) it.next()).onNetworkSpeedVisibilityChanged(isShown());
        }
    }
}
