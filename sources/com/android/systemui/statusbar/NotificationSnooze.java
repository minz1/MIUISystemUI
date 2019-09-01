package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.service.notification.SnoozeCriterion;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Property;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.statusbar.NotificationGuts;
import java.util.ArrayList;
import java.util.List;

public class NotificationSnooze extends LinearLayout implements View.OnClickListener, NotificationGuts.GutsContent {
    private int mCollapsedHeight;
    private NotificationSwipeActionHelper.SnoozeOption mDefaultOption;
    private View mDivider;
    private AnimatorSet mExpandAnimation;
    private ImageView mExpandButton;
    private boolean mExpanded;
    private NotificationGuts mGutsContainer;
    private StatusBarNotification mSbn;
    private NotificationSwipeActionHelper.SnoozeOption mSelectedOption;
    private TextView mSelectedOptionText;
    private NotificationSwipeActionHelper mSnoozeListener;
    private ViewGroup mSnoozeOptionContainer;
    private List<NotificationSwipeActionHelper.SnoozeOption> mSnoozeOptions;
    private boolean mSnoozing;
    private TextView mUndoButton;

    public NotificationSnooze(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mCollapsedHeight = getResources().getDimensionPixelSize(R.dimen.snooze_snackbar_min_height);
        findViewById(R.id.notification_snooze).setOnClickListener(this);
        this.mSelectedOptionText = (TextView) findViewById(R.id.snooze_option_default);
        this.mUndoButton = (TextView) findViewById(R.id.undo);
        this.mUndoButton.setOnClickListener(this);
        this.mExpandButton = (ImageView) findViewById(R.id.expand_button);
        this.mDivider = findViewById(R.id.divider);
        this.mDivider.setAlpha(0.0f);
        this.mSnoozeOptionContainer = (ViewGroup) findViewById(R.id.snooze_options);
        this.mSnoozeOptionContainer.setAlpha(0.0f);
        this.mSnoozeOptions = getDefaultSnoozeOptions();
        createOptionViews();
        setSelected(this.mDefaultOption);
    }

    public void setSnoozeOptions(List<SnoozeCriterion> snoozeList) {
        if (snoozeList != null) {
            this.mSnoozeOptions.clear();
            this.mSnoozeOptions = getDefaultSnoozeOptions();
            int count = Math.min(1, snoozeList.size());
            for (int i = 0; i < count; i++) {
                SnoozeCriterion sc = snoozeList.get(i);
                this.mSnoozeOptions.add(new NotificationSwipeActionHelper.SnoozeOption(sc, 0, sc.getExplanation(), sc.getConfirmation()));
            }
            createOptionViews();
        }
    }

    public boolean isExpanded() {
        return this.mExpanded;
    }

    public void setSnoozeListener(NotificationSwipeActionHelper listener) {
        this.mSnoozeListener = listener;
    }

    public void setStatusBarNotification(StatusBarNotification sbn) {
        this.mSbn = sbn;
    }

    private ArrayList<NotificationSwipeActionHelper.SnoozeOption> getDefaultSnoozeOptions() {
        ArrayList<NotificationSwipeActionHelper.SnoozeOption> options = new ArrayList<>();
        options.add(createOption(R.string.snooze_option_15_min, 15));
        options.add(createOption(R.string.snooze_option_30_min, 30));
        this.mDefaultOption = createOption(R.string.snooze_option_1_hour, 60);
        options.add(this.mDefaultOption);
        options.add(createOption(R.string.snooze_option_2_hour, 120));
        return options;
    }

    private NotificationSwipeActionHelper.SnoozeOption createOption(int descriptionResId, int minutes) {
        Resources res = getResources();
        String description = res.getString(descriptionResId);
        String resultText = String.format(res.getString(R.string.snoozed_for_time), new Object[]{description});
        SpannableString string = new SpannableString(resultText);
        string.setSpan(new StyleSpan(1), resultText.length() - description.length(), resultText.length(), 0);
        return new NotificationSwipeActionHelper.SnoozeOption(null, minutes, res.getString(descriptionResId), string);
    }

    private void createOptionViews() {
        this.mSnoozeOptionContainer.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        for (int i = 0; i < this.mSnoozeOptions.size(); i++) {
            NotificationSwipeActionHelper.SnoozeOption option = this.mSnoozeOptions.get(i);
            TextView tv = (TextView) inflater.inflate(R.layout.notification_snooze_option, this.mSnoozeOptionContainer, false);
            this.mSnoozeOptionContainer.addView(tv);
            tv.setText(option.description);
            tv.setTag(option);
            tv.setOnClickListener(this);
        }
    }

