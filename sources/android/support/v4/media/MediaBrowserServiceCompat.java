package android.support.v4.media;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.BundleCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompatApi21;
import android.support.v4.media.MediaBrowserServiceCompatApi23;
import android.support.v4.media.MediaBrowserServiceCompatApi26;
import android.support.v4.media.MediaSessionManager;
import android.support.v4.media.session.IMediaSession;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class MediaBrowserServiceCompat extends Service {
    static final boolean DEBUG = Log.isLoggable("MBServiceCompat", 3);
    final ArrayMap<IBinder, ConnectionRecord> mConnections;
    ConnectionRecord mCurConnection;
    final ServiceHandler mHandler;
    private MediaBrowserServiceImpl mImpl;
    MediaSessionCompat.Token mSession;

    public static final class BrowserRoot {
        private final Bundle mExtras;
        private final String mRootId;

        public BrowserRoot(String rootId, Bundle extras) {
            if (rootId != null) {
                this.mRootId = rootId;
                this.mExtras = extras;
                return;
            }
            throw new IllegalArgumentException("The root id in BrowserRoot cannot be null. Use null for BrowserRoot instead.");
        }

        public String getRootId() {
            return this.mRootId;
        }

        public Bundle getExtras() {
            return this.mExtras;
        }
    }

    private class ConnectionRecord implements IBinder.DeathRecipient {
        public final MediaSessionManager.RemoteUserInfo browserInfo;
        public final ServiceCallbacks callbacks;
        public final int pid;
        public final String pkg;
        public BrowserRoot root;
        public final Bundle rootHints;
        public final HashMap<String, List<Pair<IBinder, Bundle>>> subscriptions = new HashMap<>();
        public final int uid;

        ConnectionRecord(String pkg2, int pid2, int uid2, Bundle rootHints2, ServiceCallbacks callback) {
            this.pkg = pkg2;
            this.pid = pid2;
            this.uid = uid2;
            this.browserInfo = new MediaSessionManager.RemoteUserInfo(pkg2, pid2, uid2);
            this.rootHints = rootHints2;
            this.callbacks = callback;
        }

        public void binderDied() {
            MediaBrowserServiceCompat.this.mHandler.post(new Runnable() {
                public void run() {
                    MediaBrowserServiceCompat.this.mConnections.remove(ConnectionRecord.this.callbacks.asBinder());
                }
            });
        }
    }

    interface MediaBrowserServiceImpl {
        IBinder onBind(Intent intent);

        void onCreate();
    }

    class MediaBrowserServiceImplApi21 implements MediaBrowserServiceImpl, MediaBrowserServiceCompatApi21.ServiceCompatProxy {
        Messenger mMessenger;
        final List<Bundle> mRootExtrasList = new ArrayList();
        Object mServiceObj;

        MediaBrowserServiceImplApi21() {
        }

        public void onCreate() {
            this.mServiceObj = MediaBrowserServiceCompatApi21.createService(MediaBrowserServiceCompat.this, this);
            MediaBrowserServiceCompatApi21.onCreate(this.mServiceObj);
        }

        public IBinder onBind(Intent intent) {
            return MediaBrowserServiceCompatApi21.onBind(this.mServiceObj, intent);
        }

        public MediaBrowserServiceCompatApi21.BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
            IBinder iBinder;
            Bundle rootExtras = null;
            if (!(rootHints == null || rootHints.getInt("extra_client_version", 0) == 0)) {
                rootHints.remove("extra_client_version");
                this.mMessenger = new Messenger(MediaBrowserServiceCompat.this.mHandler);
                rootExtras = new Bundle();
                rootExtras.putInt("extra_service_version", 2);
                BundleCompat.putBinder(rootExtras, "extra_messenger", this.mMessenger.getBinder());
                if (MediaBrowserServiceCompat.this.mSession != null) {
                    IMediaSession extraBinder = MediaBrowserServiceCompat.this.mSession.getExtraBinder();
                    if (extraBinder == null) {
                        iBinder = null;
                    } else {
                        iBinder = extraBinder.asBinder();
                    }
                    BundleCompat.putBinder(rootExtras, "extra_session_binder", iBinder);
                } else {
                    this.mRootExtrasList.add(rootExtras);
                }
            }
            MediaBrowserServiceCompat mediaBrowserServiceCompat = MediaBrowserServiceCompat.this;
            ConnectionRecord connectionRecord = new ConnectionRecord(clientPackageName, -1, clientUid, rootHints, null);
            mediaBrowserServiceCompat.mCurConnection = connectionRecord;
            BrowserRoot root = MediaBrowserServiceCompat.this.onGetRoot(clientPackageName, clientUid, rootHints);
            MediaBrowserServiceCompat.this.mCurConnection = null;
            if (root == null) {
                return null;
            }
            if (rootExtras == null) {
                rootExtras = root.getExtras();
            } else if (root.getExtras() != null) {
                rootExtras.putAll(root.getExtras());
            }
            return new MediaBrowserServiceCompatApi21.BrowserRoot(root.getRootId(), rootExtras);
        }

        public void onLoadChildren(String parentId, final MediaBrowserServiceCompatApi21.ResultWrapper<List<Parcel>> resultWrapper) {
            MediaBrowserServiceCompat.this.onLoadChildren(parentId, new Result<List<MediaBrowserCompat.MediaItem>>(parentId) {
                /* access modifiers changed from: package-private */
                public void onResultSent(List<MediaBrowserCompat.MediaItem> list) {
                    List<Parcel> parcelList = null;
                    if (list != null) {
                        parcelList = new ArrayList<>();
                        for (MediaBrowserCompat.MediaItem item : list) {
                            Parcel parcel = Parcel.obtain();
                            item.writeToParcel(parcel, 0);
                            parcelList.add(parcel);
                        }
                    }
                    resultWrapper.sendResult(parcelList);
                }
            });
        }
    }

    class MediaBrowserServiceImplApi23 extends MediaBrowserServiceImplApi21 implements MediaBrowserServiceCompatApi23.ServiceCompatProxy {
        MediaBrowserServiceImplApi23() {
            super();
        }

        public void onCreate() {
            this.mServiceObj = MediaBrowserServiceCompatApi23.createService(MediaBrowserServiceCompat.this, this);
            MediaBrowserServiceCompatApi21.onCreate(this.mServiceObj);
        }

        public void onLoadItem(String itemId, final MediaBrowserServiceCompatApi21.ResultWrapper<Parcel> resultWrapper) {
            MediaBrowserServiceCompat.this.onLoadItem(itemId, new Result<MediaBrowserCompat.MediaItem>(itemId) {
                /* access modifiers changed from: package-private */
                public void onResultSent(MediaBrowserCompat.MediaItem item) {
                    if (item == null) {
                        resultWrapper.sendResult(null);
                        return;
                    }
                    Parcel parcelItem = Parcel.obtain();
                    item.writeToParcel(parcelItem, 0);
                    resultWrapper.sendResult(parcelItem);
                }
            });
        }
    }

    class MediaBrowserServiceImplApi26 extends MediaBrowserServiceImplApi23 implements MediaBrowserServiceCompatApi26.ServiceCompatProxy {
        MediaBrowserServiceImplApi26() {
            super();
        }

        public void onCreate() {
            this.mServiceObj = MediaBrowserServiceCompatApi26.createService(MediaBrowserServiceCompat.this, this);
            MediaBrowserServiceCompatApi21.onCreate(this.mServiceObj);
        }

        public void onLoadChildren(String parentId, final MediaBrowserServiceCompatApi26.ResultWrapper resultWrapper, Bundle options) {
            MediaBrowserServiceCompat.this.onLoadChildren(parentId, new Result<List<MediaBrowserCompat.MediaItem>>(parentId) {
                /* access modifiers changed from: package-private */
                public void onResultSent(List<MediaBrowserCompat.MediaItem> list) {
                    List<Parcel> parcelList = null;
                    if (list != null) {
                        parcelList = new ArrayList<>();
                        for (MediaBrowserCompat.MediaItem item : list) {
                            Parcel parcel = Parcel.obtain();
                            item.writeToParcel(parcel, 0);
                            parcelList.add(parcel);
                        }
                    }
                    resultWrapper.sendResult(parcelList, getFlags());
                }
            }, options);
        }
    }

    class MediaBrowserServiceImplApi28 extends MediaBrowserServiceImplApi26 {
        MediaBrowserServiceImplApi28() {
            super();
        }
    }

    class MediaBrowserServiceImplBase implements MediaBrowserServiceImpl {
        private Messenger mMessenger;

        MediaBrowserServiceImplBase() {
        }

        public void onCreate() {
            this.mMessenger = new Messenger(MediaBrowserServiceCompat.this.mHandler);
        }

        public IBinder onBind(Intent intent) {
            if ("android.media.browse.MediaBrowserService".equals(intent.getAction())) {
                return this.mMessenger.getBinder();
            }
            return null;
        }
    }

    public static class Result<T> {
        private final Object mDebug;
        private boolean mDetachCalled;
        private int mFlags;
        private boolean mSendErrorCalled;
        private boolean mSendResultCalled;

        Result(Object debug) {
            this.mDebug = debug;
        }

        public void sendResult(T result) {
            if (this.mSendResultCalled || this.mSendErrorCalled) {
                throw new IllegalStateException("sendResult() called when either sendResult() or sendError() had already been called for: " + this.mDebug);
            }
            this.mSendResultCalled = true;
            onResultSent(result);
        }

        public void sendError(Bundle extras) {
            if (this.mSendResultCalled || this.mSendErrorCalled) {
                throw new IllegalStateException("sendError() called when either sendResult() or sendError() had already been called for: " + this.mDebug);
            }
            this.mSendErrorCalled = true;
            onErrorSent(extras);
        }

        /* access modifiers changed from: package-private */
        public boolean isDone() {
            return this.mDetachCalled || this.mSendResultCalled || this.mSendErrorCalled;
        }

        /* access modifiers changed from: package-private */
        public void setFlags(int flags) {
            this.mFlags = flags;
        }

        /* access modifiers changed from: package-private */
        public int getFlags() {
            return this.mFlags;
        }

        /* access modifiers changed from: package-private */
        public void onResultSent(T t) {
        }

        /* access modifiers changed from: package-private */
        public void onErrorSent(Bundle extras) {
            throw new UnsupportedOperationException("It is not supported to send an error for " + this.mDebug);
        }
    }

    private class ServiceBinderImpl {
        final /* synthetic */ MediaBrowserServiceCompat this$0;

        public void connect(String pkg, int pid, int uid, Bundle rootHints, ServiceCallbacks callbacks) {
            if (this.this$0.isValidPackage(pkg, uid)) {
                ServiceHandler serviceHandler = this.this$0.mHandler;
                final ServiceCallbacks serviceCallbacks = callbacks;
                final String str = pkg;
                final int i = pid;
                final int i2 = uid;
                final Bundle bundle = rootHints;
                AnonymousClass1 r1 = new Runnable() {
                    public void run() {
                        IBinder b = serviceCallbacks.asBinder();
                        ServiceBinderImpl.this.this$0.mConnections.remove(b);
                        ConnectionRecord connectionRecord = new ConnectionRecord(str, i, i2, bundle, serviceCallbacks);
                        ServiceBinderImpl.this.this$0.mCurConnection = connectionRecord;
                        connectionRecord.root = ServiceBinderImpl.this.this$0.onGetRoot(str, i2, bundle);
                        ServiceBinderImpl.this.this$0.mCurConnection = null;
                        if (connectionRecord.root == null) {
                            Log.i("MBServiceCompat", "No root for client " + str + " from service " + getClass().getName());
                            try {
                                serviceCallbacks.onConnectFailed();
                            } catch (RemoteException e) {
                                Log.w("MBServiceCompat", "Calling onConnectFailed() failed. Ignoring. pkg=" + str);
                            }
                        } else {
                            try {
                                ServiceBinderImpl.this.this$0.mConnections.put(b, connectionRecord);
                                b.linkToDeath(connectionRecord, 0);
                                if (ServiceBinderImpl.this.this$0.mSession != null) {
                                    serviceCallbacks.onConnect(connectionRecord.root.getRootId(), ServiceBinderImpl.this.this$0.mSession, connectionRecord.root.getExtras());
                                }
                            } catch (RemoteException e2) {
                                Log.w("MBServiceCompat", "Calling onConnect() failed. Dropping client. pkg=" + str);
                                ServiceBinderImpl.this.this$0.mConnections.remove(b);
                            }
                        }
                    }
                };
                serviceHandler.postOrRun(r1);
                return;
            }
            throw new IllegalArgumentException("Package/uid mismatch: uid=" + uid + " package=" + pkg);
        }

        public void disconnect(final ServiceCallbacks callbacks) {
            this.this$0.mHandler.postOrRun(new Runnable() {
                public void run() {
                    ConnectionRecord old = ServiceBinderImpl.this.this$0.mConnections.remove(callbacks.asBinder());
                    if (old != null) {
                        old.callbacks.asBinder().unlinkToDeath(old, 0);
                    }
                }
            });
        }

        public void addSubscription(String id, IBinder token, Bundle options, ServiceCallbacks callbacks) {
            ServiceHandler serviceHandler = this.this$0.mHandler;
            final ServiceCallbacks serviceCallbacks = callbacks;
            final String str = id;
            final IBinder iBinder = token;
            final Bundle bundle = options;
            AnonymousClass3 r1 = new Runnable() {
                public void run() {
                    ConnectionRecord connection = ServiceBinderImpl.this.this$0.mConnections.get(serviceCallbacks.asBinder());
                    if (connection == null) {
                        Log.w("MBServiceCompat", "addSubscription for callback that isn't registered id=" + str);
                        return;
                    }
                    ServiceBinderImpl.this.this$0.addSubscription(str, connection, iBinder, bundle);
                }
            };
            serviceHandler.postOrRun(r1);
        }

        public void removeSubscription(final String id, final IBinder token, final ServiceCallbacks callbacks) {
            this.this$0.mHandler.postOrRun(new Runnable() {
                public void run() {
                    ConnectionRecord connection = ServiceBinderImpl.this.this$0.mConnections.get(callbacks.asBinder());
                    if (connection == null) {
                        Log.w("MBServiceCompat", "removeSubscription for callback that isn't registered id=" + id);
                        return;
                    }
                    if (!ServiceBinderImpl.this.this$0.removeSubscription(id, connection, token)) {
                        Log.w("MBServiceCompat", "removeSubscription called for " + id + " which is not subscribed");
                    }
                }
            });
        }

        public void getMediaItem(final String mediaId, final ResultReceiver receiver, final ServiceCallbacks callbacks) {
            if (!TextUtils.isEmpty(mediaId) && receiver != null) {
                this.this$0.mHandler.postOrRun(new Runnable() {
                    public void run() {
                        ConnectionRecord connection = ServiceBinderImpl.this.this$0.mConnections.get(callbacks.asBinder());
                        if (connection == null) {
                            Log.w("MBServiceCompat", "getMediaItem for callback that isn't registered id=" + mediaId);
                            return;
                        }
                        ServiceBinderImpl.this.this$0.performLoadItem(mediaId, connection, receiver);
                    }
                });
            }
        }

        public void registerCallbacks(ServiceCallbacks callbacks, String pkg, int pid, int uid, Bundle rootHints) {
            ServiceHandler serviceHandler = this.this$0.mHandler;
            final ServiceCallbacks serviceCallbacks = callbacks;
            final String str = pkg;
            final int i = pid;
            final int i2 = uid;
            final Bundle bundle = rootHints;
            AnonymousClass6 r1 = new Runnable() {
                public void run() {
                    IBinder b = serviceCallbacks.asBinder();
                    ServiceBinderImpl.this.this$0.mConnections.remove(b);
                    ConnectionRecord connectionRecord = new ConnectionRecord(str, i, i2, bundle, serviceCallbacks);
                    ServiceBinderImpl.this.this$0.mConnections.put(b, connectionRecord);
                    try {
                        b.linkToDeath(connectionRecord, 0);
                    } catch (RemoteException e) {
                        Log.w("MBServiceCompat", "IBinder is already dead.");
                    }
                }
            };
            serviceHandler.postOrRun(r1);
        }

        public void unregisterCallbacks(final ServiceCallbacks callbacks) {
            this.this$0.mHandler.postOrRun(new Runnable() {
                public void run() {
                    IBinder b = callbacks.asBinder();
                    ConnectionRecord old = ServiceBinderImpl.this.this$0.mConnections.remove(b);
                    if (old != null) {
                        b.unlinkToDeath(old, 0);
                    }
                }
            });
        }

        public void search(String query, Bundle extras, ResultReceiver receiver, ServiceCallbacks callbacks) {
            if (!TextUtils.isEmpty(query) && receiver != null) {
                ServiceHandler serviceHandler = this.this$0.mHandler;
                final ServiceCallbacks serviceCallbacks = callbacks;
                final String str = query;
                final Bundle bundle = extras;
                final ResultReceiver resultReceiver = receiver;
                AnonymousClass8 r1 = new Runnable() {
                    public void run() {
                        ConnectionRecord connection = ServiceBinderImpl.this.this$0.mConnections.get(serviceCallbacks.asBinder());
                        if (connection == null) {
                            Log.w("MBServiceCompat", "search for callback that isn't registered query=" + str);
                            return;
                        }
                        ServiceBinderImpl.this.this$0.performSearch(str, bundle, connection, resultReceiver);
                    }
                };
                serviceHandler.postOrRun(r1);
            }
        }

        public void sendCustomAction(String action, Bundle extras, ResultReceiver receiver, ServiceCallbacks callbacks) {
            if (!TextUtils.isEmpty(action) && receiver != null) {
                ServiceHandler serviceHandler = this.this$0.mHandler;
                final ServiceCallbacks serviceCallbacks = callbacks;
                final String str = action;
                final Bundle bundle = extras;
                final ResultReceiver resultReceiver = receiver;
                AnonymousClass9 r1 = new Runnable() {
                    public void run() {
                        ConnectionRecord connection = ServiceBinderImpl.this.this$0.mConnections.get(serviceCallbacks.asBinder());
                        if (connection == null) {
                            Log.w("MBServiceCompat", "sendCustomAction for callback that isn't registered action=" + str + ", extras=" + bundle);
                            return;
                        }
                        ServiceBinderImpl.this.this$0.performCustomAction(str, bundle, connection, resultReceiver);
                    }
                };
                serviceHandler.postOrRun(r1);
            }
        }
    }

    private interface ServiceCallbacks {
        IBinder asBinder();

        void onConnect(String str, MediaSessionCompat.Token token, Bundle bundle) throws RemoteException;

        void onConnectFailed() throws RemoteException;

        void onLoadChildren(String str, List<MediaBrowserCompat.MediaItem> list, Bundle bundle, Bundle bundle2) throws RemoteException;
    }

    private static class ServiceCallbacksCompat implements ServiceCallbacks {
        final Messenger mCallbacks;

        ServiceCallbacksCompat(Messenger callbacks) {
            this.mCallbacks = callbacks;
        }

        public IBinder asBinder() {
            return this.mCallbacks.getBinder();
        }

        public void onConnect(String root, MediaSessionCompat.Token session, Bundle extras) throws RemoteException {
            if (extras == null) {
                extras = new Bundle();
            }
            extras.putInt("extra_service_version", 2);
            Bundle data = new Bundle();
            data.putString("data_media_item_id", root);
            data.putParcelable("data_media_session_token", session);
            data.putBundle("data_root_hints", extras);
            sendRequest(1, data);
        }

        public void onConnectFailed() throws RemoteException {
            sendRequest(2, null);
        }

        public void onLoadChildren(String mediaId, List<MediaBrowserCompat.MediaItem> list, Bundle options, Bundle notifyChildrenChangedOptions) throws RemoteException {
            Bundle data = new Bundle();
            data.putString("data_media_item_id", mediaId);
            data.putBundle("data_options", options);
            data.putBundle("data_notify_children_changed_options", notifyChildrenChangedOptions);
            if (list != null) {
                data.putParcelableArrayList("data_media_item_list", list instanceof ArrayList ? (ArrayList) list : new ArrayList(list));
            }
            sendRequest(3, data);
        }

        private void sendRequest(int what, Bundle data) throws RemoteException {
            Message msg = Message.obtain();
            msg.what = what;
            msg.arg1 = 2;
            msg.setData(data);
            this.mCallbacks.send(msg);
        }
    }

    private final class ServiceHandler extends Handler {
        private final ServiceBinderImpl mServiceBinderImpl;

        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            switch (msg.what) {
                case 1:
                    this.mServiceBinderImpl.connect(data.getString("data_package_name"), data.getInt("data_calling_pid"), data.getInt("data_calling_uid"), data.getBundle("data_root_hints"), new ServiceCallbacksCompat(msg.replyTo));
                    return;
                case 2:
                    this.mServiceBinderImpl.disconnect(new ServiceCallbacksCompat(msg.replyTo));
                    return;
                case 3:
                    this.mServiceBinderImpl.addSubscription(data.getString("data_media_item_id"), BundleCompat.getBinder(data, "data_callback_token"), data.getBundle("data_options"), new ServiceCallbacksCompat(msg.replyTo));
                    return;
                case 4:
                    this.mServiceBinderImpl.removeSubscription(data.getString("data_media_item_id"), BundleCompat.getBinder(data, "data_callback_token"), new ServiceCallbacksCompat(msg.replyTo));
                    return;
                case 5:
                    this.mServiceBinderImpl.getMediaItem(data.getString("data_media_item_id"), (ResultReceiver) data.getParcelable("data_result_receiver"), new ServiceCallbacksCompat(msg.replyTo));
                    return;
                case 6:
                    this.mServiceBinderImpl.registerCallbacks(new ServiceCallbacksCompat(msg.replyTo), data.getString("data_package_name"), data.getInt("data_calling_pid"), data.getInt("data_calling_uid"), data.getBundle("data_root_hints"));
                    return;
                case 7:
                    this.mServiceBinderImpl.unregisterCallbacks(new ServiceCallbacksCompat(msg.replyTo));
                    return;
                case 8:
                    this.mServiceBinderImpl.search(data.getString("data_search_query"), data.getBundle("data_search_extras"), (ResultReceiver) data.getParcelable("data_result_receiver"), new ServiceCallbacksCompat(msg.replyTo));
                    return;
                case 9:
                    this.mServiceBinderImpl.sendCustomAction(data.getString("data_custom_action"), data.getBundle("data_custom_action_extras"), (ResultReceiver) data.getParcelable("data_result_receiver"), new ServiceCallbacksCompat(msg.replyTo));
                    return;
                default:
                    Log.w("MBServiceCompat", "Unhandled message: " + msg + "\n  Service version: " + 2 + "\n  Client version: " + msg.arg1);
                    return;
            }
        }

        public boolean sendMessageAtTime(Message msg, long uptimeMillis) {
            Bundle data = msg.getData();
            data.setClassLoader(MediaBrowserCompat.class.getClassLoader());
            data.putInt("data_calling_uid", Binder.getCallingUid());
            data.putInt("data_calling_pid", Binder.getCallingPid());
            return super.sendMessageAtTime(msg, uptimeMillis);
        }

        public void postOrRun(Runnable r) {
            if (Thread.currentThread() == getLooper().getThread()) {
                r.run();
            } else {
                post(r);
            }
        }
    }

    public abstract BrowserRoot onGetRoot(String str, int i, Bundle bundle);

    public abstract void onLoadChildren(String str, Result<List<MediaBrowserCompat.MediaItem>> result);

    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 28) {
            this.mImpl = new MediaBrowserServiceImplApi28();
        } else if (Build.VERSION.SDK_INT >= 26) {
            this.mImpl = new MediaBrowserServiceImplApi26();
        } else if (Build.VERSION.SDK_INT >= 23) {
            this.mImpl = new MediaBrowserServiceImplApi23();
        } else if (Build.VERSION.SDK_INT >= 21) {
            this.mImpl = new MediaBrowserServiceImplApi21();
        } else {
            this.mImpl = new MediaBrowserServiceImplBase();
        }
        this.mImpl.onCreate();
    }

    public IBinder onBind(Intent intent) {
        return this.mImpl.onBind(intent);
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    public void onLoadChildren(String parentId, Result<List<MediaBrowserCompat.MediaItem>> result, Bundle options) {
        result.setFlags(1);
        onLoadChildren(parentId, result);
    }

    public void onLoadItem(String itemId, Result<MediaBrowserCompat.MediaItem> result) {
        result.setFlags(2);
        result.sendResult(null);
    }

    public void onSearch(String query, Bundle extras, Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.setFlags(4);
        result.sendResult(null);
    }

    public void onCustomAction(String action, Bundle extras, Result<Bundle> result) {
        result.sendError(null);
    }

    /* access modifiers changed from: package-private */
    public boolean isValidPackage(String pkg, int uid) {
        if (pkg == null) {
            return false;
        }
        for (String equals : getPackageManager().getPackagesForUid(uid)) {
            if (equals.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: package-private */
    public void addSubscription(String id, ConnectionRecord connection, IBinder token, Bundle options) {
        List<Pair<IBinder, Bundle>> callbackList = connection.subscriptions.get(id);
        if (callbackList == null) {
            callbackList = new ArrayList<>();
        }
        for (Pair<IBinder, Bundle> callback : callbackList) {
            if (token == callback.first && MediaBrowserCompatUtils.areSameOptions(options, (Bundle) callback.second)) {
                return;
            }
        }
        callbackList.add(new Pair(token, options));
        connection.subscriptions.put(id, callbackList);
        performLoadChildren(id, connection, options, null);
    }

    /* access modifiers changed from: package-private */
    public boolean removeSubscription(String id, ConnectionRecord connection, IBinder token) {
        if (token == null) {
            return connection.subscriptions.remove(id) != null;
        }
        boolean removed = false;
        List<Pair<IBinder, Bundle>> callbackList = connection.subscriptions.get(id);
        if (callbackList != null) {
            Iterator<Pair<IBinder, Bundle>> iter = callbackList.iterator();
            while (iter.hasNext()) {
                if (token == iter.next().first) {
                    removed = true;
                    iter.remove();
                }
            }
            if (callbackList.size() == 0) {
                connection.subscriptions.remove(id);
            }
        }
        return removed;
    }

    /* access modifiers changed from: package-private */
    public void performLoadChildren(String parentId, ConnectionRecord connection, Bundle subscribeOptions, Bundle notifyChildrenChangedOptions) {
        final ConnectionRecord connectionRecord = connection;
        final String str = parentId;
        final Bundle bundle = subscribeOptions;
        final Bundle bundle2 = notifyChildrenChangedOptions;
        AnonymousClass1 r0 = new Result<List<MediaBrowserCompat.MediaItem>>(parentId) {
            /* access modifiers changed from: package-private */
            public void onResultSent(List<MediaBrowserCompat.MediaItem> list) {
                if (MediaBrowserServiceCompat.this.mConnections.get(connectionRecord.callbacks.asBinder()) != connectionRecord) {
                    if (MediaBrowserServiceCompat.DEBUG) {
                        Log.d("MBServiceCompat", "Not sending onLoadChildren result for connection that has been disconnected. pkg=" + connectionRecord.pkg + " id=" + str);
                    }
                    return;
                }
                try {
                    connectionRecord.callbacks.onLoadChildren(str, (getFlags() & 1) != 0 ? MediaBrowserServiceCompat.this.applyOptions(list, bundle) : list, bundle, bundle2);
                } catch (RemoteException e) {
                    Log.w("MBServiceCompat", "Calling onLoadChildren() failed for id=" + str + " package=" + connectionRecord.pkg);
                }
            }
        };
        this.mCurConnection = connection;
        if (subscribeOptions == null) {
            onLoadChildren(parentId, r0);
        } else {
            onLoadChildren(parentId, r0, subscribeOptions);
        }
        this.mCurConnection = null;
        if (!r0.isDone()) {
            throw new IllegalStateException("onLoadChildren must call detach() or sendResult() before returning for package=" + connection.pkg + " id=" + parentId);
        }
    }

    /* access modifiers changed from: package-private */
    public List<MediaBrowserCompat.MediaItem> applyOptions(List<MediaBrowserCompat.MediaItem> list, Bundle options) {
        if (list == null) {
            return null;
        }
        int page = options.getInt("android.media.browse.extra.PAGE", -1);
        int pageSize = options.getInt("android.media.browse.extra.PAGE_SIZE", -1);
        if (page == -1 && pageSize == -1) {
            return list;
        }
        int fromIndex = pageSize * page;
        int toIndex = fromIndex + pageSize;
        if (page < 0 || pageSize < 1 || fromIndex >= list.size()) {
            return Collections.EMPTY_LIST;
        }
        if (toIndex > list.size()) {
            toIndex = list.size();
        }
        return list.subList(fromIndex, toIndex);
    }

    /* access modifiers changed from: package-private */
    public void performLoadItem(String itemId, ConnectionRecord connection, final ResultReceiver receiver) {
        Result<MediaBrowserCompat.MediaItem> result = new Result<MediaBrowserCompat.MediaItem>(itemId) {
            /* access modifiers changed from: package-private */
            public void onResultSent(MediaBrowserCompat.MediaItem item) {
                if ((getFlags() & 2) != 0) {
                    receiver.send(-1, null);
                    return;
                }
                Bundle bundle = new Bundle();
                bundle.putParcelable("media_item", item);
                receiver.send(0, bundle);
            }
        };
        this.mCurConnection = connection;
        onLoadItem(itemId, result);
        this.mCurConnection = null;
        if (!result.isDone()) {
            throw new IllegalStateException("onLoadItem must call detach() or sendResult() before returning for id=" + itemId);
        }
    }

    /* access modifiers changed from: package-private */
    public void performSearch(String query, Bundle extras, ConnectionRecord connection, final ResultReceiver receiver) {
        Result<List<MediaBrowserCompat.MediaItem>> result = new Result<List<MediaBrowserCompat.MediaItem>>(query) {
            /* access modifiers changed from: package-private */
            public void onResultSent(List<MediaBrowserCompat.MediaItem> items) {
                if ((getFlags() & 4) != 0 || items == null) {
                    receiver.send(-1, null);
                    return;
                }
                Bundle bundle = new Bundle();
                bundle.putParcelableArray("search_results", (Parcelable[]) items.toArray(new MediaBrowserCompat.MediaItem[0]));
                receiver.send(0, bundle);
            }
        };
        this.mCurConnection = connection;
        onSearch(query, extras, result);
        this.mCurConnection = null;
        if (!result.isDone()) {
            throw new IllegalStateException("onSearch must call detach() or sendResult() before returning for query=" + query);
        }
    }

    /* access modifiers changed from: package-private */
    public void performCustomAction(String action, Bundle extras, ConnectionRecord connection, final ResultReceiver receiver) {
        Result<Bundle> result = new Result<Bundle>(action) {
            /* access modifiers changed from: package-private */
            public void onResultSent(Bundle result) {
                receiver.send(0, result);
            }

            /* access modifiers changed from: package-private */
            public void onErrorSent(Bundle data) {
                receiver.send(-1, data);
            }
        };
        this.mCurConnection = connection;
        onCustomAction(action, extras, result);
        this.mCurConnection = null;
        if (!result.isDone()) {
            throw new IllegalStateException("onCustomAction must call detach() or sendResult() or sendError() before returning for action=" + action + " extras=" + extras);
        }
    }
}
