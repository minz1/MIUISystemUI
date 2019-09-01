package com.android.systemui.miui.statusbar;

import android.graphics.drawable.Drawable;
import android.service.notification.AbstractStatusBarNotification;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import com.android.systemui.miui.statusbar.analytics.NotificationEvent;
import com.android.systemui.miui.statusbar.notification.NotificationUtil;
import com.android.systemui.miui.statusbar.notification.PushEvents;
import com.android.systemui.miui.statusbar.notification.ScoreInfo;
import com.android.systemui.miui.statusbar.phone.rank.RankUtil;
import miui.util.NotificationFilterHelper;

public class ExpandedNotification extends AbstractStatusBarNotification {
    private Drawable mAppIcon;
    private String mAppName;
    private int mAppUid;
    private boolean mBelowThreshold;
    private boolean mHasShownAfterUnlock;
    private int mImportance;
    private boolean mIsFold;
    private double mLocalScore;
    private boolean mNewlyNotification;
    private NotificationEvent mNotificationEvent = new NotificationEvent(this, getPackageName());
    private double mPushScore;
    private Drawable mRowIcon;
    private double mScoreForRank;
    private int mShowSum;
    private int mTargetSdk;

    public ExpandedNotification(StatusBarNotification sbn) {
        super(sbn);
        if (NotificationUtil.isUserFold()) {
            calculateScore();
            this.mIsFold = calculateFold();
            this.mNotificationEvent.setPushScore(this.mPushScore);
            this.mNotificationEvent.setLocalScore(this.mLocalScore);
            this.mNotificationEvent.setFold(this.mIsFold);
            return;
        }
        ScoreInfo scoreInfo = PushEvents.getScoreInfo(getNotification());
        if (scoreInfo != null) {
            this.mNotificationEvent.setPushScore(scoreInfo.getServerScore());
        }
        this.mNotificationEvent.setLocalScore(LocalAlgoModel.getScore(this));
    }

    public String getFoldPackageName() {
        if (NotificationUtil.isHybrid(this)) {
            String packageName = NotificationUtil.getCategory(this);
            if (!TextUtils.isEmpty(packageName)) {
                return packageName;
            }
        }
        return getPackageName();
    }

    public String getPackageName() {
        if (!TextUtils.isEmpty(getNotification().extraNotification.getTargetPkg())) {
            return getNotification().extraNotification.getTargetPkg().toString();
        }
        return super.getPackageName();
    }

    public String getBasePkg() {
        return super.getPackageName();
    }

    public NotificationEvent getNotificationEvent() {
        return this.mNotificationEvent;
    }

    public boolean isSubstituteNotification() {
        return !TextUtils.equals(getPackageName(), getBasePkg());
    }

    public void setAppName(String appName) {
        this.mAppName = appName;
    }

    public String getAppName() {
        if (NotificationUtil.isHybrid(this)) {
            String hybridAppName = NotificationUtil.getHybridAppName(this);
            if (!TextUtils.isEmpty(hybridAppName)) {
                return hybridAppName;
            }
        }
        return this.mAppName;
    }

    public void setAppIcon(Drawable appIcon) {
        this.mAppIcon = appIcon;
    }

    public Drawable getAppIcon() {
        return this.mAppIcon;
    }

    public void setRowIcon(Drawable rowIcon) {
        this.mRowIcon = rowIcon;
    }

    public Drawable getRowIcon() {
        return this.mRowIcon;
    }

    public void setAppUid(int appUid) {
        this.mAppUid = appUid;
    }

    public int getAppUid() {
        return this.mAppUid;
    }

    public void setTargetSdk(int targetSdk) {
        this.mTargetSdk = targetSdk;
    }

    public int getTargetSdk() {
        return this.mTargetSdk;
    }

    private void calculateScore() {
        ScoreInfo scoreInfo = PushEvents.getScoreInfo(getNotification());
        if (scoreInfo != null) {
            this.mPushScore = scoreInfo.getServerScore();
            if (scoreInfo.getThreshold() != 0.0d && scoreInfo.getServerScore() < scoreInfo.getThreshold()) {
                this.mBelowThreshold = true;
            }
        } else {
            if (NotificationUtil.isXmsf(this)) {
                this.mNotificationEvent.setNoScore();
            }
            if (LocalAlgoModel.isLocalModelAvailable()) {
                this.mLocalScore = LocalAlgoModel.getScore(this);
                if (this.mLocalScore < LocalAlgoModel.getThreshold()) {
                    this.mBelowThreshold = true;
                }
            }
        }
        generateScoreForRank();
    }

    private boolean calculateFold() {
        boolean z = false;
        if (!NotificationUtil.isUserFold() || !NotificationFilterHelper.canNotificationSetImportance(getPackageName())) {
            return false;
        }
        if (this.mImportance != 0) {
            if (this.mImportance <= 0) {
                z = true;
            }
            return z;
        } else if ((!NotificationUtil.isPkgInFoldWhiteList(getPackageName()) || getNotification().priority != 2) && this.mShowSum > RankUtil.UNFLOD_LIMIT && !getNotification().isGroupSummary() && isClearable() && !NotificationUtil.isSystemNotification(this)) {
            return this.mBelowThreshold;
        } else {
            return false;
        }
    }

    public boolean isFold() {
        return this.mIsFold;
    }

    public void setImportance(int importance) {
        this.mImportance = importance;
        this.mIsFold = calculateFold();
        this.mNotificationEvent.setImportance(this.mImportance);
        this.mNotificationEvent.setFold(this.mIsFold);
    }

    public int getImportance() {
        return this.mImportance;
    }

    public double getPushScore() {
        return this.mPushScore;
    }

    public double getLocalScore() {
        return this.mLocalScore;
    }

    private void generateScoreForRank() {
        if (this.mPushScore != 0.0d) {
            this.mScoreForRank = this.mPushScore;
        } else {
            this.mScoreForRank = LocalAlgoModel.getScoreForRank(this.mLocalScore);
        }
    }

    public int getScore() {
        return getNotification().priority;
    }

    public void setShowSum(int showSum) {
        this.mShowSum = showSum;
        this.mIsFold = calculateFold();
        this.mNotificationEvent.setFold(this.mIsFold);
    }

    public int getShowSum() {
        return this.mShowSum;
    }

    public boolean isNewlyNotification() {
        return this.mNewlyNotification;
    }

    public void setNewlyNotification(boolean newlyNotification) {
        this.mNewlyNotification = newlyNotification;
        this.mNotificationEvent.setNewly(this.mNewlyNotification);
    }

    public boolean isShowMiuiAction() {
        return NotificationUtil.showMiuiStyle() && getNotification().extras.getBoolean("miui.showAction");
    }

    public boolean hasShownAfterUnlock() {
        return this.mHasShownAfterUnlock;
    }

    public void setHasShownAfterUnlock(boolean hasShownAfterUnlock) {
        this.mHasShownAfterUnlock = isClearable() ? hasShownAfterUnlock : false;
    }
}
