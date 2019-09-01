package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class UserTile extends QSTileImpl<QSTile.State> implements UserInfoController.OnUserInfoChangedListener {
    private Pair<String, Drawable> mLastUpdate;
    private final UserInfoController mUserInfoController = ((UserInfoController) Dependency.get(UserInfoController.class));
    private final UserSwitcherController mUserSwitcherController = ((UserSwitcherController) Dependency.get(UserSwitcherController.class));

    public UserTile(QSHost host) {
        super(host);
    }

    public QSTile.State newTileState() {
        return new QSTile.State();
    }

    public Intent getLongClickIntent() {
        return new Intent("android.settings.USER_SETTINGS");
    }

    /* access modifiers changed from: protected */
    public void handleClick() {
        showDetail(true);
    }

    public DetailAdapter getDetailAdapter() {
        return this.mUserSwitcherController.userDetailAdapter;
    }

    public int getMetricsCategory() {
        return 260;
    }

    public void handleSetListening(boolean listening) {
        if (listening) {
            this.mUserInfoController.addCallback(this);
        } else {
            this.mUserInfoController.removeCallback(this);
        }
    }

    public CharSequence getTileLabel() {
        return getState().label;
    }

    /* access modifiers changed from: protected */
    public void handleUpdateState(QSTile.State state, Object arg) {
        final Pair<String, Drawable> p = arg != null ? (Pair) arg : this.mLastUpdate;
        if (p != null) {
            state.label = (CharSequence) p.first;
            state.contentDescription = (CharSequence) p.first;
            state.icon = new QSTile.Icon() {
                public Drawable getDrawable(Context context) {
                    return (Drawable) p.second;
                }
            };
        }
    }

    public void onUserInfoChanged(String name, Drawable picture, String userAccount) {
        this.mLastUpdate = new Pair<>(name, picture);
        refreshState(this.mLastUpdate);
    }
}
