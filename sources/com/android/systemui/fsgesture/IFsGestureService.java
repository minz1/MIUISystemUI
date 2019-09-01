package com.android.systemui.fsgesture;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.systemui.fsgesture.IFsGestureCallback;

public interface IFsGestureService extends IInterface {

    public static abstract class Stub extends Binder implements IFsGestureService {
        public Stub() {
            attachInterface(this, "com.android.systemui.fsgesture.IFsGestureService");
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != 1598968902) {
                switch (code) {
                    case 1:
                        data.enforceInterface("com.android.systemui.fsgesture.IFsGestureService");
                        registerCallback(data.readString(), IFsGestureCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 2:
                        data.enforceInterface("com.android.systemui.fsgesture.IFsGestureService");
                        unregisterCallback(data.readString(), IFsGestureCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 3:
                        data.enforceInterface("com.android.systemui.fsgesture.IFsGestureService");
                        notifyHomeStatus(data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            } else {
                reply.writeString("com.android.systemui.fsgesture.IFsGestureService");
                return true;
            }
        }
    }

    void notifyHomeStatus(boolean z) throws RemoteException;

    void registerCallback(String str, IFsGestureCallback iFsGestureCallback) throws RemoteException;

    void unregisterCallback(String str, IFsGestureCallback iFsGestureCallback) throws RemoteException;
}
