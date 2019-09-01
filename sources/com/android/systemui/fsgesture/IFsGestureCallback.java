package com.android.systemui.fsgesture;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IFsGestureCallback extends IInterface {

    public static abstract class Stub extends Binder implements IFsGestureCallback {

        private static class Proxy implements IFsGestureCallback {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public void changeAlphaScale(float alpha, float scale, int pivotX, int pivotY, int iconPivotX, int iconPivotY, boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.fsgesture.IFsGestureCallback");
                    _data.writeFloat(alpha);
                    _data.writeFloat(scale);
                    _data.writeInt(pivotX);
                    _data.writeInt(pivotY);
                    _data.writeInt(iconPivotX);
                    _data.writeInt(iconPivotY);
                    _data.writeInt(visible);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public TransitionAnimationSpec getSpec(String componentName, int userId) throws RemoteException {
                TransitionAnimationSpec _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.fsgesture.IFsGestureCallback");
                    _data.writeString(componentName);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = TransitionAnimationSpec.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyMiuiAnimationStart() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.fsgesture.IFsGestureCallback");
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void notifyMiuiAnimationEnd() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.android.systemui.fsgesture.IFsGestureCallback");
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public static IFsGestureCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.android.systemui.fsgesture.IFsGestureCallback");
            if (iin == null || !(iin instanceof IFsGestureCallback)) {
                return new Proxy(obj);
            }
            return (IFsGestureCallback) iin;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            if (i != 1598968902) {
                switch (i) {
                    case 1:
                        parcel.enforceInterface("com.android.systemui.fsgesture.IFsGestureCallback");
                        changeAlphaScale(data.readFloat(), data.readFloat(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt() != 0);
                        return true;
                    case 2:
                        parcel.enforceInterface("com.android.systemui.fsgesture.IFsGestureCallback");
                        TransitionAnimationSpec _result = getSpec(data.readString(), data.readInt());
                        reply.writeNoException();
                        if (_result != null) {
                            parcel2.writeInt(1);
                            _result.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 3:
                        parcel.enforceInterface("com.android.systemui.fsgesture.IFsGestureCallback");
                        notifyMiuiAnimationStart();
                        return true;
                    case 4:
                        parcel.enforceInterface("com.android.systemui.fsgesture.IFsGestureCallback");
                        notifyMiuiAnimationEnd();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            } else {
                parcel2.writeString("com.android.systemui.fsgesture.IFsGestureCallback");
                return true;
            }
        }
    }

    void changeAlphaScale(float f, float f2, int i, int i2, int i3, int i4, boolean z) throws RemoteException;

    TransitionAnimationSpec getSpec(String str, int i) throws RemoteException;

    void notifyMiuiAnimationEnd() throws RemoteException;

    void notifyMiuiAnimationStart() throws RemoteException;
}
