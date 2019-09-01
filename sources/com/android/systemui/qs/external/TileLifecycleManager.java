package com.android.systemui.qs.external;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.service.quicksettings.IQSService;
import android.service.quicksettings.IQSTileService;
import android.service.quicksettings.Tile;
import android.util.ArraySet;
import android.util.Log;
import com.android.systemui.Util;
import java.util.Objects;
import java.util.Set;

public class TileLifecycleManager extends BroadcastReceiver implements ServiceConnection, IBinder.DeathRecipient, IQSTileService {
    private int mBindRetryDelay;
    private int mBindTryCount;
    /* access modifiers changed from: private */
    public boolean mBound;
    private TileChangeListener mChangeListener;
    private IBinder mClickBinder;
    private final Context mContext;
    private final Handler mHandler;
    private final Intent mIntent;
    private boolean mIsBound;
    private boolean mListening;
    private final PackageManagerAdapter mPackageManagerAdapter;
    private Set<Integer> mQueuedMessages;
    boolean mReceiverRegistered;
    private final IBinder mToken;
    private boolean mUnbindImmediate;
    private final UserHandle mUser;
    private QSTileServiceWrapper mWrapper;

    public interface TileChangeListener {
        void onTileChanged(ComponentName componentName);
    }

    public TileLifecycleManager(Handler handler, Context context, IQSService service, Tile tile, Intent intent, UserHandle user) {
        this(handler, context, service, tile, intent, user, new PackageManagerAdapter(context));
    }

    TileLifecycleManager(Handler handler, Context context, IQSService service, Tile tile, Intent intent, UserHandle user, PackageManagerAdapter packageManagerAdapter) {
        this.mToken = new Binder();
        this.mQueuedMessages = new ArraySet();
        this.mBindRetryDelay = 1000;
        this.mContext = context;
        this.mHandler = handler;
        this.mIntent = intent;
        this.mIntent.putExtra("service", service.asBinder());
        if (Build.VERSION.SDK_INT > 24) {
            this.mIntent.putExtra("token", this.mToken);
        } else {
            this.mIntent.putExtra("android.service.quicksettings.extra.COMPONENT", intent.getComponent());
        }
        this.mUser = user;
        this.mPackageManagerAdapter = packageManagerAdapter;
    }

    public ComponentName getComponent() {
        return this.mIntent.getComponent();
    }

    public boolean hasPendingClick() {
        boolean contains;
        synchronized (this.mQueuedMessages) {
            contains = this.mQueuedMessages.contains(2);
        }
        return contains;
    }