    private void hideSelectedOption() {
        int childCount = this.mSnoozeOptionContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = this.mSnoozeOptionContainer.getChildAt(i);
            child.setVisibility(child.getTag() == this.mSelectedOption ? 8 : 0);
        }
    }

    private void showSnoozeOptions(boolean show) {
        this.mExpandButton.setImageResource(show ? R.drawable.ic_collapse_notification : R.drawable.ic_expand_notification);
        if (this.mExpanded != show) {
            this.mExpanded = show;
            animateSnoozeOptions(show);
            if (this.mGutsContainer != null) {
                this.mGutsContainer.onHeightChanged();
            }
        }
    }

    private void animateSnoozeOptions(boolean show) {
        if (this.mExpandAnimation != null) {
            this.mExpandAnimation.cancel();
        }
        View view = this.mDivider;
        Property property = View.ALPHA;
        float[] fArr = new float[2];
        fArr[0] = this.mDivider.getAlpha();
        float f = 0.0f;
        fArr[1] = show ? 1.0f : 0.0f;
        ObjectAnimator dividerAnim = ObjectAnimator.ofFloat(view, property, fArr);
        ViewGroup viewGroup = this.mSnoozeOptionContainer;
        Property property2 = View.ALPHA;
        float[] fArr2 = new float[2];
        fArr2[0] = this.mSnoozeOptionContainer.getAlpha();
        if (show) {
            f = 1.0f;
        }
        fArr2[1] = f;
        ObjectAnimator optionAnim = ObjectAnimator.ofFloat(viewGroup, property2, fArr2);
        this.mExpandAnimation = new AnimatorSet();
        this.mExpandAnimation.playTogether(new Animator[]{dividerAnim, optionAnim});
        this.mExpandAnimation.setDuration(150);
        this.mExpandAnimation.setInterpolator(show ? Interpolators.ALPHA_IN : Interpolators.ALPHA_OUT);
        this.mExpandAnimation.start();
    }

    private void setSelected(NotificationSwipeActionHelper.SnoozeOption option) {
        this.mSelectedOption = option;
        this.mSelectedOptionText.setText(option.confirmation);
        showSnoozeOptions(false);
        hideSelectedOption();
    }

    public void onClick(View v) {
        if (this.mGutsContainer != null) {
            this.mGutsContainer.resetFalsingCheck();
        }
        int id = v.getId();
        NotificationSwipeActionHelper.SnoozeOption tag = (NotificationSwipeActionHelper.SnoozeOption) v.getTag();
        if (tag != null) {
            setSelected(tag);
        } else if (id == R.id.notification_snooze) {
            showSnoozeOptions(!this.mExpanded);
        } else {
            this.mSelectedOption = null;
            int[] parentLoc = new int[2];
            int[] targetLoc = new int[2];
            this.mGutsContainer.getLocationOnScreen(parentLoc);
            v.getLocationOnScreen(targetLoc);
            int i = targetLoc[0] - parentLoc[0];
            int i2 = targetLoc[1] - parentLoc[1];
            showSnoozeOptions(false);
            this.mGutsContainer.closeControls(i + (v.getWidth() / 2), i2 + (v.getHeight() / 2), false, false);
        }
    }

    public int getActualHeight() {
        return this.mExpanded ? getHeight() : this.mCollapsedHeight;
    }

    public boolean willBeRemoved() {
        return this.mSnoozing;
    }

    public View getContentView() {
        setSelected(this.mDefaultOption);
        return this;
    }

    public void setGutsParent(NotificationGuts guts) {
        this.mGutsContainer = guts;
    }

    public boolean handleCloseControls(boolean save, boolean force) {
        if (this.mExpanded && !force) {
            showSnoozeOptions(false);
            return true;
        } else if (this.mSnoozeListener == null || this.mSelectedOption == null) {
            setSelected(this.mSnoozeOptions.get(0));
            return false;
        } else {
            this.mSnoozing = true;
            this.mSnoozeListener.snooze(this.mSbn, this.mSelectedOption);
            return true;
        }
    }

    public boolean isLeavebehind() {
        return true;
    }
}
