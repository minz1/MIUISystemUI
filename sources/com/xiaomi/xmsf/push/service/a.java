package com.xiaomi.xmsf.push.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.Map;

/* compiled from: IHttpService */
public interface a extends IInterface {

    /* renamed from: com.xiaomi.xmsf.push.service.a$a  reason: collision with other inner class name */
    /* compiled from: IHttpService */
    public static abstract class C0003a extends Binder implements a {

        /* renamed from: com.xiaomi.xmsf.push.service.a$a$a  reason: collision with other inner class name */
        /* compiled from: IHttpService */
        private static class C0004a implements a {
            private IBinder a;

            C0004a(IBinder iBinder) {
                this.a = iBinder;
            }

            public IBinder asBinder() {
                return this.a;
            }

            public String a(String str, Map map) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.xiaomi.xmsf.push.service.IHttpService");
                    obtain.writeString(str);
                    obtain.writeMap(map);
                    this.a.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readString();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }

        public static a a(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.xiaomi.xmsf.push.service.IHttpService");
            if (queryLocalInterface == null || !(queryLocalInterface instanceof a)) {
                return new C0004a(iBinder);
            }
            return (a) queryLocalInterface;
        }

        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1) {
                parcel.enforceInterface("com.xiaomi.xmsf.push.service.IHttpService");
                String a = a(parcel.readString(), parcel.readHashMap(getClass().getClassLoader()));
                parcel2.writeNoException();
                parcel2.writeString(a);
                return true;
            } else if (i != 1598968902) {
                return super.onTransact(i, parcel, parcel2, i2);
            } else {
                parcel2.writeString("com.xiaomi.xmsf.push.service.IHttpService");
                return true;
            }
        }
    }

    String a(String str, Map map) throws RemoteException;
}
