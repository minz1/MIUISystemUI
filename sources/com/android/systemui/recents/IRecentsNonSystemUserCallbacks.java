package com.android.systemui.recents;

import android.graphics.Rect;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IRecentsNonSystemUserCallbacks extends IInterface {

    public static abstract class Stub extends Binder implements IRecentsNonSystemUserCallbacks {

        private static class Proxy implements IRecentsNonSystemUserCallbacks {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public void preloadRecents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void cancelPreloadingRecents() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void showRecents(boolean triggeredFromAltTab, boolean draggingInRecents, boolean animate, boolean reloadTasks, boolean fromHome, int recentsGrowTarget) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeInt(triggeredFromAltTab);
                    _data.writeInt(draggingInRecents);
                    _data.writeInt(animate);
                    _data.writeInt(reloadTasks);
                    _data.writeInt(fromHome);
                    _data.writeInt(recentsGrowTarget);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeInt(triggeredFromAltTab);
                    _data.writeInt(triggeredFromHomeKey);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void toggleRecents(int recentsGrowTarget) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeInt(recentsGrowTarget);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onConfigurationChanged() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dockTopTask(int topTaskId, int dragMode, int stackCreateMode, Rect initialBounds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeInt(topTaskId);
                    _data.writeInt(dragMode);
                    _data.writeInt(stackCreateMode);
                    if (initialBounds != null) {
                        _data.writeInt(1);
                        initialBounds.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onDraggingInRecents(float distanceFromTop) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeFloat(distanceFromTop);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onDraggingInRecentsEnded(float velocity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                    _data.writeFloat(velocity);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
        }

        public static IRecentsNonSystemUserCallbacks asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
            if (iin == null || !(iin instanceof IRecentsNonSystemUserCallbacks)) {
                return new Proxy(obj);
            }
            return (IRecentsNonSystemUserCallbacks) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Rect _arg3;
            if (code != 1598968902) {
                boolean _arg1 = false;
                switch (code) {
                    case 1:
                        data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        preloadRecents();
                        return true;
                    case 2:
                        data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        cancelPreloadingRecents();
                        return true;
                    case 3:
                        data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        showRecents(data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.readInt() != 0, data.readInt());
                        return true;
                    case 4:
                        data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        boolean _arg0 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        hideRecents(_arg0, _arg1);
                        return true;
                    case 5:
                        data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        toggleRecents(data.readInt());
                        return true;
                    case 6:
                        data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        onConfigurationChanged();
                        return true;
                    case 7:
                        data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        int _arg02 = data.readInt();
                        int _arg12 = data.readInt();
                        int _arg2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg3 = (Rect) Rect.CREATOR.createFromParcel(data);
                        } else {
                            _arg3 = null;
                        }
                        dockTopTask(_arg02, _arg12, _arg2, _arg3);
                        return true;
                    case 8:
                        data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        onDraggingInRecents(data.readFloat());
                        return true;
                    case 9:
                        data.enforceInterface("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                        onDraggingInRecentsEnded(data.readFloat());
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            } else {
                reply.writeString("com.android.systemui.recents.IRecentsNonSystemUserCallbacks");
                return true;
            }
        }
    }

    void cancelPreloadingRecents() throws RemoteException;

    void dockTopTask(int i, int i2, int i3, Rect rect) throws RemoteException;

    void hideRecents(boolean z, boolean z2) throws RemoteException;

    void onConfigurationChanged() throws RemoteException;

    void onDraggingInRecents(float f) throws RemoteException;

    void onDraggingInRecentsEnded(float f) throws RemoteException;

    void preloadRecents() throws RemoteException;

    void showRecents(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, int i) throws RemoteException;

    void toggleRecents(int i) throws RemoteException;
}
