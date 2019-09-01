package com.android.settingslib.suggestions;

import android.app.LoaderManager;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.service.settings.suggestions.Suggestion;
import java.util.List;

public class SuggestionControllerMixin implements LoaderManager.LoaderCallbacks<List<Suggestion>>, LifecycleObserver {
    private final Context mContext;
    private final SuggestionControllerHost mHost;
    private final SuggestionController mSuggestionController;
    private boolean mSuggestionLoaded;

    public interface SuggestionControllerHost {
        void onSuggestionReady(List<Suggestion> list);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        this.mSuggestionController.start();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        this.mSuggestionController.stop();
    }

    public Loader<List<Suggestion>> onCreateLoader(int id, Bundle args) {
        if (id == 42) {
            this.mSuggestionLoaded = false;
            return new SuggestionLoader(this.mContext, this.mSuggestionController);
        }
        throw new IllegalArgumentException("This loader id is not supported " + id);
    }

    public void onLoadFinished(Loader<List<Suggestion>> loader, List<Suggestion> data) {
        this.mSuggestionLoaded = true;
        this.mHost.onSuggestionReady(data);
    }

    public void onLoaderReset(Loader<List<Suggestion>> loader) {
        this.mSuggestionLoaded = false;
    }
}
