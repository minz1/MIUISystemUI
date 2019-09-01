package com.android.systemui.recents;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import com.android.internal.os.SomeArgs;
import com.android.systemui.recents.IRecentsNonSystemUserCallbacks;

public class RecentsImplProxy extends IRecentsNonSystemUserCallbacks.Stub {
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            boolean z = true;
            switch (msg.what) {
                case 1:
                    RecentsImplProxy.this.mImpl.preloadRecents();
                    break;
                case 2:
                    RecentsImplProxy.this.mImpl.cancelPreloadingRecents();
                    break;
                case 3:
                    SomeArgs args = (SomeArgs) msg.obj;
                    RecentsImplProxy.this.mImpl.showRecents(args.argi1 != 0, args.argi2 != 0, args.argi3 != 0, args.argi4 != 0, args.argi5 != 0, args.argi6, false);
                    break;
                case 4:
                    RecentsImpl access$000 = RecentsImplProxy.this.mImpl;
                    boolean z2 = msg.arg1 != 0;
                    if (msg.arg2 == 0) {
                        z = false;
                    }
                    access$000.hideRecents(z2, z, false);
                    break;
                case 5:
                    RecentsImplProxy.this.mImpl.toggleRecents(((SomeArgs) msg.obj).argi1);
                    break;
                case 6:
                    RecentsImplProxy.this.mImpl.onConfigurationChanged();
                    break;
                case 7:
                    SomeArgs args2 = (SomeArgs) msg.obj;
                    RecentsImpl access$0002 = RecentsImplProxy.this.mImpl;
                    int i = args2.argi1;
                    int i2 = args2.argi2;
                    args2.argi3 = 0;
                    access$0002.dockTopTask(i, i2, 0, (Rect) args2.arg1);
                    break;
                case 8:
                    RecentsImplProxy.this.mImpl.onDraggingInRecents(((Float) msg.obj).floatValue());
                    break;
                case 9:
                    RecentsImplProxy.this.mImpl.onDraggingInRecentsEnded(((Float) msg.obj).floatValue());
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
            super.handleMessage(msg);
        }
    };
    /* access modifiers changed from: private */
    public RecentsImpl mImpl;

    public RecentsImplProxy(RecentsImpl recentsImpl) {
        this.mImpl = recentsImpl;
    }

    public void preloadRecents() throws RemoteException {
        this.mHandler.sendEmptyMessage(1);
    }

    public void cancelPreloadingRecents() throws RemoteException {
        this.mHandler.sendEmptyMessage(2);
    }

    public void showRecents(boolean triggeredFromAltTab, boolean draggingInRecents, boolean animate, boolean reloadTasks, boolean fromHome, int growTarget) throws RemoteException {
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = triggeredFromAltTab;
        args.argi2 = draggingInRecents;
        args.argi3 = animate;
        args.argi4 = reloadTasks;
        args.argi5 = fromHome;
        args.argi6 = growTarget;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3, args));
    }

    public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws RemoteException {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(4, triggeredFromAltTab, triggeredFromHomeKey));
    }

    public void toggleRecents(int growTarget) throws RemoteException {
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = growTarget;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, args));
    }

    public void onConfigurationChanged() throws RemoteException {
        this.mHandler.sendEmptyMessage(6);
    }

    public void dockTopTask(int topTaskId, int dragMode, int stackCreateMode, Rect initialBounds) throws RemoteException {
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = topTaskId;
        args.argi2 = dragMode;
        args.argi3 = stackCreateMode;
        args.arg1 = initialBounds;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7, args));
    }

    public void onDraggingInRecents(float distanceFromTop) throws RemoteException {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(8, Float.valueOf(distanceFromTop)));
    }

    public void onDraggingInRecentsEnded(float velocity) throws RemoteException {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9, Float.valueOf(velocity)));
    }
}
