package com.android.keyguard;

import android.content.Context;
import android.util.Log;
import android.util.Slog;
import com.miui.internal.policy.impl.AwesomeLockScreenImp.AwesomeLockScreenView;
import com.miui.internal.policy.impl.AwesomeLockScreenImp.LockScreenElementFactory;
import com.miui.internal.policy.impl.AwesomeLockScreenImp.LockScreenResourceLoader;
import com.miui.internal.policy.impl.AwesomeLockScreenImp.LockScreenRoot;
import java.io.File;
import java.util.Stack;
import miui.content.res.ThemeResources;
import miui.maml.LifecycleResourceManager;
import miui.maml.ScreenContext;

public class RootHolder {
    private ScreenContext mContext;
    private LifecycleResourceManager mResourceMgr;
    private LockScreenRoot mRoot;
    private String mTempCachePath;
    private Stack<AwesomeLockScreen> mViewList = new Stack<>();

    public boolean init(Context context, AwesomeLockScreen ls) {
        if (this.mTempCachePath == null) {
            this.mTempCachePath = context.getCacheDir() + File.separator + "lockscreen_cache";
        }
        if (this.mRoot == null) {
            ThemeResources.getSystem().resetLockscreen();
            LifecycleResourceManager lifecycleResourceManager = new LifecycleResourceManager(new LockScreenResourceLoader().setLocal(context.getResources().getConfiguration().locale), 86400000, 3600000);
            this.mResourceMgr = lifecycleResourceManager;
            this.mResourceMgr.setCacheSize(104857600);
            this.mContext = new ScreenContext(context, this.mResourceMgr, new LockScreenElementFactory());
            this.mRoot = new LockScreenRoot(this.mContext);
            this.mRoot.setConfig("/data/system/theme/config.config");
            this.mRoot.setCacheDir(this.mTempCachePath);
            if (!this.mRoot.load()) {
                Slog.e("RootHolder", "fail to load element root");
                this.mRoot = null;
                return false;
            }
            Log.d("RootHolder", "create root");
        } else {
            this.mResourceMgr.setLocal(context.getResources().getConfiguration().locale);
        }
        this.mViewList.push(ls);
        Log.d("RootHolder", "init:" + ls.toString());
        return true;
    }

    public void clear() {
        this.mRoot = null;
        this.mContext = null;
        if (this.mResourceMgr != null) {
            this.mResourceMgr.finish(false);
            this.mResourceMgr = null;
        }
        if (this.mTempCachePath != null) {
            new File(this.mTempCachePath).delete();
        }
    }

    public ScreenContext getContext() {
        return this.mContext;
    }

    public LockScreenRoot getRoot() {
        return this.mRoot;
    }

    public AwesomeLockScreenView createView(Context context) {
        if (this.mRoot == null) {
            return null;
        }
        AwesomeLockScreenView view = new AwesomeLockScreenView(context, this.mRoot);
        Log.d("RootHolder", "createView");
        return view;
    }

    public void cleanUp(AwesomeLockScreen ls) {
        if (this.mRoot != null) {
            this.mViewList.remove(ls);
            ls.cleanUpView();
            Log.d("RootHolder", "cleanUp: " + ls.toString() + " size:" + this.mViewList.size());
            if (this.mViewList.size() == 0) {
                this.mRoot.getContext().mVariables.reset();
                this.mRoot = null;
                Log.d("RootHolder", "cleanUp finish");
            } else {
                this.mViewList.peek().rebindView();
            }
        }
    }
}
