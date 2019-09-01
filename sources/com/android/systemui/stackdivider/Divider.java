package com.android.systemui.stackdivider;

import android.content.res.Configuration;
import android.os.RemoteException;
import android.util.Log;
import android.view.IDockedStackListener;
import android.view.LayoutInflater;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.Recents;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class Divider extends SystemUI {
    /* access modifiers changed from: private */
    public boolean mAdjustedForIme = false;
    private final DividerState mDividerState = new DividerState();
    private DockDividerVisibilityListener mDockDividerVisibilityListener;
    private DockedStackExistsChangedListener mDockedStackExistsChangedListener;
    private boolean mExists;
    /* access modifiers changed from: private */
    public ForcedResizableInfoActivityController mForcedResizableController;
    /* access modifiers changed from: private */
    public boolean mMinimized = false;
    /* access modifiers changed from: private */
    public DividerView mView;
    /* access modifiers changed from: private */
    public boolean mVisible = false;
    private DividerWindowManager mWindowManager;

    class DockDividerVisibilityListener extends IDockedStackListener.Stub {
        DockDividerVisibilityListener() {
        }

        public void onDividerVisibilityChanged(boolean visible) throws RemoteException {
            Log.d("Divider", "onDividerVisibilityChanged visible=" + visible);
            Divider.this.updateVisibility(visible);
        }

        public void onDockedStackExistsChanged(boolean exists) throws RemoteException {
            Log.d("Divider", "onDockedStackExistsChanged exists=" + exists);
            Divider.this.notifyDockedStackExistsChanged(exists);
        }

        public void onDockedStackMinimizedChanged(boolean minimized, long animDuration, boolean isHomeStackResizable) throws RemoteException {
            Log.d("Divider", "onDockedStackMinimizedChanged minimized=" + minimized + " animDuration=" + animDuration);
            Divider.this.updateMinimizedDockedStack(minimized, animDuration);
        }

        public void onAdjustedForImeChanged(final boolean adjustedForIme, final long animDuration) throws RemoteException {
            Log.d("Divider", "onAdjustedForImeChanged adjustedForIme=" + adjustedForIme + " animDuration=" + animDuration);
            Divider.this.mView.post(new Runnable() {
                public void run() {
                    if (Divider.this.mAdjustedForIme != adjustedForIme) {
                        boolean unused = Divider.this.mAdjustedForIme = adjustedForIme;
                        Divider.this.updateTouchable();
                        if (Divider.this.mMinimized) {
                            return;
                        }
                        if (animDuration > 0) {
                            Divider.this.mView.setAdjustedForIme(adjustedForIme, animDuration);
                        } else {
                            Divider.this.mView.setAdjustedForIme(adjustedForIme);
                        }
                    }
                }
            });
        }

        public void onDockSideChanged(final int newDockSide) throws RemoteException {
            Log.d("Divider", "onDockSideChanged newDockSide=" + newDockSide);
            Divider.this.mView.post(new Runnable() {
                public void run() {
                    Divider.this.mView.notifyDockSideChanged(newDockSide);
                }
            });
        }
    }

    public interface DockedStackExistsChangedListener {
        void onDockedStackMinimizedChanged(boolean z);
    }

    public void start() {
        this.mWindowManager = new DividerWindowManager(this.mContext);
        update(this.mContext.getResources().getConfiguration());
        putComponent(Divider.class, this);
        this.mDockDividerVisibilityListener = new DockDividerVisibilityListener();
        Recents.getSystemServices().registerDockedStackListener(this.mDockDividerVisibilityListener);
        this.mForcedResizableController = new ForcedResizableInfoActivityController(this.mContext);
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        update(newConfig);
    }

    public DividerView getView() {
        return this.mView;
    }

    private void addDivider(Configuration configuration) {
        this.mView = (DividerView) LayoutInflater.from(this.mContext).inflate(R.layout.docked_stack_divider, null);
        boolean z = false;
        this.mView.setVisibility(this.mVisible ? 0 : 4);
        int size = this.mContext.getResources().getDimensionPixelSize(R.dimen.docked_stack_divider_thickness);
        if (configuration.orientation == 2) {
            z = true;
        }
        boolean landscape = z;
        int height = -1;
        int width = landscape ? size : -1;
        if (!landscape) {
            height = size;
        }
        this.mWindowManager.add(this.mView, width, height);
        this.mView.injectDependencies(this.mWindowManager, this.mDividerState);
    }

    private void removeDivider() {
        this.mWindowManager.remove();
    }

    private void update(Configuration configuration) {
        removeDivider();
        addDivider(configuration);
        if (this.mMinimized) {
            this.mView.setMinimizedDockStack(true);
            updateTouchable();
        }
    }

    /* access modifiers changed from: private */
    public void updateVisibility(final boolean visible) {
        this.mView.post(new Runnable() {
            public void run() {
                if (Divider.this.mVisible != visible) {
                    boolean unused = Divider.this.mVisible = visible;
                    Divider.this.mView.setVisibility(visible ? 0 : 4);
                    Divider.this.mView.setMinimizedDockStack(Divider.this.mMinimized);
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateMinimizedDockedStack(final boolean minimized, final long animDuration) {
        this.mView.post(new Runnable() {
            public void run() {
                if (Divider.this.mMinimized != minimized) {
                    boolean unused = Divider.this.mMinimized = minimized;
                    Divider.this.updateTouchable();
                    if (animDuration > 0) {
                        Divider.this.mView.setMinimizedDockStack(minimized, animDuration);
                    } else {
                        Divider.this.mView.setMinimizedDockStack(minimized);
                    }
                }
            }
        });
        if (this.mDockedStackExistsChangedListener != null) {
            this.mDockedStackExistsChangedListener.onDockedStackMinimizedChanged(minimized);
        }
    }

    public void registerDockedStackExistsChangedListener(DockedStackExistsChangedListener listener) {
        this.mDockedStackExistsChangedListener = listener;
    }

    /* access modifiers changed from: private */
    public void notifyDockedStackExistsChanged(final boolean exists) {
        this.mExists = exists;
        this.mView.post(new Runnable() {
            public void run() {
                if (Divider.this.mForcedResizableController != null) {
                    Divider.this.mForcedResizableController.notifyDockedStackExistsChanged(exists);
                }
            }
        });
    }

    /* access modifiers changed from: private */
    public void updateTouchable() {
        this.mWindowManager.setTouchable(!this.mMinimized && !this.mAdjustedForIme);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("  mVisible=");
        pw.println(this.mVisible);
        pw.print("  mMinimized=");
        pw.println(this.mMinimized);
        pw.print("  mAdjustedForIme=");
        pw.println(this.mAdjustedForIme);
    }

    public boolean isMinimized() {
        return this.mMinimized;
    }

    public boolean isExists() {
        return this.mExists;
    }
}
