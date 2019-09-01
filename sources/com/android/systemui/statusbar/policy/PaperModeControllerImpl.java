package com.android.systemui.statusbar.policy;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import com.android.systemui.settings.CurrentUserTracker;
import com.android.systemui.statusbar.policy.PaperModeController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class PaperModeControllerImpl extends CurrentUserTracker implements PaperModeController {
    private static final boolean DEBUG = Log.isLoggable("PaperModeController", 3);
    private Handler mBgHandler;
    /* access modifiers changed from: private */
    public final ContentObserver mGameModeObserver;
    private final ArrayList<PaperModeController.PaperModeListener> mListeners = new ArrayList<>();
    /* access modifiers changed from: private */
    public boolean mPaperModeAvailable;
    /* access modifiers changed from: private */
    public boolean mPaperModeEnabled;
    /* access modifiers changed from: private */
    public final ContentObserver mPaperModeObserver;
    /* access modifiers changed from: private */
    public ContentResolver mResolver;

    public PaperModeControllerImpl(Context context, Looper bgLooper) {
        super(context);
        this.mBgHandler = new Handler(bgLooper);
        this.mResolver = context.getContentResolver();
        this.mPaperModeObserver = new ContentObserver(this.mBgHandler) {
            public void onChange(boolean selfChange) {
                boolean z = false;
                int paperMode = Settings.System.getIntForUser(PaperModeControllerImpl.this.mResolver, "screen_paper_mode_enabled", 0, -2);
                PaperModeControllerImpl paperModeControllerImpl = PaperModeControllerImpl.this;
                if (paperMode != 0) {
                    z = true;
                }
                boolean unused = paperModeControllerImpl.mPaperModeEnabled = z;
                PaperModeControllerImpl.this.dispatchModeChanged(PaperModeControllerImpl.this.mPaperModeEnabled);
            }
        };
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_paper_mode_enabled"), false, this.mPaperModeObserver, -1);
        this.mGameModeObserver = new ContentObserver(this.mBgHandler) {
            public void onChange(boolean selfChange) {
                boolean z = false;
                int gameMode = Settings.System.getIntForUser(PaperModeControllerImpl.this.mResolver, "screen_game_mode", 0, -2);
                PaperModeControllerImpl paperModeControllerImpl = PaperModeControllerImpl.this;
                if ((gameMode & 1) == 0) {
                    z = true;
                }
                boolean unused = paperModeControllerImpl.mPaperModeAvailable = z;
                PaperModeControllerImpl.this.dispatchAvailabilityChanged(PaperModeControllerImpl.this.mPaperModeAvailable);
            }
        };
        this.mResolver.registerContentObserver(Settings.System.getUriFor("screen_game_mode"), false, this.mGameModeObserver, -1);
        postInitPaperModeState();
        startTracking();
    }

    public boolean isAvailable() {
        return this.mPaperModeAvailable;
    }

    public boolean isEnabled() {
        return this.mPaperModeEnabled;
    }

    public void setEnabled(boolean enabled) {
        int i;
        if (this.mPaperModeEnabled != enabled && this.mPaperModeAvailable) {
            ContentResolver contentResolver = this.mResolver;
            if (enabled) {
                i = 1;
            } else {
                i = 0;
            }
            Settings.System.putIntForUser(contentResolver, "screen_paper_mode_enabled", i, -2);
        }
    }

    public void onUserSwitched(int newUserId) {
        postInitPaperModeState();
    }

    private void postInitPaperModeState() {
        this.mBgHandler.post(new Runnable() {
            public void run() {
                PaperModeControllerImpl.this.mPaperModeObserver.onChange(false);
                PaperModeControllerImpl.this.mGameModeObserver.onChange(false);
            }
        });
    }

    public void addCallback(PaperModeController.PaperModeListener listener) {
        if (listener != null && !this.mListeners.contains(listener)) {
            synchronized (this.mListeners) {
                this.mListeners.add(listener);
            }
            listener.onPaperModeAvailabilityChanged(isAvailable());
            listener.onPaperModeChanged(this.mPaperModeEnabled);
        }
    }

    public void removeCallback(PaperModeController.PaperModeListener listener) {
        if (listener != null) {
            synchronized (this.mListeners) {
                this.mListeners.remove(listener);
            }
        }
    }

    /* access modifiers changed from: private */
    public void dispatchModeChanged(boolean enabled) {
        dispatchListeners(0, enabled);
    }

    /* access modifiers changed from: private */
    public void dispatchAvailabilityChanged(boolean available) {
        dispatchListeners(1, available);
    }

    private void dispatchListeners(int message, boolean argument) {
        synchronized (this.mListeners) {
            Iterator<PaperModeController.PaperModeListener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                PaperModeController.PaperModeListener listener = it.next();
                if (message == 0) {
                    listener.onPaperModeChanged(argument);
                } else if (message == 1) {
                    listener.onPaperModeAvailabilityChanged(argument);
                }
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("PaperModeController state:");
        pw.print("  mPaperModeEnabled=");
        pw.println(this.mPaperModeEnabled);
        pw.print("  isAvailable=");
        pw.println(this.mPaperModeAvailable);
    }
}
