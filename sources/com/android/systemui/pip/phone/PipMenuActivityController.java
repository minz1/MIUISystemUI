package com.android.systemui.pip.phone;

import android.app.ActivityManager;
import android.app.ActivityManagerCompat;
import android.app.ActivityOptions;
import android.app.IActivityManager;
import android.app.RemoteAction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import com.android.systemui.pip.phone.PipMediaController;
import com.android.systemui.plugins.R;
import com.android.systemui.recents.events.RecentsEventBus;
import com.android.systemui.recents.events.component.HidePipMenuEvent;
import com.android.systemui.recents.misc.ReferenceCountedTrigger;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PipMenuActivityController {
    private IActivityManager mActivityManager;
    private ParceledListSlice mAppActions;
    private Context mContext;
    /* access modifiers changed from: private */
    public InputConsumerController mInputConsumerController;
    /* access modifiers changed from: private */
    public ArrayList<Listener> mListeners = new ArrayList<>();
    private PipMediaController.ActionListener mMediaActionListener = new PipMediaController.ActionListener() {
        public void onMediaActionsChanged(List<RemoteAction> mediaActions) {
            ParceledListSlice unused = PipMenuActivityController.this.mMediaActions = new ParceledListSlice(mediaActions);
            PipMenuActivityController.this.updateMenuActions();
        }
    };
    /* access modifiers changed from: private */
    public ParceledListSlice mMediaActions;
    private PipMediaController mMediaController;
    private int mMenuState;
    private Messenger mMessenger = new Messenger(new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case R.styleable.AppCompatTheme_textAppearancePopupMenuHeader /*100*/:
                    PipMenuActivityController.this.onMenuStateChanged(msg.arg1, true);
                    return;
                case R.styleable.AppCompatTheme_textAppearanceSearchResultSubtitle /*101*/:
                    Iterator it = PipMenuActivityController.this.mListeners.iterator();
                    while (it.hasNext()) {
                        ((Listener) it.next()).onPipExpand();
                    }
                    return;
                case R.styleable.AppCompatTheme_textAppearanceSearchResultTitle /*102*/:
                    Iterator it2 = PipMenuActivityController.this.mListeners.iterator();
                    while (it2.hasNext()) {
                        ((Listener) it2.next()).onPipMinimize();
                    }
                    return;
                case R.styleable.AppCompatTheme_textAppearanceSmallPopupMenu /*103*/:
                    Iterator it3 = PipMenuActivityController.this.mListeners.iterator();
                    while (it3.hasNext()) {
                        ((Listener) it3.next()).onPipDismiss();
                    }
                    return;
                case R.styleable.AppCompatTheme_textColorAlertDialogListItem /*104*/:
                    Messenger unused = PipMenuActivityController.this.mToActivityMessenger = msg.replyTo;
                    boolean unused2 = PipMenuActivityController.this.mStartActivityRequested = false;
                    if (PipMenuActivityController.this.mOnAttachDecrementTrigger != null) {
                        PipMenuActivityController.this.mOnAttachDecrementTrigger.decrement();
                        ReferenceCountedTrigger unused3 = PipMenuActivityController.this.mOnAttachDecrementTrigger = null;
                    }
                    if (PipMenuActivityController.this.mToActivityMessenger == null) {
                        PipMenuActivityController.this.onMenuStateChanged(0, true);
                        return;
                    }
                    return;
                case R.styleable.AppCompatTheme_textColorSearchUrl /*105*/:
                    PipMenuActivityController.this.mInputConsumerController.registerInputConsumer();
                    return;
                case R.styleable.AppCompatTheme_toolbarNavigationButtonStyle /*106*/:
                    PipMenuActivityController.this.mInputConsumerController.unregisterInputConsumer();
                    return;
                case R.styleable.AppCompatTheme_toolbarStyle /*107*/:
                    Iterator it4 = PipMenuActivityController.this.mListeners.iterator();
                    while (it4.hasNext()) {
                        ((Listener) it4.next()).onPipShowMenu();
                    }
                    return;
                default:
                    return;
            }
        }
    });
    /* access modifiers changed from: private */
    public ReferenceCountedTrigger mOnAttachDecrementTrigger;
    /* access modifiers changed from: private */
    public boolean mStartActivityRequested;
    private Bundle mTmpDismissFractionData = new Bundle();
    /* access modifiers changed from: private */
    public Messenger mToActivityMessenger;

    public interface Listener {
        void onPipDismiss();

        void onPipExpand();

        void onPipMenuStateChanged(int i, boolean z);

        void onPipMinimize();

        void onPipShowMenu();
    }

    public PipMenuActivityController(Context context, IActivityManager activityManager, PipMediaController mediaController, InputConsumerController inputConsumerController) {
        this.mContext = context;
        this.mActivityManager = activityManager;
        this.mMediaController = mediaController;
        this.mInputConsumerController = inputConsumerController;
        RecentsEventBus.getDefault().register(this);
    }

    public void onActivityPinned() {
        if (this.mMenuState == 0) {
            this.mInputConsumerController.registerInputConsumer();
        }
    }

    public void onPinnedStackAnimationEnded() {
        if (this.mToActivityMessenger != null) {
            Message m = Message.obtain();
            m.what = 6;
            try {
                this.mToActivityMessenger.send(m);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu pinned animation ended", e);
            }
        }
    }

    public void addListener(Listener listener) {
        if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
        }
    }

    public void setDismissFraction(float fraction) {
        if (this.mToActivityMessenger != null) {
            this.mTmpDismissFractionData.clear();
            this.mTmpDismissFractionData.putFloat("dismiss_fraction", fraction);
            Message m = Message.obtain();
            m.what = 5;
            m.obj = this.mTmpDismissFractionData;
            try {
                this.mToActivityMessenger.send(m);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu to update dismiss fraction", e);
            }
        } else if (!this.mStartActivityRequested) {
            startMenuActivity(0, null, null, false);
        }
    }

    public void showMenu(int menuState, Rect stackBounds, Rect movementBounds, boolean allowMenuTimeout) {
        if (this.mToActivityMessenger != null) {
            Bundle data = new Bundle();
            data.putInt("menu_state", menuState);
            data.putParcelable("stack_bounds", stackBounds);
            data.putParcelable("movement_bounds", movementBounds);
            data.putBoolean("allow_timeout", allowMenuTimeout);
            Message m = Message.obtain();
            m.what = 1;
            m.obj = data;
            try {
                this.mToActivityMessenger.send(m);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu to show", e);
            }
        } else if (!this.mStartActivityRequested) {
            startMenuActivity(menuState, stackBounds, movementBounds, allowMenuTimeout);
        }
    }

    public void pokeMenu() {
        if (this.mToActivityMessenger != null) {
            Message m = Message.obtain();
            m.what = 2;
            try {
                this.mToActivityMessenger.send(m);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify poke menu", e);
            }
        }
    }

    public void hideMenu() {
        if (this.mToActivityMessenger != null) {
            Message m = Message.obtain();
            m.what = 3;
            try {
                this.mToActivityMessenger.send(m);
            } catch (RemoteException e) {
                Log.e("PipMenuActController", "Could not notify menu to hide", e);
            }
        }
    }

    public void hideMenuWithoutResize() {
        onMenuStateChanged(0, false);
    }

    public void setAppActions(ParceledListSlice appActions) {
        this.mAppActions = appActions;
        updateMenuActions();
    }

    private ParceledListSlice resolveMenuActions() {
        if (isValidActions(this.mAppActions)) {
            return this.mAppActions;
        }
        return this.mMediaActions;
    }

    private void startMenuActivity(int menuState, Rect stackBounds, Rect movementBounds, boolean allowMenuTimeout) {
        try {
            ActivityManager.StackInfo pinnedStackInfo = ActivityManagerCompat.getStackInfo(4, 2, 0);
            if (pinnedStackInfo == null || pinnedStackInfo.taskIds == null || pinnedStackInfo.taskIds.length <= 0) {
                Log.e("PipMenuActController", "No PIP tasks found");
                return;
            }
            Intent intent = new Intent(this.mContext, PipMenuActivity.class);
            intent.putExtra("messenger", this.mMessenger);
            intent.putExtra("actions", resolveMenuActions());
            if (stackBounds != null) {
                intent.putExtra("stack_bounds", stackBounds);
            }
            if (movementBounds != null) {
                intent.putExtra("movement_bounds", movementBounds);
            }
            intent.putExtra("menu_state", menuState);
            intent.putExtra("allow_timeout", allowMenuTimeout);
            ActivityOptions options = ActivityOptions.makeCustomAnimation(this.mContext, 0, 0);
            options.setLaunchTaskId(pinnedStackInfo.taskIds[pinnedStackInfo.taskIds.length - 1]);
            options.setTaskOverlay(true, true);
            this.mContext.startActivityAsUser(intent, options.toBundle(), UserHandle.CURRENT);
            this.mStartActivityRequested = true;
        } catch (Exception e) {
            this.mStartActivityRequested = false;
            Log.e("PipMenuActController", "Error showing PIP menu activity", e);
        }
    }

    /* access modifiers changed from: private */
    public void updateMenuActions() {
        if (this.mToActivityMessenger != null) {
            Rect stackBounds = null;
            try {
                ActivityManager.StackInfo pinnedStackInfo = ActivityManagerCompat.getStackInfo(4, 2, 0);
                if (pinnedStackInfo != null) {
                    stackBounds = pinnedStackInfo.bounds;
                }
            } catch (Exception e) {
                Log.e("PipMenuActController", "Error showing PIP menu activity", e);
            }
            Bundle data = new Bundle();
            data.putParcelable("stack_bounds", stackBounds);
            data.putParcelable("actions", resolveMenuActions());
            Message m = Message.obtain();
            m.what = 4;
            m.obj = data;
            try {
                this.mToActivityMessenger.send(m);
            } catch (RemoteException e2) {
                Log.e("PipMenuActController", "Could not notify menu activity to update actions", e2);
            }
        }
    }

    private boolean isValidActions(ParceledListSlice actions) {
        return actions != null && actions.getList().size() > 0;
    }

    /* access modifiers changed from: private */
    public void onMenuStateChanged(int menuState, boolean resize) {
        if (menuState == 0) {
            this.mInputConsumerController.registerInputConsumer();
        } else {
            this.mInputConsumerController.unregisterInputConsumer();
        }
        if (menuState != this.mMenuState) {
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onPipMenuStateChanged(menuState, resize);
            }
            if (menuState == 2) {
                this.mMediaController.addListener(this.mMediaActionListener);
            } else {
                this.mMediaController.removeListener(this.mMediaActionListener);
            }
        }
        this.mMenuState = menuState;
    }

    public final void onBusEvent(HidePipMenuEvent event) {
        if (this.mStartActivityRequested) {
            this.mOnAttachDecrementTrigger = event.getAnimationTrigger();
            this.mOnAttachDecrementTrigger.increment();
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println(prefix + "PipMenuActController");
        pw.println(innerPrefix + "mMenuState=" + this.mMenuState);
        pw.println(innerPrefix + "mToActivityMessenger=" + this.mToActivityMessenger);
        pw.println(innerPrefix + "mListeners=" + this.mListeners.size());
    }
}
