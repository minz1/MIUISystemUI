package com.android.systemui.recents.events.activity;

import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.views.RecentsView;

public class FsGestureRecentsViewWrapperEvent extends RecentsEventBus.Event {
    public final View mBackGround;
    public final ViewGroup mRecentsContainer;
    public final RecentsView mRecentsView;

    public FsGestureRecentsViewWrapperEvent(RecentsView recentsView, View backGround, ViewGroup recentsContainer) {
        this.mRecentsView = recentsView;
        this.mBackGround = backGround;
        this.mRecentsContainer = recentsContainer;
    }
}
