package com.miui.voiptalk.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMiuiVoipService extends IInterface {

    public static abstract class Stub extends Binder implements IMiuiVoipService {

        private static class Proxy implements IMiuiVoipService {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public void endCall() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.miui.voiptalk.service.IMiuiVoipService");
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void answerRingingCall() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.miui.voiptalk.service.IMiuiVoipService");
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void silenceRinger() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.miui.voiptalk.service.IMiuiVoipService");
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCallState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.miui.voiptalk.service.IMiuiVoipService");
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isCallingOut() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.miui.voiptalk.service.IMiuiVoipService");
                    boolean _result = false;
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getCallBaseTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.miui.voiptalk.service.IMiuiVoipService");
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readLong();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getExtraCallState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.miui.voiptalk.service.IMiuiVoipService");
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    return _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isVoipCallUiOnBack() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.miui.voiptalk.service.IMiuiVoipService");
                    boolean _result = false;
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isVideoCall() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("com.miui.voiptalk.service.IMiuiVoipService");
                    boolean _result = false;
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = true;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public static IMiuiVoipService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("com.miui.voiptalk.service.IMiuiVoipService");
            if (iin == null || !(iin instanceof IMiuiVoipService)) {
                return new Proxy(obj);
            }
            return (IMiuiVoipService) iin;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code != 1598968902) {
                switch (code) {
                    case 1:
                        data.enforceInterface("com.miui.voiptalk.service.IMiuiVoipService");
                        endCall();
                        reply.writeNoException();
                        return true;
                    case 2:
                        data.enforceInterface("com.miui.voiptalk.service.IMiuiVoipService");
                        answerRingingCall();
                        reply.writeNoException();
                        return true;
                    case 3:
                        data.enforceInterface("com.miui.voiptalk.service.IMiuiVoipService");
                        silenceRinger();
                        reply.writeNoException();
                        return true;
                    case 4:
                        data.enforceInterface("com.miui.voiptalk.service.IMiuiVoipService");
                        int _result = getCallState();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 5:
                        data.enforceInterface("com.miui.voiptalk.service.IMiuiVoipService");
                        boolean _result2 = isCallingOut();
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 6:
                        data.enforceInterface("com.miui.voiptalk.service.IMiuiVoipService");
                        long _result3 = getCallBaseTime();
                        reply.writeNoException();
                        reply.writeLong(_result3);
                        return true;
                    case 7:
                        data.enforceInterface("com.miui.voiptalk.service.IMiuiVoipService");
                        String _result4 = getExtraCallState();
                        reply.writeNoException();
                        reply.writeString(_result4);
                        return true;
                    case 8:
                        data.enforceInterface("com.miui.voiptalk.service.IMiuiVoipService");
                        boolean _result5 = isVoipCallUiOnBack();
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 9:
                        data.enforceInterface("com.miui.voiptalk.service.IMiuiVoipService");
                        boolean _result6 = isVideoCall();
                        reply.writeNoException();
                        reply.writeInt(_result6);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            } else {
                reply.writeString("com.miui.voiptalk.service.IMiuiVoipService");
                return true;
            }
        }
    }

    void answerRingingCall() throws RemoteException;

    void endCall() throws RemoteException;

    long getCallBaseTime() throws RemoteException;

    int getCallState() throws RemoteException;

    String getExtraCallState() throws RemoteException;

    boolean isCallingOut() throws RemoteException;

    boolean isVideoCall() throws RemoteException;

    boolean isVoipCallUiOnBack() throws RemoteException;

    void silenceRinger() throws RemoteException;
}
