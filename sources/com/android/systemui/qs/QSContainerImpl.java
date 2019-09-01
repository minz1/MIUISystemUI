package com.android.systemui.qs;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSliderView;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QSContainerImpl extends FrameLayout implements TunerService.Tunable {
    protected View mBackground;
    private BrightnessController mBrightnessController;
    private boolean mBrightnessListening;
    private BrightnessMirrorController mBrightnessMirrorController;
    private ToggleSliderView mBrightnessView;
    private ValueAnimator mCaretAnimator;
    /* access modifiers changed from: private */
    public CaretDrawable mCaretDrawable;
    private Interpolator mCaretInterpolator;
    protected View mContent;
    private QSFooterDataUsage mDataUsageBar;
    private ImageView mExpandIndicator;
    private int mFooterChildCount = 0;
    private int mGutterHeight;
    protected QuickStatusBarHeader mHeader;
    private int mHeightOverride = -1;
    protected float mIndicarotProgress;
    private boolean mListening;
    protected View mQSContainer;
    private QSCustomizer mQSCustomizer;
    private QSDetail mQSDetail;
    private QSFooter mQSFooter;
    private View mQSFooterBundle;
    protected LinearLayout mQSFooterContainer;
    protected QSPanel mQSPanel;
    protected float mQsExpansion;
    protected QuickQSPanel mQuickQsPanel;
    private final Point mSizePoint = new Point();

    public QSContainerImpl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* access modifiers changed from: protected */
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "qs_show_brightness");
    }

    /* access modifiers changed from: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();
        this.mHeader = (QuickStatusBarHeader) findViewById(R.id.header);
        this.mContent = findViewById(R.id.qs_content);
        this.mQSContainer = findViewById(R.id.qs_container);
        this.mBackground = findViewById(R.id.qs_background);
        this.mQuickQsPanel = (QuickQSPanel) findViewById(R.id.quick_qs_panel);
        this.mQuickQsPanel.setVisibility(res.getBoolean(R.bool.config_showQuickSettingsRow) ? 0 : 8);
        this.mQSPanel = (QSPanel) findViewById(R.id.quick_settings_panel);
        this.mQSFooterContainer = (LinearLayout) findViewById(R.id.qs_footer_container);
        this.mQSFooterBundle = findViewById(R.id.qs_footer_bundle);
        this.mBrightnessView = (ToggleSliderView) findViewById(R.id.qs_brightness);
        this.mBrightnessController = new BrightnessController(getContext(), this.mBrightnessView);
        this.mExpandIndicator = (ImageView) findViewById(R.id.qs_expand_indicator);
        this.mCaretDrawable = new CaretDrawable(getContext());
        this.mExpandIndicator.setImageDrawable(this.mCaretDrawable);
        this.mCaretInterpolator = AnimationUtils.loadInterpolator(getContext(), 17563661);
        this.mQSDetail = (QSDetail) findViewById(R.id.qs_detail);
        this.mQSCustomizer = (QSCustomizer) findViewById(R.id.qs_customize);
        this.mGutterHeight = res.getDimensionPixelSize(R.dimen.qs_gutter_height);
        setClickable(true);
        setImportantForAccessibility(2);
        setupAnimatedViews();
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(getDisplayHeight(), 1073741824);
        this.mQSCustomizer.measure(widthMeasureSpec, heightSpec);
        this.mQSDetail.measure(widthMeasureSpec, heightSpec);
    }

    public void updateQSDataUsage(boolean enabled) {
        if (enabled) {
            if (this.mDataUsageBar == null) {
                this.mDataUsageBar = (QSFooterDataUsage) LayoutInflater.from(getContext()).inflate(R.layout.qs_footer_data_usage, this.mQSFooterContainer, false);
                this.mQSFooterContainer.addView(this.mDataUsageBar);
                this.mDataUsageBar.setQSContainer(this);
            }
        } else if (this.mDataUsageBar != null) {
            if (this.mQSFooter == null) {
                this.mQSFooterContainer.setVisibility(8);
            }
            this.mQSFooterContainer.removeView(this.mDataUsageBar);
            this.mDataUsageBar.setQSContainer(null);
            this.mDataUsageBar = null;
            updateFooter();
        }
    }

    public void updateFooter() {
        int childCount = this.mQSFooterContainer.getChildCount();
        if (this.mDataUsageBar != null && !this.mDataUsageBar.isAvailable()) {
            childCount--;
        }
        if (this.mFooterChildCount != childCount) {
            this.mFooterChildCount = childCount;
            if (childCount > 0) {
                this.mQSFooterContainer.setVisibility(0);
            } else {
                this.mQSFooterContainer.setVisibility(8);
            }
        }
    }

    public boolean isDataUsageAvailable() {
        return this.mDataUsageBar != null;
    }

    public void updateDataUsageInfo() {
        if (this.mDataUsageBar != null) {
            this.mDataUsageBar.updateDataUsageInfo();
        }
    }

    public QSFooter getQSFooter() {
        return this.mQSFooter;
    }

    /* access modifiers changed from: protected */
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mBrightnessMirrorController != null) {
            setBrightnessMirror(this.mBrightnessMirrorController);
        }
        this.mSizePoint.set(0, 0);
    }

    /* access modifiers changed from: protected */
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
    }

    public void onTuningChanged(String key, String newValue) {
        if ("qs_show_brightness".equals(key)) {
            this.mBrightnessView.setVisibility((newValue == null || Integer.parseInt(newValue) != 0) ? 0 : 8);
        }
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        this.mBrightnessView.setLayoutDirection(layoutDirection);
    }

    public boolean performClick() {
        return true;
    }

    public void setHeightOverride(int heightOverride) {
        if (this.mHeightOverride != heightOverride) {
            this.mHeightOverride = heightOverride;
            updateExpansion();
        }
    }

    public boolean isQSFullyCollapsed() {
        return this.mQsExpansion <= 0.0f;
    }

    public void setExpansion(float expansion) {
        float progress = 0.0f;
        if (this.mQsExpansion - expansion > 0.002f && expansion != 0.0f) {
            progress = -1.0f;
        } else if (expansion - this.mQsExpansion > 0.002f && expansion != 1.0f) {
            progress = 1.0f;
        }
        this.mQsExpansion = expansion;
        updateIndicator(progress);
        updateExpansion();
    }

    public void updateIndicator(float progress) {
        if (this.mIndicarotProgress != progress) {
            this.mIndicarotProgress = progress;
            if (this.mCaretAnimator != null && this.mCaretAnimator.isRunning()) {
                this.mCaretAnimator.cancel();
                this.mCaretDrawable.setCaretProgress(0.0f);
            }
            this.mCaretAnimator = ValueAnimator.ofFloat(new float[]{0.0f, progress});
            this.mCaretAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animator) {
                    QSContainerImpl.this.mCaretDrawable.setCaretProgress(((Float) animator.getAnimatedValue()).floatValue());
                }
            });
            this.mCaretAnimator.setDuration(200);
            this.mCaretAnimator.setInterpolator(this.mCaretInterpolator);
            this.mCaretAnimator.start();
        }
    }

    public void updateExpansion() {
        int height = calculateContainerHeight();
        setBottom(getTop() + height);
        this.mContent.setBottom(this.mContent.getTop() + height);
        this.mQSContainer.setBottom(this.mQSContainer.getTop() + height);
        this.mBackground.setBottom(((this.mBackground.getTop() + height) - this.mHeader.getHeight()) - this.mQSFooterBundle.getHeight());
        int footerTranslationY = (getBottom() - this.mQSFooterBundle.getBottom()) - this.mHeader.getHeight();
        this.mQSFooterContainer.setTranslationY((float) footerTranslationY);
        this.mQSFooterBundle.setTranslationY((float) footerTranslationY);
    }

    /* access modifiers changed from: protected */
    public int calculateContainerHeight() {
        int heightOverride = this.mHeightOverride != -1 ? this.mHeightOverride : this.mContent.getMeasuredHeight();
        int minHeight = getQsMinExpansionHeight();
        return Math.round((this.mQsExpansion * ((float) (heightOverride - minHeight))) + ((float) minHeight));
    }

    public int getQsMinExpansionHeight() {
        int minHeight = this.mHeader.getHeight() + this.mQuickQsPanel.getHeight() + this.mQSFooterBundle.getHeight();
        if (this.mQSFooterContainer.isShown()) {
            return minHeight + this.mQSFooterContainer.getHeight();
        }
        return minHeight;
    }

    public void setGutterEnabled(boolean gutterEnabled) {
        if (gutterEnabled != (this.mGutterHeight != 0)) {
            if (gutterEnabled) {
                this.mGutterHeight = getContext().getResources().getDimensionPixelSize(R.dimen.qs_gutter_height);
            } else {
                this.mGutterHeight = 0;
            }
            updateExpansion();
        }
    }

    public void setBrightnessMirror(BrightnessMirrorController mirrorController) {
        this.mBrightnessMirrorController = mirrorController;
        this.mBrightnessView.setMirror((ToggleSliderView) mirrorController.getMirror().findViewById(R.id.brightness_slider));
        this.mBrightnessView.setMirrorController(mirrorController);
    }

    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            if (this.mQSFooter != null) {
                this.mQSFooter.setListening(this.mListening);
            }
        }
    }

    public void setBrightnessListening(boolean listening) {
        if (this.mBrightnessListening != listening) {
            this.mBrightnessListening = listening;
            if (listening) {
                this.mBrightnessController.registerCallbacks();
            } else {
                this.mBrightnessController.unregisterCallbacks();
            }
        }
    }

    public View getBrightnessView() {
        return this.mBrightnessView;
    }

    public View getExpandIndicator() {
        return this.mExpandIndicator;
    }

    private int getDisplayHeight() {
        if (this.mSizePoint.y == 0) {
            getDisplay().getRealSize(this.mSizePoint);
        }
        return this.mSizePoint.y;
    }

    private void setupAnimatedViews() {
        List<View> detailAnimatedViews = Arrays.asList(new View[]{this.mQSFooterBundle, this.mQSFooterContainer, this.mQuickQsPanel, this.mQSPanel});
        List<View> customizerAnimatedViews = new ArrayList<>(detailAnimatedViews);
        customizerAnimatedViews.add(this.mHeader);
        this.mQSDetail.setAnimatedViews(detailAnimatedViews);
        this.mQSCustomizer.setAnimatedViews(customizerAnimatedViews);
    }
}
