package android.support.v4.media;

import android.app.PendingIntent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import java.util.List;

public interface IMediaController2 extends IInterface {

    public static abstract class Stub extends Binder implements IMediaController2 {

        private static class Proxy implements IMediaController2 {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public void onCurrentMediaItemChanged(Bundle item) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (item != null) {
                        _data.writeInt(1);
                        item.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPlayerStateChanged(long eventTimeMs, long positionMs, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeLong(eventTimeMs);
                    _data.writeLong(positionMs);
                    _data.writeInt(state);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeLong(eventTimeMs);
                    _data.writeLong(positionMs);
                    _data.writeFloat(speed);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onBufferingStateChanged(Bundle item, int state, long bufferedPositionMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (item != null) {
                        _data.writeInt(1);
                        item.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(state);
                    _data.writeLong(bufferedPositionMs);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPlaylistChanged(List<Bundle> playlist, Bundle metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeTypedList(playlist);
                    if (metadata != null) {
                        _data.writeInt(1);
                        metadata.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPlaylistMetadataChanged(Bundle metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (metadata != null) {
                        _data.writeInt(1);
                        metadata.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPlaybackInfoChanged(Bundle playbackInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (playbackInfo != null) {
                        _data.writeInt(1);
                        playbackInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onRepeatModeChanged(int repeatMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeInt(repeatMode);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onShuffleModeChanged(int shuffleMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeInt(shuffleMode);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeLong(eventTimeMs);
                    _data.writeLong(positionMs);
                    _data.writeLong(seekPositionMs);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onError(int errorCode, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeInt(errorCode);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeTypedList(routes);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onConnected(IMediaSession2 sessionBinder, Bundle commandGroup, int playerState, Bundle currentItem, long positionEventTimeMs, long positionMs, float playbackSpeed, long bufferedPositionMs, Bundle playbackInfo, int repeatMode, int shuffleMode, List<Bundle> playlist, PendingIntent sessionActivity) throws RemoteException {
                Bundle bundle = commandGroup;
                Bundle bundle2 = currentItem;
                Bundle bundle3 = playbackInfo;
                PendingIntent pendingIntent = sessionActivity;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeStrongBinder(sessionBinder != null ? sessionBinder.asBinder() : null);
                    if (bundle != null) {
                        _data.writeInt(1);
                        bundle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(playerState);
                    if (bundle2 != null) {
                        _data.writeInt(1);
                        bundle2.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    try {
                        _data.writeLong(positionEventTimeMs);
                    } catch (Throwable th) {
                        th = th;
                        long j = positionMs;
                        float f = playbackSpeed;
                        long j2 = bufferedPositionMs;
                        int i = repeatMode;
                        int i2 = shuffleMode;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeLong(positionMs);
                        try {
                            _data.writeFloat(playbackSpeed);
                            try {
                                _data.writeLong(bufferedPositionMs);
                                if (bundle3 != null) {
                                    _data.writeInt(1);
                                    bundle3.writeToParcel(_data, 0);
                                } else {
                                    _data.writeInt(0);
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                int i3 = repeatMode;
                                int i22 = shuffleMode;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            long j22 = bufferedPositionMs;
                            int i32 = repeatMode;
                            int i222 = shuffleMode;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        float f2 = playbackSpeed;
                        long j222 = bufferedPositionMs;
                        int i322 = repeatMode;
                        int i2222 = shuffleMode;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(repeatMode);
                        try {
                            _data.writeInt(shuffleMode);
                            _data.writeTypedList(playlist);
                            if (pendingIntent != null) {
                                _data.writeInt(1);
                                pendingIntent.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            this.mRemote.transact(13, _data, null, 1);
                            _data.recycle();
                        } catch (Throwable th5) {
                            th = th5;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        int i22222 = shuffleMode;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    long j3 = positionEventTimeMs;
                    long j4 = positionMs;
                    float f22 = playbackSpeed;
                    long j2222 = bufferedPositionMs;
                    int i3222 = repeatMode;
                    int i222222 = shuffleMode;
                    _data.recycle();
                    throw th;
                }
            }

            public void onDisconnected() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onCustomLayoutChanged(List<Bundle> commandButtonlist) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeTypedList(commandButtonlist);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onAllowedCommandsChanged(Bundle commands) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (commands != null) {
                        _data.writeInt(1);
                        commands.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onCustomCommand(Bundle command, Bundle args, ResultReceiver receiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (command != null) {
                        _data.writeInt(1);
                        command.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (receiver != null) {
                        _data.writeInt(1);
                        receiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    if (rootHints != null) {
                        _data.writeInt(1);
                        rootHints.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(rootMediaId);
                    if (rootExtra != null) {
                        _data.writeInt(1);
                        rootExtra.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onGetItemDone(String mediaId, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(mediaId);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onChildrenChanged(String parentId, int itemCount, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(parentId);
                    _data.writeInt(itemCount);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onGetChildrenDone(String parentId, int page, int pageSize, List<Bundle> result, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(parentId);
                    _data.writeInt(page);
                    _data.writeInt(pageSize);
                    _data.writeTypedList(result);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onSearchResultChanged(String query, int itemCount, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(query);
                    _data.writeInt(itemCount);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onGetSearchResultDone(String query, int page, int pageSize, List<Bundle> result, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaController2");
                    _data.writeString(query);
                    _data.writeInt(page);
                    _data.writeInt(pageSize);
                    _data.writeTypedList(result);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public static IMediaController2 asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("android.support.v4.media.IMediaController2");
            if (iin == null || !(iin instanceof IMediaController2)) {
                return new Proxy(obj);
            }
            return (IMediaController2) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v3, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v11, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v15, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v19, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v23, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v33, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v49, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v57, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v61, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v65, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v74, resolved type: android.os.Bundle} */
        /* JADX WARNING: type inference failed for: r0v2 */
        /* JADX WARNING: type inference failed for: r0v40 */
        /* JADX WARNING: type inference failed for: r0v53, types: [android.os.ResultReceiver] */
        /* JADX WARNING: type inference failed for: r0v69 */
        /* JADX WARNING: type inference failed for: r0v78 */
        /* JADX WARNING: type inference failed for: r0v84 */
        /* JADX WARNING: type inference failed for: r0v85 */
        /* JADX WARNING: type inference failed for: r0v86 */
        /* JADX WARNING: type inference failed for: r0v87 */
        /* JADX WARNING: type inference failed for: r0v88 */
        /* JADX WARNING: type inference failed for: r0v89 */
        /* JADX WARNING: type inference failed for: r0v90 */
        /* JADX WARNING: type inference failed for: r0v91 */
        /* JADX WARNING: type inference failed for: r0v92 */
        /* JADX WARNING: type inference failed for: r0v93 */
        /* JADX WARNING: type inference failed for: r0v94 */
        /* JADX WARNING: type inference failed for: r0v95 */
        /* JADX WARNING: type inference failed for: r0v96 */
        /* JADX WARNING: type inference failed for: r0v97 */
        /* JADX WARNING: type inference failed for: r0v98 */
        /* JADX WARNING: Multi-variable type inference failed */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean onTransact(int r32, android.os.Parcel r33, android.os.Parcel r34, int r35) throws android.os.RemoteException {
            /*
                r31 = this;
                r15 = r31
                r14 = r32
                r13 = r33
                r0 = 1598968902(0x5f4e5446, float:1.4867585E19)
                r17 = 1
                if (r14 == r0) goto L_0x032f
                r0 = 0
                switch(r14) {
                    case 1: goto L_0x0314;
                    case 2: goto L_0x02f9;
                    case 3: goto L_0x02de;
                    case 4: goto L_0x02bb;
                    case 5: goto L_0x029a;
                    case 6: goto L_0x027f;
                    case 7: goto L_0x0264;
                    case 8: goto L_0x0255;
                    case 9: goto L_0x0246;
                    case 10: goto L_0x022b;
                    case 11: goto L_0x020c;
                    case 12: goto L_0x01f9;
                    case 13: goto L_0x016e;
                    case 14: goto L_0x0165;
                    case 15: goto L_0x0156;
                    case 16: goto L_0x013d;
                    case 17: goto L_0x0104;
                    case 18: goto L_0x00d7;
                    case 19: goto L_0x00ba;
                    case 20: goto L_0x0099;
                    case 21: goto L_0x0068;
                    case 22: goto L_0x0047;
                    case 23: goto L_0x0016;
                    default: goto L_0x0011;
                }
            L_0x0011:
                boolean r0 = super.onTransact(r32, r33, r34, r35)
                return r0
            L_0x0016:
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r1)
                java.lang.String r6 = r33.readString()
                int r7 = r33.readInt()
                int r8 = r33.readInt()
                android.os.Parcelable$Creator r1 = android.os.Bundle.CREATOR
                java.util.ArrayList r9 = r13.createTypedArrayList(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x003d
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r13)
                android.os.Bundle r0 = (android.os.Bundle) r0
            L_0x003b:
                r5 = r0
                goto L_0x003e
            L_0x003d:
                goto L_0x003b
            L_0x003e:
                r0 = r15
                r1 = r6
                r2 = r7
                r3 = r8
                r4 = r9
                r0.onGetSearchResultDone(r1, r2, r3, r4, r5)
                return r17
            L_0x0047:
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r1)
                java.lang.String r1 = r33.readString()
                int r2 = r33.readInt()
                int r3 = r33.readInt()
                if (r3 == 0) goto L_0x0063
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r13)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0064
            L_0x0063:
            L_0x0064:
                r15.onSearchResultChanged(r1, r2, r0)
                return r17
            L_0x0068:
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r1)
                java.lang.String r6 = r33.readString()
                int r7 = r33.readInt()
                int r8 = r33.readInt()
                android.os.Parcelable$Creator r1 = android.os.Bundle.CREATOR
                java.util.ArrayList r9 = r13.createTypedArrayList(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x008f
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r13)
                android.os.Bundle r0 = (android.os.Bundle) r0
            L_0x008d:
                r5 = r0
                goto L_0x0090
            L_0x008f:
                goto L_0x008d
            L_0x0090:
                r0 = r15
                r1 = r6
                r2 = r7
                r3 = r8
                r4 = r9
                r0.onGetChildrenDone(r1, r2, r3, r4, r5)
                return r17
            L_0x0099:
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r1)
                java.lang.String r1 = r33.readString()
                int r2 = r33.readInt()
                int r3 = r33.readInt()
                if (r3 == 0) goto L_0x00b5
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r13)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x00b6
            L_0x00b5:
            L_0x00b6:
                r15.onChildrenChanged(r1, r2, r0)
                return r17
            L_0x00ba:
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r1)
                java.lang.String r1 = r33.readString()
                int r2 = r33.readInt()
                if (r2 == 0) goto L_0x00d2
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r13)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x00d3
            L_0x00d2:
            L_0x00d3:
                r15.onGetItemDone(r1, r0)
                return r17
            L_0x00d7:
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x00eb
                android.os.Parcelable$Creator r1 = android.os.Bundle.CREATOR
                java.lang.Object r1 = r1.createFromParcel(r13)
                android.os.Bundle r1 = (android.os.Bundle) r1
                goto L_0x00ec
            L_0x00eb:
                r1 = r0
            L_0x00ec:
                java.lang.String r2 = r33.readString()
                int r3 = r33.readInt()
                if (r3 == 0) goto L_0x00ff
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r13)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0100
            L_0x00ff:
            L_0x0100:
                r15.onGetLibraryRootDone(r1, r2, r0)
                return r17
            L_0x0104:
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x0118
                android.os.Parcelable$Creator r1 = android.os.Bundle.CREATOR
                java.lang.Object r1 = r1.createFromParcel(r13)
                android.os.Bundle r1 = (android.os.Bundle) r1
                goto L_0x0119
            L_0x0118:
                r1 = r0
            L_0x0119:
                int r2 = r33.readInt()
                if (r2 == 0) goto L_0x0128
                android.os.Parcelable$Creator r2 = android.os.Bundle.CREATOR
                java.lang.Object r2 = r2.createFromParcel(r13)
                android.os.Bundle r2 = (android.os.Bundle) r2
                goto L_0x0129
            L_0x0128:
                r2 = r0
            L_0x0129:
                int r3 = r33.readInt()
                if (r3 == 0) goto L_0x0138
                android.os.Parcelable$Creator r0 = android.os.ResultReceiver.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r13)
                android.os.ResultReceiver r0 = (android.os.ResultReceiver) r0
                goto L_0x0139
            L_0x0138:
            L_0x0139:
                r15.onCustomCommand(r1, r2, r0)
                return r17
            L_0x013d:
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x0151
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r13)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0152
            L_0x0151:
            L_0x0152:
                r15.onAllowedCommandsChanged(r0)
                return r17
            L_0x0156:
                java.lang.String r0 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r0)
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.util.ArrayList r0 = r13.createTypedArrayList(r0)
                r15.onCustomLayoutChanged(r0)
                return r17
            L_0x0165:
                java.lang.String r0 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r0)
                r31.onDisconnected()
                return r17
            L_0x016e:
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r13.enforceInterface(r1)
                android.os.IBinder r1 = r33.readStrongBinder()
                android.support.v4.media.IMediaSession2 r18 = android.support.v4.media.IMediaSession2.Stub.asInterface(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x018b
                android.os.Parcelable$Creator r1 = android.os.Bundle.CREATOR
                java.lang.Object r1 = r1.createFromParcel(r13)
                android.os.Bundle r1 = (android.os.Bundle) r1
                r2 = r1
                goto L_0x018c
            L_0x018b:
                r2 = r0
            L_0x018c:
                int r19 = r33.readInt()
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x01a0
                android.os.Parcelable$Creator r1 = android.os.Bundle.CREATOR
                java.lang.Object r1 = r1.createFromParcel(r13)
                android.os.Bundle r1 = (android.os.Bundle) r1
                r4 = r1
                goto L_0x01a1
            L_0x01a0:
                r4 = r0
            L_0x01a1:
                long r20 = r33.readLong()
                long r22 = r33.readLong()
                float r24 = r33.readFloat()
                long r25 = r33.readLong()
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x01c1
                android.os.Parcelable$Creator r1 = android.os.Bundle.CREATOR
                java.lang.Object r1 = r1.createFromParcel(r13)
                android.os.Bundle r1 = (android.os.Bundle) r1
                r12 = r1
                goto L_0x01c2
            L_0x01c1:
                r12 = r0
            L_0x01c2:
                int r27 = r33.readInt()
                int r28 = r33.readInt()
                android.os.Parcelable$Creator r1 = android.os.Bundle.CREATOR
                java.util.ArrayList r29 = r13.createTypedArrayList(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x01e1
                android.os.Parcelable$Creator r0 = android.app.PendingIntent.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r13)
                android.app.PendingIntent r0 = (android.app.PendingIntent) r0
            L_0x01de:
                r16 = r0
                goto L_0x01e2
            L_0x01e1:
                goto L_0x01de
            L_0x01e2:
                r0 = r15
                r1 = r18
                r3 = r19
                r5 = r20
                r7 = r22
                r9 = r24
                r10 = r25
                r13 = r27
                r14 = r28
                r15 = r29
                r0.onConnected(r1, r2, r3, r4, r5, r7, r9, r10, r12, r13, r14, r15, r16)
                return r17
            L_0x01f9:
                java.lang.String r0 = "android.support.v4.media.IMediaController2"
                r7 = r33
                r7.enforceInterface(r0)
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.util.ArrayList r0 = r7.createTypedArrayList(r0)
                r8 = r31
                r8.onRoutesInfoChanged(r0)
                return r17
            L_0x020c:
                r7 = r13
                r8 = r15
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r1)
                int r1 = r33.readInt()
                int r2 = r33.readInt()
                if (r2 == 0) goto L_0x0226
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r7)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0227
            L_0x0226:
            L_0x0227:
                r8.onError(r1, r0)
                return r17
            L_0x022b:
                r7 = r13
                r8 = r15
                java.lang.String r0 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r0)
                long r9 = r33.readLong()
                long r11 = r33.readLong()
                long r13 = r33.readLong()
                r0 = r8
                r1 = r9
                r3 = r11
                r5 = r13
                r0.onSeekCompleted(r1, r3, r5)
                return r17
            L_0x0246:
                r7 = r13
                r8 = r15
                java.lang.String r0 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r0)
                int r0 = r33.readInt()
                r8.onShuffleModeChanged(r0)
                return r17
            L_0x0255:
                r7 = r13
                r8 = r15
                java.lang.String r0 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r0)
                int r0 = r33.readInt()
                r8.onRepeatModeChanged(r0)
                return r17
            L_0x0264:
                r7 = r13
                r8 = r15
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x027a
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r7)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x027b
            L_0x027a:
            L_0x027b:
                r8.onPlaybackInfoChanged(r0)
                return r17
            L_0x027f:
                r7 = r13
                r8 = r15
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x0295
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r7)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0296
            L_0x0295:
            L_0x0296:
                r8.onPlaylistMetadataChanged(r0)
                return r17
            L_0x029a:
                r7 = r13
                r8 = r15
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r1)
                android.os.Parcelable$Creator r1 = android.os.Bundle.CREATOR
                java.util.ArrayList r1 = r7.createTypedArrayList(r1)
                int r2 = r33.readInt()
                if (r2 == 0) goto L_0x02b6
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r7)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x02b7
            L_0x02b6:
            L_0x02b7:
                r8.onPlaylistChanged(r1, r0)
                return r17
            L_0x02bb:
                r7 = r13
                r8 = r15
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x02d1
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r7)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x02d2
            L_0x02d1:
            L_0x02d2:
                int r1 = r33.readInt()
                long r2 = r33.readLong()
                r8.onBufferingStateChanged(r0, r1, r2)
                return r17
            L_0x02de:
                r7 = r13
                r8 = r15
                java.lang.String r0 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r0)
                long r9 = r33.readLong()
                long r11 = r33.readLong()
                float r6 = r33.readFloat()
                r0 = r8
                r1 = r9
                r3 = r11
                r5 = r6
                r0.onPlaybackSpeedChanged(r1, r3, r5)
                return r17
            L_0x02f9:
                r7 = r13
                r8 = r15
                java.lang.String r0 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r0)
                long r9 = r33.readLong()
                long r11 = r33.readLong()
                int r6 = r33.readInt()
                r0 = r8
                r1 = r9
                r3 = r11
                r5 = r6
                r0.onPlayerStateChanged(r1, r3, r5)
                return r17
            L_0x0314:
                r7 = r13
                r8 = r15
                java.lang.String r1 = "android.support.v4.media.IMediaController2"
                r7.enforceInterface(r1)
                int r1 = r33.readInt()
                if (r1 == 0) goto L_0x032a
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r7)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x032b
            L_0x032a:
            L_0x032b:
                r8.onCurrentMediaItemChanged(r0)
                return r17
            L_0x032f:
                r7 = r13
                r8 = r15
                java.lang.String r0 = "android.support.v4.media.IMediaController2"
                r1 = r34
                r1.writeString(r0)
                return r17
            */
            throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.IMediaController2.Stub.onTransact(int, android.os.Parcel, android.os.Parcel, int):boolean");
        }
    }

    void onAllowedCommandsChanged(Bundle bundle) throws RemoteException;

    void onBufferingStateChanged(Bundle bundle, int i, long j) throws RemoteException;

    void onChildrenChanged(String str, int i, Bundle bundle) throws RemoteException;

    void onConnected(IMediaSession2 iMediaSession2, Bundle bundle, int i, Bundle bundle2, long j, long j2, float f, long j3, Bundle bundle3, int i2, int i3, List<Bundle> list, PendingIntent pendingIntent) throws RemoteException;

    void onCurrentMediaItemChanged(Bundle bundle) throws RemoteException;

    void onCustomCommand(Bundle bundle, Bundle bundle2, ResultReceiver resultReceiver) throws RemoteException;

    void onCustomLayoutChanged(List<Bundle> list) throws RemoteException;

    void onDisconnected() throws RemoteException;

    void onError(int i, Bundle bundle) throws RemoteException;

    void onGetChildrenDone(String str, int i, int i2, List<Bundle> list, Bundle bundle) throws RemoteException;

    void onGetItemDone(String str, Bundle bundle) throws RemoteException;

    void onGetLibraryRootDone(Bundle bundle, String str, Bundle bundle2) throws RemoteException;

    void onGetSearchResultDone(String str, int i, int i2, List<Bundle> list, Bundle bundle) throws RemoteException;

    void onPlaybackInfoChanged(Bundle bundle) throws RemoteException;

    void onPlaybackSpeedChanged(long j, long j2, float f) throws RemoteException;

    void onPlayerStateChanged(long j, long j2, int i) throws RemoteException;

    void onPlaylistChanged(List<Bundle> list, Bundle bundle) throws RemoteException;

    void onPlaylistMetadataChanged(Bundle bundle) throws RemoteException;

    void onRepeatModeChanged(int i) throws RemoteException;

    void onRoutesInfoChanged(List<Bundle> list) throws RemoteException;

    void onSearchResultChanged(String str, int i, Bundle bundle) throws RemoteException;

    void onSeekCompleted(long j, long j2, long j3) throws RemoteException;

    void onShuffleModeChanged(int i) throws RemoteException;
}
