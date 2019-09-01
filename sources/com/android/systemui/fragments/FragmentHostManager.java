package com.android.systemui.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.Plugin;
import com.android.systemui.util.InterestingConfigChanges;
import com.android.systemui.util.leak.LeakDetector;
import com.miui.systemui.support.v4.app.Fragment;
import com.miui.systemui.support.v4.app.FragmentController;
import com.miui.systemui.support.v4.app.FragmentHostCallback;
import com.miui.systemui.support.v4.app.FragmentManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class FragmentHostManager {
    private final InterestingConfigChanges mConfigChanges = new InterestingConfigChanges(-1073741052);
    /* access modifiers changed from: private */
    public final Context mContext;
    private FragmentController mFragments;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler(Looper.getMainLooper());
    private FragmentManager.FragmentLifecycleCallbacks mLifecycleCallbacks;
    private final HashMap<String, ArrayList<FragmentListener>> mListeners = new HashMap<>();
    private final FragmentService mManager;
    /* access modifiers changed from: private */
    public final PluginFragmentManager mPlugins = new PluginFragmentManager();
    private final View mRootView;

    public interface FragmentListener {
        void onFragmentViewCreated(String str, Fragment fragment);

        void onFragmentViewDestroyed(String str, Fragment fragment);
    }

    class HostCallbacks extends FragmentHostCallback<FragmentHostManager> {
        public HostCallbacks() {
            super(FragmentHostManager.this.mContext, FragmentHostManager.this.mHandler, 0);
        }

        public void onDump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            FragmentHostManager.this.dump(prefix, fd, writer, args);
        }

        public Fragment instantiate(Context context, String className, Bundle arguments) {
            return FragmentHostManager.this.mPlugins.instantiate(context, className, arguments);
        }

        public boolean onShouldSaveFragmentState(Fragment fragment) {
            return true;
        }

        public LayoutInflater onGetLayoutInflater() {
            return LayoutInflater.from(FragmentHostManager.this.mContext).cloneInContext(FragmentHostManager.this.mContext);
        }

        public boolean onHasWindowAnimations() {
            return false;
        }

        public int onGetWindowAnimations() {
            return 0;
        }

        public View onFindViewById(int id) {
            return FragmentHostManager.this.findViewById(id);
        }

        public boolean onHasView() {
            return true;
        }
    }

    class PluginFragmentManager {
        private final ArrayMap<String, Context> mPluginLookup = new ArrayMap<>();

        PluginFragmentManager() {
        }

        public void removePlugin(String tag, String currentClass, String defaultClass) {
            Fragment fragment = FragmentHostManager.this.getFragmentManager().findFragmentByTag(tag);
            this.mPluginLookup.remove(currentClass);
            FragmentHostManager.this.getFragmentManager().beginTransaction().replace(((View) fragment.getView().getParent()).getId(), instantiate(FragmentHostManager.this.mContext, defaultClass, null), tag).commit();
            reloadFragments();
        }

        public void setCurrentPlugin(String tag, String currentClass, Context context) {
            Fragment fragment = FragmentHostManager.this.getFragmentManager().findFragmentByTag(tag);
            this.mPluginLookup.put(currentClass, context);
            FragmentHostManager.this.getFragmentManager().beginTransaction().replace(((View) fragment.getView().getParent()).getId(), instantiate(context, currentClass, null), tag).commit();
            reloadFragments();
        }

        private void reloadFragments() {
            FragmentHostManager.this.createFragmentHost(FragmentHostManager.this.destroyFragmentHost());
        }

        /* access modifiers changed from: package-private */
        public Fragment instantiate(Context context, String className, Bundle arguments) {
            Context pluginContext = this.mPluginLookup.get(className);
            if (pluginContext == null) {
                return Fragment.instantiate(context, className, arguments);
            }
            Fragment f = Fragment.instantiate(pluginContext, className, arguments);
            if (f instanceof Plugin) {
                ((Plugin) f).onCreate(FragmentHostManager.this.mContext, pluginContext);
            }
            return f;
        }
    }

    FragmentHostManager(Context context, FragmentService manager, View rootView) {
        this.mContext = context;
        this.mManager = manager;
        this.mRootView = rootView;
        this.mConfigChanges.applyNewConfig(context.getResources());
        createFragmentHost(null);
    }

    /* access modifiers changed from: private */
    public void createFragmentHost(Parcelable savedState) {
        this.mFragments = FragmentController.createController(new HostCallbacks());
        this.mFragments.attachHost(null);
        this.mLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            public void onFragmentViewCreated(FragmentManager fm, Fragment f, View v, Bundle savedInstanceState) {
                FragmentHostManager.this.onFragmentViewCreated(f);
            }

            public void onFragmentViewDestroyed(FragmentManager fm, Fragment f) {
                FragmentHostManager.this.onFragmentViewDestroyed(f);
            }

            public void onFragmentDestroyed(FragmentManager fm, Fragment f) {
                ((LeakDetector) Dependency.get(LeakDetector.class)).trackGarbage(f);
            }
        };
        getFragmentManager().registerFragmentLifecycleCallbacks(this.mLifecycleCallbacks, true);
        if (savedState != null) {
            this.mFragments.restoreAllState(savedState, null);
        }
        this.mFragments.dispatchCreate();
        this.mFragments.dispatchStart();
        this.mFragments.dispatchResume();
    }

    /* access modifiers changed from: private */
    public Parcelable destroyFragmentHost() {
        this.mFragments.dispatchPause();
        Parcelable p = this.mFragments.saveAllState();
        this.mFragments.dispatchStop();
        this.mFragments.dispatchDestroy();
        getFragmentManager().unregisterFragmentLifecycleCallbacks(this.mLifecycleCallbacks);
        return p;
    }

    public FragmentHostManager addTagListener(String tag, FragmentListener listener) {
        ArrayList<FragmentListener> listeners = this.mListeners.get(tag);
        if (listeners == null) {
            listeners = new ArrayList<>();
            this.mListeners.put(tag, listeners);
        }
        listeners.add(listener);
        Fragment current = getFragmentManager().findFragmentByTag(tag);
        if (!(current == null || current.getView() == null)) {
            listener.onFragmentViewCreated(tag, current);
        }
        return this;
    }

    public void removeTagListener(String tag, FragmentListener listener) {
        ArrayList<FragmentListener> listeners = this.mListeners.get(tag);
        if (listeners != null && listeners.remove(listener) && listeners.size() == 0) {
            this.mListeners.remove(tag);
        }
    }

    /* access modifiers changed from: private */
    public void onFragmentViewCreated(Fragment fragment) {
        String tag = fragment.getTag();
        ArrayList<FragmentListener> listeners = this.mListeners.get(tag);
        if (listeners != null) {
            Iterator<FragmentListener> it = listeners.iterator();
            while (it.hasNext()) {
                it.next().onFragmentViewCreated(tag, fragment);
            }
        }
    }

    /* access modifiers changed from: private */
    public void onFragmentViewDestroyed(Fragment fragment) {
        String tag = fragment.getTag();
        ArrayList<FragmentListener> listeners = this.mListeners.get(tag);
        if (listeners != null) {
            Iterator<FragmentListener> it = listeners.iterator();
            while (it.hasNext()) {
                it.next().onFragmentViewDestroyed(tag, fragment);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        if (this.mConfigChanges.applyNewConfig(this.mContext.getResources())) {
            createFragmentHost(destroyFragmentHost());
        } else {
            this.mFragments.dispatchConfigurationChanged(newConfig);
        }
    }

    /* access modifiers changed from: private */
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    /* access modifiers changed from: private */
    public <T extends View> T findViewById(int id) {
        return this.mRootView.findViewById(id);
    }

    public FragmentManager getFragmentManager() {
        return this.mFragments.getSupportFragmentManager();
    }

    /* access modifiers changed from: package-private */
    public PluginFragmentManager getPluginManager() {
        return this.mPlugins;
    }

    public static FragmentHostManager get(View view) {
        try {
            return ((FragmentService) Dependency.get(FragmentService.class)).getFragmentHostManager(view);
        } catch (ClassCastException e) {
            throw e;
        }
    }
}
