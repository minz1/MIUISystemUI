package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.UserSwitcherController;

public class MultiUserSwitch extends FrameLayout implements View.OnClickListener {
    private boolean mKeyguardMode;
    private KeyguardUserSwitcher mKeyguardUserSwitcher;
    protected QSPanel mQsPanel;
    private final int[] mTmpInt2 = new int[2];
    private UserSwitcherController.BaseUserAdapter mUserListener;
    final UserManager mUserManager = UserManager.get(getContext());
    protected UserSwitcherController mUserSwitcherController;

    public MultiUserSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(this);
        refreshContentDescription();
    }

    public void setQsPanel(QSPanel qsPanel) {
        this.mQsPanel = qsPanel;
        setUserSwitcherController((UserSwitcherController) Dependency.get(UserSwitcherController.class));
    }

    public boolean hasMultipleUsers() {
        boolean z = false;
        if (this.mUserListener == null) {
            return false;
        }
        if (this.mUserListener.getUserCount() != 0) {
            z = true;
        }
        return z;
    }

    public void setUserSwitcherController(UserSwitcherController userSwitcherController) {
        this.mUserSwitcherController = userSwitcherController;
        registerListener();
        refreshContentDescription();
    }

    private void registerListener() {
        if (this.mUserManager.isUserSwitcherEnabled() && this.mUserListener == null) {
            UserSwitcherController controller = this.mUserSwitcherController;
            if (controller != null) {
                this.mUserListener = new UserSwitcherController.BaseUserAdapter(controller) {
                    public void notifyDataSetChanged() {
                        MultiUserSwitch.this.refreshContentDescription();
                    }

                    public View getView(int position, View convertView, ViewGroup parent) {
                        return null;
                    }
                };
                refreshContentDescription();
            }
        }
    }

    public void onClick(View v) {
        if (this.mUserManager.isUserSwitcherEnabled()) {
            if (this.mKeyguardMode) {
                if (this.mKeyguardUserSwitcher != null) {
                    this.mKeyguardUserSwitcher.show(true);
                }
            } else if (this.mQsPanel != null && this.mUserSwitcherController != null) {
                View center = getChildCount() > 0 ? getChildAt(0) : this;
                center.getLocationInWindow(this.mTmpInt2);
                int[] iArr = this.mTmpInt2;
                iArr[0] = iArr[0] + (center.getWidth() / 2);
                int[] iArr2 = this.mTmpInt2;
                iArr2[1] = iArr2[1] + (center.getHeight() / 2);
                this.mQsPanel.showDetailAdapter(true, getUserDetailAdapter(), this.mTmpInt2);
            }
        } else if (this.mQsPanel != null) {
            ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(ContactsContract.QuickContact.composeQuickContactsIntent(getContext(), v, ContactsContract.Profile.CONTENT_URI, 3, null), 0);
        }
    }

    public void setClickable(boolean clickable) {
        super.setClickable(clickable);
        refreshContentDescription();
    }

    /* access modifiers changed from: private */
    public void refreshContentDescription() {
        String currentUser = null;
        if (this.mUserManager.isUserSwitcherEnabled() && this.mUserSwitcherController != null) {
            currentUser = this.mUserSwitcherController.getCurrentUserName(this.mContext);
        }
        String text = null;
        if (!TextUtils.isEmpty(currentUser)) {
            text = this.mContext.getString(R.string.accessibility_quick_settings_user, new Object[]{currentUser});
        }
        if (!TextUtils.equals(getContentDescription(), text)) {
            setContentDescription(text);
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(Button.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(Button.class.getName());
    }

    public boolean hasOverlappingRendering() {
        return false;
    }

    /* access modifiers changed from: protected */
    public DetailAdapter getUserDetailAdapter() {
        return this.mUserSwitcherController.userDetailAdapter;
    }
}
