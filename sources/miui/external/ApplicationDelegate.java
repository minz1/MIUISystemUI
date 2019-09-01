package miui.external;

import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.ContextWrapper;
import android.content.res.Configuration;

public abstract class ApplicationDelegate extends ContextWrapper implements ComponentCallbacks2 {
    private Application mApplication;

    public ApplicationDelegate() {
        super(null);
    }

    /* access modifiers changed from: package-private */
    public void attach(Application application) {
        this.mApplication = application;
        attachBaseContext(application);
    }

    public void onCreate() {
        this.mApplication.superOnCreate();
    }

    public void onTerminate() {
        this.mApplication.superOnTerminate();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        this.mApplication.superOnConfigurationChanged(newConfig);
    }

    public void onLowMemory() {
        this.mApplication.superOnLowMemory();
    }

    public void onTrimMemory(int level) {
        this.mApplication.superOnTrimMemory(level);
    }

    public void registerComponentCallbacks(ComponentCallbacks callback) {
        this.mApplication.registerComponentCallbacks(callback);
    }

    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        this.mApplication.unregisterComponentCallbacks(callback);
    }
}
