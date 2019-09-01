package android.support.v4.media;

import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import java.util.List;

public interface IMediaSession2 extends IInterface {

    public static abstract class Stub extends Binder implements IMediaSession2 {

        private static class Proxy implements IMediaSession2 {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public void connect(IMediaController2 caller, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void release(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setVolumeTo(IMediaController2 caller, int value, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(value);
                    _data.writeInt(flags);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void adjustVolume(IMediaController2 caller, int direction, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(direction);
                    _data.writeInt(flags);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void play(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void pause(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void reset(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void prepare(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void fastForward(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void rewind(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void seekTo(IMediaController2 caller, long pos) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeLong(pos);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void sendCustomCommand(IMediaController2 caller, Bundle command, Bundle args, ResultReceiver receiver) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
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
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void prepareFromUri(IMediaController2 caller, Uri uri, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (uri != null) {
                        _data.writeInt(1);
                        uri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void prepareFromSearch(IMediaController2 caller, String query, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void prepareFromMediaId(IMediaController2 caller, String mediaId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void playFromUri(IMediaController2 caller, Uri uri, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (uri != null) {
                        _data.writeInt(1);
                        uri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void playFromSearch(IMediaController2 caller, String query, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void playFromMediaId(IMediaController2 caller, String mediaId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setRating(IMediaController2 caller, String mediaId, Bundle rating) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    if (rating != null) {
                        _data.writeInt(1);
                        rating.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setPlaybackSpeed(IMediaController2 caller, float speed) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeFloat(speed);
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setPlaylist(IMediaController2 caller, List<Bundle> playlist, Bundle metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeTypedList(playlist);
                    if (metadata != null) {
                        _data.writeInt(1);
                        metadata.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updatePlaylistMetadata(IMediaController2 caller, Bundle metadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (metadata != null) {
                        _data.writeInt(1);
                        metadata.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void addPlaylistItem(IMediaController2 caller, int index, Bundle mediaItem) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(index);
                    if (mediaItem != null) {
                        _data.writeInt(1);
                        mediaItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void removePlaylistItem(IMediaController2 caller, Bundle mediaItem) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (mediaItem != null) {
                        _data.writeInt(1);
                        mediaItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void replacePlaylistItem(IMediaController2 caller, int index, Bundle mediaItem) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(index);
                    if (mediaItem != null) {
                        _data.writeInt(1);
                        mediaItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(25, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void skipToPlaylistItem(IMediaController2 caller, Bundle mediaItem) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (mediaItem != null) {
                        _data.writeInt(1);
                        mediaItem.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(26, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void skipToPreviousItem(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void skipToNextItem(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setRepeatMode(IMediaController2 caller, int repeatMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(repeatMode);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setShuffleMode(IMediaController2 caller, int shuffleMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(shuffleMode);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void subscribeRoutesInfo(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unsubscribeRoutesInfo(IMediaController2 caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void selectRoute(IMediaController2 caller, Bundle route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (route != null) {
                        _data.writeInt(1);
                        route.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(33, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void getLibraryRoot(IMediaController2 caller, Bundle rootHints) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (rootHints != null) {
                        _data.writeInt(1);
                        rootHints.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(34, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void getItem(IMediaController2 caller, String mediaId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    this.mRemote.transact(35, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void getChildren(IMediaController2 caller, String parentId, int page, int pageSize, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(parentId);
                    _data.writeInt(page);
                    _data.writeInt(pageSize);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(36, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void search(IMediaController2 caller, String query, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(37, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void getSearchResult(IMediaController2 caller, String query, int page, int pageSize, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    _data.writeInt(page);
                    _data.writeInt(pageSize);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(38, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void subscribe(IMediaController2 caller, String parentId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(parentId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(39, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unsubscribe(IMediaController2 caller, String parentId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken("android.support.v4.media.IMediaSession2");
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(parentId);
                    this.mRemote.transact(40, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public static IMediaSession2 asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface("android.support.v4.media.IMediaSession2");
            if (iin == null || !(iin instanceof IMediaSession2)) {
                return new Proxy(obj);
            }
            return (IMediaSession2) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v40, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v44, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v48, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v52, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v56, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v60, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v64, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v71, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v75, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v79, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v83, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v87, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v91, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v113, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v117, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v129, resolved type: android.os.Bundle} */
        /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v138, resolved type: android.os.Bundle} */
        /* JADX WARNING: type inference failed for: r0v2 */
        /* JADX WARNING: type inference failed for: r0v36, types: [android.os.ResultReceiver] */
        /* JADX WARNING: type inference failed for: r0v124 */
        /* JADX WARNING: type inference failed for: r0v133 */
        /* JADX WARNING: type inference failed for: r0v146 */
        /* JADX WARNING: type inference failed for: r0v147 */
        /* JADX WARNING: type inference failed for: r0v148 */
        /* JADX WARNING: type inference failed for: r0v149 */
        /* JADX WARNING: type inference failed for: r0v150 */
        /* JADX WARNING: type inference failed for: r0v151 */
        /* JADX WARNING: type inference failed for: r0v152 */
        /* JADX WARNING: type inference failed for: r0v153 */
        /* JADX WARNING: type inference failed for: r0v154 */
        /* JADX WARNING: type inference failed for: r0v155 */
        /* JADX WARNING: type inference failed for: r0v156 */
        /* JADX WARNING: type inference failed for: r0v157 */
        /* JADX WARNING: type inference failed for: r0v158 */
        /* JADX WARNING: type inference failed for: r0v159 */
        /* JADX WARNING: type inference failed for: r0v160 */
        /* JADX WARNING: type inference failed for: r0v161 */
        /* JADX WARNING: type inference failed for: r0v162 */
        /* JADX WARNING: type inference failed for: r0v163 */
        /* JADX WARNING: type inference failed for: r0v164 */
        /* JADX WARNING: type inference failed for: r0v165 */
        /* JADX WARNING: Multi-variable type inference failed */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean onTransact(int r15, android.os.Parcel r16, android.os.Parcel r17, int r18) throws android.os.RemoteException {
            /*
                r14 = this;
                r6 = r14
                r7 = r15
                r8 = r16
                r0 = 1598968902(0x5f4e5446, float:1.4867585E19)
                r9 = 1
                if (r7 == r0) goto L_0x04b5
                r0 = 0
                switch(r7) {
                    case 1: goto L_0x04a0;
                    case 2: goto L_0x048f;
                    case 3: goto L_0x0476;
                    case 4: goto L_0x045d;
                    case 5: goto L_0x044c;
                    case 6: goto L_0x043b;
                    case 7: goto L_0x042a;
                    case 8: goto L_0x0419;
                    case 9: goto L_0x0408;
                    case 10: goto L_0x03f7;
                    case 11: goto L_0x03e2;
                    case 12: goto L_0x03a1;
                    case 13: goto L_0x0370;
                    case 14: goto L_0x034b;
                    case 15: goto L_0x0326;
                    case 16: goto L_0x02f5;
                    case 17: goto L_0x02d0;
                    case 18: goto L_0x02ab;
                    case 19: goto L_0x0286;
                    case 20: goto L_0x0271;
                    case 21: goto L_0x024a;
                    case 22: goto L_0x0229;
                    case 23: goto L_0x0204;
                    case 24: goto L_0x01e3;
                    case 25: goto L_0x01be;
                    case 26: goto L_0x019d;
                    case 27: goto L_0x018c;
                    case 28: goto L_0x017b;
                    case 29: goto L_0x0166;
                    case 30: goto L_0x0151;
                    case 31: goto L_0x0140;
                    case 32: goto L_0x012f;
                    case 33: goto L_0x010e;
                    case 34: goto L_0x00ed;
                    case 35: goto L_0x00d8;
                    case 36: goto L_0x00a5;
                    case 37: goto L_0x0080;
                    case 38: goto L_0x004d;
                    case 39: goto L_0x0028;
                    case 40: goto L_0x0013;
                    default: goto L_0x000e;
                }
            L_0x000e:
                boolean r0 = super.onTransact(r15, r16, r17, r18)
                return r0
            L_0x0013:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                java.lang.String r1 = r16.readString()
                r6.unsubscribe(r0, r1)
                return r9
            L_0x0028:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                java.lang.String r2 = r16.readString()
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x0048
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0049
            L_0x0048:
            L_0x0049:
                r6.subscribe(r1, r2, r0)
                return r9
            L_0x004d:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r10 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                java.lang.String r11 = r16.readString()
                int r12 = r16.readInt()
                int r13 = r16.readInt()
                int r1 = r16.readInt()
                if (r1 == 0) goto L_0x0076
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
            L_0x0074:
                r5 = r0
                goto L_0x0077
            L_0x0076:
                goto L_0x0074
            L_0x0077:
                r0 = r6
                r1 = r10
                r2 = r11
                r3 = r12
                r4 = r13
                r0.getSearchResult(r1, r2, r3, r4, r5)
                return r9
            L_0x0080:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                java.lang.String r2 = r16.readString()
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x00a0
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x00a1
            L_0x00a0:
            L_0x00a1:
                r6.search(r1, r2, r0)
                return r9
            L_0x00a5:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r10 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                java.lang.String r11 = r16.readString()
                int r12 = r16.readInt()
                int r13 = r16.readInt()
                int r1 = r16.readInt()
                if (r1 == 0) goto L_0x00ce
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
            L_0x00cc:
                r5 = r0
                goto L_0x00cf
            L_0x00ce:
                goto L_0x00cc
            L_0x00cf:
                r0 = r6
                r1 = r10
                r2 = r11
                r3 = r12
                r4 = r13
                r0.getChildren(r1, r2, r3, r4, r5)
                return r9
            L_0x00d8:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                java.lang.String r1 = r16.readString()
                r6.getItem(r0, r1)
                return r9
            L_0x00ed:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                if (r2 == 0) goto L_0x0109
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x010a
            L_0x0109:
            L_0x010a:
                r6.getLibraryRoot(r1, r0)
                return r9
            L_0x010e:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                if (r2 == 0) goto L_0x012a
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x012b
            L_0x012a:
            L_0x012b:
                r6.selectRoute(r1, r0)
                return r9
            L_0x012f:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.unsubscribeRoutesInfo(r0)
                return r9
            L_0x0140:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.subscribeRoutesInfo(r0)
                return r9
            L_0x0151:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                int r1 = r16.readInt()
                r6.setShuffleMode(r0, r1)
                return r9
            L_0x0166:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                int r1 = r16.readInt()
                r6.setRepeatMode(r0, r1)
                return r9
            L_0x017b:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.skipToNextItem(r0)
                return r9
            L_0x018c:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.skipToPreviousItem(r0)
                return r9
            L_0x019d:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                if (r2 == 0) goto L_0x01b9
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x01ba
            L_0x01b9:
            L_0x01ba:
                r6.skipToPlaylistItem(r1, r0)
                return r9
            L_0x01be:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x01de
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x01df
            L_0x01de:
            L_0x01df:
                r6.replacePlaylistItem(r1, r2, r0)
                return r9
            L_0x01e3:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                if (r2 == 0) goto L_0x01ff
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0200
            L_0x01ff:
            L_0x0200:
                r6.removePlaylistItem(r1, r0)
                return r9
            L_0x0204:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x0224
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0225
            L_0x0224:
            L_0x0225:
                r6.addPlaylistItem(r1, r2, r0)
                return r9
            L_0x0229:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                if (r2 == 0) goto L_0x0245
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0246
            L_0x0245:
            L_0x0246:
                r6.updatePlaylistMetadata(r1, r0)
                return r9
            L_0x024a:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                android.os.Parcelable$Creator r2 = android.os.Bundle.CREATOR
                java.util.ArrayList r2 = r8.createTypedArrayList(r2)
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x026c
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x026d
            L_0x026c:
            L_0x026d:
                r6.setPlaylist(r1, r2, r0)
                return r9
            L_0x0271:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                float r1 = r16.readFloat()
                r6.setPlaybackSpeed(r0, r1)
                return r9
            L_0x0286:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                java.lang.String r2 = r16.readString()
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x02a6
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x02a7
            L_0x02a6:
            L_0x02a7:
                r6.setRating(r1, r2, r0)
                return r9
            L_0x02ab:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                java.lang.String r2 = r16.readString()
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x02cb
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x02cc
            L_0x02cb:
            L_0x02cc:
                r6.playFromMediaId(r1, r2, r0)
                return r9
            L_0x02d0:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                java.lang.String r2 = r16.readString()
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x02f0
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x02f1
            L_0x02f0:
            L_0x02f1:
                r6.playFromSearch(r1, r2, r0)
                return r9
            L_0x02f5:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                if (r2 == 0) goto L_0x0311
                android.os.Parcelable$Creator r2 = android.net.Uri.CREATOR
                java.lang.Object r2 = r2.createFromParcel(r8)
                android.net.Uri r2 = (android.net.Uri) r2
                goto L_0x0312
            L_0x0311:
                r2 = r0
            L_0x0312:
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x0321
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0322
            L_0x0321:
            L_0x0322:
                r6.playFromUri(r1, r2, r0)
                return r9
            L_0x0326:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                java.lang.String r2 = r16.readString()
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x0346
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x0347
            L_0x0346:
            L_0x0347:
                r6.prepareFromMediaId(r1, r2, r0)
                return r9
            L_0x034b:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                java.lang.String r2 = r16.readString()
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x036b
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x036c
            L_0x036b:
            L_0x036c:
                r6.prepareFromSearch(r1, r2, r0)
                return r9
            L_0x0370:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                if (r2 == 0) goto L_0x038c
                android.os.Parcelable$Creator r2 = android.net.Uri.CREATOR
                java.lang.Object r2 = r2.createFromParcel(r8)
                android.net.Uri r2 = (android.net.Uri) r2
                goto L_0x038d
            L_0x038c:
                r2 = r0
            L_0x038d:
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x039c
                android.os.Parcelable$Creator r0 = android.os.Bundle.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.Bundle r0 = (android.os.Bundle) r0
                goto L_0x039d
            L_0x039c:
            L_0x039d:
                r6.prepareFromUri(r1, r2, r0)
                return r9
            L_0x03a1:
                java.lang.String r1 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r1)
                android.os.IBinder r1 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r1 = android.support.v4.media.IMediaController2.Stub.asInterface(r1)
                int r2 = r16.readInt()
                if (r2 == 0) goto L_0x03bd
                android.os.Parcelable$Creator r2 = android.os.Bundle.CREATOR
                java.lang.Object r2 = r2.createFromParcel(r8)
                android.os.Bundle r2 = (android.os.Bundle) r2
                goto L_0x03be
            L_0x03bd:
                r2 = r0
            L_0x03be:
                int r3 = r16.readInt()
                if (r3 == 0) goto L_0x03cd
                android.os.Parcelable$Creator r3 = android.os.Bundle.CREATOR
                java.lang.Object r3 = r3.createFromParcel(r8)
                android.os.Bundle r3 = (android.os.Bundle) r3
                goto L_0x03ce
            L_0x03cd:
                r3 = r0
            L_0x03ce:
                int r4 = r16.readInt()
                if (r4 == 0) goto L_0x03dd
                android.os.Parcelable$Creator r0 = android.os.ResultReceiver.CREATOR
                java.lang.Object r0 = r0.createFromParcel(r8)
                android.os.ResultReceiver r0 = (android.os.ResultReceiver) r0
                goto L_0x03de
            L_0x03dd:
            L_0x03de:
                r6.sendCustomCommand(r1, r2, r3, r0)
                return r9
            L_0x03e2:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                long r1 = r16.readLong()
                r6.seekTo(r0, r1)
                return r9
            L_0x03f7:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.rewind(r0)
                return r9
            L_0x0408:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.fastForward(r0)
                return r9
            L_0x0419:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.prepare(r0)
                return r9
            L_0x042a:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.reset(r0)
                return r9
            L_0x043b:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.pause(r0)
                return r9
            L_0x044c:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.play(r0)
                return r9
            L_0x045d:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                int r1 = r16.readInt()
                int r2 = r16.readInt()
                r6.adjustVolume(r0, r1, r2)
                return r9
            L_0x0476:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                int r1 = r16.readInt()
                int r2 = r16.readInt()
                r6.setVolumeTo(r0, r1, r2)
                return r9
            L_0x048f:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                r6.release(r0)
                return r9
            L_0x04a0:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r8.enforceInterface(r0)
                android.os.IBinder r0 = r16.readStrongBinder()
                android.support.v4.media.IMediaController2 r0 = android.support.v4.media.IMediaController2.Stub.asInterface(r0)
                java.lang.String r1 = r16.readString()
                r6.connect(r0, r1)
                return r9
            L_0x04b5:
                java.lang.String r0 = "android.support.v4.media.IMediaSession2"
                r1 = r17
                r1.writeString(r0)
                return r9
            */
            throw new UnsupportedOperationException("Method not decompiled: android.support.v4.media.IMediaSession2.Stub.onTransact(int, android.os.Parcel, android.os.Parcel, int):boolean");
        }
    }

    void addPlaylistItem(IMediaController2 iMediaController2, int i, Bundle bundle) throws RemoteException;

    void adjustVolume(IMediaController2 iMediaController2, int i, int i2) throws RemoteException;

    void connect(IMediaController2 iMediaController2, String str) throws RemoteException;

    void fastForward(IMediaController2 iMediaController2) throws RemoteException;

    void getChildren(IMediaController2 iMediaController2, String str, int i, int i2, Bundle bundle) throws RemoteException;

    void getItem(IMediaController2 iMediaController2, String str) throws RemoteException;

    void getLibraryRoot(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;

    void getSearchResult(IMediaController2 iMediaController2, String str, int i, int i2, Bundle bundle) throws RemoteException;

    void pause(IMediaController2 iMediaController2) throws RemoteException;

    void play(IMediaController2 iMediaController2) throws RemoteException;

    void playFromMediaId(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void playFromSearch(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void playFromUri(IMediaController2 iMediaController2, Uri uri, Bundle bundle) throws RemoteException;

    void prepare(IMediaController2 iMediaController2) throws RemoteException;

    void prepareFromMediaId(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void prepareFromSearch(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void prepareFromUri(IMediaController2 iMediaController2, Uri uri, Bundle bundle) throws RemoteException;

    void release(IMediaController2 iMediaController2) throws RemoteException;

    void removePlaylistItem(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;

    void replacePlaylistItem(IMediaController2 iMediaController2, int i, Bundle bundle) throws RemoteException;

    void reset(IMediaController2 iMediaController2) throws RemoteException;

    void rewind(IMediaController2 iMediaController2) throws RemoteException;

    void search(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void seekTo(IMediaController2 iMediaController2, long j) throws RemoteException;

    void selectRoute(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;

    void sendCustomCommand(IMediaController2 iMediaController2, Bundle bundle, Bundle bundle2, ResultReceiver resultReceiver) throws RemoteException;

    void setPlaybackSpeed(IMediaController2 iMediaController2, float f) throws RemoteException;

    void setPlaylist(IMediaController2 iMediaController2, List<Bundle> list, Bundle bundle) throws RemoteException;

    void setRating(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void setRepeatMode(IMediaController2 iMediaController2, int i) throws RemoteException;

    void setShuffleMode(IMediaController2 iMediaController2, int i) throws RemoteException;

    void setVolumeTo(IMediaController2 iMediaController2, int i, int i2) throws RemoteException;

    void skipToNextItem(IMediaController2 iMediaController2) throws RemoteException;

    void skipToPlaylistItem(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;

    void skipToPreviousItem(IMediaController2 iMediaController2) throws RemoteException;

    void subscribe(IMediaController2 iMediaController2, String str, Bundle bundle) throws RemoteException;

    void subscribeRoutesInfo(IMediaController2 iMediaController2) throws RemoteException;

    void unsubscribe(IMediaController2 iMediaController2, String str) throws RemoteException;

    void unsubscribeRoutesInfo(IMediaController2 iMediaController2) throws RemoteException;

    void updatePlaylistMetadata(IMediaController2 iMediaController2, Bundle bundle) throws RemoteException;
}
