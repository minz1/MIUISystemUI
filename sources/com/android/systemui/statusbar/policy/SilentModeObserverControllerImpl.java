package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.provider.MiuiSettings;
import android.util.Log;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.SilentModeObserverController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import miui.provider.ExtraTelephony;

public class SilentModeObserverControllerImpl implements SilentModeObserverController {
    private static final boolean DEBUG = Log.isLoggable("SilentModeController", 3);
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public boolean mEnabled;
    private final ArrayList<WeakReference<SilentModeObserverController.SilentModeListener>> mListeners = new ArrayList<>(1);
    private ExtraTelephony.QuietModeEnableListener mQuietModeObserver;
    private CurrentUserTracker mUserTracker;

    public SilentModeObserverControllerImpl(Context context) {
        this.mContext = context;
        this.mQuietModeObserver = new ExtraTelephony.QuietModeEnableListener() {
            public void onQuietModeEnableChange(boolean isMode) {
                boolean unused = SilentModeObserverControllerImpl.this.mEnabled = isMode;
                SilentModeObserverControllerImpl.this.dispatchListeners(isMode);
            }
        };
        ExtraTelephony.registerQuietModeEnableListener(this.mContext, this.mQuietModeObserver);
        this.mEnabled = MiuiSettings.SilenceMode.isSilenceModeEnable(this.mContext);
        this.mUserTracker = new CurrentUserTracker(this.mContext) {
            public void onUserSwitched(int newUserId) {
                boolean unused = SilentModeObserverControllerImpl.this.mEnabled = MiuiSettings.SilenceMode.isSilenceModeEnable(SilentModeObserverControllerImpl.this.mContext);
                SilentModeObserverControllerImpl.this.dispatchListeners(SilentModeObserverControllerImpl.this.mEnabled);
            }
        };
        this.mUserTracker.startTracking();
    }

    public void addCallback(SilentModeObserverController.SilentModeListener l) {
        synchronized (this.mListeners) {
            cleanUpListenersLocked(l);
            this.mListeners.add(new WeakReference(l));
            l.onSilentModeChanged(this.mEnabled);
        }
    }

    public void removeCallback(SilentModeObserverController.SilentModeListener l) {
        synchronized (this.mListeners) {
            cleanUpListenersLocked(l);
        }
    }

    /* access modifiers changed from: private */
    public void dispatchListeners(boolean enable) {
        synchronized (this.mListeners) {
            int N = this.mListeners.size();
            boolean cleanup = false;
            for (int i = 0; i < N; i++) {
                SilentModeObserverController.SilentModeListener l = (SilentModeObserverController.SilentModeListener) this.mListeners.get(i).get();
                if (l != null) {
                    l.onSilentModeChanged(enable);
                } else {
                    cleanup = true;
                }
            }
            if (cleanup) {
                cleanUpListenersLocked(null);
            }
        }
    }

    private void cleanUpListenersLocked(SilentModeObserverController.SilentModeListener listener) {
        for (int i = this.mListeners.size() - 1; i >= 0; i--) {
            SilentModeObserverController.SilentModeListener found = (SilentModeObserverController.SilentModeListener) this.mListeners.get(i).get();
            if (found == null || found == listener) {
                this.mListeners.remove(i);
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("SilentModeObserverController state:");
    }
}
