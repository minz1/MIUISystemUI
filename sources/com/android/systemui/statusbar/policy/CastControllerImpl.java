package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.media.MediaRouter;
import android.media.projection.MediaProjectionInfo;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.util.ArrayMap;
import android.util.Log;
import com.android.systemui.statusbar.policy.CastController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;

public class CastControllerImpl implements CastController {
    /* access modifiers changed from: private */
    public static final boolean DEBUG = Log.isLoggable("CastController", 3);
    private boolean mCallbackRegistered;
    private final ArrayList<CastController.Callback> mCallbacks = new ArrayList<>();
    private final Context mContext;
    private boolean mDiscovering;
    private final Object mDiscoveringLock = new Object();
    private final MediaRouter.SimpleCallback mMediaCallback = new MediaRouter.SimpleCallback() {
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteAdded: " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteChanged: " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteRemoved: " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteSelected(" + type + "): " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }

        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo route) {
            if (CastControllerImpl.DEBUG) {
                Log.d("CastController", "onRouteUnselected(" + type + "): " + CastControllerImpl.routeToString(route));
            }
            CastControllerImpl.this.updateRemoteDisplays();
        }
    };
    private final MediaRouter mMediaRouter;
    private MediaProjectionInfo mProjection;
    private final MediaProjectionManager.Callback mProjectionCallback = new MediaProjectionManager.Callback() {
        public void onStart(MediaProjectionInfo info) {
            CastControllerImpl.this.setProjection(info, true);
        }

        public void onStop(MediaProjectionInfo info) {
            CastControllerImpl.this.setProjection(info, false);
        }
    };
    private final Object mProjectionLock = new Object();
    private final MediaProjectionManager mProjectionManager;
    private final ArrayMap<String, MediaRouter.RouteInfo> mRoutes = new ArrayMap<>();

    public CastControllerImpl(Context context) {
        this.mContext = context;
        this.mMediaRouter = (MediaRouter) context.getSystemService("media_router");
        this.mProjectionManager = (MediaProjectionManager) context.getSystemService("media_projection");
        this.mProjection = this.mProjectionManager.getActiveProjectionInfo();
        this.mProjectionManager.addCallback(this.mProjectionCallback, new Handler());
        if (DEBUG) {
            Log.d("CastController", "new CastController()");
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("CastController state:");
        pw.print("  mDiscovering=");
        pw.println(this.mDiscovering);
        pw.print("  mCallbackRegistered=");
        pw.println(this.mCallbackRegistered);
        pw.print("  mCallbacks.size=");
        pw.println(this.mCallbacks.size());
        pw.print("  mRoutes.size=");
        pw.println(this.mRoutes.size());
        for (int i = 0; i < this.mRoutes.size(); i++) {
            pw.print("    ");
            pw.println(routeToString(this.mRoutes.valueAt(i)));
        }
        pw.print("  mProjection=");
        pw.println(this.mProjection);
    }

    public void addCallback(CastController.Callback callback) {
        this.mCallbacks.add(callback);
        fireOnCastDevicesChanged(callback);
        synchronized (this.mDiscoveringLock) {
            handleDiscoveryChangeLocked();
        }
    }

    public void removeCallback(CastController.Callback callback) {
        this.mCallbacks.remove(callback);
        synchronized (this.mDiscoveringLock) {
            handleDiscoveryChangeLocked();
        }
    }

    private void handleDiscoveryChangeLocked() {
        if (this.mCallbackRegistered) {
            this.mMediaRouter.removeCallback(this.mMediaCallback);
            this.mCallbackRegistered = false;
        }
        if (this.mDiscovering) {
            this.mMediaRouter.addCallback(4, this.mMediaCallback, 4);
            this.mCallbackRegistered = true;
        } else if (this.mCallbacks.size() != 0) {
            this.mMediaRouter.addCallback(4, this.mMediaCallback, 8);
            this.mCallbackRegistered = true;
        }
    }

    /* access modifiers changed from: private */
    public void setProjection(MediaProjectionInfo projection, boolean started) {
        boolean changed = false;
        MediaProjectionInfo oldProjection = this.mProjection;
        synchronized (this.mProjectionLock) {
            boolean isCurrent = Objects.equals(projection, this.mProjection);
            if (started && !isCurrent) {
                this.mProjection = projection;
                changed = true;
            } else if (!started && isCurrent) {
                this.mProjection = null;
                changed = true;
            }
        }
        if (changed) {
            if (DEBUG) {
                Log.d("CastController", "setProjection: " + oldProjection + " -> " + this.mProjection);
            }
            fireOnCastDevicesChanged();
        }
    }

    /* access modifiers changed from: private */
    public void updateRemoteDisplays() {
        synchronized (this.mRoutes) {
            this.mRoutes.clear();
            int n = this.mMediaRouter.getRouteCount();
            for (int i = 0; i < n; i++) {
                MediaRouter.RouteInfo route = this.mMediaRouter.getRouteAt(i);
                if (route.isEnabled()) {
                    if (route.matchesTypes(4)) {
                        ensureTagExists(route);
                        this.mRoutes.put(route.getTag().toString(), route);
                    }
                }
            }
            MediaRouter.RouteInfo selected = this.mMediaRouter.getSelectedRoute(4);
            if (selected != null && !selected.isDefault()) {
                ensureTagExists(selected);
                this.mRoutes.put(selected.getTag().toString(), selected);
            }
        }
        fireOnCastDevicesChanged();
    }

    private void ensureTagExists(MediaRouter.RouteInfo route) {
        if (route.getTag() == null) {
            route.setTag(UUID.randomUUID().toString());
        }
    }

    private void fireOnCastDevicesChanged() {
        Iterator<CastController.Callback> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            fireOnCastDevicesChanged(it.next());
        }
    }

    private void fireOnCastDevicesChanged(CastController.Callback callback) {
        callback.onCastDevicesChanged();
    }

    /* access modifiers changed from: private */
    public static String routeToString(MediaRouter.RouteInfo route) {
        if (route == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(route.getName());
        sb.append('/');
        sb.append(route.getDescription());
        sb.append('@');
        sb.append(route.getDeviceAddress());
        sb.append(",status=");
        StringBuilder sb2 = sb.append(route.getStatus());
        if (route.isDefault()) {
            sb2.append(",default");
        }
        if (route.isEnabled()) {
            sb2.append(",enabled");
        }
        if (route.isConnecting()) {
            sb2.append(",connecting");
        }
        if (route.isSelected()) {
            sb2.append(",selected");
        }
        sb2.append(",id=");
        sb2.append(route.getTag());
        return sb2.toString();
    }
}