    public boolean isActiveTile() {
        boolean z = false;
        try {
            ServiceInfo info = this.mPackageManagerAdapter.getServiceInfo(this.mIntent.getComponent(), 8320);
            if (info.metaData != null && info.metaData.getBoolean("android.service.quicksettings.ACTIVE_TILE", false)) {
                z = true;
            }
            return z;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void flushMessagesAndUnbind() {
        this.mUnbindImmediate = true;
        setBindService(true);
    }

    public void setBindService(boolean bind) {
        if (!this.mBound || !this.mUnbindImmediate) {
            this.mBound = bind;
            if (!bind) {
                this.mBindTryCount = 0;
                this.mWrapper = null;
                if (this.mIsBound) {
                    this.mContext.unbindService(this);
                    this.mIsBound = false;
                }
            } else if (this.mBindTryCount == 5) {
                startPackageListening();
                return;
            } else if (checkComponentState()) {
                this.mBindTryCount++;
                try {
                    this.mIsBound = this.mContext.bindServiceAsUser(this.mIntent, this, 33554433, this.mUser);
                } catch (SecurityException e) {
                    Log.e("TileLifecycleManager", "Failed to bind to service", e);
                    this.mIsBound = false;
                }
            } else {
                return;
            }
            return;
        }
        if (!Util.isMiuiOptimizationDisabled()) {
            this.mUnbindImmediate = false;
        }
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        this.mBindTryCount = 0;
        QSTileServiceWrapper wrapper = new QSTileServiceWrapper(IQSTileService.Stub.asInterface(service));
        try {
            service.linkToDeath(this, 0);
        } catch (RemoteException e) {
        }
        this.mWrapper = wrapper;
        handlePendingMessages();
    }

    public void onServiceDisconnected(ComponentName name) {
        handleDeath();
    }

    private void handlePendingMessages() {
        ArraySet<Integer> queue;
        synchronized (this.mQueuedMessages) {
            queue = new ArraySet<>(this.mQueuedMessages);
            this.mQueuedMessages.clear();
        }
        if (queue.contains(0)) {
            onTileAdded();
        }
        if (this.mListening) {
            onStartListening();
        }
        if (queue.contains(2)) {
            if (!this.mListening) {
                Log.w("TileLifecycleManager", "Managed to get click on non-listening state...");
            } else {
                onClick(this.mClickBinder);
            }
        }
        if (queue.contains(3)) {
            if (!this.mListening) {
                Log.w("TileLifecycleManager", "Managed to get unlock on non-listening state...");
            } else {
                onUnlockComplete();
            }
        }
        if (queue.contains(1)) {
            if (this.mListening) {
                Log.w("TileLifecycleManager", "Managed to get remove in listening state...");
                onStopListening();
            }
            onTileRemoved();
        }
        if (this.mUnbindImmediate) {
            this.mUnbindImmediate = false;
            setBindService(false);
        }
    }

    public void handleDestroy() {
        if (this.mReceiverRegistered) {
            stopPackageListening();
        }
    }

    private void handleDeath() {
        if (this.mWrapper != null) {
            this.mWrapper = null;
            if (this.mBound && checkComponentState()) {
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        if (TileLifecycleManager.this.mBound) {
                            TileLifecycleManager.this.setBindService(true);
                        }
                    }
                }, (long) this.mBindRetryDelay);
            }
        }
    }

    private boolean checkComponentState() {
        if (isPackageAvailable() && isComponentAvailable()) {
            return true;
        }
        startPackageListening();
        return false;
    }

    private void startPackageListening() {
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_CHANGED");
        filter.addDataScheme("package");
        this.mContext.registerReceiverAsUser(this, this.mUser, filter, null, this.mHandler);
        this.mContext.registerReceiverAsUser(this, this.mUser, new IntentFilter("android.intent.action.USER_UNLOCKED"), null, this.mHandler);
        this.mReceiverRegistered = true;
    }

    private void stopPackageListening() {
        this.mContext.unregisterReceiver(this);
        this.mReceiverRegistered = false;
    }

    public void setTileChangeListener(TileChangeListener changeListener) {
        this.mChangeListener = changeListener;
    }

    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction()) || Objects.equals(intent.getData().getEncodedSchemeSpecificPart(), this.mIntent.getComponent().getPackageName())) {
            if ("android.intent.action.PACKAGE_CHANGED".equals(intent.getAction()) && this.mChangeListener != null) {
                this.mChangeListener.onTileChanged(this.mIntent.getComponent());
            }
            stopPackageListening();
            if (this.mBound) {
                setBindService(true);
            }
        }
    }

    private boolean isComponentAvailable() {
        String packageName = this.mIntent.getComponent().getPackageName();
        boolean z = false;
        try {
            if (this.mPackageManagerAdapter.getServiceInfo(this.mIntent.getComponent(), 0, this.mUser.getIdentifier()) != null) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            return false;
        }
    }

    private boolean isPackageAvailable() {
        String packageName = this.mIntent.getComponent().getPackageName();
        try {
            this.mPackageManagerAdapter.getPackageInfoAsUser(packageName, 0, this.mUser.getIdentifier());
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("TileLifecycleManager", "Package not available: " + packageName);
            return false;
        }
    }

    private void queueMessage(int message) {
        synchronized (this.mQueuedMessages) {
            this.mQueuedMessages.add(Integer.valueOf(message));
        }
    }

    public void onTileAdded() {
        if (this.mWrapper == null || !this.mWrapper.onTileAdded()) {
            queueMessage(0);
            handleDeath();
        }
    }

    public void onTileRemoved() {
        if (this.mWrapper == null || !this.mWrapper.onTileRemoved()) {
            queueMessage(1);
            handleDeath();
        }
    }

    public void onStartListening() {
        this.mListening = true;
        if (this.mWrapper != null && !this.mWrapper.onStartListening()) {
            handleDeath();
        }
    }

    public void onStopListening() {
        this.mListening = false;
        if (this.mWrapper != null && !this.mWrapper.onStopListening()) {
            handleDeath();
        }
    }

    public void onClick(IBinder iBinder) {
        if (this.mWrapper == null || !this.mWrapper.onClick(iBinder)) {
            this.mClickBinder = iBinder;
            queueMessage(2);
            handleDeath();
        }
    }

    public void onUnlockComplete() {
        if (this.mWrapper == null || !this.mWrapper.onUnlockComplete()) {
            queueMessage(3);
            handleDeath();
        }
    }

    public IBinder asBinder() {
        if (this.mWrapper != null) {
            return this.mWrapper.asBinder();
        }
        return null;
    }

    public void binderDied() {
        handleDeath();
    }

    public IBinder getToken() {
        return this.mToken;
    }

    public static boolean isTileAdded(Context context, ComponentName component) {
        return context.getSharedPreferences("tiles_prefs", 0).getBoolean(component.flattenToString(), false);
    }

    public static void setTileAdded(Context context, ComponentName component, boolean added) {
        context.getSharedPreferences("tiles_prefs", 0).edit().putBoolean(component.flattenToString(), added).commit();
    }
}
